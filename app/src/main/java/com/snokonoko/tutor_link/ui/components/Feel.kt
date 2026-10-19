package com.snokonoko.tutor_link.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snokonoko.tutor_link.ui.theme.R4
import com.snokonoko.tutor_link.ui.theme.Sans
import com.snokonoko.tutor_link.ui.theme.TL
import kotlinx.coroutines.delay

/* ---------- haptics: weighted by action, skipped when the user turns them off ---------- */

object Buzz {
    const val TAB = 6L
    const val TAP = 9L
    const val CONFIRM = 14L
    const val DECIDE = 16L
    const val ERROR = 24L
}

class Haptics(context: Context) {
    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Exception) {
        null
    }

    var enabled: Boolean = true

    fun buzz(ms: Long) {
        if (!enabled) return
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {
            // No vibrator or no permission: haptics are a nicety, never a crash.
        }
    }
}

val LocalHaptics = staticCompositionLocalOf<Haptics> { error("Haptics not provided") }

/** True when the user turned on Reduce animations, or the phone has animations off. */
val LocalReduceMotion = staticCompositionLocalOf { false }

fun systemAnimationsOff(context: Context): Boolean = try {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
} catch (_: Exception) {
    false
}

/** Press-to-shrink with a spring, as on every tappable thing in the demo. */
@Composable
fun Modifier.pressSpring(interaction: MutableInteractionSource, scale: Float = 0.975f): Modifier {
    if (LocalReduceMotion.current) return this
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "press",
    )
    return this.graphicsLayer { scaleX = s; scaleY = s }
}

/* ---------- toast: navy bar with a gold edge, above the tab bar ---------- */

@Stable
class ToastState {
    var message by mutableStateOf<String?>(null)
        private set
    var id by mutableIntStateOf(0)
        private set

    fun show(text: String) {
        message = text
        id++
    }

    fun clear() {
        message = null
    }
}

val LocalToast = staticCompositionLocalOf<ToastState> { error("Toast not provided") }

@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val msg = state.message
    LaunchedEffect(state.id) {
        if (msg != null) {
            delay(2400)
            state.clear()
        }
    }
    val reduce = LocalReduceMotion.current
    AnimatedVisibility(
        visible = msg != null,
        enter = if (reduce) fadeIn(tween(1)) else fadeIn(tween(300)) + slideInVertically(spring(dampingRatio = 0.6f)) { it / 2 },
        exit = if (reduce) fadeOut(tween(1)) else fadeOut(tween(300)) + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        Row(
            Modifier
                .padding(horizontal = 24.dp)
                .clip(R4)
                .background(TL.Navy)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(TL.Gold))
            Text(
                state.message ?: "",
                color = Color.White,
                fontFamily = Sans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            )
        }
    }
}
