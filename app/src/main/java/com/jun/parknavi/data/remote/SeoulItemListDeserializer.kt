package com.jun.parknavi.data.remote

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.jun.parknavi.data.remote.dto.SeoulItemListDto
import com.jun.parknavi.data.remote.dto.SeoulStationDto
import java.lang.reflect.Type

// TAGO에서 겪은 것과 같은 유형의 문제: 결과가 1건일 때 itemList가 배열이 아니라 단일
// 객체로 오는 경우를 흡수해서 List로 정규화한다. 중첩 DTO는 앱의 설정된 Gson 파이프라인을
// 그대로 물려받도록 JsonDeserializationContext.deserialize()로 위임한다.
class SeoulItemListDeserializer : JsonDeserializer<SeoulItemListDto> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): SeoulItemListDto {
        val items: List<SeoulStationDto> = when {
            json.isJsonArray -> json.asJsonArray.map {
                context.deserialize(it, SeoulStationDto::class.java)
            }
            json.isJsonObject -> listOf(context.deserialize(json, SeoulStationDto::class.java))
            else -> emptyList()
        }
        return SeoulItemListDto(items)
    }
}
