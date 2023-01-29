package io.morrissey.iot.ble.connections

import io.morrissey.iot.ble.NodeDef

interface DataConnection {
    val connectionSettingsKey: String
    fun sendData(nodeDef: NodeDef, dataName: String, dataValue: Int)

    fun setDataReceiver(receiver: DataReceiver)
}
