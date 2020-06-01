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

import android.content.Context
import org.ymessenger.app.fragments.FilePageFragment

class FilesPagerAdapter(
    val context: Context,
    fragmentManager: androidx.fragment.app.FragmentManager
) :
    androidx.fragment.app.FragmentPagerAdapter(fragmentManager) {

    private val fragments: ArrayList<FilePageFragment>

    init {
        val documentsFragment = FilePageFragment()
        val imagesFragment = FilePageFragment()
        val videoFragment = FilePageFragment()

        documentsFragment.setType(FilePageFragment.Type.DOCS)
        imagesFragment.setType(FilePageFragment.Type.IMAGES)
        videoFragment.setType(FilePageFragment.Type.VIDEO)

        fragments = arrayListOf(documentsFragment, imagesFragment, videoFragment)
    }

    override fun getItem(position: Int): androidx.fragment.app.Fragment {
        return fragments[position]
    }

    override fun getCount(): Int = fragments.size

    override fun getPageTitle(position: Int): CharSequence? {
        return fragments[position].getTitle(context)
    }
}