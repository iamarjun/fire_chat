package com.arjun.firechat

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


abstract class BaseFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

//    fun getAllUsers(): DatabaseReference {
////        return mDatabase.getReference("users")
//    }
//
//    fun getUser(uId: String): DatabaseReference {
//        return getAllUsers().child(uId)
//    }

}