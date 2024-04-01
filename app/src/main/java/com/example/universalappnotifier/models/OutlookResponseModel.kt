package com.example.universalappnotifier.models

import com.google.gson.annotations.SerializedName

data class OutlookResponseModel(
    @SerializedName("@odata.context") val odataContext: String,
    val value: List<Value>
)