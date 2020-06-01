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
import org.ymessenger.app.data.local.db.entities.RepliedMessage
import org.ymessenger.app.data.local.db.models.MessageModel

@Dao
interface RepliedMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(messages: List<RepliedMessage>)

    @Insert
    fun insert(message: RepliedMessage)

    @Transaction
    @Query(
        """SELECT * FROM replied_messages
                    WHERE conversation_id = :conversationId
                    AND conversation_type = :conversationType
                    AND global_id = :globalId"""
    )
    fun getRepliedMessageModel(
        globalId: String,
        conversationId: Long,
        conversationType: Int
    ): LiveData<MessageModel>

    @Update
    fun update(message: RepliedMessage)

    @Query("DELETE FROM replied_messages")
    fun deleteAll()

}