package com.arjun.firechat.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(
    var id: String = "",
    var name: String = "",
    var lastSeen: Long = 0L,
    var online: Boolean = false,
    var image: String = ""
) : Parcelable