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
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.android.synthetic.main.activity_conversation_gallery.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.ConversationGalleryAdapter
import org.ymessenger.app.adapters.GridSpacingItemDecoration
import org.ymessenger.app.data.local.db.entities.Attachment
import org.ymessenger.app.data.remote.UrlGenerator
import org.ymessenger.app.di.Injection
import org.ymessenger.app.viewmodels.ConversationGalleryViewModel

class ConversationGalleryActivity : BaseActivity() {

    private lateinit var viewModel: ConversationGalleryViewModel

    private var photoAttachments: List<Attachment> = listOf()

    companion object {
        private const val TAG = "ConversationGallery"

        private const val ARG_CONVERSATION_ID = "conversationId"
        private const val ARG_CONVERSATION_TYPE = "conversationType"
        private const val ARG_MODE_SELECTION = "modeSelection"

        const val DATA_PHOTO_URL = "dataPhotoUrl"

        private const val SPAN_COUNT = 3

        fun getIntent(
            context: Context,
            conversationId: Long,
            conversationType: Int,
            modeSelection: Boolean
        ): Intent {
            val intent = Intent(context, ConversationGalleryActivity::class.java)
            intent.putExtra(ARG_CONVERSATION_ID, conversationId)
            intent.putExtra(ARG_CONVERSATION_TYPE, conversationType)
            intent.putExtra(ARG_MODE_SELECTION, modeSelection)

            return intent
        }

        fun start(
            context: Context,
            conversationId: Long,
            conversationType: Int,
            modeSelection: Boolean = false
        ) {
            val intent = getIntent(context, conversationId, conversationType, modeSelection)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation_gallery)
        initToolbar()

        if (!intent.hasExtra(ARG_CONVERSATION_ID) || !intent.hasExtra(ARG_CONVERSATION_TYPE)) {
            throw IllegalArgumentException("You must start this activity using static method 'start'")
        }

        val conversationId = intent.extras?.getLong(ARG_CONVERSATION_ID)
            ?: throw Exception("Conversation id is null")
        val conversationType = intent.extras?.getInt(ARG_CONVERSATION_TYPE)
            ?: throw Exception("Conversation type is null")
        val modeSelection = intent.extras?.getBoolean(ARG_MODE_SELECTION)
            ?: throw Exception("Mode selection is not specified")

        val factory = Injection.provideConversationGalleryViewModelFactory(
            appBase,
            conversationId,
            conversationType
        )
        viewModel =
            ViewModelProviders.of(this, factory).get(ConversationGalleryViewModel::class.java)


        val gridLayoutManager = GridLayoutManager(this, SPAN_COUNT)
        rvPhotos.layoutManager = gridLayoutManager
        rvPhotos.addItemDecoration(
            GridSpacingItemDecoration(
                SPAN_COUNT,
                resources.getDimensionPixelSize(R.dimen.gapXSmall),
                true
            )
        )
        val adapter = ConversationGalleryAdapter(Glide.with(this)) { attachment, position ->
            if (!canClick()) return@ConversationGalleryAdapter

            if (modeSelection) {
                val url = UrlGenerator.getFileUrl(attachment.getPayloadAsFile()?.fileId) ?: ""
                selectImage(url)
            } else {
                openImage(position)
            }
        }
        rvPhotos.adapter = adapter

        subscribeUi(viewModel, adapter)
    }

    private fun subscribeUi(
        viewModel: ConversationGalleryViewModel,
        adapter: ConversationGalleryAdapter
    ) {
        viewModel.attachmentList.observe(this, Observer {
            adapter.submitList(it)
            photoAttachments = it
        })
    }

    private fun openImage(position: Int) {
        StfalconImageViewer.Builder(this, photoAttachments) { imageView, attachment ->
            val url = UrlGenerator.getFileUrl(attachment.getPayloadAsFile()?.fileId)

            Glide.with(this)
                .load(url)
                .thumbnail(0.1F)
                .apply(RequestOptions().override(Target.SIZE_ORIGINAL))
                .into(imageView)
        }.withHiddenStatusBar(false)
            .withStartPosition(position)
            .show()
    }

    private fun selectImage(url: String) {
        val intent = Intent()
        intent.putExtra(DATA_PHOTO_URL, url)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}