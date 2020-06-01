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

import org.ymessenger.app.data.local.db.dao.DraftMessageDao
import org.ymessenger.app.data.local.db.entities.DraftMessage
import org.ymessenger.app.utils.AppExecutors

class DraftMessageRepository private constructor(
    private val executors: AppExecutors,
    private val draftMessageDao: DraftMessageDao
) {

    fun getDraftMessage(conversationId: Long, conversationType: Int) =
        draftMessageDao.getDraftByConversation(conversationId, conversationType)

    fun delete(draftMessage: DraftMessage) {
        executors.diskIO.execute {
            draftMessageDao.delete(draftMessage)
        }
    }

    fun upsert(draftMessage: DraftMessage) {
        executors.diskIO.execute {
            draftMessageDao.upsert(draftMessage)
        }
    }

    fun insert(draftMessage: DraftMessage) {
        executors.diskIO.execute {
            draftMessageDao.insert(draftMessage)
        }
    }

    fun update(draftMessage: DraftMessage) {
        executors.diskIO.execute {
            draftMessageDao.update(draftMessage)
        }
    }

    companion object {
        private var instance: DraftMessageRepository? = null

        fun getInstance(
            executors: AppExecutors,
            draftMessageDao: DraftMessageDao
        ): DraftMessageRepository {
            return instance ?: synchronized(this) {
                instance
                    ?: DraftMessageRepository(
                        executors,
                        draftMessageDao
                    ).also { instance = it }
            }
        }
    }

}