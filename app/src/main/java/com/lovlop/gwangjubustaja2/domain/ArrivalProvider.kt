package com.lovlop.gwangjubustaja2.domain

// 버스 도착 정보 데이터 모델
data class BusArrival(
    val routeName: String,
    val minutes: Int,
    val isCorrected: Boolean = false,
    val correctionDelta: Int = 0
)

// 응답 결과 분류 (성공, 빈값, 에러)
sealed class ArrivalResult {
    data class Success(val arrivals: List<BusArrival>) : ArrivalResult()
    object Empty : ArrivalResult()
    object Error : ArrivalResult()
}

// 인터페이스 정의
interface ArrivalProvider {
    suspend fun getArrivals(stationId: String, routeName: String? = null): ArrivalResult
}