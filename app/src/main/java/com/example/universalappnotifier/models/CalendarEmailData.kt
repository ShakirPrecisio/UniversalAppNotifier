package com.example.universalappnotifier.models

import android.os.Parcel
import android.os.Parcelable

data class CalendarEmailData(
    var email_id: String = "",
    var color: Int = 0
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(email_id)
        parcel.writeInt(color)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CalendarEmailData> {
        override fun createFromParcel(parcel: Parcel): CalendarEmailData {
            return CalendarEmailData(parcel)
        }

        override fun newArray(size: Int): Array<CalendarEmailData?> {
            return arrayOfNulls(size)
        }
    }
}
