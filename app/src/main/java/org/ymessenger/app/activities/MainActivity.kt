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
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.menu_header.view.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.ChatPreviewsAdapter
import org.ymessenger.app.adapters.FavoriteConversationsAdapter
import org.ymessenger.app.adapters.viewholders.BaseClickListener
import org.ymessenger.app.data.ConversationType
import org.ymessenger.app.data.local.db.entities.User
import org.ymessenger.app.data.local.db.models.ChatPreviewModel
import org.ymessenger.app.data.remote.NetworkState
import org.ymessenger.app.data.remote.entities.Message
import org.ymessenger.app.di.Injection
import org.ymessenger.app.helpers.MyNotificationManager
import org.ymessenger.app.services.AsymmetricKeysGeneratorService
import org.ymessenger.app.viewmodels.ChatListViewModel


class MainActivity : BaseActivity() {

    private lateinit var viewModel: ChatListViewModel

    private var afterRegister: Boolean = false

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PICK_USER_FOR_FAVOURITE = 100
        private const val REQUEST_CODE_PICK_USER_FOR_DIALOG = 101
        private const val REQUEST_CODE_CREATE_GROUP = 200
        private const val REQUEST_CODE_SELECT_CHAT = 300

        private const val ARG_AFTER_REGISTER = "ARG_AFTER_REGISTER"
        private const val ARG_GENERATE_QR = "ARG_GENERATE_QR"

        const val ARG_SAFE_MODE = "ARG_SAFE_MODE"

        fun getIntentAfterRegister(context: Context): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(ARG_AFTER_REGISTER, true)

            return intent
        }

        fun getIntentToGenerateQR(context: Context): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(ARG_GENERATE_QR, true)

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val safeMode = intent.getBooleanExtra(ARG_SAFE_MODE, false)
        if (safeMode) {
            appBase.isLocked = false
            appBase.safeModeManager.enterSafeMode()
        }

        when {
            intent?.action == Intent.ACTION_SEND -> {
                startActivityForResult(SelectChatActivity.getIntent(this), REQUEST_CODE_SELECT_CHAT)
            }

            else -> {
                // Handle other intents, such as being started from the home screen
            }
        }

        val factory = Injection.provideChatListViewModelFactory(appBase)
        viewModel = ViewModelProviders.of(this, factory).get(ChatListViewModel::class.java)

        afterRegister = intent.getBooleanExtra(ARG_AFTER_REGISTER, false)
            .or(viewModel.getAlwaysOpenMainActivityAsAfterRegister())

        initToolbar()
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)

        initNavigation()
        initSwipeRefreshLayout()
        val userId = appBase.settingsHelper.getToken()!!.userId
        val glide = Glide.with(this)

        val favoriteConversationAdapter = FavoriteConversationsAdapter({
            if (canClick()) {
                if (it == null) {
                    addNewContactToFavourites()
                } else {
                    val identifier = it.favoriteConversation.identifier

                    when (it.favoriteConversation.conversationType) {
                        ConversationType.DIALOG -> {
                            viewModel.openDialog(identifier)
                        }

                        ConversationType.CHAT -> {
                            viewModel.openChat(identifier)
                        }

                        ConversationType.CHANNEL -> {
                            viewModel.openChannel(identifier)
                        }

                        else -> {
                            unsupportedOperation()
                        }
                    }

                }
            }
        }, {
            viewModel.reorderFavoriteConversations(it.map { it.favoriteConversation })
        })

        val chatPreviewsAdapter =
            ChatPreviewsAdapter(userId, glide, object : BaseClickListener<ChatPreviewModel> {
                override fun onClick(item: ChatPreviewModel) {
                    if (!canClick()) return
                    when (item.chatPreview.conversationType) {
                        ConversationType.CHAT -> viewModel.openChat(item.chatPreview.conversationId)
                        ConversationType.CHANNEL -> viewModel.openChannel(item.chatPreview.conversationId)
                        ConversationType.DIALOG -> viewModel.openDialog(item.chatPreview.userId!!)
                    }
                }

                override fun onLongClick(item: ChatPreviewModel) {
                    showConversationMenu(item)
                }
            }, favoriteConversationAdapter, {
                startActivity(Intent(this, GlobalSearchActivity::class.java))
            })
        initChats(chatPreviewsAdapter)

        fabNewChat.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            createNewChat()
        }



        subscribeUi(viewModel, chatPreviewsAdapter, favoriteConversationAdapter)

        if (afterRegister) {
            startActivity(Intent(this, RegisteredActivity::class.java))
        }

        // If we signed in with QR, then offer user to generate new one
        if (intent.getBooleanExtra(ARG_GENERATE_QR, false)) {
            startActivity(Intent(this, GetQRCodeForLoginActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.initTimer()
    }

    override fun onPause() {
        super.onPause()
        viewModel.cancelTimer()
    }

    private fun initNavigation() {
        nav_view.setNavigationItemSelectedListener { menuItem ->
            drawer_layout.closeDrawers()

            when (menuItem.itemId) {
                R.id.mi_create_group_chat -> createGroupChat()
                R.id.mi_tasks -> showTasks()
                R.id.mi_files -> showFiles()
                R.id.mi_contacts -> showContacts()
                R.id.mi_contact_groups -> showContactGroups()
                R.id.mi_send_invite -> sendInvite()
                R.id.mi_information -> openInformation()
                R.id.mi_settings -> openSettings()
            }

            true
        }

        drawer_layout.addDrawerListener(
            object : DrawerLayout.DrawerListener {
                override fun onDrawerStateChanged(p0: Int) {
                }

                override fun onDrawerSlide(p0: View, p1: Float) {
                }

                override fun onDrawerClosed(p0: View) {
                }

                override fun onDrawerOpened(p0: View) {
                }
            }
        )

        // get header of navigation view
        val navHeader = nav_view.getHeaderView(0)

        navHeader.btnLogout.setOnClickListener { logout() }
    }

    private fun initSwipeRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.updateChatPreviews()
        }
    }

    private fun subscribeUi(
        viewModel: ChatListViewModel,
        chatPreviewsAdapter: ChatPreviewsAdapter,
        favoriteConversationsAdapter: FavoriteConversationsAdapter
    ) {
        viewModel.subscribeOnEvents(this)

        viewModel.chatPreviews.observe(this, Observer { chatPreviews ->
            // Hide actual results if we are in safe mode
            val chatPreviewsToDisplay =
                if (appBase.safeModeManager.isSafeMode) listOf() else chatPreviews
            chatPreviewsAdapter.submitList(chatPreviewsToDisplay)

            if (chatPreviewsToDisplay.isEmpty()) {
                tvNoChats.visibility = View.VISIBLE
            } else {
                tvNoChats.visibility = View.GONE
            }
        })

        viewModel.chatPreviewsNetworkState.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it == NetworkState.LOADING
        })

        viewModel.favoriteConversations.observe(this, Observer {
            favoriteConversationsAdapter.submitList(it)
        })

        viewModel.openChatEvent.observe(this, Observer { chatId ->
            ChatActivity.startActivity(this, chatId)
        })

        viewModel.openDialogEvent.observe(this, Observer { userId ->
            DialogActivity.start(this, userId)
        })

        viewModel.openChannelEvent.observe(this, Observer { channelId ->
            ChannelActivity.startActivity(this, channelId)
        })

        appBase.getWebSocketService().getConnectionStatus().observe(this, Observer { connected ->
            supportActionBar?.apply {
                title = if (connected) {
                    getString(R.string.messages)
                } else {
                    getString(R.string.connection)
                }
            }
        })

        viewModel.logoutEvent.observe(this, Observer {
            goToEnterActivity()
        })

        viewModel.currentUser.observe(this, Observer {
            if (it != null) {
                appBase.authorizationManager.authorizedUser = it
                updateCurrentUserInfo(it)
            }
        })

        viewModel.generateShortKeysEvent.observe(this, Observer {
            // start keys generator service
            startService(
                AsymmetricKeysGeneratorService.getIntent(
                    this,
                    AsymmetricKeysGeneratorService.ACTION_GET_SHORT_KEYS
                )
            )
        })

        viewModel.authorizedEvent.observe(this, Observer {
            Log.d(TAG, "Authorized event. Call updateChatPreviews()")
            viewModel.updateChatPreviews()
        })

        viewModel.setPrivateEncryptKeyToNotificationHandlerEvent.observe(this, Observer {
            appBase.notificationHandler.setPrivateEncryptKeyToReceive(it)
        })

        viewModel.lastUserActionList.observe(this, Observer {
            chatPreviewsAdapter.setLastUserActionList(it)
        })

        viewModel.askToSetPassphraseEvent.observe(this, Observer {
            askToSetPassphrase()
        })
    }

    private fun updateCurrentUserInfo(currentUser: User) {
        // set user avatar and name
        val navHeader = nav_view.getHeaderView(0)

        navHeader.tvPhotoLabel.text = currentUser.getPhotoLabel()

        val photoUrl = currentUser.getPhotoUrl()
        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .into(navHeader.ivUserAvatar)
            navHeader.ivUserAvatar.visibility = View.VISIBLE
        } else {
            navHeader.ivUserAvatar.visibility = View.INVISIBLE
        }

        navHeader.tvUserName.text = currentUser.fullName

        tvBanned.visibility = if (currentUser.banned) View.VISIBLE else View.GONE
    }

    private fun createGroupChat() {
        startActivityForResult(
            Intent(this, CreateGroupActivity::class.java),
            REQUEST_CODE_CREATE_GROUP
        )
    }

    private fun showTasks() {
        Toast.makeText(this, "Show tasks", Toast.LENGTH_SHORT).show()
    }

    private fun showFiles() {
        startActivity(Intent(this, FilesActivity::class.java))
    }

    private fun showContacts() {
        startActivity(Intent(this, ContactsActivity::class.java))
    }

    private fun showContactGroups() {
        startActivity(Intent(this, ContactGroupsActivity::class.java))
    }

    private fun sendInvite() {
        val googlePlayLink = "https://play.google.com/store/apps/details?id=org.ymessenger.app"
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Hi! Join YMessenger now! Google Play: $googlePlayLink")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Send invite"))
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                drawer_layout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun openInformation() {
        val infoUrl = "https://ymessenger.org/en/user-guide"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(infoUrl)))
    }

    private fun logout() {
        AlertDialog.Builder(this)
            .setMessage(R.string.do_you_really_want_logout)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.logout) { _, _ ->
                viewModel.logout()
            }.show()
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

        // Hide FAB while scrolling
        rvChats.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && fabNewChat.isShown) {
                    fabNewChat.hide()
                } else if (dy < 0 && !fabNewChat.isShown) {
                    fabNewChat.show()
                }
            }
        })
    }

    private fun showConversationMenu(chatPreviewModel: ChatPreviewModel) {
        val popupMenu = PopupMenu(this, null)
        val menu = popupMenu.menu
        popupMenu.menuInflater.inflate(R.menu.chat_preview_menu, menu)

        menu.findItem(R.id.mi_mute).isVisible = !chatPreviewModel.isMuted()
        menu.findItem(R.id.mi_unmute).isVisible = chatPreviewModel.isMuted()

        menu.findItem(R.id.mi_add_to_favorites).isVisible = !chatPreviewModel.isFavourite()
        menu.findItem(R.id.mi_remove_from_favorites).isVisible = chatPreviewModel.isFavourite()

        val identifier = if (chatPreviewModel.chatPreview.isDialog()) {
            chatPreviewModel.chatPreview.userId
        } else {
            chatPreviewModel.chatPreview.conversationId
        }

        if (!menu.hasVisibleItems()) return

        val dialog = BottomSheetBuilder(this)
            .setMode(BottomSheetBuilder.MODE_LIST)
            .setMenu(menu)
            .setItemClickListener {
                when (it.itemId) {
                    R.id.mi_clear_messages_locally -> deleteAllConversationMessages(
                        chatPreviewModel.chatPreview.conversationId,
                        chatPreviewModel.chatPreview.conversationType
                    )
                    R.id.mi_delete -> deleteChatPreview(chatPreviewModel)

                    R.id.mi_mute -> viewModel.muteNotifications(chatPreviewModel.chatPreview)

                    R.id.mi_unmute -> viewModel.muteNotifications(chatPreviewModel.chatPreview)

                    R.id.mi_add_to_favorites -> {
                        identifier?.let {
                            viewModel.addToFavorites(
                                it,
                                chatPreviewModel.chatPreview.conversationType
                            )
                        }
                    }

                    R.id.mi_remove_from_favorites -> {
                        identifier?.let {
                            viewModel.removeFromFavorites(
                                it,
                                chatPreviewModel.chatPreview.conversationType
                            )
                        }
                    }

                    else -> showToast(R.string.this_feature_unavailable)
                }
            }.createDialog()

        dialog.show()
    }

    private fun deleteAllConversationMessages(conversationId: Long, conversationType: Int) {
        AlertDialog.Builder(this)
            .setTitle(R.string.attention)
            .setMessage(R.string.do_you_want_delete_messages)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteAllConversationMessages(conversationId, conversationType)
            }.show()
    }

    private fun deleteChatPreview(chatPreviewModel: ChatPreviewModel) {
        viewModel.deleteChatPreview(
            chatPreviewModel.chatPreview.conversationId,
            chatPreviewModel.chatPreview.conversationType
        )
    }

    private fun createNewChat() {
        val intent = Intent(this, ContactsActivity::class.java)
        intent.putExtra(ContactsActivity.REQUEST_CODE, ContactsActivity.REQUEST_CODE_CHOOSE_CONTACT)

        startActivityForResult(intent, REQUEST_CODE_PICK_USER_FOR_DIALOG)
    }

    private fun addNewContactToFavourites() {
        val intent = Intent(this, ContactsActivity::class.java)
        intent.putExtra(ContactsActivity.REQUEST_CODE, ContactsActivity.REQUEST_CODE_CHOOSE_CONTACT)

        startActivityForResult(intent, REQUEST_CODE_PICK_USER_FOR_FAVOURITE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_PICK_USER_FOR_FAVOURITE -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null && data.hasExtra(ContactsActivity.USER_ID)) {
                        val userId = data.getLongExtra(ContactsActivity.USER_ID, -1)
                        viewModel.addUserToFavourites(userId)
                    }
                }
            }

            REQUEST_CODE_PICK_USER_FOR_DIALOG -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null && data.hasExtra(ContactsActivity.USER_ID)) {
                        val userId = data.getLongExtra(ContactsActivity.USER_ID, -1)

                        viewModel.openDialog(userId)
                    }
                }
            }

            REQUEST_CODE_CREATE_GROUP -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null && data.hasExtra(CreateGroupActivity.CONVERSATION_ID) && data.hasExtra(
                            CreateGroupActivity.CONVERSATION_TYPE
                        )
                    ) {
                        val conversationId =
                            data.getLongExtra(CreateGroupActivity.CONVERSATION_ID, -1)
                        val conversationType =
                            data.getIntExtra(CreateGroupActivity.CONVERSATION_TYPE, -1)

                        when (conversationType) {
                            ConversationType.CHAT -> viewModel.openChat(conversationId)
                            ConversationType.CHANNEL -> viewModel.openChannel(conversationId)
                        }
                    }
                }
            }

            REQUEST_CODE_SELECT_CHAT -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val conversationType =
                        data.extras?.getInt(SelectChatActivity.KEY_CONVERSATION_TYPE)
                            ?: throw Exception("Conversation type is null")
                    val identifier = data.extras?.getLong(SelectChatActivity.KEY_IDENTIFIER)
                        ?: throw Exception("Identifier is null")
                    chooseConversationForShare(conversationType, identifier)
                }
            }
        }
    }

    private fun chooseConversationForShare(conversationType: Int, identifier: Long) {
        if ("text/plain" == intent.type) {
            handleSendText(intent, conversationType, identifier) // Handle text being sent
        } else if (intent.type?.startsWith("image/") == true) {
            handleSendImage(intent, conversationType, identifier) // Handle single image being sent
        }
    }

    private fun handleSendText(intent: Intent, conversationType: Int, identifier: Long) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            // Update UI to reflect text being shared
            when (conversationType) {
                ConversationType.DIALOG -> {
                    DialogActivity.shareText(this, identifier, it.trim())
                }

                ConversationType.CHAT -> {
                    ChatActivity.shareText(this, identifier, it.trim())
                }

                ConversationType.CHANNEL -> {
                    ChannelActivity.shareText(this, identifier, it.trim())
                }

                else -> {
                    unsupportedOperation()
                }
            }
        }
    }

    private fun handleSendImage(intent: Intent, conversationType: Int, identifier: Long) {
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
            // Update UI to reflect image being shared

            when (conversationType) {
                ConversationType.DIALOG -> {
                    DialogActivity.shareImage(this, identifier, it)
                }

                ConversationType.CHAT -> {
                    ChatActivity.shareImage(this, identifier, it)
                }

                ConversationType.CHANNEL -> {
                    ChannelActivity.shareImage(this, identifier, it)
                }

                else -> unsupportedOperation()
            }
        }
    }

    override fun onMessageReceived(remoteMessage: Message) {
        val appInBackground = appBase.appInBackground
        if (appInBackground && remoteMessage.senderId != appBase.authorizationManager.getAuthorizedUserId()) {
            MyNotificationManager.showNewMessageNotification(appBase, remoteMessage)
        }
    }

    private fun askToSetPassphrase() {
        AlertDialog.Builder(this)
            .setTitle(R.string.attention)
            .setMessage(R.string.you_need_to_set_a_passphrase)
            .setPositiveButton(R.string.ok) { dialogInterface, i ->
                startActivity(Intent(this, SetPassphraseActivity::class.java))
                viewModel.setAskedToSetPassphrase()
            }.setNegativeButton(R.string.later) { dialogInterface, i ->
                viewModel.setAskedToSetPassphrase()
            }.show()
    }
}
