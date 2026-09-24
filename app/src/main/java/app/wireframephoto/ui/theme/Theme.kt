package app.wireframephoto.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

/**
 * "Darkroom" design language: an always-dark, near-neutral UI so photos carry all the color,
 * one signature accent (Wire Lime) for the things you can act on, big friendly type and soft,
 * very round shapes. Motion is spring-based and switches off with the system "remove animations".
 */
object Wf {
    val Ink = Color(0xFF0B0B0E)
    val Graphite = Color(0xFF131317)
    val Slate = Color(0xFF1B1B21)
    val Steel = Color(0xFF25252D)
    val Fog = Color(0xFF3A3A45)
    val Paper = Color(0xFFF4F4F6)
    val Mist = Color(0xFFA3A3AE)

    /** Signature accent: selection, primary actions, focus. Used sparingly so photos stay the hero. */
    val Lime = Color(0xFFD4FF4F)
    val OnLime = Color(0xFF141A00)
    val Coral = Color(0xFFFF7A59)
    val Violet = Color(0xFFA78BFA)
    val Sky = Color(0xFF7DD3FC)

    /** Soft glow behind the canvas and hero artwork. */
    val GlowBrush = Brush.radialGradient(listOf(Color(0x33D4FF4F), Color(0x00D4FF4F)))
    val CtaBrush = Brush.linearGradient(listOf(Lime, Color(0xFF9DF26B)))

    val PillShape = RoundedCornerShape(percent = 50)
    val CardShape = RoundedCornerShape(28.dp)
    val SheetShape = RoundedCornerShape(32.dp)

    /** Expressive springs: a lively one for spatial moves, a calm one for colors/opacity. */
    fun <T> bouncy() = spring<T>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
    fun <T> snappy() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
}

/** True when the user turned animations off in system settings; decorative motion must stop. */
val LocalReducedMotion = staticCompositionLocalOf { false }

private val DarkroomColors = darkColorScheme(
    primary = Wf.Lime,
    onPrimary = Wf.OnLime,
    primaryContainer = Color(0xFF2E3A0A),
    onPrimaryContainer = Wf.Lime,
    secondary = Wf.Violet,
    onSecondary = Color(0xFF1B1036),
    secondaryContainer = Wf.Steel,
    onSecondaryContainer = Wf.Paper,
    tertiary = Wf.Coral,
    onTertiary = Color(0xFF2A0C03),
    background = Wf.Ink,
    onBackground = Wf.Paper,
    surface = Wf.Ink,
    onSurface = Wf.Paper,
    surfaceVariant = Wf.Slate,
    onSurfaceVariant = Wf.Mist,
    surfaceContainerLowest = Color(0xFF08080A),
    surfaceContainerLow = Wf.Graphite,
    surfaceContainer = Wf.Slate,
    surfaceContainerHigh = Wf.Steel,
    surfaceContainerHighest = Color(0xFF2E2E37),
    outline = Wf.Fog,
    outlineVariant = Color(0xFF2A2A33),
    inverseSurface = Wf.Paper,
    inverseOnSurface = Wf.Ink,
    error = Color(0xFFFF6B6B),
)

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
 */
private val DarkroomType = Typography().run {
    fun TextStyle.k(weight: Int, tracking: Float = 0f) = copy(
        fontFamily = Pretendard,
        fontWeight = FontWeight(weight),
        letterSpacing = tracking.em,
        lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase),
        localeList = LocaleList("ko-KR"),
    )
    copy(
        displayLarge = displayLarge.k(800, -0.03f), displayMedium = displayMedium.k(800, -0.03f),
        displaySmall = displaySmall.k(800, -0.025f),
        headlineLarge = headlineLarge.k(800, -0.025f), headlineMedium = headlineMedium.k(800, -0.02f),
        headlineSmall = headlineSmall.k(700, -0.015f),
        titleLarge = titleLarge.k(700, -0.01f), titleMedium = titleMedium.k(700), titleSmall = titleSmall.k(600),
        bodyLarge = bodyLarge.k(400), bodyMedium = bodyMedium.k(400), bodySmall = bodySmall.k(400),
        labelLarge = labelLarge.k(600), labelMedium = labelMedium.k(600), labelSmall = labelSmall.k(600, 0.01f).copy(fontSize = 11.sp),
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
fun WireframeTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val reducedMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialTheme(colorScheme = DarkroomColors, typography = DarkroomType, shapes = DarkroomShapes, content = content)
    }
}
