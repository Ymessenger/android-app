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
import org.ymessenger.app.utils.fromCamelCase
import kotlin.random.Random

abstract class WSRequest(
    @SerializedName("RequestType")
    val requestType: Int
) : CommunicationObject(TYPE_REQUEST) {
    @SerializedName("RequestId")
    val requestId: Long = Random.nextLong(Long.MAX_VALUE)

    object RequestType {
        const val ADD_USERS_CHATS = 1
        const val BLOCK_USERS = 2
        const val CHANGE_CHAT_USERS = 3
        const val CONFIRM_USER = 4
        const val DELETE_CONVERSATION = 5
        const val DELETE_FILES = 6
        const val DELETE_USER = 7
        const val DOWNLOAD_FILE = 8
        const val EDIT_CHATS = 9
        const val EDIT_USER = 10
        const val GET_ALL_CHAT_INFORMATION_NODE = 11
        const val GET_ALL_USER_CONVERSATIONS = 12
        const val GET_ALL_USERS_INFORMATION_NODE = 13
        const val GET_CHATS_INFORMATION_NODE = 15
        const val GET_CHAT_USERS = 17
        const val GET_DIALOGS = 18
        const val GET_FILES_INFORMATION = 19
        const val GET_USER_FILES_INFORMATIONS = 20
        const val GET_INFORMATION_NODE = 21
        const val GET_INFORMATION_WEB = 22
        const val GET_MESSAGES = 25
        const val GET_NODES_INFORMATION_NODE = 26
        const val GET_SELF = 27
        const val GET_USERS = 28
        const val GET_USERS_INFORMATION_NODE = 30
        const val LOGIN = 33
        const val LOGOUT = 34
        const val MESSAGES_READ = 35
        const val NEW_CHATS = 36
        const val NEW_USER = 37
        const val REFRESH_TOKENS = 38
        const val SEND_MESSAGES = 39
        const val UNBLOCK_USERS = 40
        const val UPLOAD_FILES = 41
        const val VERIFICATION_USER = 42
        const val DELETE_MESSAGES = 43
        const val GET_MESSAGES_UPDATES = 44
        const val GET_CHATS = 45
        const val SEARCH = 46
        const val CREATE_CHANNEL = 47
        const val ADD_USERS_TO_CHANNELS = 48
        const val EDIT_CHANNEL = 49
        const val EDIT_CHANNEL_USERS = 50
        const val GET_CHANNEL_USERS = 51
        const val GET_CHANNELS = 52
        const val POLLING = 53
        const val GET_POLL_VOTED_USERS = 54
        const val CREATE_OR_EDIT_GROUP = 56
        const val DELETE_GROUPS = 57
        const val ADD_USERS_TO_GROUP = 58
        const val REMOVE_USERS_FROM_GROUP = 59
        const val GET_GROUP_CONTACTS = 60
        const val GET_USER_GROUPS = 61
        const val CREATE_OR_EDIT_CONTACT = 62
        const val DELETE_CONTACTS = 63
        const val GET_USER_CONTACTS = 64
        const val GET_DEVICES_PRIVATE_KEYS = 70
        const val GET_SESSIONS = 71
        const val SEARCH_MESSAGES = 72
        const val MUTE_CONVERSATION = 73
        const val GET_QR_CODE = 74
        const val CHECK_QR_CODE = 75
        const val GET_USERS_BY_PHONES = 76
        const val EDIT_PHONE_OR_EMAIL = 77
        const val SEND_USER_ACTION = 79
        const val ADD_NEW_KEYS = 100
        const val DELETE_KEYS = 101
        const val GET_RANDOM_SEQUENCE = 102
        const val GET_USER_KEYS = 105
        const val VERIFY_NODE = 201
        const val SET_CONNECTION_ENCRYPTED = 255
    }

    fun getRequestTypeName(): String {
        return this.javaClass.simpleName.fromCamelCase()
    }
}