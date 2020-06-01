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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.entities.ContactGroup
import org.ymessenger.app.fragments.ContactsPageFragment

class ContactsPagerAdapter(
    private val context: Context,
    fragmentManager: FragmentManager
) : FragmentPagerAdapter(fragmentManager) {

    private var contactGroupList: List<ContactGroup> = listOf()

    private val allContactsFragment = ContactsPageFragment.newInstance(null)
    private var contactsPageFragments = arrayListOf<ContactsPageFragment>()

    fun setData(contactGroupList: List<ContactGroup>) {
        this.contactGroupList = contactGroupList

        contactsPageFragments.clear()
        for (contactGroup in contactGroupList) {
            contactsPageFragments.add(
                ContactsPageFragment.newInstance(contactGroup.id)
            )
        }

        notifyDataSetChanged()
    }

    override fun getItem(position: Int): Fragment {
        return if (position == 0) allContactsFragment else contactsPageFragments[position - 1]
    }

    override fun getCount(): Int =
        contactsPageFragments.size + 1 //because of first 'All contacts' fragment

    override fun getPageTitle(position: Int): CharSequence? {
        return if (position == 0) context.getString(R.string.all) else contactGroupList[position - 1].name
    }
}