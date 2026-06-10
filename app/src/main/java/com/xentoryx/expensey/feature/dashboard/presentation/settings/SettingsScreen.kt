package com.xentoryx.expensey.feature.dashboard.presentation.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.core.presentation.components.CrushCanvasDecoration
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.feature.category.presentation.CategoriesScreen
import com.xentoryx.expensey.feature.category.presentation.CategoriesViewModel
import com.xentoryx.expensey.feature.pdf_export.domain.usecase.ExportPdfReportUseCase
import com.xentoryx.expensey.feature.recurring_transaction.presentation.form.RecurringFormScreen
import com.xentoryx.expensey.feature.recurring_transaction.presentation.form.RecurringFormViewModel
import com.xentoryx.expensey.feature.recurring_transaction.presentation.list.RecurringListScreen
import com.xentoryx.expensey.feature.recurring_transaction.presentation.list.RecurringListViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface SettingsNavigation {
    data object Main : SettingsNavigation
    data object Categories : SettingsNavigation
    data object RecurringList : SettingsNavigation
    data class RecurringForm(val scheduleId: String? = null) : SettingsNavigation
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    tokenManager: TokenManager = koinInject()
) {
    var navigationStack by remember { mutableStateOf<List<SettingsNavigation>>(listOf(SettingsNavigation.Main)) }
    val currentScreen = navigationStack.last()

    val navigateTo: (SettingsNavigation) -> Unit = { screen ->
        navigationStack = navigationStack + screen
    }

    val navigateBack: () -> Unit = {
        if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
        }
    }

    BackHandler(enabled = navigationStack.size > 1) {
        navigateBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            is SettingsNavigation.Main -> {
                SettingsMainContent(
                    tokenManager = tokenManager,
                    onManageCategoriesClick = { navigateTo(SettingsNavigation.Categories) },
                    onRecurringClick = { navigateTo(SettingsNavigation.RecurringList) }
                )
            }
            is SettingsNavigation.Categories -> {
                CategoriesScreen(
                    viewModel = koinViewModel<CategoriesViewModel>(),
                    onBackClick = navigateBack
                )
            }
            is SettingsNavigation.RecurringList -> {
                RecurringListScreen(
                    viewModel = koinViewModel<RecurringListViewModel>(),
                    onBackClick = navigateBack,
                    onAddClick = { navigateTo(SettingsNavigation.RecurringForm(null)) },
                    onEditClick = { id -> navigateTo(SettingsNavigation.RecurringForm(id)) }
                )
            }
            is SettingsNavigation.RecurringForm -> {
                RecurringFormScreen(
                    scheduleId = currentScreen.scheduleId,
                    viewModel = koinViewModel<RecurringFormViewModel>(),
                    onBackClick = navigateBack
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainContent(
    tokenManager: TokenManager,
    onManageCategoriesClick: () -> Unit,
    onRecurringClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by tokenManager.themeMode.collectAsState(initial = "system")

    var isNotificationsAllowed by remember {
        mutableStateOf(checkNotificationPermission(context))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isNotificationsAllowed = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CrushCanvasDecoration(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // 1. Theme Configuration Card
                SettingsSectionCard(
                    title = "App Theme",
                    icon = Icons.Default.Palette,
                    subtitle = "Choose the display color of Expensey"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeOptionRow(
                            label = "Light Mode",
                            selected = themeMode == "light",
                            onClick = {
                                scope.launch { tokenManager.saveThemeMode("light") }
                            }
                        )
                        ThemeOptionRow(
                            label = "Dark Mode",
                            selected = themeMode == "dark",
                            onClick = {
                                scope.launch { tokenManager.saveThemeMode("dark") }
                            }
                        )
                        ThemeOptionRow(
                            label = "System Default",
                            selected = themeMode == "system",
                            onClick = {
                                scope.launch { tokenManager.saveThemeMode("system") }
                            }
                        )
                    }
                }

                // 2. Financial Tools Card (Manage custom categories, recurring transactions)
                SettingsSectionCard(
                    title = "Financial Tools",
                    icon = Icons.Default.Category,
                    subtitle = "Manage templates, categories, and reports"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToolOptionRow(
                            label = "Manage Categories",
                            onClick = onManageCategoriesClick
                        )
                        ToolOptionRow(
                            label = "Recurring Transactions",
                            onClick = onRecurringClick
                        )
                    }
                }

                // 3. Notification Permissions Card
                SettingsSectionCard(
                    title = "Push Notifications",
                    icon = Icons.Default.Notifications,
                    subtitle = "Get alerts for budgets, updates and expenses"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isNotificationsAllowed) "Notifications Enabled" else "Notifications Disabled",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isNotificationsAllowed) "You will receive updates." else "Tap to request permissions.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isNotificationsAllowed,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        isNotificationsAllowed = true
                                    }
                                } else {
                                    Toast.makeText(context, "Change in system settings if you wish to block notifications completely.", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.background,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }

                // 4. App Info Card
                SettingsSectionCard(
                    title = "Application Info",
                    icon = Icons.Default.Info,
                    subtitle = "OptiSpend Expensey version metadata"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoRow(label = "App Version", value = "1.0.0")
                        InfoRow(label = "Platform", value = "Android (Jetpack Compose)")
                        InfoRow(label = "Developers", value = "Mohammad Asif & Team")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ToolOptionRow(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            content()
        }
    }
}

@Composable
fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}
