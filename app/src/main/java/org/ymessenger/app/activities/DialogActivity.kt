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
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
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
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.android.synthetic.main.input_message_layout.*
import kotlinx.android.synthetic.main.toolbar_user_dialog.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.AttachmentItemTouchHelperCallback
import org.ymessenger.app.adapters.AttachmentsAdapter
import org.ymessenger.app.adapters.FavouriteUserOffsetDecoration
import org.ymessenger.app.adapters.MessagesPagedAdapter
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.local.db.entities.Dialog
import org.ymessenger.app.data.local.db.models.MessageModel
import org.ymessenger.app.data.local.db.models.UserActionModel
import org.ymessenger.app.data.local.db.models.UserModel
import org.ymessenger.app.data.remote.Config
import org.ymessenger.app.data.remote.entities.Attachment
import org.ymessenger.app.data.remote.entities.FileInfo
import org.ymessenger.app.data.remote.entities.Message
import org.ymessenger.app.data.remote.entities.Poll
import org.ymessenger.app.di.Injection
import org.ymessenger.app.fragments.DialogEmptyFragment
import org.ymessenger.app.fragments.MessagesFragment
import org.ymessenger.app.fragments.addfile.FilesFragment
import org.ymessenger.app.fragments.addfile.GalleryFragment
import org.ymessenger.app.helpers.FileExtensionHelper
import org.ymessenger.app.helpers.MyNotificationManager
import org.ymessenger.app.models.AttachmentModel
import org.ymessenger.app.services.AsymmetricKeysGeneratorService
import org.ymessenger.app.utils.FileUtils
import org.ymessenger.app.utils.ImageUtils
import org.ymessenger.app.utils.RichEditText
import org.ymessenger.app.utils.SimpleTextWatcher
import org.ymessenger.app.viewmodels.DialogViewModel
import java.io.File
import java.util.*

class DialogActivity : BaseActivity(), MessagesPagedAdapter.ItemClickListeners,
    MessagesPagedAdapter.EncryptedMessageCallbacks {

    private lateinit var viewModel: DialogViewModel
    private var messagesFragment: MessagesFragment? = null

    private var miProtectedConversation: MenuItem? = null

    private var displayUserActionTimer: Timer? = null

    companion object {
        private const val TAG = "DialogActivity"
        private const val REQUEST_CODE_ATTACHMENTS = 100
        private const val REQUEST_CODE_PICK_CONVERSATION_FOR_FORWARD = 200
        private const val REQUEST_CODE_CHECK_ENCRYPTION_KEY = 300
        private const val ARG_USER_ID = "user_id"

        private const val ACTION_SHARE = "ACTION_SHARE"

        private const val ARG_SHARED_TEXT = "shared_text"
        private const val ARG_SHARED_IMAGE = "shared_image"

        private const val SHARE_TYPE_TEXT = "SHARE_TYPE_TEXT"
        private const val SHARE_TYPE_IMAGE = "SHARE_TYPE_IMAGE"

        fun start(context: Context, userId: Long) {
            context.startActivity(getIntent(context, userId))
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

        fun getIntent(context: Context, userId: Long): Intent {
            val intent = Intent(context, DialogActivity::class.java)
            intent.putExtra(ARG_USER_ID, userId)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialog)

        val userId = intent.extras?.getLong(ARG_USER_ID) ?: throw Exception("User id is null")

        val factory = Injection.provideDialogViewModelFactory(appBase, userId)
        viewModel = ViewModelProviders.of(this, factory).get(DialogViewModel::class.java)

        initToolbar(toolbar)

        val currentUserId = appBase.authorizationManager.getAuthorizedUserId()
        if (currentUserId == null) {
            Log.e(TAG, "Current user id is empty")
            finish()
            return
        }

        btnAttachments.setOnClickListener {
            if (!canClick()) return@setOnClickListener

            if (viewModel.getAvailableAttachmentsCount() <= 0) {
                showToast(R.string.you_have_added_maximum_amount_of_attachments)
                return@setOnClickListener
            }

            openAddAttachmentActivity()
        }
        btnSend.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            sendMessage(viewModel)
        }
        toolbar_layout.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            viewModel.openUserProfile()
        }
        btnRemoveReply.setOnClickListener { viewModel.setReplyTo(null) }

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

        // Check if it is share intent
        handleIntent()

        supportGifKeyboard()

        val attachmentsAdapter = initRVAttachments(viewModel)

        subscribeUI(viewModel, attachmentsAdapter)
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

    private fun initRVAttachments(viewModel: DialogViewModel): AttachmentsAdapter {
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
        // TODO: Handle big files (>1Gb)
        val file = File(uri.path)
        val bytes = FileUtils.readBytesFromUri(this, uri)
        bytes?.let {
            uploadFile(it, file.name, false, Attachment.Type.PICTURE)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.initTimer()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.dialog_menu, menu)
        miProtectedConversation = menu?.findItem(R.id.mi_chat_security)
        return super.onCreateOptionsMenu(menu)
    }

    private fun subscribeUI(viewModel: DialogViewModel, attachmentsAdapter: AttachmentsAdapter) {
        viewModel.subscribeOnEvents(this)

        viewModel.userModel.observe(this, Observer { user ->
            Log.d(TAG, "Observing user")
            if (user != null) {
                setUser(user)
            }
        })

        viewModel.dialog.observe(this, Observer { dialog ->
            Log.d(TAG, "Observing dialog")
            dialog?.let {
                Log.d(TAG, "Dialog is not empty")
                openMessagesFragment(it)

                // If it will be problem, then create an activity field to store dialog_id from here
                // and cancel notifications from onResume
                MyNotificationManager.cancelMessageNotification(this, it.id.toInt())
            } ?: openDialogEmptyFragment()
        })

        viewModel.protectedConversation.observe(this, Observer {
            miProtectedConversation?.isChecked = it != null

            val lockIconRes =
                if (it != null) R.drawable.ic_lock_gray else R.drawable.ic_lock_open_gray
            miProtectedConversation?.setIcon(lockIconRes)

            val btnSendIcon =
                if (it != null) R.drawable.ic_send_as_encrypted else R.drawable.ic_send
            btnSend.setImageResource(btnSendIcon)

            val btnSendColor =
                if (it != null) R.color.send_encrypted_button_color else R.color.primary_button_color
            ImageViewCompat.setImageTintList(
                btnSend,
                ContextCompat.getColorStateList(this, btnSendColor)
            )

            pbMessageIsSending.indeterminateTintList =
                ContextCompat.getColorStateList(this, btnSendColor)
        })

        viewModel.openUserProfileEvent.observe(this, Observer { userId ->
            UserProfileActivity.startActivity(this, userId)
        })

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

        viewModel.clearMessageTextEvent.observe(this, Observer {
            etMessageText.text.clear()
        })

        viewModel.startSecretDialogEvent.observe(this, Observer {
            viewModel.startSecretDialog(it)
        })

        viewModel.openGalleryEvent.observe(this, Observer {
            ConversationGalleryActivity.start(this, it, ConversationType.DIALOG)
        })

        appBase.getWebSocketService().getConnectionStatus().observe(this, Observer {
            viewModel.setConnected(it)
            tvConnectionStatus.visibility = if (it) View.INVISIBLE else View.VISIBLE
            tvUserStatus.visibility = if (it) View.VISIBLE else View.INVISIBLE
            btnSend.isEnabled = it
        })

        viewModel.verifyEncryptionKeyEvent.observe(this, Observer {
            val intent = CheckEncryptionKeyActivity.getIntent(this, it)
            startActivityForResult(intent, REQUEST_CODE_CHECK_ENCRYPTION_KEY)
        })

        viewModel.invalidateMessagesEvent.observe(this, Observer {
            messagesFragment?.invalidateMessages()
        })

        viewModel.authorizedEvent.observe(this, Observer {
            Log.d(TAG, "Authorized Event")
            messagesFragment?.invalidateMessages()
            viewModel.updateUser()
            viewModel.checkForKeys()
        })

        viewModel.draftMessage.observe(this, Observer {
            it?.let {
                etMessageText.setText(it.text)
                etMessageText.setSelection(it.text.length)
            }
        })

        viewModel.generateShortKeysEvent.observe(this, Observer {
            // start keys generator service
            startService(
                AsymmetricKeysGeneratorService.getIntent(
                    this,
                    AsymmetricKeysGeneratorService.ACTION_GET_SHORT_KEYS
                )
            )
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
            Log.d(TAG, "User actions size - ${it.size}")
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

    private fun showUserAction(userAction: UserActionModel) {
        tvUserAction.setText(userAction.getActionLabelRes())
        tvUserAction.visibility = View.VISIBLE
        tvUserStatus.visibility = View.INVISIBLE
    }

    private fun hideUserAction() {
        tvUserAction.visibility = View.INVISIBLE
        tvUserStatus.visibility = View.VISIBLE
    }

    private fun setUser(userModel: UserModel) {
        tvPhotoLabel.text = userModel.getPhotoLabel()
        Glide.with(this)
            .load(userModel.user.getPhotoUrl())
            .into(ivUserAvatar)

        tvUserName.text = userModel.getDisplayName() ?: getString(R.string.name_is_hidden)

        tvUserStatus.text = if (userModel.user.isOnline()) {
            getString(R.string.online)
        } else {
            userModel.user.online?.let {
                getString(
                    R.string.last_online,
                    android.text.format.DateUtils.getRelativeDateTimeString(
                        this,
                        userModel.user.online!! * 1000,
                        0,
                        DateUtils.WEEK_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                    )
                )
            } ?: getString(R.string.last_online_no_information)
        }

        tvUserStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (userModel.user.isOnline()) R.color.colorPrimary else R.color.colorGray
            )
        )
    }

    override fun onPause() {
        super.onPause()
        if (etMessageText.text.isBlank()) {
            viewModel.deleteDrafts()
        } else {
            viewModel.saveDraft(etMessageText.text.toString())
        }

        viewModel.cancelTimer()
    }

    private fun replyToMessage(messageModel: MessageModel) {
        viewModel.setReplyTo(messageModel)
    }

    private fun forwardMessage(messageModel: MessageModel) {
        viewModel.pickConversationForForward(listOf(messageModel))
    }

    @Deprecated("We do not need this method no more")
    private fun saveFileToDownloads(messageModel: MessageModel) {
        viewModel.copyFileToDownloads(messageModel.getAttachment())
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

    private fun deleteMessage(messageModel: MessageModel) {
        AlertDialog.Builder(this)
            .setTitle(R.string.attention)
            .setMessage(R.string.do_you_want_delete_this_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteMessages(listOf(messageModel))
            }.show()
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

    private fun sendMessage(viewModel: DialogViewModel) {
        viewModel.btnSendMessageClicked()
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.mi_show_encryption_key)?.isVisible = viewModel.isProtectedDialog()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.mi_chat_security -> {
                if (viewModel.isProtectedDialog()) {
                    viewModel.endSecretDialog()
                } else {
                    viewModel.getMyLastKeys(appBase.authorizationManager.getAuthorizedUserId()!!)
                }
                true
            }

            R.id.mi_show_encryption_key -> {
                viewModel.showEncryptionKey()
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

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openAddAttachmentActivity() {
        startActivityForResult(
            AddAttachmentActivity.getIntent(
                this,
                ConversationType.DIALOG,
                false,
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

                            // TODO: Looks like shit. Need to move this to view model
                            if (viewModel.isProtectedDialog()) {
                                imagePaths.firstOrNull()?.let {
                                    val file = File(it)
                                    uploadFile(file, true, Attachment.Type.PICTURE, true)
                                }
                            } else {
                                val attachmentModels =
                                    imagePaths.map { AttachmentModel(Attachment.Type.PICTURE, it) }

                                viewModel.addAttachments(attachmentModels)
                            }
                        } else if (data.hasExtra(FilesFragment.SELECTED_FILE_PATHS)) {
                            val filePaths =
                                data.getStringArrayListExtra(FilesFragment.SELECTED_FILE_PATHS)

                            // TODO: Looks like shit. Need to move this to view model
                            if (viewModel.isProtectedDialog()) {
                                filePaths.firstOrNull()?.let {
                                    val file = File(it)
                                    uploadFile(file, true, Attachment.Type.FILE, true)
                                }
                            } else {
                                val attachmentModels =
                                    filePaths.map { AttachmentModel(Attachment.Type.FILE, it) }

                                viewModel.addAttachments(attachmentModels)
                            }
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

            REQUEST_CODE_CHECK_ENCRYPTION_KEY -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val hash = data.extras?.getString(CheckEncryptionKeyActivity.ARG_HASH)
                        ?: throw Exception("Hash is null")
                    hash.let {
                        val multiFormatWriter = MultiFormatWriter()
                        val size = 256
                        try {
                            val bitMatrix =
                                multiFormatWriter.encode(it, BarcodeFormat.QR_CODE, size, size)
                            val barcodeEncoder = BarcodeEncoder()
                            val bitmap = barcodeEncoder.createBitmap(bitMatrix)

                            val file = ImageUtils.qrCodeToFile(this, bitmap)
                            uploadFile(file, false, Attachment.Type.PICTURE, false)
                        } catch (e: WriterException) {
                            e.printStackTrace()
                            showError(R.string.unknown_error)
                        }
                    }
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

    private fun openImage(imageByteArray: ByteArray) {
        Log.d(TAG, "Decrypted image opened. Set secure flags")
        setSecureFlag()
        StfalconImageViewer.Builder(this, listOf(imageByteArray)) { imageView, image ->
            Glide.with(this)
                .load(image)
                .error(R.drawable.ic_error)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        showError(R.string.failed_to_open_file)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
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

    private fun uploadFile(
        file: File,
        isDocument: Boolean,
        attachmentType: Int,
        encrypt: Boolean = true
    ) {
        if (viewModel.isProtectedDialog() && encrypt) {
            viewModel.encryptFile(file, object : DialogViewModel.EncryptFileCallback {
                override fun encrypted(data: ByteArray) {
                    uploadFile(data, file.name, true, attachmentType)
                }

                override fun error() {
                    showError(R.string.failed_to_send_encrypted_file)
                }
            })
        } else {
            uploadFile(file.readBytes(), file.name, isDocument, attachmentType, encrypt)
        }
    }

    private fun uploadFile(
        data: ByteArray,
        fileName: String,
        isDocument: Boolean,
        attachmentType: Int,
        encrypt: Boolean = true
    ) {
        viewModel.uploadFile(data, fileName, isDocument) { fileInfo ->
            if (viewModel.isProtectedDialog() && encrypt) {
                viewModel.sendEncryptedFile(fileInfo, attachmentType)
            } else {
                viewModel.sendFile(fileInfo, attachmentType)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: Message) {
        val uid = intent.extras?.getLong(ARG_USER_ID) ?: throw Exception("Uid is null")
        if ((appBase.appInBackground || remoteMessage.conversationId != uid && remoteMessage.senderId != uid) &&
            remoteMessage.senderId != appBase.authorizationManager.getAuthorizedUserId()
        ) {
            MyNotificationManager.showNewMessageNotification(appBase, remoteMessage)
        }
    }

    private fun openMessagesFragment(dialog: Dialog) {
        if (messagesFragment != null) return

        messagesFragment = MessagesFragment.get(dialog.id, ConversationType.DIALOG)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.messages_container, messagesFragment!!)
        transaction.commit()
    }

    private fun openDialogEmptyFragment() {
        val dialogEmptyFragment = DialogEmptyFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.messages_container, dialogEmptyFragment)
        transaction.commit()
    }

    override fun onMessageClick(messageModel: MessageModel) {
        val popupMenu = PopupMenu(this, null)
        val menu = popupMenu.menu
        popupMenu.menuInflater.inflate(R.menu.message_menu, menu)

        // Display copy option
        menu.findItem(R.id.mi_copy).isVisible =
            messageModel.getMessage().text?.isNotBlank() ?: false

        // Display forward option
        menu.findItem(R.id.mi_forward).isVisible = messageModel.canBeForwarded()

        // Display reply option
        menu.findItem(R.id.mi_reply).isVisible = messageModel.canBeReplied()

        val dialog = BottomSheetBuilder(this)
            .setMode(BottomSheetBuilder.MODE_LIST)
            .setMenu(menu)
            .setItemClickListener {
                val id = it.itemId
                when (id) {
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

    override fun playVoice(filePath: String, callback: () -> Unit) {
        viewModel.playVoice(filePath, callback)
    }

    override fun pauseVoice() {
        viewModel.pauseVoice()
    }

    override fun decryptFile(
        file: File,
        senderId: Long,
        keyId: Long,
        signKeyId: Long,
        decryptFileCallback: DialogViewModel.DecryptFileCallback
    ) {
        viewModel.decryptFile(file, senderId, keyId, signKeyId, decryptFileCallback)
    }

    override fun onEncryptedImageClick(image: ByteArray) {
        openImage(image)
    }

    override fun openEncryptedFile(file: File, senderId: Long, keyId: Long, signKeyId: Long) {
        val fileType = FileExtensionHelper.getFileType(file.extension)

        when (fileType) {
            FileExtensionHelper.FileType.PDF -> {
                PDFViewerActivity.open(
                    this@DialogActivity,
                    file.absolutePath,
                    senderId,
                    keyId,
                    signKeyId
                )
            }

            FileExtensionHelper.FileType.Image, FileExtensionHelper.FileType.GIF -> {
                showLoadingDialog(R.string.file_decryption)
                viewModel.decryptFile(
                    file,
                    senderId,
                    keyId,
                    signKeyId,
                    object : DialogViewModel.DecryptFileCallback {
                        override fun decrypted(data: ByteArray) {
                            hideLoadingDialog()
                            openImage(data)
                        }

                        override fun error() {
                            hideLoadingDialog()
                            showError(R.string.failed_to_decrypt_file)
                        }
                    })
            }

            else -> {
                showError(R.string.unsupported_file_type)
            }
        }
    }
}