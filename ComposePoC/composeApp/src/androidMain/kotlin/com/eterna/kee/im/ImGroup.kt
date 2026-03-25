package com.eterna.kee.im

import android.util.Log
import com.tencent.imsdk.v2.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * IM 그룹 관리. Join/Quit/Invite + 실시간 이벤트 수신.
 *
 * Android SDK는 V2TIMGroupListener가 이벤트를 메서드별로 나눠주므로
 * 별도 라우팅 로직이 필요 없습니다.
 */
class ImGroup(private val client: ImClient) {

    // ── 이벤트 ──

    private val _memberEnter = MutableSharedFlow<MemberEvent>(extraBufferCapacity = 16)
    val memberEnter = _memberEnter.asSharedFlow()

    private val _memberLeave = MutableSharedFlow<MemberEvent>(extraBufferCapacity = 16)
    val memberLeave = _memberLeave.asSharedFlow()

    private val _memberKicked = MutableSharedFlow<KickEvent>(extraBufferCapacity = 16)
    val memberKicked = _memberKicked.asSharedFlow()

    private val _groupDismissed = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val groupDismissed = _groupDismissed.asSharedFlow()

    private val _groupInfoChanged = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val groupInfoChanged = _groupInfoChanged.asSharedFlow()

    // ── 리스너 관리 ──

    private var registered = false

    fun register() {
        if (registered) return
        V2TIMManager.getInstance().addGroupListener(listener)
        registered = true
    }

    fun unregister() {
        if (!registered) return
        V2TIMManager.getInstance().removeGroupListener(listener)
        registered = false
    }

    // ── API ──

    suspend fun join(groupId: String): Boolean {
        if (groupId.isBlank()) return false
        return try {
            imAwaitVoid { V2TIMManager.getInstance().joinGroup(groupId, "", it) }
            true
        } catch (e: ImException) {
            if (e.code == 10013) true // already member
            else { Log.e(TAG, "join fail: $groupId ${e.desc}"); false }
        }
    }

    suspend fun quit(groupId: String): Boolean = try {
        imAwaitVoid { V2TIMManager.getInstance().quitGroup(groupId, it) }
        true
    } catch (e: ImException) {
        Log.e(TAG, "quit fail: $groupId ${e.desc}"); false
    }

    suspend fun invite(groupId: String, userIds: List<String>): Boolean {
        if (groupId.isBlank() || userIds.isEmpty()) return false
        return try {
            imAwait<List<V2TIMGroupMemberOperationResult>> {
                V2TIMManager.getGroupManager().inviteUserToGroup(groupId, userIds, it)
            }
            true
        } catch (e: ImException) {
            Log.e(TAG, "invite fail: ${e.desc}"); false
        }
    }

    // ── 리스너 ──

    private val listener = object : V2TIMGroupListener() {
        override fun onMemberEnter(groupId: String?, members: MutableList<V2TIMGroupMemberInfo>?) {
            groupId ?: return
            members?.forEach { _memberEnter.tryEmit(MemberEvent(groupId, it.userID ?: "")) }
        }

        override fun onMemberLeave(groupId: String?, member: V2TIMGroupMemberInfo?) {
            if (groupId != null && member != null)
                _memberLeave.tryEmit(MemberEvent(groupId, member.userID ?: ""))
        }

        override fun onMemberKicked(groupId: String?, opUser: V2TIMGroupMemberInfo?,
                                    members: MutableList<V2TIMGroupMemberInfo>?) {
            groupId ?: return
            val ids = members?.mapNotNull { it.userID } ?: return
            _memberKicked.tryEmit(KickEvent(groupId, ids))
        }

        override fun onGroupDismissed(groupId: String?, opUser: V2TIMGroupMemberInfo?) {
            groupId?.let { _groupDismissed.tryEmit(it) }
        }

        override fun onGroupInfoChanged(groupId: String?, infos: MutableList<V2TIMGroupChangeInfo>?) {
            groupId?.let { _groupInfoChanged.tryEmit(it) }
        }
    }

    companion object { private const val TAG = "ImGroup" }
}

data class MemberEvent(val groupId: String, val userId: String)
data class KickEvent(val groupId: String, val userIds: List<String>)