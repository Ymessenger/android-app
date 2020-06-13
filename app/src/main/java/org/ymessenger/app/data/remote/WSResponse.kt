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

import com.google.gson.annotations.SerializedName

open class WSResponse : CommunicationObject(TYPE_RESPONSE) {
    @SerializedName("RequestId")
    var requestId: Long = 0
    @SerializedName("ResponseType")
    var responseType: Int = 0
    @SerializedName("ErrorCode")
    var errorCode: Int = 0

    object ResponseType {
        const val USERS = 0
        const val NODES = 1
        const val CHATS = 2
        const val FOUND_USERS = 4
        const val FOUND_CHATS = 5
        const val TOKENS = 6
        const val FILE = 7
        const val FILES = 8
        const val MESSAGES = 9
        const val RESULT_RESPONSE = 10
        const val USER = 12
        const val CONVERSATIONS = 15
        const val CHAT_USERS = 16
        const val UPDATED_MESSAGES = 17
        const val KEYS = 18
        const val SEQUENCE = 19
        const val CHANNELS = 20
        const val CHANNEL_USERS = 21
        const val ENCRYPTED_KEY = 22
        const val SEARCH_RESULT = 23
        const val OPERATION_ID = 24
        const val POLL = 25
        const val GROUPS = 26
        const val CONTACTS = 27
        const val SESSIONS = 29
        const val QRCODE = 30
        const val POLL_RESULTS = 31
    }

    object ErrorCode {
        const val TIMEOUT = -1
        const val NULL = 0
        const val INVALID_REQUEST_FORMAT = 1
        const val REFRESH_TOKEN_TIMEOUT = 2
        const val ACCESS_TOKEN_TIMEOUT = 3
        const val USER_NOT_FOUND = 4
        const val INVALID_ACCESS_TOKEN = 5
        const val USER_NOT_AUTHORIZED = 6
        const val DELETE_CHATS_PROBLEM = 7
        const val CREATE_VERIFICATION_CODE_PROBLEM = 8
        const val GET_USERS_PROBLEM = 9
        const val CREATE_CHAT_PROBLEM = 10
        const val WRONG_VERIFICATION_CODE = 11
        const val SEND_MESSAGE_PROBLEM = 12
        const val DELETE_USERS_FROM_CHAT_PROBLEM = 13
        const val ADD_USER_TO_CHAT_PROBLEM = 14
        const val READ_MESSAGE_PROBLEM = 15
        const val UNKNOWN_ERROR = 16
        const val PERMISSION_DENIED = 17
        const val WRONG_ARGUMENT_ERROR = 18
        const val GET_CHATS_PROBLEM = 19
        const val UPLOAD_FILE_PROBLEM = 20
        const val USER_BLOCKED = 21
        const val AUTHORIZATION_PROBLEM = 22
        const val INVALID_REQUEST_DATA = 23
        const val TOO_MANY_REQUESTS = 24
        const val TOO_LARGE_REQUEST_DATA = 25
        const val GET_MESSAGES_PROBLEM = 26
        const val DELETE_FILES_PROBLEM = 27
        const val USER_IS_NOT_IN_CHAT = 28
        const val INVALID_ATTACHMENT = 29
        const val CHAT_IS_NOT_VALID = 30
        const val SEND_VERIFICATION_CODE_ERROR = 31
        const val CHECK_QR_PROBLEM = 38
        const val POLL_VOTING_PROBLEM = 39
    }

    fun getResponseTypeName() = when (responseType) {
        ResponseType.USERS -> "Users"
        ResponseType.NODES -> "Nodes"
        ResponseType.CHATS -> "Chats"
        ResponseType.FOUND_USERS -> "Found Users"
        ResponseType.FOUND_CHATS -> "Found Chats"
        ResponseType.TOKENS -> "Tokens"
        ResponseType.FILE -> "File"
        ResponseType.FILES -> "Files"
        ResponseType.MESSAGES -> "Messages"
        ResponseType.RESULT_RESPONSE -> "Result Response"
        ResponseType.USER -> "User"
        ResponseType.CONVERSATIONS -> "Conversations"
        ResponseType.CHAT_USERS -> "Chat Users"
        ResponseType.UPDATED_MESSAGES -> "Updated Messages"
        ResponseType.KEYS -> "Keys"
        ResponseType.SEQUENCE -> "Sequence"
        ResponseType.CHANNELS -> "Channels"
        ResponseType.CHANNEL_USERS -> "Channel Users"
        ResponseType.ENCRYPTED_KEY -> "Encrypted Key"
        ResponseType.SEARCH_RESULT -> "Search Result"
        ResponseType.OPERATION_ID -> "Operation Id"
        ResponseType.POLL -> "Poll"
        ResponseType.GROUPS -> "Groups"
        ResponseType.CONTACTS -> "Contacts"
        ResponseType.SESSIONS -> "Sessions"
        ResponseType.QRCODE -> "QRCode"
        ResponseType.POLL_RESULTS -> "Poll Results"
        else -> "Unknown Response Type"
    }
}