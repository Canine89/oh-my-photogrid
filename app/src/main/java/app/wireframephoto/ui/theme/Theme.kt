package app.wireframephoto.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.wireframephoto.R

/** The two looks the user can pick on the home screen. Saved per install; Darkroom is the default. */
enum class AppTheme(val label: String) {
    /** Always dark, near-neutral, lime accent: photos carry all the color. */
    Darkroom("다크룸"),

    /** Warm cream "family album" pages, terracotta accent, softer type: cozy rather than technical. */
    Album("가족 앨범"),
}

private const val PREFS = "settings"
private const val KEY_THEME = "theme"

fun Context.loadAppTheme(): AppTheme =
    getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_THEME, null)
        ?.let { name -> AppTheme.entries.firstOrNull { it.name == name } } ?: AppTheme.Darkroom

fun Context.saveAppTheme(theme: AppTheme) {
    getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_THEME, theme.name).apply()
}

/**
 * Role-based colors shared by both themes. Surfaces go Bg → Card (sheets, dialogs) → Well (chips,
 * idle tiles) → Raised (floating bar, selected tile). Device* draw phone bodies, which stay dark
 * in every theme; Print is the white of photo paper (thumbnails, the saved-collage print).
 */
@Immutable
class WfPalette(
    val isLight: Boolean,
    val bg: Color,
    val card: Color,
    val well: Color,
    val raised: Color,
    val line: Color,
    val text: Color,
    val textDim: Color,
    val accent: Color,
    val onAccent: Color,
    val accent2: Color,
    val accent3: Color,
    val print: Color,
    val printHole: Color,
    val deviceBody: Color,
    val deviceEdge: Color,
    val deviceScreen: Color,
    val glow: Color,
    val colorScheme: ColorScheme,
)

/**
 * "Darkroom" design language: an always-dark, near-neutral UI so photos carry all the color,
 * one signature accent (Wire Lime) for the things you can act on, big friendly type and soft,
 * very round shapes. Motion is spring-based and switches off with the system "remove animations".
 */
private val Darkroom = run {
    val ink = Color(0xFF0B0B0E)
    val graphite = Color(0xFF131317)
    val slate = Color(0xFF1B1B21)
    val steel = Color(0xFF25252D)
    val fog = Color(0xFF3A3A45)
    val paper = Color(0xFFF4F4F6)
    val mist = Color(0xFFA3A3AE)
    val lime = Color(0xFFD4FF4F)
    val onLime = Color(0xFF141A00)
    val violet = Color(0xFFA78BFA)
    val coral = Color(0xFFFF7A59)
    WfPalette(
        isLight = false,
        bg = ink, card = graphite, well = slate, raised = steel, line = fog, text = paper, textDim = mist,
        accent = lime, onAccent = onLime, accent2 = violet, accent3 = coral,
        print = paper, printHole = steel,
        deviceBody = steel, deviceEdge = fog, deviceScreen = graphite,
        glow = lime,
        colorScheme = darkColorScheme(
            primary = lime,
            onPrimary = onLime,
            primaryContainer = Color(0xFF2E3A0A),
            onPrimaryContainer = lime,
            secondary = violet,
            onSecondary = Color(0xFF1B1036),
            secondaryContainer = steel,
            onSecondaryContainer = paper,
            tertiary = coral,
            onTertiary = Color(0xFF2A0C03),
            background = ink,
            onBackground = paper,
            surface = ink,
            onSurface = paper,
            surfaceVariant = slate,
            onSurfaceVariant = mist,
            surfaceContainerLowest = Color(0xFF08080A),
            surfaceContainerLow = graphite,
            surfaceContainer = slate,
            surfaceContainerHigh = steel,
            surfaceContainerHighest = Color(0xFF2E2E37),
            outline = fog,
            outlineVariant = Color(0xFF2A2A33),
            inverseSurface = paper,
            inverseOnSurface = ink,
            error = Color(0xFFFF6B6B),
        ),
    )
}

/**
 * "Family album": the warmth of printed photos in a paper album. Cream pages, white prints,
 * cocoa text, a terracotta accent with sage and butter as friends. Every text color keeps
 * 4.5:1 or better on the cream background.
 */
private val Album = run {
    val cream = Color(0xFFFAF5EE)
    val white = Color(0xFFFFFFFF)
    val linen = Color(0xFFF4ECE1)
    val sand = Color(0xFFE6DACB)
    val cocoa = Color(0xFF3B2F2A)
    val bark = Color(0xFF6E5F55)
    val terracotta = Color(0xFFB94E34)
    val sage = Color(0xFF56724B)
    val butter = Color(0xFFEDB94C)
    WfPalette(
        isLight = true,
        bg = cream, card = Color(0xFFFFFEFB), well = linen, raised = white, line = sand, text = cocoa, textDim = bark,
        accent = terracotta, onAccent = white, accent2 = sage, accent3 = butter,
        print = white, printHole = Color(0xFFEADFD1),
        deviceBody = Color(0xFF3A322D), deviceEdge = Color(0xFF564A43), deviceScreen = Color(0xFF2A2420),
        glow = Color(0xFFF4A383),
        colorScheme = lightColorScheme(
            primary = terracotta,
            onPrimary = white,
            primaryContainer = Color(0xFFFBE1D5),
            onPrimaryContainer = Color(0xFF5A1E0E),
            secondary = sage,
            onSecondary = white,
            secondaryContainer = linen,
            onSecondaryContainer = cocoa,
            tertiary = Color(0xFF9A6B12),
            onTertiary = white,
            background = cream,
            onBackground = cocoa,
            surface = cream,
            onSurface = cocoa,
            surfaceVariant = linen,
            onSurfaceVariant = bark,
            surfaceContainerLowest = white,
            surfaceContainerLow = Color(0xFFFFFEFB),
            surfaceContainer = linen,
            surfaceContainerHigh = Color(0xFFEFE6DA),
            surfaceContainerHighest = sand,
            outline = Color(0xFF8E7D70),
            outlineVariant = Color(0xFFE8DCCD),
            inverseSurface = cocoa,
            inverseOnSurface = cream,
            error = Color(0xFFBA1A1A),
        ),
    )
}

fun AppTheme.palette(): WfPalette = when (this) {
    AppTheme.Darkroom -> Darkroom
    AppTheme.Album -> Album
}

val LocalWfPalette = staticCompositionLocalOf { Darkroom }
val LocalAppTheme = staticCompositionLocalOf { AppTheme.Darkroom }

/** Design tokens. Colors follow the chosen [AppTheme]; shapes and motion are shared. */
object Wf {
    val Bg: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.bg
    val Card: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.card
    val Well: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.well
    val Raised: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.raised
    val Line: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.line
    val Text: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.text
    val TextDim: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.textDim

    /** Signature accent: selection, primary actions, focus. Used sparingly so photos stay the hero. */
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.accent
    val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.onAccent
    val Accent2: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.accent2
    val Accent3: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.accent3
    val Print: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.print
    val PrintHole: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.printHole
    val DeviceBody: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.deviceBody
    val DeviceEdge: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.deviceEdge
    val DeviceScreen: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.deviceScreen

    /** Soft glow behind the canvas and hero artwork. */
    val GlowBrush: Brush @Composable @ReadOnlyComposable get() = LocalWfPalette.current.glow.let {
        Brush.radialGradient(listOf(it.copy(alpha = 0.2f), it.copy(alpha = 0f)))
    }

    val PillShape = RoundedCornerShape(percent = 50)
    val CardShape = RoundedCornerShape(28.dp)
    val SheetShape = RoundedCornerShape(32.dp)

    /** Expressive springs: a lively one for spatial moves, a calm one for colors/opacity. */
    fun <T> bouncy() = spring<T>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
    fun <T> snappy() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
}

/** True when the user turned animations off in system settings; decorative motion must stop. */
val LocalReducedMotion = staticCompositionLocalOf { false }

private val Pretendard = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(
            R.font.pretendard_variable,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    },
)

/**
 * Pretendard throughout, heavier and tighter at display sizes. Korean wraps per syllable by
 * default ("콜라 / 주"); phrase-based word break (Korean-tagged) keeps words together.
 * [heavy]/[bold] set the display and title weights; [tight] scales the negative tracking.
 */
private fun pretendardType(heavy: Int, bold: Int, tight: Float) = Typography().run {
    fun TextStyle.k(weight: Int, tracking: Float = 0f) = copy(
        fontFamily = Pretendard,
        fontWeight = FontWeight(weight),
        letterSpacing = (tracking * tight).em,
        lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase),
        localeList = LocaleList("ko-KR"),
    )
    copy(
        displayLarge = displayLarge.k(heavy, -0.03f), displayMedium = displayMedium.k(heavy, -0.03f),
        displaySmall = displaySmall.k(heavy, -0.025f),
        headlineLarge = headlineLarge.k(heavy, -0.025f), headlineMedium = headlineMedium.k(heavy, -0.02f),
        headlineSmall = headlineSmall.k(bold, -0.015f),
        titleLarge = titleLarge.k(bold, -0.01f), titleMedium = titleMedium.k(bold), titleSmall = titleSmall.k(600),
        bodyLarge = bodyLarge.k(400), bodyMedium = bodyMedium.k(400), bodySmall = bodySmall.k(400),
        labelLarge = labelLarge.k(600), labelMedium = labelMedium.k(600), labelSmall = labelSmall.k(600).copy(letterSpacing = 0.01.em, fontSize = 11.sp),
    )
}

private val DarkroomType = pretendardType(heavy = 800, bold = 700, tight = 1f)

/** Rounder, calmer headlines for the album look: one step lighter, half the tightening. */
private val AlbumType = pretendardType(heavy = 700, bold = 600, tight = 0.5f)

private val DarkroomShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun WireframeTheme(theme: AppTheme = AppTheme.Darkroom, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val reducedMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val palette = theme.palette()
    CompositionLocalProvider(
        LocalReducedMotion provides reducedMotion,
        LocalWfPalette provides palette,
        LocalAppTheme provides theme,
    ) {
        MaterialTheme(
            colorScheme = palette.colorScheme,
            typography = if (theme == AppTheme.Album) AlbumType else DarkroomType,
            shapes = DarkroomShapes,
            content = content,
        )
    }
}
