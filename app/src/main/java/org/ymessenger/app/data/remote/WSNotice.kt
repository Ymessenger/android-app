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

open class WSNotice(
    @SerializedName("Code")
    val code: Int
) : CommunicationObject(TYPE_NOTICE) {
    object NoticeCode {
        const val NEW_MESSAGE = 0
        const val NEED_VERIFY = 1
        const val NEW_NODE = 2
        const val NEW_CHAT = 3
        const val USERS_ADDED_TO_CHAT = 4
        const val MESSAGES_READED = 5
        const val NEED_LOGIN = 6
        const val EDIT_CHAT = 7
        const val CHAT_USERS_CHANGED = 8
        const val MESSAGES_UPDATED = 9
        const val ENCRYPTED_KEYS = 12
        const val NEW_SESSION = 13
        const val USER_ACTION = 14
    }

    fun getNoticeTypeName() = when (code) {
        NoticeCode.NEW_MESSAGE -> "New Message"
        NoticeCode.NEED_VERIFY -> "Need Verify"
        NoticeCode.NEW_NODE -> "New Node"
        NoticeCode.NEW_CHAT -> "New Chat"
        NoticeCode.USERS_ADDED_TO_CHAT -> "Users Added To Chat"
        NoticeCode.MESSAGES_READED -> "Messages Read"
        NoticeCode.NEED_LOGIN -> "Need Login"
        NoticeCode.EDIT_CHAT -> "Edit Chat"
        NoticeCode.CHAT_USERS_CHANGED -> "Chat Users Changed"
        NoticeCode.MESSAGES_UPDATED -> "Messages Updated"
        NoticeCode.ENCRYPTED_KEYS -> "Encrypted Keys"
        NoticeCode.NEW_SESSION -> "New Session"
        NoticeCode.USER_ACTION -> "User Action"
        else -> "Unknown Notice Type"
    }
}