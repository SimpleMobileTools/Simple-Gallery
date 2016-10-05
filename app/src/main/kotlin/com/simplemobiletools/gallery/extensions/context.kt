package com.simplemobiletools.gallery.extensions

import android.content.Context
import android.widget.Toast

fun Context.toast(msg: String, duration: Int = Toast.LENGTH_SHORT) = Toast.makeText(this, msg, duration).show()

fun Context.toast(msgId: Int, duration: Int = Toast.LENGTH_SHORT) = Toast.makeText(this, resources.getString(msgId), duration).show()
