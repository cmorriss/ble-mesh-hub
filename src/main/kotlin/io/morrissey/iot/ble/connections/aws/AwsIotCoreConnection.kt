package io.morrissey.iot.ble.connections.aws

import io.morrissey.iot.ble.NodeDef
import io.morrissey.iot.ble.connections.DataConnection
import io.morrissey.iot.ble.connections.DataReceiver
import io.morrissey.iot.ble.logger

class AwsIotCoreConnection(private val connectionSettings: Map<String, Any>) : DataConnection {
    companion object {
        private const val IOT_CLIENT_ENDPOINT_URL_KEY = "EndpointUrl"
        private const val CERTS_DIR_KEY = "CertificatesDir"
        private const val DEFAULT_CERTS_DIR = "certs"
    }

    override val connectionSettingsKey = "AwsIotCore"

    private val endpointClient: IotEndpointClient

    init {
        val certsDir = connectionSettings[CERTS_DIR_KEY] as? String ?: DEFAULT_CERTS_DIR
        val endpointUrl = connectionSettings[IOT_CLIENT_ENDPOINT_URL_KEY] as? String ?: throw IllegalStateException(
            "A valid iot client endpoint url must be specified in the connection settings for the AWS client."
        )
        endpointClient = IotEndpointClient(endpointUrl, certsDir)
    }

    override fun sendData(nodeDef: NodeDef, dataName: String, dataValue: Int) {
        val thingName = nodeDef.toThingName()
        val payload = """
            {
                "state": {
                    "reported": {
                        "$dataName": $dataValue
                    }
                }
            }
        """.trimIndent()
        logger.info("Publishing data for $dataName: $dataValue to AWS IoT Core for node address ${nodeDef.address}...")
        try {
            val response = endpointClient.updateShadow(thingName, payload)
            logger.info("Received response when publishing to iot core: $response")
        } catch (e: Exception) {
            logger.error("Caught exception while attempting to publish to IoT core.", e)
        }
    }

    private fun NodeDef.toThingName(): String {
        return "Sensor_${address.replace(':', '_')}"
    }

    override fun setDataReceiver(receiver: DataReceiver) {
        TODO("Not yet implemented")
    }
}
