package net.common.databaselib

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import net.common.databaselib.Listener
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteOpenHelper
import kotlin.collections.ArrayList

class SQLChiperHelper(context: Context?, dbName: String, dbVersion: Int, password: String, databaseListener: Listener.OnDatabaseListener) : SQLiteOpenHelper(context, dbName, null, dbVersion) {

    var mdb: SQLiteDatabase
    var listener: Listener.OnDatabaseListener? = null

    init {
        mdb = getWritableDatabase(password)
        listener = databaseListener
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.let {
            mdb = it
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.let {
            mdb = it
        }
        listener?.let { it.onUpgrade(oldVersion, newVersion) }
    }

    fun beginTransaction() {
        mdb.beginTransaction()
    }

    fun endTransaction() {
        mdb.endTransaction()
    }

    fun setTransactionSuccessful() {
        mdb.setTransactionSuccessful()
    }

    fun executeQuery(query: String) : Cursor {
        var cursor = mdb.rawQuery(query, null)
        cursor.let {
            cursor.moveToPosition(-1)
        }
        return cursor
    }

    fun closeHelper() {
        mdb?.let {
            it.close()
        }
        close()
    }

    fun createTable(queryList: ArrayList<String>) {
        create(queryList)
    }

    fun create(sql: String) {
        var list = ArrayList<String>()
        list.add(sql)
        create(list)
    }

    fun create(queryList: ArrayList<String>) {
        for (sql in queryList) {
            mdb.execSQL(sql)
        }
    }

    fun insert(tableName: String, values: ContentValues) : Long {
        return mdb.insert(tableName, "", values)
    }

    fun update(tableName: String, values: ContentValues, where: String, condition: Array<String>?) : Long {
        return mdb.update(tableName, values, where, condition).toLong()
    }

    fun delte(tableName: String, where: String,  condition: Array<String>?) : Long {
        return mdb.delete(tableName, where, condition).toLong()
    }
}