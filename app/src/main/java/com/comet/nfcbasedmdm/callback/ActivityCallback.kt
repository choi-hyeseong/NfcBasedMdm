package com.comet.nfcbasedmdm.callback

import androidx.fragment.app.Fragment

interface ActivityCallback {

    fun switch(frag : Fragment, replace: Boolean)

    fun disableCamera(status : Boolean)
}