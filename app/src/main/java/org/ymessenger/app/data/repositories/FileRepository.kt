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

package org.ymessenger.app.data.repositories

import android.os.Environment
import android.util.Log
import androidx.annotation.IntRange
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.ymessenger.app.data.remote.FileApi
import org.ymessenger.app.data.remote.UrlGenerator
import org.ymessenger.app.data.remote.entities.FileInfo
import org.ymessenger.app.interfaces.SimpleResultCallback
import org.ymessenger.app.utils.AppExecutors
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

class FileRepository private constructor(
    private val executors: AppExecutors,
    private val fileApi: FileApi,
    private val externalPath: File
) {

    fun uploadFile(file: File, isDocument: Boolean, callback: UploadFileCallback) {
        uploadFile(file.readBytes(), file.name, isDocument, callback)
    }

    fun uploadFile(
        data: ByteArray,
        fileName: String,
        isDocument: Boolean,
        callback: UploadFileCallback
    ) {
        val requestBody =
            data.toRequestBody("multipart/form-data".toMediaTypeOrNull(), 0, data.size)

        // We can't use this constructor because of non ascii characters
//        val filenameWithoutUTF8 = file.name.replace("[^\\x00-\\x7F]".toRegex(), "?")
//        val body = MultipartBody.Part.createFormData("file", filenameWithoutUTF8, requestBody)

        // We use this headers instead with unsafe non ascii method
        val headers = Headers.Builder()
            .addUnsafeNonAscii(
                "Content-Disposition",
                "form-data; name=\"file\"; filename=\"$fileName\""
            )
            .build()
        val body = MultipartBody.Part.create(headers, requestBody)

        fileApi.uploadFile(body, isDocument)
            .enqueue(object : Callback<org.ymessenger.app.data.remote.responses.File> {
                override fun onFailure(
                    call: Call<org.ymessenger.app.data.remote.responses.File>,
                    t: Throwable
                ) {
                    Log.e(TAG, "Failed to upload image. ${t.message}")
                    callback.error()
                }

                override fun onResponse(
                    call: Call<org.ymessenger.app.data.remote.responses.File>,
                    response: Response<org.ymessenger.app.data.remote.responses.File>
                ) {
                    when {
                        response.isSuccessful -> {
                            Log.d(TAG, "File is uploaded")
                            val remoteFile = response.body()!!
                            callback.uploaded(remoteFile)
                        }
                        response.code() == 413 -> {
                            Log.e(TAG, "File is too large")
                            callback.errorLargeSize()
                        }
                        else -> {
                            Log.e(TAG, "Error while uploading file")
                            val stringBody = response.errorBody()?.string()
                            stringBody?.let {
                                Log.e(TAG, it)
                            }
                            callback.error()
                        }
                    }
                }
            })
    }

    interface UploadFileCallback {
        fun uploaded(file: org.ymessenger.app.data.remote.responses.File)
        fun errorLargeSize()
        fun error()
    }

    fun downloadFile(fileInfo: FileInfo, callback: DownloadFileCallback) {
        fileApi.downloadFile(UrlGenerator.getFileUrl(fileInfo.fileId)!!)
            .enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    callback.error()
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        try {
                            val documentsFolderName = "documents"
                            val documentsPath = File(externalPath, documentsFolderName)
                            if (!documentsPath.exists()) {
                                documentsPath.mkdirs()
                            }
                            val fileBaseName = File(documentsPath, fileInfo.filename)
                            val file = getFileWithAvailableName(fileBaseName, documentsPath)
                            val fos = FileOutputStream(file)
                            fos.write(response.body()?.bytes()) // FIXME: make it more correct
                            fos.flush()
                            fos.close()

                            callback.downloaded(file.absolutePath)
                        } catch (exception: Exception) {
                            exception.printStackTrace()
                            callback.error()
                        }
                    } else {
                        callback.error()
                    }
                }
            })
    }

    /**
     * Generates random alpha-numeric string with given length
     *
     * @param length Length of generated string
     *
     * @return Generated string
     */
    private fun generateRandomId(@IntRange(from = 1) length: Int): String {
        val values = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val sb = StringBuilder()
        repeat(length) {
            sb.append(values[Random.nextInt(values.length)])
        }
        return sb.toString()
    }

    interface DownloadFileCallback {
        fun downloaded(path: String)
        fun error()
    }

    fun clearFromCache(filePath: String) {
        val file = File(filePath)
        file.delete()
    }

    fun copyToDownloads(
        filePath: String,
        originalFilename: String,
        operationCallback: SimpleResultCallback
    ) {
        // Original file
        val file = File(filePath)
        val downloadsPath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val fileWithOriginalName = File(file.parentFile, originalFilename)
        val fileCopyTo = getFileWithAvailableName(fileWithOriginalName, downloadsPath)

        try {
            file.copyTo(fileCopyTo)
            operationCallback.success()
        } catch (e: Exception) {
            e.printStackTrace()
            operationCallback.error()
        }
    }

    /**
     * Returns file with available filename according to directory. Adds postfix like "filename (n).ext"
     * where n - number from 1 to Int.MAX_VALUE
     *
     * @param originalFile original file to get original filename
     * @param directory directory where the file should be stored
     *
     * @return File object with available filename for given filename and directory
     */
    private fun getFileWithAvailableName(originalFile: File, directory: File): File {
        // Copy original file to this one
        var fileCopyTo = File(directory, originalFile.name)

        // Checking if file with the same name already exists
        var count = 0
        while (fileCopyTo.exists()) {
            // should generate new filename
            count++
            val newFileName =
                originalFile.nameWithoutExtension + " ($count)." + originalFile.extension
            fileCopyTo = File(directory, newFileName)
        }

        return fileCopyTo
    }


    // TESTING

    fun writeToFile(bytes: ByteArray, fileName: String) {
        val file = File(externalPath, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }

        val fos = FileOutputStream(file)
        fos.write(bytes)
        fos.close()
    }

    companion object {
        private const val TAG = "FileRepository"
        private var instance: FileRepository? = null

        fun getInstance(executors: AppExecutors, fileApi: FileApi, externalPath: File) =
            instance ?: synchronized(this) {
                instance
                    ?: FileRepository(
                        executors,
                        fileApi,
                        externalPath
                    ).also { instance = it }
            }
    }

}