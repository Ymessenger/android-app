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
import org.ymessenger.app.data.local.db.entities.Keys

@Dao
interface KeysDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(keys: Keys)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(keysList: List<Keys>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReplace(keysList: List<Keys>)

    @Query("SELECT * FROM keys WHERE user_id = :userId")
    fun getKeysByUser(userId: Long): List<Keys>

    @Query("SELECT * FROM keys WHERE id = :keyId")
    fun getKeys(keyId: Long): Keys?

    @Query("SELECT * FROM keys WHERE user_id = :userId AND id = :keyId")
    fun getKeys(userId: Long, keyId: Long): Keys?

    @Query("SELECT * FROM keys WHERE user_id = :userId AND is_sign = :isSign AND private_key IS NOT NULL ORDER BY generation_time DESC LIMIT 1")
    fun getMyLastKeys(userId: Long, isSign: Boolean): Keys?

    @Query("SELECT * FROM keys WHERE private_key IS NOT NULL AND is_sign = :isSign ORDER BY generation_time DESC LIMIT 1")
    fun getMyLastKeys(isSign: Boolean): LiveData<Keys>

    @Query("DELETE FROM keys")
    fun deleteAllKeys()

    @Query("SELECT * FROM keys WHERE private_key IS NOT NULL ORDER BY generation_time DESC LIMIT 50")
    fun getFullKeys(): List<Keys>
}