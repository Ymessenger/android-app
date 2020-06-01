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

package org.ymessenger.app.fragments.addfile

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.fragment_gallery.view.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.GridSpacingItemDecoration
import org.ymessenger.app.adapters.ImagesAdapter
import org.ymessenger.app.fragments.BaseFragment


class GalleryFragment : BaseFragment() {

    private val imagesAdapter = ImagesAdapter()

    private var availableAttachments = 0

    companion object {
        private const val SPAN_COUNT = 4
        const val ARRAY_IMAGE_PATHS = "ARRAY_IMAGES_PATHS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        availableAttachments =
            activity?.intent?.getIntExtra(
                AddAttachmentFragment.ARG_AVAILABLE_ATTACHMENTS,
                0
            ) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_gallery, container, false)

        // For layout animation
        view.rootLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        initPhotos(view)

        view.fabSend.setOnClickListener { send() }

        return view
    }

    private fun initPhotos(view: View) {
        val gridLayoutManager = GridLayoutManager(activity, SPAN_COUNT)
        view.rvPhotos.layoutManager = gridLayoutManager
        view.rvPhotos.adapter = imagesAdapter
        view.rvPhotos.addItemDecoration(
            GridSpacingItemDecoration(
                SPAN_COUNT,
                activity!!.resources.getDimensionPixelSize(R.dimen.gapXSmall),
                true
            )
        )

        imagesAdapter.setMaxSelectionAmount(availableAttachments)
        imagesAdapter.setImages(getAllImagesPaths())
        imagesAdapter.onItemClick = {
            imageClick()
        }
        imagesAdapter.maximumSelected = {
            showToast(R.string.you_have_added_maximum_amount_of_attachments)
        }
    }

    private fun imageClick() {
        if (imagesAdapter.getSelectedImages().isNotEmpty()) {
            (activity as AppCompatActivity).supportActionBar?.title =
                getString(R.string.selected_placeholder, imagesAdapter.getSelectedImages().size)
            view?.fabSend?.show()
        } else {
            view?.fabSend?.hide()
            (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.add_files)
        }
    }

    private fun send() {
        val intent = Intent()
        intent.putStringArrayListExtra(ARRAY_IMAGE_PATHS, imagesAdapter.getSelectedImages())
        activity?.setResult(Activity.RESULT_OK, intent)
        activity?.finish()
    }

    private fun getAllImagesPaths(): List<String> {
        val paths = arrayListOf<String>()

        val cursor: Cursor
        var columnIndexData: Int

        var absolutePathOfImage: String?

        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        cursor = activity!!.contentResolver.query(
            uri,
            projection,
            null,
            null,
            MediaStore.MediaColumns.DATE_ADDED + " DESC"
        )!!

        columnIndexData = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(columnIndexData)

            paths.add(absolutePathOfImage)
        }

        cursor.close()

        return paths
    }

}