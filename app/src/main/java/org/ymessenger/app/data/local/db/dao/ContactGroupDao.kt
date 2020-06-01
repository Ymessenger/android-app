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
import androidx.room.Dao
import androidx.room.Query
import org.ymessenger.app.data.local.db.entities.ContactGroup

@Dao
abstract class ContactGroupDao : BaseDao<ContactGroup> {

    @Query("SELECT * FROM contact_groups ORDER BY sort")
    abstract fun getContactGroups(): LiveData<List<ContactGroup>>

    @Query("SELECT * FROM contact_groups ORDER BY sort")
    abstract fun getContactGroupsSync(): List<ContactGroup>

    @Query("SELECT * FROM contact_groups WHERE id = :idContactGroup")
    abstract fun getContactGroup(idContactGroup: String): LiveData<ContactGroup>

    @Query("SELECT * FROM contact_groups WHERE id = :idContactGroup")
    abstract fun getContactGroupSync(idContactGroup: String): ContactGroup?

    @Query("DELETE FROM contact_groups")
    abstract fun deleteAll()

    @Query("DELETE FROM contact_groups WHERE id IN (:groupsId)")
    abstract fun deleteContactGroups(groupsId: List<String>)

}