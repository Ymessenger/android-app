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

package org.ymessenger.app.data.repositories

import org.ymessenger.app.data.local.db.dao.FavoriteConversationDao
import org.ymessenger.app.data.local.db.entities.FavoriteConversation
import org.ymessenger.app.utils.AppExecutors

class FavoriteConversationRepository private constructor(
    private val executors: AppExecutors,
    private val favoriteConversationDao: FavoriteConversationDao
) {

    fun getFavoriteConversations() = favoriteConversationDao.getFavoriteConversationModels()

    fun getFavoriteConversation(identifier: Long, conversationType: Int) =
        favoriteConversationDao.getFavoriteConversation(identifier, conversationType)

    fun delete(favoriteConversation: FavoriteConversation) {
        executors.diskIO.execute {
            favoriteConversationDao.delete(favoriteConversation)
        }
    }

    fun delete(identifier: Long, conversationType: Int) {
        executors.diskIO.execute {
            favoriteConversationDao.delete(identifier, conversationType)
        }
    }

    fun insert(favoriteConversation: FavoriteConversation) {
        executors.diskIO.execute {
            favoriteConversationDao.insert(favoriteConversation)
        }
    }

    fun update(favoriteConversations: List<FavoriteConversation>) {
        executors.diskIO.execute {
            favoriteConversationDao.update(favoriteConversations)
        }
    }

    companion object {
        private var instance: FavoriteConversationRepository? = null

        fun getInstance(
            executors: AppExecutors,
            favoriteConversationDao: FavoriteConversationDao
        ): FavoriteConversationRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: FavoriteConversationRepository(
                        executors,
                        favoriteConversationDao
                    ).also { instance = it }
            }
        }
    }

}