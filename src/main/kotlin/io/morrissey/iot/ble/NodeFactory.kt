package io.morrissey.iot.ble

import io.morrissey.iot.ble.NodeStatus.*
import io.morrissey.iot.ble.connections.aws.AwsIotCoreConnection
import io.morrissey.iot.ble.connections.aws.IotEndpointClient

class NodeFactory(private val meshHub: MeshHub, dataConnectionSettings: Map<String, Any>) {
    private val awsIotConnection = AwsIotCoreConnection(dataConnectionSettings)

    fun create(packet: Packet, nodeId: Int): SensorNode {
        val address = packet.data.littleEndianToText().trim()
        logger.info("Creating node for address: \"$address\"")
        return MoistureSensor(
            knownNodeDefs.singleOrNull { it.address == address } ?: NodeDef(address, UNKNOWN),
            nodeId,
            meshHub,
            awsIotConnection
        )
    }
}

data class NodeDef(val address: String, val status: NodeStatus)

enum class NodeStatus { DEPLOYED, TESTING, UNKNOWN }


