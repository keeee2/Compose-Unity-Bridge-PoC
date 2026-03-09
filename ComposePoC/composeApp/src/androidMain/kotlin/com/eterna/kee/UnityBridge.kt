package com.eterna.kee


import android.app.Activity
import com.unity3d.player.UnityPlayer

object UnityBridge {
    private var unityPlayer: UnityPlayer? = null

    fun init(activity: Activity) {
        if (unityPlayer == null) {
            unityPlayer = UnityPlayer(activity)
        }
    }

    fun getPlayer(): UnityPlayer? = unityPlayer

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