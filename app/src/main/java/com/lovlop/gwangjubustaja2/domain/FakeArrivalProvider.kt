package com.lovlop.gwangjubustaja2.domain

import kotlinx.coroutines.delay

class FakeArrivalProvider : ArrivalProvider {
    override suspend fun getArrivals(stationId: String, routeName: String?): ArrivalResult {
        return when (stationId.lowercase().trim()) {
            // 1. 5초 지연 (로딩 UI 확인용)
            "slow" -> {
                delay(5000)
                ArrivalResult.Success(
                    listOf(
                        BusArrival("지원152", 3, isCorrected = true, correctionDelta = 2),
                        BusArrival("첨단09", 7)
                    )
                )
            }
            // 2. 도착 예정 버스 없음
            "empty" -> ArrivalResult.Empty
            // 3. 에러 발생
            "error" -> ArrivalResult.Error
            // 4. 일반 정류소 입력시 정상 데이터 반환
            else -> {
                delay(1000)
                ArrivalResult.Success(
                    listOf(
                        BusArrival("수완03", 2),
                        BusArrival("송정19", 5),
                        BusArrival("문흥18", 11, isCorrected = true, correctionDelta = 3)
                    )
                )
            }
        }
    }
}