package com.example.universalappnotifier.ui.dashboard

import android.content.Context
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject


object MSGraphRequestWrapper {
    private val TAG = MSGraphRequestWrapper::class.java.simpleName

    // See: https://docs.microsoft.com/en-us/graph/deployments#microsoft-graph-and-graph-explorer-service-root-endpoints
    const val MS_GRAPH_ROOT_ENDPOINT = "https://graph.microsoft.com/"

    /**
     * Use Volley to make an HTTP request with
     * 1) a given MSGraph resource URL
     * 2) an access token
     * to obtain MSGraph data.
     */
    fun callGraphAPIUsingVolley(
        context: Context,
        graphResourceUrl: String,
        accessToken: String,
        responseListener: Response.Listener<JSONObject>,
        errorListener: Response.ErrorListener
    ) {
        Log.d(TAG, "Starting volley request to graph")

        /* Make sure we have a token to send to graph */
        if (accessToken == null || accessToken.length == 0) {
            return
        }
        val queue = Volley.newRequestQueue(context)
        val parameters = JSONObject()
        try {
            parameters.put("key", "value")
        } catch (e: Exception) {
            Log.d(TAG, "Failed to put parameters: $e")
        }
        val request: JsonObjectRequest = object : JsonObjectRequest(
            Method.GET, graphResourceUrl,
            parameters, responseListener, errorListener
        ) {
            override fun getHeaders(): Map<String, String> {
                val headers: MutableMap<String, String> = HashMap()
                headers["Authorization"] = "Bearer $accessToken"
                Log.d(TAG, "getHeaders: $accessToken")
                return headers
            }
        }
        Log.d(TAG, "Adding HTTP GET to Queue, Request: $request")
        request.setRetryPolicy(
            DefaultRetryPolicy(
                3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        )
        queue.add(request)
    }
}
