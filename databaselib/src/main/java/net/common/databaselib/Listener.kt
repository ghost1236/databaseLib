package net.common.databaselib

class Listener {

    interface OnDatabaseListener {
        fun onUpgrade(oldVersion: Int, newVersion: Int)

        /** DB 버전 다운그레이드 시. 기본은 no-op(미설정 시 크래시 방지) */
        fun onDowngrade(oldVersion: Int, newVersion: Int) {}
    }
}
