package com.lovlop.gwangjubustaja2.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

// @Dao = 테이블에 대한 질문(쿼리) 목록. 함수 몸통은 Room이 빌드 때 만들어 준다.
// @Query의 SQL은 빌드 때 검사된다: 테이블·열 이름을 틀리면 실행 전에 빌드가 실패한다.
// suspend = 시간이 걸리는 DB 작업이라 화면(메인 스레드)을 멈추지 않도록 코루틴에서 호출.

@Dao
interface StationDao {
    /** 2주차 API 적재용. 같은 stationId가 이미 있으면 새 값으로 덮어쓴다(다시 받아도 중복 안 생김). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stations: List<Station>)

    /** 이름 일부로 검색. 예: "상무" → 이름에 "상무"가 들어간 정류소 전부. A의 개체 추출이 쓸 함수. */
    // :keyword != '' : 빈 검색어면 LIKE '%%'가 되어 전체가 나오므로, 빈 결과를 돌려준다.
    @Query("SELECT * FROM station WHERE :keyword != '' AND name LIKE '%' || :keyword || '%' ORDER BY name")
    suspend fun searchByName(keyword: String): List<Station>

    @Query("SELECT COUNT(*) FROM station")
    suspend fun count(): Int
}

@Dao
interface BusRouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routes: List<BusRoute>)

    /** 노선 번호로 정확히 찾기. 사용자가 노선 번호를 말했을 때 A가 쓸 함수. */
    @Query("SELECT * FROM bus_route WHERE routeNo = :routeNo")
    suspend fun findByNo(routeNo: String): List<BusRoute>

    @Query("SELECT COUNT(*) FROM bus_route")
    suspend fun count(): Int
}

// version = 테이블 구조 버전. 엔티티에 필드를 추가하면 1 올려야 한다(2주차).
// exportSchema = false: 스키마 기록 파일은 지금 필요 없다.
@Database(entities = [Station::class, BusRoute::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun busRouteDao(): BusRouteDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** 앱 전체에서 DB를 하나만 연다. 여러 개 열면 메모리 낭비이고 데이터가 엇갈릴 수 있다. */
        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "gwangju_bus.db")
                    // shortcut: 구조가 바뀌면 기존 데이터를 지우고 새로 만든다. 데이터가 API에서 다시 받는 것뿐이라 괜찮다. 사용자 데이터(즐겨찾기 등)가 생기면 Migration으로 바꿀 것.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
