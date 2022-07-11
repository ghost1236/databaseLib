package net.common.databaselib

import net.sqlcipher.database.SQLiteDatabase

class Listener {

    interface OnDatabaseListener {
        fun onUpgrade(oldVersion: Int, newVersion: Int)
    }
}