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
import org.ymessenger.app.data.local.db.entities.Dialog

@Dao
interface DialogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dialogs: List<Dialog>)

    @Query("SELECT * FROM dialogs")
    fun getDialogs(): LiveData<List<Dialog>>

    @Query("SELECT * FROM dialogs WHERE id = :dialogId")
    fun getDialog(dialogId: Long): LiveData<Dialog>

    @Query("SELECT * FROM dialogs WHERE user_id = :userId")
    fun getDialogByUser(userId: Long): LiveData<Dialog>

    @Insert
    fun addDialog(dialog: Dialog): Long

    @Update
    fun updateDialog(dialog: Dialog)

    @Delete
    fun removeDialog(dialog: Dialog)

}