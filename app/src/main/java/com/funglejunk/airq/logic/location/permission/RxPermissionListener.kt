package com.funglejunk.airq.logic.location.permission

import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber

class RxPermissionListener : PermissionListener {

    private val subject = BehaviorSubject.create<Boolean>()

    fun listen(): Observable<Boolean> {
        return subject.hide()
    }

    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
        Timber.d("permission granted")
        subject.onNext(true)
    }

    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
        Timber.w("need to rationale not covered ...")
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
        Timber.w("permission denied")
        subject.onNext(false)
    }

}