package com.eterna.kee.model

import com.tencent.imsdk.v2.V2TIMMessage
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════
//  채팅
// ═══════════════════════════════════════

data class ChatMessage(
    val msgId: String,
    val senderId: String,
    val nickname: String,
    val faceUrl: String,
    val groupId: String,
    val text: String,
    val timestamp: Long,
    val isSelf: Boolean,
    val raw: V2TIMMessage? = null,
) {
    companion object {
        fun from(msg: V2TIMMessage) = ChatMessage(
            msgId = msg.msgID ?: "",
            senderId = msg.sender ?: "",
            nickname = msg.nickName ?: "",
            faceUrl = msg.faceUrl ?: "",
            groupId = msg.groupID ?: "",
            text = when (msg.elemType) {
                V2TIMMessage.V2TIM_ELEM_TYPE_TEXT -> msg.textElem?.text ?: ""
                V2TIMMessage.V2TIM_ELEM_TYPE_IMAGE -> "[이미지]"
                V2TIMMessage.V2TIM_ELEM_TYPE_SOUND -> "[음성]"
                V2TIMMessage.V2TIM_ELEM_TYPE_VIDEO -> "[동영상]"
                else -> ""
            },
            timestamp = msg.timestamp,
            isSelf = msg.isSelf,
            raw = msg,
        )
    }
}

// ═══════════════════════════════════════
//  아고라
// ═══════════════════════════════════════

data class AgoraInfo(
    val id: ULong,
    val groupId: String,
    val ownerDbId: ULong,
    val name: String,
    val description: String,
    val iconUrl: String,
    val type: AgoraType,
    val memberCount: Int,
    val interests: List<String> = emptyList(),
    val channels: List<AgoraChannel> = emptyList(),
)

data class AgoraChannel(
    val id: ULong,
    val agoraId: ULong,
    val groupId: String,
    val name: String,
    val description: String,
    val memberCount: Int = 0,
)

enum class AgoraType { Public, Private }

// ═══════════════════════════════════════
//  알림
// ═══════════════════════════════════════

data class AgoraNotification(
    val type: NotificationType,
    val agoraId: ULong,
    val agoraName: String,
    val senderUserId: String = "",
    val senderNickname: String = "",
    val timestamp: Long,
    val handled: Boolean = false,
) {
    val isActionable get() = type in actionableTypes
    val isPending get() = isActionable && !handled
}

enum class NotificationType {
    AgoraInvite, ChannelInvite, ApplyRequest,
    ApplyApproved, ApplyDeclined,
    Kicked, MasterPromoted, AgoraClosed,
}

private val actionableTypes = setOf(
    NotificationType.AgoraInvite,
    NotificationType.ChannelInvite,
    NotificationType.ApplyRequest,
)

// ═══════════════════════════════════════
//  C2C 메시지 페이로드 (아고라 초대 등)
// ═══════════════════════════════════════

@Serializable
data class AgoraC2CPayload(
    val agoraID: ULong = 0UL,
    val agora_name: String = "",
    val stringKey: String = "",
    val user_name: String = "",
)