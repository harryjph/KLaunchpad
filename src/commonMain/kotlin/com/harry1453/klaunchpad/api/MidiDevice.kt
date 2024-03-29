package com.harry1453.klaunchpad.api

public interface MidiInputDeviceInfo {
    public val name: String
    public val version: String
}

/**
 * Open this MIDI Output device.
 */
public suspend fun MidiInputDeviceInfo.open(): MidiInputDevice = Launchpad.openMidiInputDevice(this)

public interface MidiOutputDeviceInfo {
    public val name: String
    public val version: String
}

/**
 * Open this MIDI Output device.
 */
public suspend fun MidiOutputDeviceInfo.open(): MidiOutputDevice = Launchpad.openMidiOutputDevice(this) // TODO make this part of the interface & the main way of doing it

@Deprecated("Use separate MidiInputDevice and MidiOutputDevice")
internal interface MidiDevice : Closable {
    fun sendMessage(channel: Int, data1: Int, data2: Int, messageType: MessageType)
    fun sendSysEx(bytes: ByteArray)
    fun clock()
    fun setMessageListener(messageListener: (ByteArray) -> Unit)

    enum class MessageType {
        NoteOn,
        NoteOff,
        ControlChange,
    }
}

@Deprecated("Use separate MidiInputDevice and MidiOutputDevice")
internal class MidiDeviceWrapper(private val inputDevice: MidiInputDevice, private val outputDevice: MidiOutputDevice) : MidiDevice { // TODO remove this class once the launchpads can use the individual devices
    override fun sendMessage(channel: Int, data1: Int, data2: Int, messageType: MidiDevice.MessageType) {
        val firstByte = (when(messageType) {
            MidiDevice.MessageType.NoteOff -> 0x80
            MidiDevice.MessageType.NoteOn -> 0x90
            MidiDevice.MessageType.ControlChange -> 0xB0
        } or (channel and 0xF)).toByte()
        val message = byteArrayOf(firstByte, data1.toByte(), data2.toByte())
        outputDevice.sendMessage(message)
    }

    override fun sendSysEx(bytes: ByteArray) {
        outputDevice.sendMessage(bytes)
    }

    override fun clock() {
        outputDevice.sendMessage(byteArrayOf(0xF8.toByte()))
    }

    override fun setMessageListener(messageListener: (ByteArray) -> Unit) {
        inputDevice.setMessageListener(messageListener)
    }

    override val isClosed: Boolean
        get() = inputDevice.isClosed && outputDevice.isClosed

    override fun close() {
        if (!inputDevice.isClosed) inputDevice.close()
        if (!outputDevice.isClosed) outputDevice.close()
    }
}

public interface MidiInputDevice: Closable {
    public fun setMessageListener(messageListener: (message: ByteArray) -> Unit)
}

public interface MidiOutputDevice: Closable {
    public fun sendMessage(message: ByteArray)
}

internal expect suspend fun listMidiInputDevicesImpl(): List<MidiInputDeviceInfo>

internal expect suspend fun openMidiInputDeviceImpl(deviceInfo: MidiInputDeviceInfo): MidiInputDevice

internal expect suspend fun listMidiOutputDevicesImpl(): List<MidiOutputDeviceInfo>

internal expect suspend fun openMidiOutputDeviceImpl(deviceInfo: MidiOutputDeviceInfo): MidiOutputDevice
