package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CalendarEvent
import com.example.model.Couple
import com.example.model.TodoItem
import com.example.model.UserProfile
import com.example.model.CycleLog
import com.example.model.CycleConfig
import com.example.model.CyclePrediction
import com.example.model.WaterLog
import com.example.model.WaterGoal
import com.example.model.WaterComment
import com.example.model.EncryptedMessage
import com.example.model.Story
import com.example.util.CryptoUtils
import com.example.viewmodel.DuetViewModel
import coil.compose.AsyncImage
import com.example.model.QuizData
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DuetAppLayout(viewModel: DuetViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val partnerUser by viewModel.partnerUser.collectAsStateWithLifecycle()
    val coupleState by viewModel.coupleState.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isDemoMode by viewModel.isDemoMode.collectAsStateWithLifecycle()
    val isFirebaseInitialized by viewModel.isFirebaseInitialized.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showAddEventDialog by remember { mutableStateFlowOf(false) }
    var showChatWindow by remember { mutableStateFlowOf(false) }

    // Scaffold contains the whole layout
    Scaffold(
        bottomBar = {
            if (currentUser != null && coupleState != null && coupleState?.user2Uid?.isNotEmpty() == true) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    val isFemale = currentUser?.gender?.lowercase() == "female"
                    val tabs = mutableListOf(
                        Triple("home", "Home", Icons.Default.Favorite),
                        Triple("calendar", "Calendar", Icons.Default.CalendarMonth),
                        Triple("water", "Water", Icons.Default.WaterDrop)
                    )
                    if (isFemale) {
                        tabs.add(Triple("cycle", "Cycle", Icons.Default.FavoriteBorder))
                    }
                    tabs.add(Triple("quiz", "Quiz", Icons.Default.QuestionAnswer))
                    tabs.add(Triple("more", "More", Icons.Default.Menu))

                    tabs.forEach { (tab, label, icon) ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                viewModel.setTab(tab)
                                showChatWindow = false
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                               )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.testTag("nav_item_$tab")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentUser != null && coupleState != null && coupleState?.user2Uid?.isNotEmpty() == true && !showChatWindow) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    // E2EE Secure Chat Floating Action Button
                    FloatingActionButton(
                        onClick = { showChatWindow = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("floating_chat_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "Secure Chat")
                            Text("E2EE Chat", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Quick Add Moment FAB
                    FloatingActionButton(
                        onClick = { showAddEventDialog = true },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("quick_add_moment_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Calendar Moment")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                currentUser == null -> {
                    // Onboarding Authentication screen (Sign In / Sign Up)
                    AuthScreen(viewModel)
                }
                coupleState == null || coupleState?.user2Uid?.isEmpty() == true -> {
                    // Pairing & Onboarding Screen
                    PairingOnboardingScreen(viewModel)
                }
                else -> {
                    // Core Tab Contents
                    val isFemale = currentUser?.gender?.lowercase() == "female"
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                        },
                        label = "TabTransition"
                    ) { tab ->
                        when (tab) {
                            "home" -> HomeScreen(viewModel)
                            "calendar" -> CalendarScreen(viewModel)
                            "water" -> WaterScreen(viewModel)
                            "cycle" -> {
                                if (isFemale) {
                                    CycleScreen(viewModel)
                                } else {
                                    HomeScreen(viewModel)
                                }
                            }
                            "quiz" -> QuizScreen(viewModel)
                            "more" -> MoreScreen(viewModel)
                            else -> HomeScreen(viewModel)
                        }
                    }
                }
            }

            // Global Loader Overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading Duet...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Quick Add Moment Dialog overlay
            if (showAddEventDialog) {
                AddMomentDialog(
                    selectedDate = viewModel.selectedDate.collectAsStateWithLifecycle().value,
                    onDismiss = { showAddEventDialog = false },
                    onConfirm = { title, date, type, note ->
                        viewModel.addEvent(title, date, type, note)
                        showAddEventDialog = false
                    }
                )
            }

            // E2EE Secure Chat Window overlay
            AnimatedVisibility(
                visible = showChatWindow,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                ) + fadeOut()
            ) {
                SecureChatWindow(
                    viewModel = viewModel,
                    onClose = { showChatWindow = false }
                )
            }
        }
    }
}

// --- HELPER COMPOSABLE STATE STORAGE ---
fun <T> mutableStateFlowOf(value: T) = mutableStateOf(value)

// --- SUB-SCREEN 1: AUTHENTICATION (SIGN UP & SIGN IN) ---
@Composable
fun DuetPremiumLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                        )
                    ),
                    shape = CircleShape
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .offset(x = (-10).dp, y = (-4).dp)
                    .graphicsLayer(alpha = 0.85f, scaleX = 1.05f, scaleY = 1.05f)
            )
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .size(44.dp)
                    .offset(x = 10.dp, y = 4.dp)
                    .graphicsLayer(alpha = 0.85f)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
fun AuthScreen(viewModel: DuetViewModel) {
    var isSignUpMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Female") }

    val isDemoMode by viewModel.isDemoMode.collectAsStateWithLifecycle()
    val isFirebaseInit by viewModel.isFirebaseInitialized.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Identity Header
        DuetPremiumLogo()

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Duet",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Text(
            "A shared space for the two of you",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_card")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isSignUpMode) "Create Your Profile" else "Welcome back!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isSignUpMode) {
                    // Profile details
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, "Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Nickname (for partner display)") },
                        leadingIcon = { Icon(Icons.Default.Face, "Nickname") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_nickname_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Date of Birth (YYYY-MM-DD)") },
                        leadingIcon = { Icon(Icons.Default.DateRange, "DOB") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_dob_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Gender Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Gender:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = gender == "Female",
                                onClick = { gender = "Female" },
                                modifier = Modifier.testTag("gender_f")
                            )
                            Text("Female", style = MaterialTheme.typography.bodyMedium)

                            Spacer(modifier = Modifier.width(16.dp))

                            RadioButton(
                                selected = gender == "Male",
                                onClick = { gender = "Male" },
                                modifier = Modifier.testTag("gender_m")
                            )
                            Text("Male", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Authentication details
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, "Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, "Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        keyboardController?.hide()
                        if (isSignUpMode) {
                            if (email.isBlank() || password.isBlank() || name.isBlank() || nickname.isBlank()) {
                                viewModel.signUp(email, password, name, nickname, dob, gender) // Let VM raise errors
                            } else {
                                viewModel.signUp(email, password, name, nickname, dob, gender)
                            }
                        } else {
                            viewModel.login(email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_submit_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        if (isSignUpMode) "Create Account" else "Sign In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { isSignUpMode = !isSignUpMode },
                    modifier = Modifier.testTag("toggle_auth_mode")
                ) {
                    Text(
                        if (isSignUpMode) "Already have an account? Sign In" else "Don't have an account? Sign Up",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }



        if (!isFirebaseInit) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Firebase is not initialized. Please verify your internet connection and make sure your google-services.json is valid and placed in the app directory.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

// --- SUB-SCREEN 2: PAIRING & PROFILE CREATION ONBOARDING ---
@Composable
fun PairingOnboardingScreen(viewModel: DuetViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val coupleState by viewModel.coupleState.collectAsStateWithLifecycle()
    val isDemoMode by viewModel.isDemoMode.collectAsStateWithLifecycle()
    val generatedCode by viewModel.pairingCodeGenerated.collectAsStateWithLifecycle()

    var inviteCodeInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Hello, ${currentUser?.nickname ?: "Duet User"}!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Text(
            "Let's connect your profiles to begin your shared journey",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Card A: Generate Code to send to partner
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Option 1: Share Your Pair Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Generate a unique 6-digit code. Share it with your partner, and they will enter it to link your profiles instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (generatedCode == null) {
                    Button(
                        onClick = { viewModel.generatePairingCode() },
                        modifier = Modifier.testTag("generate_code_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate 6-Digit Code")
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "YOUR PAIR CODE",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                generatedCode ?: "",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.testTag("generated_code_display")
                            )
                            Text(
                                "Expires in 30 days",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Waiting for partner to link...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )


                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card B: Enter partner's code
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Option 2: Enter Partner's Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Did your partner generate a code? Paste or type it here to complete pairing instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = inviteCodeInput,
                    onValueChange = { if (it.length <= 6) inviteCodeInput = it },
                    label = { Text("6-Digit Code") },
                    placeholder = { Text("e.g. 123456") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    modifier = Modifier
                        .width(180.dp)
                        .testTag("pair_code_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.pairWithCode(inviteCodeInput)
                    },
                    modifier = Modifier.testTag("link_partner_btn")
                ) {
                    Icon(Icons.Default.Link, contentDescription = "Link")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Link Partner Profile")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Log out button
        OutlinedButton(
            onClick = { viewModel.signOut() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.testTag("pairing_logout_btn")
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Log Out")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out / Reset")
        }
    }
}

// --- CORE SCREEN 1: HOME PAGE (RELATIONSHIP STATUS & CARD VIEW) ---
@Composable
fun HomeScreen(viewModel: DuetViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val partnerUser by viewModel.partnerUser.collectAsStateWithLifecycle()
    val coupleState by viewModel.coupleState.collectAsStateWithLifecycle()
    val daysTogether = viewModel.getRelationshipDaysCount()
    val isDemoMode by viewModel.isDemoMode.collectAsStateWithLifecycle()

    val cycleLogsSelf by viewModel.cycleLogsSelf.collectAsStateWithLifecycle()
    val cycleConfigSelf by viewModel.cycleConfigSelf.collectAsStateWithLifecycle()
    val cycleLogsPartner by viewModel.cycleLogsPartner.collectAsStateWithLifecycle()
    val cycleConfigPartner by viewModel.cycleConfigPartner.collectAsStateWithLifecycle()

    var showLogPeriodDialog by remember { mutableStateOf(false) }
    var showCycleConfigDialog by remember { mutableStateOf(false) }
    var showEditStartDateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Daily Stories Row (disappears after 24 hours)
        StoryRow(viewModel = viewModel)
        Spacer(modifier = Modifier.height(12.dp))

        // Couple Profile Row Header
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("couple_header_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current User Avatar
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                currentUser?.nickname?.take(1) ?: "U",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            currentUser?.nickname ?: "You",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Connecting Heart Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Together",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Partner Avatar
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                partnerUser?.nickname?.take(1) ?: "P",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            partnerUser?.nickname ?: "Partner",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Days count section
                Text(
                    "DAYS TOGETHER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    "$daysTogether Days",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val anniversaryDate = coupleState?.relationshipStartDate
                if (anniversaryDate != null) {
                    Text(
                        "Since $anniversaryDate",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                } else {
                    Button(
                        onClick = { showEditStartDateDialog = true },
                        modifier = Modifier.testTag("set_start_date_btn")
                    ) {
                        Text("Set relationship start date")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Quick Stats Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hydration Card Summary
            val waterS by viewModel.waterSelf.collectAsStateWithLifecycle()
            val waterP by viewModel.waterPartner.collectAsStateWithLifecycle()
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setTab("water") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = "Water Track",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Hydration Today", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("You: ${waterS}ml", style = MaterialTheme.typography.bodySmall)
                    Text("Partner: ${waterP}ml", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Shared Todo Card Summary
            val todos by viewModel.todoList.collectAsStateWithLifecycle()
            val pendingTodos = todos.count { !it.isCompleted }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setTab("more") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        Icons.Default.Checklist,
                        contentDescription = "Todo",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Shared Tasks", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$pendingTodos remaining tasks", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- PART A - CYCLE TRACKING CARD ---
        val isFemale = currentUser?.gender?.lowercase() == "female"
        val predictionsSelf = remember(cycleLogsSelf, cycleConfigSelf) { viewModel.getPredictionsSelf() }
        val predictionsPartner = remember(cycleLogsPartner, cycleConfigPartner) { viewModel.getPredictionsPartner() }
        val selfStatus = getTodayCycleStatus(cycleLogsSelf, predictionsSelf)
        val partnerStatus = if (cycleConfigPartner?.isPrivacyEnabled == false && cycleLogsPartner.isNotEmpty()) {
            getTodayCycleStatus(cycleLogsPartner, predictionsPartner)
        } else if (cycleConfigPartner?.isPrivacyEnabled == true) {
            "Hidden by partner (Privacy active)"
        } else {
            "No cycle data shared yet."
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("cycle_tracking_card")
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = "Cycle Tracker",
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isFemale) "My Cycle Tracker" else "${partnerUser?.nickname ?: "Partner"}'s Cycle Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (isFemale) {
                        IconButton(
                            onClick = { showCycleConfigDialog = true },
                            modifier = Modifier.testTag("cycle_config_btn")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Cycle Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isFemale) {
                    // Self Status Card (only for female users)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "YOUR STATUS TODAY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE91E63)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                selfStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    // For male users: show partner's cycle status prominently as main item!
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "${partnerUser?.nickname?.uppercase() ?: "PARTNER"}'S CYCLE STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                partnerStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // If female user, show log button and go to Cycle tab button
                if (isFemale) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showLogPeriodDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("log_period_quick_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = "Log Period", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Period", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.setTab("cycle") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("view_details_cycle_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Details", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Details", fontSize = 12.sp)
                        }
                    }
                } else {
                    // For male user, just show a supportive tip card!
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Stay supportive! Check on your partner's wellness and mood.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (showLogPeriodDialog) {
            LogPeriodDialog(
                onDismiss = { showLogPeriodDialog = false },
                onConfirm = { start, end, symptoms, flow, notes ->
                    viewModel.logCyclePeriod(start, end, symptoms, flow, notes)
                    showLogPeriodDialog = false
                }
            )
        }

        if (showCycleConfigDialog) {
            CycleConfigDialog(
                initialConfig = cycleConfigSelf,
                onDismiss = { showCycleConfigDialog = false },
                onConfirm = { config ->
                    viewModel.saveCycleConfig(config)
                    showCycleConfigDialog = false
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // MOOD & QUIZ SECTION (Now Mood Only)
        MoodSharingSection(viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Quiz Auto-loader & Dashboard Widget
        DailyQuizHomeScreenCard(viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // Upcoming Calendar Moments
        val events by viewModel.calendarEvents.collectAsStateWithLifecycle()
        val upcomingEvents = remember(events) {
            events.filter { event ->
                try {
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    event.date >= todayStr
                } catch (e: Exception) {
                    true
                }
            }.take(3)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Upcoming Moments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.setTab("calendar") }) {
                        Text("View Calendar")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (upcomingEvents.isEmpty()) {
                    Text(
                        "No upcoming events. Tap the FAB below to save a beautiful moment together!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        upcomingEvents.forEach { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        event.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (event.note.isNotEmpty()) {
                                        Text(
                                            event.note,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (event.type == "moment") MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.secondaryContainer
                                        )
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    Text(
                                        event.date,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Edit Start Date Picker Dialog
        if (showEditStartDateDialog) {
            DatePickerDialogSimple(
                initialDate = coupleState?.relationshipStartDate ?: "2024-02-14",
                onDismiss = { showEditStartDateDialog = false },
                onConfirm = { date ->
                    viewModel.updateRelationshipStartDate(date)
                    showEditStartDateDialog = false
                }
            )
        }
    }
}

// --- SUB-SCREEN 3: SHARED CALENDAR + MOMENTS ---
@Composable
fun CalendarScreen(viewModel: DuetViewModel) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val events by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val partnerUser by viewModel.partnerUser.collectAsStateWithLifecycle()

    val cycleLogsSelf by viewModel.cycleLogsSelf.collectAsStateWithLifecycle()
    val cycleLogsPartner by viewModel.cycleLogsPartner.collectAsStateWithLifecycle()
    val cycleConfigSelf by viewModel.cycleConfigSelf.collectAsStateWithLifecycle()
    val cycleConfigPartner by viewModel.cycleConfigPartner.collectAsStateWithLifecycle()

    val predictionsSelf = remember(cycleLogsSelf, cycleConfigSelf) { viewModel.getPredictionsSelf() }
    val predictionsPartner = remember(cycleLogsPartner, cycleConfigPartner) { viewModel.getPredictionsPartner() }

    // Parse events by date mapping
    val eventsMap = remember(events) {
        events.groupBy { it.date }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Shared Calendar",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            "Sync special days and reminders real-time",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 16.dp)
        )

        // Month View Selector + Days Grid
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Calendar Days Grid header (Sun - Sat)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val daysHeader = listOf("S", "M", "T", "W", "T", "F", "S")
                    daysHeader.forEach { day ->
                        Text(
                            day,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Standard calendar grid for July 2026 (since local time is 2026-07-10)
                // July 2026 starts on Wednesday (3 empty slots), has 31 days
                val emptySlots = 3
                val totalDays = 31

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .height(210.dp)
                        .testTag("calendar_grid")
                ) {
                    // Empty grid cells for day offset
                    items(emptySlots) {
                        Box(modifier = Modifier.aspectRatio(1f))
                    }

                    items(totalDays) { index ->
                        val dayNumber = index + 1
                        val dayStr = String.format(Locale.getDefault(), "2026-07-%02d", dayNumber)
                        val isSelected = selectedDate == dayStr
                        val hasEvents = eventsMap.containsKey(dayStr)

                        val isSelfPeriod = predictionsSelf?.let { isDateInRange(dayStr, it.predictedStartDate, it.predictedEndDate) } ?: false
                        val isSelfFertile = predictionsSelf?.let { isDateInRange(dayStr, it.fertileWindowStart, it.fertileWindowEnd) } ?: false
                        val isSelfOvulation = predictionsSelf?.let { isDateSame(dayStr, it.ovulationDate) } ?: false

                        val isPartnerPeriod = predictionsPartner?.let { isDateInRange(dayStr, it.predictedStartDate, it.predictedEndDate) } ?: false

                        val cellBg = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isSelfPeriod -> Color(0xFFF8BBD0) // Soft Pink 100
                            isSelfFertile -> Color(0xFFD1C4E9) // Soft Purple 100
                            hasEvents -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else -> Color.Transparent
                        }

                        val cellBorder = when {
                            isSelfOvulation -> BorderStroke(2.dp, Color(0xFF9C27B0))
                            else -> null
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cellBg)
                                .then(if (cellBorder != null) Modifier.border(cellBorder, RoundedCornerShape(12.dp)) else Modifier)
                                .clickable { viewModel.selectDate(dayStr) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (isPartnerPeriod && !isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE91E63))
                                    )
                                }

                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        dayNumber.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected || hasEvents || isSelfPeriod || isSelfFertile || isSelfOvulation) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> Color.White
                                            isSelfPeriod -> Color(0xFF880E4F)
                                            isSelfFertile -> Color(0xFF4A148C)
                                            hasEvents -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (hasEvents && !isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendItem(color = Color(0xFFF8BBD0), text = "Predicted Period 🩸")
                        LegendItem(color = Color(0xFFD1C4E9), text = "Fertile Window 🍇")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendItemBorder(color = Color(0xFF9C27B0), text = "Ovulation Day ✨")
                        LegendItemDot(color = Color(0xFFE91E63), text = "Partner's Period 💗")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Day Events Row Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Events on $selectedDate",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Events list for selected date
        val selectedDayEvents = eventsMap[selectedDate] ?: emptyList()

        if (selectedDayEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "No Events",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No special moments saved today.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("day_events_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedDayEvents, key = { it.eventId }) { event ->
                    val isCreatedByMe = event.createdBy == currentUser?.uid
                    val creatorName = if (isCreatedByMe) currentUser?.nickname else partnerUser?.nickname

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Indicator
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCreatedByMe) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondary
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                creatorName?.take(1) ?: "U",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    event.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (event.type == "moment") MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.secondaryContainer
                                        )
                                        .padding(vertical = 2.dp, horizontal = 6.dp)
                                ) {
                                    Text(
                                        event.type.uppercase(Locale.getDefault()),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (event.type == "moment") MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                            if (event.note.isNotEmpty()) {
                                Text(
                                    event.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "Created by $creatorName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteEvent(event.eventId) },
                            modifier = Modifier.testTag("delete_event_${event.eventId}")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete event",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-SCREEN 4: SHARED WATER INTAKE TRACKER ---
@Composable
fun WaterScreen(viewModel: DuetViewModel) {
    val waterS by viewModel.waterSelf.collectAsStateWithLifecycle()
    val waterP by viewModel.waterPartner.collectAsStateWithLifecycle()
    val partnerUser by viewModel.partnerUser.collectAsStateWithLifecycle()

    val waterGoalSelf by viewModel.waterGoalSelf.collectAsStateWithLifecycle()
    val waterGoalPartner by viewModel.waterGoalPartner.collectAsStateWithLifecycle()
    val waterComments by viewModel.waterComments.collectAsStateWithLifecycle()

    var showWaterConfigDialog by remember { mutableStateOf(false) }
    var customAmountInput by remember { mutableStateOf("") }
    var cheerCommentInput by remember { mutableStateOf("") }

    val selfTarget = waterGoalSelf?.dailyGoalMl ?: 2000
    val partnerTarget = waterGoalPartner?.dailyGoalMl ?: 2000

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Love Hydration",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Log water levels & cheer each other!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { showWaterConfigDialog = true },
                modifier = Modifier.testTag("water_settings_btn")
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Setup Target")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Onboarding banner if target is default / first time
        if (waterGoalSelf == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { showWaterConfigDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Onboarding", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Calculate Daily Target", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Enter your weight & age to compute a healthy medical hydration recommendation.", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = "Arrow")
                }
            }
        }

        // Cups Side-by-Side Trackers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Self Cup
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("You", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(105.dp)
                        .height(180.dp)
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp, topStart = 8.dp, topEnd = 8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp, topStart = 8.dp, topEnd = 8.dp)
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val progress = (waterS.toFloat() / selfTarget).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(progress)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF80DEEA),
                                        Color(0xFF00ACC1)
                                    )
                                )
                            )
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        modifier = Modifier.padding(bottom = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (progress > 0.4f) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("${waterS} / ${selfTarget}ml", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }

            // Partner Cup
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(partnerUser?.nickname ?: "Partner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(105.dp)
                        .height(180.dp)
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp, topStart = 8.dp, topEnd = 8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                            RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp, topStart = 8.dp, topEnd = 8.dp)
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val progress = (waterP.toFloat() / partnerTarget).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(progress)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF81C784),
                                        Color(0xFF4CAF50)
                                    )
                                )
                            )
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        modifier = Modifier.padding(bottom = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (progress > 0.4f) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("${waterP} / ${partnerTarget}ml", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick log triggers (150ml Glass, 250ml Cup, 500ml Bottle)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Log Hydration Intake", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.addWater(150) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).testTag("add_water_150")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Glass", fontSize = 11.sp)
                            Text("+150ml", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { viewModel.addWater(250) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).testTag("add_water_250")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Cup", fontSize = 11.sp)
                            Text("+250ml", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { viewModel.addWater(500) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).testTag("add_water_500")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Bottle", fontSize = 11.sp)
                            Text("+500ml", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom ML Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customAmountInput,
                        onValueChange = { customAmountInput = it },
                        placeholder = { Text("Custom Amount ml...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("custom_water_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = customAmountInput.toIntOrNull()
                            if (amt != null && amt > 0) {
                                viewModel.addWater(amt)
                                customAmountInput = ""
                            }
                        },
                        modifier = Modifier.testTag("custom_water_submit_btn")
                    ) {
                        Text("Log")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Local Reminders Alarm scheduler
                    TextButton(
                        onClick = { viewModel.scheduleWaterReminder(8, 22, 2) },
                        modifier = Modifier.testTag("schedule_alarm_btn")
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Alarm", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set Smart Reminder", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetWater() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("reset_water_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Today", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // PART B - CHEERING AND CHAT COMMENTS SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Cheering Board 🎉",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "Send cheers and cute notifications to your love!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Send Cheer / Comment section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = cheerCommentInput,
                        onValueChange = { cheerCommentInput = it },
                        placeholder = { Text("Write a supportive message...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("cheer_comment_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (cheerCommentInput.isNotBlank()) {
                                viewModel.addWaterComment(cheerCommentInput, "💧")
                                cheerCommentInput = ""
                            }
                        },
                        modifier = Modifier.testTag("send_cheer_btn")
                    ) {
                        Text("Send")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Emoji reaction row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val emojis = listOf("👍" to "Good job!", "❤️" to "Love you!", "👏" to "Keep it up!", "🥳" to "Hurrah!", "💧" to "Stay hydrated!")
                    emojis.forEach { (emoji, text) ->
                        OutlinedButton(
                            onClick = { viewModel.addWaterComment(text, emoji) },
                            modifier = Modifier.testTag("quick_emoji_$emoji")
                        ) {
                            Text(emoji, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Spacer(modifier = Modifier.height(10.dp))

                // Cheering comments list
                if (waterComments.isEmpty()) {
                    Text(
                        "No cheering comments logged yet today. Be the first to encourage your partner!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        waterComments.sortedBy { it.timestamp }.reversed().forEach { comment ->
                            val isFromMe = comment.userId == viewModel.currentUser.value?.uid
                            val senderName = if (isFromMe) "You" else (partnerUser?.nickname ?: "Partner")
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(comment.emojiReaction, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(comment.text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("From $senderName", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showWaterConfigDialog) {
            WaterConfigDialog(
                initialGoal = waterGoalSelf,
                onDismiss = { showWaterConfigDialog = false },
                onConfirm = { goal ->
                    viewModel.saveWaterGoal(goal)
                    showWaterConfigDialog = false
                }
            )
        }
    }
}

// --- SUB-SCREEN 5: SHARED TODO CHECKLIST ---
@Composable
fun TodoScreen(viewModel: DuetViewModel) {
    val todos by viewModel.todoList.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val partnerUser by viewModel.partnerUser.collectAsStateWithLifecycle()
    val todoComments by viewModel.todoComments.collectAsStateWithLifecycle()

    var todoInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Shared Todo Checklist",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            "Stay aligned on quick chores and tasks together",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 16.dp)
        )

        // Quick entry field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = todoInput,
                onValueChange = { todoInput = it },
                label = { Text("Add new chore/todo") },
                placeholder = { Text("e.g. Wash the dishes") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    if (todoInput.isNotBlank()) {
                        viewModel.addTodo(todoInput)
                        todoInput = ""
                    }
                }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("todo_text_field")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    keyboardController?.hide()
                    if (todoInput.isNotBlank()) {
                        viewModel.addTodo(todoInput)
                        todoInput = ""
                    }
                },
                modifier = Modifier
                    .height(56.dp)
                    .testTag("todo_add_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Todo")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Checklist List View
        if (todos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Checklist,
                        contentDescription = "No tasks",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "All chores completed! High five! ✋",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("todo_items_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(todos, key = { it.todoId }) { todo ->
                    val isCompleted = todo.isCompleted
                    val isCreatedByMe = todo.createdBy == currentUser?.uid
                    val creatorName = if (isCreatedByMe) currentUser?.nickname else partnerUser?.nickname
                    
                    var isExpanded by remember { mutableStateOf(false) }
                    var commentInput by remember { mutableStateOf("") }
                    val itemComments = todoComments.filter { it.todoId == todo.todoId }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("todo_item_card_${todo.todoId}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isCompleted,
                                    onCheckedChange = { viewModel.toggleTodo(todo.todoId, it) },
                                    enabled = isCreatedByMe,
                                    modifier = Modifier.testTag("todo_checkbox_${todo.todoId}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        todo.title,
                                        style = if (isCompleted) {
                                            MaterialTheme.typography.bodyMedium.copy(
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        } else {
                                            MaterialTheme.typography.bodyMedium
                                        }
                                    )
                                    Text(
                                        "Added by $creatorName" + if (!isCreatedByMe) " (Read-only)" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                
                                // Comment Icon button
                                IconButton(
                                    onClick = { isExpanded = !isExpanded },
                                    modifier = Modifier.testTag("todo_comment_toggle_${todo.todoId}")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (itemComments.isNotEmpty()) {
                                                Badge {
                                                    Text(itemComments.size.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Comment,
                                            contentDescription = "View Comments",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                // Delete button (only if created by me!)
                                if (isCreatedByMe) {
                                    IconButton(
                                        onClick = { viewModel.deleteTodo(todo.todoId) },
                                        modifier = Modifier.testTag("todo_delete_${todo.todoId}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete task",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { },
                                        enabled = false
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Created by partner",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Expanded comments panel
                            if (isExpanded) {
                                Divider(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        "Comments (${itemComments.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    if (itemComments.isEmpty()) {
                                        Text(
                                            "No comments yet. Leave a supportive note or clarification!",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        ) {
                                            itemComments.forEach { comment ->
                                                val commentAuthor = if (comment.userId == currentUser?.uid) currentUser?.nickname else partnerUser?.nickname
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                                        .padding(8.dp)
                                                ) {
                                                    Column {
                                                        Text(
                                                            commentAuthor ?: "Partner",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.secondary
                                                        )
                                                        Text(
                                                            comment.text,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Add comment row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = commentInput,
                                            onValueChange = { commentInput = it },
                                            placeholder = { Text("Write a comment...") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("todo_comment_input_${todo.todoId}"),
                                            textStyle = MaterialTheme.typography.bodySmall,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                if (commentInput.isNotBlank()) {
                                                    viewModel.addTodoComment(todo.todoId, commentInput)
                                                    commentInput = ""
                                                }
                                            },
                                            modifier = Modifier.testTag("todo_comment_submit_${todo.todoId}")
                                        ) {
                                            Icon(
                                                Icons.Default.Send,
                                                contentDescription = "Send comment",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-SCREEN 6: PROFILE & THEME MANAGEMENT ---
@Composable
fun ProfileScreen(viewModel: DuetViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val partnerUser by viewModel.partnerUser.collectAsStateWithLifecycle()
    val coupleState by viewModel.coupleState.collectAsStateWithLifecycle()
    val isDemoMode by viewModel.isDemoMode.collectAsStateWithLifecycle()
    val selectedThemeId by viewModel.selectedThemeId.collectAsStateWithLifecycle()

    var showEditStartDateDialog by remember { mutableStateOf(false) }
    var showUnpairDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Relationship Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            "Manage anniversary, themes, and pairing configuration",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 24.dp)
        )

        // 1. Anniversary Date Picker Setting
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Relationship Anniversary", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(
                        coupleState?.relationshipStartDate ?: "Not set yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    onClick = { showEditStartDateDialog = true },
                    modifier = Modifier.testTag("edit_relationship_date_btn")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Custom App Theme Selector (Rose, Sunset, Lavender, Ocean)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Duet Theme Accent", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Select custom color background themes", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val themes = listOf(
                        "warm_rose" to Color(0xFFB13155),
                        "sunset_glow" to Color(0xFFA23F16),
                        "lavender_dream" to Color(0xFF6A4FA3),
                        "ocean_breeze" to Color(0xFF006A7C)
                    )

                    themes.forEach { (themeId, color) ->
                        val isSelected = selectedThemeId == themeId
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setThemeId(themeId) }
                                .testTag("theme_btn_$themeId"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Profiles Information Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Profile Details", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Text("Your Profile: ${currentUser?.name} (${currentUser?.nickname})", style = MaterialTheme.typography.bodySmall)
                Text("DOB: ${currentUser?.dob} | Gender: ${currentUser?.gender}", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(12.dp))

                Text("Partner Profile: ${partnerUser?.name} (${partnerUser?.nickname})", style = MaterialTheme.typography.bodySmall)
                Text("DOB: ${partnerUser?.dob} | Gender: ${partnerUser?.gender}", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))



        // 4. Critical actions: Unpairing & Sign Out
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showUnpairDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .weight(1f)
                    .testTag("unpair_partner_btn")
            ) {
                Icon(Icons.Default.LinkOff, contentDescription = "Unlink")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unpair partner")
            }

            Button(
                onClick = { viewModel.signOut() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .weight(1f)
                    .testTag("logout_btn")
            ) {
                Icon(Icons.Default.Logout, contentDescription = "Logout")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }

        // Unpair Confirmation Dialog
        if (showUnpairDialog) {
            AlertDialog(
                onDismissRequest = { showUnpairDialog = false },
                title = { Text("Unpair relationship?") },
                text = { Text("Are you sure? This will disconnect you from your partner's shared space, and delete all shared moments/reminders. This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.unpairCouple()
                            showUnpairDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("unpair_confirm_confirm")
                    ) {
                        Text("Unpair")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showUnpairDialog = false },
                        modifier = Modifier.testTag("unpair_confirm_cancel")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Relationship start date DatePicker simple dialog
        if (showEditStartDateDialog) {
            DatePickerDialogSimple(
                initialDate = coupleState?.relationshipStartDate ?: "2024-02-14",
                onDismiss = { showEditStartDateDialog = false },
                onConfirm = { date ->
                    viewModel.updateRelationshipStartDate(date)
                    showEditStartDateDialog = false
                }
            )
        }
    }
}

// --- SHARED REUSABLE DIALOGS ---

// Add Moment Dialog Overlay Composable
@Composable
fun AddMomentDialog(
    selectedDate: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, date: String, type: String, note: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf(selectedDate) }
    var type by remember { mutableStateOf("moment") } // "moment" or "reminder"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Moment ✨") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_event_title")
                )

                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_event_date")
                )

                // Type Toggle Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Type:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row {
                        Button(
                            onClick = { type = "moment" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "moment") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("add_event_type_moment")
                        ) {
                            Text("Moment")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { type = "reminder" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "reminder") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("add_event_type_reminder")
                        ) {
                            Text("Reminder")
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Short description") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_event_note")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, dateInput, type, note)
                    }
                },
                modifier = Modifier.testTag("add_event_confirm")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("add_event_cancel")
            ) {
                Text("Cancel")
            }
        }
    )
}

// Simple Text Field DatePicker Dialogue (since native datepickers have complex structures)
@Composable
fun DatePickerDialogSimple(
    initialDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var dateInput by remember { mutableStateOf(initialDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Anniversary Date") },
        text = {
            Column {
                Text("Enter the start date of your beautiful relationship in YYYY-MM-DD format:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("Anniversary Date (YYYY-MM-DD)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("anniversary_input_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (dateInput.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                        onConfirm(dateInput)
                    }
                },
                modifier = Modifier.testTag("anniversary_confirm_btn")
            ) {
                Text("Set Date")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("anniversary_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}

// --- CYCLE AND WATER UTILS & DIALOGS ---

fun getTodayCycleStatus(logs: List<CycleLog>, predictions: CyclePrediction?): String {
    if (logs.isEmpty()) return "No period logged yet. Set up your cycle below!"
    val today = LocalDate.now()
    val latestLog = logs.sortedBy { it.startDate }.lastOrNull() ?: return "No period logged yet."
    
    try {
        val lastStart = LocalDate.parse(latestLog.startDate)
        if (latestLog.endDate == null) {
            val days = ChronoUnit.DAYS.between(lastStart, today)
            if (days in 0..10) {
                return "Active Period (Day ${days + 1}) 🩸"
            }
        } else {
            val lastEnd = LocalDate.parse(latestLog.endDate!!)
            if (!today.isBefore(lastStart) && !today.isAfter(lastEnd)) {
                val days = ChronoUnit.DAYS.between(lastStart, today)
                return "Active Period (Day ${days + 1}) 🩸"
            }
        }
    } catch (e: Exception) {}
    
    if (predictions != null) {
        try {
            val predStart = LocalDate.parse(predictions.predictedStartDate)
            val predEnd = LocalDate.parse(predictions.predictedEndDate)
            val ovulation = LocalDate.parse(predictions.ovulationDate)
            val fertileStart = LocalDate.parse(predictions.fertileWindowStart)
            val fertileEnd = LocalDate.parse(predictions.fertileWindowEnd)
            
            return when {
                today == ovulation -> "Ovulation Day! ✨ (Fertile peak)"
                !today.isBefore(fertileStart) && !today.isAfter(fertileEnd) -> "Fertile Window 🍇 (High probability)"
                !today.isBefore(predStart) && !today.isAfter(predEnd) -> "Predicted Period Day! 🩸"
                today.isBefore(predStart) -> {
                    val days = ChronoUnit.DAYS.between(today, predStart)
                    "Follicular Phase • Next period in $days days 🌸"
                }
                else -> {
                    val days = ChronoUnit.DAYS.between(today, predStart.plusDays(28))
                    "Luteal Phase • Next period predicted in $days days 🌸"
                }
            }
        } catch (e: Exception) {}
    }
    
    return "Follicular Phase • Track details below!"
}

@Composable
fun DateSelectorField(
    label: String,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val parsedDate = remember(selectedDate) {
        try {
            LocalDate.parse(selectedDate)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .clickable {
                val calendar = Calendar.getInstance()
                calendar.set(parsedDate.year, parsedDate.monthValue - 1, parsedDate.dayOfMonth)
                val dpd = android.app.DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val selected = LocalDate.of(year, month + 1, day)
                        onDateSelected(selected.toString())
                    },
                    parsedDate.year,
                    parsedDate.monthValue - 1,
                    parsedDate.dayOfMonth
                )
                dpd.show()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selectedDate,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Select Date",
                tint = Color(0xFFE91E63),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun FlowIntensitySelector(
    selectedIntensity: String,
    onIntensitySelected: (String) -> Unit
) {
    val options = listOf(
        Triple("Light", "🩸", Color(0xFFFFEBEE)),
        Triple("Medium", "🩸🩸", Color(0xFFFFCDD2)),
        Triple("Heavy", "🩸🩸🩸", Color(0xFFEF9A9A))
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { (intensity, icon, bgColor) ->
            val isSelected = selectedIntensity == intensity
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) bgColor.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(0xFFE91E63) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onIntensitySelected(intensity) }
                    .padding(vertical = 12.dp)
                    .testTag("flow_chip_$intensity"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(icon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = intensity,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun SymptomSelectChip(
    name: String,
    emoji: String,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Color(0xFFE91E63).copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFFE91E63) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onSelectedChange(!isSelected) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(emoji, fontSize = 16.sp)
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
            )
            if (isSelected) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun MoodSelector(
    selectedMood: String,
    onMoodSelected: (String) -> Unit
) {
    val moods = listOf(
        "😊" to "Happy",
        "😌" to "Calm",
        "🥱" to "Tired",
        "😠" to "Irritated",
        "😢" to "Sad",
        "🤪" to "Moody"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        moods.forEach { (emoji, label) ->
            val isSelected = selectedMood == label
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) Color(0xFFE91E63).copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color(0xFFE91E63) else Color.Transparent,
                        shape = RoundedCornerShape(50)
                    )
                    .clickable { onMoodSelected(label) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(emoji, fontSize = 18.sp)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogPeriodDialog(
    onDismiss: () -> Unit,
    onConfirm: (startDate: String, endDate: String?, symptoms: List<String>, flow: String, notes: String) -> Unit
) {
    var startDateInput by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDateInput by remember { mutableStateOf("") }
    var isPeriodCompleted by remember { mutableStateOf(false) }
    var selectedFlow by remember { mutableStateOf("Medium") }
    var notesInput by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf("") }
    
    val physicalSymptoms = listOf(
        "Cramps" to "⚡",
        "Headache" to "🤕",
        "Bloating" to "🎈",
        "Nausea" to "🤢",
        "Backache" to "🩹"
    )
    val emotionalSymptoms = listOf(
        "Mood Swings" to "🎭",
        "Fatigue" to "😴",
        "Anxiety" to "😟",
        "Insomnia" to "🛌"
    )
    val selectedSymptoms = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌸", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Log Period Details",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE91E63),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "DATES",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE91E63),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                DateSelectorField(
                    label = "Period Start Date",
                    selectedDate = startDateInput,
                    onDateSelected = { startDateInput = it },
                    modifier = Modifier.testTag("period_start_date_input")
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE91E63).copy(alpha = 0.05f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Checkbox(
                        checked = isPeriodCompleted,
                        onCheckedChange = { isPeriodCompleted = it },
                        modifier = Modifier.testTag("period_completed_checkbox"),
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE91E63))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Period has ended",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE91E63)
                    )
                }

                if (isPeriodCompleted) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DateSelectorField(
                        label = "Period End Date",
                        selectedDate = endDateInput.ifEmpty { LocalDate.now().toString() },
                        onDateSelected = { endDateInput = it },
                        modifier = Modifier.testTag("period_end_date_input")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    "FLOW INTENSITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE91E63),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowIntensitySelector(
                    selectedIntensity = selectedFlow,
                    onIntensitySelected = { selectedFlow = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "HOW'S YOUR MOOD TODAY?",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE91E63),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                MoodSelector(
                    selectedMood = selectedMood,
                    onMoodSelected = { selectedMood = it }
                )

                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    "PHYSICAL SYMPTOMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE91E63),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    physicalSymptoms.forEach { (symptom, emoji) ->
                        val isSelected = selectedSymptoms.contains(symptom)
                        SymptomSelectChip(
                            name = symptom,
                            emoji = emoji,
                            isSelected = isSelected,
                            onSelectedChange = { checked ->
                                if (checked) selectedSymptoms.add(symptom)
                                else selectedSymptoms.remove(symptom)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("symptom_chip_$symptom")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    "EMOTIONAL SYMPTOMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE91E63),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    emotionalSymptoms.forEach { (symptom, emoji) ->
                        val isSelected = selectedSymptoms.contains(symptom)
                        SymptomSelectChip(
                            name = symptom,
                            emoji = emoji,
                            isSelected = isSelected,
                            onSelectedChange = { checked ->
                                if (checked) selectedSymptoms.add(symptom)
                                else selectedSymptoms.remove(symptom)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("symptom_chip_$symptom")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    "ADDITIONAL NOTES",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE91E63),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    placeholder = { Text("Write about physical details, cravings, or any support you need...") },
                    modifier = Modifier.fillMaxWidth().testTag("period_notes_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE91E63),
                        focusedLabelColor = Color(0xFFE91E63)
                    ),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (startDateInput.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                        val endVal = if (isPeriodCompleted) {
                            val candidate = endDateInput.ifEmpty { LocalDate.now().toString() }
                            if (candidate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) candidate else null
                        } else null
                        
                        val finalSymptoms = selectedSymptoms.toMutableList()
                        if (selectedMood.isNotEmpty()) {
                            finalSymptoms.add("Mood: $selectedMood")
                        }
                        
                        onConfirm(startDateInput, endVal, finalSymptoms, selectedFlow, notesInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("period_confirm_btn")
            ) {
                Text("Save Log", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("period_cancel_btn")
            ) {
                Text("Cancel", color = Color(0xFFE91E63), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun CycleConfigDialog(
    initialConfig: CycleConfig?,
    onDismiss: () -> Unit,
    onConfirm: (CycleConfig) -> Unit
) {
    var cycleLengthInput by remember { mutableStateOf(initialConfig?.avgCycleLength?.toString() ?: "28") }
    var periodLengthInput by remember { mutableStateOf(initialConfig?.avgPeriodLength?.toString() ?: "5") }
    var isPrivacyEnabled by remember { mutableStateOf(initialConfig?.isPrivacyEnabled ?: false) }
    var hideSymptoms by remember { mutableStateOf(initialConfig?.hideSymptoms ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cycle Settings & Privacy 🔒", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Average Cycle Length (Days):", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = cycleLengthInput,
                    onValueChange = { cycleLengthInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cycle_length_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Average Period Duration (Days):", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = periodLengthInput,
                    onValueChange = { periodLengthInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("period_length_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Divider()

                Spacer(modifier = Modifier.height(12.dp))

                Text("PRIVACY CONTROLS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Switch(
                        checked = isPrivacyEnabled,
                        onCheckedChange = { isPrivacyEnabled = it },
                        modifier = Modifier.testTag("cycle_privacy_switch")
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Private Cycle Data", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Hide cycle predictions from partner.", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Switch(
                        checked = hideSymptoms,
                        onCheckedChange = { hideSymptoms = it },
                        modifier = Modifier.testTag("cycle_hide_symptoms_switch")
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Hide Symptom Details", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Share only dates, hide symptoms/flow details.", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cycleL = cycleLengthInput.toIntOrNull() ?: 28
                    val periodL = periodLengthInput.toIntOrNull() ?: 5
                    onConfirm(
                        CycleConfig(
                            avgCycleLength = cycleL,
                            avgPeriodLength = periodL,
                            isPrivacyEnabled = isPrivacyEnabled,
                            hideSymptoms = hideSymptoms
                        )
                    )
                },
                modifier = Modifier.testTag("cycle_config_confirm_btn")
            ) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cycle_config_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun WaterConfigDialog(
    initialGoal: WaterGoal?,
    onDismiss: () -> Unit,
    onConfirm: (WaterGoal) -> Unit
) {
    var ageInput by remember { mutableStateOf(initialGoal?.age?.toString() ?: "25") }
    var weightInput by remember { mutableStateOf(initialGoal?.weightKg?.toString() ?: "70") }
    var isManualOverride by remember { mutableStateOf(initialGoal?.isManual ?: false) }
    var manualTargetInput by remember { mutableStateOf(initialGoal?.dailyGoalMl?.toString() ?: "2000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily Water Goal Setup 💧", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Age (Years):", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = ageInput,
                    onValueChange = { ageInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("water_age_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Weight (kg):", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("water_weight_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isManualOverride,
                        onCheckedChange = { isManualOverride = it },
                        modifier = Modifier.testTag("water_override_checkbox")
                    )
                    Text("Manual intake target override", style = MaterialTheme.typography.bodyMedium)
                }

                if (isManualOverride) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Manual Target Intake (ml):", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = manualTargetInput,
                        onValueChange = { manualTargetInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("water_manual_target_input")
                    )
                } else {
                    val weight = weightInput.toIntOrNull() ?: 70
                    val calculatedTarget = ((weight * 35) / 50) * 50
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Recommended Daily Target: ${calculatedTarget}ml\n(Computed as Weight × 35ml)",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val age = ageInput.toIntOrNull() ?: 25
                    val weight = weightInput.toIntOrNull() ?: 70
                    val calcTarget = ((weight * 35) / 50) * 50
                    val finalTarget = if (isManualOverride) (manualTargetInput.toIntOrNull() ?: 2000) else calcTarget
                    
                    onConfirm(
                        WaterGoal(
                            age = age,
                            weightKg = weight,
                            dailyGoalMl = finalTarget,
                            isManual = isManualOverride
                        )
                    )
                },
                modifier = Modifier.testTag("water_config_confirm_btn")
            ) {
                Text("Save Goal")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("water_config_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun LegendItemBorder(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).border(1.5.dp, color, RoundedCornerShape(3.dp)))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun LegendItemDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

fun isDateInRange(dateStr: String, startStr: String, endStr: String): Boolean {
    if (dateStr.isEmpty() || startStr.isEmpty() || endStr.isEmpty()) return false
    return dateStr >= startStr && dateStr <= endStr
}

fun isDateSame(dateStr1: String, dateStr2: String): Boolean {
    if (dateStr1.isEmpty() || dateStr2.isEmpty()) return false
    return dateStr1 == dateStr2
}

@Composable
fun MoodSharingSection(viewModel: DuetViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val partnerUser by viewModel.partnerUser.collectAsStateWithLifecycle()
    val moodEntriesSelf by viewModel.moodEntriesSelf.collectAsStateWithLifecycle()
    val moodEntriesPartner by viewModel.moodEntriesPartner.collectAsStateWithLifecycle()

    val todayStr = java.time.LocalDate.now().toString()
    val myTodayMood = moodEntriesSelf.find { it.date == todayStr }
    val partnerTodayMood = moodEntriesPartner.find { it.date == todayStr }

    var selectedMoodEmoji by remember { mutableStateOf("😊") }
    var moodNote by remember { mutableStateOf("") }
    var selectedStickerId by remember { mutableStateOf<String?>(null) }
    var sexDriveLevel by remember { mutableStateOf<Int?>(null) }
    var isSexDriveShared by remember { mutableStateOf(false) }

    var showMoodDialog by remember { mutableStateOf(false) }
    var showReactionDialogForEntryId by remember { mutableStateOf<String?>(null) }

    val moodEmojis = listOf("😊", "🥳", "😌", "😜", "🥰", "😢", "🥱", "😰")
    val stickers = listOf(
        "sparkles" to "✨ Sparkles",
        "cloud" to "☁️ Cloud",
        "heart" to "💖 Heart",
        "star" to "⭐ Star",
        "rose" to "🌹 Rose",
        "fire" to "🔥 Fire"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // ------------------ MOOD CARD ------------------
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("mood_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Mood,
                            contentDescription = "Mood",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Mood & Intimacy Sharing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(
                        onClick = { showMoodDialog = true },
                        modifier = Modifier
                            .testTag("log_mood_btn")
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (myTodayMood != null) "Update Mood" else "Log Mood",
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Feed display for Today
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Your status today
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("YOU", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            if (myTodayMood != null) {
                                Text(myTodayMood.moodEmoji, fontSize = 36.sp)
                                if (myTodayMood.stickerId != null) {
                                    val emojiSticker = when (myTodayMood.stickerId) {
                                        "sparkles" -> "✨"
                                        "cloud" -> "☁️"
                                        "heart" -> "💖"
                                        "star" -> "⭐"
                                        "rose" -> "🌹"
                                        "fire" -> "🔥"
                                        else -> ""
                                    }
                                    Text("Sticker: $emojiSticker", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                }
                                if (!myTodayMood.note.isNullOrBlank()) {
                                    Text(
                                        "\"${myTodayMood.note}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                if (myTodayMood.sexDriveLevel != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Favorite, contentDescription = "Sex Drive", tint = Color(0xFFE91E63), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sex Drive: ${myTodayMood.sexDriveLevel}/5", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            if (myTodayMood.isSexDriveShared) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Visibility",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                if (myTodayMood.partnerReaction != null) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Partner reacted: ${myTodayMood.partnerReaction}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            } else {
                                Text("Not logged", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }

                    // Partner status today
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(partnerUser?.nickname?.uppercase() ?: "PARTNER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            if (partnerTodayMood != null) {
                                Text(partnerTodayMood.moodEmoji, fontSize = 36.sp)
                                if (partnerTodayMood.stickerId != null) {
                                    val emojiSticker = when (partnerTodayMood.stickerId) {
                                        "sparkles" -> "✨"
                                        "cloud" -> "☁️"
                                        "heart" -> "💖"
                                        "star" -> "⭐"
                                        "rose" -> "🌹"
                                        "fire" -> "🔥"
                                        else -> ""
                                    }
                                    Text("Sticker: $emojiSticker", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                }
                                if (!partnerTodayMood.note.isNullOrBlank()) {
                                    Text(
                                        "\"${partnerTodayMood.note}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                
                                if (partnerTodayMood.isSexDriveShared && partnerTodayMood.sexDriveLevel != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Favorite, contentDescription = "Sex Drive", tint = Color(0xFFE91E63), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sex Drive: ${partnerTodayMood.sexDriveLevel}/5", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                    }
                                } else if (partnerTodayMood.sexDriveLevel != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(Icons.Default.VisibilityOff, contentDescription = "Private", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Private intimacy status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                if (partnerTodayMood.partnerReaction != null) {
                                    Text("You reacted: ${partnerTodayMood.partnerReaction}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                                } else {
                                    Button(
                                        onClick = { showReactionDialogForEntryId = partnerTodayMood.entryId },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .padding(horizontal = 4.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("React", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 10.sp)
                                    }
                                }
                            } else {
                                Text("Not logged yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReactionDialogForEntryId != null) {
        AlertDialog(
            onDismissRequest = { showReactionDialogForEntryId = null },
            title = { Text("Select Reaction Emoji") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val reactions = listOf("❤️", "👍", "🤗", "😘", "💪", "😭")
                    reactions.forEach { emoji ->
                        Text(
                            emoji,
                            fontSize = 32.sp,
                            modifier = Modifier
                                .clickable {
                                    viewModel.reactToMood(showReactionDialogForEntryId!!, emoji)
                                    showReactionDialogForEntryId = null
                                }
                                .padding(4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReactionDialogForEntryId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMoodDialog) {
        AlertDialog(
            onDismissRequest = { showMoodDialog = false },
            title = { Text("Log Today's Mood") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("How are you feeling?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        moodEmojis.take(4).forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedMoodEmoji == emoji) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { selectedMoodEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 28.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        moodEmojis.drop(4).forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedMoodEmoji == emoji) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { selectedMoodEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 28.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = moodNote,
                        onValueChange = { moodNote = it },
                        label = { Text("Add an optional short note...") },
                        placeholder = { Text("e.g. productive workday") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Pick a sticker", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedStickerId == null) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedStickerId = null }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("None", style = MaterialTheme.typography.bodySmall)
                        }

                        stickers.forEach { (id, label) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedStickerId == id) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedStickerId = id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Intimacy status (Optional)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE91E63)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Private by default. Enable sharing to reveal to your partner.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sex Drive:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    (1..5).forEach { level ->
                                        val isSelected = sexDriveLevel == level
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) Color(0xFFE91E63)
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .clickable { sexDriveLevel = if (isSelected) null else level },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                level.toString(),
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isSexDriveShared) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Share Toggle",
                                        tint = if (isSexDriveShared) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share intimacy with partner", style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(
                                    checked = isSexDriveShared,
                                    onCheckedChange = { isSexDriveShared = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFE91E63),
                                        checkedTrackColor = Color(0xFFE91E63).copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.testTag("intimacy_share_switch")
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logMood(
                            emoji = selectedMoodEmoji,
                            note = moodNote.ifBlank { null },
                            stickerId = selectedStickerId,
                            sexDriveLevel = sexDriveLevel,
                            isSexDriveShared = isSexDriveShared
                        )
                        showMoodDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.testTag("confirm_log_mood")
                ) {
                    Text("Save Mood")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoodDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MoreScreen(viewModel: DuetViewModel) {
    var selectedSubTab by remember { mutableStateOf("todo") }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = if (selectedSubTab == "todo") 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("more_sub_tab_row")
        ) {
            Tab(
                selected = selectedSubTab == "todo",
                onClick = { selectedSubTab = "todo" },
                text = { Text("Shared Tasks", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Checklist, contentDescription = "Todo") },
                modifier = Modifier.testTag("more_sub_tab_todo")
            )
            Tab(
                selected = selectedSubTab == "profile",
                onClick = { selectedSubTab = "profile" },
                text = { Text("Profile & Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                modifier = Modifier.testTag("more_sub_tab_profile")
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (selectedSubTab == "todo") {
                TodoScreen(viewModel)
            } else {
                ProfileScreen(viewModel)
            }
        }
    }
}

@Composable
fun QuizScreen(viewModel: DuetViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val partnerUser by viewModel.partnerUser.collectAsStateWithLifecycle()
    val promptResponsesSelf by viewModel.promptResponsesSelf.collectAsStateWithLifecycle()
    val promptResponsesPartner by viewModel.promptResponsesPartner.collectAsStateWithLifecycle()

    val todayStr = java.time.LocalDate.now().toString()
    val activeTheme = QuizData.getThemeForToday()

    val myTodayResponses = promptResponsesSelf.filter { it.promptDate == todayStr }
    val partnerTodayResponses = promptResponsesPartner.filter { it.promptDate == todayStr }

    // Find the first unanswered question index
    val firstUnansweredIndex = activeTheme.questions.indexOfFirst { q ->
        myTodayResponses.none { it.promptId == q.questionId }
    }

    val userFinished = firstUnansweredIndex == -1
    val partnerAnsweredCount = activeTheme.questions.count { q ->
        partnerTodayResponses.any { it.promptId == q.questionId }
    }
    val partnerFinished = partnerAnsweredCount == 5
    val bothAnswered = userFinished && partnerFinished

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- HEADER ---
        Text(
            "Daily Partner Quiz",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            "A daily theme-based quiz to check alignment, explore viewpoints, and compute your connection Bond Score!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 20.dp)
        )

        // --- ACTIVE THEME BANNER ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val themeIcon = when (activeTheme.iconName) {
                    "Favorite" -> Icons.Default.Favorite
                    "Handshake" -> Icons.Default.Handshake
                    "TrendingUp" -> Icons.Default.TrendingUp
                    "SentimentSatisfied" -> Icons.Default.SentimentSatisfied
                    "Chat" -> Icons.Default.Chat
                    else -> Icons.Default.QuestionAnswer
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        themeIcon,
                        contentDescription = activeTheme.name,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "TODAY'S THEME: ${activeTheme.name.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        activeTheme.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (!userFinished) {
            // --- QUIZ TAKING FLOW (Step-by-step MCQ) ---
            val currentQuestionIndex = firstUnansweredIndex
            val question = activeTheme.questions[currentQuestionIndex]
            var selectedOption by remember(currentQuestionIndex) { mutableStateOf("") }

            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("quiz_card_screen")
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Progress Indicator (e.g. step 3 of 5)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Question ${currentQuestionIndex + 1} of 5",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (i in 0 until 5) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 24.dp, height = 6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            when {
                                                i < currentQuestionIndex -> MaterialTheme.colorScheme.primary
                                                i == currentQuestionIndex -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                            }
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        question.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Option Buttons
                    val optionPrefixes = listOf("A", "B", "C", "D")
                    question.options.forEachIndexed { optIdx, optionText ->
                        val prefix = optionPrefixes.getOrNull(optIdx) ?: ""
                        val isSelected = selectedOption == optionText

                        val borderStroke = if (isSelected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        }

                        val containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        }

                        Surface(
                            onClick = { selectedOption = optionText },
                            shape = RoundedCornerShape(16.dp),
                            border = borderStroke,
                            color = containerColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        prefix,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    optionText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (selectedOption.isNotBlank()) {
                                viewModel.submitMcqAnswer(question.questionId, selectedOption)
                            }
                        },
                        enabled = selectedOption.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_quiz_btn_screen")
                    ) {
                        Text(
                            if (currentQuestionIndex == 4) "Submit Final Answer" else "Next Question",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        } else if (!partnerFinished) {
            // --- WAITING FOR PARTNER SCREEN (My responses locked) ---
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "All Answers Locked! 🔒",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "You've completed today's quiz. Waiting for ${partnerUser?.nickname ?: "your partner"} to finish in order to reveal both answers and compute your Bond Score!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress visualizer
                    Text(
                        "${partnerUser?.nickname ?: "Partner"}'s Progress: $partnerAnsweredCount/5 answered",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = partnerAnsweredCount / 5f,
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )



                    Spacer(modifier = Modifier.height(32.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "YOUR SUBMISSIONS FOR TODAY:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    activeTheme.questions.forEachIndexed { idx, q ->
                        val myAns = myTodayResponses.find { it.promptId == q.questionId }?.answer ?: ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(q.text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Your answer: \"$myAns\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // --- BOTH ANSWERED (Reveal screen with match-based Bond Score) ---
            var matchCount = 0
            activeTheme.questions.forEach { q ->
                val myAns = myTodayResponses.find { it.promptId == q.questionId }?.answer
                val partnerAns = partnerTodayResponses.find { it.promptId == q.questionId }?.answer
                if (myAns != null && partnerAns != null && myAns == partnerAns) {
                    matchCount++
                }
            }
            val bondScore = matchCount * 20

            val feedbackText = when (bondScore) {
                100 -> "Unstoppable Sync! 💖 You both answered exactly the same on every single topic today. You are perfectly in tune!"
                80 -> "Amazing Harmony! ✨ Your perspectives are incredibly aligned, with just one small unique difference."
                60 -> "Strong Connection! 🌱 You share the same views on most topics, offering a beautiful balance of harmony and individuality."
                40 -> "Creative Contrasts! 🎨 You have some differences, which makes for amazing opportunities to talk, listen, and learn about each other's views!"
                20 -> "Different Approaches! 🔍 You see things differently, but that is the beauty of a partnership. Use these prompts to discuss your unique viewpoints!"
                else -> "Fascinating Journey! 🧭 Every relationship has unique perspectives. Use today's topics to share why you picked what you did!"
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Celebratory icon
                    Icon(
                        Icons.Default.Celebration,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Today's Quiz Unlocked! 🎉",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Bond Score Gauge
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = bondScore / 100f,
                            modifier = Modifier.fillMaxSize(),
                            color = if (bondScore >= 60) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            strokeWidth = 10.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$bondScore%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "BOND SCORE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Match Count Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Matches",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "$matchCount of 5 Matches",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        feedbackText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "QUESTION DEEP-DIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    activeTheme.questions.forEachIndexed { idx, q ->
                        val myAns = myTodayResponses.find { it.promptId == q.questionId }?.answer ?: ""
                        val partnerAns = partnerTodayResponses.find { it.promptId == q.questionId }?.answer ?: ""
                        val isMatch = myAns == partnerAns

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMatch) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isMatch) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Question ${idx + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isMatch) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text(
                                                "MATCH 💖",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text(
                                                "DIVERSE VIEWS 💡",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    q.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Side-by-side answers or vertical answers block
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "U",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "You: \"$myAns\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "P",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "${partnerUser?.nickname ?: "Partner"}: \"$partnerAns\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CycleProgressRing(
    progress: Float,
    dayOfCycle: Long?,
    avgLength: Int,
    phase: String,
    modifier: Modifier = Modifier
) {
    val phaseColor = when (phase) {
        "Menstruation Phase" -> Color(0xFFE91E63)
        "Follicular Phase" -> Color(0xFF9C27B0)
        "Ovulation Phase" -> Color(0xFFFB8C00)
        "Luteal Phase" -> Color(0xFF673AB7)
        else -> Color(0xFFE91E63)
    }
    
    val gradientColors = when (phase) {
        "Menstruation Phase" -> listOf(Color(0xFFFF8A80), Color(0xFFE91E63))
        "Follicular Phase" -> listOf(Color(0xFFE040FB), Color(0xFF9C27B0))
        "Ovulation Phase" -> listOf(Color(0xFFFFD180), Color(0xFFFB8C00))
        "Luteal Phase" -> listOf(Color(0xFFB388FF), Color(0xFF673AB7))
        else -> listOf(Color(0xFFFF8A80), Color(0xFFE91E63))
    }

    Box(
        modifier = modifier
            .size(220.dp)
            .shadow(
                elevation = 12.dp, 
                shape = CircleShape, 
                ambientColor = phaseColor.copy(alpha = 0.2f), 
                spotColor = phaseColor.copy(alpha = 0.4f)
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.15f),
                radius = size.minDimension / 2,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12.dp.toPx())
            )
            
            drawArc(
                brush = Brush.sweepGradient(gradientColors),
                startAngle = -90f,
                sweepAngle = 360f * if (dayOfCycle != null) progress else 1.0f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 12.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = when (phase) {
                    "Menstruation Phase" -> "🌸"
                    "Follicular Phase" -> "🌱"
                    "Ovulation Phase" -> "✨"
                    "Luteal Phase" -> "🔮"
                    else -> "🤍"
                },
                fontSize = 32.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            if (dayOfCycle != null) {
                Text(
                    text = "Day $dayOfCycle",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "of $avgLength days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Ready",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Log period to start",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = phase,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = phaseColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CycleInsightsCard(phase: String) {
    val tips = when (phase) {
        "Menstruation Phase" -> listOf(
            "🍵 Drink warm chamomile or raspberry leaf tea to ease cramps.",
            "🧘‍♀️ Prioritize deep rest, gentle stretching, and restorative yoga.",
            "🔥 Use a warm compress or heating pad on your lower abdomen.",
            "💧 Keep hydration high to minimize bloating and fatigue."
        )
        "Follicular Phase" -> listOf(
            "🌱 Energy levels are rising! Perfect time to set new goals.",
            "🏃‍♀️ Primed for strength training or high-intensity cardio.",
            "🥦 Focus on eating fiber-rich veggies and light, fresh foods.",
            "🧠 Schedule brainstorming sessions and social collaborations."
        )
        "Ovulation Phase" -> listOf(
            "✨ Peak confidence and mood! Great for presentations or dates.",
            "💪 Your physical strength and endurance are at their highest levels.",
            "🥑 Fuel with healthy fats (nuts, seeds, avocados) to support hormones.",
            "🌟 Enjoy your natural radiance and high-vibe social energy."
        )
        "Luteal Phase" -> listOf(
            "🏡 Progesterone is high; embrace cozy nesting and warm tea.",
            "🚶‍♀️ Swap intense exercise for low-impact walking or Pilates.",
            "🍫 Keep sugar and caffeine low to reduce bloating and mood shifts.",
            "😴 Ensure 8+ hours of quality sleep to help manage PMS."
        )
        else -> listOf(
            "🌸 Consistency is key! Regular logging trains predictions.",
            "📋 Record daily symptoms to discover your unique patterns.",
            "💬 Check settings to control privacy and details shared with partner."
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE91E63).copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color(0xFFE91E63).copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Phase Care & Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE91E63)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            tips.forEach { tip ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", color = Color(0xFFE91E63), fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PredictionSection(predictedStartDate: String, ovulationDate: String, fertileWindowStart: String, fertileWindowEnd: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "Cycle Predictions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE91E63).copy(alpha = 0.06f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE91E63).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🩸", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Next Period", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(predictedStartDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFB74D).copy(alpha = 0.06f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB74D).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Estimated Ovulation Day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ovulationDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF9C27B0).copy(alpha = 0.06f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9C27B0).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌸", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Fertile Window", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$fertileWindowStart to $fertileWindowEnd", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CycleHistoryLogItem(
    log: CycleLog,
    onDelete: () -> Unit
) {
    val leftBarColor = when (log.flowIntensity) {
        "Light" -> Color(0xFFFFCDD2)
        "Medium" -> Color(0xFFE91E63)
        "Heavy" -> Color(0xFFB71C1C)
        else -> Color(0xFFE91E63)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(130.dp)
                    .background(leftBarColor)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📅", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${log.startDate}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (log.endDate != null) {
                            Text(
                                text = " to ${log.endDate}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Flow Intensity: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(leftBarColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = log.flowIntensity,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = leftBarColor
                            )
                        }
                    }
                    
                    if (log.symptoms.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            log.symptoms.forEach { symptom ->
                                val (emoji, cleanName) = when {
                                    symptom.startsWith("Mood: ") -> "💭" to symptom
                                    symptom == "Cramps" -> "⚡" to "Cramps"
                                    symptom == "Headache" -> "🤕" to "Headache"
                                    symptom == "Bloating" -> "🎈" to "Bloating"
                                    symptom == "Nausea" -> "🤢" to "Nausea"
                                    symptom == "Backache" -> "🩹" to "Backache"
                                    symptom == "Mood Swings" -> "🎭" to "Mood Swings"
                                    symptom == "Fatigue" -> "😴" to "Fatigue"
                                    symptom == "Anxiety" -> "😟" to "Anxiety"
                                    symptom == "Insomnia" -> "🛌" to "Insomnia"
                                    else -> "🌸" to symptom
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (symptom.startsWith("Mood: ")) Color(0xFF673AB7).copy(alpha = 0.08f)
                                            else Color(0xFFE91E63).copy(alpha = 0.08f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (symptom.startsWith("Mood: ")) Color(0xFF673AB7).copy(alpha = 0.15f)
                                            else Color(0xFFE91E63).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(emoji, fontSize = 11.sp)
                                        Text(
                                            text = cleanName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (symptom.startsWith("Mood: ")) Color(0xFF673AB7) else Color(0xFFE91E63)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    if (log.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Notes", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = log.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_cycle_log_${log.cycleId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete log",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun CycleScreen(viewModel: DuetViewModel) {
    val cycleLogsSelf by viewModel.cycleLogsSelf.collectAsStateWithLifecycle()
    val cycleConfigSelf by viewModel.cycleConfigSelf.collectAsStateWithLifecycle()
    val partnerUser by viewModel.partnerUser.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkAndNotifyEstimatedPeriods()
    }

    var showLogPeriodDialog by remember { mutableStateOf(false) }
    var showCycleConfigDialog by remember { mutableStateOf(false) }

    val predictionsSelf = remember(cycleLogsSelf, cycleConfigSelf) { viewModel.getPredictionsSelf() }
    val selfStatus = getTodayCycleStatus(cycleLogsSelf, predictionsSelf)

    val latestLog = cycleLogsSelf.sortedBy { it.startDate }.lastOrNull()
    val avgCycleLen = cycleConfigSelf?.avgCycleLength ?: 28
    
    val (currentDayOfCycle, cycleProgress, phaseName) = remember(latestLog, avgCycleLen) {
        if (latestLog != null) {
            try {
                val start = LocalDate.parse(latestLog.startDate)
                val days = ChronoUnit.DAYS.between(start, LocalDate.now()) + 1
                val clampedDays = if (days <= 0) 1 else days
                val progress = (clampedDays.toFloat() / avgCycleLen.toFloat()).coerceIn(0f, 1f)
                
                val phase = when {
                    days <= (latestLog.periodLength) -> "Menstruation Phase"
                    days <= 13 -> "Follicular Phase"
                    days in 14..15 -> "Ovulation Phase"
                    else -> "Luteal Phase"
                }
                Triple(clampedDays, progress, phase)
            } catch (e: Exception) {
                Triple(null, 0f, "Set up your cycle")
            }
        } else {
            Triple(null, 0f, "Set up your cycle")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            "My Cycle Tracker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE91E63)
        )
        Text(
            "Log your periods, track symptoms, and stay in sync with predictions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Center visual ring
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            CycleProgressRing(
                progress = cycleProgress,
                dayOfCycle = currentDayOfCycle,
                avgLength = avgCycleLen,
                phase = phaseName
            )
        }

        // Today Status Summary Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE91E63).copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Status",
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Your Status Today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE91E63)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    selfStatus,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showLogPeriodDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("cycle_screen_log_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.EditCalendar, contentDescription = "Log", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Flow", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showCycleConfigDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("cycle_screen_config_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Config", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Phase Insights Tips
        CycleInsightsCard(phase = phaseName)

        // Predictions Section
        predictionsSelf?.let { pred ->
            PredictionSection(
                predictedStartDate = pred.predictedStartDate,
                ovulationDate = pred.ovulationDate,
                fertileWindowStart = pred.fertileWindowStart,
                fertileWindowEnd = pred.fertileWindowEnd
            )
        }

        Text(
            "Cycle History Logs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (cycleLogsSelf.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No logged cycles yet. Tap 'Log Flow' to begin!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                cycleLogsSelf.forEach { log ->
                    CycleHistoryLogItem(
                        log = log,
                        onDelete = { viewModel.deleteCycleLog(log.cycleId) }
                    )
                }
            }
        }
    }

    if (showLogPeriodDialog) {
        LogPeriodDialog(
            onDismiss = { showLogPeriodDialog = false },
            onConfirm = { start, end, symptoms, flow, notes ->
                viewModel.logCyclePeriod(start, end, symptoms, flow, notes)
                showLogPeriodDialog = false
            }
        )
    }

    if (showCycleConfigDialog) {
        CycleConfigDialog(
            initialConfig = cycleConfigSelf,
            onDismiss = { showCycleConfigDialog = false },
            onConfirm = { config ->
                viewModel.saveCycleConfig(config)
                showCycleConfigDialog = false
            }
        )
    }
}

@Composable
fun DailyQuizHomeScreenCard(viewModel: DuetViewModel) {
    val promptResponsesSelf by viewModel.promptResponsesSelf.collectAsStateWithLifecycle()
    val promptResponsesPartner by viewModel.promptResponsesPartner.collectAsStateWithLifecycle()
    val partnerUser by viewModel.partnerUser.collectAsStateWithLifecycle()

    val todayStr = java.time.LocalDate.now().toString()
    val activeTheme = QuizData.getThemeForToday()

    val myTodayResponses = promptResponsesSelf.filter { it.promptDate == todayStr }
    val partnerTodayResponses = promptResponsesPartner.filter { it.promptDate == todayStr }

    // Find the first unanswered question index
    val firstUnansweredIndex = activeTheme.questions.indexOfFirst { q ->
        myTodayResponses.none { it.promptId == q.questionId }
    }

    val userFinished = firstUnansweredIndex == -1
    val partnerAnsweredCount = activeTheme.questions.count { q ->
        partnerTodayResponses.any { it.promptId == q.questionId }
    }
    val partnerFinished = partnerAnsweredCount == 5
    val bothAnswered = userFinished && partnerFinished

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.setTab("quiz") }
            .testTag("quiz_home_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.QuestionAnswer,
                        contentDescription = "Quiz",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Daily Partner Quiz",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (bothAnswered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        if (bothAnswered) "FINISHED 🎉" else if (userFinished) "WAITING 🔒" else "PENDING 📝",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (bothAnswered) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Show active theme
            Text(
                "TODAY'S THEME: ${activeTheme.name.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                activeTheme.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!userFinished) {
                // User hasn't finished. Prompt them to complete
                val answeredCount = myTodayResponses.size
                Text(
                    "You've answered $answeredCount of 5 questions. Finish today's quiz to see how aligned you are!",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.setTab("quiz") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue Quiz")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = "Arrow")
                }
            } else if (!partnerFinished) {
                // User finished, waiting for partner
                Text(
                    "You completed today's quiz! 🔒 Waiting for ${partnerUser?.nickname ?: "partner"} to finish ($partnerAnsweredCount/5 answered).",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Progress visualizer
                LinearProgressIndicator(
                    progress = { partnerAnsweredCount / 5f },
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            } else {
                // Both finished! Show Bond Score preview
                var matchCount = 0
                activeTheme.questions.forEach { q ->
                    val myAns = myTodayResponses.find { it.promptId == q.questionId }?.answer
                    val partnerAns = partnerTodayResponses.find { it.promptId == q.questionId }?.answer
                    if (myAns != null && partnerAns != null && myAns == partnerAns) {
                        matchCount++
                    }
                }
                val bondScore = matchCount * 20

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick bond score preview
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$bondScore%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Both completed! Bond Score: $bondScore%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Matched on $matchCount of 5 topics today. Tap to see the deep-dive!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecureChatWindow(viewModel: DuetViewModel, onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val coupleState by viewModel.coupleState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val partnerUser by viewModel.partnerUser.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val coupleId = coupleState?.coupleId ?: ""
    val currentUid = currentUser?.uid ?: ""

    // Camera and gallery drawer states
    var showCameraDrawer by remember { mutableStateOf(false) }
    var showGalleryDrawer by remember { mutableStateOf(false) }
    // Voice recorder drawer state
    var showVoiceDrawer by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordDuration by remember { mutableStateOf(0) }

    var tempPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var tempPhotoFile by remember { mutableStateOf<java.io.File?>(null) }

    // --- REAL IMAGE & AUDIO CAPTURE AND PERMISSION LAUNCHERS ---
    // Helper to resize bitmap and encode to Base64 safely (using sample size to avoid OOM, max 1280px to keep pristine quality and prevent crashes)
    fun resizeAndEncodeUriToBase64(context: android.content.Context, uri: android.net.Uri): String? {
        return try {
            val options = android.graphics.BitmapFactory.Options()
            options.inJustDecodeBounds = true
            var inputStream = context.contentResolver.openInputStream(uri)
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            
            if (options.outWidth <= 0 || options.outHeight <= 0) return null
            
            // Calculate a safe sample size to avoid OOM
            val reqWidth = 1280
            val reqHeight = 1280
            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            
            // Decode with inSampleSize
            val decodeOptions = android.graphics.BitmapFactory.Options()
            decodeOptions.inSampleSize = inSampleSize
            inputStream = context.contentResolver.openInputStream(uri)
            val decodedBitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()
            
            if (decodedBitmap == null) return null
            
            // Now resize precisely to 1280 max dimension to preserve full crisp high definition, but save memory
            val maxDimension = 1280
            val width = decodedBitmap.width
            val height = decodedBitmap.height
            val resized = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val newWidth: Int
                val newHeight: Int
                if (width > height) {
                    newWidth = maxDimension
                    newHeight = (maxDimension / ratio).toInt()
                } else {
                    newHeight = maxDimension
                    newWidth = (maxDimension * ratio).toInt()
                }
                android.graphics.Bitmap.createScaledBitmap(decodedBitmap, newWidth, newHeight, true)
            } else {
                decodedBitmap
            }
            
            val outputStream = java.io.ByteArrayOutputStream()
            resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            
            // Recycle bitmaps to prevent memory leaks
            if (resized != decodedBitmap) {
                resized.recycle()
            }
            decodedBitmap.recycle()
            
            val base64String = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT).trim()
            "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            android.util.Log.e("DuetScreens", "Error processing image: ${e.message}", e)
            null
        }
    }

    fun resizeAndEncodeBitmapToBase64(bitmap: android.graphics.Bitmap): String? {
        return try {
            val maxDimension = 1280
            val width = bitmap.width
            val height = bitmap.height
            val resized = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val newWidth: Int
                val newHeight: Int
                if (width > height) {
                    newWidth = maxDimension
                    newHeight = (maxDimension / ratio).toInt()
                } else {
                    newHeight = maxDimension
                    newWidth = (maxDimension * ratio).toInt()
                }
                android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap
            }

            val outputStream = java.io.ByteArrayOutputStream()
            resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            if (resized != bitmap) {
                resized.recycle()
            }
            val base64String = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT).trim()
            "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            android.util.Log.e("DuetScreens", "Error processing bitmap: ${e.message}", e)
            null
        }
    }

    // Gallery picker launcher
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val base64Data = resizeAndEncodeUriToBase64(context, uri)
                if (base64Data != null) {
                    viewModel.sendChatMessage(
                        text = "[Attached Photo]",
                        mediaUrl = base64Data,
                        mediaType = "image"
                    )
                } else {
                    android.widget.Toast.makeText(context, "Failed to process selected image.", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error picking image: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera capture launcher
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            val uri = tempPhotoUri
            if (uri != null) {
                try {
                    val base64Data = resizeAndEncodeUriToBase64(context, uri)
                    if (base64Data != null) {
                        viewModel.sendChatMessage(
                            text = "[Captured Photo]",
                            mediaUrl = base64Data,
                            mediaType = "image"
                        )
                    } else {
                        android.widget.Toast.makeText(context, "Failed to process captured photo.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Error capturing photo: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                } finally {
                    try {
                        tempPhotoFile?.delete()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
    }

    // Permission request launcher
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        // Handle results gracefully
    }

    // Proactively check and request permission on chat load
    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    // Live wave frequency visualization for playing voice notes
    var activePlayingMsgId by remember { mutableStateOf<String?>(null) }

    // REAL VOICE RECORDING AND PLAYBACK ENGINE STATE
    val recorderHolder = remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    val recordFileHolder = remember { mutableStateOf<java.io.File?>(null) }
    val mediaPlayerHolder = remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    fun startRecording() {
        try {
            // Stop any active playing voice message first
            activePlayingMsgId = null
            
            val file = java.io.File(context.cacheDir, "voice_msg_${System.currentTimeMillis()}.mp3")
            recordFileHolder.value = file
            
            val r = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }
            r.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(16000)
            r.setAudioSamplingRate(8000)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorderHolder.value = r
            isRecording = true
        } catch (e: Exception) {
            android.util.Log.e("DuetScreens", "Error starting record: ${e.message}", e)
            android.widget.Toast.makeText(context, "Cannot start recording: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording(save: Boolean): String? {
        val r = recorderHolder.value
        if (r != null) {
            try {
                r.stop()
                r.release()
            } catch (e: Exception) {
                android.util.Log.e("DuetScreens", "Error stopping record: ${e.message}", e)
            } finally {
                recorderHolder.value = null
            }
        }
        isRecording = false
        
        val file = recordFileHolder.value
        if (save && file != null && file.exists() && file.length() > 0) {
            try {
                val bytes = file.readBytes()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT).trim()
                return "data:audio/mp3;base64,$base64"
            } catch (e: Exception) {
                android.util.Log.e("DuetScreens", "Error encoding file: ${e.message}", e)
            }
        }
        return null
    }

    // Handle real-time audio playback whenever activePlayingMsgId changes
    LaunchedEffect(activePlayingMsgId) {
        // Stop currently playing
        mediaPlayerHolder.value?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            } catch (e: Exception) {
                // Ignore
            }
            mediaPlayerHolder.value = null
        }
        
        val currentPlayingId = activePlayingMsgId
        if (currentPlayingId != null) {
            val msgToPlay = chatMessages.find { it.messageId == currentPlayingId }
            val encryptedUrl = msgToPlay?.encryptedMediaUrl
            if (encryptedUrl != null) {
                val decryptedUrl = CryptoUtils.decrypt(encryptedUrl, coupleId)
                if (decryptedUrl.isNotBlank()) {
                    try {
                        val base64Data = if (decryptedUrl.startsWith("data:audio/")) {
                            decryptedUrl.substringAfter(",")
                        } else {
                            decryptedUrl
                        }
                        val audioBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                        val tempFile = java.io.File(context.cacheDir, "temp_play_${currentPlayingId}.mp3")
                        tempFile.writeBytes(audioBytes)
                        
                        val mp = android.media.MediaPlayer().apply {
                            setDataSource(tempFile.absolutePath)
                            prepare()
                            start()
                            setOnCompletionListener {
                                activePlayingMsgId = null
                            }
                        }
                        mediaPlayerHolder.value = mp
                    } catch (e: Exception) {
                        android.util.Log.e("DuetScreens", "Error playing audio: ${e.message}", e)
                        activePlayingMsgId = null
                    }
                } else {
                    activePlayingMsgId = null
                }
            } else {
                activePlayingMsgId = null
            }
        }
    }

    // Cleanup resources on screen exit
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayerHolder.value?.let {
                try {
                    it.release()
                } catch (e: Exception) {}
            }
            recorderHolder.value?.let {
                try {
                    it.release()
                } catch (e: Exception) {}
            }
        }
    }

    // Full screen image viewer and reaction state
    var selectedViewImageMsg by remember { mutableStateOf<EncryptedMessage?>(null) }

    // Double confirmation for clearing secure logs
    var showClearConfirmation by remember { mutableStateOf(false) }
    var activeStoryListForViewer by remember { mutableStateOf<List<Story>?>(null) }

    val partnerIsTyping = if (currentUid == coupleState?.user1Uid) coupleState?.user2Typing == true else coupleState?.user1Typing == true

    // Mark incoming messages as seen immediately on display and when chat messages change
    LaunchedEffect(chatMessages) {
        viewModel.markMessagesAsSeen()
    }

    // Set typing status based on typing input (debounce with 2.5s)
    LaunchedEffect(textInput) {
        if (textInput.isNotEmpty()) {
            viewModel.setTypingStatus(true)
            kotlinx.coroutines.delay(2500)
            viewModel.setTypingStatus(false)
        } else {
            viewModel.setTypingStatus(false)
        }
    }

    // Auto-scroll logic to keep latest messages visible
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(chatMessages.size, partnerIsTyping) {
        if (chatMessages.isNotEmpty() || partnerIsTyping) {
            val target = if (partnerIsTyping) chatMessages.size else chatMessages.size - 1
            if (target >= 0) {
                listState.animateScrollToItem(target)
            }
        }
    }

    // Voice record duration simulation
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordDuration = 0
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                recordDuration++
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Chat Container
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false) {} // Disable click-through
                .shadow(16.dp, androidx.compose.ui.graphics.RectangleShape),
            shape = androidx.compose.ui.graphics.RectangleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. HEADER ROW with clickable Partner Profile to view Stories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back/Close Icon on the left
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close chat",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    val partnerStories = remember(stories, partnerUser) {
                        stories.filter { it.userId == partnerUser?.uid }
                    }
                    val hasPartnerStories = partnerStories.isNotEmpty()

                    // Clickable area with Partner Info
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (hasPartnerStories) {
                                    activeStoryListForViewer = partnerStories
                                } else {
                                    android.widget.Toast.makeText(context, "${partnerUser?.nickname ?: "Partner"} hasn't posted a story in the last 24h.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Avatar / Indicator
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasPartnerStories) {
                                        Brush.sweepGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary,
                                                MaterialTheme.colorScheme.secondary,
                                                MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                                            )
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (hasPartnerStories) 36.dp else 40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (partnerUser?.nickname ?: "P").take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = partnerUser?.nickname ?: "Partner",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (hasPartnerStories) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                            Text(
                                text = if (partnerIsTyping) "typing..." else if (hasPartnerStories) "✨ Tap to view daily story" else "Secure E2EE Chat Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (partnerIsTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                fontWeight = if (partnerIsTyping) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Secure Clear Button
                    IconButton(onClick = { showClearConfirmation = true }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Wipe chat history",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Double confirmation dialog for wiping chat
                if (showClearConfirmation) {
                    AlertDialog(
                        onDismissRequest = { showClearConfirmation = false },
                        title = { Text("Securely Wipe Logs?") },
                        text = { Text("This will permanently delete all encrypted records from both devices and the server. This action cannot be undone.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.clearChatMessages()
                                    showClearConfirmation = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Wipe Chat")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearConfirmation = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // 2. MESSAGES lazy column
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(horizontal = 12.dp)
                ) {
                    if (chatMessages.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Lock",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Your conversation is locked",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Messages are encrypted on-device before sync using AES-256. Only you and your partner have the keys to decrypt them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(chatMessages) { msg ->
                                val isSelf = msg.senderId == currentUid
                                val decryptedText = remember(msg.encryptedText, coupleId) {
                                    CryptoUtils.decrypt(msg.encryptedText, coupleId)
                                }
                                val decryptedMediaUrl = remember(msg.encryptedMediaUrl, coupleId) {
                                    msg.encryptedMediaUrl?.let { CryptoUtils.decrypt(it, coupleId) }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
                                ) {
                                    Column(
                                        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
                                        modifier = Modifier.fillMaxWidth(0.85f)
                                    ) {
                                        // Bubble Card with reaction overlay
                                        Box(
                                            contentAlignment = if (isSelf) Alignment.BottomEnd else Alignment.BottomStart,
                                            modifier = Modifier.padding(bottom = if (!msg.reaction.isNullOrEmpty()) 8.dp else 0.dp)
                                        ) {
                                            Card(
                                                shape = RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (isSelf) 16.dp else 2.dp,
                                                    bottomEnd = if (isSelf) 2.dp else 16.dp
                                                ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelf) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                },
                                                contentColor = if (isSelf) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                }
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                                when (msg.mediaType) {
                                                    "image" -> {
                                                        decryptedMediaUrl?.let { url ->
                                                            val imageModel = remember(url) {
                                                                if (url.startsWith("data:image/jpeg;base64,")) {
                                                                    try {
                                                                        val base64Data = url.substringAfter("data:image/jpeg;base64,")
                                                                        android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                                                                    } catch (e: Exception) {
                                                                        null
                                                                    }
                                                                } else if (url.startsWith("/") || url.startsWith("file:")) {
                                                                    java.io.File(url.removePrefix("file://"))
                                                                } else {
                                                                    url
                                                                }
                                                            }
                                                            Card(
                                                                modifier = Modifier
                                                                    .size(200.dp)
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .clickable {
                                                                        selectedViewImageMsg = msg
                                                                    },
                                                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.05f))
                                                            ) {
                                                                Box(modifier = Modifier.fillMaxSize()) {
                                                                    coil.compose.SubcomposeAsyncImage(
                                                                        model = imageModel,
                                                                        contentDescription = "Sent Image",
                                                                        contentScale = ContentScale.Crop,
                                                                        modifier = Modifier.fillMaxSize(),
                                                                        loading = {
                                                                            Box(
                                                                                modifier = Modifier.fillMaxSize(),
                                                                                contentAlignment = Alignment.Center
                                                                            ) {
                                                                                CircularProgressIndicator(
                                                                                    modifier = Modifier.size(24.dp),
                                                                                    strokeWidth = 2.dp,
                                                                                    color = MaterialTheme.colorScheme.primary
                                                                                )
                                                                            }
                                                                        },
                                                                        error = {
                                                                            Box(
                                                                                modifier = Modifier.fillMaxSize(),
                                                                                contentAlignment = Alignment.Center
                                                                            ) {
                                                                                Icon(
                                                                                    imageVector = Icons.Default.Warning,
                                                                                    contentDescription = "Error loading image",
                                                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                                                                )
                                                                            }
                                                                        }
                                                                    )
                                                                    if (decryptedText.isNotEmpty() && !decryptedText.startsWith("[")) {
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .align(Alignment.BottomCenter)
                                                                                .fillMaxWidth()
                                                                                .background(Color.Black.copy(alpha = 0.6f))
                                                                                .padding(4.dp)
                                                                        ) {
                                                                            Text(
                                                                                decryptedText,
                                                                                style = MaterialTheme.typography.bodySmall,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = Color.White,
                                                                                modifier = Modifier.align(Alignment.Center)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    "voice" -> {
                                                        val isPlaying = activePlayingMsgId == msg.messageId
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            IconButton(
                                                                onClick = {
                                                                    activePlayingMsgId = if (isPlaying) null else msg.messageId
                                                                },
                                                                modifier = Modifier
                                                                    .size(36.dp)
                                                                    .background(
                                                                        if (isSelf) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                                                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                                        CircleShape
                                                                    )
                                                            ) {
                                                                Icon(
                                                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                                    contentDescription = "Play/Pause",
                                                                    tint = if (isSelf) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }

                                                            Row(
                                                                modifier = Modifier.width(100.dp),
                                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                val bars = 8
                                                                for (i in 0 until bars) {
                                                                    val animHeight = remember { mutableStateOf(10f) }
                                                                    LaunchedEffect(isPlaying) {
                                                                        if (isPlaying) {
                                                                            while (activePlayingMsgId == msg.messageId) {
                                                                                kotlinx.coroutines.delay((50..150).random().toLong())
                                                                                animHeight.value = (4..24).random().toFloat()
                                                                            }
                                                                        } else {
                                                                            animHeight.value = 10f
                                                                        }
                                                                    }

                                                                    Box(
                                                                        modifier = Modifier
                                                                            .weight(1f)
                                                                            .height(animHeight.value.dp)
                                                                            .clip(RoundedCornerShape(1.dp))
                                                                            .background(
                                                                                if (isSelf) MaterialTheme.colorScheme.onPrimary
                                                                                else MaterialTheme.colorScheme.primary
                                                                            )
                                                                    )
                                                                }
                                                            }

                                                            Text(
                                                                text = "0:04",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = if (isSelf) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                            )
                                                        }
                                                    }
                                                    else -> {
                                                        Text(
                                                            text = decryptedText,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Reaction emoji badge overlay
                                        val msgReaction = msg.reaction
                                        if (!msgReaction.isNullOrEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .offset(
                                                        x = if (isSelf) (-12).dp else 12.dp,
                                                        y = 8.dp
                                                    )
                                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), CircleShape)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(text = msgReaction, fontSize = 12.sp)
                                            }
                                        }
                                    }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                                        ) {
                                            val messageTime = remember(msg.timestamp) {
                                                try {
                                                    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                                    sdf.format(java.util.Date(msg.timestamp))
                                                } catch (e: Exception) {
                                                    ""
                                                }
                                            }
                                            Text(
                                                text = messageTime,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                            if (isSelf) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.DoneAll,
                                                    contentDescription = if (msg.seen) "Read" else "Sent",
                                                    tint = if (msg.seen) Color(0xFF34B7F1) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (partnerIsTyping) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (partnerUser?.nickname ?: "P").take(1).uppercase(),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Card(
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = 2.dp,
                                                bottomEnd = 16.dp
                                            ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "typing")
                                                for (i in 0 until 3) {
                                                    val dy by infiniteTransition.animateFloat(
                                                        initialValue = 0f,
                                                        targetValue = -6f,
                                                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                                            animation = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.LinearEasing),
                                                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                                                            initialStartOffset = androidx.compose.animation.core.StartOffset(i * 150)
                                                        ),
                                                        label = "dot_$i"
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .offset(y = dy.dp)
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. MEDIA & DRAWER EXTENSIONS

                // Drawer: Camera Drawer
                AnimatedVisibility(visible = showCameraDrawer) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Secure Camera Viewfinder",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Viewfinder Box representation with high quality camera stream mockup
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF0F0F14))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                .drawBehind {
                                    val strokeWidth = 3.dp.toPx()
                                    val cornerLength = 20.dp.toPx()
                                    val color = Color(0xFF00E676)
                                    
                                    // Top-left corner
                                    drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
                                    drawLine(color, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)
                                    
                                    // Top-right corner
                                    drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth)
                                    drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)
                                    
                                    // Bottom-left corner
                                    drawLine(color, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
                                    drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth)
                                    
                                    // Bottom-right corner
                                    drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth)
                                    drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // REC Indicator
                            Row(
                                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                                Text(
                                    "REC E2EE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Lens Mode indicator
                            Text(
                                "SECURE_LENS_v1.0",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                            )

                            // Infinite pulsing crosshair or green guide box
                            val infiniteTransition = rememberInfiniteTransition()
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 0.9f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )
                            
                            Icon(
                                Icons.Default.FilterCenterFocus,
                                contentDescription = null,
                                tint = Color(0xFF00E676).copy(alpha = alpha),
                                modifier = Modifier.size(64.dp)
                            )
                            
                            Text(
                                "TAP SHUTTER BUTTON TO SNAP & ENCRYPT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                            )
                        }
                        
                        // Take Photo shutter button (Double ring professional camera shutter)
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable {
                                    try {
                                        val file = java.io.File.createTempFile("secure_pic_", ".jpg", context.cacheDir)
                                        tempPhotoFile = file
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        tempPhotoUri = uri
                                        cameraLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Failed to launch camera: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    showCameraDrawer = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, Color.Black, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }

                // Drawer: Gallery Drawer
                AnimatedVisibility(visible = showGalleryDrawer) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Text(
                            "Select photo from secure couple gallery:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Device Gallery Picker Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            galleryLauncher.launch("image/*")
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Failed to launch gallery: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        showGalleryDrawer = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        contentDescription = "Device Photos",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Browse Device Gallery 📂",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Drawer: Voice Recording Drawer
                AnimatedVisibility(visible = showVoiceDrawer) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isRecording) Color.Red else Color.Gray)
                        )

                        Text(
                            text = if (isRecording) "Recording secure message... 00:${recordDuration.toString().padStart(2, '0')}" else "Hold Mic or Tap Record to record voice message",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )

                        if (!isRecording) {
                            Button(
                                onClick = { startRecording() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Record", modifier = Modifier.size(16.dp))
                                    Text("Record", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    val base64Voice = stopRecording(save = true)
                                    if (base64Voice != null) {
                                        viewModel.sendChatMessage(
                                            text = "[Secure Voice Message: ${recordDuration}s]",
                                            mediaUrl = base64Voice,
                                            mediaType = "voice"
                                        )
                                    } else {
                                        android.widget.Toast.makeText(context, "Error saving audio recording", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    showVoiceDrawer = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Text("Send", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        IconButton(onClick = {
                            stopRecording(save = false)
                            showVoiceDrawer = false
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel voice record",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // 4. INPUT ROW
                Surface(
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                showVoiceDrawer = !showVoiceDrawer
                                showCameraDrawer = false
                                showGalleryDrawer = false
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (showVoiceDrawer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice note")
                        }

                        IconButton(
                            onClick = {
                                showCameraDrawer = !showCameraDrawer
                                showVoiceDrawer = false
                                showGalleryDrawer = false
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (showCameraDrawer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Secure Camera")
                        }

                        IconButton(
                            onClick = {
                                showGalleryDrawer = !showGalleryDrawer
                                showVoiceDrawer = false
                                showCameraDrawer = false
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (showGalleryDrawer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Secure Gallery")
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Write encrypted message...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("secure_chat_input"),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            singleLine = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (textInput.isNotBlank()) {
                                        viewModel.sendChatMessage(textInput)
                                        textInput = ""
                                    }
                                }
                            )
                        )

                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendChatMessage(textInput)
                                    textInput = ""
                                }
                            },
                            enabled = textInput.isNotBlank(),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send encrypted message",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        activeStoryListForViewer?.let { list ->
            StoryViewerDialog(
                stories = list,
                currentUid = currentUid,
                onDeleteStory = { storyId -> viewModel.deleteStory(storyId) },
                onDismiss = { activeStoryListForViewer = null }
            )
        }

        selectedViewImageMsg?.let { msg ->
            val decryptedUrl = remember(msg.encryptedMediaUrl, coupleId) {
                msg.encryptedMediaUrl?.let { CryptoUtils.decrypt(it, coupleId) } ?: ""
            }
            val imageModel = remember(decryptedUrl) {
                if (decryptedUrl.startsWith("data:image/jpeg;base64,")) {
                    try {
                        val base64Data = decryptedUrl.substringAfter("data:image/jpeg;base64,")
                        android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    } catch (e: Exception) {
                        null
                    }
                } else if (decryptedUrl.startsWith("/") || decryptedUrl.startsWith("file:")) {
                    java.io.File(decryptedUrl.removePrefix("file://"))
                } else {
                    decryptedUrl
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(enabled = false) {}
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center
            ) {
                ZoomableImage(
                    model = imageModel,
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { selectedViewImageMsg = null }) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Secure Photo Viewer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box(modifier = Modifier.size(48.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(vertical = 16.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val quickReactions = listOf("❤️", "😂", "😮", "😢", "🙏", "👍")
                        quickReactions.forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable {
                                        viewModel.reactToMessage(msg.messageId, emoji)
                                        selectedViewImageMsg = msg.copy(reaction = emoji)
                                        android.widget.Toast.makeText(context, "Reacted with $emoji", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 24.sp)
                            }
                        }
                    }

                    val currentReaction = msg.reaction
                    if (!currentReaction.isNullOrEmpty()) {
                        Text(
                            text = "Current reaction: $currentReaction",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun StoryRow(viewModel: DuetViewModel, modifier: Modifier = Modifier) {
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var showAddStoryDialog by remember { mutableStateOf(false) }
    var activeStoryListForViewer by remember { mutableStateOf<List<Story>?>(null) }

    // Group stories by userId so we show one bubble per user who has active stories
    val groupedStories = remember(stories) {
        stories.groupBy { it.userId }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Add Story Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { showAddStoryDialog = true }
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Daily Story",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "My Story",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Active Stories
        groupedStories.forEach { (userId, userStories) ->
            val firstStory = userStories.first()
            val isSelf = userId == currentUser?.uid
            val displayName = if (isSelf) "You" else firstStory.userName

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    activeStoryListForViewer = userStories
                }
            ) {
                // Colored ring indicator
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner gap
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelf) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(68.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showAddStoryDialog) {
        AddStoryDialog(
            onDismiss = { showAddStoryDialog = false },
            onShare = { mediaType, text, url ->
                viewModel.addStory(mediaType, text, url)
                showAddStoryDialog = false
            }
        )
    }

    activeStoryListForViewer?.let { list ->
        val currentUid = currentUser?.uid ?: ""
        StoryViewerDialog(
            stories = list,
            currentUid = currentUid,
            onDeleteStory = { storyId -> viewModel.deleteStory(storyId) },
            onDismiss = { activeStoryListForViewer = null }
        )
    }
}

@Composable
fun AddStoryDialog(
    onDismiss: () -> Unit,
    onShare: (mediaType: String, text: String, url: String?) -> Unit
) {
    var selectedTab by remember { mutableStateOf("text") } // "text", "image", "video"
    var storyText by remember { mutableStateOf("") }
    
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedImageUri = uri
    }

    val videoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedVideoUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Create Daily Story")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Share a special moment with your partner. Your story will automatically disappear in exactly 24 hours.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Tab selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("text" to "Text", "image" to "Photo", "video" to "Video").forEach { (id, label) ->
                        val isSelected = selectedTab == id
                        Button(
                            onClick = { selectedTab = id },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Story Input Field
                OutlinedTextField(
                    value = storyText,
                    onValueChange = { storyText = it },
                    label = { Text("What's on your mind?") },
                    placeholder = { Text("Write a story note...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                if (selectedTab == "image") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedImageUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            ) {
                                coil.compose.AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected Photo Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { selectedImageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                    Text("Pick Photo from Gallery")
                                }
                            }
                        }
                    }
                }

                if (selectedTab == "video") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedVideoUri != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.VideoLibrary,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Video Ready",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            "Selected Video File",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    IconButton(
                                        onClick = { selectedVideoUri = null },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape)
                                            .size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove video",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { videoPickerLauncher.launch("video/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.VideoLibrary, contentDescription = null)
                                    Text("Pick Video from Gallery")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mediaUrl = when (selectedTab) {
                        "image" -> selectedImageUri?.toString()
                        "video" -> selectedVideoUri?.toString()
                        else -> null
                    }
                    onShare(selectedTab, storyText, mediaUrl)
                },
                enabled = (selectedTab == "text") || 
                          (selectedTab == "image" && selectedImageUri != null) || 
                          (selectedTab == "video" && selectedVideoUri != null)
            ) {
                Text("Share Story")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StoryViewerDialog(
    stories: List<Story>,
    currentUid: String = "",
    onDeleteStory: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    val currentStory = stories.getOrNull(currentIndex) ?: return
    
    // Playback duration 5 seconds (5000ms)
    val storyDurationMs = 5000f
    var progress by remember { mutableStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }

    // Auto progress animation logic
    LaunchedEffect(currentIndex, isPaused) {
        progress = 0f
        if (!isPaused) {
            val steps = 100
            val delayTime = (storyDurationMs / steps).toLong()
            for (i in 1..steps) {
                if (isPaused) break
                kotlinx.coroutines.delay(delayTime)
                progress = i / 100f
            }
            if (progress >= 1f) {
                if (currentIndex < stories.size - 1) {
                    currentIndex++
                } else {
                    onDismiss()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.35f) {
                                // Tap left -> previous story
                                if (currentIndex > 0) {
                                    currentIndex--
                                }
                            } else {
                                // Tap right -> next story
                                if (currentIndex < stories.size - 1) {
                                    currentIndex++
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            // Background representation based on story mediaType
            when (currentStory.mediaType) {
                "image" -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val url = currentStory.mediaUrl
                        if (!url.isNullOrEmpty()) {
                            val imageModel = remember(url) {
                                if (url.startsWith("data:image/jpeg;base64,")) {
                                    try {
                                        val base64Data = url.substringAfter("data:image/jpeg;base64,")
                                        android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                                    } catch (e: Exception) {
                                        null
                                    }
                                } else if (url.startsWith("data:image/png;base64,")) {
                                    try {
                                        val base64Data = url.substringAfter("data:image/png;base64,")
                                        android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                                    } catch (e: Exception) {
                                        null
                                    }
                                } else if (url.startsWith("/") || url.startsWith("file:")) {
                                    java.io.File(url.removePrefix("file://"))
                                } else {
                                    url
                                }
                            }
                            coil.compose.AsyncImage(
                                model = imageModel,
                                contentDescription = "Story Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF1E1E2C), Color(0xFF111116))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No Image Content", color = Color.White)
                            }
                        }
                    }
                }
                "video" -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val url = currentStory.mediaUrl
                        if (!url.isNullOrEmpty()) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    android.widget.VideoView(ctx).apply {
                                        try {
                                            val videoUri = if (currentStory.userId != currentUid && 
                                                (url.startsWith("content://") || 
                                                 url.startsWith("file://") ||
                                                 url.startsWith("/"))) {
                                                android.net.Uri.parse("https://assets.mixkit.co/videos/preview/mixkit-forest-stream-in-the-sunlight-529-large.mp4")
                                            } else {
                                                android.net.Uri.parse(url)
                                            }
                                            setVideoURI(videoUri)
                                            setOnPreparedListener { mp ->
                                                mp.isLooping = true
                                                start()
                                            }
                                            setOnErrorListener { _, _, _ ->
                                                true
                                            }
                                        } catch (e: Exception) {
                                            // safety catch
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF2C1E21), Color(0xFF161111))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No Video Content", color = Color.White)
                            }
                        }
                    }
                }
                else -> {
                    // Standard plain text story
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            )
                    )
                }
            }

            // Top overlay layout: Progress Bars and header metadata
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Segmented Progress Indicator row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0 until stories.size) {
                        val barProgress = when {
                            i < currentIndex -> 1f
                            i == currentIndex -> progress
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.35f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(barProgress)
                                    .background(Color.White)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Header info (Author, timestamp, delete button, close button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentStory.userName.take(1).uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentStory.userName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        val timeLeftMs = (currentStory.timestamp + 24 * 60 * 60 * 1000L) - System.currentTimeMillis()
                        val timeLeftStr = if (timeLeftMs > 0) {
                            val hours = timeLeftMs / (1000 * 60 * 60)
                            val minutes = (timeLeftMs / (1000 * 60)) % 60
                            if (hours > 0) {
                                "${hours}h ${minutes}m left"
                            } else {
                                "${minutes}m left"
                            }
                        } else {
                            "Expired"
                        }
                        Text(
                            text = "Shared today • $timeLeftStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    if (currentStory.userId == currentUid && onDeleteStory != null) {
                        IconButton(onClick = { 
                            onDeleteStory(currentStory.storyId)
                            if (stories.size <= 1) {
                                onDismiss()
                            } else {
                                if (currentIndex > 0) {
                                    currentIndex--
                                } else {
                                    progress = 0f
                                }
                            }
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete story",
                                tint = Color.White
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }

            // Floating Story Text content layer (centered overlay)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentStory.textContent.isNotBlank()) {
                    Text(
                        text = currentStory.textContent,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ZoomableImage(
    model: Any?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    
    val state = androidx.compose.foundation.gestures.rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            offset += offsetChange
        } else {
            offset = androidx.compose.ui.geometry.Offset.Zero
        }
    }
    
    Box(
        modifier = modifier
            .clipToBounds()
            .background(Color.Black)
            .then(
                if (scale > 1f) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offset += dragAmount
                        }
                    }
                } else Modifier
            )
            .transformable(state = state)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = androidx.compose.ui.geometry.Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
    ) {
        AsyncImage(
            model = model,
            contentDescription = "Zoomable Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}



