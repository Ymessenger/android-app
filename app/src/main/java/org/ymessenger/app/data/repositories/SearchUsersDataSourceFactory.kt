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

package org.ymessenger.app.data.repositories

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.data.mappers.UserMapper
import org.ymessenger.app.data.remote.WebSocketService

class SearchUsersDataSourceFactory(
    private val searchQuery: String,
    private val webSocketService: WebSocketService,
    private val userMapper: UserMapper
) : DataSource.Factory<Long, User>() {
    val sourceLiveData = MutableLiveData<SearchUserDataSource>()

    override fun create(): DataSource<Long, User> {
        val source = SearchUserDataSource(searchQuery, webSocketService, userMapper)
        sourceLiveData.postValue(source)
        return source
    }
}