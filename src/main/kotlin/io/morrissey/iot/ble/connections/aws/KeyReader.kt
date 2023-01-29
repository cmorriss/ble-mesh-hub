package io.morrissey.iot.ble.connections.aws

import java.io.File
import java.io.FileInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64

private const val PKCS_1_PEM_HEADER = "-----BEGIN RSA PRIVATE KEY-----\n"
private const val PKCS_1_PEM_FOOTER = "-----END RSA PRIVATE KEY-----"
private const val PKCS_8_PEM_HEADER = "-----BEGIN PRIVATE KEY-----\n"
private const val PKCS_8_PEM_FOOTER = "-----END PRIVATE KEY-----"

fun loadCertificate(certFilePath: String): X509Certificate {
    val certificateFactory = CertificateFactory.getInstance("X.509")
    return FileInputStream(certFilePath).use {
        certificateFactory.generateCertificate(it) as X509Certificate
    }
}

fun convertPkcs1ToPkcs8(filePath: String): String {
    val keyDataString = File(filePath).readText().replace(PKCS_1_PEM_HEADER, "").replace(PKCS_1_PEM_FOOTER, "")
    val pkcs8Bytes = convertPkcs1BytestoPkcs8(Base64.getMimeDecoder().decode(keyDataString))
    return "$PKCS_8_PEM_HEADER${String(Base64.getMimeEncoder().encode(pkcs8Bytes))}\n$PKCS_8_PEM_FOOTER"
}

private fun convertPkcs1BytestoPkcs8(pkcs1Bytes: ByteArray): ByteArray { // We can't use Java internal APIs to parse ASN.1 structures, so we build a PKCS#8 key Java can understand
    val pkcs1Length = pkcs1Bytes.size
    val totalLength = pkcs1Length + 22
    val pkcs8Header = byteArrayOf(
        0x30,
        0x82.toByte(),
        (totalLength shr 8 and 0xff).toByte(),
        (totalLength and 0xff).toByte(),  // Sequence + total length
        0x2,
        0x1,
        0x0,  // Integer (0)
        0x30,
        0xD,
        0x6,
        0x9,
        0x2A,
        0x86.toByte(),
        0x48,
        0x86.toByte(),
        0xF7.toByte(),
        0xD,
        0x1,
        0x1,
        0x1,
        0x5,
        0x0,  // Sequence: 1.2.840.113549.1.1.1, NULL
        0x4,
        0x82.toByte(),
        (pkcs1Length shr 8 and 0xff).toByte(),
        (pkcs1Length and 0xff).toByte() // Octet string + length
    )
    return join(pkcs8Header, pkcs1Bytes)
}

private fun join(byteArray1: ByteArray, byteArray2: ByteArray): ByteArray {
    val bytes = ByteArray(byteArray1.size + byteArray2.size)
    System.arraycopy(byteArray1, 0, bytes, 0, byteArray1.size)
    System.arraycopy(byteArray2, 0, bytes, byteArray1.size, byteArray2.size)
    return bytes
}
