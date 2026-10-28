package com.sarpai.app

import android.app.Application
import com.sarpai.app.data.Repo

class SarpApp : Application() {
    val repo by lazy { Repo(this) }
}
