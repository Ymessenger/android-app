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
import org.ymessenger.app.data.local.db.entities.ChannelUser
import org.ymessenger.app.data.local.db.models.ChannelUserModel

@Dao
interface ChannelUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: ChannelUser)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(items: List<ChannelUser>)

    @Query("SELECT * FROM channel_users")
    fun getChannelUsers(): LiveData<List<ChannelUser>>

    @Transaction
    @Query("SELECT * FROM channel_users WHERE channel_id = :channelId ORDER BY user_id")
    fun getChannelUserModels(channelId: Long): DataSource.Factory<Int, ChannelUserModel>

    @Query("SELECT * FROM channel_users WHERE channel_id = :channelId")
    fun getChannelUsersByChannel(channelId: Long): LiveData<List<ChannelUser>>

    @Query("SELECT * FROM channel_users WHERE channel_id = :channelId AND user_id = :userId")
    fun getChannelUser(channelId: Long, userId: Long): LiveData<ChannelUser>

    @Update
    fun update(item: ChannelUser)

    @Delete
    fun delete(item: ChannelUser)

    @Delete
    fun delete(items: List<ChannelUser>)

    @Update
    fun update(item: List<ChannelUser>)

    @Query("DELETE FROM channel_users WHERE channel_id = :channelId")
    fun deleteAllChannelUsers(channelId: Long)
}