package com.logie.wowforever

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.logie.wowforever.look.CinzelStyle
import com.logie.wowforever.look.GlassButton
import com.logie.wowforever.look.Modern
import com.logie.wowforever.look.PillSegmented
import com.logie.wowforever.look.PlayCta
import com.logie.wowforever.look.RoundIcon
import com.logie.wowforever.look.SectionLabel
import com.logie.wowforever.look.Sheet
import com.logie.wowforever.look.Snow
import com.logie.wowforever.look.Tag
import com.logie.wowforever.look.box
import com.logie.wowforever.look.rememberArtPalette
import com.logie.wowforever.look.smallResource
import kotlinx.coroutines.delay

/**
 * The launcher, in logie-github/Blizzard's modern home layout: the game's art over the whole screen, the name in
 * Cinzel, Play and ••• at the bottom, the box art beside them on a tall screen.
 *
 * First time: **Install Battle.net** runs Blizzard's own installer. After that: **Play** asks Battle.net to start the
 * game (or opens Battle.net, with the switch to go back to the launcher), and opening the app starts it by itself
 * after a short countdown.
 */
@Composable
fun Home(autoStartAllowed: Boolean) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val state by Runtime.state.collectAsState()
    var installed by remember { mutableStateOf(Runtime.battleNetInstalled(context)) }
    var openBattleNet by remember { mutableStateOf(settings.openBattleNet) }
    var showSettings by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(if (autoStartAllowed && settings.autoStart) AUTO_START_S else 0) }

    LifecycleResumeEffect(Unit) {
        installed = Runtime.battleNetInstalled(context)
        onPauseOrDispose { countdown = 0 }
    }

    fun play() {
        countdown = 0
        Runtime.start(context, if (!installed) Runtime.Mode.SETUP else if (openBattleNet) Runtime.Mode.LAUNCHER else Runtime.Mode.GAME)
    }

    LaunchedEffect(countdown, installed) {
        if (countdown <= 0 || !installed) return@LaunchedEffect
        delay(1000)
        countdown -= 1
        if (countdown == 0) play()
    }

    val busy = state as? Runtime.State.Working
    val failed = state as? Runtime.State.Failed

    BoxWithConstraints(Modifier.fillMaxSize().background(Modern.Background)) {
        val wide = maxWidth >= 680.dp
        val tall = maxHeight >= 520.dp
        val screenWidth = maxWidth
        Backdrop()
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = if (wide) 28.dp else 20.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.forever_logo), null, Modifier.size(36.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.app_name), style = CinzelStyle.copy(fontSize = 17.sp))
                Spacer(Modifier.weight(1f))
                RoundIcon(Icons.Default.Settings, stringResource(R.string.wf_settings)) { showSettings = true }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Text("World of Warcraft", style = CinzelStyle.copy(fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = 0.02.sp), maxLines = 2)
                        Text("Forever", style = MaterialTheme.typography.headlineSmall.copy(color = Modern.Muted, fontWeight = FontWeight.Normal))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (installed) Tag(stringResource(R.string.wf_tag_installed), Modern.Good, filled = true)
                        else Tag(stringResource(R.string.wf_tag_not_installed), Modern.Warn, filled = true)
                        if (installed) Tag(stringResource(if (openBattleNet) R.string.wf_tag_launcher else R.string.wf_tag_game))
                    }
                    failed?.let {
                        Column(Modifier.widthIn(max = 520.dp).box(tint = Modern.Bad).padding(12.dp)) {
                            Text(stringResource(R.string.wf_failed), style = MaterialTheme.typography.titleSmall.copy(color = Modern.Bad))
                            Text(it.message, style = MaterialTheme.typography.bodySmall.copy(color = Modern.Text))
                        }
                    }
                    if (!installed && busy == null) {
                        Text(stringResource(R.string.wf_first_time), style = MaterialTheme.typography.bodySmall, modifier = Modifier.widthIn(max = 520.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val label = when {
                            !installed -> stringResource(R.string.wf_install)
                            countdown > 0 -> stringResource(R.string.wf_play_in, countdown)
                            openBattleNet -> stringResource(R.string.wf_open_battlenet)
                            else -> stringResource(R.string.wf_play)
                        }
                        PlayCta(label, if (installed) Icons.Default.PlayArrow else Icons.Default.Download, Modifier.widthIn(min = 210.dp), enabled = busy == null) {
                            Runtime.dismiss()
                            play()
                        }
                        if (countdown > 0) GlassButton(stringResource(R.string.wf_stay)) { countdown = 0 }
                        else if (installed && !openBattleNet && busy == null) GlassButton(stringResource(R.string.wf_battlenet), icon = Icons.Default.Storefront) {
                            Runtime.start(context, Runtime.Mode.LAUNCHER)
                        }
                        RoundIcon(Icons.Default.MoreHoriz, stringResource(R.string.wf_settings)) { showSettings = true }
                    }
                    AnimatedVisibility(busy != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(busy?.let { stepText(it) }.orEmpty(), style = MaterialTheme.typography.bodySmall.copy(color = Modern.Text))
                            val mod = Modifier.widthIn(max = 320.dp).fillMaxWidth().height(4.dp).clip(CircleShape)
                            val p = busy?.progress
                            if (p != null) LinearProgressIndicator(progress = { p }, color = Modern.AccentBright, trackColor = Color.White.copy(alpha = 0.1f), modifier = mod)
                            else LinearProgressIndicator(color = Modern.AccentBright, trackColor = Color.White.copy(alpha = 0.1f), modifier = mod)
                        }
                    }
                }
                if (wide && tall) {
                    Image(
                        painterResource(R.drawable.cover_wow), null, contentScale = ContentScale.Crop,
                        modifier = Modifier.width((screenWidth * 0.2f).coerceIn(170.dp, 260.dp)).aspectRatio(3f / 4f)
                            .shadow(28.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Modern.OutlineStrong, RoundedCornerShape(16.dp)),
                    )
                }
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            settings = settings,
            installed = installed,
            openBattleNet = openBattleNet,
            onOpenBattleNet = {
                settings.openBattleNet = it
                openBattleNet = it
            },
            onReinstall = {
                showSettings = false
                Runtime.start(context, Runtime.Mode.SETUP)
            },
            onClose = { showSettings = false },
        )
    }
}

@Composable
private fun stepText(working: Runtime.State.Working): String = stringResource(
    when (working.step) {
        Runtime.Step.RUNTIME -> R.string.wf_step_runtime
        Runtime.Step.DOWNLOAD -> R.string.wf_step_download
        Runtime.Step.WINDOWS -> R.string.wf_step_windows
        Runtime.Step.STARTING -> R.string.wf_step_starting
    },
)

@Composable
private fun Backdrop() {
    val drift by rememberInfiniteTransition(label = "drift").animateFloat(
        0f, 1f, infiniteRepeatable(tween(40_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "d",
    )
    Image(
        painterResource(R.drawable.wide_wow), null, contentScale = ContentScale.Crop, alignment = BiasAlignment(0.3f, -0.4f),
        modifier = Modifier.fillMaxSize().graphicsLayer {
            val z = 1.06f + 0.06f * drift
            scaleX = z; scaleY = z
            transformOrigin = TransformOrigin(0.7f, 0.3f)
            alpha = 0.9f
        },
    )
    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Modern.Background.copy(alpha = 0.92f), 0.55f to Modern.Background.copy(alpha = 0.35f), 1f to Color.Transparent)))
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Modern.Background.copy(alpha = 0.55f), 0.35f to Color.Transparent, 0.65f to Modern.Background.copy(alpha = 0.55f), 1f to Modern.Background)))
    val palette = rememberArtPalette(R.drawable.wide_wow) { smallResource(it, R.drawable.wide_wow) }
    Snow(flakes = 44, alpha = 0.7f, palette = palette)
}

@Composable
private fun SettingsSheet(
    settings: Settings,
    installed: Boolean,
    openBattleNet: Boolean,
    onOpenBattleNet: (Boolean) -> Unit,
    onReinstall: () -> Unit,
    onClose: () -> Unit,
) {
    var screen by remember { mutableStateOf(settings.screenSize) }
    var box64 by remember { mutableStateOf(settings.box64Preset) }
    var code by remember { mutableStateOf(settings.gameCode) }
    var auto by remember { mutableStateOf(settings.autoStart) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Sheet(stringResource(R.string.app_name), onClose, subtitle = "v${BuildConfig.VERSION_NAME}") {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingSwitch(stringResource(R.string.wf_setting_open_battlenet), stringResource(R.string.wf_setting_open_battlenet_note), openBattleNet, onOpenBattleNet)
                SettingSwitch(stringResource(R.string.wf_setting_auto), stringResource(R.string.wf_setting_auto_note), auto) {
                    auto = it
                    settings.autoStart = it
                }
                SectionLabel(stringResource(R.string.wf_setting_screen), Modifier.padding(top = 6.dp))
                PillSegmented(Settings.SCREEN_SIZES.map { it.replace("x", " × ") to it }, screen) {
                    screen = it
                    settings.screenSize = it
                }
                SectionLabel(stringResource(R.string.wf_setting_box64), Modifier.padding(top = 6.dp))
                PillSegmented(
                    listOf(stringResource(R.string.wf_box64_performance) to Settings.BOX64_PERFORMANCE, stringResource(R.string.wf_box64_stability) to Settings.BOX64_STABILITY),
                    box64,
                ) {
                    box64 = it
                    settings.box64Preset = it
                }
                SectionLabel(stringResource(R.string.wf_setting_code), Modifier.padding(top = 6.dp))
                OutlinedTextField(code, { code = it; settings.gameCode = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.wf_setting_code_note), style = MaterialTheme.typography.bodySmall)
                if (installed) {
                    SectionLabel(stringResource(R.string.wf_setting_installer), Modifier.padding(top = 6.dp))
                    GlassButton(stringResource(R.string.wf_run_installer), icon = Icons.Default.Download, onClick = onReinstall)
                }
                SectionLabel(stringResource(R.string.wf_setting_rules), Modifier.padding(top = 6.dp))
                Text(stringResource(R.string.wf_rules), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, note: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().box().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(note, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked, onChange)
    }
}

private const val AUTO_START_S = 3
