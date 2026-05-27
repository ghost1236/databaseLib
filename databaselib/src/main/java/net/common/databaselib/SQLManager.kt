package net.common.databaselib

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import net.common.databaselib.DBUtils.convertMapToContentValues

object SQLManager {

    lateinit var sqlLiteHelper: SQLiteHelper
    lateinit var sqlChiperHelper: SQLChiperHelper
    private var isPassword: Boolean = false

    /** 마이그레이션을 등록하지 않았거나, 단계 마이그레이션으로 못 채운 구간의 fallback 콜백 */
    var upgradeCallback: ((Int, Int) -> Unit)? = null

    /** 다운그레이드 콜백(미설정 시 no-op) */
    var downgradeCallback: ((Int, Int) -> Unit)? = null

    private data class Migration(val from: Int, val to: Int, val migrate: () -> Unit)

    private val migrations = mutableListOf<Migration>()

    private val dbListener = object : Listener.OnDatabaseListener {
        override fun onUpgrade(oldVersion: Int, newVersion: Int) = handleUpgrade(oldVersion, newVersion)
        override fun onDowngrade(oldVersion: Int, newVersion: Int) = handleDowngrade(oldVersion, newVersion)
    }

    /**
     * 단계별 마이그레이션을 등록한다. **init() 호출 전에** 등록해야 한다.
     * 예) addMigration(1, 2) { createTable("ALTER TABLE ...") }
     *     addMigration(2, 3) { ... }
     * → 버전 1→3 업그레이드 시 1→2, 2→3 이 순차 실행된다.
     */
    fun addMigration(fromVersion: Int, toVersion: Int, migrate: () -> Unit) {
        migrations.add(Migration(fromVersion, toVersion, migrate))
    }

    fun init(context: Context, dbName: String, dbVersion: Int) {
        isPassword = false
        // helper 를 먼저 할당한 뒤 open() 해야, open 중 트리거되는 onUpgrade 콜백에서
        // SQLManager 의 CRUD(createTable/executeQuery 등)를 안전하게 쓸 수 있다.
        sqlLiteHelper = SQLiteHelper(context, dbName, dbVersion, dbListener)
        sqlLiteHelper.open()
    }

    fun init(context: Context, dbName: String, dbVersion: Int, password: String) {
        isPassword = true
        sqlChiperHelper = SQLChiperHelper(context, dbName, dbVersion, password, dbListener)
        sqlChiperHelper.open()
    }

    // --- 마이그레이션 처리 --------------------------------------------------

    private fun handleUpgrade(oldVersion: Int, newVersion: Int) {
        if (migrations.isEmpty()) {
            upgradeCallback?.invoke(oldVersion, newVersion)
            return
        }
        var current = oldVersion
        while (current < newVersion) {
            val migration = migrations.firstOrNull { it.from == current && it.to <= newVersion } ?: break
            migration.migrate()
            current = migration.to
        }
        // 등록된 마이그레이션으로 newVersion 까지 못 채운 구간은 콜백에 위임
        if (current < newVersion) {
            upgradeCallback?.invoke(current, newVersion)
        }
    }

    private fun handleDowngrade(oldVersion: Int, newVersion: Int) {
        downgradeCallback?.invoke(oldVersion, newVersion)
    }

    // --- 테이블 / 커서 ------------------------------------------------------

    fun createTable(tableQueryList: ArrayList<String>) {
        try {
            if (isPassword) sqlChiperHelper.createTable(tableQueryList) else sqlLiteHelper.createTable(tableQueryList)
        } catch (e: SQLiteException) {
            e.printStackTrace()
        }
    }

    fun createTable(sql: String) {
        try {
            if (isPassword) sqlChiperHelper.create(sql) else sqlLiteHelper.create(sql)
        } catch (e: SQLiteException) {
            e.printStackTrace()
        }
    }

    fun closeCursor(cursor: Cursor?) {
        cursor?.close()
    }

    fun close() {
        // 사용 중인 helper 만 닫는다(미초기화 helper 접근으로 인한 크래시 방지)
        if (isPassword) {
            if (::sqlChiperHelper.isInitialized) sqlChiperHelper.closeHelper()
        } else {
            if (::sqlLiteHelper.isInitialized) sqlLiteHelper.closeHelper()
        }
    }

    // --- 트랜잭션 -----------------------------------------------------------

    fun beginTransaction() {
        if (isPassword) sqlChiperHelper.beginTransaction() else sqlLiteHelper.beginTransaction()
    }

    fun endTransaction() {
        if (isPassword) sqlChiperHelper.endTransaction() else sqlLiteHelper.endTransaction()
    }

    fun setTransactionSuccessful() {
        if (isPassword) sqlChiperHelper.setTransactionSuccessful() else sqlLiteHelper.setTransactionSuccessful()
    }

    fun executeQuery(query: String): Cursor =
        if (isPassword) sqlChiperHelper.executeQuery(query) else sqlLiteHelper.executeQuery(query)

    // --- Insert / Update / Delete ------------------------------------------

    /**
     * 여러 건을 한 트랜잭션으로 삽입한다.
     * setTransactionSuccessful() 을 호출해야 커밋되며, 예외 시 endTransaction 으로 롤백된다.
     */
    fun insertAll(tableName: String, list: List<Map<String, Any>>): Int {
        var count = 0
        beginTransaction()
        try {
            for (map in list) {
                insert(tableName, map)
                count++
            }
            setTransactionSuccessful()   // ← 누락 시 롤백되던 버그 수정
        } finally {
            endTransaction()
        }
        return count
    }

    fun insert(tableName: String, values: ContentValues): Long =
        if (isPassword) sqlChiperHelper.insert(tableName, values) else sqlLiteHelper.insert(tableName, values)

    fun insert(tableName: String, map: Map<String, Any>): Long =
        if (isPassword) sqlChiperHelper.insert(tableName, map.convertMapToContentValues())
        else sqlLiteHelper.insert(tableName, map.convertMapToContentValues())

    fun insertOrUpdate(tableName: String, map: Map<String, Any>): Long =
        if (isPassword) sqlChiperHelper.insertOrUpdate(tableName, map.convertMapToContentValues())
        else sqlLiteHelper.insertOrUpdate(tableName, map.convertMapToContentValues())

    fun insertAllNoTransaction(tableName: String, list: List<Map<String, Any>>): Int {
        var count = 0
        for (map in list) {
            insert(tableName, map)
            count++
        }
        return count
    }

    fun insertOrUpdateAllNoTransaction(tableName: String, list: List<Map<String, Any>>): Int {
        var count = 0
        for (map in list) {
            insertOrUpdate(tableName, map)
            count++
        }
        return count
    }

    fun update(tableName: String, map: Map<String, Any>, where: String): Long =
        if (isPassword) sqlChiperHelper.update(tableName, map.convertMapToContentValues(), where, null)
        else sqlLiteHelper.update(tableName, map.convertMapToContentValues(), where, null)

    fun delete(tableName: String, where: String): Long =
        if (isPassword) sqlChiperHelper.delete(tableName, where, null) else sqlLiteHelper.delete(tableName, where, null)
}
