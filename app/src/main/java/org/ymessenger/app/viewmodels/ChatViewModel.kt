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

package org.ymessenger.app.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import org.ymessenger.app.R
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.Limitations
import org.ymessenger.app.data.local.db.entities.ChatUser
import org.ymessenger.app.data.local.db.entities.DraftMessage
import org.ymessenger.app.data.local.db.entities.Keys
import org.ymessenger.app.data.local.db.entities.ProtectedConversation
import org.ymessenger.app.data.local.db.models.ChatUserModel
import org.ymessenger.app.data.local.db.models.MessageModel
import org.ymessenger.app.data.remote.Config
import org.ymessenger.app.data.remote.entities.*
import org.ymessenger.app.data.remote.requests.AddUsersChats
import org.ymessenger.app.data.remote.requests.ChangeChatUsers
import org.ymessenger.app.data.remote.requests.Polling
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.*
import org.ymessenger.app.helpers.*
import org.ymessenger.app.interfaces.SimpleResultCallback
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.models.AttachmentModel
import org.ymessenger.app.utils.SingleLiveEvent
import java.io.File
import java.io.IOException
import java.util.*

class ChatViewModel(
    private val chatId: Long,
    private val chatRepository: ChatRepository,
    private val chatUserRepository: ChatUserRepository,
    private val chatPreviewRepository: ChatPreviewRepository,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
    private val fileRepository: FileRepository,
    private val attachmentRepository: AttachmentRepository,
    private val protectedConversationRepository: ProtectedConversationRepository,
    private val pollRepository: PollRepository,
    private val authorizationManager: AuthorizationManager,
    private val draftMessageRepository: DraftMessageRepository,
    private val encryptionWrapper: EncryptionWrapper,
    private val keysRepository: KeysRepository,
    private val imageCompressor: ImageCompressor,
    private val audioRecorder: AudioRecorder,
    private val voicePlayerHelper: VoicePlayerHelper,
    private val userActionRepository: UserActionRepository
) : BaseViewModel(), ConversationViewModel {

    val chat = chatRepository.getChat(chatId)
    val chatUsers = Transformations.map(chatUserRepository.getChatUserModels(chatId)) {
        checkForNullUsers(it)
        it
    }
    val protectedConversation =
        protectedConversationRepository.getProtectedConversation(chatId, ConversationType.CHAT)
    val currentChatUser =
        chatUserRepository.getChatUser(chatId, authorizationManager.getAuthorizedUserId()!!)


    var replyTo = MutableLiveData<MessageModel>()

    val messageIsSending = MutableLiveData<Boolean>()

    val openGroupInfoEvent = SingleLiveEvent<Long>()
    val openGalleryEvent = SingleLiveEvent<Long>()
    val pickConversationForForwardEvent = SingleLiveEvent<Void>()
    val leaveChatEvent = SingleLiveEvent<Void>()

    val clearMessageTextEvent = SingleLiveEvent<Void>()

    private val messagesToForward = arrayListOf<MessageModel>()

    val draftMessage = draftMessageRepository.getDraftMessage(chatId, ConversationType.CHAT)

    private var connected: Boolean = false

    val authorizedEvent = authorizationManager.authorizedEvent

    private var messageText: String? = null

    val attachments = MutableLiveData<List<AttachmentModel>>()

    val messageIsEmpty = MutableLiveData<Boolean>(true)
    val voiceMessageIsRecording = MutableLiveData<Boolean>()

    private var actionSentTime: Long = 0

    private var voiceRecordingActionTimer: Timer? = null

    val lastUserActions = userActionRepository.getLastUserActionModels(
        chatId,
        ConversationType.CHAT,
        System.currentTimeMillis() / 1000L
    )

    // FIXME: EXPERIMENTAL
    private var voiceStopCallback: (() -> Unit)? = null

    init {
        if (!authorizationManager.isAuthorized) {
            authorizationManager.tryAuthorize()
        }

        attachments.observeForever {
            checkMessageIsEmpty()
        }

        voicePlayerHelper.initMediaPlayer(object : VoicePlayerHelper.PlaybackCallback {
            override fun onComplete() {
                voiceStopCallback?.invoke()
            }

            override fun onError() {
                showError(R.string.unknown_error)
            }
        })
    }

    private fun checkForNullUsers(chatUserModels: List<ChatUserModel>) {
        val usersId = hashSetOf<Long>()
        for (chatUserModel in chatUserModels) {
            if (chatUserModel.getUser() == null) {
                usersId.add(chatUserModel.chatUser.userId)
            }
        }

        if (usersId.isNotEmpty()) {
            Log.d(TAG, "${usersId.size} users is missing. Getting them")
            userRepository.getUsers(usersId.toList())
        } else {
            Log.d(TAG, "All users are already downloaded")
        }
    }

    fun switchProtected() {
        if (protectedConversation.value != null) {
            protectedConversationRepository.delete(protectedConversation.value!!)
        } else {
            protectedConversationRepository.insert(
                ProtectedConversation(
                    chatId,
                    ConversationType.CHAT
                )
            )
        }
    }

    fun setMessageText(text: String?) {
        messageText = text
        checkMessageIsEmpty()
        if (!text.isNullOrEmpty()) {
            sendTypingAction()
        }
    }

    private fun sendTypingAction() {
        sendAction(UserActionType.TYPING)
    }

    private fun sendVoiceRecordingAction() {
        sendAction(UserActionType.RECORDING_VOICE)
        voiceRecordingActionTimer?.cancel()
        voiceRecordingActionTimer = Timer()
        voiceRecordingActionTimer?.schedule(object : TimerTask() {
            override fun run() {
                if (voiceMessageIsRecording.value == true) {
                    sendVoiceRecordingAction()
                }
            }
        }, Config.USER_ACTION_SEND_PERIOD * 1000L)
    }

    private fun sendAction(action: Int) {
        val now = System.currentTimeMillis() / 1000L
        if (now - actionSentTime < Config.USER_ACTION_SEND_PERIOD) return

        userActionRepository.sendUserAction(chatId, ConversationType.CHAT, action) {
            // action was sent
        }

        actionSentTime = System.currentTimeMillis() / 1000L
    }

    fun sendFile(fileInfo: FileInfo, attachmentType: Int) {
        if (!hasConnection()) return

        // Prepare attachment
        val attachment = Attachment(
            attachmentType,
            null,
            0,
            fileInfo.fileId,
            null
        )

        // Generate message object
        val message = Message.Builder(ConversationType.CHAT, chatId)
            .setAttachments(listOf(attachment))
            .setReplyTo(replyTo.value?.getMessage()?.globalId)
            .build()

        sendMessage(message)
    }

    fun votePoll(optionId: Int, poll: Poll, callback: (() -> Unit)) {
        prepareVote(optionId, poll, callback)
    }

    private fun prepareVote(optionId: Int, poll: Poll, callback: () -> Unit) {
        if (poll.signRequired) {
            val authorizedUserId = authorizationManager.getAuthorizedUserId()
            if (authorizedUserId == null) {
                showError(R.string.user_is_not_authorized)
                return
            }

            keysRepository.getMyLastKey(
                authorizedUserId,
                true,
                object : KeysRepository.GetKeyResult {
                    override fun result(keys: Keys) {
                        try {
                            val yEncrypt = encryptionWrapper.getYEncrypt()

                            val signData =
                                Polling.Option.SignData(poll.pollId!!, authorizedUserId, optionId)
                            val signDataStr = signData.getJson()
                            Log.d(TAG, "signDataStr: $signDataStr")
                            yEncrypt.privateSignKeyToSend = keys.privateKey
                            val signedData = yEncrypt.signMsg(1, 1, 0, signDataStr.toByteArray())
                            val signedDataBase64 = EncryptHelper.bytesToBase64(signedData)
                            val sign = Polling.Option.Sign(keys.id, signedDataBase64)

                            val option = Polling.Option(optionId, sign)
                            sendVote(poll, option, callback)
                        } catch (e: Exception) {
                            showError(R.string.error_while_signing_data)
                            return
                        }
                    }

                    override fun error() {
                        showError(R.string.there_is_no_available_sign_key)
                    }
                })

        } else {
            val option = Polling.Option(optionId, null)
            sendVote(poll, option, callback)
        }
    }

    private fun sendVote(poll: Poll, option: Polling.Option, callback: () -> Unit) {
        val polling =
            Polling(poll.pollId!!, poll.conversationId, poll.conversationType, listOf(option))
        pollRepository.votePoll(polling, object : SuccessErrorCallback {
            override fun success() {
                callback.invoke()
            }

            override fun error(error: ResultResponse) {
                showErrorFromCode(error.errorCode)
            }
        })
    }

    private fun sendMessage(message: Message) {
        messageIsSending.postValue(true)
        messageRepository.sendMessage(message, object : MessageRepository.SendMessageCallback {
            override fun sent(message: Message) {
                messageIsSending.postValue(false)
                clearMessageTextEvent.call()
                attachments.postValue(listOf())
            }

            override fun error(error: ResultResponse) {
                messageIsSending.postValue(false)
                showError(R.string.failed_to_send_message)
            }
        })
        replyTo.postValue(null)
    }

    fun uploadFile(file: File, isDocument: Boolean, success: (fileInfo: FileInfo) -> Unit) {
        uploadFile(file.readBytes(), file.name, isDocument, success)
    }

    fun uploadFile(
        data: ByteArray,
        fileName: String,
        isDocument: Boolean,
        success: (fileInfo: FileInfo) -> Unit
    ) {
        if (!hasConnection()) return

        messageIsSending.postValue(true)
        fileRepository.uploadFile(
            data,
            fileName,
            isDocument,
            object : FileRepository.UploadFileCallback {
                override fun uploaded(file: org.ymessenger.app.data.remote.responses.File) {
                    messageIsSending.postValue(false)
                    success(file.fileInfo)
                }

                override fun errorLargeSize() {
                    messageIsSending.postValue(false)
                    showError(R.string.file_size_is_too_large_error)
                }

                override fun error() {
                    messageIsSending.postValue(false)
                    showError(R.string.file_upload_error)
                }
            })
    }

    fun downloadFile(fileInfo: FileInfo, success: (savedAt: String) -> Unit) {
        if (!hasConnection()) return

        fileRepository.downloadFile(fileInfo, object : FileRepository.DownloadFileCallback {
            override fun downloaded(path: String) {
                success(path)
            }

            override fun error() {
                showError(R.string.download_file_error)
            }
        })
    }

    fun saveAttachmentFilePath(attachmentId: Long, path: String?) {
        attachmentRepository.updateSavedAt(attachmentId, path)
    }

    fun openGroupInfo() {
        if (isChatDeleted()) return
        openGroupInfoEvent.postValue(chatId)
    }

    fun joinChat() {
        if (!hasConnection()) return

        startLoading()
        val addUsersChats =
            AddUsersChats(listOf(chatId), listOf(authorizationManager.getAuthorizedUserId()!!))
        chatUserRepository.addUsersToChats(addUsersChats, object : SuccessErrorCallback {
            override fun success() {
                endLoading()
                chatUserRepository.getChatUsersByChat(chatId)
            }

            override fun error(error: ResultResponse) {
                endLoading()
                showErrorFromCode(error.errorCode)
            }
        })
    }

    fun leaveChat(userId: Long) {
        if (!hasConnection()) return

        startLoading()
        val chatUser = ChatUser(
            chatId,
            userId,
            true,
            null,
            null,
            null
        )
        val changeChatUsers = ChangeChatUsers(listOf(chatUser), chatId)
        chatUserRepository.changeChatUsers(
            changeChatUsers,
            object : ChatUserRepository.ChangeChatUsersCallback {
                override fun success(editedChatUsers: List<ChatUser>) {
                    endLoading()
                    chatPreviewRepository.deleteChatPreview(chatId, ConversationType.CHAT)
                    leaveChatEvent.call()
                }

                override fun error(error: ResultResponse) {
                    endLoading()
                    showError(R.string.failed_to_leave_chat)
                }
            })
    }

    fun isMember(): Boolean {
        return currentChatUser.value != null
    }

    fun setReplyTo(messageModel: MessageModel?) {
        replyTo.postValue(messageModel)
    }

    override fun deleteMessages(messages: List<MessageModel>) {
        if (!hasConnection()) return

        val messagesId = messages.map { it.getMessage().globalId }
        messageRepository.deleteMessages(chatId, ConversationType.CHAT, messagesId)
    }

    fun pickConversationForForward(messagesToForward: List<MessageModel>) {
        this.messagesToForward.clear()
        this.messagesToForward.addAll(messagesToForward)
        pickConversationForForwardEvent.call()
    }

    fun forwardMessagesTo(conversationType: Int, identifier: Long) {
        if (!hasConnection()) return

        if (messagesToForward.isEmpty())
            return


        // prepare payload
        val firstMessage = messagesToForward.first()
        val messagesConversationType: Int
        val messagesConversationId: Long
        val messageIds: List<String>
        if (firstMessage.isForwarded()) {
            val forwardedMessageInfo = firstMessage.getForwardedMessageInfo()!!.forwardedMessageInfo
            messagesConversationType = forwardedMessageInfo.forwardedFromConversationType
            messagesConversationId = forwardedMessageInfo.forwardedFromConversationId
            messageIds =
                messagesToForward.map { it.getForwardedMessageInfo()!!.forwardedMessageInfo.forwardedFromMessageId }
        } else {
            messagesConversationType = firstMessage.getMessage().conversationType
            messagesConversationId = firstMessage.getMessage().conversationId
            messageIds = messagesToForward.map { it.getMessage().globalId }
        }

        val forwardedMessagesInfo =
            ForwardedMessagesInfo(messagesConversationType, messagesConversationId, messageIds)

        // build attachment object
        val attachment = Attachment(
            Attachment.Type.FORWARDED_MESSAGES,
            null,
            0,
            Gson().toJson(forwardedMessagesInfo),
            null
        )

        // build message object
        val message = Message.Builder(conversationType, identifier)
            .setAttachments(listOf(attachment))
            .build()

        messageRepository.sendMessage(message, object : MessageRepository.SendMessageCallback {
            override fun sent(message: Message) {
                Log.d(TAG, "Messages was forwarded")
                if (conversationType != message.conversationType || identifier != chatId) {
                    showToast(R.string.message_was_forwarded)
                }
            }

            override fun error(error: ResultResponse) {
                showError(R.string.failed_to_send_message)
            }
        })
    }

    fun clearMessagesToForward() {
        this.messagesToForward.clear()
    }

    @Deprecated("We do not need this method no more")
    fun clearFileFromCache(attachment: org.ymessenger.app.data.local.db.entities.Attachment) {
        attachment.savedAt?.let {
            fileRepository.clearFromCache(it)
            saveAttachmentFilePath(attachment.id!!, null)
        }
    }

    @Deprecated("We do not need this method no more")
    fun copyFileToDownloads(attachment: org.ymessenger.app.data.local.db.entities.Attachment) {
        if (attachment.savedAt != null) {
            val fileInfo = attachment.getPayloadAsFile() ?: return
            fileRepository.copyToDownloads(
                attachment.savedAt!!,
                fileInfo.filename,
                object : SimpleResultCallback {
                    override fun success() {
                        showToast(R.string.file_has_been_copied_to_downloads)
                    }

                    override fun error() {
                        showError(R.string.failed_to_copy_file)
                    }
                })
        }
    }

    /**
     * For testing purposes
     */
    fun clearMessagesLocally() {
        messageRepository.clearMessagesLocally(ConversationType.CHAT, chatId)
    }

    fun isChatDeleted() = chat.value?.deleted ?: true

    fun setConnected(status: Boolean) {
        connected = status
    }

    private fun hasConnection(): Boolean {
        if (!connected) {
            showError(R.string.connection_is_lost)
        }

        return connected
    }

    fun openGallery() {
        openGalleryEvent.postValue(chatId)
    }

    fun saveDraft(text: String) {
        if (text.isBlank()) return

        val draftMessage =
            DraftMessage(chatId, ConversationType.CHAT, text, System.currentTimeMillis() / 1000)
        draftMessageRepository.upsert(draftMessage)
    }

    fun deleteDrafts() {
        draftMessage.value?.let {
            draftMessageRepository.delete(it)
        }
    }

    fun addAttachments(attachmentModels: List<AttachmentModel>) {
        val newAttachmentList = getAttachmentListAsMutable()

        // reorder attachments by type [Images, Files, Polls]
        // attachmentsModels supposed to be same type
        val firstAttachment = attachmentModels.firstOrNull()
        firstAttachment?.let {
            val indexToInsert =
                getIndexToInsertAttachmentBySortType(firstAttachment.sortType)
            newAttachmentList.addAll(indexToInsert, attachmentModels)
        }

        attachments.value = newAttachmentList
    }

    private fun getIndexToInsertAttachmentBySortType(sortType: Int): Int {
        val attachmentList = attachments.value ?: return 0
        if (attachmentList.isEmpty()) return 0

        var indexToInsert = 0

        for ((index, attachment) in attachmentList.withIndex()) {
            indexToInsert = index
            if (attachment.sortType > sortType) {
                break
            }

            if (index == attachmentList.size - 1 && sortType >= attachment.sortType) {
                indexToInsert++
            }
        }

        return indexToInsert
    }

    fun hasPollAttachment(): Boolean {
        val attachmentList = attachments.value ?: return false

        for (attachment in attachmentList) {
            if (attachment.type == Attachment.Type.POLL) {
                return true
            }
        }

        return false
    }

    fun getAttachmentsCount(): Int {
        return attachments.value?.size ?: 0
    }

    fun getAvailableAttachmentsCount(): Int {
        return Limitations.MAX_ATTACHMENTS - getAttachmentsCount()
    }

    fun deleteAttachment(attachmentModel: AttachmentModel) {
        val newAttachmentList = getAttachmentListAsMutable()
        newAttachmentList.remove(attachmentModel)
        attachments.value = newAttachmentList
    }

    private fun getAttachmentListAsMutable(): MutableList<AttachmentModel> {
        val attachmentList = attachments.value ?: listOf()
        val mutableAttachmentList = mutableListOf<AttachmentModel>()
        mutableAttachmentList.addAll(attachmentList)

        return mutableAttachmentList
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val attachmentList = getAttachmentListAsMutable()
        val itemToMove = attachmentList[fromPosition]
        attachmentList.removeAt(fromPosition)
        attachmentList.add(toPosition, itemToMove)

        attachments.value = attachmentList
    }

    // TODO: rethink how to send messages with attachments
    //      1. Check if there are any attachments
    //      2. Iterate through list of attachments
    //      3. Upload files and save fileIds
    //      4. When all attachments are prepared, build message with attachments
    //      5. Send

    private fun prepareAttachments(index: Int = 0) {
        val attachmentList = attachments.value ?: return

        if (index == 0) {
            startLoading(R.string.uploading_attachments)
        }

        if (index >= attachmentList.size) {
            // All attachments are prepared
            endLoading()
            buildMessageToSend()
            return
        }

        val attachmentModel = attachmentList[index]

        if (attachmentModel.readyToSend) {
            prepareAttachments(index + 1)
            return
        }

        when (attachmentModel.type) {
            Attachment.Type.PICTURE -> {
                // Compress image, upload file and go next
                val originalFile = File(attachmentModel.filePath)
                val fileToUpload = if (FileExtensionHelper.isGIF(originalFile.extension)) {
                    originalFile
                } else {
                    imageCompressor.compress(originalFile)
                }

                uploadFile(fileToUpload, false) {
                    attachmentModel.fileId = it.fileId
                    attachmentModel.readyToSend = true
                    // upload next
                    prepareAttachments(index + 1)
                }
            }
            Attachment.Type.FILE -> {
                // Upload file and go next
                val file = File(attachmentModel.filePath)
                uploadFile(file, true) {
                    attachmentModel.fileId = it.fileId
                    attachmentModel.readyToSend = true
                    // upload next
                    prepareAttachments(index + 1)
                }
            }
            else -> {
                attachmentModel.readyToSend = true
                // upload next
                prepareAttachments(index + 1)
            }
        }
    }

    fun prepareMessageToSend() {
        if (!hasConnection()) return

        if (attachments.value.isNullOrEmpty()) {
            buildMessageToSend()
        } else {
            prepareAttachments()
        }
    }

    private fun buildMessageToSend() {
        val attachmentList = attachments.value ?: listOf()

        if (attachmentList.isEmpty() && messageText.isNullOrEmpty()) {
            Log.w(TAG, "Can't send empty message")
            return
        }

        val attachmentsToSend = mutableListOf<Attachment>()
        for (attachment in attachmentList) {
            if (!attachment.readyToSend) continue

            when (attachment.type) {
                Attachment.Type.PICTURE, Attachment.Type.FILE -> {
                    attachmentsToSend.add(
                        Attachment(
                            attachment.type,
                            null,
                            0,
                            attachment.fileId!!,
                            null
                        )
                    )
                }

                Attachment.Type.POLL -> {
                    attachmentsToSend.add(
                        Attachment(
                            attachment.type,
                            null,
                            0,
                            attachment.pollJson!!,
                            null
                        )
                    )
                }

                // TODO: add more types
            }
        }

        val messageBuilder = Message.Builder(ConversationType.CHAT, chatId)
            .setReplyTo(replyTo.value?.getMessage()?.globalId)
            .setAttachments(attachmentsToSend)
            .setText(messageText)

        sendMessage(messageBuilder.build())
    }

    private fun checkMessageIsEmpty() {
        val attachmentsAreEmpty = attachments.value.isNullOrEmpty()
        messageIsEmpty.postValue(attachmentsAreEmpty && messageText.isNullOrBlank())
    }

    private fun setVoiceRecording(recording: Boolean) {
        voiceMessageIsRecording.postValue(recording)
    }

    fun startRecordingVoiceMessage() {
        try {
            Log.d(TAG, "Start recording voice message")
            audioRecorder.startRecording()
            setVoiceRecording(true)
            sendVoiceRecordingAction()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stopRecordingVoiceMessage() {
        try {
            Log.d(TAG, "Stop recording voice message")
            audioRecorder.stopRecording()
            val outputFilename = audioRecorder.getRecordingFilename()
            outputFilename?.let {
                sendVoiceMessage(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Something went wrong")
            e.printStackTrace()
        }
        setVoiceRecording(false)
    }

    private fun sendVoiceMessage(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            uploadFile(file, true) {
                sendFile(it, Attachment.Type.VOICE)
            }
        } else {
            showToast("File isn't exist")
        }
    }

    fun cancelVoiceMessage() {

    }

    fun playVoice(filePath: String, callback: () -> Unit) {
        if (voicePlayerHelper.isSourceSet(filePath)) {
            voicePlayerHelper.play()
            // FIXME: EXPERIMENTAL
            this.voiceStopCallback = callback
        } else {
            if (voicePlayerHelper.isSourceSet()) {
                voicePlayerHelper.stop()
                // Stop previous voice
                voiceStopCallback?.invoke()
            }
            // FIXME: EXPERIMENTAL
            this.voiceStopCallback = callback
            voicePlayerHelper.setSource(filePath)
            voicePlayerHelper.play()
        }
    }

    fun pauseVoice() {
        voicePlayerHelper.pause()
    }

    class Factory(
        private val chatId: Long,
        private val chatRepository: ChatRepository,
        private val chatUserRepository: ChatUserRepository,
        private val chatPreviewRepository: ChatPreviewRepository,
        private val userRepository: UserRepository,
        private val messageRepository: MessageRepository,
        private val fileRepository: FileRepository,
        private val attachmentRepository: AttachmentRepository,
        private val protectedConversationRepository: ProtectedConversationRepository,
        private val pollRepository: PollRepository,
        private val authorizationManager: AuthorizationManager,
        private val draftMessageRepository: DraftMessageRepository,
        private val encryptionWrapper: EncryptionWrapper,
        private val keysRepository: KeysRepository,
        private val imageCompressor: ImageCompressor,
        private val audioRecorder: AudioRecorder,
        private val voicePlayerHelper: VoicePlayerHelper,
        private val userActionRepository: UserActionRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChatViewModel(
                chatId,
                chatRepository,
                chatUserRepository,
                chatPreviewRepository,
                userRepository,
                messageRepository,
                fileRepository,
                attachmentRepository,
                protectedConversationRepository,
                pollRepository,
                authorizationManager,
                draftMessageRepository,
                encryptionWrapper,
                keysRepository,
                imageCompressor,
                audioRecorder,
                voicePlayerHelper,
                userActionRepository
            ) as T
        }
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}