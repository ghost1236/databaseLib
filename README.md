# databaselib 사용 가이드

암호화(SQLCipher)/일반 SQLite 를 **하나의 API(`SQLManager`)** 로 다루는 안드로이드 라이브러리.
`init` 의 password 유무로 암호화/일반을 선택하며, 단계별 마이그레이션을 지원한다.

## 설치 (jitpack)
```gradle
// settings.gradle 또는 build.gradle
repositories { maven { url 'https://jitpack.io' } }

// app/build.gradle
dependencies {
    implementation 'com.github.ghost1236.databaseLib:databaselib:2.0.0'
}
```
> ⚠️ **Kotlin 2.1.20 / AGP 8.6 / compileSdk 35** 로 빌드됨 → 소비 앱도 **Kotlin 2.1+, compileSdk 35** 필요.
> SQLCipher(`net.zetetic:sqlcipher-android`)가 의존성으로 포함되어 **native 라이브러리까지 자동 전파**되므로, 앱이 별도로 SQLCipher 를 추가할 필요가 없습니다.
> 정확한 좌표는 jitpack 페이지(`jitpack.io/#ghost1236/databaseLib`)에서 확인.

---

## 1. 초기화 — 암호화 / 일반 선택
```kotlin
// 일반 SQLite
SQLManager.init(context, "app.db", 1)

// 암호화(SQLCipher) — password 를 주면 암호화 DB
SQLManager.init(context, "app.db", 1, "my-password")
```
`init` 안에서 DB 가 열리며, 버전이 올라가 있으면 마이그레이션 콜백이 호출된다.
> DB 오픈은 동기 작업이라, 큰 DB 면 백그라운드 스레드에서 `init` 을 호출하는 것을 권장.

## 2. 테이블 생성
```kotlin
SQLManager.createTable("CREATE TABLE IF NOT EXISTS user (id INTEGER PRIMARY KEY, name TEXT)")
// 여러 개:
SQLManager.createTable(arrayListOf(sql1, sql2))
```

## 3. 삽입 / 수정 / 삭제
```kotlin
// Map 으로 삽입 (키=컬럼명)
SQLManager.insert("user", mapOf("id" to 1, "name" to "kim"))

// 여러 건을 한 트랜잭션으로(성공 시 커밋, 실패 시 롤백)
SQLManager.insertAll("user", listOf(mapOf("id" to 1, "name" to "a"), mapOf("id" to 2, "name" to "b")))

// 있으면 교체(CONFLICT_REPLACE)
SQLManager.insertOrUpdate("user", mapOf("id" to 1, "name" to "lee"))

// 수정 / 삭제
SQLManager.update("user", mapOf("name" to "park"), "id = 1")
SQLManager.delete("user", "id = 1")
```

## 4. 조회
```kotlin
val cursor = SQLManager.executeQuery("SELECT * FROM user")
while (cursor.moveToNext()) {
    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
}
SQLManager.closeCursor(cursor)
```

## 5. 트랜잭션 (직접 제어)
```kotlin
SQLManager.beginTransaction()
try {
    // ... 여러 작업 ...
    SQLManager.setTransactionSuccessful()   // 호출해야 커밋됨
} finally {
    SQLManager.endTransaction()
}
```

## 6. 마이그레이션
DB 버전을 올리면(`init` 의 dbVersion 증가) 등록된 마이그레이션이 실행된다.
```kotlin
// init() 호출 "전에" 등록
SQLManager.addMigration(1, 2) {
    SQLManager.createTable("ALTER TABLE user ADD COLUMN age INTEGER")
}
SQLManager.addMigration(2, 3) { /* ... */ }

// 마이그레이션을 등록하지 않았거나, 못 채운 구간의 fallback
SQLManager.upgradeCallback = { old, new -> /* 직접 처리 */ }

// 다운그레이드(일반 SQLite 만 지원, 암호화 DB 는 미지원)
SQLManager.downgradeCallback = { old, new -> /* ... */ }

SQLManager.init(context, "app.db", 3)   // 등록 후 init → 1→2, 2→3 순차 실행
```

## 7. 종료
```kotlin
SQLManager.close()   // 사용 중인 helper 만 안전하게 닫음
```

---

## 참고
- 값은 `Map<String, Any>` 로 전달하며 내부적으로 `ContentValues` 로 변환됩니다(문자열 기반 저장).
- 암호화 DB 의 다운그레이드 콜백은 SQLCipher API 제약으로 지원되지 않습니다(일반 SQLite 만).
- 동일 앱에서 `init()`(일반)과 `init(password)`(암호화)를 상황에 따라 선택할 수 있습니다.
