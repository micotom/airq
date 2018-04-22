package com.funglejunk.airq.logic

import com.funglejunk.airq.view.MainActivityView

interface MainActivityPresenterInterface {

    fun viewStarted(activity: MainActivityView)

    fun viewStopped()

}