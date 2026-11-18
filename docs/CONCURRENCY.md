# Concurrency Strategy in DAPS

To prevent `ConcurrentModificationException`, guarantee sub-millisecond performance for the APS core, and ensure data integrity across UI and background threads, the following strategy is enforced:

## 1. Thread-Safe Simple Caches
Simple in-memory calculation caches (like `SampledCarbsCalculationCache` and `SampledInsulinCalculationCache`) must use `ConcurrentHashMap` from `java.util.concurrent`. This enables concurrent reads and lock-free thread-safe updates.

## 2. Room-Backed Reactive `StateFlow`s in Repositories
Repositories managing active therapy and treatment state (such as `TreatmentRepository`) use Room-backed `StateFlow`s (`stateIn`) instead of manual in-memory `MutableList` caches protected by `Mutex`:
- **Lock-Free Reads**: Core and UI components read active state synchronously via `stateFlow.value` in sub-milliseconds without acquiring locks.
- **Transactional Writes**: Writes (`insert`, `update`, `delete`) execute directly against Room DAOs, which manage thread safety and SQLite transactions.
- **Automatic Propagation**: Database changes automatically trigger Room `Flow` emissions, which update the `StateFlow`s atomically.
- **Memory Boundary**: Repositories scope active `StateFlow`s to the rolling `historySize` window (e.g. 24–48 hours) to maintain a minimal memory footprint.

## 3. Core Threading & Reentrancy Protection
The APS Core execution is orchestrated by `SystemOrchestrator` and `Core`:
- **Single-Threaded Dispatcher**: Core operations run on a dedicated single-thread dispatcher (`Executors.newSingleThreadExecutor().asCoroutineDispatcher()`). This guarantees thread affinity, FIFO execution ordering, and CPU cache locality.
- **Mutex Protection for Reentrancy**: Because coroutines suspend at I/O boundaries (e.g. database access or pump synchronization), a `kotlinx.coroutines.sync.Mutex` inside `Core` protects all entry points (`processCalculation`, `onNewBgReading`, `onTherapySettingsChanged`, `onMealsChanged`, `onInsulinChanged`, `getAssumedBg`). This prevents reentrancy and logical race conditions across suspension points.
- **External Interface Protection**: Objects exposed to external threads or UI ViewModels (such as `BolusCorrectionCalculator`) are wrapped in a Mutex decorator (`MutexProtectedBolusCorrectionCalculator`). This ensures UI calls acquire the Core's Mutex and wait for active Core ticks to complete, guaranteeing consistent, atomic snapshots.

## 4. Immutable State Snapshots
Repositories expose data as immutable `List` references wrapped in `StateFlow` or returned by `suspend` functions:
- **Immutability**: `StateFlow` emits new immutable lists on state changes.
- **Safety**: Readers iterating over `stateFlow.value` operate on immutable snapshots, eliminating `ConcurrentModificationException` without requiring manual defensive copies or locks.
- **Rule**: Never expose internal `MutableList` or mutable collection references.

## 5. Reactive Updates via Flows
Use `Flow` and `StateFlow` for state propagation:
- Room DAOs provide thread-safe `Flow` streams for database observation.
- ViewModels consume these flows and expose them as `StateFlow` to the Compose UI layer.

## Summary Table

| Use Case | Recommended Approach |
| :--- | :--- |
| **Simple Cache** | `ConcurrentHashMap` |
| **Repository Active State** | Room-backed `StateFlow` (`stateIn`) with lock-free `.value` reads & DAO writes |
| **Repository Historical Data** | Uncached Room `Flow` streams (direct from SQLite for UI screens) |
| **APS Core Loop** | Single-Threaded Dispatcher + `Mutex.withLock` on all entry points |
| **Core External Tools (Calculators)** | Mutex Decorator (`MutexProtectedBolusCorrectionCalculator`) |
| **State Propagation** | `Flow` / `StateFlow` |
| **Database Access** | Room DAOs (thread-safe, transactional) |