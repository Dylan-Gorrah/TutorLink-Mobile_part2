package com.snokonoko.tutor_link.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.snokonoko.tutor_link.data.Role
import com.snokonoko.tutor_link.graph
import com.snokonoko.tutor_link.ui.components.Buzz
import com.snokonoko.tutor_link.ui.components.ErrorBlock
import com.snokonoko.tutor_link.ui.components.LocalHaptics
import com.snokonoko.tutor_link.ui.components.LocalReduceMotion
import com.snokonoko.tutor_link.ui.components.LocalToast
import com.snokonoko.tutor_link.ui.components.ToastHost
import com.snokonoko.tutor_link.ui.components.ToastState
import com.snokonoko.tutor_link.ui.components.systemAnimationsOff
import com.snokonoko.tutor_link.ui.screens.ApplicantScreen
import com.snokonoko.tutor_link.ui.screens.ChoosePasswordScreen
import com.snokonoko.tutor_link.ui.screens.ComingSoon
import com.snokonoko.tutor_link.ui.screens.StudentAuthScreen
import com.snokonoko.tutor_link.ui.screens.ModulePickerScreen
import com.snokonoko.tutor_link.ui.screens.PortalScreen
import com.snokonoko.tutor_link.ui.screens.RoleSelectScreen
import com.snokonoko.tutor_link.ui.screens.ScrollBody
import com.snokonoko.tutor_link.ui.screens.SettingsScreen
import com.snokonoko.tutor_link.ui.screens.SignInScreen
import com.snokonoko.tutor_link.ui.theme.TL
import com.snokonoko.tutor_link.ui.staff.AdminTab
import com.snokonoko.tutor_link.ui.staff.ApplicationScreen
import com.snokonoko.tutor_link.ui.staff.LecturerTab
import com.snokonoko.tutor_link.ui.staff.StaffNav
import com.snokonoko.tutor_link.ui.staff.StaffViewModel
import com.snokonoko.tutor_link.ui.staff.ThreadScreen
import com.snokonoko.tutor_link.ui.staff.TutorDetailScreen
import com.snokonoko.tutor_link.ui.staff.tutorCounts
import com.snokonoko.tutor_link.ui.tutor.Loadable
import com.snokonoko.tutor_link.ui.tutor.TutorTab
import com.snokonoko.tutor_link.ui.tutor.TutorViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

object Routes {
    const val START = "start"
    const val ROLES = "roles"
    const val AUTH = "auth/{role}/{mode}"
    const val MODULES = "modules/{mode}"
    const val APP = "app"
    const val SETTINGS = "settings"

    fun auth(role: Role, register: Boolean = false) = "auth/${role.key}/${if (register) "register" else "signin"}"
    fun modules(signup: Boolean) = "modules/${if (signup) "signup" else "edit"}"
}

// The demo's push easing: cubic-bezier(.32, .72, 0, 1), 460 ms.
private val Ease = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

@Composable
fun AppRoot(vm: SessionViewModel) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val toast = remember { ToastState() }
    val haptics = remember { context.graph.haptics }
    haptics.enabled = state.settings.haptics
    val reduce = state.settings.reduceMotion || systemAnimationsOff(context)

    // Lecturer and admin share one view model across their tabs and detail screens, keyed to the account.
    val staffId = state.me?.takeIf { Role.of(it.account.role) != Role.Tutor }?.account?.id
    val staffVm: StaffViewModel? = staffId?.let { viewModel(key = "staff-$it") }
    LaunchedEffect(staffVm) {
        val svm = staffVm ?: return@LaunchedEffect
        if (Role.of(state.me?.account?.role) == Role.Lecturer) svm.loadLecturer() else svm.loadAdmin()
        svm.toasts.collect {
            toast.show(it.text)
            haptics.buzz(if (it.error) Buzz.ERROR else Buzz.CONFIRM)
        }
    }

    LaunchedEffect(Unit) {
        vm.messages.collect {
            toast.show(it.text)
            if (it.error) haptics.buzz(Buzz.ERROR)
        }
    }

    // Where the user is decides the root screen: signed in → their app, signed out → role select.
    LaunchedEffect(state.phase) {
        val route = nav.currentDestination?.route
        when (state.phase) {
            Phase.SignedIn -> if (route != Routes.APP) nav.go(Routes.APP)
            Phase.NeedsPassword -> if (route != "password") nav.go("password")
            Phase.Applicant -> if (route != "applicant") nav.go("applicant")
            Phase.SignedOut -> if (route == null || route in listOf(Routes.START, Routes.APP, Routes.SETTINGS, "password", "applicant") || route == Routes.MODULES) {
                if (route != Routes.MODULES || vm.pendingLecturer == null) nav.go(Routes.ROLES)
            }
            else -> Unit
        }
    }

    val dur = if (reduce) 1 else 460
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (reduce) fadeIn(tween(1)) else slideInHorizontally(tween(dur, easing = Ease)) { it }
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (reduce) fadeOut(tween(1)) else slideOutHorizontally(tween(dur, easing = Ease)) { -it / 4 } + fadeOut(tween(dur), 0.85f)
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (reduce) fadeIn(tween(1)) else slideInHorizontally(tween(dur, easing = Ease)) { -it / 4 } + fadeIn(tween(dur), 0.85f)
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (reduce) fadeOut(tween(1)) else slideOutHorizontally(tween(dur, easing = Ease)) { it }
    }

    CompositionLocalProvider(
        LocalHaptics provides haptics,
        LocalReduceMotion provides reduce,
        LocalToast provides toast,
    ) {
        Box(Modifier.fillMaxSize().background(TL.Bg)) {
            NavHost(
                nav,
                startDestination = Routes.START,
                enterTransition = enter,
                exitTransition = exit,
                popEnterTransition = popEnter,
                popExitTransition = popExit,
            ) {
                composable(Routes.START) {
                    if (state.phase == Phase.Offline) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            ErrorBlock(state.offlineMessage) { vm.restore() }
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TL.Gold, strokeWidth = 2.dp)
                        }
                    }
                }
                composable(Routes.ROLES) {
                    RoleSelectScreen(onPick = { role -> nav.navigate(Routes.auth(role)) }, onStudent = { nav.navigate("student/signin") })
                }
                composable(Routes.AUTH) { entry ->
                    val role = Role.of(entry.arguments?.getString("role")) ?: Role.Tutor
                    val register = entry.arguments?.getString("mode") == "register" && role == Role.Lecturer
                    SignInScreen(
                        role = role,
                        register = register,
                        state = state,
                        onBack = { nav.popBackStack() },
                        onSignIn = { login, pass -> vm.signIn(role, login, pass) },
                        onGoogle = { vm.signInWithGoogle(role) },
                        onSwitchMode = {
                            nav.navigate(Routes.auth(role, !register)) {
                                popUpTo(Routes.AUTH) { inclusive = true }
                            }
                        },
                        onRegister = { n, e, p, c ->
                            vm.startRegistration(n, e, p, c).also { if (it == null) nav.navigate(Routes.modules(signup = true)) }
                        },
                    )
                }
                composable("student/{mode}") { entry ->
                    val register = entry.arguments?.getString("mode") == "register"
                    StudentAuthScreen(
                        register = register,
                        state = state,
                        onBack = { nav.popBackStack() },
                        onSignIn = { login, pass -> vm.signIn(null, login, pass) },
                        onGoogle = { vm.signInWithGoogle(null) },
                        onSwitchMode = { nav.navigate(if (register) "student/signin" else "student/register") { popUpTo("student/{mode}") { inclusive = true } } },
                        onRegister = vm::registerStudent,
                    )
                }
                composable("password") {
                    ChoosePasswordScreen(state, onSave = vm::choosePassword, onSignOut = vm::signOut)
                }
                composable("applicant") {
                    ApplicantScreen(state, onApply = vm::apply, onRefresh = vm::refreshApplicant, onSignOut = vm::signOut)
                }
                composable(Routes.MODULES) { entry ->
                    val signup = entry.arguments?.getString("mode") == "signup"
                    ModulePickerScreen(
                        signup = signup,
                        initial = if (signup) emptyList() else state.me?.modules?.map { it.code }.orEmpty(),
                        busy = state.busy,
                        onBack = { nav.popBackStack() },
                        onSave = { codes ->
                            if (signup) vm.completeRegistration(codes) else vm.saveModules(codes) { nav.popBackStack() }
                        },
                    )
                }
                composable(Routes.APP) {
                    val me = state.me
                    val openSettings = { nav.navigate(Routes.SETTINGS) }
                    val editModules = { nav.navigate(Routes.modules(signup = false)) }
                    if (me != null && Role.of(me.account.role) == Role.Tutor) {
                        TutorApp(state, me.account.id, openSettings, editModules, vm::signOut)
                    } else if (me != null && staffVm != null) {
                        val nav2 = StaffNav(
                            openThread = { nav.navigate("thread/$it") },
                            openApp = { nav.navigate("application/$it") },
                            openTutor = { nav.navigate("tutor/$it") },
                            editModules = editModules,
                            openSettings = openSettings,
                            signOut = vm::signOut,
                        )
                        StaffApp(state, staffVm, nav2)
                    } else {
                        PortalScreen(
                            state = state,
                            onOpenSettings = openSettings,
                            onEditModules = editModules,
                            onSignOut = { vm.signOut() },
                        ) { _, tab, _, _ ->
                            ScrollBody { ComingSoon(tab.replaceFirstChar { it.uppercase() }) }
                        }
                    }
                }
                composable("thread/{tid}") { entry ->
                    val tid = entry.arguments?.getString("tid").orEmpty()
                    val me = state.me
                    if (staffVm != null && me != null) {
                        val ss by staffVm.state.collectAsStateWithLifecycle()
                        ThreadScreen(tid, me.account.id, Role.of(me.account.role) == Role.Lecturer, state.settings.messagePreviews, staffVm, ss) { nav.popBackStack() }
                    }
                }
                composable("application/{id}") { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    if (staffVm != null) {
                        val ss by staffVm.state.collectAsStateWithLifecycle()
                        ApplicationScreen(id, staffVm, ss) { nav.popBackStack() }
                    }
                }
                composable("tutor/{id}") { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    if (staffVm != null) {
                        val ss by staffVm.state.collectAsStateWithLifecycle()
                        TutorDetailScreen(id, staffVm, ss, onBack = { nav.popBackStack() }, onThread = { nav.navigate("thread/$it") })
                    }
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        state = state,
                        onBack = { nav.popBackStack() },
                        onChange = vm::updateSettings,
                        onChangePassword = vm::changePassword,
                        onEditModules = { nav.navigate(Routes.modules(signup = false)) },
                        onSignOut = { vm.signOut() },
                    )
                }
            }
            ToastHost(toast, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 86.dp))
        }
    }
}

/** The tutor's five tabs. The view model is keyed to the account so a new sign-in starts fresh. */
@Composable
private fun TutorApp(state: SessionState, accountId: String, onOpenSettings: () -> Unit, onEditModules: () -> Unit, onSignOut: () -> Unit) {
    val tvm: TutorViewModel = viewModel(key = "tutor-$accountId")
    val ts by tvm.state.collectAsStateWithLifecycle()
    val unread by tvm.unread.collectAsStateWithLifecycle()
    val toast = LocalToast.current
    val haptics = LocalHaptics.current
    LaunchedEffect(tvm) {
        tvm.loadAll()
        tvm.toasts.collect {
            toast.show(it.text)
            haptics.buzz(if (it.error) Buzz.ERROR else Buzz.CONFIRM)
        }
    }
    PortalScreen(
        state = state,
        onOpenSettings = onOpenSettings,
        onEditModules = onEditModules,
        onSignOut = onSignOut,
        unreadOverride = unread,
    ) { _, tab, me, goTab ->
        TutorTab(tab, me, tvm, ts, state.settings.messagePreviews, goTab)
    }
}

/** The lecturer's three tabs or the admin's five. */
@Composable
private fun StaffApp(state: SessionState, svm: StaffViewModel, nav: StaffNav) {
    val ss by svm.state.collectAsStateWithLifecycle()
    val role = Role.of(state.me?.account?.role)
    val pending = (ss.applications as? Loadable.Ready)?.data?.count { it.status == "pending" } ?: 0
    PortalScreen(
        state = state,
        onOpenSettings = nav.openSettings,
        onEditModules = nav.editModules,
        onSignOut = nav.signOut,
        tabCounts = if (role == Role.Admin) mapOf("apps" to pending) else emptyMap(),
        moduleCounts = tutorCounts(ss),
    ) { r, tab, me, goTab ->
        if (r == Role.Lecturer) LecturerTab(tab, me, svm, ss, state.firstRun, nav, goTab)
        else AdminTab(tab, me, svm, ss, nav, goTab)
    }
}

/** Replace the whole back stack with [route] (used when the user signs in or out). */
private fun NavHostController.go(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
