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
import androidx.room.Transaction
import org.ymessenger.app.data.local.db.entities.FavoriteConversation
import org.ymessenger.app.data.local.db.models.FavoriteConversationModel

@Dao
abstract class FavoriteConversationDao : BaseDao<FavoriteConversation> {

    @Transaction
    @Query("SELECT * FROM favorite_conversation ORDER BY sort")
    abstract fun getFavoriteConversationModels(): LiveData<List<FavoriteConversationModel>>

    @Query("SELECT * FROM favorite_conversation WHERE identifier = :identifier AND conversation_type = :conversationType")
    abstract fun getFavoriteConversation(
        identifier: Long,
        conversationType: Int
    ): LiveData<FavoriteConversation?>

    @Query("DELETE FROM favorite_conversation WHERE identifier = :identifier AND conversation_type = :conversationType")
    abstract fun delete(identifier: Long, conversationType: Int)

}