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
import org.ymessenger.app.data.local.db.entities.Channel

@Dao
interface ChannelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(channel: Channel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(channel: List<Channel>)

    @Query("SELECT * FROM channels")
    fun getChannels(): LiveData<List<Channel>>

    @Query("SELECT * FROM channels WHERE id = :channelId")
    fun getChannel(channelId: Long): LiveData<Channel>

    @Query("SELECT * FROM channels WHERE id = :channelId")
    fun getChannelSync(channelId: Long): Channel?

    @Update
    fun update(channel: Channel)

    @Delete
    fun delete(channel: Channel)

    @Query("DELETE FROM channels WHERE id = :channelId")
    fun deleteChannel(channelId: Long)

    @Query("UPDATE channels SET deleted = 1 WHERE id IN (:channelsId)")
    fun setDeleted(channelsId: List<Long>)

    @Query("DELETE FROM channels")
    fun deleteAll()
}