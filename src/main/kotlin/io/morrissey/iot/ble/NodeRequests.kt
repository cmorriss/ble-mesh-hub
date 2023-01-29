package io.morrissey.iot.ble

import io.morrissey.iot.ble.MeshHub.Companion.PACKET_SEND_DELAY
import io.morrissey.iot.ble.MeshHub.Companion.RESEND_DELAY
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class NodeRequests(private val meshHub: MeshHub) {
    private val pendingNodeRequests: MutableMap<Int, MutableMap<PacketType, NodeRequest>> = ConcurrentHashMap()
    private val resender = Resender()

    fun addRequest(request: NodeRequest) {
        val requestsForNode = pendingNodeRequests[request.nodeId] ?: ConcurrentHashMap()
        requestsForNode[request.packetType.response] = request
        pendingNodeRequests[request.nodeId] = requestsForNode
        logger.info("Added new request to node requests: $request")
        resender.start()
    }

    fun handleResponse(nodeId: Int, packetType: PacketType) {
        pendingNodeRequests[nodeId]?.remove(packetType)
    }

    fun outstandingRequests(): List<NodeRequest> {
        return pendingNodeRequests.values.flatMap { it.values }
    }

    fun isPending(nodeId: Int, packetType: PacketType): Boolean {
        return pendingNodeRequests[nodeId]?.get(packetType) != null
    }

    fun clear() {
        pendingNodeRequests.clear()
    }

    private inner class Resender : Runnable {
        private val running: AtomicBoolean = AtomicBoolean(false)

        fun start() {
            if (!running.getAndSet(true)) {
                logger.info("Starting resender...")
                Thread(this).start()
            }
        }
        override fun run() {
            do {
                Thread.sleep(RESEND_DELAY)
                outstandingRequests().forEach { request ->
                    logger.info("Resending packet type ${request.packetType} to node ${request.nodeId}")
                    meshHub.sendPacket(Packet(
                        HUB_NODE_ID,
                        request.nodeId.toByte(),
                        PACKET_TTL,
                        meshHub.nextIdempotencyKey(),
                        request.packetType,
                        request.data.size.toByte(),
                        request.data
                    ), false)
                    Thread.sleep(PACKET_SEND_DELAY)
                }
            } while (running.getAndSet(pendingNodeRequests.isNotEmpty()))
            logger.info("Resender finished.")
        }

    }
}

