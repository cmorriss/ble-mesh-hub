package io.morrissey.iot.ble

data class NodeRequest(val nodeId: Int, val packetType: RequestPacketType, val data: ByteArray)
