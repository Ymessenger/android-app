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

package org.ymessenger.app.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.ymessenger.app.data.local.db.entities.ChatUser
import org.ymessenger.app.data.local.db.entities.UserAction
import org.ymessenger.app.data.mappers.ChatMapper
import org.ymessenger.app.data.mappers.ChatUserMapper
import org.ymessenger.app.data.mappers.KeysMapper
import org.ymessenger.app.data.remote.entities.AsymmetricKey
import org.ymessenger.app.data.remote.entities.Chat
import org.ymessenger.app.data.remote.entities.Message
import org.ymessenger.app.data.remote.entities.Session
import org.ymessenger.app.data.remote.notices.*
import org.ymessenger.app.data.repositories.*
import org.ymessenger.app.helpers.EncryptHelper
import org.ymessenger.app.helpers.EncryptionWrapper

class NotificationHandler(
    private val messageRepository: MessageRepository,
    private val chatPreviewRepository: ChatPreviewRepository,
    private val chatRepository: ChatRepository,
    private val chatUserRepository: ChatUserRepository,
    private val chatMapper: ChatMapper,
    private val chatUserMapper: ChatUserMapper,
    private val keysMapper: KeysMapper,
    private val encryptionWrapper: EncryptionWrapper,
    private val keysRepository: KeysRepository,
    private val userActionRepository: UserActionRepository
) {
    private var newMessageListener: NewMessageListener? = null
    private var needLoginListener: NeedLoginListener? = null

    var onNewSession: ((Session) -> Unit)? = null

    private var privateEncryptKey: ByteArray? = null

    fun messagesAreRead(messagesAreRead: MessagesAreRead) {
        messageRepository.messagesRead(
            messagesAreRead.messagesId,
            messagesAreRead.conversationId,
            messagesAreRead.conversationType
        )
        Log.d(TAG, "Messages are read")

        updateChatPreviews()
    }

    fun newMessage(newMessage: NewMessage) {
        messageRepository.saveRemoteMessages(listOf(newMessage.message))
        updateChatPreviews()

        newMessageListener?.run {
            val chatPreviewModel = chatPreviewRepository.getChatPreviewByChatSync(
                newMessage.message.conversationId!!,
                newMessage.message.conversationType!!
            )
            if (chatPreviewModel?.isMuted() == false || newMessage.call) {
                onMessageReceived(newMessage.message)
            }
        } ?: Log.d(TAG, "NewMessageListener is null")
    }

    fun newChat(newChat: NewChat) {
        saveRemoteChat(newChat.chat)
        updateChatPreviews()
    }

    fun chatEdited(editedChat: EditChat) {
        saveRemoteChat(editedChat.chat)
        updateChatPreviews()
    }

    private fun saveRemoteChat(remoteChat: Chat) {
        val dbChat = chatMapper.toDb(remoteChat)
        chatRepository.insertChat(dbChat)

        val dbChatUsers = remoteChat.users.map { chatUserMapper.toDb(it) }
        chatUserRepository.insertChatUsers(dbChatUsers)
    }

    fun usersAddedToChat(usersAddedToChat: UsersAddedToChat) {
        val dbChatUsers = usersAddedToChat.newUsers.map { chatUserMapper.toDb(it) }
        chatUserRepository.insertChatUsers(dbChatUsers)
    }

    fun chatUsersChanged(chatUsersChanged: ChatUsersChanged) {
        val chatUsersToDelete = arrayListOf<ChatUser>()
        val chatUsersToUpdate = arrayListOf<ChatUser>()
        for (remoteChatUser in chatUsersChanged.chatUsers) {
            val dbChatUser = chatUserMapper.toDb(remoteChatUser)
            if (remoteChatUser.deleted || remoteChatUser.banned!!) {
                chatUsersToDelete.add(dbChatUser)
            } else {
                chatUsersToUpdate.add(dbChatUser)
            }
        }
        chatUserRepository.updateChatUsers(chatUsersToUpdate)
        chatUserRepository.deleteChatUsers(chatUsersToDelete)
    }

    fun messagesUpdated(messagesUpdated: MessagesUpdated) {
        messagesUpdated.deleted?.let {
            messageRepository.deleteLocalMessages(
                it.conversationType,
                it.conversationId,
                it.messageIds
            )
        }

        // TODO: handle updates

        updateChatPreviews()
    }

    fun updateChatPreviews() {
        Log.d(TAG, "Updating chat previews...")
        chatPreviewRepository.updateChatPreviews()
    }

    fun newSession(newSession: NewSession) {
        onNewSession?.invoke(newSession.session)
    }

    fun encryptedKeys(encryptedKeys: EncryptedKeys) {
        if (!encryptionWrapper.isInitialized()) {
            Log.e(TAG, "YEncrypt is not initialized")
            return
        }

        val yEncrypt = try {
            encryptionWrapper.getYEncrypt()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        // 1. Decrypt symmetric key
        // 2. Decrypt private keys
        // 3. Save private keys

        if (privateEncryptKey == null) {
            Log.e(TAG, "Failed to decrypt private keys because our private encrypt key is null")
            return
        }

        val encryptedSymmetricKey = EncryptHelper.base64ToBytes(encryptedKeys.encryptedSymmetricKey)
        val encryptedKeyBytes = EncryptHelper.base64ToBytes(encryptedKeys.encryptedKeys)
        val publicSignKeyToReceive = EncryptHelper.base64ToBytes(encryptedKeys.publicSignKey)

        yEncrypt.publicSignKeyToReceive = publicSignKeyToReceive
        yEncrypt.privateEncryptKeyToReceive = privateEncryptKey

        try {
            val symmetricKey = yEncrypt.decrypKeysMsg(encryptedSymmetricKey)

            // Now decrypt private keys
            yEncrypt.setSymmetricEncryptKey(symmetricKey.key)
            yEncrypt.publicSignKeyToReceive = publicSignKeyToReceive

            val decryptedMsg = yEncrypt.decryptSecretMsg(encryptedKeyBytes)
            val decryptedData = decryptedMsg.msg

            // Convert this to string. This should be json of array of keys
            val jsonPrivateKeys = String(decryptedData)
            val gson = Gson()
            val asymmetricKeysList = gson.fromJson<List<AsymmetricKey>>(
                jsonPrivateKeys,
                object : TypeToken<List<AsymmetricKey>>() {}.type
            )
            // Convert to DB objects
            val keysList = asymmetricKeysList.map { keysMapper.toDb(it) }
            // Save this keys to the database
            keysRepository.savePrivateKeys(keysList)
            Log.d(
                TAG,
                "Saved ${asymmetricKeysList.size} private keys from ${encryptedKeys.deviceName}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt private keys")
            e.printStackTrace()
        }
    }

    fun setPrivateEncryptKeyToReceive(key: ByteArray) {
        privateEncryptKey = key
    }

    fun needLogin() {
        needLoginListener?.onNeedLogin()
    }

    interface NewMessageListener {
        fun onMessageReceived(remoteMessage: Message)
    }

    interface NeedLoginListener {
        fun onNeedLogin()
    }

    fun setNewMessageListener(listener: NewMessageListener?) {
        this.newMessageListener = listener
    }

    fun setNeedLoginListener(listener: NeedLoginListener?) {
        this.needLoginListener = listener
    }

    fun newUserAction(newUserAction: NewUserAction) {
        val userAction = UserAction(
            newUserAction.conversationId,
            newUserAction.conversationType,
            newUserAction.action,
            System.currentTimeMillis() / 1000L,
            newUserAction.userId
        )
        userActionRepository.insert(userAction)
    }

    companion object {
        private const val TAG = "NotificationHandler"
    }
}