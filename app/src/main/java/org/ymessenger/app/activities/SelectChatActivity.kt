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

package org.ymessenger.app.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_select_chat.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.ChatPreviewsAdapter
import org.ymessenger.app.adapters.FavoriteConversationsAdapter
import org.ymessenger.app.adapters.viewholders.BaseClickListener
import org.ymessenger.app.data.local.db.models.ChatPreviewModel
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.SelectChatViewModel

class SelectChatActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_chat)

        initToolbar()

        val userId = appBase.authorizationManager.getAuthorizedUserId()

        if (userId == null) {
            Log.e(TAG, "UserId is null")
            showToast(R.string.unknown_error)
            finish()
            return
        }

        val factory = Injection.provideSelectChatViewModelFactory(appBase)
        val viewModel = ViewModelProviders.of(this, factory).get(SelectChatViewModel::class.java)

        val glide = Glide.with(this)

        val favoriteConversationAdapter = FavoriteConversationsAdapter({
            if (canClick()) {
                if (it == null) {
                    // nothing
                } else {
                    val identifier = it.favoriteConversation.identifier
                    chatSelected(identifier, it.favoriteConversation.conversationType)
                }
            }
        }, {
            // nothing
        }, true)

        val chatPreviewsAdapter =
            ChatPreviewsAdapter(userId, glide, object : BaseClickListener<ChatPreviewModel> {
                override fun onClick(item: ChatPreviewModel) {
                    if (!canClick()) return

                    val identifier = if (item.chatPreview.isDialog()) {
                        item.chatPreview.userId!!
                    } else {
                        item.chatPreview.conversationId
                    }

                    chatSelected(identifier, item.chatPreview.conversationType)
                }

                override fun onLongClick(item: ChatPreviewModel) {
                    // nothing
                }
            }, favoriteConversationAdapter, {
                // nothing
            }, true)
        initChats(chatPreviewsAdapter)

        subscribeUi(viewModel, chatPreviewsAdapter, favoriteConversationAdapter)
    }

    private fun initChats(chatPreviewsAdapter: ChatPreviewsAdapter) {
        val linearLayoutManager = LinearLayoutManager(this)
        rvChats.layoutManager = linearLayoutManager
        rvChats.addItemDecoration(
            object : DividerItemDecoration(
                this,
                linearLayoutManager.orientation
            ) {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val position = parent.getChildAdapterPosition(view)
                    // hide the divider for the first child
                    if (position == 0) {
                        outRect.setEmpty()
                    } else {
                        super.getItemOffsets(outRect, view, parent, state)
                    }
                }
            }
        )
        rvChats.adapter = chatPreviewsAdapter
    }

    private fun subscribeUi(
        viewModel: SelectChatViewModel,
        chatPreviewsAdapter: ChatPreviewsAdapter,
        favoriteConversationsAdapter: FavoriteConversationsAdapter
    ) {
        viewModel.subscribeOnEvents(this)

        viewModel.chatPreviews.observe(this, Observer { chatPreviews ->
            // Hide actual results if we are in safe mode
            chatPreviewsAdapter.submitList(if (appBase.safeModeManager.isSafeMode) listOf() else chatPreviews)
        })

        viewModel.favoriteConversations.observe(this, Observer {
            favoriteConversationsAdapter.submitList(it)
        })
    }

    private fun chatSelected(identifier: Long, conversationType: Int) {
        val intentData = Intent()
        intentData.putExtra(KEY_IDENTIFIER, identifier)
        intentData.putExtra(KEY_CONVERSATION_TYPE, conversationType)
        setResult(Activity.RESULT_OK, intentData)
        finish()
    }

    companion object {
        private const val TAG = "SelectChatActivity"

        const val KEY_IDENTIFIER = "identifier"
        const val KEY_CONVERSATION_TYPE = "conversationType"

        fun getIntent(context: Context): Intent {
            return Intent(context, SelectChatActivity::class.java)
        }
    }

}