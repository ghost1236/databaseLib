package net.common.databaselib

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SQLiteHelper(
    context: Context,
    dbName: String,
    dbVersion: Int,
    private var listener: Listener.OnDatabaseListener
) : SQLiteOpenHelper(context, dbName, null, dbVersion) {

    lateinit var mdb: SQLiteDatabase

    /**
     * DB 를 연다.
     * 생성자에서 바로 열지 않고 분리한 이유: writableDatabase 접근이 onCreate/onUpgrade 를 트리거하는데,
     * 그 시점에 SQLManager 의 helper 참조가 이미 할당돼 있어야 마이그레이션 콜백에서 안전하게 쓸 수 있다.
     */
    fun open() {
        mdb = writableDatabase
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.let { mdb = it }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.let { mdb = it }
        listener.onUpgrade(oldVersion, newVersion)
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.let { mdb = it }
        listener.onDowngrade(oldVersion, newVersion)
    }

    fun beginTransaction() = mdb.beginTransaction()
    fun endTransaction() = mdb.endTransaction()
    fun setTransactionSuccessful() = mdb.setTransactionSuccessful()

    fun executeQuery(query: String): Cursor {
        val cursor = mdb.rawQuery(query, null)
        cursor.moveToPosition(-1)
        return cursor
    }

    fun closeHelper() {
        if (::mdb.isInitialized) mdb.close()
        close()
    }

    fun createTable(queryList: ArrayList<String>) = create(queryList)

    fun create(sql: String) = create(arrayListOf(sql))

    fun create(queryList: ArrayList<String>) {
        for (sql in queryList) {
            mdb.execSQL(sql)
        }
    }

    fun insert(tableName: String, values: ContentValues): Long =
        mdb.insert(tableName, null, values)

    fun insertOrUpdate(tableName: String, values: ContentValues): Long =
        mdb.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE)

    fun update(tableName: String, values: ContentValues, where: String, condition: Array<String>?): Long =
        mdb.update(tableName, values, where, condition).toLong()

    fun delete(tableName: String, where: String, condition: Array<String>?): Long =
        mdb.delete(tableName, where, condition).toLong()
}
