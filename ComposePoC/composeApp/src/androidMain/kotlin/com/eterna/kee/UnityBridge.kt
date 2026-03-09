package com.eterna.kee

import android.app.Activity
import android.view.View
import com.unity3d.player.IUnityPlayerLifecycleEvents
import com.unity3d.player.UnityPlayer
import com.unity3d.player.UnityPlayerForActivityOrService

object UnityBridge {
    private var unityPlayer: UnityPlayerForActivityOrService? = null

    fun init(activity: Activity) {
        if (unityPlayer == null) {
            unityPlayer = UnityPlayerForActivityOrService(activity, object : IUnityPlayerLifecycleEvents {
                override fun onUnityPlayerUnloaded() {}
                override fun onUnityPlayerQuitted() {}
            })
        }
    }

    fun getView(): View? = unityPlayer?.view

    fun sendMessage(gameObject: String, method: String, message: String) {
        UnityPlayer.UnitySendMessage(gameObject, method, message)
    }

    fun resume() { unityPlayer?.resume() }
    fun pause() { unityPlayer?.pause() }
    fun destroy() {
        unityPlayer?.destroy()
        unityPlayer = null
    }
}