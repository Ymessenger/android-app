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

package org.ymessenger.app.adapters

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.item_file_list.view.*
import org.ymessenger.app.R
import org.ymessenger.app.models.FileInfo
import java.io.File

class FilesAdapter : RecyclerView.Adapter<FilesAdapter.Holder>() {

    private val files = arrayListOf<FileInfo>()
    var onItemClick: ((FileInfo) -> Unit)? = null
    private var displayFormat = DisplayFormat.LINE

    private val selectedFilePaths = hashSetOf<String>()

    enum class DisplayFormat(val layout: Int) {
        LINE(R.layout.item_file_list),
        GRID(R.layout.item_file_grid)
    }

    fun setDisplayFormat(displayFormat: DisplayFormat) {
        this.displayFormat = displayFormat
        notifyDataSetChanged()
    }

    fun setData(files: List<FileInfo>) {
        this.files.clear()
        this.files.addAll(files)

        notifyDataSetChanged()
    }

    fun setSelectedFiles(selectedFiles: HashSet<String>) {
        selectedFilePaths.clear()
        selectedFilePaths.addAll(selectedFiles)

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(displayFormat.layout, parent, false)

        return Holder(view)
    }

    override fun getItemCount(): Int = files.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(files[position])
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imageView: ImageView

        init {
            itemView.setOnClickListener { onItemClick?.invoke(files[adapterPosition]) }
            imageView = itemView.findViewById(R.id.ivImage)
        }

        fun bind(fileInfo: FileInfo) {
            itemView.tvTitle.text = fileInfo.name
            if (fileInfo.isFolder) {
                imageView.setImageDrawable(itemView.context.getDrawable(R.drawable.folder))
                if (fileInfo.isParent) {
                    itemView.tvSubtitle?.text = fileInfo.path
                } else {
                    itemView.tvSubtitle?.text = itemView.context.getString(R.string.folder)
                }

                itemView.ivSelected.visibility = View.GONE
            } else {
                imageView.setImageDrawable(itemView.context.getDrawable(R.drawable.file))

                var typeWithImage = false
                for (ext in FileInfo.Extension.getTypesWithImage()) {
                    if (fileInfo.name.endsWith(ext.extension, true)) {
                        typeWithImage = true
//                        val size = itemView.context.resources.getDimensionPixelSize(R.dimen.user_avatar_size_m)

                        Glide.with(itemView)
                            .load(File(fileInfo.path))
                            .thumbnail(0.1F)
                            .apply(RequestOptions().centerCrop())
                            .into(imageView)
                        break
                    }
                }

                if (!typeWithImage) {
                    for (ext in FileInfo.Extension.values()) {
                        if (fileInfo.name.endsWith(ext.extension, true)) {
                            imageView.setImageDrawable(itemView.context.getDrawable(ext.drawable))
                            break
                        }
                    }
                }

                itemView.tvSubtitle?.text = Formatter.formatFileSize(
                    itemView.context,
                    File(fileInfo.path).length()
                )

                itemView.ivSelected.visibility = if (selectedFilePaths.contains(fileInfo.path)) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }
    }

}