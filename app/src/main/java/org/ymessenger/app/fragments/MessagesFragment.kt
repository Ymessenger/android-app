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

package org.ymessenger.app.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import kotlinx.android.synthetic.main.fragment_messages.*
import kotlinx.android.synthetic.main.fragment_messages.view.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.MessageMarginItemDecoration
import org.ymessenger.app.adapters.MessagesPagedAdapter
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.local.db.entities.Attachment
import org.ymessenger.app.data.local.db.models.MessageModel
import org.ymessenger.app.data.remote.entities.FileInfo
import org.ymessenger.app.data.remote.entities.Poll
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.DialogViewModel
import org.ymessenger.app.viewmodels.MessagesViewModel
import java.io.File

class MessagesFragment : BaseFragment() {

    private var conversationType: Int = 0
    private var conversationId: Long = 0

    private lateinit var viewModel: MessagesViewModel
    private lateinit var messagesAdapter: MessagesPagedAdapter

    private lateinit var itemClickListeners: MessagesPagedAdapter.ItemClickListeners
    private var encryptedMessageCallbacks: MessagesPagedAdapter.EncryptedMessageCallbacks? = null

    private var lastTimeCheckEncryptedMessages = 0L

    companion object {
        private const val TAG = "MessagesFragment"

        private const val ARG_CONVERSATION_ID = "conversation_id"
        private const val ARG_CONVERSATION_TYPE = "conversation_type"

        fun get(conversationId: Long, conversationType: Int): MessagesFragment {
            val fragment = MessagesFragment()
            val bundle = Bundle().apply {
                putLong(ARG_CONVERSATION_ID, conversationId)
                putInt(ARG_CONVERSATION_TYPE, conversationType)
            }
            fragment.arguments = bundle

            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        itemClickListeners = context as? MessagesPagedAdapter.ItemClickListeners
            ?: throw ClassCastException("$context must implement MessagesPagedAdapter.ItemClickListeners")

        encryptedMessageCallbacks = context as? MessagesPagedAdapter.EncryptedMessageCallbacks
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments == null) {
            Log.e(TAG, "Null arguments")
            return
        }

        conversationType = arguments!!.getInt(ARG_CONVERSATION_TYPE)
        conversationId = arguments!!.getLong(ARG_CONVERSATION_ID)

        val factory =
            Injection.provideMessagesViewModelFactory(appBase, conversationId, conversationType)
        viewModel = ViewModelProviders.of(this, factory).get(MessagesViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_messages, container, false)

        setupAdapter(rootView, Glide.with(this), itemClickListeners, encryptedMessageCallbacks)

        subscribeUi(viewModel)

        return rootView
    }

    private fun setupAdapter(
        rootView: View,
        glide: RequestManager,
        itemClickListeners: MessagesPagedAdapter.ItemClickListeners,
        encryptedMessageCallbacks: MessagesPagedAdapter.EncryptedMessageCallbacks?
    ) {
        messagesAdapter = MessagesPagedAdapter(
            { message ->
                viewModel.readMessage(message)
            },
            appBase.authorizationManager.getAuthorizedUserId() ?: 0,
            conversationType,
            glide,
            object : MessagesPagedAdapter.ItemClickListeners {
                override fun onMessageClick(messageModel: MessageModel) {
                    if (!canClick()) return
                    itemClickListeners.onMessageClick(messageModel)
                }

                override fun onUserClick(userId: Long) {
                    if (!canClick()) return
                    itemClickListeners.onUserClick(userId)
                }

                override fun onChannelClick(channelId: Long) {
                    if (!canClick()) return
                    itemClickListeners.onChannelClick(channelId)
                }

                override fun onImageClick(imageUrl: String?) {
                    if (!canClick()) return
                    itemClickListeners.onImageClick(imageUrl)
                }

                override fun openFile(fileInfo: FileInfo, attachment: Attachment) {
                    if (!canClick()) return
                    itemClickListeners.openFile(fileInfo, attachment)
                }

                override fun downloadFile(fileInfo: FileInfo, attachmentId: Long) {
                    if (!canClick()) return
                    itemClickListeners.downloadFile(fileInfo, attachmentId)
                }

                override fun votePoll(optionId: Int, poll: Poll, callback: () -> Unit) {
                    if (!canClick()) return
                    itemClickListeners.votePoll(optionId, poll, callback)
                }

                override fun showVotedUsers(optionId: Int, poll: Poll) {
                    if (!canClick()) return
                    itemClickListeners.showVotedUsers(optionId, poll)
                }

                override fun updateMessage(messageId: String) {
                    viewModel.updateMessage(messageId)
                }

                override fun playVoice(filePath: String, callback: () -> Unit) {
                    itemClickListeners.playVoice(filePath, callback)
                }

                override fun pauseVoice() {
                    itemClickListeners.pauseVoice()
                }
            }, object : MessagesPagedAdapter.EncryptedMessageCallbacks {
                override fun decryptFile(
                    file: File,
                    senderId: Long,
                    keyId: Long,
                    signKeyId: Long,
                    decryptFileCallback: DialogViewModel.DecryptFileCallback
                ) {
                    encryptedMessageCallbacks?.decryptFile(
                        file,
                        senderId,
                        keyId,
                        signKeyId,
                        decryptFileCallback
                    )
                }

                override fun onEncryptedImageClick(image: ByteArray) {
                    encryptedMessageCallbacks?.onEncryptedImageClick(image)
                }

                override fun openEncryptedFile(
                    file: File,
                    senderId: Long,
                    keyId: Long,
                    signKeyId: Long
                ) {
                    encryptedMessageCallbacks?.openEncryptedFile(file, senderId, keyId, signKeyId)
                }
            }
        )
        val layoutManager = LinearLayoutManager(activity)
        layoutManager.reverseLayout = true
        rootView.rvMessages.layoutManager = layoutManager
        rootView.rvMessages.adapter = messagesAdapter
        // Adds margins to related messages
        rootView.rvMessages.addItemDecoration(MessageMarginItemDecoration(context!!))

        // Scrolls to 0 position if new message added to begin
        messagesAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0 && itemCount == 1) {
                    rootView.rvMessages.layoutManager?.scrollToPosition(0)
                }
            }
        })

        // Encryption is in dialogs only
        if (conversationType == ConversationType.DIALOG) {
            rootView.rvMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (availableToCheck()) {
                        checkForEncryptedMessages(layoutManager)
                    }
                }
            })

            // Instant restrict to make screenshots
            messagesAdapter.onRenderEncryptedMessage = {
                setSecureFlag()
            }
        }
    }

    private fun availableToCheck(): Boolean {
        var available: Boolean

        val minThreshold = 500L

        val now = System.currentTimeMillis()
        if (now - lastTimeCheckEncryptedMessages < minThreshold) {
            // Too many checks causes bad performance, reduce tries amount
            // But longer threshold gives ability to make screenshot while scrolling
            // Solution: check every bind message, and callback if it's encrypted
            available = false
        } else {
            lastTimeCheckEncryptedMessages = now
            available = true
        }

        return available
    }

    private fun checkForEncryptedMessages(layoutManager: LinearLayoutManager) {
        val fromPosition = layoutManager.findFirstVisibleItemPosition()
        val toPosition = layoutManager.findLastVisibleItemPosition()
        val showEncryptedMessages = messagesAdapter.showEncrypted(fromPosition, toPosition)

        if (showEncryptedMessages) {
            setSecureFlag()
        } else {
            clearSecureFlag()
        }
    }

    private fun setSecureFlag() {
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    private fun clearSecureFlag() {
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun subscribeUi(viewModel: MessagesViewModel) {
        viewModel.messageModels.observe(this, Observer { messageModels ->
            // Display no messages if we are in safe mode
            val messages = if (!appBase.safeModeManager.isSafeMode) messageModels else null
            messagesAdapter.submitList(messages)
            if (messages.isNullOrEmpty()) {
                tvMessagesCount.visibility = View.VISIBLE
            } else {
                tvMessagesCount.visibility = View.GONE
            }
        })

        viewModel.lastMessage.observe(this, Observer {
            viewModel.invalidateMessages()
        })
    }

    /**
     * This function is needed to invalidate messages for example if there was saved new
     * symmetric key and you need to try to decrypt messages again.
     */
    fun invalidateMessages() {
        if (::viewModel.isInitialized) {
            viewModel.invalidateMessages()
        }
    }
}