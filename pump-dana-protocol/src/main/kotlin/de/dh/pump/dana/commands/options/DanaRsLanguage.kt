package de.dh.pump.dana.commands.options

/**
 * Confirmed Language Mapping for Dana-i (Europe Firmware).
 */
enum class DanaRsLanguage(val id: Int, val displayName: String) {
    ENGLISH(2, "English"),
    GERMAN(4, "Deutsch"),
    FRENCH(16, "Français"),
    DUTCH(20, "Nederlands"),
    ITALIAN(21, "Italiano"),
    UNKNOWN(-1, "Unknown");

    companion object {
        fun fromId(id: Int): DanaRsLanguage = entries.firstOrNull { it.id == id } ?: UNKNOWN

        /**
         * Returns a readable name for a given language ID,
         * using the confirmed mapping or a fallback.
         */
        fun getName(id: Int): String {
            val lang = fromId(id)
            return if (lang == UNKNOWN) "ID $id" else lang.displayName
        }
    }
}