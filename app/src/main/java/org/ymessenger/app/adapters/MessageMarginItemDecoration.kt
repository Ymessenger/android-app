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

package org.ymessenger.app.adapters

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.entities.Message

class MessageMarginItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val marginHorizontal = context.resources.getDimensionPixelSize(R.dimen.gapSmall)
    private val marginVertical =
        context.resources.getDimensionPixelSize(R.dimen.message_cloud_vertical_margin)

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {

        val adapter = parent.adapter as IMessagesAdapter

        val position = parent.getChildAdapterPosition(view)

        if (position > 0) {
            val curMessage = adapter.getMessage(position)
            val nextMessage: Message = adapter.getMessage(position - 1)
            if (areMessagesRelated(curMessage, nextMessage)) {
                outRect.bottom = 0
            } else {
                outRect.bottom = marginVertical
            }
        } else {
            outRect.bottom = marginVertical * 4
        }

        if (adapter.showPhoto()) {
            outRect.left = marginHorizontal
        } else {
            outRect.left = 0
        }
        outRect.top = marginVertical
    }

    private fun areMessagesRelated(curMessage: Message, nextMessage: Message): Boolean {
        return curMessage.senderId == nextMessage.senderId &&
                (nextMessage.sentAt - curMessage.sentAt) < MIN_TIME_RELATED_MESSAGE
    }

    companion object {
        private const val MIN_TIME_RELATED_MESSAGE = 3 * 60 // 3 min
    }

}