package com.example

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(application = OutpassApplication::class, sdk = [34])
class MainActivityLaunchTest {

    @Test
    fun testActivityLaunchAndResume() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.create().start().resume().visible()
        ShadowLooper.idleMainLooper()
        val activity = controller.get()
        org.junit.Assert.assertNotNull(activity)
        org.junit.Assert.assertFalse(activity.isFinishing)
    }

    @Test
    fun testLaunchesDirectlyToLoginScreen() {
        val repo = com.example.data.repository.OutpassRepository.getInstance()
        org.junit.Assert.assertNull("currentUser should be null on initial app launch", repo.currentUser.value)
    }
}
