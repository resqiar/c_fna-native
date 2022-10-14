package com.example.findnearbyapp.others

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Event(
    val id: String? = null,
    val title: String,
    val description: String?,
//    val timestamp: String,
//    val distance: String,
//    val image_url: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Int,
    val authorId: String,
    @ServerTimestamp
    val timestamp: Timestamp? = null
)
