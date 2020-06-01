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

package org.ymessenger.app.data.remote.entities

import com.google.gson.annotations.SerializedName

data class Poll(
    @SerializedName("Title")
    val title: String,
    @SerializedName("MultipleSelection")
    val multipleSelection: Boolean,
    @SerializedName("ResultsVisibility")
    val resultsVisibility: Boolean,
    @SerializedName("PollOptions")
    val pollOptions: List<PollOption>,
    @SerializedName("SignRequired")
    val signRequired: Boolean = false
) {
    @SerializedName("PollId")
    var pollId: String? = null

    @SerializedName("ConversationId")
    var conversationId: Long = 0

    @SerializedName("ConversationType")
    var conversationType: Int = 0

    @SerializedName("Voted")
    var voted: Boolean = false
}