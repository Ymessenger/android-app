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

class EditUser {

    @SerializedName("NameFirst")
    var nameFirst: String? = null

    @SerializedName("NameSecond")
    var nameSecond: String? = null

    @SerializedName("About")
    var about: String? = null

    @SerializedName("Photo")
    var photo: String? = null

    @SerializedName("Country")
    var country: String? = null

    @SerializedName("City")
    var city: String? = null

    @SerializedName("Birthday")
    var birthday: Long? = null

    @SerializedName("Language")
    var language: String? = null

    @SerializedName("Phones")
    var phones: List<UserPhone>? = null

    @SerializedName("Emails")
    var emails: List<String>? = null

    @SerializedName("Privacy")
    var privacy: BooleanArray? = null

    @SerializedName("SyncContacts")
    var syncContacts: Boolean? = null

    // TODO: add security
}