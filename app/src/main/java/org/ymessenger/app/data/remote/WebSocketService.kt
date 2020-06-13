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

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.collection.LongSparseArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.ymessenger.app.data.remote.clientRequests.GetKeys
import org.ymessenger.app.data.remote.clientResponses.EncryptedData
import org.ymessenger.app.data.remote.clientResponses.Error
import org.ymessenger.app.data.remote.entities.Attachment
import org.ymessenger.app.data.remote.notices.*
import org.ymessenger.app.data.remote.requests.*
import org.ymessenger.app.data.remote.responses.*
import org.ymessenger.app.helpers.EncryptionWrapper
import org.ymessenger.app.utils.AppExecutors
import java.lang.reflect.Type

class WebSocketService(
    private val encryptionWrapper: EncryptionWrapper
) {

    private lateinit var webSocket: WebSocket
    private val gson = GsonBuilder()
//        .disableHtmlEscaping()
        .registerTypeAdapter(Attachment::class.java, AttachmentsDeserializer())
        .create()
    private val connectionStatus = MutableLiveData<Boolean>()
    private val responseCallbackArray = LongSparseArray<ResponseCallback<WSResponse>>()
    private val timeoutCallbackArray = LongSparseArray<WebSocketTimeoutCallback>()

    private var notificationHandler: NotificationHandler? = null
    private var clientRequestHandler: ClientRequestHandler? = null

    private val client = OkHttpClient()
    private var request: Request? = null

    private var symmetricKey: ByteArray? = null
    private var myPrivateSignKey: ByteArray? = null
    private var nodePublicSignKey: ByteArray? = null
    private var encryptedConnection = false

    /**
     * Flag to determine if it's manual reconnect (not lost internet connection)
     */
    private var reconnect = false

    /**
     * This callback calls when call reconnect to web socket
     */
    var onReconnect: (() -> Unit)? = null

    /**
     * This callback calls after successful reconnect to web socket
     */
    var onReconnected: (() -> Unit)? = null

    var onConnected: (() -> Unit)? = null


    private val wsListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            Log.d(TAG, "onOpen")
            setConnectionStatus(true)
            if (reconnect) {
                onReconnected?.invoke()
            }

            onConnected?.invoke()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            Log.e(TAG, "onFailure, ${t.message}")
            setConnectionStatus(false)
            reconnect()
            if (encryptedConnection) {
                endEncryptedConnection()
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
            Log.d(TAG, "onClosing, code - $code, reason - $reason")
            setConnectionStatus(false)
            reconnect()
            if (encryptedConnection) {
                endEncryptedConnection()
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            Log.d(TAG, "<<< onMessage received: $text")
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
            if (encryptedConnection && !bytes.utf8().startsWith('{')) {
                // FIXME:
                AppExecutors.getInstance().networkIO.execute {
                    try {
                        val decryptedData = decryptData(bytes.toByteArray())
                        handleMessage(decryptedData)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to decrypt data")
                        e.printStackTrace()
                    }
                }
            } else {
                val data = bytes.utf8()
                handleMessage(data)
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            Log.d(TAG, "onClosed. Code - $code, reason - $reason")
            setConnectionStatus(false)
            if (encryptedConnection) {
                endEncryptedConnection()
            }
        }
    }

    fun connect() {
        request = Request.Builder().url(UrlGenerator.getWebSocketUrl()).build()
        connectToWebSocket()
    }

    private fun connectToWebSocket() {
        if (request == null) {
            Log.e(TAG, "Cannot connect to web socket: request is null")
        } else {
            // HOTFIX: Always end encryption before new connection
            if (encryptedConnection) {
                endEncryptedConnection()
            }

            Log.d(TAG, "Connection to web socket... ${request!!.url}")
            setConnectionStatus(false)
            webSocket = client.newWebSocket(request!!, wsListener)
        }
    }

    /**
     * Trying to reconnect to web socket with interval 5 seconds
     */
    private fun reconnect() {
        onReconnect?.invoke()

        reconnect = true
        val delay = 5L * 1000
        Log.d(TAG, "Reconnect in $delay ms")

        Handler(Looper.getMainLooper()).postDelayed({
            connect()
        }, delay)
    }

    fun forceReconnect() {
        Log.d(TAG, "Force reconnect")
        if (encryptedConnection) {
            endEncryptedConnection()
        }
        reconnect = true
        connectToWebSocket()
    }

    private fun setConnectionStatus(status: Boolean) {
        connectionStatus.postValue(status)
    }

    fun setNotificationHandler(notificationHandler: NotificationHandler) {
        this.notificationHandler = notificationHandler
    }

    fun setClientRequestHandler(clientRequestHandler: ClientRequestHandler) {
        this.clientRequestHandler = clientRequestHandler
    }

    fun getConnectionStatus(): LiveData<Boolean> = connectionStatus

    /**
     * This method determines what type of communication object is come and passes it
     * to the required handler (response, notice)
     *
     * @param data Raw string text of received message
     */
    private fun handleMessage(data: String) {
        try {
            val communicationObject = gson.fromJson(data, CommunicationObject::class.java)
            when (communicationObject.type) {
                CommunicationObject.TYPE_RESPONSE -> processResponse(data)
                CommunicationObject.TYPE_NOTICE -> processNotice(data)
                CommunicationObject.TYPE_REQUEST -> Log.w(
                    TAG,
                    "This is income message (response, notice), but type is 'request'"
                )
                CommunicationObject.TYPE_CLIENT_REQUEST -> processClientRequest(data)
                else -> Log.e(TAG, "Unknown type of communication object")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle income message")
            e.printStackTrace()
        }
    }

    /**
     * Determines response type and converts response to object
     *
     * @param data Raw message text
     */
    private fun processResponse(data: String) {
        try {
            // First we need to figure it out what type of response we got
            val generalResponse = gson.fromJson(data, WSResponse::class.java)
            logResponse(generalResponse.getResponseTypeName(), data)

            var response: WSResponse? = null
            when (generalResponse.responseType) {
                WSResponse.ResponseType.TOKENS -> {
                    response = gson.fromJson(data, Tokens::class.java)
                    Log.d(TAG, "Authorization successful. User id ${response.token.userId}")
                }

                WSResponse.ResponseType.USER -> {
                    response = gson.fromJson(data, User::class.java)
                    Log.d(TAG, "Get user with id ${response.user.id}")
                }

                WSResponse.ResponseType.USERS -> {
                    response = gson.fromJson(data, Users::class.java)
                    Log.d(TAG, "Users information size ${response.users.size}")
                }

                WSResponse.ResponseType.CHAT_USERS -> {
                    response = gson.fromJson(data, ChatUsers::class.java)
                    Log.d(TAG, "Chat users information size ${response.chatUsers.size}")
                }

                WSResponse.ResponseType.CONVERSATIONS -> {
                    response = gson.fromJson(data, Conversations::class.java)
                    Log.d(TAG, "Conversations size ${response.conversations.size}")
                }

                WSResponse.ResponseType.MESSAGES -> {
                    response = gson.fromJson(data, Messages::class.java)
                    Log.d(TAG, "Messages size ${response.messages.size}")
                }

                WSResponse.ResponseType.CHATS -> {
                    response = gson.fromJson(data, Chats::class.java)
                    Log.d(TAG, "Chats size ${response.chats.size}")
                }

                WSResponse.ResponseType.UPDATED_MESSAGES -> {
                    response = gson.fromJson(data, UpdatedMessages::class.java)
                    Log.d(
                        TAG,
                        "Deleted messages ${response.deleted?.size}, updated messages ${response.updated?.size}"
                    )
                }

                WSResponse.ResponseType.CHANNELS -> {
                    response = gson.fromJson(data, Channels::class.java)
                    Log.d(TAG, "Channels size ${response.channels.size}")
                }

                WSResponse.ResponseType.CHANNEL_USERS -> {
                    response = gson.fromJson(data, ChannelUsers::class.java)
                    Log.d(
                        TAG,
                        "Channel users. Administrations size ${response.administration?.size}, Subscribers size ${response.subscribers?.size}, BlockedUsers size ${response.blockedUsers?.size}"
                    )
                }

                WSResponse.ResponseType.KEYS -> {
                    response = gson.fromJson(data, Keys::class.java)
                    Log.d(TAG, "Keys size ${response.keys.size}")
                }

                WSResponse.ResponseType.POLL -> {
                    response = gson.fromJson(data, Polls::class.java)
                    Log.d(TAG, "Poll with id ${response.poll.pollId}")
                }

                WSResponse.ResponseType.CONTACTS -> {
                    response = gson.fromJson(data, Contacts::class.java)
                    Log.d(TAG, "Contacts size ${response.contacts.size}")
                }

                WSResponse.ResponseType.GROUPS -> {
                    response = gson.fromJson(data, Groups::class.java)
                    Log.d(TAG, "Groups size ${response.groups.size}")
                }

                WSResponse.ResponseType.SEARCH_RESULT -> {
                    response = gson.fromJson(data, SearchResult::class.java)
                    Log.d(
                        TAG,
                        "Users ${response.users.size}, Chats ${response.chats.size}, Channels ${response.channels.size}"
                    )
                }

                WSResponse.ResponseType.SESSIONS -> {
                    response = gson.fromJson(data, Sessions::class.java)
                    Log.d(TAG, "Sessions size ${response.sessions.size}")
                }

                WSResponse.ResponseType.NODES -> {
                    response = gson.fromJson(data, Nodes::class.java)
                    Log.d(TAG, "Nodes size ${response.nodes.size}")
                }

                WSResponse.ResponseType.SEQUENCE -> {
                    response = gson.fromJson(data, Sequence::class.java)
                    Log.d(TAG, "Sequence length ${response.sequence.length}")
                }

                WSResponse.ResponseType.ENCRYPTED_KEY -> {
                    response = gson.fromJson(data, EncryptedKey::class.java)
                    Log.d(TAG, "Encrypted data length ${response.encryptedData.length}")
                }

                WSResponse.ResponseType.POLL_RESULTS -> {
                    response = gson.fromJson(data, PollResult::class.java)
                    Log.d(TAG, "Poll results size ${response.pollResults.size}")
                }

                WSResponse.ResponseType.QRCODE -> {
                    response = gson.fromJson(data, QRCode::class.java)
                    Log.d(TAG, "QR code with id ${response.qrCode.id}")
                }

                WSResponse.ResponseType.OPERATION_ID -> {
                    response = gson.fromJson(data, OperationId::class.java)
                    Log.d(TAG, "Operation Id ${response.operationId}")
                }

                WSResponse.ResponseType.RESULT_RESPONSE -> {
                    response = gson.fromJson(data, ResultResponse::class.java)
                    if (response.errorCode != WSResponse.ErrorCode.NULL) {
                        val message = response.message?.let {
                            " with message '$it'"
                        } ?: ""
                        Log.e(TAG, "Response results with error code ${response.errorCode}$message")
                    }
                }

                else -> Log.e(TAG, "There is no handler for this response type")
            }

            response?.let {
                returnResponse(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Logging of web socket actions such as send request and receive response
     *
     * @param direction Direction of action (request, response or notice)
     * @param type Type of action
     * @param data Raw data
     */
    private fun logAction(direction: String, type: String, data: String) {
        val signProtected = if (encryptedConnection) " $SIGN_PROTECTED" else ""
        Log.d(TAG, "$direction$signProtected [$type] :: $data")
    }

    /**
     * Logging of web socket requests
     *
     * @param type Type of request
     * @param data Raw data
     */
    private fun logRequest(type: String, data: String) {
        logAction(DIRECTION_REQUEST, type, data)
    }

    /**
     * Logging of web socket responses
     *
     * @param type Type of response
     * @param data Raw data
     */
    private fun logResponse(type: String, data: String) {
        logAction(DIRECTION_RESPONSE, type, data)
    }

    /**
     * Logging of web socket notices
     *
     * @param type Type of notice
     * @param data Raw data
     */
    private fun logNotice(type: String, data: String) {
        logAction(DIRECTION_NOTICE, type, data)
    }

    /**
     * Logging of web socket client requests
     *
     * @param type Type of request
     * @param data Raw data
     */
    private fun logClientRequest(type: String, data: String) {
        logAction(DIRECTION_CLIENT_REQUEST, type, data)
    }

    /**
     * Logging of web socket client responses
     *
     * @param type Type of response
     * @param data Raw data
     */
    private fun logClientResponse(type: String, data: String) {
        logAction(DIRECTION_CLIENT_RESPONSE, type, data)
    }

    /**
     * Determines type of notice, builds notice object and passes it to notification handler
     *
     * @param data Raw notice message text
     */
    private fun processNotice(data: String) {
        try {
            val notice = gson.fromJson(data, WSNotice::class.java)
            logNotice(notice.getNoticeTypeName(), data)
            when (notice.code) {
                WSNotice.NoticeCode.MESSAGES_READED -> {
                    val messagesRead = gson.fromJson(data, MessagesAreRead::class.java)
                    Log.d(TAG, "Message ${messagesRead.messagesId} has been read")
                    notificationHandler?.messagesAreRead(messagesRead)
                }

                WSNotice.NoticeCode.NEW_MESSAGE -> {
                    val newMessage = gson.fromJson(data, NewMessage::class.java)
                    Log.d(
                        TAG,
                        "New message, text:\"${newMessage.message.text}\" guid: \"${newMessage.message.globalId}\""
                    )
                    notificationHandler?.newMessage(newMessage)
                }

                WSNotice.NoticeCode.NEW_CHAT -> {
                    val newChat = gson.fromJson(data, NewChat::class.java)
                    Log.d(
                        TAG,
                        "New chat, name:\"${newChat.chat.name}\" id: \"${newChat.chat.id}\""
                    )
                    notificationHandler?.newChat(newChat)
                }

                WSNotice.NoticeCode.EDIT_CHAT -> {
                    val editChat = gson.fromJson(data, EditChat::class.java)
                    Log.d(
                        TAG,
                        "Chat edited, name:\"${editChat.chat.name}\" id: \"${editChat.chat.id}\""
                    )
                    notificationHandler?.chatEdited(editChat)
                }

                WSNotice.NoticeCode.USERS_ADDED_TO_CHAT -> {
                    val usersAddedToChat = gson.fromJson(data, UsersAddedToChat::class.java)
                    Log.d(TAG, "New chat users, size = ${usersAddedToChat.newUsers.size}")
                    notificationHandler?.usersAddedToChat(usersAddedToChat)
                }

                WSNotice.NoticeCode.CHAT_USERS_CHANGED -> {
                    val chatUsersChanged = gson.fromJson(data, ChatUsersChanged::class.java)
                    Log.d(TAG, "Chat users changed, size = ${chatUsersChanged.chatUsers.size}")
                    notificationHandler?.chatUsersChanged(chatUsersChanged)
                }

                WSNotice.NoticeCode.MESSAGES_UPDATED -> {
                    val messagesUpdated = gson.fromJson(data, MessagesUpdated::class.java)
                    Log.d(
                        TAG,
                        "Messages deleted ${messagesUpdated.deleted?.messageIds?.size}, updated ${messagesUpdated.updated?.messageIds?.size}"
                    )
                    notificationHandler?.messagesUpdated(messagesUpdated)
                }

                WSNotice.NoticeCode.ENCRYPTED_KEYS -> {
                    val encryptedKeys = gson.fromJson(data, EncryptedKeys::class.java)
                    Log.d(
                        TAG,
                        "Private keys from: Device - ${encryptedKeys.deviceName}, OS - ${encryptedKeys.deviceOSName}, app - ${encryptedKeys.appName}"
                    )
                    notificationHandler?.encryptedKeys(encryptedKeys)
                }

                WSNotice.NoticeCode.NEW_SESSION -> {
                    val newSession = gson.fromJson(data, NewSession::class.java)
                    Log.d(
                        TAG,
                        "New session: Device - ${newSession.session.deviceName}, OS - ${newSession.session.OSName}, tokenId ${newSession.session.tokenId}"
                    )
                    notificationHandler?.newSession(newSession)
                }

                WSNotice.NoticeCode.NEED_LOGIN -> {
                    Log.d(TAG, "Need login!")
                    notificationHandler?.needLogin()
                }

                WSNotice.NoticeCode.USER_ACTION -> {
                    val newUserAction = gson.fromJson(data, NewUserAction::class.java)
                    Log.d(TAG, "New user action - ${newUserAction.action}")
                    notificationHandler?.newUserAction(newUserAction)
                }

                else -> Log.e(TAG, "There is no handler for this notice type")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processClientRequest(data: String) {
        try {
            val commonRequest = gson.fromJson(data, ClientRequest::class.java)
            logClientRequest(commonRequest.requestTypeName(), data)

            when (commonRequest.requestType) {
                ClientRequest.RequestType.GET_KEYS -> {
                    val getKeys = gson.fromJson(data, GetKeys::class.java)
                    Log.d(
                        TAG,
                        "Client request to get private keys. Public key: ${getKeys.publicKey}"
                    )

                    clientRequestHandler?.getKeys(getKeys)
                }

                else -> {
                    Log.e(TAG, "There is no handler for this client request type")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * This method founds callback by request id and passes response to it
     *
     * @param response Response object
     */
    private fun returnResponse(response: WSResponse) {
        // Find callback from pull of callbacks by request id
        val responseCallback = responseCallbackArray[response.requestId]
        if (responseCallback != null) {
            Handler(Looper.getMainLooper()).post {
                if (response.errorCode == WSResponse.ErrorCode.NULL) {
                    responseCallback.onResponse(response)
                } else {
                    responseCallback.onError(response as ResultResponse)
                }
            }
            unregisterCallback(response.requestId)
        } else {
            Log.e(TAG, "Callback is not found")
        }
    }


    // ****************************** METHODS FOR WEB SOCKET SERVER ********************************

    fun verificationUser(
        verificationUser: VerificationUser,
        responseCallback: ResponseCallback<ResultResponse>
    ) {
        sendRequest(verificationUser, responseCallback)
    }

    fun authorize(loginRequest: Login, responseCallback: ResponseCallback<Tokens>) {
        sendRequest(loginRequest, responseCallback)
    }

    fun newUser(newUser: NewUser, responseCallback: ResponseCallback<Tokens>) {
        sendRequest(newUser, responseCallback)
    }

    fun logout(logout: Logout, responseCallback: ResponseCallback<ResultResponse>) {
        sendRequest(logout, responseCallback)
    }

    fun getSelf(getSelf: GetSelf, responseCallback: ResponseCallback<User>) {
        sendRequest(getSelf, responseCallback)
    }

    fun getAllUsersInformationNode(
        getAllUsersInformationNode: GetAllUsersInformationNode,
        responseCallback: ResponseCallback<Users>
    ) {
        sendRequest(getAllUsersInformationNode, responseCallback)
    }

    fun getUsers(getUsers: GetUsers, responseCallback: ResponseCallback<Users>) {
        sendRequest(getUsers, responseCallback)
    }

    fun search(search: Search, responseCallback: ResponseCallback<SearchResult>) {
        sendRequest(search, responseCallback)
    }

    fun getChats(getChats: GetChats, responseCallback: ResponseCallback<Chats>) {
        sendRequest(getChats, responseCallback)
    }

    fun getDialogs(getDialogs: GetDialogs, responseCallback: ResponseCallback<Conversations>) {
        sendRequest(getDialogs, responseCallback)
    }

    fun getChatUsers(getChatUsers: GetChatUsers, responseCallback: ResponseCallback<ChatUsers>) {
        sendRequest(getChatUsers, responseCallback)
    }

    fun getAllUserConversations(
        getAllUserConversations: GetAllUserConversations,
        responseCallback: ResponseCallback<Conversations>
    ) {
        sendRequest(getAllUserConversations, responseCallback)
    }

    fun getMessages(getMessages: GetMessages, responseCallback: ResponseCallback<Messages>) {
        sendRequest(getMessages, responseCallback)
    }

    fun sendMessages(sendMessages: SendMessages, responseCallback: ResponseCallback<Messages>) {
        sendRequest(sendMessages, responseCallback)
    }

    fun messagesRead(
        messagesRead: MessagesRead,
        responseCallback: ResponseCallback<ResultResponse>
    ) {
        sendRequest(messagesRead, responseCallback)
    }

    fun deleteMessages(
        deleteMessages: DeleteMessages,
        responseCallback: ResponseCallback<UpdatedMessages>
    ) {
        sendRequest(deleteMessages, responseCallback)
    }

    fun getMessagesUpdates(
        getMessagesUpdates: GetMessagesUpdates,
        responseCallback: ResponseCallback<UpdatedMessages>
    ) {
        sendRequest(getMessagesUpdates, responseCallback)
    }

    fun editUser(editUser: EditUser, responseCallback: ResponseCallback<User>) {
        sendRequest(editUser, responseCallback)
    }

    fun editChats(editChats: EditChats, responseCallback: ResponseCallback<Chats>) {
        sendRequest(editChats, responseCallback)
    }

    fun newChats(newChats: NewChats, responseCallback: ResponseCallback<Chats>) {
        sendRequest(newChats, responseCallback)
    }

    fun deleteConversation(
        deleteConversation: DeleteConversation,
        responseCallback: ResponseCallback<WSResponse>
    ) {
        sendRequest(deleteConversation, responseCallback)
    }

    fun changeChatUsers(
        changeChatUsers: ChangeChatUsers,
        responseCallback: ResponseCallback<ChatUsers>
    ) {
        sendRequest(changeChatUsers, responseCallback)
    }

    fun addUsersChats(addUsersChats: AddUsersChats, responseCallback: ResponseCallback<ChatUsers>) {
        sendRequest(addUsersChats, responseCallback)
    }

    fun createChannel(createChannel: CreateChannel, responseCallback: ResponseCallback<Channels>) {
        sendRequest(createChannel, responseCallback)
    }

    fun getChannels(getChannels: GetChannels, responseCallback: ResponseCallback<Channels>) {
        sendRequest(getChannels, responseCallback)
    }

    fun getChannelUsers(
        getChannelUsers: GetChannelUsers,
        responseCallback: ResponseCallback<ChannelUsers>
    ) {
        sendRequest(getChannelUsers, responseCallback)
    }

    fun addUsersToChannels(
        addUsersToChannels: AddUsersToChannels,
        responseCallback: ResponseCallback<ChannelUsers>
    ) {
        sendRequest(addUsersToChannels, responseCallback)
    }

    fun editChannelUsers(
        editChannelUsers: EditChannelUsers,
        responseCallback: ResponseCallback<ChannelUsers>
    ) {
        sendRequest(editChannelUsers, responseCallback)
    }

    fun editChannel(editChannel: EditChannel, responseCallback: ResponseCallback<Channels>) {
        sendRequest(editChannel, responseCallback)
    }

    fun addNewKeys(addNewKeys: AddNewKeys, responseCallback: ResponseCallback<Keys>) {
        sendRequest(addNewKeys, responseCallback)
    }

    fun getUserKeys(getUserKeys: GetUserKeys, responseCallback: ResponseCallback<Keys>) {
        sendRequest(getUserKeys, responseCallback)
    }

    fun polling(polling: Polling, responseCallback: ResponseCallback<Polls>) {
        sendRequest(polling, responseCallback)
    }

    fun getPollVotedUsers(
        getPollVotedUsers: GetPollVotedUsers,
        responseCallback: ResponseCallback<PollResult>
    ) {
        sendRequest(getPollVotedUsers, responseCallback)
    }

    fun createOrEditGroup(
        createOrEditGroup: CreateOrEditGroup,
        responseCallback: ResponseCallback<Groups>
    ) {
        sendRequest(createOrEditGroup, responseCallback)
    }

    fun deleteGroups(
        deleteGroups: DeleteGroups,
        responseCallback: ResponseCallback<ResultResponse>
    ) {
        sendRequest(deleteGroups, responseCallback)
    }

    fun getGroupContacts(
        getGroupContacts: GetGroupContacts,
        responseCallback: ResponseCallback<Contacts>
    ) {
        sendRequest(getGroupContacts, responseCallback)
    }

    fun getUserGroups(getUserGroups: GetUserGroups, responseCallback: ResponseCallback<Groups>) {
        sendRequest(getUserGroups, responseCallback)
    }

    fun createOrEditContact(
        createOrEditContact: CreateOrEditContact,
        responseCallback: ResponseCallback<Contacts>
    ) {
        sendRequest(createOrEditContact, responseCallback)
    }

    fun deleteContacts(
        deleteContacts: DeleteContacts,
        responseCallback: ResponseCallback<ResultResponse>
    ) {
        sendRequest(deleteContacts, responseCallback)
    }

    fun getUserContacts(
        getUserContacts: GetUserContacts,
        responseCallback: ResponseCallback<Contacts>
    ) {
        sendRequest(getUserContacts, responseCallback)
    }

    fun addUsersToGroup(
        addUsersToGroup: AddUsersToGroup,
        responseCallback: ResponseCallback<ResultResponse>
    ) {
        sendRequest(addUsersToGroup, responseCallback)
    }

    fun removeUsersFromGroup(
        removeUsersFromGroup: RemoveUsersFromGroup,
        responseCallback: ResponseCallback<ResultResponse>
    ) {
        sendRequest(removeUsersFromGroup, responseCallback)
    }

    fun getSessions(getSessions: GetSessions, responseCallback: ResponseCallback<Sessions>) {
        sendRequest(getSessions, responseCallback)
    }

    fun verifyNode(verifyNode: VerifyNode, responseCallback: ResponseCallback<Sequence>) {
        sendRequest(verifyNode, responseCallback)
    }

    fun getRandomSequence(
        getRandomSequence: GetRandomSequence,
        responseCallback: ResponseCallback<Sequence>
    ) {
        sendRequest(getRandomSequence, responseCallback)
    }

    fun muteConversation(
        muteConversation: MuteConversation,
        responseCallback: ResponseCallback<ResultResponse>
    ) {
        sendRequest(muteConversation, responseCallback)
    }

    fun searchMessages(
        searchMessages: SearchMessages,
        responseCallback: ResponseCallback<Messages>
    ) {
        sendRequest(searchMessages, responseCallback)
    }

    fun getUsersByPhones(
        getUsersByPhones: GetUsersByPhones,
        responseCallback: ResponseCallback<Users>
    ) {
        sendRequest(getUsersByPhones, responseCallback)
    }

    fun editPhoneOrEmail(
        editPhoneOrEmail: EditPhoneOrEmail,
        responseCallback: ResponseCallback<Users>
    ) {
        sendRequest(editPhoneOrEmail, responseCallback)
    }

    fun getDevicesPrivateKeys(
        getDevicesPrivateKeys: GetDevicesPrivateKeys,
        responseCallback: ResponseCallback<ResultResponse>
    ) {
        sendRequest(getDevicesPrivateKeys, responseCallback)
    }

    fun getInformationNode(
        getInformationNode: GetInformationNode,
        responseCallback: ResponseCallback<Nodes>
    ) {
        sendRequest(getInformationNode, responseCallback)
    }

    fun setConnectionEncrypted(
        setConnectionEncrypted: SetConnectionEncrypted,
        responseCallback: ResponseCallback<EncryptedKey>
    ) {
        sendRequest(setConnectionEncrypted, responseCallback)
    }

    fun getQRCode(getQRCode: GetQRCode, responseCallback: ResponseCallback<QRCode>) {
        sendRequest(getQRCode, responseCallback)
    }

    fun checkQRCode(checkQRCode: CheckQRCode, responseCallback: ResponseCallback<Tokens>) {
        sendRequest(checkQRCode, responseCallback)
    }

    fun sendUserAction(
        sendUserAction: SendUserAction,
        responseCallback: ResponseCallback<ResultResponse>
    ) {
        sendRequest(sendUserAction, responseCallback)
    }

    fun changeNode(
        changeNode: ChangeNode,
        responseCallback: ResponseCallback<OperationId>
    ) {
        sendRequest(changeNode, responseCallback)
    }

    /**
     * This method adds request callback to pull of callbacks
     *
     * @param request Request object
     * @param responseCallback Callback object
     */
    private fun registerCallback(request: WSRequest, responseCallback: ResponseCallback<*>) {
        @Suppress("UNCHECKED_CAST")
        responseCallbackArray.put(
            request.requestId,
            responseCallback as ResponseCallback<WSResponse>
        )

        // Register timeout callback
        timeoutCallbackArray.put(request.requestId, object : WebSocketTimeoutCallback() {
            override fun onTimeout() {
                Log.e(
                    TAG,
                    "--- [${request.getRequestTypeName()}] :: Timeout ${Config.WEB_SOCKET_REQUEST_TIMEOUT / 1000} sec (requestId ${request.requestId})"
                )
                returnResponse(getTimeoutResponse(request.requestId))
            }
        })
    }

    private fun getTimeoutResponse(requestId: Long): ResultResponse {
        val timeoutResponse = ResultResponse("WebSocket Timeout")
        timeoutResponse.errorCode = WSResponse.ErrorCode.TIMEOUT
        timeoutResponse.requestId = requestId

        return timeoutResponse
    }

    /**
     * Removes response callback from pull of callbacks
     *
     * @param requestId Identifier of request
     */
    private fun unregisterCallback(requestId: Long) {
        responseCallbackArray.remove(requestId)

        timeoutCallbackArray[requestId]?.cancel()
        timeoutCallbackArray.remove(requestId)
    }

    /**
     * Sends request to web socket
     *
     * @param request Request object
     * @param responseCallback Callback object
     */
    private fun sendRequest(request: WSRequest, responseCallback: ResponseCallback<*>) {
        try {
            registerCallback(request, responseCallback)
            val data = gson.toJson(request)
            sendToWebSocket(data)
            logRequest(request.getRequestTypeName(), data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send request [${request.getRequestTypeName()}]")
            e.printStackTrace()
            unregisterCallback(request.requestId)
        }
    }

    /**
     * Sends client response to web socket
     *
     * @param clientResponse Response object
     */
    private fun sendClientResponse(clientResponse: ClientResponse) {
        try {
            val data = gson.toJson(clientResponse)
            sendToWebSocket(data)
            logClientResponse(clientResponse.getResponseTypeName(), data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send client response [${clientResponse.getResponseTypeName()}]")
            e.printStackTrace()
        }
    }

    private fun sendToWebSocket(data: String) {
        if (encryptedConnection) {
            // FIXME:
            AppExecutors.getInstance().networkIO.execute {
                try {
                    val encryptedData = encryptData(data)
                    val byteString = encryptedData.toByteString(0, encryptedData.size)
                    webSocket.send(byteString)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to encryptData")
                    e.printStackTrace()
                }
            }
        } else {
            webSocket.send(data)
        }
    }

    fun sendEncryptedData(encryptedData: EncryptedData) {
        sendClientResponse(encryptedData)
    }

    fun sendError(error: Error) {
        sendClientResponse(error)
    }

    /**
     * This function probably shouldn't be here. But this is the simplest solution.
     */
    fun updateChatPreviews() {
        notificationHandler?.updateChatPreviews()
    }

    interface ResponseCallback<in T : WSResponse> {
        fun onResponse(response: T)
        fun onError(error: ResultResponse)
    }

    fun startEncryptedConnection(
        symmetricKey: ByteArray,
        myPrivateSignKey: ByteArray,
        nodePublicSignKey: ByteArray
    ) {
        if (encryptionWrapper.isInitialized()) {
            Log.d(TAG, "Start encrypted connection")
            this.symmetricKey = symmetricKey
            this.myPrivateSignKey = myPrivateSignKey
            this.nodePublicSignKey = nodePublicSignKey
            encryptedConnection = true
        } else {
            Log.w(TAG, "Can't set encrypted connection. YEncrypt is not initialized yet.")
        }
    }

    private fun endEncryptedConnection() {
        Log.d(TAG, "Finish encrypted connection")
        this.symmetricKey = null
        this.myPrivateSignKey = null
        this.nodePublicSignKey = null
        encryptedConnection = false
    }

    private fun encryptData(data: String): ByteArray {
        if (encryptionWrapper.isInitialized()) {
            val yEncrypt = encryptionWrapper.getYEncrypt()
            val start = System.currentTimeMillis()
            yEncrypt.setSymmetricEncryptKey(symmetricKey)
            yEncrypt.privateSignKeyToSend = myPrivateSignKey
            val encryptedData = yEncrypt.encryptSecretMsg(1, 0, 1000, data.toByteArray())
            val encryptionTime = System.currentTimeMillis() - start
            Log.i(TAG, "Encryption time - $encryptionTime ms")

            return encryptedData
        } else {
            throw Exception("yEncrypt is not initialized")
        }
    }

    private fun decryptData(encryptedData: ByteArray): String {
        if (encryptionWrapper.isInitialized()) {
            val yEncrypt = encryptionWrapper.getYEncrypt()
            val start = System.currentTimeMillis()
            yEncrypt.setSymmetricEncryptKey(symmetricKey)
            yEncrypt.publicSignKeyToReceive = nodePublicSignKey
            try {
                val decryptedMsg = yEncrypt.decryptSecretMsg(encryptedData)
                val decryptionTime = System.currentTimeMillis() - start
                Log.i(TAG, "Decryption time - $decryptionTime ms")

                return String(decryptedMsg.msg)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrypt secret message")
                e.printStackTrace()
                throw Exception("Failed to decrypt secret message")
            }
        } else {
            throw Exception("yEncrypt is not initialized")
        }
    }

    companion object {
        private const val TAG = "WebSocketService"
        private const val DIRECTION_REQUEST = "-->"
        private const val DIRECTION_RESPONSE = "<--"
        private const val DIRECTION_NOTICE = "***"
        private const val DIRECTION_CLIENT_REQUEST = "<<-"
        private const val DIRECTION_CLIENT_RESPONSE = "->>"

        private const val SIGN_PROTECTED = "\uD83D\uDD12" // Lock symbol
    }

    class AttachmentsDeserializer : JsonDeserializer<Attachment> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): Attachment {
            Log.d("PayloadDeserializer", json?.toString())
            val jsonObject = json?.asJsonObject!!
            return Attachment(
                jsonObject.get("Type")?.asInt,
                context!!.deserialize(jsonObject.get("Hash"), String::class.java),
                0,
                jsonObject.get("Payload").toString(),
                null
            )
        }
    }
}