package de.dh.pump.omnipod.dash.db

enum class OmnipodHistoryEventType {
    BOLUS,
    CANCEL_BOLUS,
    STOP_BOLUS,
    TEMP_BASAL,
    CANCEL_TEMP_BASAL,
    SUSPEND,
    RESUME,
    PROFILE,
    STATUS_UPDATE,
    PRIMING,
    CANNULA_INSERTION,
    POD_ACTIVATED,
    POD_DEACTIVATED,
    ALARM_OCCURRED,
    ALARM_SILENCED,
    POD_FAULT,
    PLAY_TEST_BEEP
}