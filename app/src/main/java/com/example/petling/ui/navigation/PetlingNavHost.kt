package com.example.petling.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.petling.domain.model.CharacterSpec
import com.example.petling.ui.appContainer
import com.example.petling.ui.calendar.CalendarScreen
import com.example.petling.ui.characterdetail.CharacterDetailScreen
import com.example.petling.ui.home.AppPet
import com.example.petling.ui.home.HomeScreen
import com.example.petling.ui.home.rememberYardState
import com.example.petling.ui.onboarding.OnboardingScreen
import com.example.petling.ui.schedule.ScheduleDetailScreen
import com.example.petling.ui.schedule.ScheduleEditScreen
import com.example.petling.ui.settings.SettingsScreen
import com.example.petling.ui.theme.Brand500
import com.example.petling.ui.theme.Dimens

private data class TabItem(val icon: ImageVector, val label: String)

@Composable
fun PetlingNavHost(
    startOnboarding: Boolean,
    navController: NavHostController = rememberNavController(),
    initialScheduleId: Long? = null,
    sharedImageUri: android.net.Uri? = null,
    sharedText: String? = null,
    openLibrary: Boolean = false,
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentDest = backStack?.destination

    // 전역 마당 캐릭터(모든 메인 탭에서 하단을 혼자 배회)
    val container = appContainer()
    val petCharacter by container.characterRepository.characterState
        .collectAsStateWithLifecycle(initialValue = null)
    val petGreeting by container.petSpeech.collectAsStateWithLifecycle()
    val yardState = rememberYardState()
    // perch 레지스트리: NavHost 레벨 remember → 4탭 공유·지속. 화면·오버레이 공통 스코프로 제공.
    val perchRegistry = androidx.compose.runtime.remember { com.example.petling.ui.overlay.PerchRegistry() }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        container.petCelebrate.collect { evolved -> yardState.celebrate(evolved) }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        container.petSnack.collect { frac ->
            yardState.events.trySend(com.example.petling.ui.home.YardEvent.Snack(frac))
        }
    }
    // 주인이 앱에 들어오면(콜드스타트+백그라운드 복귀) 종별 인사 — 60초 스로틀
    val lastGreetAt = androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        val now = System.currentTimeMillis()
        if (now - lastGreetAt.longValue > 60_000) {
            lastGreetAt.longValue = now
            yardState.events.trySend(com.example.petling.ui.home.YardEvent.Greet)
        }
    }

    // 정리 완료 알림 탭 → 보관함으로(1회)
    androidx.compose.runtime.LaunchedEffect(openLibrary) {
        if (openLibrary && !startOnboarding) {
            navController.navigate(LibraryRoute)
        }
    }

    // 알림 탭으로 진입한 경우 해당 일정 상세로 이동(1회)
    androidx.compose.runtime.LaunchedEffect(initialScheduleId) {
        if (initialScheduleId != null) {
            navController.navigate(ScheduleDetailRoute(initialScheduleId))
        }
    }

    // 공유로 이미지가 들어온 경우 캡처 화면으로 이동(1회)
    val consumedShare = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(sharedImageUri) {
        if (sharedImageUri != null && !consumedShare.value && !startOnboarding) {
            consumedShare.value = true
            navController.navigate(CaptureRoute)
        }
    }

    // 공유로 텍스트가 들어온 경우 붙여넣기 화면으로 이동(1회)
    val consumedText = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(sharedText) {
        if (sharedText != null && !consumedText.value && !startOnboarding) {
            consumedText.value = true
            navController.navigate(PasteRoute)
        }
    }

    val showBottomBar = currentDest?.let {
        it.hasRoute(HomeRoute::class) ||
            it.hasRoute(LibraryRoute::class) ||
            it.hasRoute(CalendarRoute::class) ||
            it.hasRoute(SettingsRoute::class)
    } ?: false

    androidx.compose.runtime.CompositionLocalProvider(
        com.example.petling.ui.overlay.LocalPerchRegistry provides perchRegistry,
    ) {
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(modifier = Modifier.height(Dimens.NavBarHeight + Dimens.Space10)) {
                    tabItem(currentDest, HomeRoute, Icons.Filled.Home, "홈") { navigateTab(navController, HomeRoute) }
                    tabItem(currentDest, LibraryRoute, Icons.Filled.PhotoLibrary, "보관함") { navigateTab(navController, LibraryRoute) }
                    tabItem(currentDest, CalendarRoute, Icons.Filled.CalendarMonth, "캘린더") { navigateTab(navController, CalendarRoute) }
                    tabItem(currentDest, SettingsRoute, Icons.Filled.Settings, "설정") { navigateTab(navController, SettingsRoute) }
                }
            }
        },
    ) { padding ->
      // unpadded 래퍼: 원점=root(0,0). 캐릭터 오버레이가 perch boundsInRoot()와 1:1 정렬되도록
      // NavHost(padded)와 AppPet(unpadded)을 형제로 둔다. 네비바는 금지구역이라 덮지 않는다.
      Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        NavHost(
            navController = navController,
            startDestination = if (startOnboarding) OnboardingRoute else HomeRoute,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(onComplete = {
                    navController.navigate(HomeRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                })
            }
            composable<HomeRoute> {
                HomeScreen(
                    onAddSchedule = { navController.navigate(ScheduleEditRoute()) },
                    onCaptureSchedule = { navController.navigate(CaptureRoute) },
                    onVoiceSchedule = { navController.navigate(VoiceRoute) },
                    onPasteSchedule = { navController.navigate(PasteRoute) },
                    onOpenSchedule = { navController.navigate(ScheduleDetailRoute(it)) },
                    onOpenCharacter = { navController.navigate(CharacterRoute) },
                )
            }
            composable<LibraryRoute> {
                com.example.petling.ui.library.LibraryScreen(
                    onOpenCapture = { navController.navigate(CaptureDetailRoute(it)) },
                )
            }
            composable<CalendarRoute> {
                CalendarScreen(
                    onAddSchedule = { navController.navigate(ScheduleEditRoute(presetEpochDay = it)) },
                    onOpenSchedule = { navController.navigate(ScheduleDetailRoute(it)) },
                )
            }
            composable<CharacterRoute> { CharacterDetailScreen(onBack = { navController.popBackStack() }) }
            composable<SettingsRoute> {
                SettingsScreen(onOpenCategories = { navController.navigate(CategoryRoute) })
            }
            composable<CategoryRoute> {
                com.example.petling.ui.category.CategoryManageScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable<CaptureRoute> {
                com.example.petling.ui.capture.CaptureScreen(
                    initialImageUri = if (consumedShare.value) sharedImageUri else null,
                    onBack = { navController.popBackStack() },
                    onOpenLibrary = {
                        navController.navigate(LibraryRoute) {
                            popUpTo(HomeRoute)
                        }
                    },
                )
            }
            composable<CaptureDetailRoute> { entry ->
                val route = entry.toRoute<CaptureDetailRoute>()
                com.example.petling.ui.library.CaptureDetailScreen(
                    captureId = route.captureId,
                    onBack = { navController.popBackStack() },
                    onOpenSchedule = { navController.navigate(ScheduleDetailRoute(it)) },
                )
            }
            composable<VoiceRoute> {
                com.example.petling.ui.voice.VoiceScreen(
                    onBack = { navController.popBackStack() },
                    onReviewDraft = { seed -> navController.navigate(seed.toEditRoute()) },
                )
            }
            composable<PasteRoute> {
                com.example.petling.ui.paste.PasteScreen(
                    initialText = if (consumedText.value) sharedText else null,
                    onBack = { navController.popBackStack() },
                    onReviewDraft = { seed -> navController.navigate(seed.toEditRoute()) },
                )
            }
            composable<ScheduleEditRoute> { entry ->
                val route = entry.toRoute<ScheduleEditRoute>()
                val seed = route.toSeed()
                ScheduleEditScreen(
                    scheduleId = route.scheduleId,
                    presetEpochDay = route.presetEpochDay,
                    seed = seed,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable<ScheduleDetailRoute> { entry ->
                val route = entry.toRoute<ScheduleDetailRoute>()
                ScheduleDetailScreen(
                    scheduleId = route.scheduleId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(ScheduleEditRoute(scheduleId = it)) },
                )
            }
        }
        } // padded NavHost 박스 닫기

        // 메인 4개 탭에서만 캐릭터를 띄운다(온보딩/상세 화면 제외). unpadded 형제라 root 좌표.
        // 지면선은 콘텐츠 하단(네비바 위)에 두도록 bottom inset을 넘긴다.
        val ch = petCharacter
        if (showBottomBar && ch != null) {
            AppPet(
                baseSpec = CharacterSpec.from(ch),
                affection = ch.affection,
                state = yardState,
                showBubble = currentDest?.hasRoute(HomeRoute::class) ?: false,
                greeting = petGreeting,
                bottomInset = padding.calculateBottomPadding(),
                perchRegistry = perchRegistry,
                onOpenCharacter = { navController.navigate(CharacterRoute) },
            )
        }
      }
    }
    }
}

@Composable
private fun <T : Any> RowScope.tabItem(
    currentDest: androidx.navigation.NavDestination?,
    route: T,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val selected = currentDest?.hasRoute(route::class) ?: false
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { androidx.compose.material3.Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Brand500,
            selectedTextColor = Brand500,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    )
}

private fun <T : Any> navigateTab(navController: NavHostController, route: T) {
    navController.navigate(route) {
        popUpTo(HomeRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
