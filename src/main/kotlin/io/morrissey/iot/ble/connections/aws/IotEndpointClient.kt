package io.morrissey.iot.ble.connections.aws

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.io.File
import java.lang.RuntimeException


class IotEndpointClient(
    private val endpointUrl: String,
    certsDir: String
) {
    companion object {
        private const val IOT_CERT_FILE = "certificate.pem.crt"
        private const val IOT_PRIVATE_KEY_FILE = "private.pem.key"
        private const val IOT_CA_CERT_FILE = "amazon_ca_cert.pem.crt"
    }
    private val client: OkHttpClient
    private val moshi = Moshi.Builder().build()
    private val jsonAdapter: JsonAdapter<Map<String, Any>> = moshi.adapter(Types.newParameterizedType(
        Map::class.java, String::class.java, Any::class.java
    ))

    init {
        val privateKey = convertPkcs1ToPkcs8("$certsDir/$IOT_PRIVATE_KEY_FILE")
        val caCert = loadCertificate("$certsDir/$IOT_CA_CERT_FILE")

        val clientCertificate = HeldCertificate.decode(File("$certsDir/$IOT_CERT_FILE").readText() + "\n" + privateKey)
        val clientCertificates: HandshakeCertificates = HandshakeCertificates.Builder()
            .heldCertificate(clientCertificate)
            .addTrustedCertificate(caCert)
            .build()
        client = OkHttpClient.Builder()
            .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
            .build()
    }

    fun getShadow(thingName: String): String {
        val url = "$endpointUrl/api/things/$thingName/shadow"
        val call: Call = client.newCall(
            Request.Builder().url(url).build()
        )
        return filterErrorResponse(call.execute().use {
            it.body?.string()
        })
    }

    fun updateShadow(thingName: String, body: String): String {
        val requestBody = body.toRequestBody("application/json".toMediaType())
        val url = "$endpointUrl/api/things/$thingName/shadow"
        val call: Call = client.newCall(
            Request.Builder().url(url).post(requestBody).build()
        )
        return filterErrorResponse(call.execute().use {
            it.body?.string()
        })
    }

    private fun filterErrorResponse(jsonResponse: String?): String {
        if (jsonResponse == null) throw RuntimeException("Empty response received from request.")
        val respObj = jsonAdapter.fromJson(jsonResponse) ?: throw RuntimeException("json response was invalid: $jsonResponse")
        respObj["message"]?.let { throw RuntimeException("Received message from request: $it") }
        return jsonResponse
    }
}
