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

import android.os.AsyncTask
import android.util.Log
import androidx.paging.PositionalDataSource
import com.google.gson.Gson
import org.ymessenger.app.data.local.db.entities.Attachment
import org.ymessenger.app.data.local.db.models.MessageModel
import org.ymessenger.app.data.remote.responses.ResultResponse
import org.ymessenger.app.helpers.EncryptHelper
import org.ymessenger.app.helpers.EncryptionWrapper
import org.ymessenger.app.interfaces.SuccessErrorCallback
import org.ymessenger.app.utils.DateUtils
import y.encrypt.DecrypteMsg
import y.encrypt.YEncrypt

class MessagePositionalDataSource(
    private val conversationType: Int,
    private val conversationId: Long,
    private val messageRepository: MessageRepository,
    private val keysRepository: KeysRepository,
    private val symmetricKeyRepository: SymmetricKeyRepository,
    private val attachmentRepository: AttachmentRepository,
    private val encryptionWrapper: EncryptionWrapper,
    private val decryptedDataHashMapCache: HashMap<String, DecrypteMsg>,
    private val fastSymmetricKey: ByteArray
) : PositionalDataSource<MessageModel>() {

    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<MessageModel>
    ) {
//        Log.d(TAG, "loadInitial. params.requestedStartPosition ${params.requestedStartPosition}, params.requestedLoadSize ${params.requestedLoadSize}")

        val newStartPos = if (params.placeholdersEnabled) {
            params.requestedStartPosition
        } else {
//            max(0, params.requestedStartPosition - (params.requestedLoadSize / 2)) // it worked when there was bug in paging library
            params.requestedStartPosition
        }

//        Log.d(TAG, "loadInitial. newStartPos $newStartPos, params.requestedLoadSize ${params.requestedLoadSize}")

        val items = messageRepository.getMessagesByConversation(
            conversationId,
            conversationType,
            newStartPos,
            params.requestedLoadSize
        )
//        Log.d(TAG, "${items.size} loaded")

        if (newStartPos != 0 && items.isEmpty()) {
            // FIXME: We've tried to get items from non-null start position, but there is no data. Skip callback
        } else {
            checkMessages(items)
            callback.onResult(items, newStartPos)
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<MessageModel>) {
//        Log.d(TAG, "loadRange. params.startPosition ${params.startPosition}, params.loadSize ${params.loadSize}")

        val items = messageRepository.getMessagesByConversation(
            conversationId,
            conversationType,
            params.startPosition,
            params.loadSize
        )
//        Log.d(TAG, "${items.size} loaded")
        checkMessages(items)

        callback.onResult(items)

    }

    private fun checkMessages(messages: List<MessageModel>) {
        // 1. Iterate through all messages to find encrypted
        // 2. Decrypt messages which are in cache
        // 3. Send other messages to decrypt later

        val messagesToDecryptLater = arrayListOf<MessageModel>()

        var encryptedMessagesCount = 0
        var cachedDecryptedMessagesCount = 0
        val startDecryption = System.currentTimeMillis()

        for (message in messages) {
            if (message.hasAttachments() && message.getAttachment().isEncryptedMessage()) {
                encryptedMessagesCount++
                val messageId = message.getMessage().globalId

                if (decryptedDataHashMapCache.containsKey(messageId)) {
                    cachedDecryptedMessagesCount++
                    message.setDecryptedMessage(decryptedDataHashMapCache[messageId]!!)
                } else {
                    messagesToDecryptLater.add(message)
                }
            }
        }

        val decryptionTime = System.currentTimeMillis() - startDecryption
        if (encryptedMessagesCount > 0) {
            Log.d(
                TAG, "Check messages. Encrypted messages - $encryptedMessagesCount, " +
                        "Cached of them - $cachedDecryptedMessagesCount, " +
                        if (cachedDecryptedMessagesCount > 0) {
                            "Decrypted for $decryptionTime ms"
                        } else {
                            "Decryption is needed"
                        }
            )
        }

        if (messagesToDecryptLater.isNotEmpty()) {
            DecryptMessagesAsyncTask().execute(messagesToDecryptLater)
        }
    }

    private fun decryptFastEncryptedMessagesOnly(encryptedMessages: List<MessageModel>) {
        Log.d(TAG, "Start fast decrypting messages")
        val startDecryptionTime = System.currentTimeMillis()
        val encryptedMessagesCount = encryptedMessages.size
        var decryptedEncryptedMessagesCount = 0
        var fastDecryption = 0

        val messagesToLongDecryption = mutableListOf<MessageModel>()

        val gson = Gson()

        for (message in encryptedMessages) {
            val messageId = message.getMessage().globalId
            val time = DateUtils.dateTimeFormat(message.getMessage().sentAt)

            val encryptedMessage = message.getAttachment().getPayloadAsEncryptedMessage()

            // Check if it's easy encrypted message
            if (encryptedMessage.fastEncrypted) {
                // It's an easy encrypted message
                fastDecryption++
                try {
                    val decryptedMessageBytes = YEncrypt.DecryptData(
                        1L,
                        EncryptHelper.base64ToBytes(encryptedMessage.encryptedData),
                        fastSymmetricKey
                    )

                    val decryptedMessageJson = String(decryptedMessageBytes)
                    val decryptedMessage =
                        gson.fromJson(decryptedMessageJson, DecrypteMsg::class.java)

                    // This should be enough

                    // Save to cache
                    decryptedDataHashMapCache[messageId] = decryptedMessage

                    decryptedEncryptedMessagesCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fast decrypt message $messageId, time $time")
                    e.printStackTrace()
                    continue
                }
            } else {
                messagesToLongDecryption.add(message)
            }
        }

        val allDecryptionTime = System.currentTimeMillis() - startDecryptionTime

        Log.d(
            TAG,
            "Finish fast decrypting messages. Encrypted messages - $encryptedMessagesCount, " +
                    "Decrypted of them - $decryptedEncryptedMessagesCount, " +
                    "Fast decrypted of them - $fastDecryption, " +
                    "Decryption time - $allDecryptionTime ms" +
                    if (messagesToLongDecryption.isNotEmpty()) {
                        ", Messages to long decryption - ${messagesToLongDecryption.size}"
                    } else {
                        ""
                    }
        )

        if (messagesToLongDecryption.isNotEmpty()) {
            LongDecryptMessagesAsyncTask().execute(messagesToLongDecryption)
        }

        if (decryptedEncryptedMessagesCount > 0) {
            Log.d(TAG, "Invalidate after fast decryption")
            invalidate()
        }
    }

    private fun decryptMessagesLong(encryptedMessages: List<MessageModel>) {
        val yEncrypt = try {
            encryptionWrapper.getYEncrypt()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        val signKeyIds = hashSetOf<Long>()
        var keysOwnerId: Long = 0

        val attachmentsToUpdate = mutableListOf<Attachment>()

        Log.d(TAG, "Start long decrypting messages")
        val startDecryptionTime = System.currentTimeMillis()
        val encryptedMessagesCount = encryptedMessages.size
        var decryptedEncryptedMessagesCount = 0
        var alreadyCached = 0

        val gson = Gson()

        for (message in encryptedMessages) {
            val messageId = message.getMessage().globalId
            val time = DateUtils.dateTimeFormat(message.getMessage().sentAt)

            // Check if there are cache of this message
            if (decryptedDataHashMapCache.containsKey(messageId)) {
                alreadyCached++
                continue
            }

            val encryptedMessage = message.getAttachment().getPayloadAsEncryptedMessage()

            // Check if it's not easy encrypted message
            if (!encryptedMessage.fastEncrypted) {
                // It's an usual encrypted message
                val symmetricKey =
                    symmetricKeyRepository.getSymmetricKeySync(encryptedMessage.keyId)
                if (symmetricKey == null) {
                    Log.w(
                        TAG,
                        "There is no symmetric key to decrypt message $messageId, time $time"
                    )
                    continue
                }

                val signKey = keysRepository.getKeysSync(encryptedMessage.signKeyId)
                if (signKey == null) {
                    Log.w(TAG, "There is no sign key to decrypt message $messageId, time $time")
                    keysOwnerId = message.getMessage().senderId!!
                    signKeyIds.add(encryptedMessage.signKeyId)
                    continue
                }


                yEncrypt.setSymmetricEncryptKey(symmetricKey.data)
                yEncrypt.publicSignKeyToReceive = signKey.publicKey

                try {
                    val decryptedMessage =
                        yEncrypt.decryptSecretMsg(EncryptHelper.base64ToBytes(encryptedMessage.encryptedData))

                    // Save to cache
                    decryptedDataHashMapCache[messageId] = decryptedMessage

                    decryptedEncryptedMessagesCount++

                    // Encrypt with easy key
                    // 1. Get bytes from decryptedMessage
                    // 2. Encrypt it on symmetric key
                    // 3. Put to encryptedMessage.encryptedData (create new one)
                    // 4. Set keyId and signKeyId to 0
                    // 5. Convert encryptedMessage to json and put to attachment (create new)
                    // 6. Save this attachment to database

                    val decryptedMessageJson = gson.toJson(decryptedMessage)
                    val decryptedMessageBytes = decryptedMessageJson.toByteArray()
                    val encryptedDecryptedMessage =
                        YEncrypt.EncryptData(1L, decryptedMessageBytes, fastSymmetricKey)
                    val newEncryptedMessage = encryptedMessage.copy(
                        encryptedData = EncryptHelper.bytesToBase64(encryptedDecryptedMessage),
                        fastEncrypted = true
                    )
                    val newEncryptedMessageJson = gson.toJson(newEncryptedMessage)
                    val newAttachment = message.getAttachment().copy(
                        payload = newEncryptedMessageJson
                    )
                    newAttachment.id = message.getAttachment().id

                    attachmentsToUpdate.add(newAttachment)

                    // Decrypt 5 messages at a time to show them faster
//                    if (decryptedEncryptedMessagesCount >= MAX_DECRYPTED_MESSAGES_AT_TIME) {
//                        break
//                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to decrypt message $messageId, time $time")
                    e.printStackTrace()
                    continue
                }
            }
        }

        val allDecryptionTime = System.currentTimeMillis() - startDecryptionTime

        Log.d(
            TAG,
            "Finish long decrypting messages. Encrypted messages - $encryptedMessagesCount, " +
                    "Decrypted of them - $decryptedEncryptedMessagesCount, " +
                    "Already cached of them - $alreadyCached, " +
                    "Decryption time - $allDecryptionTime ms"
        )

        if (attachmentsToUpdate.isNotEmpty()) {
            updateAttachments(attachmentsToUpdate)
        }

        if (signKeyIds.isNotEmpty()) {
            getSignKeys(keysOwnerId, signKeyIds.toList())
        }

        // We don't actually need to call invalidate there, because it will automatically invalidated after attachments are saved
//        if (decryptedEncryptedMessagesCount > 0) {
//            Log.d(TAG, "Invalidate after long decryption")
//            invalidate()
//        }
    }

    private fun updateAttachments(attachments: List<Attachment>) {
        Log.d(TAG, "${attachments.size} messages was re-encrypted using fast key. Storing to DB...")
        attachmentRepository.update(attachments)
    }

    private fun getSignKeys(keysOwnerId: Long, signKeyIds: List<Long>) {
        Log.d(TAG, "Trying to get ${signKeyIds.size} sign keys...")
        keysRepository.loadKeysByUser(
            keysOwnerId,
            signKeyIds,
            object : SuccessErrorCallback {
                override fun success() {
                    Log.d(
                        TAG,
                        "We got sign key from server! We must invalidate messages to try to decrypt messages again"
                    )
                    invalidate()
                }

                override fun error(error: ResultResponse) {
                    Log.e(TAG, "We couldn't get public sign key")
                }
            })
    }

    inner class DecryptMessagesAsyncTask : AsyncTask<List<MessageModel>, Void, Void>() {
        override fun doInBackground(vararg params: List<MessageModel>?): Void? {
            params[0]?.let {
                decryptFastEncryptedMessagesOnly(it)
            }

            return null
        }
    }

    inner class LongDecryptMessagesAsyncTask : AsyncTask<List<MessageModel>, Void, Void>() {
        override fun doInBackground(vararg params: List<MessageModel>?): Void? {
            params[0]?.let {
                decryptMessagesLong(it)
            }

            return null
        }
    }

    companion object {
        private const val TAG = "MessagePositionalDS"

        private const val MAX_DECRYPTED_MESSAGES_AT_TIME = 3
    }
}