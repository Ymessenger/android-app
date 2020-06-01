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

package org.ymessenger.app.helpers

import android.util.Log
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.requests.GetMessagesUpdates
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.remote.responses.UpdatedMessages
import org.ymessenger.app.data.repositories.MessageRepository

class MessageUpdater(
    private val messageRepository: MessageRepository,
    private val webSocketService: WebSocketService,
    private val settingsHelper: SettingsHelper
) {

    fun getMessagesUpdates() {
        val lastUpdated = settingsHelper.getLastUpdateTime()
        val getMessagesUpdates = GetMessagesUpdates(lastUpdated)
        webSocketService.getMessagesUpdates(
            getMessagesUpdates,
            object : WebSocketService.ResponseCallback<UpdatedMessages> {
                override fun onResponse(response: UpdatedMessages) {
                    messageRepository.deleteLocalMessages(response)

                    val updatedTime = response.end ?: System.currentTimeMillis() / 1000
                    settingsHelper.setLastUpdateTime(updatedTime)

                    if (response.end != null) {
                        getMessagesUpdates()
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to get updates")
                }
            })
    }

    companion object {
        private const val TAG = "MessageUpdater"
    }

}