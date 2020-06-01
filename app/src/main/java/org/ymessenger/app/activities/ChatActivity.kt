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

package org.ymessenger.app.activities

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.input_message_layout.*
import kotlinx.android.synthetic.main.toolbar_chat.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.*
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.local.db.entities.Chat
import org.ymessenger.app.data.local.db.models.MessageModel
import org.ymessenger.app.data.local.db.models.UserActionModel
import org.ymessenger.app.data.remote.Config
import org.ymessenger.app.data.remote.entities.Attachment
import org.ymessenger.app.data.remote.entities.FileInfo
import org.ymessenger.app.data.remote.entities.Message
import org.ymessenger.app.data.remote.entities.Poll
import org.ymessenger.app.di.Injection
import org.ymessenger.app.fragments.MessagesFragment
import org.ymessenger.app.fragments.addfile.CreatePollFragment
import org.ymessenger.app.fragments.addfile.FilesFragment
import org.ymessenger.app.fragments.addfile.GalleryFragment
import org.ymessenger.app.helpers.MyNotificationManager
import org.ymessenger.app.models.AttachmentModel
import org.ymessenger.app.utils.FileUtils
import org.ymessenger.app.utils.RichEditText
import org.ymessenger.app.utils.SimpleTextWatcher
import org.ymessenger.app.viewmodels.ChatViewModel
import java.io.File
import java.util.*

class ChatActivity : BaseActivity(), MessagesPagedAdapter.ItemClickListeners {

    private lateinit var viewModel: ChatViewModel
    private var messagesFragment: MessagesFragment? = null

    private lateinit var chatUsersMiniAdapter: ChatUsersMiniAdapter

    private var displayUserActionTimer: Timer? = null

    companion object {
        private const val TAG = "ChatActivity"
        private const val ARG_CHAT_ID = "chat_id"

        private const val REQUEST_CODE_ATTACHMENTS = 100
        private const val REQUEST_CODE_PICK_CONVERSATION_FOR_FORWARD = 200
        private const val REQUEST_CODE_CHAT_INFO_ACTIVITY = 300

        private const val ACTION_SHARE = "ACTION_SHARE"

        private const val ARG_SHARED_TEXT = "shared_text"
        private const val ARG_SHARED_IMAGE = "shared_image"

        private const val SHARE_TYPE_TEXT = "SHARE_TYPE_TEXT"
        private const val SHARE_TYPE_IMAGE = "SHARE_TYPE_IMAGE"

        fun startActivity(context: Context, chatId: Long) {
            context.startActivity(getIntent(context, chatId))
        }

        fun shareText(context: Context, userId: Long, text: String) {
            val intent = getIntent(context, userId)
            intent.action = ACTION_SHARE
            intent.type = SHARE_TYPE_TEXT
            intent.putExtra(ARG_SHARED_TEXT, text)

            context.startActivity(intent)
        }

        fun shareImage(context: Context, userId: Long, uri: Uri) {
            val intent = getIntent(context, userId)
            intent.action = ACTION_SHARE
            intent.type = SHARE_TYPE_IMAGE
            intent.putExtra(ARG_SHARED_IMAGE, uri)

            context.startActivity(intent)
        }

        fun getIntent(context: Context, chatId: Long): Intent {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra(ARG_CHAT_ID, chatId)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val chatId = getChatId()
        val factory = Injection.provideChatViewModelFactory(appBase, chatId)
        viewModel = ViewModelProviders.of(this, factory).get(ChatViewModel::class.java)

        initToolbar(toolbar)

        val currentUserId = appBase.authorizationManager.getAuthorizedUserId()
        if (currentUserId == null) {
            Log.e(TAG, "Current user id is empty")
            finish()
            return
        }

        // Put messages fragment
        messagesFragment = MessagesFragment.get(chatId, ConversationType.CHAT)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.messages_container, messagesFragment!!)
        transaction.commit()

        btnSend.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            sendMessage(viewModel)
        }
        btnAttachments.setOnClickListener {
            if (!canClick()) return@setOnClickListener

            if (viewModel.getAvailableAttachmentsCount() <= 0) {
                showToast(R.string.you_have_added_maximum_amount_of_attachments)
                return@setOnClickListener
            }

            openAddAttachmentActivity()
        }
        toolbar_layout.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            viewModel.openGroupInfo()
        }
        btnRemoveReply.setOnClickListener { viewModel.setReplyTo(null) }

        btnJoinChat.setOnClickListener {
            viewModel.joinChat()
        }

        etMessageText.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onChanged(text: String) {
                viewModel.setMessageText(if (text.isBlank()) null else text)
            }
        })

        btnVoiceMessage.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "Action down")
                    if (hasPermissionForVoiceMessaging()) {
                        viewModel.startRecordingVoiceMessage()
                    }
                }

                MotionEvent.ACTION_UP -> {
                    Log.d(TAG, "Action up")
                    viewModel.stopRecordingVoiceMessage()
                }

                MotionEvent.ACTION_CANCEL -> {
                    Log.d(TAG, "Action cancel. Stop voice recording")
                    viewModel.stopRecordingVoiceMessage()
                }
            }

            return@setOnTouchListener false
        }

        // In case if it is shared intent
        handleIntent()

        supportGifKeyboard()

        prepareRecyclerViewForUserCalling()
        prepareEditTextForUserCalling()

        val attachmentsAdapter = initRVAttachments(viewModel)

        subscribeUi(viewModel, attachmentsAdapter)
    }

    private fun hasPermissionForVoiceMessaging(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
                ), 0
            )
            return false
        }
        return true
    }

    private fun supportGifKeyboard() {
        etMessageText.setKeyboardInputCallbackListener(object :
            RichEditText.KeyboardInputCallbackListener {
            override fun onCommitContent(
                inputContentInfo: InputContentInfoCompat,
                flags: Int,
                opts: Bundle?
            ) {
                sendImageFromUri(inputContentInfo.contentUri)
            }
        })
    }

    private fun initRVAttachments(viewModel: ChatViewModel): AttachmentsAdapter {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvAttachments.layoutManager = layoutManager

        val glide = Glide.with(this)
        val adapter = AttachmentsAdapter(glide, {
            viewModel.deleteAttachment(it)
        }, { fromPosition, toPosition ->
            viewModel.moveItem(fromPosition, toPosition)
        })

        // Move items
        val callback = AttachmentItemTouchHelperCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(rvAttachments)

        rvAttachments.adapter = adapter

        val itemOffsetDecoration =
            FavouriteUserOffsetDecoration(resources.getDimensionPixelSize(R.dimen.gapSmall))
        rvAttachments.addItemDecoration(itemOffsetDecoration)

        return adapter
    }

    private fun prepareEditTextForUserCalling() {
        etMessageText.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onChanged(text: String) {
                if (text.isNotBlank() && text.last() == '@') {
                    Log.d(TAG, "Last symbol is @")
                    rvChatUsers.visibility = View.VISIBLE
                } else {
                    rvChatUsers.visibility = View.GONE
                }
            }
        })
    }

    private fun prepareRecyclerViewForUserCalling() {
        rvChatUsers.layoutManager = LinearLayoutManager(this)
        chatUsersMiniAdapter = ChatUsersMiniAdapter {
            Log.d(TAG, "Chat user ${it.chatUser.userId} selected")
            // remove @ and insert template for calling
            val text = etMessageText.text.toString()
            val newText =
                text.substring(0, text.length - 1) + "[UserCalling](userId=${it.chatUser.userId}) "
            etMessageText.setText(newText)
            etMessageText.setSelection(newText.length)
        }

        rvChatUsers.adapter = chatUsersMiniAdapter
    }

    private fun handleIntent() {
        if (ACTION_SHARE == intent.action) {
            viewModel.setConnected(
                appBase.getWebSocketService().getConnectionStatus().value ?: false
            )
            if (SHARE_TYPE_TEXT == intent.type) {
                val sharedText = intent.getStringExtra(ARG_SHARED_TEXT)
                if (sharedText.isNotBlank()) {
                    etMessageText.setText(sharedText)
                }
            } else if (SHARE_TYPE_IMAGE == intent.type) {
                val imageUri = intent.getParcelableExtra<Uri>(ARG_SHARED_IMAGE)
                imageUri?.let { uri ->
                    // Build file to get filename
                    sendImageFromUri(uri)
                }
            }
        }
    }

    private fun sendImageFromUri(uri: Uri) {
        val file = File(uri.path)
        val bytes = FileUtils.readBytesFromUri(this, uri)
        bytes?.let {
            uploadFile(it, file.name, false, Attachment.Type.PICTURE)
        }
    }

    override fun onResume() {
        super.onResume()
        MyNotificationManager.cancelMessageNotification(this, getChatId().toInt())
    }

    private fun getChatId(): Long {
        return intent.extras?.getLong(ARG_CHAT_ID) ?: throw NullPointerException("chat_id is null")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (!viewModel.isChatDeleted() && viewModel.isMember()) {
            menuInflater.inflate(R.menu.chat_menu, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        // this callback is called after protectedConversation is posted, so update the value here. This is safe
        menu?.findItem(R.id.mi_chat_security)?.isChecked =
            viewModel.protectedConversation.value != null
        menu?.findItem(R.id.mi_chat_security)?.isVisible =
            false // Hide item since there is no encryption

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.mi_chat_security -> {
                viewModel.switchProtected()
                true
            }

            R.id.mi_gallery -> {
                viewModel.openGallery()
                true
            }

            R.id.mi_clear_messages_locally -> {
                viewModel.clearMessagesLocally()
                true
            }

            R.id.mi_leave -> {
                leaveChat()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun subscribeUi(viewModel: ChatViewModel, attachmentsAdapter: AttachmentsAdapter) {
        viewModel.chat.observe(this, Observer { chat ->
            if (chat != null) {
                setChat(chat)
            }
        })

        viewModel.protectedConversation.observe(this, Observer {
            invalidateOptionsMenu()
        })

        viewModel.chatUsers.observe(this, Observer { chatUsers ->
            val count = chatUsers.size
            tvMembersCount.text = resources.getQuantityString(R.plurals.members, count, count)
            Log.d(TAG, "chat users count $count")

            chatUsersMiniAdapter.submitList(chatUsers)
        })

        viewModel.currentChatUser.observe(this, Observer {
            if (it == null) {
                btnJoinChat.visibility = View.VISIBLE
                input_message_layout.visibility = View.GONE
            } else {
                btnJoinChat.visibility = View.GONE
                input_message_layout.visibility = View.VISIBLE
            }

            invalidateOptionsMenu()
        })

        viewModel.openGroupInfoEvent.observe(this, Observer {
            startActivityForResult(
                GroupInfoActivity.getIntent(this, it),
                REQUEST_CODE_CHAT_INFO_ACTIVITY
            )
        })

        viewModel.leaveChatEvent.observe(this, Observer {
            finish()
        })

        viewModel.subscribeOnEvents(this)

        viewModel.replyTo.observe(this, Observer {
            updateReplyMessage(it)
        })

        viewModel.pickConversationForForwardEvent.observe(this, Observer {
            startActivityForResult(
                SelectChatActivity.getIntent(this),
                REQUEST_CODE_PICK_CONVERSATION_FOR_FORWARD
            )
        })

        viewModel.messageIsSending.observe(this, Observer {
            layout_send_buttons.visibility = if (it) View.INVISIBLE else View.VISIBLE
            layout_send_buttons.isEnabled = !it
            pbMessageIsSending.visibility = if (it) View.VISIBLE else View.INVISIBLE
        })

        appBase.getWebSocketService().getConnectionStatus().observe(this, Observer {
            viewModel.setConnected(it)
            tvConnectionStatus.visibility = if (it) View.INVISIBLE else View.VISIBLE
            tvMembersCount.visibility = if (it) View.VISIBLE else View.INVISIBLE
            btnSend.isEnabled = it
        })

        viewModel.openGalleryEvent.observe(this, Observer {
            ConversationGalleryActivity.start(this, it, ConversationType.CHAT)
        })

        viewModel.authorizedEvent.observe(this, Observer {
            Log.d(TAG, "Authorized Event")
            messagesFragment?.invalidateMessages()
        })

        viewModel.draftMessage.observe(this, Observer {
            it?.let {
                etMessageText.setText(it.text)
                etMessageText.setSelection(it.text.length)
            }
        })

        viewModel.clearMessageTextEvent.observe(this, Observer {
            etMessageText.text.clear()
        })

        viewModel.attachments.observe(this, Observer {
            attachmentsAdapter.submitList(it)
            rvAttachments.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
        })

        viewModel.messageIsEmpty.observe(this, Observer {
            if (it) {
                btnSend.visibility = View.GONE
                btnVoiceMessage.visibility = View.VISIBLE
            } else {
                btnSend.visibility = View.VISIBLE
                btnVoiceMessage.visibility = View.GONE
            }
        })

        viewModel.voiceMessageIsRecording.observe(this, Observer {
            if (it) {
                etMessageText.visibility = View.INVISIBLE
                tvVoiceRecordingLabel.visibility = View.VISIBLE
            } else {
                etMessageText.visibility = View.VISIBLE
                tvVoiceRecordingLabel.visibility = View.INVISIBLE
            }
        })

        viewModel.lastUserActions.observe(this, Observer {
            Log.d(TAG, "Last user actions size - ${it.size}")
            displayUserActionTimer?.cancel()
            if (it.isNotEmpty()) {
                showUserAction(it.first())
                displayUserActionTimer = Timer()
                displayUserActionTimer?.schedule(object : TimerTask() {
                    override fun run() {
                        hideUserAction()
                    }
                }, Config.USER_ACTION_DISPLAY_PERIOD * 1000L)
            } else {
                hideUserAction()
            }
        })
    }

    private fun showUserAction(userActionModel: UserActionModel) {
        val userName = userActionModel.getDisplayName() ?: getString(R.string.name_is_hidden)
        val actionLabel = getString(userActionModel.getActionPlaceholderLabelRes(), userName)
        tvUserAction.text = actionLabel
        tvUserAction.visibility = View.VISIBLE
        tvMembersCount.visibility = View.INVISIBLE
    }

    private fun hideUserAction() {
        tvUserAction.visibility = View.INVISIBLE
        tvMembersCount.visibility = View.VISIBLE
    }

    private fun setChat(chat: Chat) {
        tvPhotoLabel.text = chat.getPhotoLabel()
        Glide.with(this)
            .load(chat.getPhotoUrl())
            .into(ivChatPhoto)

        tvChatName.text = chat.name

        if (chat.deleted) {
            tvMembersCount.text = getString(R.string.chat_is_deleted)
            tvMembersCount.setTextColor(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this,
                        R.color.colorGray
                    )
                )
            )
            input_message_layout.visibility = View.GONE
        }

        invalidateOptionsMenu()
    }

    override fun onPause() {
        super.onPause()
        if (etMessageText.text.isBlank()) {
            viewModel.deleteDrafts()
        } else {
            viewModel.saveDraft(etMessageText.text.toString())
        }
    }

    private fun replyToMessage(messageModel: MessageModel) {
        viewModel.setReplyTo(messageModel)
    }

    private fun forwardMessage(messageModel: MessageModel) {
        viewModel.pickConversationForForward(listOf(messageModel))
    }

    private fun updateReplyMessage(messageModel: MessageModel?) {
        if (messageModel != null) {
            layout_reply.visibility = View.VISIBLE
            layout_reply.findViewById<TextView>(R.id.tvUserNameReply).text =
                messageModel.getAuthor()?.firstName ?: getString(R.string.name_is_hidden)
            layout_reply.findViewById<TextView>(R.id.tvMessageTextReply).text =
                messageModel.getMessage().text ?: getString(R.string.attachment)
        } else {
            layout_reply.visibility = View.GONE
        }
    }

    private fun copyMessage(messageModel: MessageModel) {
        // FIXME: only text messages
        Toast.makeText(this, getString(R.string.message_text_was_copied), Toast.LENGTH_SHORT).show()
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.primaryClip =
            ClipData.newPlainText("Message text", messageModel.getMessage().text)
    }

    private fun deleteMessage(messageModel: MessageModel) {
        AlertDialog.Builder(this)
            .setTitle(R.string.attention)
            .setMessage(R.string.do_you_want_delete_this_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteMessages(listOf(messageModel))
            }.show()
    }

    @Deprecated("We do not need this method no more")
    private fun clearFromCache(messageModel: MessageModel) {
        AlertDialog.Builder(this)
            .setTitle(R.string.attention)
            .setMessage(R.string.do_you_want_clear_this_file_from_cache)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear_from_cache) { _, _ ->
                viewModel.clearFileFromCache(messageModel.getAttachment())
            }.show()
    }

    @Deprecated("We do not need this method no more")
    private fun saveFileToDownloads(messageModel: MessageModel) {
        viewModel.copyFileToDownloads(messageModel.getAttachment())
    }

    private fun sendMessage(viewModel: ChatViewModel) {
        viewModel.prepareMessageToSend()
    }

    private fun leaveChat() {
        AlertDialog.Builder(this)
            .setTitle(R.string.attention)
            .setMessage(R.string.do_you_really_want_leave_chat)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.leave) { _, _ ->
                appBase.authorizationManager.getAuthorizedUserId()?.let { userId ->
                    viewModel.leaveChat(userId)
                }
            }.show()
    }

    private fun openAddAttachmentActivity() {
        startActivityForResult(
            AddAttachmentActivity.getIntent(
                this,
                ConversationType.CHAT,
                viewModel.hasPollAttachment(),
                viewModel.getAvailableAttachmentsCount()
            ),
            REQUEST_CODE_ATTACHMENTS
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_ATTACHMENTS -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        if (data.hasExtra(GalleryFragment.ARRAY_IMAGE_PATHS)) {
                            val imagePaths =
                                data.getStringArrayListExtra(GalleryFragment.ARRAY_IMAGE_PATHS)

                            val attachmentModels =
                                imagePaths.map { AttachmentModel(Attachment.Type.PICTURE, it) }

                            viewModel.addAttachments(attachmentModels)
                        } else if (data.hasExtra(FilesFragment.SELECTED_FILE_PATHS)) {
                            val filePaths =
                                data.getStringArrayListExtra(FilesFragment.SELECTED_FILE_PATHS)

                            val attachmentModels =
                                filePaths.map { AttachmentModel(Attachment.Type.FILE, it) }

                            viewModel.addAttachments(attachmentModels)
                        } else if (data.hasExtra(CreatePollFragment.POLL_OBJECT)) {
                            val pollJson = data.getStringExtra(CreatePollFragment.POLL_OBJECT)

                            val attachmentModel = AttachmentModel(Attachment.Type.POLL, null)
                            attachmentModel.pollJson = pollJson

                            viewModel.addAttachments(listOf(attachmentModel))
                        }
                    }
                }
            }

            REQUEST_CODE_PICK_CONVERSATION_FOR_FORWARD -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val conversationType =
                        data.extras?.getInt(SelectChatActivity.KEY_CONVERSATION_TYPE)
                            ?: throw Exception("Conversation type is null")
                    val identifier = data.extras?.getLong(SelectChatActivity.KEY_IDENTIFIER)
                        ?: throw Exception("Identifier is null")
                    viewModel.forwardMessagesTo(conversationType, identifier)
                } else {
                    viewModel.clearMessagesToForward()
                }
            }

            REQUEST_CODE_CHAT_INFO_ACTIVITY -> {
                if (resultCode == GroupInfoActivity.RESULT_CODE_CHAT_DELETED) {
                    // TODO: delete local messages from this chat
                    finish()
                }
            }
        }
    }

    private fun openImage(photoUrl: String?) {
        StfalconImageViewer.Builder(this, listOf(photoUrl)) { imageView, image ->
            Glide.with(this)
                .load(image)
                .thumbnail(0.1F)
                .apply(RequestOptions().override(Target.SIZE_ORIGINAL))
                .into(imageView)
        }.withHiddenStatusBar(false)
            .show()
    }

    private fun openFile(file: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooserIntent = Intent.createChooser(intent, "Open file with:")
            startActivity(chooserIntent)
        } catch (exception: Exception) {
            exception.printStackTrace()
            showError(R.string.failed_to_open_file)
        }
    }

    private fun uploadFile(file: File, isDocument: Boolean, attachmentType: Int) {
        uploadFile(file.readBytes(), file.name, isDocument, attachmentType)
    }

    private fun uploadFile(
        data: ByteArray,
        fileName: String,
        isDocument: Boolean,
        attachmentType: Int
    ) {
        viewModel.uploadFile(data, fileName, isDocument) { fileInfo ->
            viewModel.sendFile(fileInfo, attachmentType)
        }
    }

    override fun onMessageReceived(remoteMessage: Message) {
        val uid = getChatId()
        if ((appBase.appInBackground || remoteMessage.conversationId != uid && remoteMessage.senderId != uid) &&
            remoteMessage.senderId != appBase.authorizationManager.getAuthorizedUserId()
        ) {
            MyNotificationManager.showNewMessageNotification(appBase, remoteMessage)
        }
    }

    override fun onMessageClick(messageModel: MessageModel) {
        val popupMenu = PopupMenu(this, null)
        val menu = popupMenu.menu
        popupMenu.menuInflater.inflate(R.menu.message_menu, menu)

        // Display copy option
        menu.findItem(R.id.mi_copy).isVisible =
            messageModel.getMessage().text?.isNotBlank() ?: false

        if (viewModel.isChatDeleted()) {
            menu.findItem(R.id.mi_reply).isVisible = false
            menu.findItem(R.id.mi_forward).isVisible = false
            menu.findItem(R.id.mi_delete).isVisible = false
        }

        val dialog = BottomSheetBuilder(this)
            .setMode(BottomSheetBuilder.MODE_LIST)
            .setMenu(menu)
            .setItemClickListener {
                when (it.itemId) {
                    R.id.mi_reply -> replyToMessage(messageModel)
                    R.id.mi_forward -> forwardMessage(messageModel)
                    R.id.mi_copy -> copyMessage(messageModel)
                    R.id.mi_delete -> deleteMessage(messageModel)
                    else -> showToast(R.string.this_feature_unavailable)
                }
            }.createDialog()

        dialog.show()
    }

    override fun onUserClick(userId: Long) {
        UserProfileActivity.startActivity(this, userId)
    }

    override fun onChannelClick(channelId: Long) {
        ChannelActivity.startActivity(this, channelId)
    }

    override fun onImageClick(imageUrl: String?) {
        openImage(imageUrl)
    }

    override fun openFile(
        fileInfo: FileInfo,
        attachment: org.ymessenger.app.data.local.db.entities.Attachment
    ) {
        val savedAt = attachment.savedAt ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.open_file)
            .setMessage(getString(R.string.file_saved_at, savedAt))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.open_file) { _, _ ->
                val file = File(savedAt)
                if (file.exists()) {
                    openFile(file)
                } else {
                    viewModel.saveAttachmentFilePath(attachment.id!!, null)
                    showError(R.string.file_does_not_exist)
                }
            }
            .show()
    }

    override fun downloadFile(fileInfo: FileInfo, attachmentId: Long) {
        viewModel.downloadFile(fileInfo) { path ->
            viewModel.saveAttachmentFilePath(attachmentId, path)
        }
    }

    override fun votePoll(optionId: Int, poll: Poll, callback: () -> Unit) {
        viewModel.votePoll(optionId, poll, callback)
    }

    override fun showVotedUsers(optionId: Int, poll: Poll) {
        val optionText =
            poll.pollOptions[optionId].description // use optionId as index, but it can be wrong
        VotedUserListActivity.open(
            this,
            poll.pollId!!,
            optionId,
            optionText,
            poll.conversationId,
            poll.conversationType,
            poll.signRequired
        )
    }

    override fun updateMessage(messageId: String) {
        // nothing
    }

    override fun playVoice(filePath: String) {
        viewModel.playVoice(filePath)
    }
}