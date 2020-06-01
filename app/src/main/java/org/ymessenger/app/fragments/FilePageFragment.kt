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

package org.ymessenger.app.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_files_page.view.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.FilesAdapter
import org.ymessenger.app.models.FileInfo
import org.ymessenger.app.singleton.Data

open class FilePageFragment : Fragment() {

    private lateinit var type: Type
    private var titleResource = 0
    private val filesAdapter = FilesAdapter()

    companion object {
        private const val SPAN = 4
    }

    enum class Type {
        DOCS,
        IMAGES,
        VIDEO
    }

    fun getTitle(context: Context): String = context.getString(titleResource)

    fun setType(type: Type) {
        this.type = type

        titleResource = when (type) {
            Type.DOCS -> {
                R.string.documents
            }

            Type.IMAGES -> {
                R.string.images
            }

            Type.VIDEO -> {
                R.string.video
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_files_page, container, false)

        val gridLayoutManager = androidx.recyclerview.widget.GridLayoutManager(activity, SPAN)
        view.rvFiles.layoutManager = gridLayoutManager
        view.rvFiles.adapter = filesAdapter
        filesAdapter.setDisplayFormat(FilesAdapter.DisplayFormat.GRID)
        filesAdapter.setData(getFilteredData())

        filesAdapter.onItemClick = {
            Toast.makeText(activity, "View document ${it.name}", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun getFilteredData(): List<FileInfo> {
        val files = arrayListOf<FileInfo>()

        for (fileInfo in Data.files) {
            when (type) {
                Type.IMAGES -> {
                    for (ext in FileInfo.Extension.getTypesWithImage()) {
                        if (fileInfo.name.endsWith(ext.extension, true)) {
                            files.add(fileInfo)
                            break
                        }
                    }
                }

                Type.VIDEO -> {
                    for (ext in FileInfo.Extension.getVideoTypes()) {
                        if (fileInfo.name.endsWith(ext.extension, true)) {
                            files.add(fileInfo)
                            break
                        }
                    }
                }

                else -> {
                    var found = false

                    for (ext in FileInfo.Extension.getTypesWithImage()) {
                        if (fileInfo.name.endsWith(ext.extension, true)) {
                            found = true
                            break
                        }
                    }

                    for (ext in FileInfo.Extension.getVideoTypes()) {
                        if (fileInfo.name.endsWith(ext.extension, true)) {
                            found = true
                            break
                        }
                    }

                    if (!found)
                        files.add(fileInfo)
                }
            }
        }

        return files
    }
}