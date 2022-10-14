package com.example.findnearbyapp

import android.os.Parcel
import android.os.Parcelable

data class MainCardDTO(
    val title: String,
    val description: String,
    val timestamp: String,
    val distance: String,
    val image_url: String,
    val latitude: String,
    val longitude: String,
    val radius: Int
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(timestamp)
        parcel.writeString(distance)
        parcel.writeString(image_url)
        parcel.writeString(latitude)
        parcel.writeString(longitude)
        parcel.writeInt(radius)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MainCardDTO> {
        override fun createFromParcel(parcel: Parcel): MainCardDTO {
            return MainCardDTO(parcel)
        }

        override fun newArray(size: Int): Array<MainCardDTO?> {
            return arrayOfNulls(size)
        }
    }
}