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

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.group_info_header.view.*
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.entities.Chat

class GroupInfoHeaderHolder(
    itemView: View,
    onPhotoClick: (url: String) -> Unit,
    onFavoriteClick: () -> Unit,
    onTagClick: (String) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val ivGroupPhoto: ImageView = itemView.findViewById(R.id.ivGroupPhoto)
    private val tvGroupName: TextView = itemView.findViewById(R.id.tvGroupName)
    private val tvAbout: TextView = itemView.findViewById(R.id.tvAbout)
    private val tvAboutLabel: TextView = itemView.findViewById(R.id.tvAboutLabel)
    private val tvTag: TextView = itemView.findViewById(R.id.tvTag)
    private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavourite)

    private var chat: Chat? = null

    init {
        ivGroupPhoto.setOnClickListener {
            chat?.let {
                it.getPhotoUrl()?.let { photoUrl ->
                    onPhotoClick(photoUrl)
                }
            }
        }

        btnFavorite.setOnClickListener {
            onFavoriteClick.invoke()
        }

        tvTag.setOnClickListener {
            val tagText = tvTag.text.toString().trim()
            if (tagText.isNotBlank()) {
                onTagClick.invoke(tagText)
            }
        }
    }

    fun bind(chat: Chat) {
        this.chat = chat

        itemView.tvPhotoLabel.text = chat.getPhotoLabel()
        val photoUrl = chat.getPhotoUrl()
        if (photoUrl != null) {
            Glide.with(itemView)
                .load(photoUrl)
                .into(ivGroupPhoto)
            ivGroupPhoto.visibility = View.VISIBLE
        } else {
            ivGroupPhoto.visibility = View.INVISIBLE
        }


        tvGroupName.text = chat.name
        if (chat.about != null) {
            tvAbout.text = chat.about
            tvAbout.visibility = View.VISIBLE
            tvAboutLabel.visibility = View.VISIBLE
        } else {
            tvAbout.visibility = View.GONE
            tvAboutLabel.visibility = View.GONE
        }

        tvTag.text = chat.tag.toString()
    }

    fun updateFavorite(isFavorite: Boolean) {
        btnFavorite.setImageResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border)
    }

}