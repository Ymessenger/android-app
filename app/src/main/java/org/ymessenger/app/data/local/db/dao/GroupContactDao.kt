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

package org.ymessenger.app.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.ymessenger.app.data.local.db.entities.GroupContact
import org.ymessenger.app.data.local.db.models.ContactModel

@Dao
interface GroupContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGroupContact(groupContact: GroupContact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGroupContacts(groupContacts: List<GroupContact>)

    // TODO: make GroupContactModel
    @Transaction
    @Query("SELECT contacts.* FROM contacts, group_contacts WHERE group_id = :contactGroupId AND contacts.id = group_contacts.contact_id ORDER BY user_id")
    fun getContactsByContactGroup(contactGroupId: String): LiveData<List<ContactModel>>

    @Transaction
    @Query("SELECT contacts.* FROM contacts, group_contacts WHERE group_id = :contactGroupId AND contacts.id = group_contacts.contact_id ORDER BY user_id")
    fun getContactsByContactGroupSync(contactGroupId: String): List<ContactModel>

    @Transaction
    @Query("SELECT contacts.* FROM contacts, group_contacts WHERE group_id = :groupId AND contacts.id = group_contacts.contact_id AND name LIKE '%' || :name || '%' ORDER BY user_id")
    fun getContactsByGroupAndName(groupId: String, name: String): LiveData<List<ContactModel>>

    @Delete
    fun removeContactGroupUser(groupContact: GroupContact)

    @Query("DELETE FROM group_contacts WHERE group_id = :contactGroupId")
    fun removeAllUsersFromContactGroup(contactGroupId: String)

    @Query("DELETE FROM group_contacts WHERE group_id = :groupId AND contact_id IN (:contactsId)")
    fun removeGroupContacts(groupId: String, contactsId: List<String>)

}