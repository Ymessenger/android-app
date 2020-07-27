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

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import org.ymessenger.app.R
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.EncryptedMessageType
import org.ymessenger.app.data.Limitations
import org.ymessenger.app.data.local.db.entities.*
import org.ymessenger.app.data.local.db.models.MessageModel
import org.ymessenger.app.data.remote.Config
import org.ymessenger.app.data.remote.WSResponse
import org.ymessenger.app.data.remote.entities.*
import org.ymessenger.app.data.remote.entities.Attachment
import org.ymessenger.app.data.remote.entities.Message
import org.ymessenger.app.data.remote.requests.Polling
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.repositories.*
import org.ymessenger.app.helpers.*
import org.ymessenger.app.interfaces.SimpleResultCallback
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.models.AttachmentModel
import org.ymessenger.app.utils.AppExecutors
import org.ymessenger.app.utils.SingleLiveEvent
import y.encrypt.YKeyException
import java.io.File
import java.io.IOException
import java.util.*

class DialogViewModel(
    private val userId: Long,
    private val userRepository: UserRepository,
    private val dialogRepository: DialogRepository,
    private val messageRepository: MessageRepository,
    private val fileRepository: FileRepository,
    private val attachmentRepository: AttachmentRepository,
    private val protectedConversationRepository: ProtectedConversationRepository,
    private val keysRepository: KeysRepository,
    private val symmetricKeyRepository: SymmetricKeyRepository,
    private val encryptionWrapper: EncryptionWrapper,
    private val pollRepository: PollRepository,
    private val settingsHelper: SettingsHelper,
    private val authorizationManager: AuthorizationManager,
    private val draftMessageRepository: DraftMessageRepository,
    private val imageCompressor: ImageCompressor,
    private val safeModeManager: SafeModeManager,
    private val audioRecorder: AudioRecorder,
    private val voicePlayerHelper: VoicePlayerHelper,
    private val userActionRepository: UserActionRepository
) : BaseViewModel(), ConversationViewModel {

    val currentUserId = authorizationManager.getAuthorizedUserId() ?: 0

    private val updateUserEvent = SingleLiveEvent<Void>().apply { call() }
    val userModel = Transformations.switchMap(updateUserEvent) {
        userRepository.getUserModel(userId)
    }
    val dialog = dialogRepository.getDialogByUser(userId)
    val protectedConversation = Transformations.switchMap(dialog) {
        it?.let {
            protectedConversationRepository.getProtectedConversation(it.id, ConversationType.DIALOG)
        }
    }

    val draftMessage = Transformations.switchMap(dialog) {
        it?.let {
            draftMessageRepository.getDraftMessage(it.id, ConversationType.DIALOG)
        }
    }

    private var messageText: String? = null

    var replyTo = MutableLiveData<MessageModel>()

    val messageIsSending = MutableLiveData<Boolean>()

    /**
     * Event for clearing message input text
     *
     * This need to be deleted, because while uploading photo and started to write new message,
     * this text will be deleted after photo will be delivered.
     */
    val clearMessageTextEvent = SingleLiveEvent<Void>()

    val startSecretDialogEvent = SingleLiveEvent<Keys>()

    val invalidateMessagesEvent = SingleLiveEvent<Void>()

    val generateShortKeysEvent = SingleLiveEvent<Void>()

    private val myLastSymmetricKey = Transformations.switchMap(dialog) {
        it?.let {
            symmetricKeyRepository.getLastKeyForDialogByUser(it.id, currentUserId)
        }
    }

    private val myLastSignKeys = keysRepository.getMyLastKeys(true)

    private var _myLastSymmetricKey: SymmetricKey? = null
    private var _myLastSignKeys: Keys? = null

    val openUserProfileEvent = SingleLiveEvent<Long>()
    val openGalleryEvent = SingleLiveEvent<Long>()
    val pickConversationForForwardEvent = SingleLiveEvent<Void>()

    val verifyEncryptionKeyEvent = SingleLiveEvent<Long>()

    private val messagesToForward = arrayListOf<MessageModel>()

    private var connected: Boolean = false

    val authorizedEvent = authorizationManager.authorizedEvent

    private lateinit var timer: Timer

    val attachments = MutableLiveData<List<AttachmentModel>>()

    private val decryptedFilesCache = hashMapOf<String, ByteArray>()

    val messageIsEmpty = MutableLiveData<Boolean>(true)
    val voiceMessageIsRecording = MutableLiveData<Boolean>()

    private var actionSentTime: Long = 0

    private var voiceRecordingActionTimer: Timer? = null

    val lastUserActions = Transformations.switchMap(dialog) {
        it?.let {
            val currentTime = System.currentTimeMillis() / 1000L
            userActionRepository.getLastUserActionModels(
                it.id,
                ConversationType.DIALOG,
                currentTime
            )
        }
    }

    // FIXME: EXPERIMENTAL
    private var voiceStopCallback: (() -> Unit)? = null

    val openConversationEvent = SingleLiveEvent<Pair<Int, Long>>()
    val finishActivityEvent = SingleLiveEvent<Void>()

    init {
        if (!authorizationManager.isAuthorized) {
            authorizationManager.tryAuthorize()
        } else {
            checkForKeys()
        }

        // If we have new symmetric key, then we need to invalidate messages
        myLastSymmetricKey.observeForever {
            if (it != null) {
                invalidateMessagesEvent.call()
            }
            _myLastSymmetricKey = it
        }
        myLastSignKeys.observeForever {
            _myLastSignKeys = it
        }

//        dialog.observeForever {
//            it?.let {
//                Log.d(TAG, "Dialog observed. Probably should be deleted")
//                messageRepository.getLastSymmetricKeysMessages(it.id, ConversationType.DIALOG)
//            }
//        }

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

    fun initTimer() {
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                Log.d(TAG, "Timer tick")
                Handler(Looper.getMainLooper()).post {
                    updateUser()
                }
            }
        }, Config.TIMER_UPDATE_PERIOD, Config.TIMER_UPDATE_PERIOD)
        Log.d(TAG, "Timer started")
    }

    fun cancelTimer() {
        timer.cancel()
        Log.d(TAG, "Timer was cancelled")
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

        val conversationId = dialog.value?.id ?: return

        userActionRepository.sendUserAction(conversationId, ConversationType.DIALOG, action) {
            // action was sent
        }

        actionSentTime = System.currentTimeMillis() / 1000L
    }

    private fun checkMessageIsEmpty() {
        val attachmentsAreEmpty = attachments.value.isNullOrEmpty()
        messageIsEmpty.postValue(attachmentsAreEmpty && messageText.isNullOrBlank())
    }

    fun getMyLastKeys(userId: Long) {
        keysRepository.getMyLastKey(userId, true, object : KeysRepository.GetKeyResult {
            override fun result(keys: Keys) {
                startSecretDialogEvent.postValue(keys)
            }

            override fun error() {
                Log.e(TAG, "Failed to get my last keys")
                showError(R.string.failed_to_start_encrypted_conversation)
            }
        })
    }

    // TODO: Maybe I should move this check to AuthorizationManager
    fun checkForKeys() {
        // We are looking for ENCRYPTION (not sign) keys, because if we don't have encryption key
        // nobody can send us an encrypted message
        if (currentUserId == 0L) {
            Log.e(TAG, "Current user id is empty")
            return
        }

        val isSign = false
        keysRepository.getMyLastKey(currentUserId, isSign, object : KeysRepository.GetKeyResult {
            override fun result(keys: Keys) {
                Log.d(TAG, "There is my asymmetric keys: keyId = ${keys.id}")

                // Checking for keys lifetime and if it is expired it's need to generate new keys
                if (EncryptHelper.isExpired(keys)) {
                    Log.d(TAG, "My asymmetric keys are expired. Generating new one...")
                    generateShortKeys()
                }
            }

            override fun error() {
                Log.e(TAG, "Failed to get my asymmetric keys. Generating new one...")
                generateShortKeys()
            }
        })
    }

    private fun generateShortKeys() {
        generateShortKeysEvent.call()
    }

    fun startSecretDialog(mySignKeys: Keys) {
        val dialogId = dialog.value?.id
        if (dialogId == null) {
            showError(R.string.start_a_dialog_first)
            return
        }

        if (!hasConnection()) return

        showEncryptionTurningOnDialog()

        if (EncryptHelper.isExpired(mySignKeys)) {
            prepareNewSignKeys {
                startSecretDialog(it)
            }

            return
        }

        prepareNewSymmetricKey(mySignKeys) {
            hideEncryptionTurningOnDialog()
            setProtectedFlag()
        }
    }

    private fun setProtectedFlag() {
        protectedConversationRepository.insert(
            ProtectedConversation(
                dialog.value!!.id,
                ConversationType.DIALOG
            )
        )
    }

    private fun showEncryptionTurningOnDialog() {
        startLoading(R.string.enabling_encryption)
    }

    private fun hideEncryptionTurningOnDialog() {
        endLoading()
    }

    fun endSecretDialog() {
        // Remove protected flag
        protectedConversationRepository.delete(protectedConversation.value!!)
    }

    fun isProtectedDialog() = protectedConversation.value != null

    fun btnSendMessageClicked() {
        // Restrict sending messages if we are in safe mode
        if (safeModeManager.isSafeMode) {
            showError(R.string.failed_to_send_message)
            return
        }

        if (isProtectedDialog()) {
            sendEncryptedTextMessage()
        } else {
            prepareMessageToSend()
        }
    }

    fun sendEncryptedTextMessage() {
        if (!hasConnection()) return

        if (messageText.isNullOrBlank()) return

        val text = messageText!!

        // 1. Get last symmetric key for this dialog
        // 2. Get my last private sign key
        // 3. Encrypt message text with library
        // 4. Create EncryptedMessage object
        // 5. Put this object to message's attachments

        val messageBuilder = Message.Builder(ConversationType.DIALOG, userId)
            .setReplyTo(replyTo.value?.getMessage()?.globalId)

        try {
            val yEncrypt = encryptionWrapper.getYEncrypt()

            val lastSymmetricKey = _myLastSymmetricKey
                ?: throw NullPointerException("Symmetric key is null")

            val myLastSignKeys =
                _myLastSignKeys ?: throw NullPointerException("Private key is null")

            if (EncryptHelper.isExpired(myLastSignKeys)) {
                prepareNewSignKeys {
                    sendEncryptedTextMessage()
                }

                return
            }

            if (EncryptHelper.isExpired(lastSymmetricKey)) {
                // If symmetric key is expired, we have to generate new one and send it to second user
                // After that try to send this message again

                prepareNewSymmetricKey(myLastSignKeys) {
                    sendEncryptedTextMessage()
                }
                return
            }

            yEncrypt.setSymmetricEncryptKey(lastSymmetricKey.data)
            yEncrypt.privateSignKeyToSend = myLastSignKeys.privateKey
            val encryptedData =
                yEncrypt.encryptSecretMsg(
                    1,
                    EncryptedMessageType.TEXT,
                    0,
                    text.toByteArray()
                )

            val encryptedMessage =
                EncryptedMessage(
                    lastSymmetricKey.id,
                    myLastSignKeys.id,
                    if (settingsHelper.getSaveEncryptedMessagesOnServer()) 1 else 0,
                    0,
                    EncryptHelper.bytesToBase64(encryptedData)
                )

            val gson = Gson()
            val attachment = Attachment(
                Attachment.Type.ENCRYPTED_MESSAGE,
                null,
                0,
                gson.toJson(encryptedMessage),
                null
            )
            messageBuilder.setAttachments(listOf(attachment))
        } catch (e: Exception) {
            showError(R.string.failed_to_send_encrypted_message)
            e.printStackTrace()
            return
        }

        sendMessage(messageBuilder.build(), object : SimpleResultCallback {
            override fun success() {
                clearMessageTextEvent.call()
            }

            override fun error() {
                // nothing
            }
        })
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
        val message = Message.Builder(ConversationType.DIALOG, userId)
            .setAttachments(listOf(attachment))
            .setReplyTo(replyTo.value?.getMessage()?.globalId)
            .build()

        sendMessage(message, null)
    }

    fun sendEncryptedFile(fileInfo: FileInfo, attachmentType: Int) {
        if (!hasConnection()) return

        val messageBuilder = Message.Builder(ConversationType.DIALOG, userId)
            .setReplyTo(replyTo.value?.getMessage()?.globalId)

        // 1. Get last symmetric key for this dialog
        // 2. Get my last private key
        // 3. Encrypt message text with library
        // 4. Create EncryptedMessage object
        // 5. Put this object to message's attachments

        try {
            val yEncrypt = encryptionWrapper.getYEncrypt()

            // We don't need to generate new keys, because we came from encryptFile method
            // where we used relevant keys (they almost can't be expired)
            val mySignKeys = _myLastSignKeys ?: throw NullPointerException("Sign key is null")
            val symmetricKey =
                _myLastSymmetricKey ?: throw NullPointerException("Symmetric key is null")

            if (EncryptHelper.isExpired(mySignKeys)) {
                showError(R.string.sign_keys_are_expired)
                return
            }

            if (EncryptHelper.isExpired(symmetricKey)) {
                showError(R.string.symmetric_key_is_expired)
                return
            }

            val lifetime = 0L
            yEncrypt.setSymmetricEncryptKey(symmetricKey.data)
            yEncrypt.privateSignKeyToSend = mySignKeys.privateKey

            val type = when (attachmentType) {
                Attachment.Type.FILE -> EncryptedMessageType.FILE
                Attachment.Type.PICTURE -> EncryptedMessageType.PHOTO
                Attachment.Type.VOICE -> EncryptedMessageType.VOICE
                else -> throw Exception("Wrong attachment type")
            }

            val gson = Gson()

            val encryptedData =
                yEncrypt.encryptSecretMsg(1, type, lifetime, gson.toJson(fileInfo).toByteArray())

            val encryptedMessage =
                EncryptedMessage(
                    symmetricKey.id,
                    mySignKeys.id,
                    if (settingsHelper.getSaveEncryptedMessagesOnServer()) 1 else 0,
                    lifetime,
                    EncryptHelper.bytesToBase64(encryptedData)
                )

            val attachment = Attachment(
                Attachment.Type.ENCRYPTED_MESSAGE,
                null,
                0,
                gson.toJson(encryptedMessage),
                null
            )
            messageBuilder.setAttachments(listOf(attachment))
        } catch (e: Exception) {
            showError(R.string.failed_to_send_encrypted_message)
            e.printStackTrace()
            return
        }

        sendMessage(messageBuilder.build(), null)
    }

    fun sendKeyExchangeMessage(
        keyExchangeMessage: KeyExchangeMessage,
        callback: SimpleResultCallback
    ) {
        if (!hasConnection()) return

        val gson = Gson()

        // Prepare attachment
        val attachment = Attachment(
            Attachment.Type.KEY_EXCHANGE_MESSAGE,
            null,
            0,
            gson.toJson(keyExchangeMessage),
            null
        )

        // Generate message object
        val message = Message.Builder(ConversationType.DIALOG, userId)
            .setAttachments(listOf(attachment))
            .setReplyTo(replyTo.value?.getMessage()?.globalId)
            .build()

        sendMessage(message, callback)
    }

    private fun sendMessage(message: Message, callback: SimpleResultCallback?) {
        messageIsSending.postValue(true)
        messageRepository.sendMessage(message, object : MessageRepository.SendMessageCallback {
            override fun sent(message: Message) {
                callback?.success()
                messageIsSending.postValue(false)
                if (dialog.value == null) {
                    createDialog(message.conversationId!!, userId)
                }
            }

            override fun error(error: ResultResponse) {
                callback?.error()
                messageIsSending.postValue(false)
                // FIXME: Do it in better way
                if (error.errorCode == WSResponse.ErrorCode.INVALID_ATTACHMENT &&
                    error.message?.contains("forbidden") == true
                ) {
                    showError(R.string.sending_encrypted_messages_is_restricted)
                } else {
                    showError(R.string.failed_to_send_message)
                }
            }
        })
        replyTo.postValue(null)
    }

    private fun prepareNewSignKeys(callback: (Keys) -> Unit) {
        Log.e(TAG, "My sign keys are expired. Generating new one and try again...")

        // Generate new keys, send it to server and call this function again
        val keyPairWrapper = KeysGeneratorHelper(encryptionWrapper)
            .getAsymmetricKeys(KeysGeneratorHelper.Length.SHORT, true)

        val key = Key(
            keyPairWrapper.keysId,
            EncryptHelper.bytesToBase64(keyPairWrapper.keyPair.publicKey),
            keyPairWrapper.lifetime,
            keyPairWrapper.generationTime,
            keyPairWrapper.version.toInt(),
            null,
            null,
            if (keyPairWrapper.isSign) 0 else 1
        )

        keysRepository.addNewKey(key, keyPairWrapper.keyPair.privateKey) {
            if (it == null) {
                hideEncryptionTurningOnDialog()
                showError(R.string.failed_to_send_keys)
            } else {
                _myLastSignKeys = it
                callback.invoke(it)
            }
        }
    }

    private fun prepareNewSymmetricKey(mySignKeys: Keys, callback: (SymmetricKey) -> Unit) {
        val yEncrypt = try {
            encryptionWrapper.getYEncrypt()
        } catch (e: Exception) {
            e.printStackTrace()
            hideEncryptionTurningOnDialog()
            showError(R.string.failed_to_start_encrypted_conversation)
            return
        }

        Log.e(TAG, "Symmetric key is expired. Generating new one and try again...")

        val dialogId = dialog.value?.id
        if (dialogId == null) {
            hideEncryptionTurningOnDialog()
            showError(R.string.failed_to_start_encrypted_conversation)
            return
        }

        keysRepository.getLastUserKey(userId, object : KeysRepository.GetKeyResult {
            override fun result(keys: Keys) {
                if (EncryptHelper.isExpired(keys)) {
                    Log.e(TAG, "User's public key is expired")
                    hideEncryptionTurningOnDialog()
                    showError(R.string.user_has_no_public_keys)
                    return
                }

                // Generation of symmetric key
                val keysGeneratorHelper = KeysGeneratorHelper(encryptionWrapper)
                val symmetricKeyWrapper = keysGeneratorHelper.getSymmetricKey()

                // Setup encrypt object
                yEncrypt.publicEncryptKeyToSend = keys.publicKey
                yEncrypt.privateSignKeyToSend = mySignKeys.privateKey

                try {
                    val encryptedSymKey = yEncrypt.encrypKeysMsg(
                        symmetricKeyWrapper.version,
                        symmetricKeyWrapper.generationTime,
                        symmetricKeyWrapper.key
                    )

                    val keyExchangeMessage =
                        KeyExchangeMessage(
                            keys.id,
                            mySignKeys.id,
                            EncryptHelper.bytesToBase64(encryptedSymKey)
                        )

                    sendKeyExchangeMessage(keyExchangeMessage, object : SimpleResultCallback {
                        override fun success() {
                            // Save symmetric key
                            val symmetricKey =
                                SymmetricKey(
                                    symmetricKeyWrapper.keyId,
                                    dialogId,
                                    symmetricKeyWrapper.key,
                                    symmetricKeyWrapper.generationTime,
                                    symmetricKeyWrapper.lifetime,
                                    currentUserId
                                )
                            symmetricKeyRepository.saveSymmetricKey(symmetricKey)

                            _myLastSymmetricKey = symmetricKey
                            callback.invoke(symmetricKey)
                        }

                        override fun error() {
                            hideEncryptionTurningOnDialog()
                            showError(R.string.failed_start_secret_dialog)
                        }
                    })
                } catch (e: YKeyException) {
                    e.printStackTrace()
                    hideEncryptionTurningOnDialog()
                    showToast(R.string.error)
                } catch (e: Exception) {
                    e.printStackTrace()
                    hideEncryptionTurningOnDialog()
                    showToast(R.string.unknown_error)
                }
            }

            override fun error() {
                Log.e(TAG, "Failed to get last user key")
                hideEncryptionTurningOnDialog()
                showError(R.string.user_has_no_public_keys)
            }
        })
    }

    fun encryptFile(
        file: File,
        callback: EncryptFileCallback
    ) {
        try {
            val yEncrypt = encryptionWrapper.getYEncrypt()

            val symmetricKey =
                _myLastSymmetricKey ?: throw NullPointerException("Symmetric key is null")
            val mySignKeys = _myLastSignKeys ?: throw NullPointerException("Sign key is null")

            if (EncryptHelper.isExpired(mySignKeys)) {
                prepareNewSignKeys {
                    encryptFile(file, callback)
                }

                return
            }

            if (EncryptHelper.isExpired(symmetricKey)) {
                prepareNewSymmetricKey(mySignKeys) {
                    encryptFile(file, callback)
                }

                return
            }

            val lifetime = 0L
            yEncrypt.setSymmetricEncryptKey(symmetricKey.data)
            yEncrypt.privateSignKeyToSend = mySignKeys.privateKey
            val encryptedData =
                yEncrypt.encryptSecretMsg(1, EncryptedMessageType.TEXT, lifetime, file.readBytes())

            callback.encrypted(encryptedData)
        } catch (e: Exception) {
            showError(R.string.failed_to_encrypt_file)
            e.printStackTrace()
            callback.error()
        }
    }

    fun decryptFile(
        file: File,
        senderId: Long,
        keyId: Long,
        signKeyId: Long,
        decryptFileCallback: DecryptFileCallback
    ) {
        // 1. Get symmetric key
        // 2. Get sign key
        // 3. Decrypt file

        // Check cache first
        if (decryptedFilesCache.containsKey(file.path)) {
            Log.d(TAG, "Decrypted file is cached")
            decryptFileCallback.decrypted(decryptedFilesCache[file.path]!!)
        } else {
            Log.d(TAG, "Start file decryption")
            val startDecryption = System.currentTimeMillis()
            symmetricKeyRepository.getSymmetricKey(
                keyId,
                object : SymmetricKeyRepository.GetKeyCallback {
                    override fun result(symmetricKey: SymmetricKey) {
                        keysRepository.getKeysByUser(senderId, signKeyId) {
                            if (it != null) {
                                AppExecutors.getInstance().diskIO.execute {
                                    try {
                                        val yEncrypt = encryptionWrapper.getYEncrypt()

                                        yEncrypt.setSymmetricEncryptKey(symmetricKey.data)
                                        yEncrypt.publicSignKeyToReceive = it.publicKey
                                        val data = yEncrypt.decryptSecretMsg(file.readBytes())
                                        val decryptionTime =
                                            System.currentTimeMillis() - startDecryption
                                        Log.d(TAG, "File has decrypted for $decryptionTime ms")
                                        // Add to cache
                                        decryptedFilesCache[file.path] = data.msg

                                        Handler(Looper.getMainLooper()).post {
                                            decryptFileCallback.decrypted(data.msg)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to decrypt file")
                                        Handler(Looper.getMainLooper()).post {
                                            decryptFileCallback.error()
                                        }
                                    }
                                }
                            } else {
                                Log.e(TAG, "There is no sign key")
                                decryptFileCallback.error()
                            }
                        }
                    }

                    override fun keyNotFound() {
                        Log.e(TAG, "There is no symmetric key")
                        decryptFileCallback.error()
                    }
                })
        }
    }

    interface EncryptFileCallback {
        fun encrypted(data: ByteArray)
        fun error()
    }

    interface DecryptFileCallback {
        fun decrypted(data: ByteArray)
        fun error()
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

    fun openUserProfile() {
        openUserProfileEvent.postValue(userId)
    }

    override fun deleteMessages(messages: List<MessageModel>) {
        val deleteLocally = hashSetOf<String>()
        val deleteRemotely = hashSetOf<String>()

        for (message in messages) {
            if (message.isProtected() &&
                message.getAttachment().isEncryptedMessage() &&
                message.getAttachment().getPayloadAsEncryptedMessage().saveFlag == 0
            ) {
                deleteLocally.add(message.getMessage().globalId)
            } else {
                deleteRemotely.add(message.getMessage().globalId)
            }
        }

        if (deleteLocally.isNotEmpty()) {
            messageRepository.deleteMessagesLocally(
                dialog.value!!.id,
                ConversationType.DIALOG,
                deleteLocally.toList()
            )
        }

        if (deleteRemotely.isNotEmpty()) {
            if (!hasConnection()) return
            messageRepository.deleteMessages(
                dialog.value!!.id,
                ConversationType.DIALOG,
                deleteRemotely.toList()
            )
        }
    }

    fun setReplyTo(messageModel: MessageModel?) {
        replyTo.postValue(messageModel)
    }

    private fun createDialog(dialogId: Long, userId: Long) {
        val newDialog = Dialog(dialogId, userId)
        dialogRepository.createDialog(newDialog)
    }

    /**
     * For testing purposes
     */
    fun clearMessagesLocally() {
        dialog.value?.let {
            messageRepository.clearMessagesLocally(ConversationType.DIALOG, it.id)
        }
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
                if (conversationType != message.conversationType || identifier != userId) {
                    showToast(R.string.message_was_forwarded)
                }
            }

            override fun error(error: ResultResponse) {
                showError(R.string.failed_to_send_message)
            }
        })

        // Open conversation if it's not the current one
        if (conversationType != message.conversationType || identifier != userId) {
            openConversationEvent.postValue(conversationType to identifier)
            finishActivityEvent.call()
        }
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

    fun setConnected(status: Boolean) {
        connected = status
    }

    private fun hasConnection(): Boolean {
        if (!connected) {
            showError(R.string.connection_is_lost)
        }

        return connected
    }

    fun showEncryptionKey() {
        dialog.value?.let {
            verifyEncryptionKeyEvent.postValue(it.id)
        }
    }

    fun openGallery() {
        dialog.value?.id?.let {
            openGalleryEvent.postValue(it)
        }
    }

    fun updateUser() {
        updateUserEvent.call()
    }

    fun saveDraft(text: String) {
        if (text.isBlank()) return

        val conversationId = dialog.value?.id ?: return

        val draftMessage = DraftMessage(
            conversationId,
            ConversationType.DIALOG,
            text,
            System.currentTimeMillis() / 1000
        )
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

    fun getAttachmentsCount(): Int {
        return attachments.value?.size ?: 0
    }

    fun getAvailableAttachmentsCount(): Int {
        return if (isProtectedDialog())
            1
        else
            Limitations.MAX_ATTACHMENTS - getAttachmentsCount()
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
                // Upload next
                prepareAttachments(index + 1)
            }
        }
    }

    private fun prepareMessageToSend() {
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
            }
        }

        val messageBuilder = Message.Builder(ConversationType.DIALOG, userId)
            .setReplyTo(replyTo.value?.getMessage()?.globalId)
            .setAttachments(attachmentsToSend)
            .setText(messageText)

        sendMessage(messageBuilder.build(), object : SimpleResultCallback {
            override fun success() {
                clearMessageTextEvent.call()
                attachments.postValue(listOf())
            }

            override fun error() {
                // nothing
            }
        })
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
            // Send encrypted voice message if it's encrypted dialog
            if (isProtectedDialog()) {
                encryptFile(file, object : EncryptFileCallback {
                    override fun encrypted(data: ByteArray) {
                        uploadFile(data, file.name, true, {
                            sendEncryptedFile(it, Attachment.Type.VOICE)
                        })
                    }

                    override fun error() {
                        showError(R.string.failed_to_encrypt_file)
                    }
                })
            } else {
                uploadFile(file, true) {
                    sendFile(it, Attachment.Type.VOICE)
                }
            }
        } else {
            showToast("File does not exist")
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

    fun playVoice(decryptedBytes: ByteArray, filePath: String, callback: () -> Unit) {
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
            voicePlayerHelper.setSource(decryptedBytes, filePath)
            voicePlayerHelper.play()
        }
    }

    fun pauseVoice() {
        voicePlayerHelper.pause()
    }

    class Factory(
        private val userId: Long,
        private val userRepository: UserRepository,
        private val dialogRepository: DialogRepository,
        private val messageRepository: MessageRepository,
        private val fileRepository: FileRepository,
        private val attachmentRepository: AttachmentRepository,
        private val protectedConversationRepository: ProtectedConversationRepository,
        private val keysRepository: KeysRepository,
        private val symmetricKeyRepository: SymmetricKeyRepository,
        private val encryptionWrapper: EncryptionWrapper,
        private val pollRepository: PollRepository,
        private val settingsHelper: SettingsHelper,
        private val authorizationManager: AuthorizationManager,
        private val draftMessageRepository: DraftMessageRepository,
        private val imageCompressor: ImageCompressor,
        private val safeModeManager: SafeModeManager,
        private val audioRecorder: AudioRecorder,
        private val voicePlayerHelper: VoicePlayerHelper,
        private val userActionRepository: UserActionRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return DialogViewModel(
                userId,
                userRepository,
                dialogRepository,
                messageRepository,
                fileRepository,
                attachmentRepository,
                protectedConversationRepository,
                keysRepository,
                symmetricKeyRepository,
                encryptionWrapper,
                pollRepository,
                settingsHelper,
                authorizationManager,
                draftMessageRepository,
                imageCompressor,
                safeModeManager,
                audioRecorder,
                voicePlayerHelper,
                userActionRepository
            ) as T
        }
    }

    companion object {
        private const val TAG = "DialogViewModel"
    }
}