package com.logie.wowforever

import com.winlator.container.Container
import java.io.File

/**
 * Windows settings Battle.net checks for, which Wine's Windows doesn't have: written into the app's own Windows
 * (its registry), before Wine starts. Nothing of Blizzard's is touched.
 *
 * - **Secondary Logon** (`seclogon`): Battle.net's installer refuses to run without the service ("Battle.net
 *   requires the Windows Secondary Logon service to be enabled", BLZBNTBTS00000025; Wine bug 39261). Wine has no
 *   such service, so it's registered as Windows has it, set to start on demand (enabled).
 */
object WindowsSetup {
    /** The key as Wine's `system.reg` writes it (relative to HKEY_LOCAL_MACHINE, backslashes doubled). */
    private const val SECLOGON_KEY = "[System\\\\ControlSet001\\\\Services\\\\seclogon]"

    fun apply(container: Container) = apply(File(container.rootDir, ".wine/system.reg"))

    /** [system]: the Windows' `system.reg` (HKEY_LOCAL_MACHINE). Wine must not be running (it rewrites the file). */
    fun apply(system: File) {
        if (!system.isFile) return
        val text = system.readText()
        if (text.contains(SECLOGON_KEY, ignoreCase = true)) return
        val seconds = System.currentTimeMillis() / 1000
        val block = buildString {
            append('\n').append(SECLOGON_KEY).append(' ').append(seconds).append('\n')
            append("\"Description\"=\"Enables starting processes under alternate credentials.\"\n")
            append("\"DisplayName\"=\"Secondary Logon\"\n")
            append("\"ErrorControl\"=dword:00000001\n")
            append("\"ImagePath\"=str(2):\"C:\\\\windows\\\\system32\\\\svchost.exe -k netsvcs -p\"\n")
            append("\"ObjectName\"=\"LocalSystem\"\n")
            // 3: start on demand (enabled; 4 would be disabled). 0x20: a shared service process.
            append("\"Start\"=dword:00000003\n")
            append("\"Type\"=dword:00000020\n")
        }
        val temp = File(system.path + ".wf")
        temp.writeText(text.trimEnd('\n') + "\n" + block)
        if (!temp.renameTo(system)) {
            temp.delete()
            throw IllegalStateException("Couldn't write ${system.name}")
        }
    }
}
