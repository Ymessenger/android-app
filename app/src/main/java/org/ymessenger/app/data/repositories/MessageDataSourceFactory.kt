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

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import org.ymessenger.app.data.local.db.models.MessageModel
import org.ymessenger.app.helpers.EncryptionWrapper
import y.encrypt.DecrypteMsg

class MessageDataSourceFactory(
    private val conversationType: Int,
    private val conversationId: Long,
    private val messageRepository: MessageRepository,
    private val keysRepository: KeysRepository,
    private val symmetricKeyRepository: SymmetricKeyRepository,
    private val attachmentRepository: AttachmentRepository,
    private val encryptionWrapper: EncryptionWrapper,
    private val fastSymmetricKey: ByteArray
) : DataSource.Factory<Int, MessageModel>() {

    val messagesLiveData = MutableLiveData<MessagePositionalDataSource>()

    private val decryptedDataHashMapCache = hashMapOf<String, DecrypteMsg>()

    override fun create(): DataSource<Int, MessageModel> {
        val dataSource = MessagePositionalDataSource(
            conversationType,
            conversationId,
            messageRepository,
            keysRepository,
            symmetricKeyRepository,
            attachmentRepository,
            encryptionWrapper,
            decryptedDataHashMapCache,
            fastSymmetricKey
        )
        messagesLiveData.postValue(dataSource)
        return dataSource
    }
}