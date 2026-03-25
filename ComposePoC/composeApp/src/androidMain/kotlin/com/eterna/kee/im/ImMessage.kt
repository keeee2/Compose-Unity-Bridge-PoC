package com.eterna.kee.im

import android.util.Log
import com.eterna.kee.model.ChatMessage
import com.tencent.imsdk.v2.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 메시지 전송, 히스토리 조회, 실시간 수신.
 * C2C 커스텀 메시지(아고라 초대 등)도 여기서 감지하여 이벤트로 발행합니다.
 */
class ImMessage {

    // ── 실시간 수신 이벤트 ──

    private val _newMessage = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val newMessage = _newMessage.asSharedFlow()

    /** C2C CustomElem 수신 (아고라 초대 등). raw JSON string으로 전달. */
    private val _c2cCustom = MutableSharedFlow<C2CCustomEvent>(extraBufferCapacity = 16)
    val c2cCustom = _c2cCustom.asSharedFlow()

    // ── 리스너 ──

    private var registered = false

    fun register() {
        if (registered) return
        V2TIMManager.getMessageManager().addAdvancedMsgListener(listener)
        registered = true
    }

    fun unregister() {
        if (!registered) return
        V2TIMManager.getMessageManager().removeAdvancedMsgListener(listener)
        registered = false
    }

    // ── 전송 ──

    suspend fun sendText(groupId: String, text: String): ChatMessage? {
        val msg = V2TIMManager.getMessageManager().createTextMessage(text) ?: return null
        return try {
            val sent = suspendCancellableCoroutine<V2TIMMessage> { cont ->
                V2TIMManager.getMessageManager().sendMessage(
                    msg, null, groupId,
                    V2TIMMessage.V2TIM_PRIORITY_DEFAULT,
                    false, null,
                    object : V2TIMSendCallback<V2TIMMessage> {
                        override fun onSuccess(result: V2TIMMessage) { if (cont.isActive) cont.resume(result) }
                        override fun onError(code: Int, desc: String?) { if (cont.isActive) cont.resumeWithException(ImException(code, desc ?: "")) }
                        override fun onProgress(progress: Int) {}
                    },
                )
            }
            ChatMessage.from(sent)
        } catch (e: ImException) {
            Log.e(TAG, "send fail: ${e.desc}"); null
        }
    }

    // ── 히스토리 ──

    suspend fun history(groupId: String, count: Int = 50): List<ChatMessage> {
        val option = V2TIMMessageListGetOption().apply {
            this.groupID = groupId
            this.count = count
            getType = V2TIMMessageListGetOption.V2TIM_GET_CLOUD_OLDER_MSG
        }
        return try {
            imAwait<List<V2TIMMessage>> {
                V2TIMManager.getMessageManager().getHistoryMessageList(option, it)
            }.map(ChatMessage::from)
        } catch (e: ImException) {
            Log.e(TAG, "history fail: ${e.desc}"); emptyList()
        }
    }

    /** 특정 메시지 이전의 과거 메시지 (페이징). */
    suspend fun historyBefore(groupId: String, lastMsg: V2TIMMessage, count: Int = 50): List<ChatMessage> {
        val option = V2TIMMessageListGetOption().apply {
            this.groupID = groupId
            this.count = count
            this.lastMsg = lastMsg
            getType = V2TIMMessageListGetOption.V2TIM_GET_CLOUD_OLDER_MSG
        }
        return try {
            imAwait<List<V2TIMMessage>> {
                V2TIMManager.getMessageManager().getHistoryMessageList(option, it)
            }.map(ChatMessage::from)
        } catch (e: ImException) {
            Log.e(TAG, "historyBefore fail: ${e.desc}"); emptyList()
        }
    }

    // ── 리스너 ──

    private val listener = object : V2TIMAdvancedMsgListener() {
        override fun onRecvNewMessage(msg: V2TIMMessage) {
            // 그룹 메시지 → 채팅 로그
            if (!msg.groupID.isNullOrEmpty()) {
                _newMessage.tryEmit(ChatMessage.from(msg))
                return
            }

            // C2C + CustomElem → 아고라 초대 등
            if (!msg.userID.isNullOrEmpty()) {
                val data = msg.customElem?.data ?: return
                val json = String(data)
                if (json.isNotBlank()) {
                    _c2cCustom.tryEmit(C2CCustomEvent(msg.sender ?: "", json))
                }
            }
        }
    }

    companion object { private const val TAG = "ImMessage" }
}

data class C2CCustomEvent(val senderId: String, val json: String)