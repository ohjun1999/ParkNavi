package com.jun.parknavi.data.remote

import com.google.gson.GsonBuilder
import com.jun.parknavi.data.remote.dto.SeoulItemListDto
import org.junit.Assert.assertEquals
import org.junit.Test

// 실제 Gson 파싱 경로를 거치는 테스트. BusStopRepositoryImplTest는 API 응답을 코틀린
// 객체로 직접 만들어 넣기 때문에, "itemList" 필드의 실제 JSON 형태(배열/단일 객체)가
// 뒤섞이는 문제는 여기서만 잡힌다 — TAGO API 연동 때 실기기 테스트에서 겪었던 것과
// 같은 유형의 공공데이터 흔한 함정.
class SeoulItemListDeserializerTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(SeoulItemListDto::class.java, SeoulItemListDeserializer())
        .create()

    @Test
    fun `itemList가 배열이면 그대로 파싱한다`() {
        val json = """[{"stationId":"1","stationNm":"A","gpsX":127.1,"gpsY":37.1}]"""

        val result = gson.fromJson(json, SeoulItemListDto::class.java)

        assertEquals(1, result.item.size)
        assertEquals("A", result.item[0].stationNm)
    }

    @Test
    fun `itemList가 결과 1건이라 객체 하나로 와도 리스트로 정규화한다`() {
        val json = """{"stationId":"1","stationNm":"A","gpsX":127.1,"gpsY":37.1}"""

        val result = gson.fromJson(json, SeoulItemListDto::class.java)

        assertEquals(listOf("A"), result.item.map { it.stationNm })
    }
}
