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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_session.view.*
import org.ymessenger.app.R
import org.ymessenger.app.data.remote.entities.Session
import org.ymessenger.app.utils.DateUtils

class SessionViewHolder(itemView: View, private val onItemClick: (Session) -> Unit) :
    RecyclerView.ViewHolder(itemView) {

    private var _session: Session? = null

    init {
        itemView.setOnClickListener {
            _session?.let {
                onItemClick.invoke(it)
            }
        }
    }

    fun bind(session: Session) {
        _session = session

        val context = itemView.context

        itemView.tvDeviceName.text = session.deviceName ?: noInformation(context)
        itemView.tvOSName.text = session.OSName ?: noInformation(context)
        itemView.tvAppName.text = session.appName ?: noInformation(context)
        val dateTime = if (session.lastActivityTime != 0L)
            DateUtils.dateTimeFormat(session.lastActivityTime)
        else
            noInformation(context)
        itemView.tvLastActivityTime.text = context.getString(R.string.last_activity, dateTime)

        itemView.tvCurrentSessionLabel.visibility =
            if (session.isCurrent) View.VISIBLE else View.GONE
    }

    private fun noInformation(context: Context): String {
        return context.getString(R.string.no_info)
    }

    companion object {
        fun create(parent: ViewGroup, onItemClick: (Session) -> Unit): SessionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_session, parent, false)
            return SessionViewHolder(view, onItemClick)
        }
    }

}