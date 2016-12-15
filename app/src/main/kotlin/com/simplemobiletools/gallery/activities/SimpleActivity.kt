package com.simplemobiletools.gallery.activities

import android.os.Bundle
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.gallery.helpers.Config

open class SimpleActivity : BaseSimpleActivity() {
    lateinit var config: Config

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = Config.newInstance(applicationContext)
    }
}
