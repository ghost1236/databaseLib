package net.common.databaselib

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import net.common.databaselib.DBUtils.convertMapToContentValues

object SQLManager {

    private lateinit var sqlLiteHelper: SQLiteHelper
    private lateinit var sqlChiperHelper: SQLChiperHelper
    private var isPassword: Boolean = false

    var upgradeCallback: ((Int,Int)->Unit)? = null

    fun init(context: Context, dbName: String, dbVersion: Int) {
        isPassword = false
        sqlLiteHelper = SQLiteHelper(context, dbName, dbVersion, object : Listener.OnDatabaseListener {
            override fun onUpgrade(oldVersion: Int, newVersion: Int) {
                upgradeCallback?.invoke(oldVersion, newVersion)
            }
        })
    }

    fun init(context: Context, dbName: String, dbVersion: Int, password: String) {
        isPassword = true
        sqlChiperHelper = SQLChiperHelper(context, dbName, dbVersion, password, object : Listener.OnDatabaseListener {
            override fun onUpgrade(oldVersion: Int, newVersion: Int) {
                upgradeCallback?.invoke(oldVersion, newVersion)
            }
        })
    }

    fun createTable(tableQueryList: ArrayList<String>) {
        try {
            if (isPassword) sqlChiperHelper.createTable(tableQueryList) else sqlLiteHelper.crateTable(tableQueryList)
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
        cursor?.let {
            cursor.close()
        }
    }

    fun close() {
        sqlChiperHelper.let {
            sqlChiperHelper.closeHelper()
        }

        sqlLiteHelper.let {
            sqlLiteHelper.closeHelper()
        }
    }

    fun beginTransaction() {
        if (isPassword) sqlChiperHelper.beginTransaction() else sqlLiteHelper.beginTransaction()
    }

    fun endTransaction() {
        if (isPassword) sqlChiperHelper.endTransaction() else sqlLiteHelper.endTransaction()
    }

    fun setTransactionSuccessful() {
        if(isPassword) sqlChiperHelper.setTransactionSuccessful() else sqlLiteHelper.setTransactionSuccessful()
    }

    fun executeQuery(query: String) : Cursor {
        return if (isPassword)  sqlChiperHelper.executeQuery(query) else sqlLiteHelper.executeQuery(query)
    }

    fun insertAll(tableName: String, list: List<Map<String, Any>>) : Int {
        var count = 0
        beginTransaction()
        for (map in list) {
            insert(tableName, map)
            count++
        }
        endTransaction()
        return count
    }

    fun insert(tableName: String, values: ContentValues) : Long {
        return if (isPassword) sqlChiperHelper.insert(tableName, values) else sqlLiteHelper.insert(tableName, values)
    }

    fun insert(tableName: String, map: Map<String, Any>) : Long {
        return if (isPassword) sqlChiperHelper.insert(tableName, map.convertMapToContentValues()) else sqlLiteHelper.insert(tableName, map.convertMapToContentValues())
    }

    fun insertOrUpdate(tableName: String, map: Map<String, Any>) : Long {
        return if (isPassword) sqlChiperHelper.insertOrUpdate(tableName, map.convertMapToContentValues()) else sqlLiteHelper.insertOrUpdate(tableName, map.convertMapToContentValues())
    }

    fun insertAllNoTransaction(tableName: String, list: List<Map<String, Any>>) : Int {
        var count = 0
        for (map in list) {
            insert(tableName, map)
            count++
        }
        return count
    }

    fun insertOrUpdateAllNoTransaction(tableName: String, list: List<Map<String, Any>>) : Int {
        var count = 0
        for (map in list) {
            insertOrUpdate(tableName, map)
            count++
        }
        return count
    }

    fun update(tableName: String, map: Map<String, Any>, where: String) : Long {
        return if (isPassword) sqlChiperHelper.update(tableName, map.convertMapToContentValues(), where, null) else sqlLiteHelper.update(tableName, map.convertMapToContentValues(), where, null)
    }

    fun delete(tableName: String, where: String) : Long {
        return if (isPassword) sqlChiperHelper.delte(tableName, where, null) else sqlLiteHelper.delete(tableName, where, null)
    }

}