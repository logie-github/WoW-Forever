package com.logie.wowforever

import android.content.Context
import android.content.Intent
import com.winlator.XServerDisplayActivity
import com.winlator.container.Container
import com.winlator.container.ContainerManager
import com.winlator.xenvironment.RootFSInstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

/**
 * Runs Blizzard's own programs, unmodified, on Winlator (Wine, Box64 and its X server):
 *
 * 1. Winlator's runtime is installed the first time (and after an update that changes it);
 * 2. one Windows (a Winlator container) is made the first time;
 * 3. the first time, Blizzard's own Battle.net installer is downloaded from Blizzard ([INSTALLER_URL]) and run:
 *    the player installs Battle.net, signs in and installs World of Warcraft in it, as on a PC;
 * 4. after that, Battle.net is started ([Mode.LAUNCHER]) or asked to start the game ([Mode.GAME]: its own
 *    `--exec` option). Battle.net keeps the sign-in itself; this app never sees or stores a password.
 *
 * The app downloads nothing else from Blizzard, changes no game file and talks to no Blizzard server.
 */
object Runtime {
    /** Blizzard's own download link for the Battle.net installer (Windows). */
    const val INSTALLER_URL = "https://downloader.battle.net/download/getInstaller?os=win&installer=Battle.net-Setup.exe"

    enum class Mode { SETUP, LAUNCHER, GAME }

    sealed interface State {
        data object Idle : State
        data class Working(val step: Step, val progress: Float? = null) : State
        data class Failed(val message: String) : State
    }

    enum class Step { RUNTIME, DOWNLOAD, WINDOWS, STARTING }

    private const val CONTAINER_NAME = "WoW Forever"

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Where the installer is kept: the app's own folder, drive D: in Windows. */
    fun downloads(context: Context): File = File(context.filesDir, "downloads").apply { mkdirs() }

    private fun installerFile(context: Context) = File(downloads(context), "Battle.net-Setup.exe")

    /** The Windows' C: drive, once it has been made. */
    private fun driveC(context: Context): File? =
        runCatching { ContainerManager(context).containers.firstOrNull { it.name == CONTAINER_NAME } }.getOrNull()
            ?.let { File(it.rootDir, ".wine/drive_c") }

    private fun battleNetDir(driveC: File) = File(driveC, "Program Files (x86)/Battle.net")

    /** Whether Battle.net has been installed in the Windows (its launcher is there). */
    fun battleNetInstalled(context: Context): Boolean =
        driveC(context)?.let { File(battleNetDir(it), "Battle.net Launcher.exe").isFile } == true

    fun dismiss() {
        if (_state.value is State.Failed) _state.value = State.Idle
    }

    fun start(context: Context, mode: Mode) {
        if (_state.value is State.Working) return
        val app = context.applicationContext
        _state.value = State.Working(Step.RUNTIME)
        scope.launch {
            try {
                run(app, mode)
                _state.value = State.Idle
            } catch (e: kotlinx.coroutines.CancellationException) {
                _state.value = State.Idle
                throw e
            } catch (e: Throwable) {
                // (also an Error: Winlator's native libraries failing to load is an UnsatisfiedLinkError)
                _state.value = State.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    private suspend fun run(context: Context, mode: Mode) {
        _state.value = State.Working(Step.RUNTIME, 0f)
        val installed = withContext(Dispatchers.IO) {
            RootFSInstaller.installIfNeeded(context) { percent -> _state.value = State.Working(Step.RUNTIME, percent / 100f) }
        }
        if (!installed) throw IllegalStateException(context.getString(R.string.wf_error_runtime))

        _state.value = State.Working(Step.WINDOWS)
        val settings = Settings(context)
        val container = container(context, settings)
        // What Battle.net checks Windows for (the Secondary Logon service), in the registry before Wine starts.
        withContext(Dispatchers.IO) { WindowsSetup.apply(container) }
        val driveC = File(container.rootDir, ".wine/drive_c")

        val (program, args) = when (mode) {
            Mode.SETUP -> {
                _state.value = State.Working(Step.DOWNLOAD, 0f)
                val file = withContext(Dispatchers.IO) { downloadInstaller(context) }
                file to ""
            }
            Mode.LAUNCHER -> File(battleNetDir(driveC), "Battle.net Launcher.exe") to ""
            // Battle.net's own option to start a game it has installed.
            Mode.GAME -> File(battleNetDir(driveC), "Battle.net.exe") to "--exec=\"launch ${settings.gameCode}\""
        }
        if (!program.isFile) throw IllegalStateException(context.getString(R.string.wf_error_no_battlenet))

        _state.value = State.Working(Step.STARTING)
        val intent = Intent(context, XServerDisplayActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("container_id", container.id)
            .putExtra("exec_path", program.path)
            .putExtra("exec_args", args)
            // The app's touch controls (GestureView) in place of Winlator's touchpad.
            .putExtra("gestures", true)
        context.startActivity(intent)
    }

    /** Blizzard's installer, from Blizzard's own link, kept once downloaded. */
    private fun downloadInstaller(context: Context): File {
        val target = installerFile(context)
        if (target.isFile && target.length() > 0) return target
        val part = File(target.path + ".part")
        var url = URL(INSTALLER_URL)
        repeat(5) {
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            val code = connection.responseCode
            if (code in 300..399) {
                url = URL(url, connection.getHeaderField("Location"))
                connection.disconnect()
                return@repeat
            }
            if (code != 200) throw IllegalStateException(context.getString(R.string.wf_error_download, code))
            val total = connection.contentLengthLong
            var done = 0L
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        if (total > 0) _state.value = State.Working(Step.DOWNLOAD, done.toFloat() / total)
                    }
                }
            }
            connection.disconnect()
            if (!part.renameTo(target)) throw IllegalStateException(context.getString(R.string.wf_error_download, -1))
            return target
        }
        throw IllegalStateException(context.getString(R.string.wf_error_download, -2))
    }

    /** The Windows: made the first time; its screen, graphics and Box64 set at every start. */
    private suspend fun container(context: Context, settings: Settings): Container {
        val manager = withContext(Dispatchers.IO) { ContainerManager(context) }
        val drives = "D:" + downloads(context).path
        val adreno = runCatching { com.winlator.core.GPUHelper.getAdrenoModelId(context) > 0 }.getOrDefault(false)
        val graphicsDriver = if (adreno) "turnip,gladio" else "vortek,gladio"
        val existing = manager.containers.firstOrNull { it.name == CONTAINER_NAME }
        if (existing != null) {
            existing.setScreenSize(settings.screenSize)
            existing.setDrives(drives)
            existing.setGraphicsDriver(graphicsDriver)
            existing.setDXWrapper("dxvk")
            existing.setBox64Preset(settings.box64Preset)
            withContext(Dispatchers.IO) { existing.saveData() }
            return existing
        }
        val defaults = Container(0)
        val data = JSONObject()
            .put("name", CONTAINER_NAME)
            .put("screenSize", settings.screenSize)
            .put("envVars", defaults.getEnvVars())
            .put("graphicsDriver", graphicsDriver)
            .put("dxwrapper", "dxvk")
            .put("audioDriver", defaults.getAudioDriver())
            .put("wincomponents", defaults.getWinComponents())
            .put("drives", drives)
            .put("hudMode", defaults.getHUDMode().toInt())
            .put("startupSelection", defaults.getStartupSelection().toInt())
            .put("box64Preset", settings.box64Preset)
            .put("desktopTheme", defaults.getDesktopTheme())
        val created = suspendCancellableCoroutine { done ->
            manager.createContainerAsync(data) { container -> done.resume(container) }
        }
        return created ?: throw IllegalStateException(context.getString(R.string.wf_error_windows))
    }
}
