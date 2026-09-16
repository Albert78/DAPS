# Documentation: Alarm Model & Alarm System in DAPS

This document describes the architecture, objectives, and technical implementation of the alarm system in DAPS.

---

## 💡 Motivation & System Philosophy

During the design of the alarm system, two practical requirements were paramount:

1. **Prevention of Alarm Fatigue**: Non-critical events (e.g., slightly elevated glucose levels or upcoming maintenance reminders) must not overwhelm the user with high-frequency or unnecessarily loud signals, preserving user attention for acutely actionable conditions.
2. **Efficient Configuration**: Instead of fine-grained individual configuration for dozens of different alarm types, the system relies on structured, severity-based inheritance.

DAPS addresses these requirements through a three-level severity hierarchy, context-aware alarm profiles, and the automatic resetting of temporary settings.

---

## ⚙️ System Architecture & Core Concepts

### 1. Severities & Categories (3-Tier Model)
In the system, every `AlarmType` is permanently assigned to a **Category** and a **Severity**:

* **Categories**:
  * `GLUCOSE`: Blood glucose-related conditions (hypoglycemia, hyperglycemia, BG deltas).
  * `PUMP`: Insulin pump status (occlusion, low insulin, battery level).
  * `CGM`: Glucose sensor status (signal loss, sensor expiration).
  * `SYSTEM`: App and operating system states (system battery, permissions).

* **Severities**:
  * 🔴 **`CRITICAL`**: Acutely actionable emergencies (e.g., severe hypoglycemia `< 54 mg/dL`, pump occlusion).
  * 🟡 **`WARNING`**: Conditions requiring attention (e.g., low BG `< 70 mg/dL`, high BG `> 250 mg/dL`, CGM signal loss).
  * 🔵 **`INFO`**: Maintenance and status information (e.g., low battery, infusion set change reminder).

At the severity level, the user defines global default settings for volume, vibration pattern, and DND bypass, which automatically apply to all assigned alarms.

---

### 2. Alarm Profiles & Cascading Overrides
An **Alarm Profile** bundles the signal configurations (`AlarmSoundConfig`) of all three severities into a logical application context (e.g., *Standard*, *Cinema / Discrete*, *Loud / Outdoor*).

* **Inheritance System**:
  1. **Severity Defaults**: Provide the base configuration for all assigned alarms.
  2. **Custom Overrides (`customOverrides`)**: Individual alarm types can be specifically customized (e.g., a different vibration for infusion set changes despite the `INFO` severity).

---

### 3. Safety-Critical Alarms & "Do Not Disturb" (DND)
Safety-relevant events (e.g., severe hypoglycemia or interruption of insulin delivery) are declared in the system as `isSafetyCritical = true`.

* **DND Bypass**: When the `overrideDnd` option is enabled, the audio engine uses Android audio attributes `AudioAttributes.USAGE_ALARM` and `AudioManager.STREAM_ALARM` to bypass the system's "Do Not Disturb" mode.
* **Minimum Volume**: Safety-critical alarms maintain a defined minimum volume even in quiet profiles.

---

### 4. Smart Snooze & Re-Alarming
* **Snooze Function**: Acknowledged alarms can be temporarily muted (e.g., for 15, 30, or 60 minutes) to allow time for countermeasures (e.g., carbohydrate intake) to take effect.
* **Re-Alarming**: After the snooze interval expires, the system re-evaluates the current state. If the condition persists, the notification is triggered again.
* **Safety Limit**: For safety-critical alarms, a maximum snooze duration applies (e.g., max 30 minutes).

---

### 5. Integration into Therapy Settings & Scheduled Adjustments

#### Dynamic Alarm Profile in `CurrentTherapySettings`
The active alarm profile is an integral part of the current therapy settings (`CurrentTherapySettings`):
* `defaultAlarmProfile`: The regularly active base profile.
* `alarmProfileOverride`: An optional temporary override profile.
* `effectiveAlarmProfile`: Evaluated as `alarmProfileOverride ?: defaultAlarmProfile`.

When a temporary therapy adjustment (e.g., "Exercise" or "Cinema") is activated, a different alarm profile can be assigned to it. Upon expiration or cancellation of the therapy adjustment, the system automatically restores the default alarm profile.

#### Scheduled Adjustments (`ScheduledTherapyAdjustment`)
Since therapy adjustments can be scheduled in advance (`ScheduledTherapyAdjustment` with `startTime` and `endTime`), the assigned alarm profile is automatically applied on a schedule. The `TherapyManager` handles transitions using Android system wakeups (`SystemWakeService`).