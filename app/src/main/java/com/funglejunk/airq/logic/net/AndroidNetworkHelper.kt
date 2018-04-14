package com.funglejunk.airq.logic.net

import android.content.Context
import android.net.ConnectivityManager


class AndroidNetworkHelper(private val context: Context) : NetworkHelper {

    override fun networkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }

}