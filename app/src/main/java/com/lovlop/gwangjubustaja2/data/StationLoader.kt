package com.lovlop.gwangjubustaja2.data

// 정류소 목록을 DB에 적재하는 흐름. (노선은 /routes 응답을 받으면 같은 모양으로 추가)
// fetch = "목록을 받아 오는 함수"를 바깥에서 넘겨받는다.
//   지금: 테스트에서 가짜 목록을 넘긴다.
//   API 응답 샘플을 받은 뒤: 실제 API(또는 B의 Worker)를 부르는 함수를 넘긴다. 이 파일은 안 바뀐다.
// 네트워크 오류는 여기서 잡지 않고 부른 쪽으로 올려 보낸다. 실패하면 DB가 빈 채로 남아 다음 실행 때 다시 시도된다.

/**
 * DB에 정류소가 하나도 없을 때만 받아서 넣는다. 앱을 켤 때마다 전체 목록을 다시 받지 않으려고.
 * @return 이번에 넣은 개수. 이미 있어서 건너뛰면 0.
 */
suspend fun loadStationsIfEmpty(dao: StationDao, fetch: suspend () -> List<Station>): Int {
    if (dao.count() > 0) return 0
    // shortcut: 한 번 넣으면 다시 안 받는다. 정류소가 새로 생겨도 반영 안 됨 → 필요해지면 "마지막 적재 날짜" 기준으로 갱신.
    val stations = fetch()
    dao.insertAll(stations)
    return stations.size
}
