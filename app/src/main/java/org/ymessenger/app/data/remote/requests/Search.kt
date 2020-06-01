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

import com.google.gson.annotations.SerializedName
import org.ymessenger.app.data.remote.WSRequest

class Search(
    @SerializedName("SearchQuery")
    val searchQuery: String,
    @SerializedName("NavigationId")
    val navigationId: Long? = null,
    @SerializedName("Direction")
    val direction: Boolean = true,
    @SerializedName("SearchTypes")
    val searchTypes: List<Int> = listOf()
) : WSRequest(RequestType.SEARCH) {

    companion object {
        const val USERS = 0
        const val CHATS = 1
        const val CHANNELS = 2
    }
}