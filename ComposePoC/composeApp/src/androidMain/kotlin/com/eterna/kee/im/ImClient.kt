package com.eterna.kee.im

import android.content.Context
import android.util.Log
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMSDKConfig
import com.tencent.imsdk.v2.V2TIMSDKListener
import com.tencent.imsdk.v2.V2TIMUserFullInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tencent IM SDK 진입점.
 * init → login → (사용) → logout → destroy 순서로 호출합니다.
 */
class ImClient(private val context: Context) {

    private val _state = MutableStateFlow(ImState())
    val state = _state.asStateFlow()

    private var initialized = false

    fun init(appId: Int): Boolean {
        if (initialized) return true

        val config = V2TIMSDKConfig().apply {
            logLevel = V2TIMSDKConfig.V2TIM_LOG_WARN
        }

        initialized = V2TIMManager.getInstance().initSDK(context, appId, config, listener)
        Log.i(TAG, if (initialized) "SDK init OK" else "SDK init FAILED")
        return initialized
    }

    suspend fun login(userId: String, userSig: String) {
        check(initialized) { "Call init() first" }

        imAwaitVoid { V2TIMManager.getInstance().login(userId, userSig, it) }
        _state.value = _state.value.copy(userId = userId, loggedIn = true)
        Log.i(TAG, "Login OK: $userId")
    }

    suspend fun logout() {
        if (!_state.value.loggedIn) return
        imAwaitVoid { V2TIMManager.getInstance().logout(it) }
        _state.value = _state.value.copy(loggedIn = false)
        Log.i(TAG, "Logout OK")
    }

    suspend fun setNickname(name: String) {
        val info = V2TIMUserFullInfo().apply { setNickname(name) }
        imAwaitVoid { V2TIMManager.getInstance().setSelfInfo(info, it) }
    }

    fun destroy() {
        if (!initialized) return
        V2TIMManager.getInstance().unInitSDK()
        initialized = false
        _state.value = ImState()
    }

    private val listener = object : V2TIMSDKListener() {
        override fun onConnecting() { updateConnection(Connection.Connecting) }
        override fun onConnectSuccess() { updateConnection(Connection.Connected) }
        override fun onConnectFailed(code: Int, error: String?) { updateConnection(Connection.Disconnected) }
        override fun onKickedOffline() {
            _state.value = _state.value.copy(loggedIn = false)
            Log.w(TAG, "Kicked offline")
        }
        override fun onUserSigExpired() {
            _state.value = _state.value.copy(loggedIn = false)
            Log.w(TAG, "UserSig expired")
        }
    }

    private fun updateConnection(conn: Connection) {
        _state.value = _state.value.copy(connection = conn)
    }

    companion object { private const val TAG = "ImClient" }
}

data class ImState(
    val userId: String? = null,
    val loggedIn: Boolean = false,
    val connection: Connection = Connection.Disconnected,
)

enum class Connection { Disconnected, Connecting, Connected }