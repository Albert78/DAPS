package de.dh.daps.core.system

import de.dh.daps.core.SystemRegistry

/**
 * Interface to be implemented by the Application class to provide access to the registry.
 */
interface RegistryProvider {
    val registry: SystemRegistry
}