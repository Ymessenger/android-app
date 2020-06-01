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

package org.ymessenger.app.utils

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import org.ymessenger.app.R
import org.ymessenger.app.data.local.db.entities.User

@BindingAdapter("isGone")
fun visibleGone(view: View, isGone: Boolean) {
    view.visibility = if (isGone) View.GONE else View.VISIBLE
}

@BindingAdapter("isVisible")
fun isVisible(view: View, visible: Boolean) {
    view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("hideIfEmpty")
fun hideIfEmpty(view: View, text: String?) {
    view.visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
}

@BindingAdapter("imageUrl")
fun loadImage(imageView: ImageView, url: String?) {
    if (url != null) {
        Glide.with(imageView)
            .load(url)
            .thumbnail(0.1f)
            .into(imageView)
        imageView.visibility = View.VISIBLE
    } else {
        imageView.visibility = View.INVISIBLE
    }
}

@BindingAdapter("favouriteIcon")
fun setFavouriteIcon(imageButton: ImageButton, isFavorite: Boolean) {
    imageButton.setImageResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border)
}

@BindingAdapter("userRole")
fun setUserRole(textView: TextView, userRole: Int) {
    if (userRole > 0) {
        val textResource = when (userRole) {
            3 -> R.string.creator
            2 -> R.string.admin
            1 -> R.string.moderator
            else -> return
        }
        textView.setText(textResource)
        textView.visibility = View.VISIBLE
    } else {
        textView.visibility = View.GONE
    }

}

@BindingAdapter("lastOnline")
fun setLastOnline(textView: TextView, user: User?) {
    val context = textView.context

    textView.text = if (user?.isOnline() == true) {
        context.getString(R.string.online)
    } else {
        user?.online?.let {
            context.getString(
                R.string.last_online,
                android.text.format.DateUtils.getRelativeDateTimeString(
                    context,
                    user.online!! * 1000,
                    0,
                    android.text.format.DateUtils.WEEK_IN_MILLIS,
                    android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
                )
            )
        } ?: context.getString(R.string.last_online_no_information)
    }

    textView.setTextColor(
        ContextCompat.getColor(
            context,
            if (user?.isOnline() == true) R.color.colorPrimary else R.color.colorGray
        )
    )
}

@BindingAdapter("app:isSignRequired", "app:isVerified", "app:isLoading")
fun pollVoteSignedStatus(
    imageView: ImageView,
    isSignRequired: Boolean,
    isVerified: Boolean,
    isLoading: Boolean
) {
    if (isSignRequired && !isLoading) {
        imageView.visibility = View.VISIBLE
        val context = imageView.context
        if (isVerified) {
            imageView.setImageResource(R.drawable.ic_verified)
            imageView.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    R.color.colorMaterialGreen500
                )
            )
        } else {
            imageView.setImageResource(R.drawable.ic_error)
            imageView.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorMaterialRed500))
        }
    } else {
        imageView.visibility = View.GONE
    }
}

@BindingAdapter("requestFocus")
fun requestFocus(view: View, requestFocus: Boolean) {
    if (requestFocus) {
        view.requestFocus()
    }
}