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

import androidx.lifecycle.LiveData
import org.ymessenger.app.data.local.db.dao.AttachmentDao
import org.ymessenger.app.data.local.db.entities.Attachment
import org.ymessenger.app.utils.AppExecutors

class AttachmentRepository private constructor(
    private val executors: AppExecutors,
    private val attachmentDao: AttachmentDao
) {

    fun getAttachmentsByMessage(messageId: Long) = attachmentDao.getAttachmentsByMessage(messageId)

    fun getAttachment(attachmentId: Long) = attachmentDao.getAttachment(attachmentId)

    fun insert(attachment: Attachment) {
        executors.diskIO.execute {
            attachmentDao.insert(attachment)
        }
    }

    fun update(attachment: Attachment) {
        executors.diskIO.execute {
            attachmentDao.update(attachment)
        }
    }

    fun update(attachments: List<Attachment>) {
        executors.diskIO.execute {
            attachmentDao.update(attachments)
        }
    }

    fun updateSavedAt(attachmentId: Long, savedAt: String?) {
        executors.diskIO.execute {
            attachmentDao.updateSavedAt(attachmentId, savedAt)
        }
    }

    fun delete(attachment: Attachment) {
        executors.diskIO.execute {
            attachmentDao.delete(attachment)
        }
    }

    fun getPhotosByConversation(
        conversationId: Long,
        conversationType: Int
    ): LiveData<List<Attachment>> {
        return attachmentDao.getPhotosByConversation(conversationId, conversationType)
    }

    companion object {
        private var instance: AttachmentRepository? = null

        fun getInstance(executors: AppExecutors, attachmentDao: AttachmentDao) =
            instance ?: synchronized(this) {
                instance
                    ?: AttachmentRepository(
                        executors,
                        attachmentDao
                    ).also { instance = it }
            }
    }

}