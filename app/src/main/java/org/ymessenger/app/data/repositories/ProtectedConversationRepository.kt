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

import org.ymessenger.app.data.local.db.dao.ProtectedConversationDao
import org.ymessenger.app.data.local.db.entities.ProtectedConversation
import org.ymessenger.app.utils.AppExecutors

class ProtectedConversationRepository private constructor(
    private val executors: AppExecutors,
    private val protectedConversationDao: ProtectedConversationDao
) {

    fun getProtectedConversation(conversationId: Long, conversationType: Int) =
        protectedConversationDao.getProtectedConversation(conversationId, conversationType)

    fun delete(protectedConversation: ProtectedConversation) {
        executors.diskIO.execute {
            protectedConversationDao.delete(protectedConversation)
        }
    }

    fun insert(protectedConversation: ProtectedConversation) {
        executors.diskIO.execute {
            protectedConversationDao.insert(protectedConversation)
        }
    }

    companion object {
        private var instance: ProtectedConversationRepository? = null

        fun getInstance(
            executors: AppExecutors,
            protectedConversationDao: ProtectedConversationDao
        ): ProtectedConversationRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: ProtectedConversationRepository(
                        executors,
                        protectedConversationDao
                    ).also { instance = it }
            }
        }
    }

}