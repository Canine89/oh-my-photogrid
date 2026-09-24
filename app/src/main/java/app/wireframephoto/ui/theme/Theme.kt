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
import androidx.compose.ui.text.font.FontSynthesis
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

    /** Candy-colored "jelly" family album: squishy glossy surfaces, rounded type, bouncy motion. */
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
    /** Surfaces render as glossy jelly and presses squash and wobble (see components/Jelly.kt). */
    val jelly: Boolean = false,
    /** Candy blobs drifting behind the page when [jelly]. */
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
 * "가족 앨범" as candy jelly. Family photo apps aim for warm, soft and safe; claymorphism-style
 * "digital texture" makes that tactile. Strawberry-milk page, honey and strawberry jelly for
 * the things you touch, mint and peach as friends, cocoa ink. Text colors keep 4.5:1 or better.
 */
private val Album = run {
    val milk = Color(0xFFFFF4EA)
    val white = Color(0xFFFFFFFF)
    val peachCream = Color(0xFFFFE7D9)
    val blush = Color(0xFFF4D5C4)
    val cocoa = Color(0xFF4A3228)
    val latte = Color(0xFF735649)
    val honey = Color(0xFFFFC94D)
    val strawberry = Color(0xFFC73A4A)
    val mint = Color(0xFF7FD1AE)
    val pink = Color(0xFFFF8FA3)
    WfPalette(
        isLight = true,
        bg = milk, card = Color(0xFFFFFBF7), well = peachCream, raised = white, line = blush, text = cocoa, textDim = latte,
        accent = honey, onAccent = cocoa, accentInk = strawberry, accent2 = mint, accent3 = pink,
        print = white, printHole = Color(0xFFFBE3D3),
        // A strawberry-milk silicone jelly case; the screen itself stays dark like a real phone.
        deviceBody = Color(0xFFFFB3C1), deviceEdge = Color(0xFFF48FA5), deviceScreen = Color(0xFF2E2226),
        glow = Color(0xFFFFB38A),
        colorScheme = lightColorScheme(
            primary = strawberry,
            onPrimary = white,
            primaryContainer = Color(0xFFFFE0E3),
            onPrimaryContainer = Color(0xFF5A1420),
            secondary = Color(0xFF2F7A5B),
            onSecondary = white,
            secondaryContainer = peachCream,
            onSecondaryContainer = cocoa,
            tertiary = Color(0xFF8A5A00),
            onTertiary = white,
            background = milk,
            onBackground = cocoa,
            surface = milk,
            onSurface = cocoa,
            surfaceVariant = peachCream,
            onSurfaceVariant = latte,
            surfaceContainerLowest = white,
            surfaceContainerLow = Color(0xFFFFFBF7),
            surfaceContainer = peachCream,
            surfaceContainerHigh = Color(0xFFFFE0CF),
            surfaceContainerHighest = blush,
            outline = Color(0xFF9C7B6C),
            outlineVariant = blush,
            inverseSurface = cocoa,
            inverseOnSurface = milk,
            error = Color(0xFFB3261E),
        ),
        jelly = true,
        backdrop = listOf(
            Color(0x66FFD66B), // honey
            Color(0x55FFB3C1), // strawberry milk
            Color(0x559FE3C4), // mint
            Color(0x4DA9D8FF), // sky
            Color(0x4DFFB38A), // peach
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

private val Jua = FontFamily(Font(R.font.jua))

/**
 * Album: Jua, a chunky rounded Korean face (OFL), for everything big or tappable; Pretendard 500
 * keeps long text readable. Jua has one weight, so bold is never faked (it would smear).
 */
private val AlbumType = pretendardType(heavy = 700, bold = 600, tight = 0.5f).run {
    fun TextStyle.jua(size: Float? = null) = copy(
        fontFamily = Jua,
        fontWeight = FontWeight.Normal,
        fontSynthesis = FontSynthesis.None,
        letterSpacing = 0.em,
        fontSize = size?.sp ?: fontSize,
    )
    fun TextStyle.body() = copy(fontWeight = FontWeight.Medium)
    copy(
        displayLarge = displayLarge.jua(), displayMedium = displayMedium.jua(), displaySmall = displaySmall.jua(),
        headlineLarge = headlineLarge.jua(), headlineMedium = headlineMedium.jua(), headlineSmall = headlineSmall.jua(),
        titleLarge = titleLarge.jua(), titleMedium = titleMedium.jua(17f), titleSmall = titleSmall.jua(15f),
        labelLarge = labelLarge.jua(15f), labelMedium = labelMedium.jua(13f),
        bodyLarge = bodyLarge.body(), bodyMedium = bodyMedium.body(), bodySmall = bodySmall.body(),
    )
}

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
