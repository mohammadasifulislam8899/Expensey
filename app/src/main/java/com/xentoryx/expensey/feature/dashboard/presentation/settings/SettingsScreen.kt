package com.xentoryx.expensey.feature.dashboard.presentation.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.core.presentation.util.BiometricPromptManager
import com.xentoryx.expensey.feature.auth.domain.model.User
import com.xentoryx.expensey.feature.auth.domain.usecase.GetProfileUseCase
import com.xentoryx.expensey.feature.auth.domain.usecase.UpdateProfileUseCase
import com.xentoryx.expensey.feature.auth.domain.usecase.ChangePasswordUseCase
import com.xentoryx.expensey.feature.auth.domain.usecase.UpdateProfileParams
import com.xentoryx.expensey.feature.auth.domain.usecase.ChangePasswordParams
import com.xentoryx.expensey.core.presentation.components.CrushCanvasDecoration
import com.xentoryx.expensey.core.domain.model.AppCurrency
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.feature.auth.domain.repository.AuthRepository
import com.xentoryx.expensey.core.data.database.AppDatabase
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.xentoryx.expensey.core.data.database.dao.TransactionDao
import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.core.data.database.entity.TransactionEntity
import com.xentoryx.expensey.core.presentation.util.NotificationScheduler
import com.xentoryx.expensey.feature.dashboard.presentation.dashboard.NetWorthOverviewCard
import com.xentoryx.expensey.feature.dashboard.presentation.dashboard.FinancialSummaryRow
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.Locale

sealed interface SettingsNavigation {
    data object Main : SettingsNavigation
    data object NotificationSettings : SettingsNavigation
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
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
                    onNotificationSettingsClick = { navigateTo(SettingsNavigation.NotificationSettings) },
                    onBackClick = onBackClick,
                    onLogoutClick = onLogoutClick
                )
            }
            is SettingsNavigation.NotificationSettings -> {
                NotificationSettingsScreen(
                    tokenManager = tokenManager,
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
    onNotificationSettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by tokenManager.themeMode.collectAsState(initial = "system")

    val getProfileUseCase: GetProfileUseCase = koinInject()
    val updateProfileUseCase: UpdateProfileUseCase = koinInject()
    val changePasswordUseCase: ChangePasswordUseCase = koinInject()
    val authRepository: AuthRepository = koinInject()
    val appDatabase: AppDatabase = koinInject()

    var userProfile by remember { mutableStateOf<User?>(null) }
    var isLoadingProfile by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    val activity = context as? FragmentActivity
    val biometricPromptManager = remember(activity) { activity?.let { BiometricPromptManager(it) } }
    val biometricEnabledState by tokenManager.biometricEnabled.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        isLoadingProfile = true
        when (val result = getProfileUseCase()) {
            is Result.Success -> {
                userProfile = result.data
            }
            is Result.Error -> {
                // handle error
            }
        }
        isLoadingProfile = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
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

                // 0. Premium Profile Banner Card (Outlined & Minimalist)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isLoadingProfile) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary)
                        } else {
                            val user = userProfile
                            if (user != null) {
                                // Outlined Minimalist Avatar
                                val initials = user.fullName.split(" ")
                                    .take(2)
                                    .map { it.take(1) }
                                    .joinToString("")
                                    .uppercase()

                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 26.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = user.fullName,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = user.email,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Currency: ${user.currencyCode}",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showEditProfileDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Edit Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { showChangePasswordDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Password", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 1. Preferences Group Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Preferences",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Configure app visual theme and daily reminders",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                        // App Theme Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "App Theme",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val themes = listOf("light" to "Light", "dark" to "Dark", "system" to "System")
                                themes.forEach { (mode, label) ->
                                    val isSelected = themeMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                            )
                                            .clickable { scope.launch { tokenManager.saveThemeMode(mode) } },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                        // Daily Reminders Option Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onNotificationSettingsClick)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Daily Summary Reminders",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Configure offline-first daily transaction reminders",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Biometrics Switch Row
                        if (biometricPromptManager?.isBiometricAvailable() == true) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Biometric Login",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Unlock using fingerprint or face ID",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = biometricEnabledState,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            biometricPromptManager.showBiometricPrompt(
                                                title = "Enable Biometric Login",
                                                description = "Confirm credentials to enable this feature.",
                                                onSuccess = {
                                                    scope.launch {
                                                        tokenManager.saveBiometricEnabled(true)
                                                        Toast.makeText(context, "Biometric login enabled!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onError = { _, errString ->
                                                    Toast.makeText(context, "Failed: $errString", Toast.LENGTH_SHORT).show()
                                                },
                                                onFailed = {
                                                    Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            scope.launch {
                                                tokenManager.saveBiometricEnabled(false)
                                                Toast.makeText(context, "Biometric login disabled.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. App Info Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Application Info",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "OptiSpend Expensey version metadata",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            InfoRow(label = "App Version", value = "1.0.0")
                            InfoRow(label = "Platform", value = "Android (Jetpack Compose)")
                            InfoRow(label = "Developers", value = "Mohammad Asif & Team")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. Danger Zone Card (Premium Outlined Design)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
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
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Danger Zone",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Irreversible destructive account operations",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                        var showResetConfirm by remember { mutableStateOf(false) }
                        var showDeleteConfirm1 by remember { mutableStateOf(false) }
                        var showDeleteConfirm2 by remember { mutableStateOf(false) }
                        var isProcessingDangerAction by remember { mutableStateOf(false) }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Reset Data Button
                            Button(
                                onClick = { showResetConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isProcessingDangerAction
                            ) {
                                Text("Reset All Data", fontWeight = FontWeight.Bold)
                            }

                            // Delete Account Button
                            Button(
                                onClick = { showDeleteConfirm1 = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isProcessingDangerAction
                            ) {
                                Text("Delete User Account", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Reset Confirmation Dialog
                        if (showResetConfirm) {
                            AlertDialog(
                                onDismissRequest = { showResetConfirm = false },
                                title = { Text("Reset All Data?") },
                                text = { Text("Are you sure you want to reset all your data? This will permanently wipe all accounts, transactions, budgets, and templates. This action cannot be undone.") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showResetConfirm = false
                                            scope.launch {
                                                isProcessingDangerAction = true
                                                when (authRepository.resetAllData()) {
                                                    is Result.Success -> {
                                                        withContext(Dispatchers.IO) {
                                                            appDatabase.clearAllTables()
                                                        }
                                                        tokenManager.saveDashboardCache("")
                                                        Toast.makeText(context, "All data reset successfully", Toast.LENGTH_SHORT).show()
                                                    }
                                                    is Result.Error -> {
                                                        Toast.makeText(context, "Failed to reset data on server", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                isProcessingDangerAction = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Confirm Reset")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showResetConfirm = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        // Delete Confirmation Dialog 1
                        if (showDeleteConfirm1) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm1 = false },
                                title = { Text("Delete Account?") },
                                text = { Text("Are you sure you want to delete your account? All your profile information, currency configurations, and transaction records will be permanently deleted.") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showDeleteConfirm1 = false
                                            showDeleteConfirm2 = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Continue")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm1 = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        // Delete Confirmation Dialog 2 (Double Confirmation)
                        if (showDeleteConfirm2) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm2 = false },
                                title = { Text("Final Confirmation") },
                                text = { Text("THIS ACTION IS COMPLETELY IRREVERSIBLE. Are you absolutely certain you want to permanently delete your account and all associated data?") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showDeleteConfirm2 = false
                                            scope.launch {
                                                isProcessingDangerAction = true
                                                when (authRepository.deleteAccount()) {
                                                    is Result.Success -> {
                                                        withContext(Dispatchers.IO) {
                                                            appDatabase.clearAllTables()
                                                        }
                                                        Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                                        onLogoutClick()
                                                    }
                                                    is Result.Error -> {
                                                        Toast.makeText(context, "Failed to delete account from server", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                isProcessingDangerAction = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Delete Permanently")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm2 = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Edit Profile Dialog
            if (showEditProfileDialog) {
                var name by remember { mutableStateOf(userProfile?.fullName ?: "") }
                var currency by remember { mutableStateOf(userProfile?.currencyCode ?: "") }
                var country by remember { mutableStateOf(userProfile?.countryCode ?: "BD") }
                var error by remember { mutableStateOf<String?>(null) }
                var isSaving by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { if (!isSaving) showEditProfileDialog = false },
                    title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            var currencyMenuExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = currencyMenuExpanded,
                                onExpandedChange = { currencyMenuExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = AppCurrency.fromCode(currency).displayName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Currency") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenuExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = currencyMenuExpanded,
                                    onDismissRequest = { currencyMenuExpanded = false }
                                ) {
                                    AppCurrency.values().forEach { appCurrency ->
                                        DropdownMenuItem(
                                            text = { Text(appCurrency.displayName) },
                                            onClick = {
                                                currency = appCurrency.code
                                                currencyMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            var countryMenuExpanded by remember { mutableStateOf(false) }
                            val countries = listOf(
                                "BD" to "Bangladesh",
                                "US" to "United States",
                                "IN" to "India",
                                "SA" to "Saudi Arabia",
                                "AE" to "United Arab Emirates"
                            )
                            ExposedDropdownMenuBox(
                                expanded = countryMenuExpanded,
                                onExpandedChange = { countryMenuExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = countries.firstOrNull { it.first == country.uppercase() }?.second ?: country,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Country") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryMenuExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = countryMenuExpanded,
                                    onDismissRequest = { countryMenuExpanded = false }
                                ) {
                                    countries.forEach { (code, name) ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                country = code
                                                countryMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            if (error != null) {
                                Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    error = null
                                    when (val res = updateProfileUseCase(UpdateProfileParams(name, currency, country))) {
                                        is Result.Success -> {
                                            userProfile = res.data
                                            showEditProfileDialog = false
                                            Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                        }
                                        is Result.Error -> {
                                            error = when (val err = res.error) {
                                                is DataError.Api -> err.message
                                                else -> "Failed to update profile"
                                            }
                                        }
                                    }
                                    isSaving = false
                                }
                            },
                            enabled = !isSaving && name.isNotBlank() && currency.isNotBlank()
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Save")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showEditProfileDialog = false },
                            enabled = !isSaving
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Change Password Dialog
            if (showChangePasswordDialog) {
                var currentPassword by remember { mutableStateOf("") }
                var newPassword by remember { mutableStateOf("") }
                var confirmPassword by remember { mutableStateOf("") }
                var error by remember { mutableStateOf<String?>(null) }
                var isSaving by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { if (!isSaving) showChangePasswordDialog = false },
                    title = { Text("Change Password", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = currentPassword,
                                onValueChange = { currentPassword = it },
                                label = { Text("Current Password") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("New Password") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm New Password") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            if (error != null) {
                                Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newPassword != confirmPassword) {
                                    error = "New passwords do not match"
                                    return@Button
                                }
                                scope.launch {
                                    isSaving = true
                                    error = null
                                    when (val res = changePasswordUseCase(ChangePasswordParams(currentPassword, newPassword))) {
                                        is Result.Success -> {
                                            showChangePasswordDialog = false
                                            Toast.makeText(context, "Password changed!", Toast.LENGTH_SHORT).show()
                                        }
                                        is Result.Error -> {
                                            error = when (val err = res.error) {
                                                is DataError.Api -> err.message
                                                else -> "Failed to change password"
                                            }
                                        }
                                    }
                                    isSaving = false
                                }
                            },
                            enabled = !isSaving && currentPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank()
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Change")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showChangePasswordDialog = false },
                            enabled = !isSaving
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    tokenManager: TokenManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val transactionDao: TransactionDao = koinInject()
    val accountDao: AccountDao = koinInject()

    val enabled by tokenManager.notificationEnabled.collectAsState(initial = false)
    val hour by tokenManager.notificationHour.collectAsState(initial = 21)
    val minute by tokenManager.notificationMinute.collectAsState(initial = 0)

    var todayIncome by remember { mutableStateOf(0.0) }
    var todayExpense by remember { mutableStateOf(0.0) }
    var totalBalance by remember { mutableStateOf(0.0) }
    var currencyCode by remember { mutableStateOf("BDT") }
    var currencySymbol by remember { mutableStateOf("৳") }
    var recentTransactions by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val accounts = accountDao.getAccounts()
            totalBalance = accounts.sumOf { it.balance }
            val code = try { tokenManager.userCurrency.first() } catch (e: Exception) { "BDT" }
            currencyCode = code
            currencySymbol = AppCurrency.fromCode(code).symbol

            val todayStr = LocalDate.now().toString()
            val txs = transactionDao.getTransactionsByDate(todayStr)
            var inc = 0.0
            var exp = 0.0
            txs.forEach { tx ->
                when (tx.type.uppercase(Locale.US)) {
                    "INCOME" -> inc += tx.amount
                    "EXPENSE" -> exp += tx.amount
                }
            }
            todayIncome = inc
            todayExpense = exp

            recentTransactions = transactionDao.getRecentTransactions()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var isNotificationsAllowed by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isNotificationsAllowed = isGranted
        if (isGranted) {
            scope.launch {
                tokenManager.saveNotificationEnabled(true)
                NotificationScheduler.scheduleDailyNotification(context, hour, minute)
            }
            Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission is required to receive daily summaries.", Toast.LENGTH_LONG).show()
        }
    }

    if (showTimePicker) {
        DisposableEffect(Unit) {
            val timePickerDialog = android.app.TimePickerDialog(
                context,
                { _, pickedHour, pickedMinute ->
                    scope.launch {
                        tokenManager.saveNotificationTime(pickedHour, pickedMinute)
                        if (enabled) {
                            NotificationScheduler.scheduleDailyNotification(context, pickedHour, pickedMinute)
                        }
                    }
                    showTimePicker = false
                },
                hour,
                minute,
                false
            )
            timePickerDialog.setOnDismissListener { showTimePicker = false }
            timePickerDialog.show()
            onDispose {
                timePickerDialog.dismiss()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Notification Settings",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        letterSpacing = (-0.5).sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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

                // Card containing the Main Notification settings
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Header Icon and Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Daily Reminder",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Receive daily financial summaries.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                        // Switch row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (enabled) "Status: Notifications Enabled" else "Status: Notifications Disabled",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = if (enabled) "Summaries trigger daily." else "Summaries are paused.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = enabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isNotificationsAllowed) {
                                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            scope.launch {
                                                tokenManager.saveNotificationEnabled(true)
                                                NotificationScheduler.scheduleDailyNotification(context, hour, minute)
                                            }
                                        }
                                    } else {
                                        scope.launch {
                                            tokenManager.saveNotificationEnabled(false)
                                            NotificationScheduler.cancelDailyNotification(context)
                                        }
                                    }
                                }
                            )
                        }

                        if (enabled) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                            // Time Picker Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Reminder Time",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = formatTime(hour, minute),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Button(
                                    onClick = { showTimePicker = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Change Time", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Premium notification mockup preview card
                Text(
                    text = "Preview",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                val contentText = "Total Balance: $currencySymbol${String.format(Locale.US, "%,.2f", totalBalance)} • Today: +$currencySymbol${String.format(Locale.US, "%,.0f", todayIncome)} | -$currencySymbol${String.format(Locale.US, "%,.0f", todayExpense)}"

                val points = remember(totalBalance, recentTransactions) {
                    var current = totalBalance
                    val list = mutableListOf<Double>()
                    list.add(current)
                    recentTransactions.forEach { tx ->
                        val isExpense = tx.type.uppercase(Locale.US) == "EXPENSE"
                        if (isExpense) {
                            current += tx.amount
                        } else if (tx.type.uppercase(Locale.US) == "INCOME") {
                            current -= tx.amount
                        }
                        list.add(current)
                    }
                    list.reversed()
                }

                val displayPoints = remember(points) {
                    if (points.size >= 2) points
                    else listOf(1000.0, 1200.0, 1100.0, 1500.0, 1350.0, 1800.0)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "E",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "EXPENSEY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "now",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "📊 Daily Finance Summary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = contentText,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Spacer(modifier = Modifier.height(8.dp))

                            val mockSavingsRate = remember(todayIncome, todayExpense) {
                                if (todayIncome > 0.0) {
                                    val net = todayIncome - todayExpense
                                    if (net > 0.0) (net / todayIncome) * 100.0 else 0.0
                                } else {
                                    0.0
                                }
                            }

                            NetWorthOverviewCard(
                                totalBalance = totalBalance,
                                sparklinePoints = displayPoints,
                                currencyCode = currencyCode
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FinancialSummaryRow(
                                totalIncome = todayIncome,
                                totalExpense = todayExpense,
                                savingsRate = mockSavingsRate,
                                currencyCode = currencyCode
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ToolOptionRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
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

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
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
