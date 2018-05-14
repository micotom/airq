package com.funglejunk.airq.logic

import com.funglejunk.airq.model.Location
import com.funglejunk.airq.view.MainFragmentView

interface MainActivityPresenterInterface {

    fun viewStarted(fragment: MainFragmentView)

    fun viewStopped()

    fun signalUserLocation(location: Location)

}