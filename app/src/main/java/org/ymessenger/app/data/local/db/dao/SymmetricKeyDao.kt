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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.ymessenger.app.data.local.db.entities.SymmetricKey

@Dao
interface SymmetricKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(symmetricKey: SymmetricKey)

    @Query("SELECT * FROM symmetric_keys WHERE dialog_id = :dialogId ORDER BY generation_time DESC LIMIT 1")
    fun getSymmetricKeyByDialog(dialogId: Long): LiveData<SymmetricKey>

    @Query("SELECT * FROM symmetric_keys WHERE id = :keyId")
    fun getSymmetricKey(keyId: Long): LiveData<SymmetricKey>

    @Query("SELECT * FROM symmetric_keys WHERE id = :keyId")
    fun getSymmetricKeySync(keyId: Long): SymmetricKey?

    @Query("DELETE FROM symmetric_keys")
    fun deleteAllKeys()

}