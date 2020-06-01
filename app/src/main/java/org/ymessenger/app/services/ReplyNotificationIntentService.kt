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

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log
import org.ymessenger.app.AppBase
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.remote.entities.Message
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.MessageRepository
import org.ymessenger.app.di.Injection
import org.ymessenger.app.helpers.MyNotificationManager

class ReplyNotificationIntentService : IntentService("ReplyNotificationIntentService") {

    private lateinit var appBase: AppBase

    override fun onHandleIntent(intent: Intent?) {
        Log.d(TAG, "onHandleIntent")
        if (intent == null) return
        appBase = application as AppBase

        val conversationType = intent.extras?.getInt(ARG_CONVERSATION_TYPE)
            ?: throw Exception("Conversation type is null")
        val conversationId = intent.extras?.getLong(ARG_CONVERSATION_ID)
            ?: throw Exception("Conversation id is null")
        val senderId = intent.extras?.getLong(ARG_SENDER_ID) ?: throw Exception("Sender id is null")
        val messageId =
            intent.extras?.getString(ARG_MESSAGE_ID) ?: throw Exception("Message id is null")
        val repliedText =
            if (intent.hasExtra(ARG_REPLY_TEXT)) intent.getStringExtra(ARG_REPLY_TEXT) else null

        val messageRepository = Injection.provideMessageRepository(appBase)
        readMessage(messageRepository, conversationType, conversationId, messageId)
        repliedText?.let {
            sendMessage(messageRepository, conversationType, conversationId, senderId, it)
        }
    }

    private fun readMessage(
        messageRepository: MessageRepository,
        conversationType: Int,
        conversationId: Long,
        messageId: String
    ) {
        messageRepository.readMessage(listOf(messageId), conversationType, conversationId) {
            MyNotificationManager.cancelMessageNotification(
                applicationContext,
                conversationId.toInt()
            )
        }
    }

    private fun sendMessage(
        messageRepository: MessageRepository,
        conversationType: Int,
        conversationId: Long,
        senderId: Long,
        text: String
    ) {
        val identifier = if (conversationType == ConversationType.CHAT) conversationId else senderId
        val message = Message.Builder(conversationType, identifier).setText(text).build()

        messageRepository.sendMessage(message, object : MessageRepository.SendMessageCallback {
            override fun sent(message: Message) {
                Log.d(TAG, "Message sent successfully")
                MyNotificationManager.cancelMessageNotification(
                    applicationContext,
                    conversationId.toInt()
                )
            }

            override fun error(error: ResultResponse) {
                Log.e(TAG, "Failed to send message")
            }
        })
    }

    companion object {
        private const val TAG = "ReplyActionService"

        private const val ARG_CONVERSATION_TYPE = "conversation_type"
        private const val ARG_CONVERSATION_ID = "conversation_id"
        private const val ARG_SENDER_ID = "sender_id"
        private const val ARG_MESSAGE_ID = "message_id"
        private const val ARG_REPLY_TEXT = "reply_text"

        fun getIntent(
            context: Context,
            conversationType: Int,
            conversationId: Long,
            senderId: Long,
            messageId: String,
            replyText: String?
        ): Intent {
            val intent = Intent(context, ReplyNotificationIntentService::class.java)
            intent.putExtra(ARG_CONVERSATION_TYPE, conversationType)
            intent.putExtra(ARG_CONVERSATION_ID, conversationId)
            intent.putExtra(ARG_SENDER_ID, senderId)
            intent.putExtra(ARG_MESSAGE_ID, messageId)
            replyText?.let {
                intent.putExtra(ARG_REPLY_TEXT, it)
            }

            return intent
        }
    }
}