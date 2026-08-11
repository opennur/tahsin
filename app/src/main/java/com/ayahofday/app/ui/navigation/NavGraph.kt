package com.ayahofday.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ayahofday.app.R
import com.ayahofday.app.theme.AyahColors
import com.ayahofday.app.theme.AyahTypography
import com.ayahofday.app.ui.components.AyahText
import com.ayahofday.app.ui.screens.BookmarksScreen
import com.ayahofday.app.ui.screens.HomeScreen
import com.ayahofday.app.ui.screens.ReflectionScreen
import com.ayahofday.app.ui.screens.TafsirScreen

/** 4 tab utama: Home, Tafsir, Reflection, Bookmarks. */
enum class BottomTab(val route: String, val labelRes: Int, val emoji: String) {
    Home("home", R.string.tab_home, "🕌"),
    Tafsir("tafsir", R.string.tab_tafsir, "📖"),
    Reflection("reflection", R.string.tab_reflection, "✍️"),
    Bookmarks("bookmarks", R.string.tab_bookmarks, "⭐"),
}

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun navigateTo(tab: BottomTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = BottomTab.Home.route,
            modifier = Modifier.weight(1f),
        ) {
            composable(BottomTab.Home.route) {
                HomeScreen(
                    onOpenTafsir = { navigateTo(BottomTab.Tafsir) },
                    onOpenReflection = { navigateTo(BottomTab.Reflection) },
                )
            }
            composable(BottomTab.Tafsir.route) {
                TafsirScreen()
            }
            composable(BottomTab.Reflection.route) {
                ReflectionScreen()
            }
            composable(BottomTab.Bookmarks.route) {
                BookmarksScreen(
                    // TODO: kirim ayat terpilih via nav argument (mis. "tafsir/{surah}/{ayah}")
                    onOpenVerse = { navigateTo(BottomTab.Tafsir) },
                )
            }
        }

        AyahBottomBar(currentRoute = currentRoute, onTabSelected = ::navigateTo)
    }
}

/** Bottom navigation kustom (tanpa Material 3): ikon emoji + label teks. */
@Composable
private fun AyahBottomBar(
    currentRoute: String?,
    onTabSelected: (BottomTab) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AyahColors.Surface)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            BottomTab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) AyahColors.Primary.copy(alpha = 0.12f) else Color.Transparent,
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AyahText(
                        tab.emoji,
                        style = TextStyle(fontSize = 20.sp, color = AyahColors.TextPrimary),
                    )
                    AyahText(
                        stringResource(tab.labelRes),
                        style = AyahTypography.Caption.copy(
                            color = if (selected) AyahColors.Primary else AyahColors.TextSecondary,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }
    }
}
