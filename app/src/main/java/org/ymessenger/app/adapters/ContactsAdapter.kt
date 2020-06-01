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
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.ymessenger.app.data.local.db.models.ContactModel
import org.ymessenger.app.databinding.ItemUserSimpleListBinding

class ContactsAdapter(private val onClick: ((ContactModel) -> Unit)) :
    ListAdapter<ContactModel, ContactsAdapter.ContactHolder>(ContactDiffCallback()) {

    private var type = TYPE_SIMPLE
    private var selectedContactsId = listOf<String>()

    companion object {
        const val TYPE_SIMPLE = 0
        const val TYPE_SELECTION = 1
    }

    fun setSelectedContactsId(selectedContacts: List<String>) {
        this.selectedContactsId = selectedContacts
        notifyDataSetChanged()
    }

    fun setType(type: Int) {
        this.type = type

        when (type) {
            TYPE_SIMPLE -> {
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ContactHolder {
        return ContactHolder.create(parent, onClick, this.type)
    }

    override fun onBindViewHolder(holder: ContactHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, isSelected(item))
    }

    // Should replace selectedContacts with selectedContactsId?
    private fun isSelected(item: ContactModel): Boolean {
        var selected = false
        for (contactId in selectedContactsId) {
            if (contactId == item.contact.id) {
                selected = true
                break
            }
        }

        return selected
    }

    class ContactHolder(
        private val binding: ItemUserSimpleListBinding,
        private val onClick: ((ContactModel) -> Unit),
        private val type: Int
    ) : RecyclerView.ViewHolder(binding.root) {

        private var item: ContactModel? = null

        init {
            itemView.setOnClickListener {
                item?.let {
                    when (type) {
                        TYPE_SIMPLE -> onClick.invoke(it)
                        TYPE_SELECTION -> onClick.invoke(it) // is this bad ?
                    }
                }
            }
        }

        fun bind(contactModel: ContactModel, selected: Boolean) {
            item = contactModel
            binding.displayName = contactModel.getDisplayName()
            binding.photoUrl = contactModel.getUser()?.getPhotoUrl()
            binding.photoLabel = contactModel.getPhotoLabel()
            binding.executePendingBindings()

            if (type == TYPE_SELECTION) {
                binding.isSelected = selected
            }
        }

        companion object {
            fun create(
                parent: ViewGroup,
                onClick: ((ContactModel) -> Unit),
                type: Int
            ): ContactHolder {
                val binding = ItemUserSimpleListBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ContactHolder(binding, onClick, type)
            }
        }
    }

    class ContactDiffCallback : DiffUtil.ItemCallback<ContactModel>() {
        override fun areItemsTheSame(oldItem: ContactModel, newItem: ContactModel): Boolean {
            return oldItem.contact.id == newItem.contact.id
        }

        override fun areContentsTheSame(oldItem: ContactModel, newItem: ContactModel): Boolean {
            return oldItem.getUser()?.firstName == newItem.getUser()?.firstName &&
                    oldItem.getUser()?.lastName == newItem.getUser()?.lastName &&
                    oldItem.getUser()?.photo == newItem.getUser()?.photo
        }

    }
}