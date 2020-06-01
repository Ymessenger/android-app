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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.*
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import org.ymessenger.app.AppBase
import org.ymessenger.app.R
import org.ymessenger.app.activities.ChannelActivity
import org.ymessenger.app.activities.ChatActivity
import org.ymessenger.app.activities.DialogActivity
import org.ymessenger.app.activities.MainActivity
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.local.db.AppDatabase
import org.ymessenger.app.data.local.db.models.UserModel
import org.ymessenger.app.data.remote.entities.Attachment
import org.ymessenger.app.data.remote.entities.Message
import org.ymessenger.app.data.remote.entities.Session
import org.ymessenger.app.di.Injection
import org.ymessenger.app.services.ReplyNotificationReceiver

object MyNotificationManager {

    private const val CHANNEL_ID = "CHANNEL_ID"
    private const val CHANNEL_ID_SESSIONS = "CHANNEL_ID_SESSIONS"
    private const val TAG = "MyNotificationManager"

    fun showNewMessageNotification(
        appBase: AppBase,
        remoteMessage: Message,
        disableButtons: Boolean = false
    ) {
        val context = appBase.applicationContext

        val chatPreviewDao = AppDatabase.getInstance(context).chatPreviewDao()
        val chatPreviewModel = chatPreviewDao.getChatPreviewByChatSync(
            remoteMessage.conversationId!!,
            remoteMessage.conversationType!!
        )

        val userDao = AppDatabase.getInstance(context).userDao()
        var userModel: UserModel? = null
        remoteMessage.senderId?.let {
            userModel = userDao.getUserModelSync(remoteMessage.senderId)
        }

        val conversationTitle =
            userModel?.getDisplayName() ?: chatPreviewModel?.chatPreview?.chatName
            ?: context.getString(R.string.new_message)
        val conversationPhotoUrl = chatPreviewModel?.chatPreview?.getPhotoUrl()

        val userPhotoUrl = userModel?.user?.getPhotoUrl()
        val userName = userModel?.getDisplayName() ?: context.getString(R.string.name_is_hidden)

        ///////////
        val settingsHelper = Injection.provideSettingsHelper(appBase)

        val text = if (settingsHelper.getHideNotificationContent()) {
            context.getString(R.string.new_message)
        } else {
            remoteMessage.text ?: getAttachmentType(context, remoteMessage)
        }

        // Creates intent for dialog or chat activity
        val intent: Intent = when (remoteMessage.conversationType) {
            ConversationType.DIALOG -> DialogActivity.getIntent(context, remoteMessage.senderId!!)
            ConversationType.CHAT -> ChatActivity.getIntent(context, remoteMessage.conversationId!!)
            else -> ChannelActivity.getIntent(context, remoteMessage.conversationId!!)
        }

        // open activity with back stack
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(intent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }


        val personBuilder = Person.Builder().setName(userName)

        if (remoteMessage.conversationType == ConversationType.CHANNEL) {
            personBuilder.setName(conversationTitle)
        }

        // Load user photo. Can be user or channel photo
        val personPhotoUrl = if (remoteMessage.conversationType != ConversationType.CHANNEL) {
            userPhotoUrl
        } else {
            conversationPhotoUrl
        }


        if (personPhotoUrl != null) {
            val futureTarget = Glide.with(context)
                .asBitmap()
                .load(personPhotoUrl)
                .apply(RequestOptions().circleCrop())
                .submit()

            try {
                val bitmap = futureTarget.get()
                personBuilder.setIcon(IconCompat.createWithBitmap(bitmap))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load person photo")
                e.printStackTrace()
            }

            Glide.with(context).clear(futureTarget)
        }

        val person = personBuilder.build()
        val messagingStyle = NotificationCompat.MessagingStyle(person)
            .setConversationTitle(conversationTitle)
            .addMessage(text, System.currentTimeMillis(), person)
            .setGroupConversation(remoteMessage.conversationType == ConversationType.CHAT)

        // Add reply button
        var replyLabel = context.getString(R.string.reply)
        var remoteInput = RemoteInput.Builder(ReplyNotificationReceiver.KEY_TEXT_REPLY).run {
            setLabel(replyLabel)
            build()
        }

        var replyPendingIntent = PendingIntent.getBroadcast(
            context,
            remoteMessage.conversationId!!.toInt(),
            ReplyNotificationReceiver.getIntent(context, remoteMessage),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        var replyAction =
            NotificationCompat.Action.Builder(R.drawable.ic_send, replyLabel, replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build()

        // Add mark as read button
        val readPendingIntent = PendingIntent.getBroadcast(
            context,
            remoteMessage.conversationId.toInt(),
            ReplyNotificationReceiver.getIntent(context, remoteMessage),
            0
        )

        // Building notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (!disableButtons) {
            // disable reply button for channels
            if (remoteMessage.conversationType != ConversationType.CHANNEL) {
                builder.addAction(replyAction)
            }

            builder.addAction(
                R.drawable.ic_visibility,
                context.getString(R.string.mark_as_read),
                readPendingIntent
            )
        }

        // Set conversation photo
        if (conversationPhotoUrl != null) {
            val futureTarget = Glide.with(context)
                .asBitmap()
                .load(conversationPhotoUrl)
                .apply(RequestOptions().circleCrop())
                .submit()

            try {
                val bitmap = futureTarget.get()
                builder.setLargeIcon(bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load conversation photo")
                e.printStackTrace()
            }

            Glide.with(context).clear(futureTarget)
        }

        // FIXME: it will break one day
        val id = remoteMessage.conversationId.toInt()

        with(NotificationManagerCompat.from(context)) {
            notify(id, builder.build())
        }
    }

    private fun getAttachmentType(context: Context, remoteMessage: Message): String {
        return if (remoteMessage.attachments != null) {
            if (remoteMessage.attachments.first().type == Attachment.Type.KEY_EXCHANGE_MESSAGE) {
                context.getString(R.string.interlocutor_turned_on_protected_mode)
            } else {
                val attachmentTypes = remoteMessage.attachments.map { it.type!! }
                org.ymessenger.app.data.local.db.entities.Attachment.getAttachmentsDescription(
                    context,
                    attachmentTypes
                )
            }
        } else {
            if (remoteMessage.text == null) {
                context.getString(R.string.encrypted_message)
            } else {
                context.getString(R.string.new_message)
            }
        }
    }

    fun showNewSessionNotification(context: Context, session: Session) {
        val device = session.deviceName ?: "Unknown"
        val os = session.OSName ?: "Unknown"
        val application = session.appName ?: "Unknown"

        val sb = StringBuilder()
        sb.append("${context.getString(R.string.device)}: $device\n")
        sb.append("${context.getString(R.string.os)}: $os\n")
        sb.append("${context.getString(R.string.application)}: $application")

        // Create an explicit intent for an Activity in your app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SESSIONS)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(context.getString(R.string.new_authorization))
            .setContentText(sb.toString())
            .setStyle(NotificationCompat.BigTextStyle().bigText(sb.toString()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val id = session.tokenId.toInt()

        with(NotificationManagerCompat.from(context)) {
            notify(id, builder.build())
        }
    }

    fun cancelMessageNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.messages)
            val descriptionText = context.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotificationChannelForSessions(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.sessions)
            val descriptionText = context.getString(R.string.it_informs_about_new_authorizations)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID_SESSIONS, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}