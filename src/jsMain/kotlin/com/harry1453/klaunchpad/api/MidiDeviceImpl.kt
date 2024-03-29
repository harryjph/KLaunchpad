package com.harry1453.klaunchpad.api

import jsExternal.*
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array
import toMap

private data class MidiInputDeviceInfoImpl(
    override val name: String,
    override val version: String,
    val device: MIDIInput
) : MidiInputDeviceInfo

internal actual suspend fun listMidiInputDevicesImpl(): List<MidiInputDeviceInfo> {
    val midiAccess = window.navigator.requestMIDIAccess(midiOptions(sysex = true)).await()
    return midiAccess.inputs.toMap().map { (_, device) -> MidiInputDeviceInfoImpl(device.name.orEmpty(), device.version.orEmpty(), device) }
}

internal actual suspend fun openMidiInputDeviceImpl(deviceInfo: MidiInputDeviceInfo): MidiInputDevice {
    require(deviceInfo is MidiInputDeviceInfoImpl)
    return MidiInputDeviceImpl(deviceInfo.device.open().await())
}

private data class MidiOutputDeviceInfoImpl(
    override val name: String,
    override val version: String,
    val device: MIDIOutput
) : MidiOutputDeviceInfo

internal actual suspend fun listMidiOutputDevicesImpl(): List<MidiOutputDeviceInfo> {
    val midiAccess = window.navigator.requestMIDIAccess(midiOptions(sysex = true)).await()
    return midiAccess.outputs.toMap().map { (_, device) -> MidiOutputDeviceInfoImpl(device.name.orEmpty(), device.version.orEmpty(), device) }
}

internal actual suspend fun openMidiOutputDeviceImpl(deviceInfo: MidiOutputDeviceInfo): MidiOutputDevice {
    require(deviceInfo is MidiOutputDeviceInfoImpl)
    return MidiOutputDeviceImpl(deviceInfo.device.open().await())
}

private abstract class AbstractMidiDeviceWrapper(private val device: MIDIPort) : Closable {
    override val isClosed: Boolean get() = device.state == "connected"

    override fun close() {
        device.close()
    }
}

private class MidiInputDeviceImpl(private val device: MIDIInput) : AbstractMidiDeviceWrapper(device), MidiInputDevice {
    override fun setMessageListener(messageListener: (message: ByteArray) -> Unit) {
        device.onmidimessage = {
            messageListener(it.data.toByteArray())
        }
    }
}

private class MidiOutputDeviceImpl(private val device: MIDIOutput) : AbstractMidiDeviceWrapper(device), MidiOutputDevice {
    override fun sendMessage(message: ByteArray) {
        device.send(Uint8Array(message.toTypedArray()))
    }
}
