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

package org.ymessenger.app.data.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.gson.Gson
import org.ymessenger.app.data.local.db.dao.*
import org.ymessenger.app.data.local.db.entities.*
import org.ymessenger.app.data.local.db.models.MessageModel
import org.ymessenger.app.data.mappers.MessageMapper
import org.ymessenger.app.data.mappers.RepliedMessageMapper
import org.ymessenger.app.data.remote.Listing
import org.ymessenger.app.data.remote.WebSocketService
import org.ymessenger.app.data.remote.entities.KeyExchangeMessage
import org.ymessenger.app.data.remote.requests.*
import org.ymessenger.app.data.remote.responses.Keys
import org.ymessenger.app.data.remote.responses.Messages
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.data.remote.responses.UpdatedMessages
import org.ymessenger.app.helpers.EncryptHelper
import org.ymessenger.app.helpers.EncryptionWrapper
import org.ymessenger.app.models.FoundMessage
import org.ymessenger.app.utils.AppExecutors

class MessageRepository private constructor(
    private val executors: AppExecutors,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao,
    private val repliedMessageDao: RepliedMessageDao,
    private val forwardedMessageInfoDao: ForwardedMessageInfoDao,
    private val keysDao: KeysDao,
    private val symmetricKeyDao: SymmetricKeyDao,
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    private val channelDao: ChannelDao,
    private val encryptionWrapper: EncryptionWrapper,
    private val webSocketService: WebSocketService,
    private val messageMapper: MessageMapper,
    private val repliedMessageMapper: RepliedMessageMapper,
    private val lastLoadedMessageIdDao: LastLoadedMessageIdDao
) {

    /**
     * Return messages page by page
     *
     * @param conversationId Id of conversation
     * @param conversationType Type of conversation
     * @param messageDataSourceFactory DataSource factory
     *
     * @return PagedList wrapped in LiveData
     */
    fun getMessagesByConversationPaged(
        conversationType: Int,
        conversationId: Long,
        messageDataSourceFactory: MessageDataSourceFactory
    ): LiveData<PagedList<MessageModel>> {
        val boundaryCallback = MessageBoundaryCallback(
            conversationType,
            conversationId,
            webSocketService,
            messageDao,
            this::insertResultIntoDb
        )

        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(MESSAGES_PAGE_SIZE)
            .build()

        return LivePagedListBuilder(messageDataSourceFactory, config)
            .setBoundaryCallback(boundaryCallback)
            .setFetchExecutor(executors.diskIO)
            .build()
    }

    /**
     * This method returns range of messages in selected conversation
     */
    fun getMessagesByConversation(
        conversationId: Long,
        conversationType: Int,
        requestedStartPosition: Int,
        requestedLoadSize: Int
    ) = messageDao.getMessagesByConversation(
        conversationId,
        conversationType,
        requestedStartPosition,
        requestedLoadSize
    )

    fun searchMessages(
        query: String,
        conversationId: Long? = null,
        conversationType: Int? = null,
        chatPreviewRepository: ChatPreviewRepository
    ): Listing<FoundMessage> {

        val sourceFactory = SearchMessagesDataSourceFactory(
            query,
            conversationId,
            conversationType,
            webSocketService,
            chatPreviewRepository,
            executors,
            messageMapper
        )

        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(MESSAGES_PAGE_SIZE)
            .build()

        val livePagedListBuilder =
            LivePagedListBuilder<SearchMessagesDataSource.SearchKey, FoundMessage>(
                sourceFactory,
                config
            )
                .setFetchExecutor(executors.networkIO)
                .build()

        val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
            it.initialLoad
        }

        return Listing(
            livePagedListBuilder,
            refresh = {
                sourceFactory.sourceLiveData.value?.invalidate()
            },
            refreshState = refreshState
        )
    }

    /**
     * Observe this LiveData to invalidate MessagePositionalDataSource
     */
    fun getLastMessage(conversationId: Long, conversationType: Int): LiveData<MessageModel> {
        return messageDao.getLastMessage(conversationId, conversationType)
    }

    /**
     * Inserts remote messages into database. This function converts remote entities to database entities, extracts
     * attachments and save all the data in DB
     *
     * @param remoteMessages List of remote message entities
     */
    private fun insertResultIntoDb(remoteMessages: List<org.ymessenger.app.data.remote.entities.Message>) {
        if (remoteMessages.isEmpty()) {
            return
        }

        val keyExchangeMessages = arrayListOf<org.ymessenger.app.data.remote.entities.Message>()
        val repliedMessagesIdsHashSet = hashSetOf<String>()
        val conversationId = remoteMessages.first().conversationId!!
        val conversationType = remoteMessages.first().conversationType!!

        val dbMessages = arrayListOf<Message>()
        val dbAttachments = arrayListOf<Attachment>()
        val forwardedMessageInfoList = arrayListOf<ForwardedMessageInfo>()

        for (remoteMessage in remoteMessages) {
            // Save message id of replied messages to download their
            remoteMessage.replyTo?.let { messageId ->
                repliedMessagesIdsHashSet.add(messageId)
            }

            // Will be added to dbMessages list
            var dbMessage: Message? = null

            // Checking for attachments
            if (!remoteMessage.attachments.isNullOrEmpty()) {
                // Extract attachments if they are not empty
                var attachments = extractAttachmentsFromMessage(remoteMessage)

                // Checking if this message is forwarded
                val attachment = attachments.first()
                if (attachment.isForwardedMessages()) {
                    // Extract forwarded message from attachments
                    val remoteForwardedMessage =
                        attachment.getPayloadAsForwardedMessages().firstOrNull() ?: continue

                    // Build remote message from forwarded message with text and attachments
                    val remoteMessageFromForwarded =
                        org.ymessenger.app.data.remote.entities.Message(
                            remoteMessage.id,
                            remoteMessage.sendingTime,
                            remoteMessage.senderId,
                            remoteMessage.receiverId,
                            remoteMessage.conversationId,
                            remoteMessage.conversationType,
                            remoteMessage.globalId,
                            remoteMessage.read,
                            remoteMessage.nodesId,
                            remoteMessage.replyTo,
                            remoteForwardedMessage.text,
                            remoteForwardedMessage.attachments
                        )

                    // Build DB message entity
                    val dbForwardedMessage = messageMapper.toDb(remoteMessageFromForwarded)

                    // Build forwarded message info object
                    val forwardedMessageInfo = ForwardedMessageInfo(
                        dbForwardedMessage.globalId,
                        dbForwardedMessage.conversationId,
                        dbForwardedMessage.conversationType,
                        remoteForwardedMessage.senderId,
                        remoteForwardedMessage.globalId!!,
                        remoteForwardedMessage.conversationId!!,
                        remoteForwardedMessage.conversationType!!
                    )
                    // Add to list to save to DB
                    forwardedMessageInfoList.add(forwardedMessageInfo)

                    // Put our DB entity here
                    dbMessage = dbForwardedMessage

                    // We must extract attachments from forwarded message instead of regular message (because it
                    // contained our forwarded message entity)
                    attachments = extractAttachmentsFromMessage(remoteMessageFromForwarded)
                }

                // Check if this attachment is symmetric key
                if (attachment.isExchangeKeyMessage()) {
                    keyExchangeMessages.add(remoteMessage)
                }

                // Add DB attachments entities into list to save to DB
                dbAttachments.addAll(attachments)
            }

            // If this is not forwarded message, dbMessage should be null and we just convert it to DB entity
            if (dbMessage == null) {
                dbMessage = messageMapper.toDb(remoteMessage)
            }

            // Add DB messages entities into list to save to DB
            dbMessages.add(dbMessage)
        }

        if (keyExchangeMessages.isNotEmpty()) {
            saveSymmetricKeys(keyExchangeMessages)
        }

        // Save entities into DB
        messageDao.insertAll(dbMessages)
        attachmentDao.insert(dbAttachments)
        forwardedMessageInfoDao.insert(forwardedMessageInfoList)
        Log.d(
            TAG,
            "${dbMessages.size} message(s) saved to database (attachments - ${dbAttachments.size}, forwarded messages info - ${forwardedMessageInfoList.size})"
        )

        // Download replied messages if they are not empty
        if (repliedMessagesIdsHashSet.isNotEmpty()) {
            getRepliedMessagesFromServer(
                conversationId,
                conversationType,
                repliedMessagesIdsHashSet.toList()
            )
        }
    }

    private fun extractAttachmentsFromMessage(remoteMessage: org.ymessenger.app.data.remote.entities.Message): List<Attachment> {
        val dbAttachments = arrayListOf<Attachment>()
        remoteMessage.attachments?.forEach {
            dbAttachments.add(
                Attachment(
                    remoteMessage.globalId!!,
                    remoteMessage.conversationId!!,
                    remoteMessage.conversationType!!,
                    it.type!!,
                    it.payload,
                    null
                )
            )
        }

        return dbAttachments
    }

    /**
     * Parses KeyExchangeMessage and call saveSymmetricKey function
     */
    private fun saveSymmetricKeys(remoteMessages: List<org.ymessenger.app.data.remote.entities.Message>) {
        val gson = Gson()

        for (remoteMessage in remoteMessages) {
            val jsonString = remoteMessage.attachments!!.first().payload
            val keyExchangeMessage =
                gson.fromJson(jsonString, KeyExchangeMessage::class.java)
            saveSymmetricKey(
                keyExchangeMessage,
                remoteMessage.conversationId!!,
                remoteMessage.senderId!!
            )
        }
    }

    /**
     * Decrypts symmetric key and saves it to database
     *
     * @param keyExchangeMessage KeyExchangeMessage object
     * @param dialogId Identifier of conversation
     * @param userId Identifier of user
     */
    private fun saveSymmetricKey(
        keyExchangeMessage: KeyExchangeMessage,
        dialogId: Long,
        userId: Long
    ) {
        if (!encryptionWrapper.isInitialized()) {
            Log.w(TAG, "YEncrypt is not initialized yet")
            return
        }

        val yEncrypt = try {
            encryptionWrapper.getYEncrypt()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        executors.diskIO.execute {
            val myKeys = keysDao.getKeys(keyExchangeMessage.keyId)
            if (myKeys?.privateKey != null) {
                val getUserKeys = GetUserKeys(userId, keysId = listOf(keyExchangeMessage.signKeyId))
                webSocketService.getUserKeys(
                    getUserKeys,
                    object : WebSocketService.ResponseCallback<Keys> {
                        override fun onResponse(response: Keys) {
                            if (response.keys.isEmpty()) {
                                Log.e(TAG, "There is no keys")
                                return
                            }

                            val key = response.keys.first()
                            val signKey = EncryptHelper.base64ToBytes(key.data)

                            yEncrypt.privateEncryptKeyToReceive = myKeys.privateKey
                            yEncrypt.publicSignKeyToReceive = signKey

                            try {
                                val symmetricKey =
                                    yEncrypt.decrypKeysMsg(
                                        EncryptHelper.base64ToBytes(
                                            keyExchangeMessage.encryptedData
                                        )
                                    )
                                Log.d(
                                    TAG,
                                    "Received symmetric key: ${EncryptHelper.bytesToBase64(
                                        symmetricKey.key
                                    )} with ID ${symmetricKey.id}"
                                )

                                val symmetricKeyDb = SymmetricKey(
                                    symmetricKey.id,
                                    dialogId,
                                    symmetricKey.key,
                                    symmetricKey.date,
                                    symmetricKey.timeLife,
                                    userId
                                )

                                executors.diskIO.execute {
                                    symmetricKeyDao.insert(symmetricKeyDb)
                                    Log.d(TAG, "Symmetric key is saved!")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to extract symmetric key")
                                e.printStackTrace()
                            }
                        }

                        override fun onError(error: ResultResponse) {
                            Log.e(TAG, "Failed to get keys")
                        }
                    })
            } else {
                Log.d(TAG, "There is no my private key to decrypt symmetric key")
            }
        }
    }

    fun getMessagesFromServer(
        conversationId: Long,
        conversationType: Int,
        globalIds: List<String>
    ) {
        val getMessages = GetMessages(conversationType, conversationId, messagesId = globalIds)
        webSocketService.getMessages(
            getMessages,
            object : WebSocketService.ResponseCallback<Messages> {
                override fun onResponse(response: Messages) {
                    executors.diskIO.execute {
                        insertResultIntoDb(response.messages)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Get message errors with code ${error.errorCode}")
                }

            })
    }

    fun getMessagesFromServerByTypes(
        conversationId: Long,
        conversationType: Int,
        attachmentsTypes: IntArray?
    ) {
        val getMessages =
            GetMessages(conversationType, conversationId, attachmentsTypes = attachmentsTypes)
        webSocketService.getMessages(
            getMessages,
            object : WebSocketService.ResponseCallback<Messages> {
                override fun onResponse(response: Messages) {
                    executors.diskIO.execute {
                        insertResultIntoDb(response.messages)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Get message errors with code ${error.errorCode}")
                }

            })
    }

    /**
     * This method is used to load and save all messages from server, which were not loaded for some
     * reason and there is a bunch of missing messages that should be loaded.
     *
     * @param conversationId Conversation identifier
     * @param conversationType Conversation type
     * @param fromId Last saved message globalId. We should load all messages from this id
     * @param loadedMessages Temporary storage for loaded messages. It stores all pages of messages
     * and then save them all to the database
     */
    fun loadAllMessagesFromServerAfter(
        conversationId: Long,
        conversationType: Int,
        fromId: String,
        loadedMessages: ArrayList<org.ymessenger.app.data.remote.entities.Message>? = null
    ) {
        val getMessages = GetMessages(conversationType, conversationId, fromId, false)

        webSocketService.getMessages(
            getMessages,
            object : WebSocketService.ResponseCallback<Messages> {
                override fun onResponse(response: Messages) {
                    if (response.messages.isNotEmpty()) {
                        loadedMessages?.addAll(response.messages)
                        Log.d(TAG, "Load next messages page")
                        val lastId = loadedMessages?.last()?.globalId
                        if (lastId != null) {
                            loadAllMessagesFromServerAfter(
                                conversationId,
                                conversationType,
                                lastId,
                                loadedMessages
                            )
                        } else {
                            Log.e(TAG, "Last message globalId is null, can't load next page")
                        }
                    } else {
                        Log.d(TAG, "All messages were loaded. Save them to the database")
                        loadedMessages?.let {
                            executors.diskIO.execute {
                                insertResultIntoDb(it)
                            }
                        }
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to load all messages after $fromId")
                }
            })
    }

    fun getLastLoadedMessageId(
        conversationId: Long,
        conversationType: Int,
        callback: (String?) -> Unit
    ) {
        executors.diskIO.execute {
            val lastLoadedMessageId =
                lastLoadedMessageIdDao.getLastLoadedMessageId(conversationId, conversationType)
            callback.invoke(lastLoadedMessageId?.globalId)
        }
    }

    fun saveLastLoadedMessageId(
        conversationId: Long,
        conversationType: Int,
        lastLoadedMessageId: String
    ) {
        executors.diskIO.execute {
            lastLoadedMessageIdDao.upsert(
                LastLoadedMessageId(
                    conversationId,
                    conversationType,
                    lastLoadedMessageId
                )
            )
        }
    }

    fun getLastSymmetricKeysMessages(conversationId: Long, conversationType: Int) {
        val attachmentsTypes =
            intArrayOf(org.ymessenger.app.data.remote.entities.Attachment.Type.KEY_EXCHANGE_MESSAGE)

        val getMessages =
            GetMessages(conversationType, conversationId, attachmentsTypes = attachmentsTypes)
        webSocketService.getMessages(
            getMessages,
            object : WebSocketService.ResponseCallback<Messages> {
                override fun onResponse(response: Messages) {
                    saveSymmetricKeys(response.messages)
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Get message errors with code ${error.errorCode}")
                }

            })
    }

    fun sendMessage(
        message: org.ymessenger.app.data.remote.entities.Message,
        callback: SendMessageCallback
    ) {
        val sendMessages = SendMessages(listOf(message))
        webSocketService.sendMessages(
            sendMessages,
            object : WebSocketService.ResponseCallback<Messages> {
                override fun onResponse(response: Messages) {
                    saveRemoteMessages(response.messages)
                    if (response.messages.isNotEmpty()) {
                        callback.sent(response.messages.first())
                    }
                    webSocketService.updateChatPreviews()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Get message errors with code ${error.errorCode}")
                    callback.error(error)
                }

            })
    }

    interface SendMessageCallback {
        fun sent(message: org.ymessenger.app.data.remote.entities.Message)
        fun error(error: ResultResponse)
    }

    fun readMessage(message: Message) {
        val messagesRead =
            MessagesRead(listOf(message.globalId), message.conversationType, message.conversationId)
        webSocketService.messagesRead(
            messagesRead,
            object : WebSocketService.ResponseCallback<ResultResponse> {
                override fun onResponse(response: ResultResponse) {
                    message.read = true
                    executors.diskIO.execute {
                        messageDao.update(message)
                    }
                    webSocketService.updateChatPreviews()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Messages read errors with code ${error.errorCode}")
                }

            })
    }

    fun readMessage(
        messagesId: List<String>,
        conversationType: Int,
        conversationId: Long,
        success: (() -> Unit)
    ) {
        val messagesRead = MessagesRead(messagesId, conversationType, conversationId)
        webSocketService.messagesRead(
            messagesRead,
            object : WebSocketService.ResponseCallback<ResultResponse> {
                override fun onResponse(response: ResultResponse) {
                    messagesRead(messagesId, conversationId, conversationType)
                    webSocketService.updateChatPreviews()
                    success()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Messages read errors with code ${error.errorCode}")
                }

            })
    }

    fun messagesRead(messagesId: List<String>, conversationId: Long, conversationType: Int) {
        executors.diskIO.execute {
            messageDao.readAllMessagesBefore(messagesId.first(), conversationId, conversationType)
        }
    }

    fun saveMessage(message: Message) {
        executors.diskIO.execute {
            messageDao.saveMessage(message)
        }
    }

    fun saveRemoteMessages(remoteMessages: List<org.ymessenger.app.data.remote.entities.Message>) {
        executors.diskIO.execute {
            insertResultIntoDb(remoteMessages)
        }
    }

    fun deleteAfter(messageId: String) {
        executors.diskIO.execute {
            messageDao.deleteAfter(messageId)
        }
    }


    ///////////////////////////

    fun getRepliedMessage(
        conversationId: Long,
        conversationType: Int,
        globalId: String
    ): LiveData<MessageModel> {
        getRepliedMessagesFromServer(conversationId, conversationType, listOf(globalId))

        return repliedMessageDao.getRepliedMessageModel(globalId, conversationId, conversationType)
    }

    private fun getRepliedMessagesFromServer(
        conversationId: Long,
        conversationType: Int,
        globalIds: List<String>
    ) {
        val getMessages = GetMessages(conversationType, conversationId, messagesId = globalIds)
        webSocketService.getMessages(
            getMessages,
            object : WebSocketService.ResponseCallback<Messages> {
                override fun onResponse(response: Messages) {
                    val dbMessages = response.messages.map { repliedMessageMapper.toDb(it) }
                    executors.diskIO.execute {
                        repliedMessageDao.insertAll(dbMessages)
                    }
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Get message errors with code ${error.errorCode}")
                }

            })
    }

    fun deleteMessages(conversationId: Long, conversationType: Int, messageIds: List<String>) {
        val deleteMessages = DeleteMessages(messageIds, conversationId, conversationType)
        webSocketService.deleteMessages(
            deleteMessages,
            object : WebSocketService.ResponseCallback<UpdatedMessages> {
                override fun onResponse(response: UpdatedMessages) {
                    deleteLocalMessages(response)
                    webSocketService.updateChatPreviews()
                }

                override fun onError(error: ResultResponse) {
                    Log.e(TAG, "Failed to delete messages")
                }
            })
    }

    fun deleteMessagesLocally(
        conversationId: Long,
        conversationType: Int,
        messageIds: List<String>
    ) {
        executors.diskIO.execute {
            messageDao.deleteMessages(conversationId, conversationType, messageIds)
        }
    }

    fun deleteLocalMessages(updatedMessages: UpdatedMessages) {
        executors.diskIO.execute {
            updatedMessages.deleted?.forEach {
                messageDao.deleteMessages(it.conversationId, it.conversationType, it.messageIds)
            }

            // TODO: handle updates
        }
    }

    fun deleteLocalMessages(conversationType: Int, conversationId: Long, messageIds: List<String>) {
        executors.diskIO.execute {
            messageDao.deleteMessages(conversationId, conversationType, messageIds)
        }
    }

    /**
     * For testing purposes
     */
    fun clearMessagesLocally(conversationType: Int, conversationId: Long) {
        executors.diskIO.execute {
            messageDao.clearMessages(conversationType, conversationId)
        }
    }

    ////////////////////////////

    companion object {
        private const val TAG = "MessageRepository"
        const val MESSAGES_PAGE_SIZE = 30

        private var instance: MessageRepository? = null

        fun getInstance(
            executors: AppExecutors,
            messageDao: MessageDao,
            attachmentDao: AttachmentDao,
            repliedMessageDao: RepliedMessageDao,
            forwardedMessageInfoDao: ForwardedMessageInfoDao,
            keysDao: KeysDao,
            symmetricKeyDao: SymmetricKeyDao,
            userDao: UserDao,
            chatDao: ChatDao,
            channelDao: ChannelDao,
            encryptionWrapper: EncryptionWrapper,
            webSocketService: WebSocketService,
            messageMapper: MessageMapper,
            repliedMessageMapper: RepliedMessageMapper,
            lastLoadedMessageIdDao: LastLoadedMessageIdDao
        ): MessageRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: MessageRepository(
                        executors,
                        messageDao,
                        attachmentDao,
                        repliedMessageDao,
                        forwardedMessageInfoDao,
                        keysDao,
                        symmetricKeyDao,
                        userDao,
                        chatDao,
                        channelDao,
                        encryptionWrapper,
                        webSocketService,
                        messageMapper,
                        repliedMessageMapper,
                        lastLoadedMessageIdDao
                    ).also { instance = it }
            }
        }
    }

}