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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_add_attachment.view.*
import org.ymessenger.app.R
import org.ymessenger.app.data.ConversationType

class AddAttachmentFragment : Fragment() {

    var onClickListener: ((View) -> Unit)? = null
    private var conversationType: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationType = arguments!!.getInt(ARG_CONVERSATION_TYPE, ConversationType.DIALOG)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_attachment, container, false)

        view.btnGallery.setOnClickListener(onClickListener)
        view.btnDocuments.setOnClickListener(onClickListener)
        view.btnPoll.setOnClickListener(onClickListener)

        view.btnPoll.visibility =
            if (conversationType == ConversationType.DIALOG) View.GONE else View.VISIBLE

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.setTitle(R.string.add_files)
    }

    companion object {
        const val ARG_CONVERSATION_TYPE = "CONVERSATION_TYPE"
        const val ARG_HAS_POLL = "HAS_POLL"
        const val ARG_AVAILABLE_ATTACHMENTS = "AVAILABLE_ATTACHMENTS"
    }
}