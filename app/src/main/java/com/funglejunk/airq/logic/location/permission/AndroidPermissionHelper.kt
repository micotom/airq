package com.funglejunk.airq.logic.location.permission

import android.Manifest
import android.app.Activity
import com.karumi.dexter.Dexter

class AndroidPermissionHelper(private val permissionListener: RxPermissionListener,
                              private val activity: Activity) :
        PermissionHelperInterface {

    override fun check() {
        Dexter.withActivity(activity)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(permissionListener)
                .check()
    }

}