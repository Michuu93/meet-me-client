package com.meetme.meetmeclient

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.meetme.meetmeclient.profile.User
import kotlinx.android.synthetic.main.profile_info.view.*


class ProfileInfoAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

    override fun getInfoContents(marker: Marker?): View {
        val layout = (context as Activity).layoutInflater.inflate(R.layout.profile_info, null)
        layout.layoutParams = ViewGroup.LayoutParams(800, 400)
        val user: User? = marker?.tag as User?

        layout.usernameTextView.text = user?.userName
        layout.descriptionTextView.text = user?.userDescription
        layout.genderTextView.text = user?.gender

        return layout
    }

    override fun getInfoWindow(p0: Marker?): View? {
        return null
    }

}