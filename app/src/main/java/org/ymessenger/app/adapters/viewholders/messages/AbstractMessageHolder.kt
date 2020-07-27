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

package org.ymessenger.app.adapters.viewholders.messages

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.LayerDrawable
import android.media.AudioManager
import android.text.TextUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import kotlinx.android.synthetic.main.item_poll_option_vote.view.*
import kotlinx.android.synthetic.main.message_body_layout.view.*
import kotlinx.android.synthetic.main.message_file.view.*
import kotlinx.android.synthetic.main.message_file.view.ivImage
import kotlinx.android.synthetic.main.message_file.view.pbDownloading
import kotlinx.android.synthetic.main.message_file.view.tvFileSize
import kotlinx.android.synthetic.main.message_files_container.view.*
import kotlinx.android.synthetic.main.message_forwarded_header.view.*
import kotlinx.android.synthetic.main.message_images_container.view.*
import kotlinx.android.synthetic.main.message_poll.view.*
import kotlinx.android.synthetic.main.message_reply_header.view.*
import kotlinx.android.synthetic.main.message_reply_header.view.progressBar
import kotlinx.android.synthetic.main.message_system_text.view.*
import kotlinx.android.synthetic.main.message_text.view.*
import kotlinx.android.synthetic.main.message_voice.view.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.MessagesPagedAdapter
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.EncryptedMessageType
import org.ymessenger.app.data.local.db.models.MessageModel
import org.ymessenger.app.data.remote.UrlGenerator
import org.ymessenger.app.utils.DateUtils
import org.ymessenger.app.viewmodels.DialogViewModel
import java.io.File
import java.util.*
import kotlin.math.roundToInt

abstract class AbstractMessageHolder(
    itemView: View,
    protected val glide: RequestManager,
    private val itemClickListeners: MessagesPagedAdapter.ItemClickListeners,
    private val encryptedMessageCallbacks: MessagesPagedAdapter.EncryptedMessageCallbacks?
) : RecyclerView.ViewHolder(itemView) {

    protected var messageModel: MessageModel? = null

    protected var messageCloudColor = R.color.backgroundGray
    protected var messageReplyColor = R.color.colorDark
    protected var messageTextColor = R.color.colorWhite
    protected var messageFileSizeColor = R.color.colorGray

    protected val context: Context = itemView.context

    private var playNextVoiceCallback: (() -> Unit)? = null

    init {
        itemView.setOnClickListener {
            messageModel?.let {
                itemClickListeners.onMessageClick(messageModel!!)
            }
        }
    }

    /**
     * In this method we must initialize {@link #messageCloudColor} and {@link #messageReplyColor}
     * with specified colors for concrete message type
     */
    abstract fun initializeColors(messageModel: MessageModel)

    open fun bind(messageModel: MessageModel, displayDateDivider: Boolean) {
        this.messageModel = messageModel

        // Set sent time
        val sentAt =
            Calendar.getInstance().apply { timeInMillis = messageModel.getMessage().sentAt * 1000 }
        itemView.findViewById<TextView>(R.id.tvTime).text = DateUtils.timeFormat(sentAt)

        // Getting right colors for messageWithUser clouds and reply/forward headers
        initializeColors(messageModel)

        updateColors()

        if (messageModel.isForwarded()) {
            itemView.forwarder_header_layout.visibility = View.VISIBLE

            val name = messageModel.getForwardedMessageInfo()?.let { forwardedMessageInfo ->
                if (forwardedMessageInfo.isChannelMessage()) {
                    forwardedMessageInfo.getChannel()?.name
                        ?: context.getString(R.string.loading)
                } else {
                    if (forwardedMessageInfo.forwardedMessageInfo.forwardedFromUser == null) {
                        context.getString(R.string.name_is_hidden)
                    } else {
                        forwardedMessageInfo.getAuthorUserModel()?.let {
                            it.getDisplayName() ?: context.getString(R.string.name_is_hidden)
                        } ?: context.getString(R.string.loading)
                    }
                }
            } ?: context.getString(R.string.name_is_hidden)

            itemView.forwarder_header_layout.tvForwardedFrom.text =
                context.getString(R.string.from_placeholder, name)
        } else {
            itemView.forwarder_header_layout.visibility = View.GONE
        }

        itemView.forwarder_header_layout.tvForwardedFrom.setOnClickListener {
            messageModel.getForwardedMessageInfo()?.let { forwardedMessageInfoModel ->
                if (forwardedMessageInfoModel.isChannelMessage()) {
                    forwardedMessageInfoModel.getChannel()?.let {
                        itemClickListeners.onChannelClick(it.id)
                    }
                } else {
                    forwardedMessageInfoModel.getAuthor()?.let {
                        itemClickListeners.onUserClick(it.id)
                    }
                }
            }
        }

        itemView.findViewById<TextView>(R.id.tvDateDivider).apply {
            text = DateUtils.dateFormatWithoutYear(sentAt)
            visibility = if (displayDateDivider) View.VISIBLE else View.GONE
        }

        updateMessageText(messageModel)
        updateRepliedMessage(messageModel)
        updateAttachments(messageModel)
    }

    private fun updateColors() {
        // Message cloud color
        itemView.findViewById<View>(R.id.message_layout).backgroundTintList =
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    messageCloudColor
                )
            )

        // Message reply color
        itemView.findViewById<View>(R.id.reply_message_layout)?.backgroundTintList =
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    messageReplyColor
                )
            )
        itemView.findViewById<TextView>(R.id.tvUserNameReply)
            .setTextColor(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        messageReplyColor
                    )
                )
            )
        itemView.findViewById<TextView>(R.id.tvMessageTextReply)
            .setTextColor(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        messageReplyColor
                    )
                )
            )

        // Message forwarded from color the same as reply
        itemView.findViewById<TextView>(R.id.tvForwardedFrom)
            .setTextColor(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        messageReplyColor
                    )
                )
            )

        // Message text color the same as reply
        itemView.findViewById<TextView>(R.id.tvMessageText).apply {
            setTextColor(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        messageTextColor
                    )
                )
            )
            setLinkTextColor(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        messageTextColor
                    )
                )
            )
        }

        // Message system text color the same as reply
        itemView.findViewById<TextView>(R.id.tvMessageSystemText).apply {
            setTextColor(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        messageTextColor
                    )
                )
            )
        }

        // Poll color
        itemView.findViewById<TextView>(R.id.tvTitle).setTextColor(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    messageTextColor
                )
            )
        )

        itemView.findViewById<TextView>(R.id.tvVotesCount).setTextColor(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    messageTextColor
                )
            )
        )
    }

    fun updateMessageText(messageModel: MessageModel) {
        this.messageModel = messageModel
        // Show message text
        itemView.tvMessageText.text = messageModel.getMessage().text
    }

    fun updateRepliedMessage(messageModel: MessageModel) {
        this.messageModel = messageModel
        if (messageModel.getMessage().replyTo != null) {
            itemView.reply_message_layout.visibility = View.VISIBLE
            val repliedMessage = messageModel.getReplyToMessage()
            if (repliedMessage != null) {
                itemView.progressBar.visibility = View.GONE
                itemView.tvUserNameReply.visibility = View.VISIBLE
                itemView.tvMessageTextReply.visibility = View.VISIBLE

                val name =
                    if (repliedMessage.message.conversationType == ConversationType.CHANNEL) {
                        // We only can reply to message in the same conversation
                        // so we can just take channel name from this main message
                        messageModel.getChannel()?.name ?: context.getString(R.string.loading)
                    } else {
                        repliedMessage.getUserModel()?.getDisplayName()
                            ?: context.getString(R.string.name_is_hidden)
                    }

                val content = repliedMessage.message.text ?: context.getString(R.string.attachment)

                itemView.tvUserNameReply.text = name
                itemView.tvMessageTextReply.text = content
            } else {
                itemView.progressBar.visibility = View.VISIBLE
                itemView.tvUserNameReply.visibility = View.INVISIBLE
                itemView.tvMessageTextReply.visibility = View.INVISIBLE
            }
        } else {
            itemView.reply_message_layout.visibility = View.GONE
        }
    }

    fun updateAttachments(messageModel: MessageModel) {
        this.messageModel = messageModel

        itemView.layout_images_container.visibility = View.GONE
        (itemView.layout_images_container as ViewGroup).removeAllViews()

        itemView.layout_files_container.visibility = View.GONE
        (itemView.layout_files_container as ViewGroup).removeAllViews()

        itemView.poll_layout.visibility = View.GONE

        itemView.layout_voice.visibility = View.GONE

        itemView.tvMessageSystemText.visibility = View.GONE

        showEncryptedIcon(false)

        if (messageModel.hasAttachments()) {
            // Hide message text if it's empty
            itemView.tvMessageText.visibility =
                if (messageModel.getMessage().text.isNullOrBlank()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

            for (attachment in messageModel.attachments) {
                when {
                    attachment.isFile() -> {

                        val fileInfo = attachment.getPayloadAsFile()

                        if (fileInfo == null) {
                            attachmentIsUnavailable()
                            // TODO: show that this attachment is unavailable
                            // FIXME: is return okay?
                            return
                        }

                        val fileLayout = LayoutInflater.from(context)
                            .inflate(R.layout.message_file, itemView.layout_files_container, false)

                        // Fix colors of each file

                        // File icon color the same as reply
                        fileLayout.ivImage.setColorFilter(
                            ContextCompat.getColor(
                                context,
                                messageTextColor
                            )
                        )

                        // File name text color the same as reply
                        fileLayout.tvFileName.setTextColor(
                            ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    context,
                                    messageTextColor
                                )
                            )
                        )

                        // File size text color
                        fileLayout.tvFileSize.setTextColor(
                            ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    context,
                                    messageFileSizeColor
                                )
                            )
                        )

                        fileLayout.tvFileName.text = fileInfo.filename
                        fileLayout.tvFileSize.text =
                            Formatter.formatFileSize(context, fileInfo.size)

                        val hasStoredLocation = !TextUtils.isEmpty(attachment.savedAt)
                        var downloading = false

                        if (hasStoredLocation) {
                            fileLayout.ivDownload.visibility = View.INVISIBLE
                            fileLayout.pbDownloading.visibility = View.INVISIBLE
                            downloading = false
                        } else {
                            fileLayout.ivDownload.visibility = View.VISIBLE
                            fileLayout.pbDownloading.visibility = View.INVISIBLE
                        }


                        fileLayout.setOnClickListener {
                            if (hasStoredLocation) {
                                itemClickListeners.openFile(fileInfo, attachment)
                            } else if (!downloading) {
                                fileLayout.ivDownload.visibility = View.GONE
                                fileLayout.pbDownloading.visibility = View.VISIBLE
                                downloading = true
                                itemClickListeners.downloadFile(fileInfo, attachment.id!!)
                            }
                        }

                        (itemView.layout_files_container as ViewGroup).addView(fileLayout)
                        itemView.layout_files_container.visibility = View.VISIBLE
                    }

                    attachment.isPicture() -> {
                        // TODO: There can be multiple images

                        val fileInfo = attachment.getPayloadAsFile()

                        if (fileInfo == null) {
                            attachmentIsUnavailable()
                            // TODO: show that this attachment is unavailable
                            // FIXME: is return okay?
                            return
                        }

                        val imageLayout = LayoutInflater.from(context)
                            .inflate(
                                R.layout.message_image,
                                itemView.layout_images_container,
                                false
                            )

                        val imageUrl = UrlGenerator.getFileUrl(fileInfo.fileId)
                        glide.load(imageUrl)
                            .thumbnail(0.1f)
                            .into(imageLayout.ivImage)

                        imageLayout.ivImage.setOnClickListener {
                            itemClickListeners.onImageClick(imageUrl)
                        }

                        (itemView.layout_images_container as ViewGroup).addView(imageLayout)
                        itemView.layout_images_container.visibility = View.VISIBLE
                    }

                    attachment.isEncryptedMessage() -> {
                        showSystemMessage(R.string.encrypted_message)
                        showEncryptedIcon(true)

                        when (messageModel.getDecryptedMessageType()) {
                            EncryptedMessageType.TEXT -> {
                                val textMessage = messageModel.getDecryptedAsText()

                                hideSystemMessage()

                                itemView.tvMessageText.visibility = View.VISIBLE
                                itemView.tvMessageText.text = textMessage
                            }

                            EncryptedMessageType.FILE -> {
                                val fileInfo = messageModel.getDecryptedAsFileInfo()

                                hideSystemMessage()

                                itemView.tvMessageText.visibility = View.GONE

                                // COPIED FROM ABOVE
                                val fileLayout = LayoutInflater.from(context)
                                    .inflate(
                                        R.layout.message_file,
                                        itemView.layout_files_container,
                                        false
                                    )

                                // Fix colors of each file

                                // File icon color the same as reply
                                fileLayout.ivImage.setColorFilter(
                                    ContextCompat.getColor(
                                        context,
                                        messageTextColor
                                    )
                                )

                                // File name text color the same as reply
                                fileLayout.tvFileName.setTextColor(
                                    ColorStateList.valueOf(
                                        ContextCompat.getColor(
                                            context,
                                            messageTextColor
                                        )
                                    )
                                )

                                // File size text color
                                fileLayout.tvFileSize.setTextColor(
                                    ColorStateList.valueOf(
                                        ContextCompat.getColor(
                                            context,
                                            messageFileSizeColor
                                        )
                                    )
                                )

                                fileLayout.tvFileName.text = fileInfo.filename
                                fileLayout.tvFileSize.text =
                                    Formatter.formatFileSize(context, fileInfo.size)

                                val hasStoredLocation = !TextUtils.isEmpty(attachment.savedAt)
                                var downloading = false

                                if (hasStoredLocation) {
                                    fileLayout.ivDownload.visibility = View.INVISIBLE
                                    fileLayout.pbDownloading.visibility = View.INVISIBLE
                                    downloading = false
                                } else {
                                    fileLayout.ivDownload.visibility = View.VISIBLE
                                    fileLayout.pbDownloading.visibility = View.INVISIBLE
                                }

                                fileLayout.setOnClickListener {
                                    if (hasStoredLocation) {
                                        val file = File(attachment.savedAt!!)
                                        val encMes = attachment.getPayloadAsEncryptedMessage()
                                        encryptedMessageCallbacks?.openEncryptedFile(
                                            file,
                                            messageModel.getMessage().senderId!!,
                                            encMes.keyId,
                                            encMes.signKeyId
                                        )
                                    } else if (!downloading) {
                                        fileLayout.ivDownload.visibility = View.GONE
                                        fileLayout.pbDownloading.visibility = View.VISIBLE
                                        downloading = true
                                        itemClickListeners.downloadFile(fileInfo, attachment.id!!)
                                    }
                                }

                                (itemView.layout_files_container as ViewGroup).addView(fileLayout)
                                itemView.layout_files_container.visibility = View.VISIBLE
                                // COPIED FROM ABOVE END
                            }

                            EncryptedMessageType.PHOTO -> {
                                val fileInfo = messageModel.getDecryptedAsFileInfo()

                                hideSystemMessage()

                                itemView.tvMessageText.visibility = View.GONE

                                val imageLayout = LayoutInflater.from(context)
                                    .inflate(
                                        R.layout.message_image,
                                        itemView.layout_images_container,
                                        false
                                    )

                                // check savedAt, if it's empty - download a file
                                // else decrypt the file
                                // store the file in filesystem?
                                // and display it with glide

                                if (attachment.savedAt == null) {
                                    itemClickListeners.downloadFile(fileInfo, attachment.id!!)
                                    imageLayout.progressBar.visibility = View.VISIBLE
                                    glide.clear(imageLayout.ivImage)
                                } else {
                                    val file = File(attachment.savedAt!!)
                                    val encMes = attachment.getPayloadAsEncryptedMessage()
                                    encryptedMessageCallbacks?.decryptFile(
                                        file,
                                        messageModel.getMessage().senderId!!,
                                        encMes.keyId,
                                        encMes.signKeyId,
                                        object : DialogViewModel.DecryptFileCallback {
                                            override fun decrypted(data: ByteArray) {
                                                glide.load(data)
                                                    .into(imageLayout.ivImage)

                                                imageLayout.ivImage.setOnClickListener {
                                                    encryptedMessageCallbacks?.onEncryptedImageClick(
                                                        data
                                                    )
                                                }
                                            }

                                            override fun error() {
                                                imageLayout.visibility = View.GONE
                                                showSystemMessage(R.string.unsupported_encrypted_message)
                                            }
                                        })
                                }

                                (itemView.layout_images_container as ViewGroup).addView(imageLayout)
                                itemView.layout_images_container.visibility = View.VISIBLE
                            }

                            EncryptedMessageType.VOICE -> {
                                val fileInfo = messageModel.getDecryptedAsFileInfo()

                                hideSystemMessage()

                                itemView.tvMessageText.visibility = View.GONE

                                // Fix colors of each file

                                // File icon color the same as reply
                                itemView.ivImage.setColorFilter(
                                    ContextCompat.getColor(
                                        context,
                                        messageTextColor
                                    )
                                )

                                // Pause icon color
                                itemView.ivPause.setColorFilter(
                                    ContextCompat.getColor(
                                        context,
                                        messageTextColor
                                    )
                                )

                                // File name text color the same as reply
                                itemView.tvVoiceMessageLabel.setTextColor(
                                    ColorStateList.valueOf(
                                        ContextCompat.getColor(
                                            context,
                                            messageTextColor
                                        )
                                    )
                                )

                                // File size text color
                                itemView.tvFileSize.setTextColor(
                                    ColorStateList.valueOf(
                                        ContextCompat.getColor(
                                            context,
                                            messageFileSizeColor
                                        )
                                    )
                                )

                                itemView.tvFileSize.text =
                                    Formatter.formatFileSize(context, fileInfo.size)

                                val hasStoredLocation = !TextUtils.isEmpty(attachment.savedAt)
                                var downloading = false

                                if (hasStoredLocation) {
                                    itemView.pbDownloading.visibility = View.INVISIBLE
                                    downloading = false
                                } else {
                                    downloading = true
//                            itemView.pbDownloading.visibility = View.VISIBLE
                                    itemClickListeners.downloadFile(fileInfo, attachment.id!!)
                                }

                                itemView.ivImage.setOnClickListener {
                                    if (hasStoredLocation) {
                                        val file = File(attachment.savedAt!!)
                                        val encMes = attachment.getPayloadAsEncryptedMessage()
                                        // decrypt file
                                        encryptedMessageCallbacks?.decryptFile(
                                            file,
                                            messageModel.getMessage().senderId!!,
                                            encMes.keyId,
                                            encMes.signKeyId,
                                            object : DialogViewModel.DecryptFileCallback {
                                                override fun decrypted(data: ByteArray) {
                                                    // Play decrypted voice
                                                    itemClickListeners.playVoice(data, attachment.savedAt!!) {
                                                        // stop
                                                        itemView.ivPause.visibility = View.INVISIBLE
                                                        itemView.ivImage.visibility = View.VISIBLE

                                                        // Play next voice message
                                                        playNextVoiceCallback?.invoke()
                                                    }
                                                    checkVolumeLevel()
                                                    itemView.ivPause.visibility = View.VISIBLE
                                                    itemView.ivImage.visibility = View.INVISIBLE
                                                }

                                                override fun error() {
                                                    Toast.makeText(context, R.string.failed_to_decrypt_file, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    } else if (!downloading) {
                                        itemView.pbDownloading.visibility = View.VISIBLE
                                        downloading = true
                                        itemClickListeners.downloadFile(fileInfo, attachment.id!!)
                                    }
                                }

                                itemView.ivPause.setOnClickListener {
                                    itemClickListeners.pauseVoice()
                                    itemView.ivPause.visibility = View.INVISIBLE
                                    itemView.ivImage.visibility = View.VISIBLE
                                }

                                itemView.layout_voice.visibility = View.VISIBLE
                            }

                            null -> {
                                // nothing
                            }

                            else -> {
                                itemView.tvMessageText.visibility = View.GONE
                                showSystemMessage(R.string.unsupported_encrypted_message)
                            }
                        }
                    }

                    attachment.isExchangeKeyMessage() -> {
                        showSystemMessage(getSymmetricKeyMessage())
                        showEncryptedIcon(true)
                    }

                    attachment.isPoll() -> {
                        val poll = attachment.getPayloadAsPoll()

                        if (poll == null) {
                            attachmentIsUnavailable()
                            return
                        }

                        itemView.poll_layout.visibility = View.VISIBLE
                        itemView.tvTitle.text = poll.title
                        itemView.llOptions.removeAllViews()

                        val inflater = LayoutInflater.from(context)

                        itemView.tvMultipleChoice.visibility =
                            if (poll.multipleSelection) View.VISIBLE else View.GONE
                        itemView.tvHiddenResults.visibility =
                            if (poll.resultsVisibility) View.GONE else View.VISIBLE
                        itemView.tvSignRequired.visibility =
                            if (poll.signRequired) View.VISIBLE else View.GONE

                        var canSeeResults = true

                        var votesCount = 0
                        var maxVoted = 0

                        for (option in poll.pollOptions) {
                            canSeeResults = option.votedUsersCount != null

                            val voted = option.votedUsersCount?.toInt() ?: 0
                            votesCount += voted

                            if (voted > maxVoted) {
                                maxVoted = voted
                            }
                        }

                        for (option in poll.pollOptions) {
                            val optionRow = inflater.inflate(
                                R.layout.item_poll_option_vote,
                                itemView.llOptions,
                                false
                            )

                            val optionVotesCount = option.votedUsersCount?.toInt() ?: 0

                            // Set optionId as tag
                            optionRow.tag = option.optionId

                            // Set option description
                            optionRow.tvOptionDescription.text = option.description

                            // Set percent
                            val percent = if (optionVotesCount > 0)
                                (optionVotesCount.toFloat() / votesCount * 100).roundToInt()
                            else
                                0

                            optionRow.tvPercent.text =
                                context.getString(R.string.percent_label, percent)

                            // Set progress
                            optionRow.progressBar.max = maxVoted
                            optionRow.progressBar.progress = optionVotesCount

                            optionRow.setOnClickListener {
                                val id = it.tag as Int
                                itemClickListeners.votePoll(id, poll) {
                                    itemClickListeners.updateMessage(messageModel.getMessage().globalId)
                                }
                            }

                            optionRow.setOnLongClickListener {
                                if (poll.voted && optionVotesCount > 0) {
                                    val optionId = it.tag as Int
                                    itemClickListeners.showVotedUsers(optionId, poll)
                                    true
                                } else {
                                    false
                                }
                            }

                            optionRow.tvOptionDescription.setTextColor(
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(
                                        context,
                                        messageTextColor
                                    )
                                )
                            )

                            // Change progressbar color
                            val layers = optionRow.progressBar.progressDrawable as LayerDrawable
                            layers.getDrawable(2).setColorFilter(
                                ContextCompat.getColor(
                                    context,
                                    messageTextColor
                                ), PorterDuff.Mode.SRC_IN
                            )

                            // Handle visibility of views if poll is voted
                            if (poll.voted) {
                                optionRow.tvPercent.visibility =
                                    if (canSeeResults) View.VISIBLE else View.INVISIBLE
                                optionRow.progressBar.visibility = View.VISIBLE
                                optionRow.radioButton.visibility = View.INVISIBLE
                            } else {
                                optionRow.tvPercent.visibility = View.INVISIBLE
                                optionRow.progressBar.visibility = View.INVISIBLE
                                optionRow.radioButton.visibility = View.VISIBLE
                            }

                            // Option voted visibility
                            optionRow.ivVoted.visibility =
                                if (option.voted) View.VISIBLE else View.INVISIBLE

                            itemView.llOptions.addView(optionRow)
                        }

                        itemView.tvVotesCount.text =
                            context.resources.getQuantityString(
                                R.plurals.votes,
                                votesCount,
                                votesCount
                            )

                        itemView.tvVotesCount.visibility =
                            if (canSeeResults) View.VISIBLE else View.GONE

                        // FIXME: Make method for getting poll object by pollId instead of getting message object
                        itemClickListeners.updateMessage(messageModel.getMessage().globalId)
                    }

                    attachment.isVoice() -> {
                        val fileInfo = attachment.getPayloadAsFile()

                        if (fileInfo == null) {
                            attachmentIsUnavailable()
                            // TODO: show that this attachment is unavailable
                            // FIXME: is return okay?
                            return
                        }

                        // Fix colors of each file

                        // File icon color the same as reply
                        itemView.ivImage.setColorFilter(
                            ContextCompat.getColor(
                                context,
                                messageTextColor
                            )
                        )

                        // Pause icon color
                        itemView.ivPause.setColorFilter(
                            ContextCompat.getColor(
                                context,
                                messageTextColor
                            )
                        )

                        // File name text color the same as reply
                        itemView.tvVoiceMessageLabel.setTextColor(
                            ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    context,
                                    messageTextColor
                                )
                            )
                        )

                        // File size text color
                        itemView.tvFileSize.setTextColor(
                            ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    context,
                                    messageFileSizeColor
                                )
                            )
                        )

                        itemView.tvFileSize.text =
                            Formatter.formatFileSize(context, fileInfo.size)

                        val hasStoredLocation = !TextUtils.isEmpty(attachment.savedAt)
                        var downloading = false

                        if (hasStoredLocation) {
                            itemView.pbDownloading.visibility = View.INVISIBLE
                            downloading = false
                        } else {
                            downloading = true
//                            itemView.pbDownloading.visibility = View.VISIBLE
                            itemClickListeners.downloadFile(fileInfo, attachment.id!!)
                        }

                        itemView.ivImage.setOnClickListener {
                            if (hasStoredLocation) {
                                itemClickListeners.playVoice(attachment.savedAt!!) {
                                    // stop
                                    itemView.ivPause.visibility = View.INVISIBLE
                                    itemView.ivImage.visibility = View.VISIBLE

                                    // Play next voice message
                                    playNextVoiceCallback?.invoke()
                                }
                                checkVolumeLevel()
                                itemView.ivPause.visibility = View.VISIBLE
                                itemView.ivImage.visibility = View.INVISIBLE
                            } else if (!downloading) {
                                itemView.pbDownloading.visibility = View.VISIBLE
                                downloading = true
                                itemClickListeners.downloadFile(fileInfo, attachment.id!!)
                            }
                        }

                        itemView.ivPause.setOnClickListener {
                            itemClickListeners.pauseVoice()
                            itemView.ivPause.visibility = View.INVISIBLE
                            itemView.ivImage.visibility = View.VISIBLE
                        }

                        itemView.layout_voice.visibility = View.VISIBLE
                    }

                    else -> {
                        showSystemMessage(R.string.unsupported_message)
                    }
                }
            }
        } else {
            itemView.tvMessageText.visibility = View.VISIBLE
            if (messageModel.isForwarded() && messageModel.getMessage().text == null) {
                itemView.tvMessageText.text = context.getString(R.string.attachment_is_deleted)
            }
        }
    }

    private fun attachmentIsUnavailable() {
        // Hide all attachment layouts
        itemView.poll_layout.visibility = View.GONE
        itemView.layout_images_container.visibility = View.GONE
        itemView.layout_files_container.visibility = View.GONE

        // Show text
        itemView.tvMessageText.visibility = View.VISIBLE
        itemView.tvMessageText.text = context.getString(R.string.attachment_is_unavailable)
    }

    private fun showEncryptedIcon(show: Boolean) {
        itemView.findViewById<ImageView>(R.id.ivEncrypted)?.let {
            it.visibility = if (show) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
        }
    }

    private fun showSystemMessage(@StringRes resId: Int) {
        showSystemMessage(context.getString(resId))
    }

    private fun showSystemMessage(message: String) {
        itemView.tvMessageSystemText.text = message
        itemView.tvMessageSystemText.visibility = View.VISIBLE
    }

    private fun hideSystemMessage() {
        itemView.tvMessageSystemText.visibility = View.GONE
    }

    abstract fun getSymmetricKeyMessage(): Int

    private fun checkVolumeLevel() {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val volumeLevel = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolumeLevel = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volumePercent = volumeLevel * 100 / maxVolumeLevel
        if (volumePercent < 20) {
            Toast.makeText(context, R.string.turn_up_the_volume, Toast.LENGTH_SHORT).show()
        }
    }

    fun setPlayNextVoiceCallback(playNextVoice: (() -> Unit)?) {
        this.playNextVoiceCallback = playNextVoice
    }

    fun tryPlayVoiceMessage() {
        messageModel?.let { message ->
            if (!message.hasAttachments()) return

            val attachment = message.getAttachment()
            if (attachment.isVoice()) {
                if (TextUtils.isEmpty(attachment.savedAt)) return

                itemClickListeners.playVoice(attachment.savedAt!!) {
                    // On voice played
                    itemView.ivPause.visibility = View.INVISIBLE
                    itemView.ivImage.visibility = View.VISIBLE

                    // Play next voice message
                    playNextVoiceCallback?.invoke()
                }
                itemView.ivPause.visibility = View.VISIBLE
                itemView.ivImage.visibility = View.INVISIBLE
            } else if (message.getDecryptedMessageType() == EncryptedMessageType.VOICE) {
                if (TextUtils.isEmpty(attachment.savedAt)) return

                val file = File(attachment.savedAt!!)
                val encMes = attachment.getPayloadAsEncryptedMessage()
                // decrypt file
                encryptedMessageCallbacks?.decryptFile(
                    file,
                    message.getMessage().senderId!!,
                    encMes.keyId,
                    encMes.signKeyId,
                    object : DialogViewModel.DecryptFileCallback {
                        override fun decrypted(data: ByteArray) {
                            // Play decrypted voice
                            itemClickListeners.playVoice(data, attachment.savedAt!!) {
                                // stop
                                itemView.ivPause.visibility = View.INVISIBLE
                                itemView.ivImage.visibility = View.VISIBLE

                                // Play next voice message
                                playNextVoiceCallback?.invoke()
                            }
                            checkVolumeLevel()
                            itemView.ivPause.visibility = View.VISIBLE
                            itemView.ivImage.visibility = View.INVISIBLE
                        }

                        override fun error() {
                            Toast.makeText(context, R.string.failed_to_decrypt_file, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    companion object {
        private const val TAG = "AbstractMessageHolder"
    }
}