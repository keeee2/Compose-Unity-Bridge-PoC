package com.eterna.kee.im

import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMValueCallback
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ImException(val code: Int, val desc: String) : Exception("IM $code: $desc")

/** V2TIMValueCallback → suspend. 결과가 있는 SDK 호출용. */
suspend fun <T> imAwait(block: (V2TIMValueCallback<T>) -> Unit): T =
    suspendCancellableCoroutine { cont ->
        block(object : V2TIMValueCallback<T> {
            override fun onSuccess(result: T) { cont.safeResume(result) }
            override fun onError(code: Int, desc: String?) {
                cont.safeResumeWith(ImException(code, desc ?: ""))
            }
        })
    }

/** V2TIMCallback → suspend. 결과 없는 SDK 호출용. */
suspend fun imAwaitVoid(block: (V2TIMCallback) -> Unit): Unit =
    suspendCancellableCoroutine { cont ->
        block(object : V2TIMCallback {
            override fun onSuccess() { cont.safeResume(Unit) }
            override fun onError(code: Int, desc: String?) {
                cont.safeResumeWith(ImException(code, desc ?: ""))
            }
        })
    }

private fun <T> CancellableContinuation<T>.safeResume(value: T) {
    if (isActive) resume(value)
}

private fun <T> CancellableContinuation<T>.safeResumeWith(ex: Throwable) {
    if (isActive) resumeWithException(ex)
}