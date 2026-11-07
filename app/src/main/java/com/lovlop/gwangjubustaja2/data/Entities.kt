package com.lovlop.gwangjubustaja2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// @Entity = DB 테이블 하나. 클래스의 각 필드가 테이블의 열(column)이 된다.
//
// 필드는 일부러 최소만 둔다(ID + 이름). 좌표·정류소 번호 같은 나머지 필드는
// 2주차에 실제 API 응답을 보고 그 응답에 있는 값만 추가한다. 지금 추측으로 만들지 않는다.
// 여기 필드 이름은 우리가 정한 이름이고, API 응답의 필드 이름과 같을 필요는 없다(2주차에 변환).

/** 정류소. 같은 이름이 도로 양쪽에 있을 수 있어서 name이 아니라 stationId로 구분한다. */
@Entity(tableName = "station")
data class Station(
    // String인 이유: API가 주는 ID에 문자나 앞자리 0이 섞여 있어도 그대로 보존하려고.
    @PrimaryKey val stationId: String,
    val name: String,
)

/** 버스 노선. routeNo는 화면·챗봇에 보이는 노선 번호, routeId는 API가 쓰는 내부 ID. */
@Entity(tableName = "bus_route")
data class BusRoute(
    @PrimaryKey val routeId: String,
    // String인 이유: 노선 번호가 순수 숫자라는 보장이 없다. 숫자로 바꾸면 그런 노선이 깨진다.
    val routeNo: String,
)
