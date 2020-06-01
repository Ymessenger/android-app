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
import org.ymessenger.app.data.local.db.entities.UserAction
import org.ymessenger.app.data.local.db.models.UserActionModel
import org.ymessenger.app.data.remote.Config

@Dao
interface UserActionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: UserAction)

    @Query(
        """SELECT * FROM user_action 
        WHERE conversation_id = :conversationId 
        AND conversation_type = :conversationType 
        AND :currentTime - time <= ${Config.USER_ACTION_CHECK_PERIOD} 
        ORDER BY time DESC"""
    )
    fun getLastUserActions(
        conversationId: Long,
        conversationType: Int,
        currentTime: Long
    ): LiveData<List<UserAction>>

    @Query(
        """SELECT * FROM user_action 
        WHERE conversation_id = :conversationId 
        AND conversation_type = :conversationType 
        AND :currentTime - time <= ${Config.USER_ACTION_CHECK_PERIOD} 
        ORDER BY time DESC"""
    )
    fun getLastUserActionModels(
        conversationId: Long,
        conversationType: Int,
        currentTime: Long
    ): LiveData<List<UserActionModel>>


    @Query(
        """SELECT * FROM user_action 
        WHERE :currentTime - time <= ${Config.USER_ACTION_CHECK_PERIOD} 
        ORDER BY time DESC"""
    )
    fun getLastUserActionModels(
        currentTime: Long
    ): LiveData<List<UserActionModel>>

}