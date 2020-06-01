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

package org.ymessenger.app.adapters.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import kotlinx.android.synthetic.main.item_chat_list.view.*
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.entities.Chat

class ChatViewHolder(
    itemView: View,
    private val glide: RequestManager,
    private val onItemClick: (Chat) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    var chat: Chat? = null

    fun bind(chat: Chat) {
        this.chat = chat

        itemView.tvPhotoLabel.text = chat.getPhotoLabel()
        itemView.tvTitle.text = chat.name
        itemView.tvAbout.text = chat.about
        glide.load(chat.getPhotoUrl()).into(itemView.ivPhoto)

        itemView.setOnClickListener { onItemClick(chat) }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            glide: RequestManager,
            onItemClick: (Chat) -> Unit
        ): ChatViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_list, parent, false)
            return ChatViewHolder(view, glide, onItemClick)
        }
    }

}