package com.arjun.firechat

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


abstract class BaseFragment : Fragment() {

    var mAuth: FirebaseAuth? = null
    private var mDatabase: FirebaseDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()
    }

    fun getUser(uId: String): DatabaseReference? {
        return mDatabase?.reference?.child("Users")?.child(uId)
    }

}