package io.morrissey.iot.ble

import java.nio.ByteBuffer
import java.nio.ByteOrder

const val SRC_IDX = 0
const val DST_IDX = SRC_IDX + 1
const val TTL_IDX = DST_IDX + 1
const val IDEMPOTENCY_KEY_IDX = TTL_IDX + 1
const val TYPE_IDX = IDEMPOTENCY_KEY_IDX + 1
const val DATA_LEN_IDX = TYPE_IDX + 1
const val DATA_IDX = DATA_LEN_IDX + 1

sealed class PacketType(val byte: Byte) {
    private val byteString = "%02x".format(byte)

    init {
        values[byte] = this
    }

    companion object {
        private val values = mutableMapOf<Byte, PacketType>()

        fun fromByte(byte: Byte): PacketType {
            PtNodeConnected.toString()
            return values[byte]!!
        }
    }

    override fun toString(): String {
        return "${this::class.java.simpleName}[$byteString]"
    }
}

abstract class RequestPacketType(byte: Byte, val response: PacketType) : PacketType(byte)

/* Base packet types */
object PtNodeConnected : PacketType(1)
object PtNodeConnectedResp : PacketType(2)
object PtOtaUpdateReq : RequestPacketType(3, PtOtaUpdateResp)
object PtOtaUpdateResp : PacketType(4)
object PtGoToSleep : PacketType(5)

/* Data request types */
object PtBatteryPctReq : RequestPacketType(10, PtBatteryPctResp)
object PtBatteryPctResp : PacketType(11)
object PtBatteryVoltageReq : RequestPacketType(12, PtBatteryVoltageResp)
object PtBatteryVoltageResp : PacketType(13)
object PtMoisturePctReq : RequestPacketType(14, PtMoisturePctResp)
object PtMoisturePctResp : PacketType(15)
object PtMoistureVoltageReq : RequestPacketType(16, PtMoistureVoltageResp)
object PtMoistureVoltageResp : PacketType(17)

/* Configuration update types */
object PtSensorHvUpdate : RequestPacketType(30, PtSensorHvAck)
object PtSensorHvAck : PacketType(31)
object PtSensorLvUpdate : RequestPacketType(32, PtSensorLvAck)
object PtSensorLvAck : PacketType(33)
object PtBatteryHvUpdate : RequestPacketType(34, PtBatteryHvAck)
object PtBatteryHvAck : PacketType(35)
object PtBatteryLvUpdate : RequestPacketType(36, PtBatteryLvAck)
object PtBatteryLvAck : PacketType(37)
object PtSleepDurationUpdate : RequestPacketType(38, PtSleepDurationAck)
object PtSleepDurationAck : PacketType(39)

const val PACKET_TTL: Byte = 5

data class Packet(
    val source: Byte,
    val dest: Byte,
    val ttl: Byte,
    val idempotencyKey: Byte,
    val type: PacketType,
    val dataLength: Byte,
    val data: ByteArray
) {
    constructor(packedDataArray: ByteArray) : this(
        packedDataArray.getSource(),
        packedDataArray.getDest(),
        packedDataArray.getTtl(),
        packedDataArray.getIdempotencyKey(),
        packedDataArray.getType(),
        packedDataArray.getDataLength(),
        packedDataArray.getDataArray()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Packet

        return source == other.source && idempotencyKey == other.idempotencyKey
    }

    override fun hashCode(): Int {
        var result = source.toInt()
        result = 31 * result + idempotencyKey.toInt()
        return result
    }

    override fun toString(): String {
        return "Packet(source=$source, dest=$dest, ttl=$ttl, idempotencyKey=$idempotencyKey, type=$type, dataLength=$dataLength, data=${data.toText()})"
    }

    fun pack(): ByteArray {
        return byteArrayOf(
            source,
            dest,
            ttl,
            idempotencyKey,
            type.byte,
            dataLength,
            *data
        )
    }
}

fun ByteArray.getSource() = get(SRC_IDX)
fun ByteArray.getDest() = get(DST_IDX)
fun ByteArray.getTtl() = get(TTL_IDX)
fun ByteArray.getIdempotencyKey() = get(IDEMPOTENCY_KEY_IDX)
fun ByteArray.getType() = PacketType.fromByte(get(TYPE_IDX))
fun ByteArray.getDataLength() = get(DATA_LEN_IDX)
fun ByteArray.getDataArray(): ByteArray {
    val dataArray = ByteArray(getDataLength().toInt())
    copyInto(dataArray, 0, DATA_IDX)
    return dataArray
}

fun ByteArray.toText(): String {
    return joinToString(":") { "%02x".format(it).toUpperCase() }
}

fun ByteArray.littleEndianToText(): String {
    return reversedArray().toText()
}

fun ByteArray.toInt() = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).int
fun Int.toBytes() = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(this).array()
