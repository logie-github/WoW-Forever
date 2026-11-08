// SPDX-License-Identifier: GPL-3.0-or-later
// The setup screen in the layout of logie-github/Blizzard's modern home (ui/ModernHome.kt): the game's art fills the
// screen, a panel down the left, the game's name, Play and ••• at the bottom right, the box beside them.

package app.gamenative.ui.screen.wow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.gamenative.BuildConfig
import app.gamenative.R
import app.gamenative.ui.screen.wow.look.GlassButton
import app.gamenative.ui.screen.wow.look.Modern
import app.gamenative.ui.screen.wow.look.PillSegmented
import app.gamenative.ui.screen.wow.look.PlayCta
import app.gamenative.ui.screen.wow.look.RoundIcon
import app.gamenative.ui.screen.wow.look.SectionLabel
import app.gamenative.ui.screen.wow.look.Sheet
import app.gamenative.ui.screen.wow.look.Snow
import app.gamenative.ui.screen.wow.look.Tag
import app.gamenative.ui.screen.wow.look.CinzelStyle
import app.gamenative.ui.screen.wow.look.box
import app.gamenative.ui.screen.wow.look.rememberArtPalette
import app.gamenative.ui.screen.wow.look.smallResource

/** One line of the readiness list. */
internal data class Check(val label: String, val ready: Boolean)

/** A notice in the panel: an update, a failure. */
internal data class Notice(val title: String, val body: String, val color: Color)

/** What the home screen shows. */
internal data class HomeState(
    val edition: String,
    val checks: List<Check>,
    val notices: List<Notice>,
    val tags: List<Pair<String, Color>>,
    /** Work under way (installing, updating, starting), with its line, or null. */
    val busy: String?,
    val primary: Primary,
    /** A second action beside Play, if any (locate existing files, launch anyway). */
    val secondary: Secondary?,
)

internal data class Primary(val label: String, val icon: ImageVector, val enabled: Boolean, val onClick: () -> Unit)

internal data class Secondary(val label: String, val icon: ImageVector?, val color: Color, val onClick: () -> Unit)

internal object HomeIcons {
    val Play = Icons.Default.PlayArrow
    val Download = Icons.Default.Download
    val Refresh = Icons.Default.Refresh
    val Folder = Icons.Default.FolderOpen
}

@Composable
internal fun ForeverHome(state: HomeState, onOpenSettings: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Modern.Background)) {
        val wide = maxWidth >= 680.dp
        val tall = maxHeight >= 520.dp
        val screenWidth = maxWidth
        Backdrop()
        if (wide) {
            Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                StatusPanel(state, Modifier.width((screenWidth * 0.3f).coerceIn(260.dp, 360.dp)).fillMaxHeight().padding(16.dp))
                Column(Modifier.weight(1f).fillMaxHeight().padding(end = 28.dp, top = 16.dp, bottom = 24.dp)) {
                    TopBar(Modifier.fillMaxWidth(), showLogo = false, onOpenSettings)
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        Hero(state, onOpenSettings, Modifier.weight(1f))
                        if (tall) Cover(Modifier.width((screenWidth * 0.2f).coerceIn(170.dp, 260.dp)))
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                TopBar(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), showLogo = true, onOpenSettings)
                StatusPanel(state, Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), showLogo = false)
                Hero(state, onOpenSettings, Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

/** The art, filling the screen, drifting slowly, darkened toward the words; snow in its own colours over it. */
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
private fun TopBar(modifier: Modifier, showLogo: Boolean, onOpenSettings: () -> Unit) {
    Row(modifier.height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showLogo) Logo()
        Spacer(Modifier.weight(1f))
        RoundIcon(Icons.Default.Settings, "Settings", onClick = onOpenSettings)
    }
}

@Composable
private fun Logo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.forever_logo), contentDescription = null, modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(10.dp))
        Text("WoW Forever", style = CinzelStyle.copy(fontSize = 17.sp), maxLines = 1)
    }
}

/** The panel down the left (the Blizzard home's library): the logo, the readiness list and any notices. */
@Composable
private fun StatusPanel(state: HomeState, modifier: Modifier, showLogo: Boolean = true) {
    Column(
        modifier.clip(RoundedCornerShape(Modern.SheetCorner)).background(Modern.Surface.copy(alpha = 0.78f))
            .border(1.dp, Modern.Outline, RoundedCornerShape(Modern.SheetCorner)),
    ) {
        if (showLogo) Box(Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp)) { Logo() }
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = if (showLogo) 4.dp else 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (notice in state.notices) {
                Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).box(tint = notice.color).padding(12.dp)) {
                    Text(notice.title, style = MaterialTheme.typography.titleSmall.copy(color = notice.color))
                    Spacer(Modifier.height(4.dp))
                    Text(notice.body, style = MaterialTheme.typography.bodySmall.copy(color = Modern.Text.copy(alpha = 0.85f)))
                }
            }
            SectionLabel("Ready to play", Modifier.padding(start = 8.dp, top = 4.dp, bottom = 6.dp))
            for (check in state.checks) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (check.ready) Icons.Default.CheckCircle else Icons.Default.Warning, null,
                        tint = if (check.ready) Modern.Good else Modern.Warn, modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(check.label, style = MaterialTheme.typography.bodySmall.copy(color = Modern.Text.copy(alpha = 0.88f)), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** The game: its name, its tags, Play and •••, and the work going on under them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Hero(state: HomeState, onOpenSettings: () -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text("World of Warcraft", style = CinzelStyle.copy(fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = 0.02.sp), maxLines = 2)
            Text(
                "Forever · ${state.edition} Edition",
                style = MaterialTheme.typography.headlineSmall.copy(color = Modern.Muted, fontWeight = FontWeight.Normal, fontSize = 18.sp),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (state.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((text, color) in state.tags) Tag(text, color, filled = color != Modern.Muted)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val p = state.primary
            PlayCta(p.label, p.icon, Modifier.widthIn(min = 210.dp), enabled = p.enabled && state.busy == null, onClick = p.onClick)
            state.secondary?.takeIf { state.busy == null }?.let { s -> GlassButton(s.label, icon = s.icon, color = s.color, onClick = s.onClick) }
            RoundIcon(Icons.Default.MoreHoriz, "More", onClick = onOpenSettings)
        }
        AnimatedVisibility(state.busy != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.busy.orEmpty(), style = MaterialTheme.typography.bodySmall.copy(color = Modern.Text.copy(alpha = 0.85f)), maxLines = 2)
                LinearProgressIndicator(
                    color = Modern.AccentBright, trackColor = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth().height(4.dp).clip(CircleShape),
                )
            }
        }
    }
}

/** The game's box beside the words on a tall screen. */
@Composable
private fun Cover(modifier: Modifier) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
    Image(
        painterResource(R.drawable.cover_wow), "World of Warcraft", contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxWidth().aspectRatio(3f / 4f)
            .graphicsLayer { alpha = appear.value; translationY = (1f - appear.value) * 18.dp.toPx(); rotationY = -6f * (1f - appear.value) }
            .shadow(28.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Modern.OutlineStrong, RoundedCornerShape(16.dp)),
    )
}

/** What the settings sheet changes. */
internal data class SettingsState(
    val region: GameRegion,
    val resolution: RenderResolution,
    val touchControls: Boolean,
    val hasSavedLogin: Boolean,
    val gamePath: String,
    val enabled: Boolean,
)

/** The ••• and the gear: region, resolution, touch controls, the Battle.net login and the game's folder. */
@Composable
internal fun SettingsSheet(
    state: SettingsState,
    onClose: () -> Unit,
    onRegion: (GameRegion) -> Unit,
    onResolution: (RenderResolution) -> Unit,
    onTouchControls: (Boolean) -> Unit,
    onLogin: () -> Unit,
    onForgetLogin: () -> Unit,
    onChangeLocation: () -> Unit,
    onRefresh: () -> Unit,
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Sheet("World of Warcraft", onClose, subtitle = "Forever · v${BuildConfig.VERSION_NAME}") {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionLabel("Region")
                PillSegmented(GameRegion.entries.map { it.label to it }, state.region, enabled = state.enabled, onSelect = onRegion)
                SectionLabel("Resolution", Modifier.padding(top = 6.dp))
                PillSegmented(RenderResolution.entries.map { it.label to it }, state.resolution, enabled = state.enabled, onSelect = onResolution)
                SectionLabel("Touch controls", Modifier.padding(top = 6.dp))
                PillSegmented(listOf("Blizzard gestures" to true, "Touchpad" to false), state.touchControls, onSelect = onTouchControls)
                Text(
                    if (state.touchControls) {
                        "Left half: swipe and hold to walk. Right half: drag to turn the camera. Tap clicks, double tap jumps or attacks, " +
                            "hold right-clicks. Two fingers: pinch to zoom, tap for Tab, swipe up for chat. Three fingers: keyboard. Four: menu."
                    } else {
                        "Winlator's touchpad: the screen moves the mouse."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                SectionLabel("Battle.net login", Modifier.padding(top = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(if (state.hasSavedLogin) "Update login" else "Configure login", icon = Icons.Default.Key, onClick = onLogin)
                    if (state.hasSavedLogin) GlassButton("Forget", icon = Icons.Default.Delete, color = Modern.Bad, onClick = onForgetLogin)
                }
                SectionLabel("Game folder", Modifier.padding(top = 6.dp))
                Text(state.gamePath, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton("Change location", icon = Icons.Default.FolderOpen, enabled = state.enabled, onClick = onChangeLocation)
                    GlassButton("Refresh status", icon = Icons.Default.Refresh, onClick = onRefresh)
                }
            }
        }
    }
}
