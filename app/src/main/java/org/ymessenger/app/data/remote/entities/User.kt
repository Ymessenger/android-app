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

data class User(
    @SerializedName("Id")
    val id: Long?,
    @SerializedName("NameFirst")
    val nameFirst: String?,
    @SerializedName("NameSecond")
    val nameSecond: String?,
    @SerializedName("About")
    val about: String?,
    @SerializedName("Photo")
    val photo: String?,
    @SerializedName("Country")
    val country: String?,
    @SerializedName("City")
    val city: String?,
    @SerializedName("Phones")
    val phones: List<UserPhone>?,
    @SerializedName("Emails")
    val emails: List<String>?,
    @SerializedName("Online")
    val online: Long?,
    @SerializedName("Tag")
    val tag: String?,
    @SerializedName("Privacy")
    val privacy: BooleanArray?,
    @SerializedName("Contact")
    val contact: Contact?,
    @SerializedName("Groups")
    val groups: List<Group>?,
    @SerializedName("Confirmed")
    val confirmed: Boolean,
    @SerializedName("Banned")
    val banned: Boolean,
    @SerializedName("SyncContacts")
    val syncContacts: Boolean
) {
}