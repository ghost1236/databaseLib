package net.common.databaselib

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import net.zetetic.database.sqlcipher.SQLiteDatabase

/**
 * SQLCipher(암호화) DB 헬퍼. (sqlcipher-android 4.15 신 API)
 *
 * 신 API 의 SQLiteOpenHelper.getWritableDatabase 는 반환타입이 다른 오버로드가 둘이라
 * Kotlin 에서 호출이 모호해진다. 그래서 SQLiteOpenHelper 를 상속하지 않고
 * SQLiteDatabase.openOrCreateDatabase 로 직접 열고, 버전/마이그레이션을 수동 처리한다.
 */
class SQLChiperHelper(
    private val context: Context?,
    private val dbName: String,
    private val dbVersion: Int,
    private val password: String,
    private var listener: Listener.OnDatabaseListener
) {

    lateinit var mdb: SQLiteDatabase

    /** native 로드 후 password 로 DB 를 열고, 버전 변화 시 마이그레이션 콜백을 호출한다. */
    fun open() {
        System.loadLibrary("sqlcipher")
        val dbFile = context!!.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        mdb = SQLiteDatabase.openOrCreateDatabase(dbFile, password, null, null)
        handleVersion()
    }

    // SQLiteOpenHelper 의 onCreate/onUpgrade/onDowngrade 자동 호출을 대신하는 수동 버전 처리
    private fun handleVersion() {
        val old = mdb.version
        when {
            old == 0 -> mdb.version = dbVersion                                   // 신규 DB
            old < dbVersion -> { listener.onUpgrade(old, dbVersion); mdb.version = dbVersion }
            old > dbVersion -> { listener.onDowngrade(old, dbVersion); mdb.version = dbVersion }
        }
    }

    fun beginTransaction() = mdb.beginTransaction()
    fun endTransaction() = mdb.endTransaction()
    fun setTransactionSuccessful() = mdb.setTransactionSuccessful()

    fun executeQuery(query: String): Cursor {
        val cursor = mdb.rawQuery(query, null as Array<String>?)
        cursor.moveToPosition(-1)
        return cursor
    }

    fun closeHelper() {
        if (::mdb.isInitialized) mdb.close()
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
