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
import androidx.paging.DataSource
import androidx.room.*
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.data.local.db.models.UserModel

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(users: List<User>): List<Long>

    @Query("SELECT * FROM users")
    fun getUsers(): DataSource.Factory<Int, User>

    @Query("SELECT * FROM users WHERE id IN (:usersId)")
    fun getUsers(usersId: List<Long>): LiveData<List<User>>

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUser(userId: Long): LiveData<User>

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserModel(userId: Long): LiveData<UserModel>

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserSync(userId: Long): User?

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserModelSync(userId: Long): UserModel?

    @Update
    fun updateUser(user: User)

    @Query("DELETE FROM users")
    fun deleteAllUsers()

    @Query("UPDATE users SET privacy = :privacy WHERE id = :userId")
    fun updatePrivacy(userId: Long, privacy: String?)
}