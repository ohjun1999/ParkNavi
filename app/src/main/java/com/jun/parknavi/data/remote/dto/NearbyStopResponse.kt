package com.jun.parknavi.data.remote.dto

data class NearbyStopResponse(
    val response: ResponseWrapperDto,
)

data class ResponseWrapperDto(
    val header: ResponseHeaderDto,
    val body: ResponseBodyDto,
)

data class ResponseHeaderDto(
    val resultCode: String,
    val resultMsg: String,
)

data class ResponseBodyDto(
    val items: ItemsDto?,
    val totalCount: Int = 0,
)

// TAGO(공공데이터포털) API는 결과가 1건일 때 item이 배열이 아니라 단일 객체로 내려온다.
// 이 흔한 함정은 ItemsDeserializer(NetworkModule 참고)에서 흡수해서, 이 DTO를 쓰는 쪽은
// 항상 List<BusStopDto>만 다루면 되게 만들어둔다.
data class ItemsDto(
    val item: List<BusStopDto> = emptyList(),
)

// Gson은 리플렉션으로 값을 채우기 때문에 Kotlin의 non-null 선언을 강제하지 않는다 —
// 필드가 누락된 레코드가 오면 이 non-null 타입에도 조용히 null이 들어간다. 그래서 여기서는
// 정직하게 nullable로 선언하고, BusStopRepositoryImpl.toDomain()에서 누락된 레코드를
// 걸러낸다(전체 목록을 깨뜨리지 않고 그 레코드만 건너뛴다).
data class BusStopDto(
    val nodeid: String?,
    val nodenm: String?,
    val gpslati: Double?,
    val gpslong: Double?,
)
