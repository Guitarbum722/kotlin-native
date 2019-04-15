/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

import kotlin.native.internal.ExportForCppRuntime
import kotlin.native.internal.ExportTypeInfo
import kotlin.native.internal.NativePtrArray

/**
 * The base class for all errors and exceptions. Only instances of this class can be thrown or caught.
 *
 * @param message the detail message string.
 * @param cause the cause of this throwable.
 */
@ExportTypeInfo("theThrowableTypeInfo")
public open class Throwable(open val message: String?, open val cause: Throwable?) {

    constructor(message: String?) : this(message, null)

    constructor(cause: Throwable?) : this(cause?.toString(), cause)

    constructor() : this(null, null)

    @get:ExportForCppRuntime("Kotlin_Throwable_getStackTrace")
    private val stackTrace = getCurrentStackTrace()

    private val stackTraceStrings: Array<String> by lazy {
        getStackTraceStrings(stackTrace)
    }

    public fun getStackTrace(): Array<String> = stackTraceStrings

    public fun printStackTrace(): Unit = dumpStackTraceTo(StdOut)

    internal fun dumpStackTrace(): String = buildString { dumpStackTraceTo(this) }

    private fun dumpStackTraceTo(appendable: Appendable): Unit = with(appendable) {
        appendln(this@Throwable.toString())

        for (element in stackTraceStrings) {
            appendln("        at $element")
        }

        cause?.let {
            // TODO: should skip common stack frames
            append("Caused by: ")
            it.dumpStackTraceTo(this)
        }
    }

    public override fun toString(): String {
        val kClass = this::class
        val s = kClass.qualifiedName ?: kClass.simpleName ?: "Throwable"
        return if (message != null) s + ": " + message.toString() else s
    }
}

@SymbolName("Kotlin_getCurrentStackTrace")
private external fun getCurrentStackTrace(): NativePtrArray

@SymbolName("Kotlin_getStackTraceStrings")
private external fun getStackTraceStrings(stackTrace: NativePtrArray): Array<String>

private fun Appendable.appendln() = append('\n')
private fun Appendable.appendln(str: String) = append(str).appendln()

private object StdOut: Appendable {
    override fun append(c: Char) = apply { print(c) }
    override fun append(csq: CharSequence?) = apply { print(csq) }
    override fun append(csq: CharSequence?, start: Int, end: Int) = apply { print(csq?.subSequence(start, end)) }
}