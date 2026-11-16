package com.lovlop.gwangjubustaja2.domain

import com.lovlop.gwangjubustaja2.data.model.Arrival
import kotlinx.coroutines.delay

class FakeArrivalProvider : ArrivalProvider {
    override suspend fun getArrivals(stationId: String, routeName: String?): ArrivalResult {
        return when (stationId.lowercase().trim()) {
            // 1. 5초 지연 (로딩 UI 확인용)
            "slow" -> {
                delay(5000)
                ArrivalResult.Success(
                    listOf(
                        Arrival(routeId = "152", routeName = "지원152", routeType = 2, minutes = 3, rawMinutes = 1, isCorrected = true),
                        Arrival(routeId = "09", routeName = "첨단09", routeType = 1, minutes = 7, rawMinutes = 7, isCorrected = false)
                    )
                )
            }
            // 2. 도착 예정 버스 없음
            "empty" -> ArrivalResult.Empty
            // 3. 에러 발생
            "error" -> ArrivalResult.Error()
            // 4. 일반 정류소 입력시 정상 데이터 반환
            else -> {
                delay(1000)
                ArrivalResult.Success(
                    listOf(
                        Arrival(routeId = "03", routeName = "수완03", routeType = 1, minutes = 2, rawMinutes = 2, isCorrected = false),
                        Arrival(routeId = "19", routeName = "송정19", routeType = 2, minutes = 5, rawMinutes = 5, isCorrected = false),
                        Arrival(routeId = "18", routeName = "문흥18", routeType = 2, minutes = 11, rawMinutes = 8, isCorrected = true)
                    )
                )
            }
        }
    }
}