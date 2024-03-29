package com.harry1453.klaunchpad.api

import kotlinx.coroutines.runBlocking

/**
 * Provides a synchronous API for all of the suspend functions in [Launchpad].
 * Intended to be used by non-Kotlin JVM code, which does not have native coroutines.
 */
public object LaunchpadSync {
    /**
     * Synchronously get a list of currently connected MIDI output devices
     */
    @JvmStatic
    public fun listMidiInputDevices(): List<MidiInputDeviceInfo> = runBlocking { Launchpad.listMidiInputDevices() }

    /**
     * Synchronously open the MIDI device described by [deviceInfo], which must have been returned by [listMidiOutputDevices]
     */
    @JvmStatic
    public fun openMidiInputDevice(deviceInfo: MidiInputDeviceInfo): MidiInputDevice = runBlocking { Launchpad.openMidiInputDevice(deviceInfo) }

    /**
     * Synchronously get a list of currently connected MIDI output devices
     */
    @JvmStatic
    public fun listMidiOutputDevices(): List<MidiOutputDeviceInfo> = runBlocking { Launchpad.listMidiOutputDevices() }

    /**
     * Synchronously open the MIDI device described by [deviceInfo], which must have been returned by [listMidiOutputDevices]
     */
    @JvmStatic
    public fun openMidiOutputDevice(deviceInfo: MidiOutputDeviceInfo): MidiOutputDevice = runBlocking { Launchpad.openMidiOutputDevice(deviceInfo) }
}
