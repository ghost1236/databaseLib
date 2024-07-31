package net.common.databaselib

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import net.common.databaselib.Listener

class SQLiteHelper(context: Context, dbName: String, dbVersion: Int, private var listener: Listener.OnDatabaseListener) : SQLiteOpenHelper(context, dbName, null, dbVersion) {

    var mdb: SQLiteDatabase

    init {
        mdb = writableDatabase
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.let {
            mdb = db
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.let {
            mdb = db
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

    fun crateTable(queryList: ArrayList<String>) {
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

    fun insertOrUpdate(tableName: String, values: ContentValues) : Long {
        return mdb.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun update(tableName: String, values: ContentValues, where: String, condition: Array<String>?) : Long {
        return mdb.update(tableName, values, where, condition).toLong()
    }

    fun delete(tableName: String, where: String, condition: Array<String>?) : Long {
        return mdb.delete(tableName, where, condition).toLong()
    }
}