package com.funglejunk.airq.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.funglejunk.airq.R
import com.funglejunk.airq.logic.MainActivityPresenterInterface
import com.funglejunk.airq.logic.MainActivityView
import com.funglejunk.airq.logic.location.permission.RxPermissionListener
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity(), MainActivityView {

    private val presenter: MainActivityPresenterInterface by inject(
            parameters = { mapOf("permissionListener" to RxPermissionListener(),
                    "activity" to this, "context" to this) }
    )

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
