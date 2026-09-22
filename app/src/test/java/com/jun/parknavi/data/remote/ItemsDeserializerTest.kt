package com.jun.parknavi.data.remote

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.jun.parknavi.data.remote.dto.ItemsDto
import org.junit.Assert.assertEquals
import org.junit.Test

// 실제 Gson 파싱 경로를 거치는 테스트. BusStopRepositoryImplTest는 API 응답을 코틀린
// 객체로 직접 만들어 넣기 때문에, "item" 필드의 실제 JSON 형태(배열/객체/빈 문자열)가
// 뒤섞이는 문제는 여기서만 잡힌다 — 실제로 이 케이스 때문에 실기기 테스트에서
// "Not a JSON Object: \"\"" 크래시가 났었다.
class ItemsDeserializerTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(ItemsDto::class.java, ItemsDeserializer())
        .create()

    @Test
    fun `item이 배열이면 그대로 파싱한다`() {
        val json = """{"item":[{"nodeid":"1","nodenm":"A","gpslati":37.1,"gpslong":127.1}]}"""

        val result = gson.fromJson(json, ItemsDto::class.java)

        assertEquals(1, result.item.size)
        assertEquals("A", result.item[0].nodenm)
    }

    @Test
    fun `item이 결과 1건이라 객체 하나로 와도 리스트로 정규화한다`() {
        val json = """{"item":{"nodeid":"1","nodenm":"A","gpslati":37.1,"gpslong":127.1}}"""

        val result = gson.fromJson(json, ItemsDto::class.java)

        assertEquals(listOf("A"), result.item.map { it.nodenm })
    }

    @Test
    fun `items 필드 자체가 빈 문자열이어도 빈 리스트를 돌려준다`() {
        val json = JsonParser.parseString(""" "" """)

        val result = gson.fromJson<ItemsDto>(json, ItemsDto::class.java)

        assertEquals(emptyList<Any>(), result.item)
    }
}
