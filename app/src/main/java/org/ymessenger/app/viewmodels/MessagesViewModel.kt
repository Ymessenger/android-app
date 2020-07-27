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

package org.ymessenger.app.viewmodels

import android.util.Log
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedList
import org.ymessenger.app.data.local.db.entities.Message
import org.ymessenger.app.data.local.db.models.MessageModel
import org.ymessenger.app.data.repositories.*

class MessagesViewModel(
    private val conversationId: Long,
    private val conversationType: Int,
    private val messageDataSourceFactory: MessageDataSourceFactory,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val channelRepository: ChannelRepository,
    private val repliedMessageRepository: RepliedMessageRepository
) : ViewModel() {

    val messageModels =
        Transformations.map(
            messageRepository.getMessagesByConversationPaged(
                conversationType,
                conversationId,
                messageDataSourceFactory
            )
        ) {
            checkForNullFields(it)
            it
        }

    val lastMessage = messageRepository.getLastMessage(conversationId, conversationType)

    init {
        var lastMessageObserved = false

        lastMessage.observeForever { lastMessageModel ->
            if (lastMessageObserved) return@observeForever

            // Get last loaded message id with callback (not LiveData)
            //  if it doesn't match to lastMessage.globalId then call messageRepository.loadAllMessagesFromServerAfter(lastLoadedMessageId)
            messageRepository.getLastLoadedMessageId(conversationId, conversationType) { globalId ->
                if (globalId != null && lastMessageModel.getMessage().globalId != globalId) {
                    Log.w(TAG, "Last message and last loaded message don't match. Loading all messages from last loaded message...")
                    messageRepository.loadAllMessagesFromServerAfter(conversationId, conversationType, globalId)
                } else {
                    Log.d(TAG, "Last loaded message is up to date")
                }
            }

            lastMessageObserved = true
        }
    }

    private fun checkForNullFields(messageModels: PagedList<MessageModel>) {
        val usersId = hashSetOf<Long>()
        val channelsId = hashSetOf<Long>()
        val repliedMessagesId = hashSetOf<String>()
        val conversationId = messageModels.firstOrNull()?.getMessage()?.conversationId
        val conversationType = messageModels.firstOrNull()?.getMessage()?.conversationType

        messageModels.forEach { messageModel ->
            if (messageModel.getAuthor() == null) {
                messageModel.getMessage().senderId?.let {
                    usersId.add(it)
                }
            }

            if (messageModel.isForwarded()) {
                messageModel.getForwardedMessageInfo()?.let { forwardedMessageInfoModel ->
                    if (forwardedMessageInfoModel.isChannelMessage()) {
                        if (forwardedMessageInfoModel.getChannel() == null) {
                            channelsId.add(forwardedMessageInfoModel.forwardedMessageInfo.forwardedFromConversationId)
                        }
                    } else {
                        if (forwardedMessageInfoModel.getAuthor() == null) {
                            forwardedMessageInfoModel.forwardedMessageInfo.forwardedFromUser?.let {
                                usersId.add(it)
                            }
                        }
                    }
                }
            }

            if (messageModel.getMessage().replyTo != null && messageModel.getReplyToMessage() == null) {
                repliedMessagesId.add(messageModel.getMessage().replyTo!!)
            }
        }
        if (usersId.isNotEmpty()) {
            userRepository.getUsers(usersId.toList())
        }

        if (channelsId.isNotEmpty()) {
            channelRepository.getChannelsFromServer(channelsId.toList())
        }

        if (repliedMessagesId.isNotEmpty()) {
            repliedMessageRepository.getRepliedMessagesFromServer(
                conversationId!!,
                conversationType!!,
                repliedMessagesId.toList()
            )
        }
    }

    fun readMessage(message: Message) {
        messageRepository.readMessage(message)
    }

    fun updateMessage(messageId: String) {
        messageRepository.getMessagesFromServer(conversationId, conversationType, listOf(messageId))
    }

    fun invalidateMessages() {
        Log.d(TAG, "invalidate")
        messageDataSourceFactory.messagesLiveData.value?.invalidate()
    }

    fun saveLastLoadedMessageId() {
        lastMessage.value?.getMessage()?.globalId?.let {
            messageRepository.saveLastLoadedMessageId(conversationId, conversationType, it)
        }
    }

    companion object {
        private const val TAG = "MessagesViewModel"
    }

    class Factory(
        private val conversationId: Long,
        private val conversationType: Int,
        private val messageDataSourceFactory: MessageDataSourceFactory,
        private val messageRepository: MessageRepository,
        private val userRepository: UserRepository,
        private val channelRepository: ChannelRepository,
        private val repliedMessageRepository: RepliedMessageRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MessagesViewModel(
                conversationId,
                conversationType,
                messageDataSourceFactory,
                messageRepository,
                userRepository,
                channelRepository,
                repliedMessageRepository
            ) as T
        }
    }
}