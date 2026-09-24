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

    /** Warm liquid glass: frosted panes over the family photos' own colors. */
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
data class WfPalette(
    val isLight: Boolean,
    val bg: Color,
    val card: Color,
    val well: Color,
    val raised: Color,
    val line: Color,
    val text: Color,
    val textDim: Color,
    /** Fill for selections and custom buttons; [onAccent] is its text/icon color. */
    val accent: Color,
    val onAccent: Color,
    /** Accent for text, rings and icons on the page (text-safe; equals [accent] in Darkroom). */
    val accentInk: Color,
    val accent2: Color,
    val accent3: Color,
    val print: Color,
    val printHole: Color,
    val deviceBody: Color,
    val deviceEdge: Color,
    val deviceScreen: Color,
    val glow: Color,
    val colorScheme: ColorScheme,
    /** Surfaces render as frosted glass over a warm backdrop (see components/Glass.kt). */
    val glass: Boolean = false,
    /** Warm color blobs behind the glass on screens without photos, when [glass]. */
    val backdrop: List<Color> = emptyList(),
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
        accent = lime, onAccent = onLime, accentInk = lime, accent2 = violet, accent3 = coral,
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
 * "가족 앨범" as warm liquid glass (the iOS 26-era translucent look, warmed for family photos).
 * Surfaces are frosted panes; the color comes from what is behind them — soft peach, rose, sky,
 * sage and lilac on the home screen, the user's own photos in the editor. One coral accent, warm
 * near-black ink, no yellow or beige cast. Text colors keep 4.5:1 or better.
 */
private val Album = run {
    val page = Color(0xFFF8F5F3)
    val white = Color(0xFFFFFFFF)
    val pearl = Color(0xFFFFFBF7)
    val mist = Color(0xFFEFE4DA)
    val ink = Color(0xFF2E2320)
    val taupe = Color(0xFF66554C)
    val coral = Color(0xFFC4513A)
    val coralInk = Color(0xFFA8432F)
    WfPalette(
        isLight = true,
        bg = page, card = pearl, well = white.copy(alpha = 0.45f), raised = white, line = Color(0xFFE6D9CE), text = ink, textDim = taupe,
        accent = coral, onAccent = white, accentInk = coralInk, accent2 = Color(0xFF7FA88A), accent3 = Color(0xFF7FB3DA),
        print = white, printHole = mist,
        // A frosted glass phone; the screen itself stays dark like a real one.
        deviceBody = white.copy(alpha = 0.55f), deviceEdge = white, deviceScreen = Color(0xFF2A2220),
        glow = Color(0xFFFFB08A),
        colorScheme = lightColorScheme(
            primary = coral,
            onPrimary = white,
            primaryContainer = Color(0xFFFFE1D8),
            onPrimaryContainer = Color(0xFF5A1C0E),
            secondary = Color(0xFF4F6F58),
            onSecondary = white,
            secondaryContainer = mist,
            onSecondaryContainer = ink,
            tertiary = Color(0xFF3F6A8A),
            onTertiary = white,
            background = page,
            onBackground = ink,
            surface = page,
            onSurface = ink,
            surfaceVariant = mist,
            onSurfaceVariant = taupe,
            surfaceContainerLowest = white,
            surfaceContainerLow = pearl,
            surfaceContainer = mist,
            surfaceContainerHigh = Color(0xFFE9DDD2),
            surfaceContainerHighest = Color(0xFFE2D4C8),
            outline = Color(0xFF8E7C72),
            outlineVariant = Color(0xFFE6D9CE),
            inverseSurface = ink,
            inverseOnSurface = page,
            error = Color(0xFFB3261E),
        ),
        glass = true,
        backdrop = listOf(
            Color(0xE6FFB08A), // peach
            Color(0xB3D9CCF5), // lilac
            Color(0xCCA8D4F5), // sky
            Color(0xCCBFDDB0), // sage
            Color(0xB3F5B5BE), // rose
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
    val AccentInk: Color @Composable @ReadOnlyComposable get() = LocalWfPalette.current.accentInk
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

/** Album: the same Pretendard, one step lighter with half the tightening — calm, not technical. */
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
