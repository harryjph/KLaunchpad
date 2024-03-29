package com.harry1453.klaunchpad.impl.util

internal fun String.parseHexString(): ByteArray {
    return ByteArray(this.length / 2) { index ->
        this.substring(index * 2 .. index * 2 + 1).toUByte(radix = 16).toByte()
    }
}

internal fun ByteArray.toHexString(): String {
    return this.map { it.toUByte().toString(16).uppercase() }
        .map { if (it.length < 2) "0$it" else it }
        .joinToString(separator = "") { it }
}

internal infix operator fun ByteArray.plus(other: ByteArray): ByteArray {
    val newArray = ByteArray(this.size + other.size)
    this.copyInto(newArray)
    other.copyInto(newArray, destinationOffset = this.size)
    return newArray
}

internal infix operator fun ByteArray.plus(other: Int): ByteArray {
    val newArray = ByteArray(this.size + 1)
    this.copyInto(newArray)
    newArray[this.size] = other.toByte()
    return newArray
}

internal infix operator fun ByteArray.plus(other: Byte): ByteArray {
    val newArray = ByteArray(this.size + 1)
    this.copyInto(newArray)
    newArray[this.size] = other
    return newArray
}
