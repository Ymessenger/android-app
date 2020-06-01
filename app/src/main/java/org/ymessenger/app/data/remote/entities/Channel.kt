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

data class Channel(
    @SerializedName("ChannelId")
    val id: Long?,
    @SerializedName("ChannelName")
    val name: String?,
    @SerializedName("About")
    val about: String?,
    @SerializedName("Tag")
    val tag: String?,
    @SerializedName("Photo")
    val photo: String?,
    @SerializedName("SubscribersCount")
    val subscribersCount: Int?,
    @SerializedName("UserRole")
    val userRole: Int?
) {
}