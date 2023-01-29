package io.morrissey.iot.ble

import io.morrissey.iot.ble.connections.DataConnection

const val HUB_NODE_ID: Byte = 0
const val PROVISIONING_NODE_ID: Byte = 1

open class SensorNode(
    val nodeDef: NodeDef,
    val nodeId: Int = 1,
    private val meshHub: MeshHub,
    protected val dataConnection: DataConnection
) {
    open var awake: Boolean = false
    private var sleepTimeStart: Long? = null

    open fun processPacket(packet: Packet) {

    }

    protected fun sendPacket(packetType: PacketType, bytes: ByteArray = byteArrayOf(0)) {
        meshHub.sendPacket(
            Packet(
                HUB_NODE_ID,
                nodeId.toByte(),
                PACKET_TTL,
                meshHub.nextIdempotencyKey(),
                packetType,
                bytes.size.toByte(),
                bytes
            )
        )
    }

    fun wakeUp() {
        if (!awake) {
            onAwake()
        }
    }

    protected open fun onAwake() {
        awake = true
        sleepTimeStart?.let {
            val sleepTime = System.currentTimeMillis() - it
            logger.info("Node at address ${nodeDef.address} slept for ${sleepTime / 1000} seconds.")
            sleepTimeStart = null
        }
    }

    open fun sleep() {
        sendPacket(PtGoToSleep)
        sleepTimeStart = System.currentTimeMillis()
        awake = false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorNode

        return nodeDef == other.nodeDef
    }

    override fun hashCode(): Int {
        return nodeDef.hashCode()
    }
}



