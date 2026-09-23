package com.jun.parknavi.data.remote.dto

// 서울특별시_정류소정보조회 서비스(data.go.kr 경유, ws.bus.go.kr/api/rest/stationinfo/getStaionsByPosList)
// 응답 구조. 정확한 필드명은 data.go.kr 상품 페이지에서 확인했지만, 실제 JSON 응답 형태(성공
// 코드 값, item이 배열/객체로 오는지 등)는 활용신청 승인 후 실기기로 검증해야 한다 — TAGO
// API 때도 문서만으로는 못 잡는 형태 차이가 있었다.
data class SeoulNearbyStationResponse(
    val msgHeader: SeoulMsgHeaderDto,
    val msgBody: SeoulMsgBodyDto,
)

data class SeoulMsgHeaderDto(
    val headerCd: String,
    val headerMsg: String,
)

data class SeoulMsgBodyDto(
    val itemList: SeoulItemListDto?,
)

// itemList가 결과 1건일 때 배열이 아니라 단일 객체로 올 수 있다(TAGO에서 겪은 것과 같은
// 공공데이터 흔한 함정) — SeoulItemListDeserializer(NetworkModule 참고)가 이걸 흡수해서
// 이 DTO를 쓰는 쪽은 항상 List<SeoulStationDto>만 다루면 되게 만들어둔다.
data class SeoulItemListDto(
    val item: List<SeoulStationDto> = emptyList(),
)

data class SeoulStationDto(
    val stationId: String?,
    val stationNm: String?,
    val arsId: String?,
    val gpsX: Double?, // WGS84 경도
    val gpsY: Double?, // WGS84 위도
)
