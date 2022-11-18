package io.snabble.sdk.dynamicview.data.dto.serializer

import com.google.gson.JsonParseException
import io.snabble.sdk.dynamicview.data.dto.PaddingDto
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object PaddingValueListSerializer : KSerializer<PaddingDto> {

    override val descriptor: SerialDescriptor = ListSerializer(Int.serializer()).descriptor

    override fun serialize(encoder: Encoder, value: PaddingDto) {
        val values = listOf(value.start, value.top, value.bottom, value.end)
        encoder.encodeSerializableValue(ListSerializer(Int.serializer()), values)
    }

    override fun deserialize(decoder: Decoder): PaddingDto {
        val values = decoder
            .decodeSerializableValue(ListSerializer(Int.serializer()))
            .dissolve()
        return PaddingDto(start = values[0], top = values[1], end = values[3], bottom = values[2])
    }
}

private fun List<Int>.dissolve(): List<Int> = when (size) {
    4 -> this
    2 -> listOf(this[0], this[1], this[1], this[0])
    1 -> List(size = 4) { this.first() }
    else -> throw JsonParseException("Wrong number of padding values: $size")
}
