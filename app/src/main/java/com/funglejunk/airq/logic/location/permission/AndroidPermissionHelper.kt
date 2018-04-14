package com.funglejunk.airq.logic.location.permission

import android.Manifest
import android.app.Activity
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.single.PermissionListener
import timber.log.Timber

class AndroidPermissionHelper(private val activity: Activity,
                              private val permissionListener: PermissionListener) :
        PermissionHelperInterface {

    override fun check() {
        Timber.d("check location")
        Dexter.withActivity(activity)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(permissionListener)
                .check()
    }

}