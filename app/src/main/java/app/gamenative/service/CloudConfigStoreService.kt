package app.gamenative.service

import app.gamenative.steamproto.SteamCloudConfigStore.CCloudConfigStore_Download_Response
import `in`.dragonbra.javasteam.base.PacketClientMsgProtobuf
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.UnifiedService

class CloudConfigStoreService(unifiedMessages: SteamUnifiedMessages) : UnifiedService(unifiedMessages) {
    override val serviceName = "CloudConfigStore"

    override fun handleResponseMsg(methodName: String, packetMsg: PacketClientMsgProtobuf) {
        when (methodName) {
            "Download" -> postResponseMsg(CCloudConfigStore_Download_Response::class.java, packetMsg)
        }
    }

    override fun handleNotificationMsg(methodName: String, packetMsg: PacketClientMsgProtobuf) {}
}
