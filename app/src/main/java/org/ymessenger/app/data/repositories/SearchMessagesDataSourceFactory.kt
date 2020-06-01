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
import org.ymessenger.app.data.mappers.MessageMapper
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.models.FoundMessage
import org.ymessenger.app.utils.AppExecutors

class SearchMessagesDataSourceFactory(
    private val query: String,
    private val conversationId: Long?,
    private val conversationType: Int?,
    private val webSocketService: WebSocketService,
    private val chatPreviewRepository: ChatPreviewRepository,
    private val executors: AppExecutors,
    private val messageMapper: MessageMapper
) : DataSource.Factory<SearchMessagesDataSource.SearchKey, FoundMessage>() {
    val sourceLiveData = MutableLiveData<SearchMessagesDataSource>()

    override fun create(): DataSource<SearchMessagesDataSource.SearchKey, FoundMessage> {
        val source = SearchMessagesDataSource(
            query,
            conversationId,
            conversationType,
            webSocketService,
            chatPreviewRepository,
            executors,
            messageMapper
        )
        sourceLiveData.postValue(source)
        return source
    }
}