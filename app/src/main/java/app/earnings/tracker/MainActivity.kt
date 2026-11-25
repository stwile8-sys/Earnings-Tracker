package app.earnings.tracker

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT)
        )
        val store = Store(this)
        setContent { App(store) }
    }
}

@Composable
fun App(store: Store) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.green, onPrimary = C.onGreen,
            background = C.bg, onBackground = C.text,
            surface = C.surface, onSurface = C.text,
            surfaceVariant = C.surface2, onSurfaceVariant = C.muted,
        )
    ) {
        // "o" — обзор, "a" — новая запись, "m:2026-11" — месяц по неделям
        var route by rememberSaveable { mutableStateOf("o") }
        BackHandler(enabled = route != "o") { route = "o" }

        Box(Modifier.fillMaxSize().background(C.bg).systemBarsPadding().imePadding()) {
            AnimatedContent(
                targetState = route,
                transitionSpec = {
                    if (targetState != "o") {
                        (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn(tween(380))) togetherWith
                            (slideOutHorizontally(tween(380, easing = FastOutSlowInEasing)) { -it / 5 } + fadeOut(tween(250)))
                    } else {
                        (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { -it / 5 } + fadeIn(tween(380))) togetherWith
                            (slideOutHorizontally(tween(380, easing = FastOutSlowInEasing)) { it / 3 } + fadeOut(tween(250)))
                    }
                },
                label = "nav"
            ) { r ->
                when {
                    r == "a" -> AddScreen(store) { route = "o" }
                    r.startsWith("m:") -> MonthScreen(store, YearMonth.parse(r.drop(2))) { route = "o" }
                    else -> OverviewScreen(
                        store,
                        onOpenMonth = { route = "m:$it" },
                        onAdd = { route = "a" }
                    )
                }
            }
        }
    }
}
