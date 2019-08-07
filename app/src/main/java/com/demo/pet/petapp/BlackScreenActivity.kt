package com.demo.pet.petapp

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class BlackScreenActivity : AppCompatActivity() {

    companion object {
        val activeActivitySet = HashSet<Activity>()

        fun finishAll() {
            activeActivitySet.forEach { it.finish() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activeActivitySet.add(this)

        setContentView(R.layout.black_screen)
    }

    override fun onDestroy() {

        activeActivitySet.remove(this)

        super.onDestroy()
    }

}
