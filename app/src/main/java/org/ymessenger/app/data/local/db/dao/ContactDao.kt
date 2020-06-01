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
import org.ymessenger.app.data.local.db.entities.Contact
import org.ymessenger.app.data.local.db.models.ContactModel

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts")
    fun getContacts(): LiveData<List<Contact>>

    @Query("SELECT * FROM contacts")
    fun getContactsSync(): List<Contact>

    @Query("SELECT * FROM contacts WHERE user_id IN (:usersId)")
    fun getContactsByUsersId(usersId: List<Long>): List<Contact>

    @Query("SELECT * FROM contacts WHERE user_id = :userId")
    fun getContactByUserId(userId: Long): Contact?

    @Transaction
    @Query("SELECT * FROM contacts ORDER BY user_id")
    fun getContactModels(): LiveData<List<ContactModel>>

    @Transaction
    @Query("SELECT * FROM contacts WHERE id IN (:contactsId)")
    fun getContactModelsById(contactsId: List<String>): LiveData<List<ContactModel>>

    @Transaction
    @Query("SELECT * FROM contacts WHERE id = :contactId")
    fun getContactModelById(contactId: String): LiveData<ContactModel>

    @Transaction
    @Query("SELECT contacts.* FROM users, contacts WHERE users.id = contacts.user_id AND users.first_name || ' ' || users.last_name LIKE '%' || :name || '%'")
    fun getContactModelsByName(name: String): LiveData<List<ContactModel>>

    @Query("SELECT * FROM contacts WHERE id = :contactId")
    fun getContact(contactId: Long): LiveData<Contact>

    @Query("SELECT * FROM contacts WHERE user_id = :userId")
    fun getContactByUser(userId: Long): LiveData<Contact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addContact(contact: Contact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllContact(contacts: List<Contact>)

    @Delete
    fun removeContact(contact: Contact)

    @Update
    fun updateContact(contact: Contact)

    @Query("DELETE FROM contacts")
    fun deleteAllContacts()

    @Query("DELETE FROM contacts WHERE id IN (:contactsId)")
    fun deleteContacts(contactsId: List<String>)

}