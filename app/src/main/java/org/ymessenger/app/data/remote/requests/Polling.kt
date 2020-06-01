/*
 * This file is part of Y messenger.
 *
 * Y messenger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Y messenger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Y messenger.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.ymessenger.app.data.remote.requests

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import org.ymessenger.app.data.remote.WSRequest
import java.lang.reflect.Type

class Polling(
    @SerializedName("PollId")
    val pollId: String,
    @SerializedName("ConversationId")
    val conversationId: Long,
    @SerializedName("ConversationType")
    val conversationType: Int,
    @SerializedName("Options")
    val options: List<Option>
) : WSRequest(RequestType.POLLING) {

    class Option(
        @SerializedName("OptionId")
        val optionId: Int,
        @SerializedName("Sign")
        val sign: Sign?
    ) {

        class Sign(
            @SerializedName("KeyId")
            val keyId: Long,
            @SerializedName("Data")
            val data: String
        )

        class SignData(
            @SerializedName("PollId")
            val pollId: String,
            @SerializedName("UserId")
            val userId: Long,
            @SerializedName("OptionId")
            val optionId: Int
        ) {

            /**
             * Returns Json with correct order of fields
             */
            fun getJson(): String {
                val gson =
                    GsonBuilder().registerTypeAdapter(SignData::class.java, SignDataSerializer())
                        .create()
                return gson.toJson(this)
            }

            private class SignDataSerializer : JsonSerializer<SignData> {
                override fun serialize(
                    src: SignData?,
                    typeOfSrc: Type?,
                    context: JsonSerializationContext?
                ): JsonElement {
                    val obj = JsonObject()
                    obj.add("PollId", context?.serialize(src?.pollId))
                    obj.add("UserId", context?.serialize(src?.userId))
                    obj.add("OptionId", context?.serialize(src?.optionId))

                    return obj
                }
            }
        }

    }
}