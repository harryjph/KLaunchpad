package nativeApi

import com.harry1453.klaunchpad.api.Color
import com.harry1453.klaunchpad.api.Launchpad
import com.harry1453.klaunchpad.api.Pad
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Helper function for externally facing non-member functions
 */
internal inline fun <T> externalFunction(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    initRuntimeIfNeeded()
    return block()
}

/**
 * Helper function for externally facing launchpad member functions
 */
internal inline fun <T> externalFunctionWithLaunchpad(launchpadPtr: COpaquePointer, block: (Launchpad) -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    initRuntimeIfNeeded()
    if (launchpadPtr.rawValue.toLong() == 0L) throw NullPointerException()
    val launchpad = launchpadPtr.asStableRef<Launchpad>().get()
    return block(launchpad)
}

/**
 * Helper function for externally facing pad member functions
 */
internal inline fun <T> externalFunctionWithPad(padPtr: COpaquePointer, block: (Pad) -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    initRuntimeIfNeeded()
    if (padPtr.rawValue.toLong() == 0L) throw NullPointerException()
    val pad = padPtr.asStableRef<Pad>().get()
    return block(pad)
}

internal fun Pad.toPointer(): COpaquePointer {
    return StableRef.create(this).asCPointer() // TODO when is this freed?
}

internal fun COpaquePointer.toPad(): Pad? {
    if (this.rawValue.toLong() == 0L) return null
    return this.asStableRef<Pad>().get()
}

internal fun Color.toPointer(): COpaquePointer {
    return StableRef.create(this).asCPointer() // TODO when is this freed?
}

internal fun COpaquePointer.toColor(): Color {
    if (this.rawValue.toLong() == 0L) throw NullPointerException()
    return this.asStableRef<Color>().get()
}