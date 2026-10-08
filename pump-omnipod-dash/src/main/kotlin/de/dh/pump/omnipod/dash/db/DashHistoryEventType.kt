package de.dh.pump.omnipod.dash.db

enum class DashHistoryEventType {
    BOLUS,
    STOP_BOLUS,
    TEMP_BASAL,
    CANCEL_TEMP_BASAL,
    SUSPEND,
    RESUME,
    PROFILE,
    STATUS_UPDATE,
    POD_ACTIVATED,
    POD_DEACTIVATED
}