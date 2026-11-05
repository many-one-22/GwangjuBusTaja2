package com.lovlop.gwangjubustaja2.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// 실행: Android Studio에서 이 파일을 열고 클래스 이름 옆 초록 ▶ → Run (기기나 에뮬레이터 필요)
// 테스트 데이터의 정류소·노선 이름은 가짜다. 실제 광주 정류소가 아니다.
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var db: AppDatabase

    @Before
    fun openDb() {
        // 메모리 DB: 파일을 안 만들고 테스트가 끝나면 사라진다. 진짜 앱 데이터를 건드리지 않는다.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDb() = db.close()

    @Test
    fun insertAndSearchStations() = runBlocking {
        db.stationDao().insertAll(
            listOf(Station("S1", "테스트역 앞"), Station("S2", "테스트역 건너편"), Station("S3", "다른 정류소"))
        )
        assertEquals(3, db.stationDao().count())
        // "테스트역"이 이름에 들어간 정류소는 2개
        assertEquals(listOf("S2", "S1"), db.stationDao().searchByName("테스트역").map { it.stationId })
        assertEquals(0, db.stationDao().searchByName("").size) // 빈 검색어는 전체가 아니라 0개
    }

    @Test
    fun sameIdIsReplacedNotDuplicated() = runBlocking {
        db.stationDao().insertAll(listOf(Station("S1", "옛 이름")))
        db.stationDao().insertAll(listOf(Station("S1", "새 이름")))
        assertEquals(1, db.stationDao().count())
        assertEquals("새 이름", db.stationDao().searchByName("이름").single().name)
    }

    @Test
    fun loaderFetchesOnlyWhenEmpty() = runBlocking {
        var fetchCalls = 0
        val fakeFetch: suspend () -> List<Station> = { fetchCalls++; listOf(Station("S1", "가짜 정류소 1"), Station("S2", "가짜 정류소 2")) }

        assertEquals(2, loadStationsIfEmpty(db.stationDao(), fakeFetch)) // 첫 실행: 비어 있으니 받아서 2개 넣음
        assertEquals(0, loadStationsIfEmpty(db.stationDao(), fakeFetch)) // 두 번째: 이미 있으니 건너뜀
        assertEquals(1, fetchCalls)                                       // 받아 오기는 한 번만 일어났다
        assertEquals(2, db.stationDao().count())
    }

    @Test
    fun findRouteByNumber() = runBlocking {
        db.busRouteDao().insertAll(listOf(BusRoute("R1", "100"), BusRoute("R2", "200")))
        assertEquals("R2", db.busRouteDao().findByNo("200").single().routeId)
        assertEquals(0, db.busRouteDao().findByNo("999").size)
    }
}
