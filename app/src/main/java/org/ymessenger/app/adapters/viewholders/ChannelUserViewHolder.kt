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
import kotlinx.android.synthetic.main.item_channel_user.view.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.ChannelUsersPagedAdapter
import org.ymessenger.app.data.local.db.models.ChannelUserModel

class ChannelUserViewHolder(
    itemView: View,
    private val glide: RequestManager,
    channelUserClickListeners: ChannelUsersPagedAdapter.ChannelUserClickListeners
) : RecyclerView.ViewHolder(itemView) {

    private var item: ChannelUserModel? = null

    init {
        itemView.setOnClickListener {
            item?.let {
                channelUserClickListeners.onItemClick(it)
            }
        }

        itemView.setOnLongClickListener {
            item?.let {
                channelUserClickListeners.onLongItemClick(it)
                true
            } ?: false
        }
    }

    fun bind(item: ChannelUserModel) {
        this.item = item
        val context = itemView.context

        itemView.tvUserName.text =
            item.getUserModel()?.getDisplayName() ?: context.getString(R.string.name_is_hidden)

        itemView.tvUserRole.visibility =
            if (item.isAdmin() || item.isCreator()) View.VISIBLE else View.GONE
        itemView.tvUserRole.text = context.getString(
            when {
                item.isCreator() -> R.string.creator
                item.isAdmin() -> R.string.admin
                else -> R.string.undefined
            }
        )

        itemView.tvPhotoLabel.text = item.getPhotoLabel()
        val photoUrl = item.getUser()?.getPhotoUrl()
        if (photoUrl != null) {
            glide.load(photoUrl).into(itemView.ivUserPhoto)
            itemView.ivUserPhoto.visibility = View.VISIBLE
        } else {
            itemView.ivUserPhoto.visibility = View.INVISIBLE
        }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            glide: RequestManager,
            channelUserClickListeners: ChannelUsersPagedAdapter.ChannelUserClickListeners
        ): ChannelUserViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_channel_user, parent, false)
            return ChannelUserViewHolder(view, glide, channelUserClickListeners)
        }
    }

}