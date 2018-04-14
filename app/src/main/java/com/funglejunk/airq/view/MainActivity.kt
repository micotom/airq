package com.funglejunk.airq.view

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.funglejunk.airq.R
import com.funglejunk.airq.logic.MainActivityPresenter
import com.funglejunk.airq.logic.MainActivityPresenterInterface
import com.funglejunk.airq.logic.MainActivityView
import com.funglejunk.airq.logic.net.AndroidNetworkHelper
import com.funglejunk.airq.logic.location.AndroidGeocoder
import com.funglejunk.airq.logic.location.AndroidLocationProvider
import com.funglejunk.airq.logic.location.permission.AndroidPermissionHelper
import com.funglejunk.airq.logic.location.permission.RxPermissionListener
import com.funglejunk.airq.logic.net.AirNowClient
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), MainActivityView {

    private val presenter: MainActivityPresenterInterface

    init {
        val permissionListener = RxPermissionListener()
        val permissionHelper = AndroidPermissionHelper(this, permissionListener)
        presenter = MainActivityPresenter(this, permissionListener, permissionHelper,
                AndroidNetworkHelper(this), AndroidLocationProvider(this),
                AndroidGeocoder(this), AirNowClient())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        presenter.viewStarted()
    }

    override fun onPause() {
        presenter.viewStopped()
        super.onPause()
    }

    override fun displayResult(text: String) {
        AndroidSchedulers.mainThread().createWorker().schedule {
            textview.text = text
        }
    }

}
