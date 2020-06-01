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

package org.ymessenger.app.data

import android.content.Context
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.WSResponse

object ErrorCodeHelper {
    fun getErrorMessage(errorCode: Int) = getErrorMessageResource(errorCode)

    fun getErrorMessage(errorCode: Int, context: Context) =
        context.getString(getErrorMessageResource(errorCode))

    private fun getErrorMessageResource(errorCode: Int) = when (errorCode) {
        WSResponse.ErrorCode.DELETE_CHATS_PROBLEM -> R.string.delete_chats_problem
        WSResponse.ErrorCode.PERMISSION_DENIED -> R.string.ec_permission_denied
        WSResponse.ErrorCode.UNKNOWN_ERROR -> R.string.unknown_error
        WSResponse.ErrorCode.SEND_VERIFICATION_CODE_ERROR -> R.string.failed_to_send_verification_code
        WSResponse.ErrorCode.POLL_VOTING_PROBLEM -> R.string.poll_voting_problem
        WSResponse.ErrorCode.INVALID_REQUEST_DATA -> R.string.invalid_request_data
        WSResponse.ErrorCode.WRONG_VERIFICATION_CODE -> R.string.wrong_verification_code
        else -> R.string.unknown_error
    }
}