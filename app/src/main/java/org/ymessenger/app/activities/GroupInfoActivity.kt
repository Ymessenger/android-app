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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.android.synthetic.main.activity_group_info.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.GroupInfoAdapter
import org.ymessenger.app.data.ChatUserType
import org.ymessenger.app.data.local.db.models.ChatUserModel
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.GroupInfoViewModel

class GroupInfoActivity : BaseActivity() {

    private lateinit var viewModel: GroupInfoViewModel
    private var currentChatUser: ChatUserModel? = null

    companion object {
        private const val CHAT_ID = "chat_id"

        private const val REQUEST_CODE_PICK_USERS_TO_ADD = 100

        const val RESULT_CODE_CHAT_DELETED = 20

        fun getIntent(context: Context, chatId: Long): Intent {
            val intent = Intent(context, GroupInfoActivity::class.java)
            intent.putExtra(CHAT_ID, chatId)

            return intent
        }

        fun startActivity(context: Context, chatId: Long) {
            context.startActivity(getIntent(context, chatId))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_info)

        val chatId = intent.extras?.getLong(CHAT_ID) ?: throw Exception("Chat id is null")
        val currentUserId = appBase.authorizationManager.getAuthorizedUserId()
            ?: throw NullPointerException("currentUserId is null")

        val factory1 = Injection.provideGroupInfoViewModelFactory(
            appBase,
            chatId,
            currentUserId
        )
        viewModel = ViewModelProviders.of(this, factory1).get(GroupInfoViewModel::class.java)

        initToolbar()

        val groupInfoAdapter = GroupInfoAdapter({ chatUserModel ->
            if (!canClick()) return@GroupInfoAdapter
            UserProfileActivity.startActivity(this, chatUserModel.chatUser.userId)
        }, { chatUserModel ->
            showChatUserMenu(chatUserModel)
        }, {
            if (!canClick()) return@GroupInfoAdapter
            openGroupPhoto(it)
        }, {
            viewModel.switchFavorite()
        }, {
            copyTag(it)
        })
        val linearLayoutManager = LinearLayoutManager(this)
        rvUsers.layoutManager = linearLayoutManager
        rvUsers.addItemDecoration(DividerItemDecoration(this, linearLayoutManager.orientation))
        rvUsers.adapter = groupInfoAdapter

        fabAction.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            pickUsersToAdd()
        }

        subscribeUi(viewModel, groupInfoAdapter)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_info_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val currentChatUserModel = viewModel.currentChatUser.value
        menu?.findItem(R.id.mi_edit_chat)?.isVisible = currentChatUserModel?.canEdit() == true
        menu?.findItem(R.id.mi_delete_chat)?.isVisible = currentChatUserModel?.isCreator() == true
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.mi_edit_chat -> {
                viewModel.openChatEdit()
                true
            }

            R.id.mi_delete_chat -> {
                deleteChat()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun subscribeUi(viewModel: GroupInfoViewModel, groupInfoAdapter: GroupInfoAdapter) {
        viewModel.chat.observe(this, Observer { chat ->
            if (chat != null) {
                groupInfoAdapter.setChat(chat)
            }
        })

        viewModel.favoriteConversation.observe(this, Observer {
            groupInfoAdapter.updateFavorite(it != null)
        })

        viewModel.chatUserModels.observe(this, Observer {
            groupInfoAdapter.submitList(it)
        })

        viewModel.currentChatUser.observe(this, Observer {
            currentChatUser = it

            if (it == null) {
                fabAction.hide()
            } else {
                fabAction.show()
            }

            invalidateOptionsMenu()
        })

        viewModel.subscribeOnEvents(this)

        viewModel.openChatEditEvent.observe(this, Observer { chatId ->
            ChatEditActivity.start(this, chatId)
        })

        viewModel.contactsToAdd.observe(this, Observer {
            val usersId = arrayListOf<Long>()
            for (contactModel in it) {
                usersId.add(contactModel.contact.userId)
            }
            viewModel.addUsersToGroup(usersId)
        })

        viewModel.chatDeletedEvent.observe(this, Observer {
            setResult(RESULT_CODE_CHAT_DELETED)
            finish()
        })

        appBase.getWebSocketService().getConnectionStatus().observe(this, Observer {
            viewModel.setConnected(it)
        })
    }

    private fun openGroupPhoto(chatPhotoUrl: String) {
        StfalconImageViewer.Builder(this, listOf(chatPhotoUrl)) { imageView, image ->
            Glide.with(this)
                .load(image)
                .thumbnail(0.1F)
                .apply(RequestOptions().placeholder(R.drawable.no_chat_photo).override(Target.SIZE_ORIGINAL))
                .into(imageView)
        }.withHiddenStatusBar(false).show()
    }

    private fun pickUsersToAdd() {
        val intent =
            ContactsActivity.getIntent(this, ContactsActivity.REQUEST_CODE_PICK_CONTACT_LIST)
        startActivityForResult(intent, REQUEST_CODE_PICK_USERS_TO_ADD)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_PICK_USERS_TO_ADD -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        @Suppress("UNCHECKED_CAST") val contactsId =
                            data.getSerializableExtra(ContactsActivity.ARRAY_CONTACTS_ID) as ArrayList<String>
                        viewModel.addContactsToGroup(contactsId)
                    }
                }
            }
        }
    }

    private fun showChatUserMenu(item: ChatUserModel) {
        val popupMenu = PopupMenu(this, null)
        val menu = popupMenu.menu
        popupMenu.menuInflater.inflate(R.menu.chat_user_menu, menu)

        val userRole = currentChatUser?.chatUser?.userRole ?: return

        // Display remove from chat
        menu.findItem(R.id.mi_remove_from_chat).isVisible = userRole > item.chatUser.userRole

        // Display user promotions/demotions
        if (userRole > item.chatUser.userRole) {
            menu.findItem(R.id.mi_demote_to_moderator).isVisible = item.isAdmin()

            menu.findItem(R.id.mi_promote_to_admin).isVisible =
                item.isModerator() && userRole == ChatUserType.CREATOR
            menu.findItem(R.id.mi_dismiss_moderator_rights).isVisible = item.isModerator()

            menu.findItem(R.id.mi_promote_to_moderator).isVisible =
                item.isChatUser() && userRole >= ChatUserType.ADMIN
        }

        if (!menu.hasVisibleItems()) return

        val dialog = BottomSheetBuilder(this)
            .setMode(BottomSheetBuilder.MODE_LIST)
            .setMenu(menu)
            .setItemClickListener {
                when (it.itemId) {
                    R.id.mi_dismiss_moderator_rights -> viewModel.setUserRole(
                        item.chatUser.userId,
                        ChatUserType.USER
                    )
                    R.id.mi_promote_to_moderator, R.id.mi_demote_to_moderator -> viewModel.setUserRole(
                        item.chatUser.userId,
                        ChatUserType.MODERATOR
                    )
                    R.id.mi_promote_to_admin -> viewModel.setUserRole(
                        item.chatUser.userId,
                        ChatUserType.ADMIN
                    )
                    R.id.mi_remove_from_chat -> removeUserFromChat(item)
                    else -> showToast(R.string.this_feature_unavailable)
                }
            }.createDialog()

        dialog.show()
    }

    private fun removeUserFromChat(chatUserModel: ChatUserModel) {
        AlertDialog.Builder(this)
            .setTitle(R.string.attention)
            .setMessage(R.string.do_you_want_remove_user_from_chat)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.removeUsersFromChat(listOf(chatUserModel.chatUser.userId))
            }.show()
    }

    private fun deleteChat() {
        AlertDialog.Builder(this)
            .setTitle(R.string.attention)
            .setMessage(R.string.do_you_really_want_delete_chat)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteChat()
            }.show()
    }

    private fun copyTag(tag: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.primaryClip =
            ClipData.newPlainText("Tag", tag)
        showToast(R.string.copied)
    }

}