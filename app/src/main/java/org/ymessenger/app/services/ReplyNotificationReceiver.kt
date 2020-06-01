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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import org.ymessenger.app.data.remote.entities.Message

class ReplyNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        val conversationType = intent.extras?.getInt(ARG_CONVERSATION_TYPE)
            ?: throw Exception("Conversation type is null")
        val conversationId = intent.extras?.getLong(ARG_CONVERSATION_ID)
            ?: throw Exception("Conversation id is null")
        val senderId = intent.extras?.getLong(ARG_SENDER_ID) ?: throw Exception("Sender id is null")
        val messageId =
            intent.extras?.getString(ARG_MESSAGE_ID) ?: throw Exception("Message id is null")
        val repliedText = RemoteInput.getResultsFromIntent(intent)?.getString(KEY_TEXT_REPLY)

        context!!.startService(
            ReplyNotificationIntentService.getIntent(
                context,
                conversationType,
                conversationId,
                senderId,
                messageId,
                repliedText
            )
        )
    }

    companion object {
        private const val TAG = "ReplyReceiver"
        const val KEY_TEXT_REPLY = "key_text_reply"

        private const val ARG_CONVERSATION_TYPE = "conversation_type"
        private const val ARG_CONVERSATION_ID = "conversation_id"
        private const val ARG_SENDER_ID = "sender_id"
        private const val ARG_MESSAGE_ID = "message_id"

        fun getIntent(context: Context, remoteMessage: Message): Intent {
            val intent = Intent(context, ReplyNotificationReceiver::class.java)
            intent.putExtra(ARG_CONVERSATION_TYPE, remoteMessage.conversationType)
            intent.putExtra(ARG_CONVERSATION_ID, remoteMessage.conversationId)
            intent.putExtra(ARG_SENDER_ID, remoteMessage.senderId)
            intent.putExtra(ARG_MESSAGE_ID, remoteMessage.globalId)

            return intent
        }
    }
}