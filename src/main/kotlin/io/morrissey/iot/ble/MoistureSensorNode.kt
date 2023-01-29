package io.morrissey.iot.ble

import io.morrissey.iot.ble.MeshHub.Companion.PACKET_SEND_DELAY
import io.morrissey.iot.ble.connections.DataConnection

class MoistureSensor(
    nodeDef: NodeDef,
    nodeId: Int,
    meshHub: MeshHub,
    dataConnection: DataConnection
) : SensorNode(nodeDef, nodeId, meshHub, dataConnection) {
    companion object {
        private val sensorData = mapOf(
            PtBatteryPctReq to "batteryPercent",
            PtBatteryVoltageReq to "batteryVoltage",
            PtMoisturePctReq to "moisturePercent",
            PtMoistureVoltageReq to "moistureVoltage"
        )
    }

    override fun processPacket(packet: Packet) {
        sensorData.keys.singleOrNull { it.response == packet.type }?.let {
            val curData = sensorData[it]!!
            dataConnection.sendData(nodeDef, curData, packet.data.toInt())
        } ?: logger.warn("Unexpected sensor packet received for sensor ${nodeDef.address}: $packet")
    }

    override fun onAwake() {
        super.onAwake()
        sensorData.keys.forEach{
            Thread.sleep(PACKET_SEND_DELAY)
            sendPacket(it)
        }
    }

    override fun equals(other: Any?): Boolean {
        return (other as? MoistureSensor)?.let {
            nodeDef == other.nodeDef
        } ?: false
    }

    override fun hashCode(): Int {
        return nodeDef.hashCode()
    }

    override fun toString(): String {
        return "MoistureSensor($nodeDef, nodeId=$nodeId)"
    }
}

data class ConfigUpdate(
    val nodeDef: NodeDef,
    val packetType: PacketType,
    val value: Int
)
