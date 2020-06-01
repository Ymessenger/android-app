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

package org.ymessenger.app.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.ymessenger.app.AppBase
import org.ymessenger.app.data.remote.WSNotice
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.Attachment
import org.ymessenger.app.data.remote.entities.Message
import org.ymessenger.app.data.remote.notices.NewMessage
import org.ymessenger.app.data.remote.notices.NewSession
import org.ymessenger.app.di.Injection
import org.ymessenger.app.helpers.MyNotificationManager

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        MyNotificationManager.createNotificationChannel(this)
        MyNotificationManager.createNotificationChannelForSessions(this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New token: $token")
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        Log.d(TAG, "New message received: $data")

        val content = data["content"]

        if (content.isNullOrBlank()) {
            Log.e(TAG, "Content is empty")
            return
        }

        Log.d(TAG, "Content: $content")

        val gson = GsonBuilder()
            .registerTypeAdapter(
                Attachment::class.java,
                WebSocketService.AttachmentsDeserializer()
            )
            .create()

        try {
            val notice = Gson().fromJson(content, WSNotice::class.java)
            Log.d(TAG, notice.getNoticeTypeName())
            when (notice.code) {
                WSNotice.NoticeCode.NEW_MESSAGE -> {
                    val newMessage = gson.fromJson(content, NewMessage::class.java)

                    if (newMessage == null) {
                        Log.e(TAG, "Failed to parse new message")
                        return
                    }

                    val message = newMessage.message
                    val call = newMessage.call

                    if (message.text == null && message.attachments.isNullOrEmpty()) {
                        Log.d(TAG, "This is encrypted message without content, don't save it")
                    } else {
                        saveMessageToDatabase(message)
                    }

                    val chatPreviewRepository =
                        Injection.provideChatPreviewRepository(application as AppBase)
                    val chatPreviewModel = chatPreviewRepository.getChatPreviewByChatSync(
                        message.conversationId!!,
                        message.conversationType!!
                    )

                    if (chatPreviewModel?.isMuted() == false || call) {
                        MyNotificationManager.showNewMessageNotification(
                            application as AppBase,
                            message,
                            disableButtons = true
                        )
                    }
                }

                WSNotice.NoticeCode.NEW_SESSION -> {
                    val newSession = gson.fromJson(content, NewSession::class.java)
                    val session = newSession.session

                    MyNotificationManager.showNewSessionNotification(this, session)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process push notification")
            e.printStackTrace()
        }
    }

    private fun saveMessageToDatabase(message: Message) {
        val messageRepository = Injection.provideMessageRepository(application as AppBase)
        messageRepository.saveRemoteMessages(listOf(message))
    }

    companion object {
        private const val TAG = "MyFirebaseService"
    }
}