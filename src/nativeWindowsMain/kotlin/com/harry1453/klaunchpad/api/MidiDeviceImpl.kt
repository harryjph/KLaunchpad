package com.harry1453.klaunchpad.api

import kotlinx.cinterop.*
import platform.windows.*

internal class Holder<T> {
    var value: T? = null
}

private data class MidiInputDeviceInfoImpl(
    override val name: String,
    override val version: String,
    val deviceID: UInt
) : MidiInputDeviceInfo

internal actual suspend fun listMidiInputDevicesImpl(): List<MidiInputDeviceInfo> {
    val inputDeviceCount = WindowsMidiApi.midiInGetNumDevs!!()
    val list = mutableListOf<MidiInputDeviceInfo>()
    memScoped {
        for (i in 0u until inputDeviceCount) {
            val capabilities = alloc<MIDIINCAPS>()
            val retVal = WindowsMidiApi.midiInGetDevCaps!!(i.toUInt(), capabilities.ptr, sizeOf<MIDIINCAPS>().toUInt())
            WindowsMidiApi.throwIfError(retVal)
            list.add(MidiInputDeviceInfoImpl(capabilities.szPname.toKString(), capabilities.vDriverVersion.toString(16), i)) // TODO proper version conversion
        }
    }
    return list
}

internal actual suspend fun openMidiInputDeviceImpl(deviceInfo: MidiInputDeviceInfo): MidiInputDevice {
    require(deviceInfo is MidiInputDeviceInfoImpl)

    val midiDeviceHolder = Holder<MidiInputDeviceImpl>()
    val midiDeviceHolderRef = StableRef.create(midiDeviceHolder)

    val device = nativeHeap.alloc<HMIDIINVar>()
    var retVal = WindowsMidiApi.midiInOpen!!(device.ptr, deviceInfo.deviceID, staticCFunction(::midiInCallback).rawValue.toLong().toULong(), midiDeviceHolderRef.asCPointer().rawValue.toLong().toULong(), CALLBACK_FUNCTION.toUInt())
    WindowsMidiApi.throwIfError(retVal)
    retVal = WindowsMidiApi.midiInStart!!(device.value!!)
    WindowsMidiApi.throwIfError(retVal)

    val midiDevice = MidiInputDeviceImpl(device, midiDeviceHolderRef)
    midiDeviceHolder.value = midiDevice
    return midiDevice
}

private data class MidiOutputDeviceInfoImpl(
    override val name: String,
    override val version: String,
    val deviceID: UInt
) : MidiOutputDeviceInfo

internal actual suspend fun listMidiOutputDevicesImpl(): List<MidiOutputDeviceInfo> {
    val outputDeviceCount = WindowsMidiApi.midiOutGetNumDevs!!()
    val list = mutableListOf<MidiOutputDeviceInfo>()
    memScoped {
        for (i in 0u until outputDeviceCount) {
            val capabilities = alloc<MIDIOUTCAPS>()
            val retVal = WindowsMidiApi.midiOutGetDevCaps!!(i.toUInt(), capabilities.ptr, sizeOf<MIDIINCAPS>().toUInt())
            WindowsMidiApi.throwIfError(retVal)
            list.add(MidiOutputDeviceInfoImpl(capabilities.szPname.toKString(), capabilities.vDriverVersion.toString(16), i)) // TODO proper version conversion
        }
    }
    return list
}

internal actual suspend fun openMidiOutputDeviceImpl(deviceInfo: MidiOutputDeviceInfo): MidiOutputDevice {
    require(deviceInfo is MidiOutputDeviceInfoImpl)

    val device = nativeHeap.alloc<HMIDIOUTVar>()
    val retVal = WindowsMidiApi.midiOutOpen!!(device.ptr, deviceInfo.deviceID, 0u, 0u, CALLBACK_NULL.toUInt())
    WindowsMidiApi.throwIfError(retVal)
    return MidiOutputDeviceImpl(device)
}

@Suppress("UNUSED_PARAMETER")
internal fun midiInCallback(hmi: HMIDIIN, wMsg: UINT, dwInstance: DWORD_PTR, dwParam1: DWORD_PTR, dwParam2: DWORD_PTR) {
    initRuntimeIfNeeded()
    val midiDeviceHolder = dwInstance.toLong().toCPointer<CPointed>()!!.asStableRef<Holder<MidiInputDeviceImpl>>().get()
    if (midiDeviceHolder.value == null) return
    when(wMsg.toInt()) {
        MIM_DATA -> {
            val decodedInt = dwParam1.toInt().toBytesLE()
            val messageBytes = ByteArray(3)
            decodedInt.copyInto(messageBytes, endIndex = messageBytes.size)
            midiDeviceHolder.value?.messageListener?.invoke(messageBytes)
        }
        MIM_LONGDATA -> {
            val midiHdr = dwParam1.toLong().toCPointer<MIDIHDR>()!!.pointed
            val buffer = ByteArray(midiHdr.dwBytesRecorded.toInt())
            for (i in 0 until midiHdr.dwBytesRecorded.toInt()) buffer[i] = midiHdr.lpData!![i]
            midiDeviceHolder.value?.messageListener?.invoke(buffer)
        }
    }
}

private class MidiInputDeviceImpl(private val device: HMIDIINVar, private val midiDeviceHolderRef: StableRef<Holder<MidiInputDeviceImpl>>) :
    MidiInputDevice {
    override var isClosed: Boolean = false

    internal var messageListener: ((ByteArray) -> Unit)? = null

    override fun setMessageListener(messageListener: (message: ByteArray) -> Unit) {
        this.messageListener = messageListener
    }

    override fun close() {
        var retVal = WindowsMidiApi.midiInStop!!(device.value!!)
        WindowsMidiApi.throwIfError(retVal)
        retVal = WindowsMidiApi.midiInClose!!(device.value!!)
        WindowsMidiApi.throwIfError(retVal)
        midiDeviceHolderRef.dispose()
        nativeHeap.free(device)
        isClosed = true
    }
}

private class MidiOutputDeviceImpl(private val device: HMIDIOUTVar) : MidiOutputDevice {
    override var isClosed: Boolean = false

    override fun sendMessage(message: ByteArray) {
        if (message.size <= 3) {
            val buffer = ByteArray(4)
            message.copyInto(buffer)
            val retVal = WindowsMidiApi.midiOutShortMsg!!(device.value!!, buffer.toUintLE())
            WindowsMidiApi.throwIfError(retVal)
        } else {
            memScoped {
                val buffer = allocArray<ByteVar>(message.size)
                message.forEachIndexed { index, byte ->
                    buffer[index] = byte
                }
                val midiHdr = alloc<MIDIHDR>()
                midiHdr.lpData = buffer
                midiHdr.dwBufferLength = message.size.toUInt()
                midiHdr.dwBytesRecorded = message.size.toUInt()

                var retVal = WindowsMidiApi.midiOutPrepareHeader!!(device.value!!, midiHdr.ptr, sizeOf<MIDIHDR>().toUInt())
                WindowsMidiApi.throwIfError(retVal)
                retVal = WindowsMidiApi.midiOutLongMsg!!(device.value!!, midiHdr.ptr, sizeOf<MIDIHDR>().toUInt())
                WindowsMidiApi.throwIfError(retVal)
                retVal = WindowsMidiApi.midiOutUnprepareHeader!!(device.value!!, midiHdr.ptr, sizeOf<MIDIHDR>().toUInt())
                WindowsMidiApi.throwIfError(retVal)
            }
        }
    }

    override fun close() {
        val retVal = WindowsMidiApi.midiOutClose!!(device.value!!)
        WindowsMidiApi.throwIfError(retVal)
        nativeHeap.free(device)
        isClosed = true
    }
}
