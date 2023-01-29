package io.morrissey.iot.ble

import com.welie.blessed.BluetoothCommandStatus
import com.welie.blessed.BluetoothGattCharacteristic
import com.welie.blessed.BluetoothGattDescriptor
import com.welie.blessed.BluetoothGattService
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.BluetoothPeripheralCallback

class MeshConnection(
    private val peripheral: BluetoothPeripheral,
    private val hub: MeshHub
) : BluetoothPeripheralCallback() {
    var rssi: Int? = null

    fun sendPacket(packet: Packet) {
        logger.debug("Sending packet: \n$packet")
        logger.info("Sending packet type ${packet.type} to node ${packet.dest}")
        peripheral.writeCharacteristic(
            MESH_SENSOR_SVC_UUID, MESH_SENSOR_CHR_W_UUID, packet.pack(),
            BluetoothGattCharacteristic.WriteType.WITH_RESPONSE
        )
    }

    override fun onCharacteristicUpdate(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: BluetoothCommandStatus
    ) {
        logger.debug("onCharacteristicUpdate: characteristic uuid: \"${characteristic.uuid}\", value: ${value.toText()}")
        try {
            if (characteristic.uuid.toString() == MESH_SENSOR_CHR_R_UUID.toString()) {
                val dataPacket = Packet(value)
                hub.receivePacket(dataPacket)
            } else {
                logger.warn("Received a characteristic update for an unknown characteristic with uuid: ${characteristic.uuid}")
            }
        } catch (e: Exception) {
            logger.error("Error occurred when processing packet notification.", e)
        }
    }

    override fun onServicesDiscovered(peripheral: BluetoothPeripheral, services: MutableList<BluetoothGattService>) {
        val servicesInfo = services.map {
            "Service: ${it.uuid}, Num chars: ${it.characteristics.size}, Characteristics: \n${
                it.characteristics.joinToString(
                    "\n"
                ) { characteristic -> characteristic.uuid.toString() }
            }"
        }
        logger.debug("onServicesDiscovered: ${peripheral.name}: \n$servicesInfo ")
        peripheral.setNotify(MESH_SENSOR_SVC_UUID, MESH_SENSOR_CHR_R_UUID, true)
    }

    override fun onNotificationStateUpdate(
        peripheral: BluetoothPeripheral,
        characteristic: BluetoothGattCharacteristic,
        status: BluetoothCommandStatus
    ) {
        logger.debug("onNotificationStateUpdate: ${peripheral.name}: characteristic uuid: ${characteristic.uuid}, status: $status")
    }

    override fun onCharacteristicWrite(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: BluetoothCommandStatus
    ) {
        logger.debug("onCharacteristicWrite: ${peripheral.name}: value: ${value.toText()}, characteristic uuid: ${characteristic.uuid}, status: $status")
    }

    override fun onDescriptorRead(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        descriptor: BluetoothGattDescriptor,
        status: BluetoothCommandStatus
    ) {
        logger.debug("onDescriptorRead: ${peripheral.name}: value: ${value.toText()}, descriptor: $descriptor, status: $status")
    }

    override fun onDescriptorWrite(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        descriptor: BluetoothGattDescriptor,
        status: BluetoothCommandStatus
    ) {
        logger.debug("onDescriptorWrite: ${peripheral.name}: value: ${value.toText()}, descriptor: $descriptor, status: $status")
    }

    override fun onBondingStarted(peripheral: BluetoothPeripheral) {
        logger.debug("onBondingStarted: ${peripheral.name}")
    }

    override fun onBondingSucceeded(peripheral: BluetoothPeripheral) {
        logger.debug("onBondingSucceeded: ${peripheral.name}")
    }

    override fun onBondingFailed(peripheral: BluetoothPeripheral) {
        logger.debug("onBondingFailed: ${peripheral.name}")
    }

    override fun onBondLost(peripheral: BluetoothPeripheral) {
        logger.debug("onBondLost: ${peripheral.name}")
    }

    override fun onReadRemoteRssi(peripheral: BluetoothPeripheral, rssi: Int, status: BluetoothCommandStatus) {
        logger.debug("onReadRemoteRssi: ${peripheral.name}, rssi: $rssi, status: $status")
    }
}
