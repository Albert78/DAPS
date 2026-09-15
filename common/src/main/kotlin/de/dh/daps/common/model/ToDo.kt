package de.dh.daps.common.model

import android.util.Log

object ToDo {
    fun toBeImplemented(what: String) {
        Log.i("ToDo", "Code is still to be implemented: $what")
    }

    fun removeTestCode(what: String) {
        Log.i("ToDo", "Testcode to be removed: $what")
    }
}