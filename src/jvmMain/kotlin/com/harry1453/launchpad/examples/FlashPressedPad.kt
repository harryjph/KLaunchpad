@file:JvmName("FlashPressedPad")

package com.harry1453.launchpad.examples

import com.harry1453.launchpad.api.Color
import com.harry1453.launchpad.api.Launchpad
import com.harry1453.launchpad.impl.mk2.LaunchpadMk2

fun main() {
    val color1 = Color(0, 0, 255)
    val color2 = Color(255, 0, 0)
    val launchpad = Launchpad.connectToLaunchpadMK2()
    Runtime.getRuntime().addShutdownHook(Thread { launchpad.close() })
    launchpad.autoClockTempo = 60
    launchpad.autoClockEnabled = true
    launchpad.setPadButtonListener { pad, pressed, _ ->
        if (pressed) {
            launchpad.flashPadLight(pad, color1, color2)
        } else {
            launchpad.clearPadLight(pad)
        }
    }
}