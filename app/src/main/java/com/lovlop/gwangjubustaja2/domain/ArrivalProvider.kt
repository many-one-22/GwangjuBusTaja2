package com.lovlop.gwangjubustaja2.domain

import com.lovlop.gwangjubustaja2.data.model.Arrival

// 응답 결과 분류 (성공, 빈값, 에러)
sealed class ArrivalResult {
    data class Success(val arrivals: List<Arrival>) : ArrivalResult()
    object Empty : ArrivalResult()
    data class Error(
        val reason: Reason = Reason.UNKNOWN,
        val message: String? = null
    ) : ArrivalResult() {
        enum class Reason {
            NETWORK,
            STATION_NOT_FOUND,
            SERVER,
            QUOTA_EXCEEDED,
            UNKNOWN
        }
    }
}

// 인터페이스 정의
interface ArrivalProvider {
    suspend fun getArrivals(stationId: String, routeName: String? = null): ArrivalResult
}