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

package org.ymessenger.app.adapters.viewholders

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import kotlinx.android.synthetic.main.item_attachment.view.*
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.entities.Attachment
import org.ymessenger.app.helpers.FileExtensionHelper
import org.ymessenger.app.models.AttachmentModel
import java.io.File

class AttachmentViewHolder(
    itemView: View,
    private val glide: RequestManager,
    private val onDeleteClick: (AttachmentModel) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    var item: AttachmentModel? = null

    init {
        itemView.btnDeleteAttachment.setOnClickListener {
            item?.let {
                onDeleteClick.invoke(it)
            }
        }
    }

    fun bind(attachmentModel: AttachmentModel) {
        this.item = attachmentModel

        itemView.tvAttachmentType.setText(Attachment.getAttachmentDescriptionRes(attachmentModel.type))

        if (attachmentModel.type == Attachment.Type.PICTURE && !attachmentModel.filePath.isNullOrEmpty()) {
            itemView.tvAttachmentType.visibility = View.GONE
            glide.load(attachmentModel.filePath).into(itemView.ivPhotoPreview)
            hideFile()
        } else {
            itemView.tvAttachmentType.visibility = View.VISIBLE
            glide.clear(itemView.ivPhotoPreview)

            if (attachmentModel.type == Attachment.Type.FILE) {
                showFile(attachmentModel)
            } else {
                hideFile()
            }
        }
    }

    private fun hideFile() {
        itemView.tvFileSize.visibility = View.GONE
        itemView.tvFileName.visibility = View.GONE
        itemView.imageFilter.visibility = View.GONE
    }

    private fun showFile(attachmentModel: AttachmentModel) {
        val file = File(attachmentModel.filePath)

        itemView.tvFileSize.visibility = View.VISIBLE
        itemView.tvFileName.visibility = View.VISIBLE
        itemView.imageFilter.visibility = View.VISIBLE

        itemView.tvFileName.text = file.nameWithoutExtension

        itemView.tvFileSize.text = Formatter.formatFileSize(
            itemView.context,
            file.length()
        )

        itemView.tvAttachmentType.text = file.extension

        if (FileExtensionHelper.isImage(file.extension)) {
            glide.load(file).into(itemView.ivPhotoPreview)
        } else {
            glide.clear(itemView.ivPhotoPreview)
        }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            glide: RequestManager,
            onDeleteClick: (AttachmentModel) -> Unit
        ): AttachmentViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_attachment, parent, false)
            return AttachmentViewHolder(view, glide, onDeleteClick)
        }
    }

}