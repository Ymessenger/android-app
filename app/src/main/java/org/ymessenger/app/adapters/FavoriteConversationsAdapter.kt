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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.item_user_avatar.view.*
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.models.FavoriteConversationModel

class FavoriteConversationsAdapter(
    private val onItemClick: (FavoriteConversationModel?) -> Unit,
    private val onItemMoved: ((List<FavoriteConversationModel>) -> Unit),
    private val selectMode: Boolean = false
) : ListAdapter<FavoriteConversationModel, RecyclerView.ViewHolder>(
    FavoriteConversationsDiffCallback()
), ItemTouchHelperAdapter {

    companion object {
        const val TYPE_USER = 0
        const val TYPE_ADD = -1
    }

    override fun getItemViewType(position: Int): Int = when (position) {
        itemCount - 1 -> if (selectMode) TYPE_USER else TYPE_ADD
        else -> TYPE_USER
    }

    override fun getItemCount(): Int {
        return if (selectMode) {
            super.getItemCount()
        } else {
            super.getItemCount() + 1 // Because of add button
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_USER -> (holder as FavoriteConversationHolder).bind(getItem(position))
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val payloadObject = payloads[0] as FavoriteConversationsDiffCallback.Payloads
            val favoriteConversationModel = getItem(position)
            val favoriteConversationHolder = holder as FavoriteConversationHolder

            if (payloadObject.photoChanged) {
                favoriteConversationHolder.updatePhoto(favoriteConversationModel)
            }

            if (payloadObject.userOnlineChanged) {
                favoriteConversationHolder.updateOnline(favoriteConversationModel.isOnline())
            }

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        type: Int
    ): RecyclerView.ViewHolder {
        return when (type) {
            TYPE_ADD -> AddButtonHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_user_add_favourites,
                    parent,
                    false
                ),
                onItemClick
            )
            else -> FavoriteConversationHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_user_avatar,
                    parent,
                    false
                ),
                onItemClick
            )
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        // We take currentList, make movement and update all rows in database
        val currentListImmutable = ArrayList(currentList)
        val contactGroup = getItem(fromPosition)
        currentListImmutable.removeAt(fromPosition)
        currentListImmutable.add(toPosition, contactGroup)
        onItemMoved.invoke(currentListImmutable)
    }

    override fun onItemDismiss(position: Int) {
        // nothing
    }

    class FavoriteConversationHolder(
        itemView: View,
        onItemClick: (FavoriteConversationModel?) -> Unit
    ) :
        RecyclerView.ViewHolder(itemView) {

        private var favoriteConversationModel: FavoriteConversationModel? = null

        init {
            itemView.setOnClickListener { onItemClick.invoke(favoriteConversationModel) }
        }

        fun bind(favoriteConversationModel: FavoriteConversationModel) {
            this.favoriteConversationModel = favoriteConversationModel

            updatePhoto(favoriteConversationModel)

            if (false) {
                itemView.tvUnreadMessages.text = ""
                itemView.tvUnreadMessages.visibility = View.VISIBLE
            } else {
                itemView.tvUnreadMessages.visibility = View.INVISIBLE
            }

            updateOnline(favoriteConversationModel.isOnline())
        }

        fun updateOnline(isOnline: Boolean) {
            if (isOnline) {
                itemView.ivOnline.visibility = View.VISIBLE
            } else {
                itemView.ivOnline.visibility = View.INVISIBLE
            }
        }

        fun updatePhoto(favoriteConversationModel: FavoriteConversationModel) {
            val photoLabel = favoriteConversationModel.getPhotoLabel()
            val photoUrl = favoriteConversationModel.getPhotoUrl()

            itemView.tvPhotoLabel.text = photoLabel

            if (photoUrl != null) {
                Glide.with(itemView)
                    .load(photoUrl)
                    .thumbnail(0.1F)
                    .into(itemView.ivUserAvatar)
                itemView.ivUserAvatar.visibility = View.VISIBLE
            } else {
                itemView.ivUserAvatar.visibility = View.INVISIBLE
            }
        }


    }

    class AddButtonHolder(itemView: View, onItemClick: (FavoriteConversationModel?) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener { onItemClick.invoke(null) }
        }
    }

    class FavoriteConversationsDiffCallback : DiffUtil.ItemCallback<FavoriteConversationModel>() {

        data class Payloads(
            var photoChanged: Boolean = false,
            var userOnlineChanged: Boolean = false
        ) {
            fun hasChanges(): Boolean {
                return photoChanged ||
                        userOnlineChanged
            }
        }

        override fun areItemsTheSame(
            oldItem: FavoriteConversationModel,
            newItem: FavoriteConversationModel
        ): Boolean {
            return oldItem.favoriteConversation.conversationType == newItem.favoriteConversation.conversationType
                    && oldItem.favoriteConversation.identifier == newItem.favoriteConversation.identifier
        }

        override fun areContentsTheSame(
            oldItem: FavoriteConversationModel,
            newItem: FavoriteConversationModel
        ): Boolean {
            return oldItem.online == newItem.isOnline()
        }

        override fun getChangePayload(
            oldItem: FavoriteConversationModel,
            newItem: FavoriteConversationModel
        ): Any? {

            val payloads = Payloads()

            if (oldItem.online != newItem.isOnline()) {
                payloads.userOnlineChanged = true
            }

            when {
                oldItem.getUser()?.photo != newItem.getUser()?.photo -> {
                    payloads.photoChanged = true
                }
                oldItem.getChat()?.photo != newItem.getChat()?.photo -> {
                    payloads.photoChanged = true
                }
                oldItem.getChannel()?.photo != newItem.getChannel()?.photo -> {
                    payloads.photoChanged = true
                }
            }

            return if (payloads.hasChanges()) {
                payloads
            } else {
                null
            }

        }
    }
}