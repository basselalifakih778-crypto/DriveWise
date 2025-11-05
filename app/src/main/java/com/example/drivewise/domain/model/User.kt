package com.example.drivewise.domain.model

import android.provider.ContactsContract.CommonDataKinds.Email

data class User(

    val id: String,
    val fullName:String,
    val email: String,
    val role: Role,

)
