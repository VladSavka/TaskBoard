package com.example.taskboard

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application entry point; enables Hilt dependency injection for the whole app. */
@HiltAndroidApp
class TaskBoardApplication : Application()
