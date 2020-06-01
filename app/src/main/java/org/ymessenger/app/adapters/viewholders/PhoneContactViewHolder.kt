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
import kotlinx.android.synthetic.main.item_phone_contact.view.*
import org.ymessenger.app.R
import org.ymessenger.app.interfaces.SimpleItemClickListener
import org.ymessenger.app.models.PhoneContact

class PhoneContactViewHolder(
    itemView: View,
    itemClickListener: SimpleItemClickListener<PhoneContact>
) :
    RecyclerView.ViewHolder(itemView) {

    private var item: PhoneContact? = null

    init {
        itemView.setOnClickListener {
            item?.let {
                itemClickListener.onClick(it)
            }
        }
    }

    fun bind(phoneContact: PhoneContact) {
        item = phoneContact
        itemView.tvName.text = phoneContact.name
        itemView.tvPhone.text = phoneContact.number
    }

    companion object {
        fun create(
            parent: ViewGroup,
            itemClickListener: SimpleItemClickListener<PhoneContact>
        ): PhoneContactViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_phone_contact, parent, false)
            return PhoneContactViewHolder(view, itemClickListener)
        }
    }

}