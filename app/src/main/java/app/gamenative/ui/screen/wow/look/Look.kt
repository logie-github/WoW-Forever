// SPDX-License-Identifier: GPL-3.0-or-later
// The modern look of logie-github/Blizzard (ui/Look.kt, ui/Theme.kt, ui/ModernHome.kt): its palette, Cinzel and Jost,
// the drifting snow, the Play button, the pill switch and the sheet.

package app.gamenative.ui.screen.wow.look

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.gamenative.R
import kotlin.math.PI
import kotlin.math.sin

/** Near-black blue surfaces, white type, Blizzard blue as the one accent, gold kept for small honours. */
object Modern {
    val Background = Color(0xFF070B11)
    val Surface = Color(0xFF0F151E)
    val SurfaceHigh = Color(0xFF161E2A)
    val SurfaceTop = Color(0xFF1E2836)
    val Outline = Color(0x1FFFFFFF)
    val OutlineStrong = Color(0x33FFFFFF)
    val Text = Color(0xFFF3F6FA)
    val Muted = Color(0xFF93A1B4)
    val Faint = Color(0xFF5F6C7E)
    val Accent = Color(0xFF1A8CFF)
    val AccentBright = Color(0xFF45B4FF)
    val AccentSoft = Color(0x261A8CFF)
    val Gold = Color(0xFFF0B84C)
    val Good = Color(0xFF39D98A)
    val Warn = Color(0xFFF5A524)
    val Bad = Color(0xFFFF5D5D)
    val Glow = Color(0xFFE6F3FF)

    val Corner = 16.dp
    val SheetCorner = 24.dp
    val ButtonCorner = 12.dp

    val AccentBrush = Brush.horizontalGradient(listOf(Color(0xFF1477E6), Color(0xFF2AA3FF)))
    val SheetBrush = Brush.verticalGradient(listOf(Color(0xFF131A25), Color(0xFF0C121A)))
}

@OptIn(ExperimentalTextApi::class)
val DisplayFont = FontFamily(
    Font(R.font.blizz_cinzel, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.blizz_cinzel, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(650))),
    Font(R.font.blizz_cinzel, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

val BodyFont = FontFamily(
    Font(R.font.blizz_jost_light, FontWeight.Light),
    Font(R.font.blizz_jost_regular, FontWeight.Normal),
    Font(R.font.blizz_jost_medium, FontWeight.Medium),
)

/** Cinzel, the Blizzard titles' face (the game's name). */
val CinzelStyle = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, color = Modern.Text, letterSpacing = 0.08.em)

private val Body = TextStyle(fontFamily = BodyFont, color = Modern.Text)

private val ModernTypography = Typography(
    headlineLarge = Body.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium),
    headlineSmall = Body.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
    titleLarge = Body.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    titleMedium = Body.copy(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium),
    titleSmall = Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    bodyLarge = Body.copy(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = Body.copy(fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = Body.copy(fontSize = 13.sp, lineHeight = 18.sp, color = Modern.Muted),
    labelLarge = Body.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    labelMedium = Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Modern.Muted),
)

/** The Blizzard launcher's theme: dialogs, menus and text fields inside follow it. */
@Composable
fun BlizzTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Modern.Accent, onPrimary = Color.White, secondary = Modern.Gold,
            background = Modern.Background, onBackground = Modern.Text, surface = Modern.Surface, onSurface = Modern.Text,
            surfaceVariant = Modern.SurfaceHigh, onSurfaceVariant = Modern.Muted, outline = Modern.OutlineStrong,
            surfaceContainer = Modern.SurfaceHigh, surfaceContainerHigh = Modern.SurfaceTop, surfaceContainerHighest = Modern.SurfaceTop,
            error = Modern.Bad,
        ),
        typography = ModernTypography,
        content = content,
    )
}

/** Particles drifting over a picture: snow, or embers rising over a fiery one, in the picture's own colours. */
@Composable
fun Snow(modifier: Modifier = Modifier, flakes: Int = 70, alpha: Float = 1f, color: Color = Modern.Glow, palette: ArtPalette? = null) {
    val motion = rememberInfiniteTransition(label = "snow")
    val time by motion.animateFloat(0f, 1f, infiniteRepeatable(tween(20_000, easing = LinearEasing)), label = "time")
    val seeds = remember(flakes) {
        val random = java.util.Random(7)
        List(flakes) { floatArrayOf(random.nextFloat(), random.nextFloat(), 0.4f + random.nextFloat() * 0.6f, random.nextFloat(), random.nextFloat()) }
    }
    val goesUp = palette?.warm ?: false
    Canvas(modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }) {
        for (seed in seeds) {
            val x0 = seed[0]; val y0 = seed[1]; val depth = seed[2]; val phase = seed[3]
            val travel = (y0 + time * (1f + 2f * depth)) % 1f
            val fall = if (goesUp) 1f - travel else travel
            val sway = sin((time * 6f + phase) * 2 * PI).toFloat() * (if (goesUp) 0.02f else 0.012f)
            val x = ((x0 + sway - time * 0.15f * depth) % 1f + 1f) % 1f
            val tint = palette?.pick(seed[4]) ?: color
            val glow = if (goesUp) 0.35f + 0.55f * (1f - fall).coerceIn(0f, 1f) else 0.25f + 0.5f * depth
            drawCircle(
                tint.copy(alpha = glow.coerceIn(0f, 1f)),
                radius = (0.8f + 1.8f * depth).dp.toPx(),
                center = Offset(x * size.width, fall * size.height),
            )
        }
    }
}

/** A sheet's surface: rounded, a hairline edge and a soft shadow. */
fun Modifier.panel(corner: Dp = Modern.SheetCorner, elevated: Boolean = true): Modifier {
    val shape = RoundedCornerShape(corner)
    return (if (elevated) this.shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black) else this)
        .clip(shape).background(Modern.SheetBrush).border(1.dp, Modern.Outline, shape)
}

/** A small rounded card inside a panel, tinted when [selected]. */
fun Modifier.box(selected: Boolean = false, corner: Dp = 12.dp, tint: Color? = null): Modifier {
    val shape = RoundedCornerShape(corner)
    return this.clip(shape)
        .background(tint?.copy(alpha = 0.12f) ?: if (selected) Modern.AccentSoft else Color.White.copy(alpha = 0.035f))
        .border(1.dp, tint?.copy(alpha = 0.5f) ?: if (selected) Modern.Accent.copy(alpha = 0.7f) else Modern.Outline, shape)
}

/** The big Play button, glowing softly in the accent blue. */
@Composable
fun PlayCta(label: String, icon: ImageVector, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 700f), label = "press")
    val haptic = LocalHapticFeedback.current
    val glow by rememberInfiniteTransition(label = "glow").animateFloat(
        0.25f, 0.55f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g",
    )
    Row(
        modifier
            .graphicsLayer { val k = 1f - 0.035f * press; scaleX = k; scaleY = k; alpha = if (enabled) 1f else 0.45f }
            .shadow(18.dp, RoundedCornerShape(16.dp), ambientColor = Modern.Accent.copy(alpha = glow), spotColor = Modern.Accent.copy(alpha = glow))
            .clip(RoundedCornerShape(16.dp))
            .background(Modern.AccentBrush)
            .drawWithContent { drawContent(); drawRect(Color.White.copy(alpha = 0.12f * press)) }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick()
            })
            .heightIn(min = 58.dp)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontSize = 19.sp), maxLines = 1)
    }
}

/** A second button: quiet glass with a hairline edge. */
@Composable
fun GlassButton(label: String, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color = Modern.Text, enabled: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 800f), label = "press")
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(Modern.ButtonCorner)
    Row(
        modifier
            .graphicsLayer { val k = 1f - 0.04f * press; scaleX = k; scaleY = k; alpha = if (enabled) 1f else 0.45f }
            .heightIn(min = 46.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.07f + 0.05f * press))
            .border(1.dp, Modern.Outline, shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick()
            })
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        icon?.let { Icon(it, null, tint = color, modifier = Modifier.size(18.dp)) }
        Text(label, style = MaterialTheme.typography.labelLarge.copy(color = color, fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A round icon button (the settings gear, the •••). */
@Composable
fun RoundIcon(icon: ImageVector, description: String, size: Dp = 44.dp, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, label = "press")
    Box(
        Modifier.size(size).graphicsLayer { val k = 1f - 0.06f * press; scaleX = k; scaleY = k }
            .clip(CircleShape).background(Color.White.copy(alpha = 0.08f + 0.06f * press)).border(1.dp, Modern.Outline, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = Modern.Text, modifier = Modifier.size(size * 0.5f)) }
}

/** A small rounded tag: "Up to date", "Update 1.15.8", "Login saved". */
@Composable
fun Tag(text: String, color: Color = Modern.Muted, filled: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall.copy(color = color, fontWeight = FontWeight.Medium, fontSize = 12.sp),
        maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clip(CircleShape)
            .background(if (filled) color.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** A pill-shaped segmented switch with a sliding highlight. */
@Composable
fun <T> PillSegmented(options: List<Pair<String, T>>, selected: T, modifier: Modifier = Modifier, enabled: Boolean = true, onSelect: (T) -> Unit) {
    val index = options.indexOfFirst { it.second == selected }.coerceAtLeast(0)
    BoxWithConstraints(
        modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Modern.Outline, RoundedCornerShape(22.dp)).padding(4.dp),
    ) {
        val cell = maxWidth / options.size.coerceAtLeast(1)
        val x by animateDpAsState(cell * index, spring(dampingRatio = 0.8f, stiffness = 500f), label = "pill")
        Box(Modifier.offset(x = x).width(cell).fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(Modern.AccentBrush))
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, (label, value) ->
                val on = i == index
                val color by animateColorAsState(if (on) Color.White else Modern.Muted, label = "pillText")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(18.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = enabled) { onSelect(value) },
                    contentAlignment = Alignment.Center,
                ) { Text(label, style = MaterialTheme.typography.labelLarge.copy(color = color, fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

/** A section heading inside a sheet: small, quiet, in capitals. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.bodySmall.copy(color = Modern.Muted, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp, fontSize = 11.sp),
        modifier = modifier,
    )
}

/** The standard popup: a sheet with its title, an optional subtitle, a close button and its content. */
@Composable
fun Sheet(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    maxWidth: Dp = 600.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.padding(14.dp).widthIn(max = maxWidth).fillMaxWidth().panel()) {
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 8.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Modern.Muted, modifier = Modifier.padding(top = 2.dp)) }
            }
            Box(
                Modifier.padding(6.dp).size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Close, "Close", tint = Modern.Text, modifier = Modifier.size(20.dp)) }
        }
        content()
    }
}
