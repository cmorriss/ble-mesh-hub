package io.morrissey.iot.ble

import com.welie.blessed.BluetoothCentralManager
import com.welie.blessed.BluetoothCentralManagerCallback
import com.welie.blessed.BluetoothCommandStatus
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.ConnectionState
import com.welie.blessed.ScanResult
import io.morrissey.iot.ble.MeshState.AWAKE
import io.morrissey.iot.ble.MeshState.SLEEPING
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class MeshHub : BluetoothCentralManagerCallback() {
    companion object {
        const val MAX_MESH_OPERATION_TIME = 45_000L
        const val RESEND_DELAY = 10000L
        const val PACKET_SEND_DELAY = 50L
    }

    private val nodeRequests = NodeRequests(this)
    private val discoveredNodes = mutableMapOf<Int, SensorNode>()
    private val meshConnections = mutableMapOf<String, MeshConnection>()
    private val bleManager: BluetoothCentralManager = BluetoothCentralManager(this)
    private var meshState: MeshState = SLEEPING
    private var timeWokenUp: Long = System.currentTimeMillis()
    private val nodeFactory = NodeFactory(this, loadDataConnectionSettings())
    private var idempotencyCounter = 0
    private val sleepTimer = SleepTimer(MAX_MESH_OPERATION_TIME)

    init {
        logger.info("Starting scan for peripherals...")
        bleManager.scanForPeripherals()
    }

    private fun loadDataConnectionSettings(): Map<String, Any> {
        // load the connection settings for your AWS account here.
        TODO("You must load the AWS connection settings from somewhere and return them here.")
    }

    fun receivePacket(packet: Packet) {
        logger.debug("Received Data Packet: $packet")
        if (packet.type == PtNodeConnected) {
            if (meshState == SLEEPING) {
                wakeUp()
            }
            var node = nodeFactory.create(packet, discoveredNodes.size + 2)
            if (!discoveredNodes.values.contains(node)) {
                logger.info("Adding newly provisioned node to discoveredNodes: $node")
                discoveredNodes[node.nodeId] = node
            } else {
                // So we have an existing node, but our node won't have the same node id. So let's get the real node
                // and use it as it has the correct nodeId.
                node = discoveredNodes.values.single { it == node }
                logger.debug("Received another node connected packet from an existing node. Resending its node id.")
            }

            val nodeIdData = byteArrayOf(*packet.data, node.nodeId.toByte())
            val connectedResponsePacket = Packet(
                HUB_NODE_ID,
                PROVISIONING_NODE_ID,
                PACKET_TTL,
                nextIdempotencyKey(),
                PtNodeConnectedResp,
                nodeIdData.size.toByte(),
                nodeIdData
            )
            logger.debug("Sending connected response packet to address ${node.nodeDef.address}")
            sendPacket(connectedResponsePacket)
            node.wakeUp()
        } else if (packet.source != HUB_NODE_ID) {
            val node = discoveredNodes[packet.source.toInt()]
            if (node == null) {
                logger.warn("Received packet of type ${packet.type} from node with id ${packet.source.toInt()} that I don't know about!")
            } else if (nodeRequests.isPending(node.nodeId, packet.type)) {
                nodeRequests.handleResponse(node.nodeId, packet.type)
                node.processPacket(packet)
            }
        }

        if (allNodesReadyToSleep()) {
            goToSleep()
        }
    }

    fun nextIdempotencyKey(): Byte {
        return idempotencyCounter++.toByte()
    }

    private fun allNodesAwake(): Boolean {
        return knownNodeDefs.filter { it.status == NodeStatus.DEPLOYED }.all { addressIsAwake(it.address) }
    }

    private fun allNodesReadyToSleep(): Boolean {
        return allNodesAwake() && nodeRequests.outstandingRequests().isEmpty()
    }

    private fun goToSleep() {
        sleepTimer.stop()
        nodeRequests.clear()
        discoveredNodes.values.sortedBy { it.nodeId }.reversed().forEach {
            Thread.sleep(PACKET_SEND_DELAY)
            it.sleep()
        }
        meshState = SLEEPING
    }

    private fun wakeUp() {
        logger.info("Waking up...")
        meshState = AWAKE
        timeWokenUp = System.currentTimeMillis()
        sleepTimer.start()
    }

    private fun addressIsAwake(address: String): Boolean {
        return discoveredNodes.values.singleOrNull { discoveredNode ->
            address == discoveredNode.nodeDef.address && discoveredNode.awake
        } != null
    }

    fun sendPacket(packet: Packet, newRequest: Boolean = true) {
        if (packet.type is RequestPacketType && newRequest) {
            nodeRequests.addRequest(NodeRequest(packet.dest.toInt(), packet.type, packet.data))
        }
        meshConnections.values.forEach { meshConnection -> meshConnection.sendPacket(packet) }
    }

    override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral, scanResult: ScanResult) {
        if (peripheral.name == "mn" && peripheral.state == ConnectionState.DISCONNECTED && peripheral.address == "10:52:1C:6C:84:EA") {
            val meshConnection = MeshConnection(peripheral, this).apply {
                meshConnections[peripheral.address] = this
            }
            meshConnection.rssi = scanResult.rssi
            logger.info("Connecting to peripheral ${peripheral.address} with rssi of ${scanResult.rssi}")
            bleManager.connectPeripheral(peripheral, meshConnection)
        }
    }

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
        logger.info("Connected to peripheral ${peripheral.address}")
    }

    override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral, status: BluetoothCommandStatus) {
        logger.info("Disconnected from peripheral ${peripheral.address}")
    }

    private inner class SleepTimer(private val timeUntilForcedSleep: Long) : Runnable {
        private var stopped = true
        private var stopTime: Long = -1

        fun start() {
            if (stopped) {
                stopped = false
                stopTime = System.currentTimeMillis() + timeUntilForcedSleep
                Thread(this).start()
            }
        }

        fun stop() {
            stopped = true
        }

        override fun run() {
            while (!stopped && System.currentTimeMillis() < stopTime) {
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                }
            }
            if (!stopped) {
                goToSleep()
            }
            stopped = true
        }
    }
}

enum class MeshState {
    SLEEPING, AWAKE
}

val logger: Logger = LoggerFactory.getLogger("BleMeshHub")

fun main() {
    SyncBeacon("/home/pi/", 180, System.currentTimeMillis())
    MeshHub()
}

val MESH_SENSOR_SVC_UUID: UUID = UUID.fromString("00005000-0000-1000-8000-00805f9b34fb")
val MESH_SENSOR_CHR_W_UUID: UUID = UUID.fromString("00005001-0000-1000-8000-00805f9b34fb")
val MESH_SENSOR_CHR_R_UUID: UUID = UUID.fromString("00005002-0000-1000-8000-00805f9b34fb")
