package com.logie.wowforever

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

class WindowsSetupTest {
    @Test
    fun secondaryLogonIsRegisteredAndEnabled() {
        val dir = Files.createTempDirectory("reg").toFile()
        val system = File(dir, "system.reg")
        system.writeText(
            "WINE REGISTRY Version 2\n;; All keys relative to \\\\Machine\n\n#arch=win64\n\n" +
                "[System\\\\ControlSet001\\\\Services\\\\Spooler] 1758569995\n#time=1dc2bf8ac65d5ca\n\"Start\"=dword:00000003\n",
        )
        WindowsSetup.apply(system)
        val text = system.readText()
        assertTrue("[System\\\\ControlSet001\\\\Services\\\\seclogon]" in text, text)
        assertTrue("\"Start\"=dword:00000003" in text.substringAfter("seclogon]"), text)
        assertTrue("\"DisplayName\"=\"Secondary Logon\"" in text, text)
        assertTrue("\"ImagePath\"=str(2):\"C:\\\\windows\\\\system32\\\\svchost.exe -k netsvcs -p\"" in text, text)
        assertTrue("Spooler" in text)
        // Twice changes nothing more (every start writes it).
        WindowsSetup.apply(system)
        assertTrue(Regex("Services\\\\\\\\seclogon]").findAll(system.readText()).count() == 1, system.readText())
    }
}
