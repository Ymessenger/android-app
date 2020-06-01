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

package org.ymessenger.app.fragments.intro

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.intro_item.view.*
import org.ymessenger.app.R
import org.ymessenger.app.models.IntroPageModel

class IntroPageFragment : Fragment() {

    private lateinit var pageModel: IntroPageModel

    companion object {
        fun newInstance(introPageModel: IntroPageModel): IntroPageFragment {
            val pageFragment = IntroPageFragment()

            pageFragment.pageModel = introPageModel

            return pageFragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.intro_item, container, false)

        if (pageModel.image == null) {
            view.imageHolderLayout.visibility = View.GONE
        } else {
            view.imageView.setImageDrawable(pageModel.image)
        }
        view.tvTitle.text = pageModel.title
        view.tvDescription.text = pageModel.description

        view.tvDescription.movementMethod = ScrollingMovementMethod()

        return view
    }

}