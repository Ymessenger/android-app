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

package org.ymessenger.app.data.remote.entities

import com.google.gson.annotations.SerializedName

class Key(
    @SerializedName("KeyId")
    val keyId: Long,
    @SerializedName("Data")
    val data: String,
    @SerializedName("Lifetime")
    val lifetime: Long,
    @SerializedName("GenerationTime")
    val generationTime: Long,
    @SerializedName("Version")
    val version: Int,
    @SerializedName("UserId")
    val userId: Long?,
    @SerializedName("ChatId")
    val chatId: Long?,
    @SerializedName("Type")
    val type: Int
) {
}