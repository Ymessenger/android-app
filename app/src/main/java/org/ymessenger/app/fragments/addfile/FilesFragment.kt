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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_files.*
import kotlinx.android.synthetic.main.fragment_files.view.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.FilesAdapter
import org.ymessenger.app.fragments.BaseFragment
import org.ymessenger.app.models.FileInfo
import java.io.File

class FilesFragment : BaseFragment() {

    private var currentFolder = Environment.getExternalStorageDirectory()
    private var filesAdapter = FilesAdapter()

    private val selectedFilePaths = linkedSetOf<String>()

    private var availableAttachments = 0

    companion object {
        const val SELECTED_FILE_PATHS = "SELECTED_FILE_PATHS"
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
        val view = inflater.inflate(R.layout.fragment_files, container, false)

        val linearLayoutManager = LinearLayoutManager(activity)
        view.rvFiles.layoutManager = linearLayoutManager
        view.rvFiles.addItemDecoration(
            DividerItemDecoration(
                activity,
                linearLayoutManager.orientation
            )
        )
        view.rvFiles.adapter = filesAdapter

        filesAdapter.onItemClick = { fileInfo ->
            clickOnFile(fileInfo)
        }

        view.fabSend.setOnClickListener {
            done()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        fill(currentFolder)
    }

    private fun fill(file: File) {
        val dirs = arrayListOf<FileInfo>()

        for (fileItem in file.listFiles()) {
            dirs.add(FileInfo(fileItem))
        }

        dirs.sort()

        if (file != Environment.getExternalStorageDirectory() && file.parentFile != null) {
            dirs.add(
                0,
                FileInfo(file.parentFile, true)
            )
        }

        filesAdapter.setData(dirs)
        rvFiles.scrollToPosition(0)
    }

    private fun clickOnFile(fileInfo: FileInfo) {
        if (fileInfo.isFolder) {
            currentFolder = File(fileInfo.path)
            fill(currentFolder)
        } else {
            selectFile(fileInfo)
        }
    }

    private fun selectFile(fileInfo: FileInfo) {
        if (selectedFilePaths.contains(fileInfo.path)) {
            selectedFilePaths.remove(fileInfo.path)
        } else {
            if (selectedFilePaths.size >= availableAttachments) {
                showToast(R.string.you_have_added_maximum_amount_of_attachments)
                return
            } else {
                selectedFilePaths.add(fileInfo.path)
            }
        }

        updateUi()

        filesAdapter.setSelectedFiles(selectedFilePaths)
    }

    private fun updateUi() {
        if (selectedFilePaths.isEmpty()) {
            (activity as AppCompatActivity).supportActionBar?.title =
                getString(R.string.add_files)
            fabSend.hide()
        } else {
            (activity as AppCompatActivity).supportActionBar?.title =
                getString(R.string.selected_placeholder, selectedFilePaths.size)
            fabSend.show()
        }
    }

    private fun done() {
        val intent = Intent()

        if (selectedFilePaths.isEmpty()) {
            activity?.setResult(Activity.RESULT_CANCELED)
        } else {
            intent.putStringArrayListExtra(
                SELECTED_FILE_PATHS,
                ArrayList<String>(selectedFilePaths)
            )
            activity?.setResult(Activity.RESULT_OK, intent)
        }

        activity?.finish()
    }
}