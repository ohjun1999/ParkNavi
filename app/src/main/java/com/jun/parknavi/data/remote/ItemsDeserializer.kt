package com.jun.parknavi.data.remote

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.jun.parknavi.data.remote.dto.BusStopDto
import com.jun.parknavi.data.remote.dto.ItemsDto
import java.lang.reflect.Type

// TAGO API가 결과 0건/1건일 때 "item" 필드가 배열이 아니라 객체(또는 빈 문자열)로 오는
// 공공데이터포털 특유의 응답 형태를 List로 정규화한다.
class ItemsDeserializer : JsonDeserializer<ItemsDto> {
    private val gson = Gson()

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): ItemsDto {
        val itemElement = json.asJsonObject.get("item") ?: return ItemsDto(emptyList())
        val items: List<BusStopDto> = when {
            itemElement.isJsonArray -> itemElement.asJsonArray.map {
                gson.fromJson(it, BusStopDto::class.java)
            }
            itemElement.isJsonObject -> listOf(gson.fromJson(itemElement, BusStopDto::class.java))
            else -> emptyList()
        }
        return ItemsDto(items)
    }
}
