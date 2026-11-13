package com.example.gastify.data.model

import com.google.firebase.Timestamp

data class Workspace(
    val id: String = "",
    val name: String = "",
    val createdAt: Timestamp? = null
)
