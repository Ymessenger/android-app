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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.item_image.view.*
import org.ymessenger.app.R
import java.io.File

class ImagesAdapter :
    androidx.recyclerview.widget.RecyclerView.Adapter<ImagesAdapter.ImageHolder>() {

    companion object {
        private const val MAX_IMAGES_SELECTED_COUNT = 10
    }

    private val imagePaths = arrayListOf<String>()
    private val selectedImagePaths = arrayListOf<String>()
    var onItemClick: ((String) -> Unit)? = null
    var maximumSelected: (() -> Unit)? = null

    private var maxSelectionAmount = MAX_IMAGES_SELECTED_COUNT

    fun setImages(imagePaths: List<String>) {
        this.imagePaths.clear()
        this.imagePaths.addAll(imagePaths)
        this.selectedImagePaths.clear()

        notifyDataSetChanged()
    }

    fun setMaxSelectionAmount(max: Int) {
        maxSelectionAmount = max
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ImageHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)

        return ImageHolder(view)
    }

    override fun getItemCount(): Int = imagePaths.size

    override fun onBindViewHolder(holder: ImageHolder, position: Int) {
        holder.bind(imagePaths[position])
    }

    private fun clickImage(imagePath: String) {
        if (isSelected(imagePath)) {
            selectedImagePaths.remove(imagePath)
        } else {
            if (selectedImagePaths.size < maxSelectionAmount) {
                selectedImagePaths.add(imagePath)
            } else {
                maximumSelected?.invoke()
            }
        }
    }

    private fun isSelected(imagePath: String): Boolean = selectedImagePaths.contains(imagePath)

    fun getSelectedImages(): ArrayList<String> = selectedImagePaths

    inner class ImageHolder(itemView: View) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

        init {
//            (itemView as ViewGroup).layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
            itemView.setOnClickListener {
                clickImage(imagePaths[adapterPosition])
                onItemClick?.invoke(imagePaths[adapterPosition])

                setSelection(imagePaths[adapterPosition])

//                notifyItemChanged(adapterPosition)
            }
        }

        fun bind(imagePath: String) {
            Glide.with(itemView)
                .load(File(imagePath))
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions().centerCrop())
                .thumbnail(0.1F)
                .into(itemView.ivImage)

            setSelection(imagePath)
        }

        private fun setSelection(imagePath: String) {
            if (isSelected(imagePath)) {
                itemView.ivSelected.visibility = View.VISIBLE
                val gap = itemView.context.resources.getDimensionPixelSize(R.dimen.gapXSmall)
                itemView.ivImage.setPadding(gap, gap, gap, gap)
            } else {
                itemView.ivSelected.visibility = View.INVISIBLE
                itemView.ivImage.setPadding(0, 0, 0, 0)
            }
        }

    }

}