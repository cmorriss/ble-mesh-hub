package io.morrissey.iot.ble

interface SensorData {
    val reqPacketType: PacketType
    val respPacketType: PacketType
    val valueName: String
}
