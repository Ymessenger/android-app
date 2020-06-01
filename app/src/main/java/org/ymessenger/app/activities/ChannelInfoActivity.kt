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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.android.synthetic.main.activity_channel_info.*
import kotlinx.android.synthetic.main.channel_info_header.*
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.entities.Channel
import org.ymessenger.app.data.local.db.models.ChatUserModel
import org.ymessenger.app.di.Injection
import org.ymessenger.app.utils.ShortNumberUtils
import org.ymessenger.app.viewmodels.ChannelInfoViewModel

class ChannelInfoActivity : BaseActivity() {

    private lateinit var viewModel: ChannelInfoViewModel

    companion object {
        private const val CHANNEL_ID = "channel_id"

        const val RESULT_CODE_CHANNEL_DELETED = 6

        fun getIntent(context: Context, channelId: Long): Intent {
            val intent = Intent(context, ChannelInfoActivity::class.java)
            intent.putExtra(CHANNEL_ID, channelId)

            return intent
        }

        fun startActivity(context: Context, channelId: Long) {
            context.startActivity(getIntent(context, channelId))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel_info)

        val currentUserId =
            appBase.authorizationManager.getAuthorizedUserId()
                ?: throw NullPointerException("currentUserId is null")
        val channelId = intent.extras?.getLong(CHANNEL_ID) ?: throw Exception("Channel id is null")

        val factory =
            Injection.provideChannelInfoViewModelFactory(appBase, channelId, currentUserId)
        viewModel = ViewModelProviders.of(this, factory).get(ChannelInfoViewModel::class.java)

        initToolbar()

        btnSubscribers.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            viewModel.openSubscribers()
        }

        btnJoinChannel.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            viewModel.joinChannel()
        }

        btnLeaveChannel.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            AlertDialog.Builder(this)
                .setMessage(R.string.do_you_really_want_leave_channel)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.leave) { _, _ ->
                    viewModel.leaveChannel()
                }.show()
        }

        btnDeleteChannel.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            AlertDialog.Builder(this)
                .setMessage(R.string.do_you_really_want_delete_channel)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewModel.deleteChannel()
                }.show()
        }

        ivGroupPhoto.setOnClickListener {
            if (!canClick()) return@setOnClickListener
            viewModel.openPhoto()
        }

        btnFavourite.setOnClickListener {
            viewModel.switchFavorite()
        }

        tvTag.setOnClickListener {
            val tagText = tvTag.text.toString().trim()
            if (tagText.isNotBlank()) {
                copyTag(tagText)
            }
        }

        subscribeUi(viewModel)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.mi_edit_channel -> {
                viewModel.openChannelEdit()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.channel_info_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.mi_edit_channel)?.isVisible =
            viewModel.channel.value?.isAdministrator() ?: false
        return super.onPrepareOptionsMenu(menu)
    }

    private fun subscribeUi(viewModel: ChannelInfoViewModel) {
        viewModel.subscribeOnEvents(this)

        viewModel.channel.observe(this, Observer {
            if (it != null) {
                setChannel(it)
            }
        })

        viewModel.favoriteConversation.observe(this, Observer {
            val isFavorite = it != null
            btnFavourite.setImageResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border)
        })

        viewModel.openChannelEditEvent.observe(this, Observer {
            ChannelEditActivity.start(this, it)
        })

        viewModel.channelDeletedEvent.observe(this, Observer {
            setResult(RESULT_CODE_CHANNEL_DELETED)
            finish()
        })

        viewModel.openSubscribersEvent.observe(this, Observer {
            ChannelUsersActivity.start(this, it)
        })

        viewModel.openPhotoEvent.observe(this, Observer { photoUrl ->
            photoUrl?.let {
                openPhoto(it)
            }
        })

        appBase.getWebSocketService().getConnectionStatus().observe(this, Observer {
            viewModel.setConnected(it)
        })
    }

    private fun setChannel(channel: Channel) {
        if (channel.deleted) {
            showToast(R.string.channel_is_deleted)
            finish()
            return
        }


        tvPhotoLabel.text = channel.getPhotoLabel()
        val photoUrl = channel.getPhotoUrl()
        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .into(ivGroupPhoto)
            ivGroupPhoto.visibility = View.VISIBLE
        } else {
            ivGroupPhoto.visibility = View.INVISIBLE
        }

        val subscribersCount = channel.subscribersCount
        tvSubscribersCount.text = resources.getQuantityString(
            R.plurals.subscribers,
            subscribersCount,
            ShortNumberUtils.getShortNumber(subscribersCount)
        )

        tvGroupName.text = channel.name
        if (channel.about != null) {
            tvAbout.text = channel.about
            tvAbout.visibility = View.VISIBLE
            tvAboutLabel.visibility = View.VISIBLE
        } else {
            tvAbout.visibility = View.GONE
            tvAboutLabel.visibility = View.GONE
        }

        tvTag.text = channel.tag

        btnSubscribers.visibility = if (channel.isAdministrator()) View.VISIBLE else View.GONE

        invalidateOptionsMenu()

        if (channel.userRole == null) {
            btnJoinChannel.visibility = View.VISIBLE
            btnLeaveChannel.visibility = View.GONE
        } else {
            btnJoinChannel.visibility = View.GONE
            btnLeaveChannel.visibility = View.VISIBLE
        }

        if (channel.isCreator()) {
            btnDeleteChannel.visibility = View.VISIBLE
            btnLeaveChannel.visibility = View.GONE
        } else {
            btnDeleteChannel.visibility = View.GONE
        }
    }

    private fun openPhoto(photoUrl: String) {
        StfalconImageViewer.Builder(this, listOf(photoUrl)) { imageView, image ->
            Glide.with(this)
                .load(image)
                .thumbnail(0.1F)
                .apply(RequestOptions().placeholder(R.drawable.no_chat_photo).override(Target.SIZE_ORIGINAL))
                .into(imageView)
        }.withHiddenStatusBar(false).show()
    }

    private fun removeUserFromChat(chatUserModel: ChatUserModel) {
        AlertDialog.Builder(this)
            .setTitle(R.string.attention)
            .setMessage(R.string.do_you_want_remove_user_from_chat)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.removeUsersFromChannel(listOf(chatUserModel.chatUser.userId))
            }.show()
    }

    private fun copyTag(tag: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.primaryClip =
            ClipData.newPlainText("Tag", tag)
        showToast(R.string.copied)
    }

}