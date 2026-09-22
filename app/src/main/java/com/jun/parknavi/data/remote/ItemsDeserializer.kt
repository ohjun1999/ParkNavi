package com.jun.parknavi.data.remote

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.jun.parknavi.data.remote.dto.BusStopDto
import com.jun.parknavi.data.remote.dto.ItemsDto
import java.lang.reflect.Type

// TAGO API가 결과 0건/1건일 때 "item" 필드가 배열이 아니라 객체로 오는 건 물론,
// items 필드 자체가 (0건일 때) 객체가 아니라 빈 문자열 ""로 오는 경우도 있다.
// 이런 공공데이터포털 특유의 응답 형태 흔들림을 전부 흡수해서 List로 정규화한다.
//
// 중첩된 BusStopDto는 독자적인 Gson()을 새로 만들어 파싱하지 않고, Gson이 넘겨주는
// JsonDeserializationContext.deserialize()로 위임한다 — 이래야 NetworkModule.provideGson()에
// 등록된(지금은 이 어댑터뿐이지만 나중에 늘어날 수 있는) 설정을 그대로 물려받는다.
class ItemsDeserializer : JsonDeserializer<ItemsDto> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): ItemsDto {
        if (!json.isJsonObject) return ItemsDto(emptyList())

        val itemElement = json.asJsonObject.get("item") ?: return ItemsDto(emptyList())
        val items: List<BusStopDto> = when {
            itemElement.isJsonArray -> itemElement.asJsonArray.map {
                context.deserialize(it, BusStopDto::class.java)
            }
            itemElement.isJsonObject -> listOf(context.deserialize(itemElement, BusStopDto::class.java))
            else -> emptyList()
        }
        return ItemsDto(items)
    }
}
