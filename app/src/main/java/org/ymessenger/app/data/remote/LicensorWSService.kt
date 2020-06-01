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

package org.ymessenger.app.data.remote

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.collection.LongSparseArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import okhttp3.*
import okio.ByteString
import org.ymessenger.app.data.remote.licensorRequests.GetNodes
import org.ymessenger.app.data.remote.licensorResponses.Nodes
import org.ymessenger.app.data.remote.responses.LicensorResultResponse

class LicensorWSService {

    private lateinit var webSocket: WebSocket
    private val gson = Gson()
    private val connectionStatus = MutableLiveData<Boolean>()
    private val responseCallbackArray = LongSparseArray<ResponseCallback<LicensorResponse>>()
    private val timeoutCallbackArray = LongSparseArray<WebSocketTimeoutCallback>()

    private val client = OkHttpClient()
    private lateinit var request: Request

    /**
     * Flag to determine if it's manual reconnect (not lost internet connection)
     */
    private var reconnect = false

    /**
     * This callback calls after successful reconnect to web socket
     */
    var onReconnected: (() -> Unit)? = null


    private val wsListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            Log.d(TAG, "onOpen")
            setConnectionStatus(true)
            if (reconnect) {
                onReconnected?.invoke()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            Log.e(TAG, "onFailure. ${t.message}")
            setConnectionStatus(false)
            reconnect()
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
            Log.d(TAG, "onClosing, code - $code, reason - $reason")
            setConnectionStatus(false)
            reconnect()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            Log.d(TAG, "<<< onMessage received: $text")
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
            val data = bytes.utf8()
//                Log.d(TAG, "<<< Received data: $data")
            handleMessage(data)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            Log.d(TAG, "onClosed")
            setConnectionStatus(false)
        }
    }

    fun connect() {
        request = Request.Builder().url(UrlGenerator.getLicensorWSUrl()).build()
        connectToWebSocket()
    }

    private fun connectToWebSocket() {
        Log.d(TAG, "Connection to licensor... ${request.url}")
        setConnectionStatus(false)
        webSocket = client.newWebSocket(request, wsListener)
    }

    /**
     * Trying to reconnect to web socket with interval 5 seconds
     */
    private fun reconnect() {
        reconnect = true
        val delay = 5L * 1000
        Log.d(TAG, "Reconnect in $delay ms")

        Handler(Looper.getMainLooper()).postDelayed({
            connectToWebSocket()
        }, delay)
    }

    private fun setConnectionStatus(status: Boolean) {
        connectionStatus.postValue(status)
    }

    fun getConnectionStatus(): LiveData<Boolean> = connectionStatus

    /**
     * This method determines what type of communication object is come and passes it
     * to the required handler (response, notice)
     *
     * @param data Raw string text of received message
     */
    private fun handleMessage(data: String) {
        try {
            val communicationObject = gson.fromJson(data, CommunicationObject::class.java)
            when (communicationObject.type) {
                CommunicationObject.TYPE_LICENSOR_RESPONSE -> processResponse(data)
                CommunicationObject.TYPE_LICENSOR_REQUEST -> Log.w(
                    TAG,
                    "This is income message (response), but type is 'request'"
                )
                else -> Log.e(TAG, "Unknown type of communication object")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Determines response type and converts response to object
     *
     * @param data Raw message text
     */
    private fun processResponse(data: String) {
        try {
            // First we need to figure it out what type of response we got
            val generalResponse = gson.fromJson(data, LicensorResponse::class.java)
            logResponse(generalResponse.getResponseTypeName(), data)

            var response: LicensorResponse? = null
            when (generalResponse.responseType) {

                LicensorResponse.ResponseType.NODES -> {
                    response = gson.fromJson(data, Nodes::class.java)
                    Log.d(TAG, "Nodes size ${response.nodes.size}")
                }

                LicensorResponse.ResponseType.RESULT_RESPONSE -> {
                    response = gson.fromJson(data, LicensorResultResponse::class.java)
                    Log.e(TAG, "Error: ${response.errorMessage}")
                }

                else -> Log.e(TAG, "There is no handler for this response type")
            }

            response?.let {
                returnResponse(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Logging of web socket actions such as send request and receive response
     *
     * @param direction Direction of action (request, response or notice)
     * @param type Type of action
     * @param data Raw data
     */
    private fun logAction(direction: String, type: String, data: String) {
        Log.d(TAG, "$direction [$type] :: $data")
    }

    /**
     * Logging of web socket requests
     *
     * @param type Type of request
     * @param data Raw data
     */
    private fun logRequest(type: String, data: String) {
        logAction(DIRECTION_REQUEST, type, data)
    }

    /**
     * Logging of web socket responses
     *
     * @param type Type of response
     * @param data Raw data
     */
    private fun logResponse(type: String, data: String) {
        logAction(DIRECTION_RESPONSE, type, data)
    }

    /**
     * This method founds callback by request id and passes response to it
     *
     * @param response Response object
     */
    private fun returnResponse(response: LicensorResponse) {
        // Find callback from pull of callbacks by request id
        val responseCallback = responseCallbackArray[response.requestId]
        if (responseCallback != null) {
            Handler(Looper.getMainLooper()).post {
                if (response is LicensorResultResponse && response.errorMessage != null) {
                    responseCallback.onError(response)
                } else {
                    responseCallback.onResponse(response)
                }
            }

            unregisterCallback(response.requestId)
        } else {
            Log.e(TAG, "Callback is not found")
        }
    }


    // ************************* METHODS FOR LICENSOR WEB SOCKET SERVER ****************************

    fun getNodes(getNodes: GetNodes, responseCallback: ResponseCallback<Nodes>) {
        sendRequest(getNodes, responseCallback)
    }

    /**
     * This method adds request callback to pull of callbacks
     *
     * @param request Request object
     * @param responseCallback Callback object
     */
    private fun registerCallback(request: LicensorRequest, responseCallback: ResponseCallback<*>) {
        @Suppress("UNCHECKED_CAST")
        responseCallbackArray.put(
            request.requestId,
            responseCallback as ResponseCallback<LicensorResponse>
        )

        // Register timeout callback
        timeoutCallbackArray.put(request.requestId, object : WebSocketTimeoutCallback() {
            override fun onTimeout() {
                Log.e(
                    TAG,
                    "--- [${request.requestTypeName()}] :: Timeout ${Config.WEB_SOCKET_REQUEST_TIMEOUT / 1000} sec (requestId ${request.requestId})"
                )
                returnResponse(getTimeoutResponse(request.requestId))
            }
        })
    }

    /**
     * Removes response callback from pull of callbacks
     *
     * @param requestId Identifier of request
     */
    private fun unregisterCallback(requestId: Long) {
        responseCallbackArray.remove(requestId)

        timeoutCallbackArray[requestId]?.cancel()
        timeoutCallbackArray.remove(requestId)
    }

    private fun getTimeoutResponse(requestId: Long): LicensorResultResponse {
        val timeoutResponse = LicensorResultResponse("WebSocket Timeout")
        timeoutResponse.requestId = requestId

        return timeoutResponse
    }

    /**
     * Sends request to web socket
     *
     * @param request Request object
     * @param responseCallback Callback object
     */
    private fun sendRequest(request: LicensorRequest, responseCallback: ResponseCallback<*>) {
        registerCallback(request, responseCallback)
        val data = gson.toJson(request)
        webSocket.send(data)
        logRequest(request.requestTypeName(), data)
    }

    interface ResponseCallback<in T : LicensorResponse> {
        fun onResponse(response: T)
        fun onError(error: LicensorResultResponse)
    }

    companion object {
        private const val TAG = "LicensorWSService"
        private const val DIRECTION_REQUEST = "-->"
        private const val DIRECTION_RESPONSE = "<--"
    }
}