package com.example.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.*
import com.example.receiver.WaterReminderReceiver
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.UUID

class DuetRepository(private val context: Context) {

    private val tag = "DuetRepository"

    // Core states
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _partnerUser = MutableStateFlow<UserProfile?>(null)
    val partnerUser: StateFlow<UserProfile?> = _partnerUser.asStateFlow()

    private val _couple = MutableStateFlow<Couple?>(null)
    val couple: StateFlow<Couple?> = _couple.asStateFlow()

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()

    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()

    private val _waterIntakeSelf = MutableStateFlow(0)
    val waterIntakeSelf: StateFlow<Int> = _waterIntakeSelf.asStateFlow()

    private val _waterIntakePartner = MutableStateFlow(0)
    val waterIntakePartner: StateFlow<Int> = _waterIntakePartner.asStateFlow()

    // --- PART A - Cycle Tracking States ---
    private val _cycleLogsSelf = MutableStateFlow<List<CycleLog>>(emptyList())
    val cycleLogsSelf: StateFlow<List<CycleLog>> = _cycleLogsSelf.asStateFlow()

    private val _cycleLogsPartner = MutableStateFlow<List<CycleLog>>(emptyList())
    val cycleLogsPartner: StateFlow<List<CycleLog>> = _cycleLogsPartner.asStateFlow()

    private val _cycleConfigSelf = MutableStateFlow<CycleConfig?>(null)
    val cycleConfigSelf: StateFlow<CycleConfig?> = _cycleConfigSelf.asStateFlow()

    private val _cycleConfigPartner = MutableStateFlow<CycleConfig?>(null)
    val cycleConfigPartner: StateFlow<CycleConfig?> = _cycleConfigPartner.asStateFlow()

    // --- PART B - Water Tracking States ---
    private val _waterGoalSelf = MutableStateFlow<WaterGoal?>(null)
    val waterGoalSelf: StateFlow<WaterGoal?> = _waterGoalSelf.asStateFlow()

    private val _waterGoalPartner = MutableStateFlow<WaterGoal?>(null)
    val waterGoalPartner: StateFlow<WaterGoal?> = _waterGoalPartner.asStateFlow()

    private val _waterLogsSelf = MutableStateFlow<List<WaterLog>>(emptyList())
    val waterLogsSelf: StateFlow<List<WaterLog>> = _waterLogsSelf.asStateFlow()

    private val _waterLogsPartner = MutableStateFlow<List<WaterLog>>(emptyList())
    val waterLogsPartner: StateFlow<List<WaterLog>> = _waterLogsPartner.asStateFlow()

    private val _waterComments = MutableStateFlow<List<WaterComment>>(emptyList())
    val waterComments: StateFlow<List<WaterComment>> = _waterComments.asStateFlow()

    // --- PART C - Mood Tracking States ---
    private val _moodEntriesSelf = MutableStateFlow<List<MoodEntry>>(emptyList())
    val moodEntriesSelf: StateFlow<List<MoodEntry>> = _moodEntriesSelf.asStateFlow()

    private val _moodEntriesPartner = MutableStateFlow<List<MoodEntry>>(emptyList())
    val moodEntriesPartner: StateFlow<List<MoodEntry>> = _moodEntriesPartner.asStateFlow()

    // --- PART D - Daily Partner Quiz States ---
    private val _promptResponsesSelf = MutableStateFlow<List<PromptResponse>>(emptyList())
    val promptResponsesSelf: StateFlow<List<PromptResponse>> = _promptResponsesSelf.asStateFlow()

    private val _promptResponsesPartner = MutableStateFlow<List<PromptResponse>>(emptyList())
    val promptResponsesPartner: StateFlow<List<PromptResponse>> = _promptResponsesPartner.asStateFlow()

    // --- PART E - Shared Todo Comments States ---
    private val _todoComments = MutableStateFlow<List<TodoComment>>(emptyList())
    val todoComments: StateFlow<List<TodoComment>> = _todoComments.asStateFlow()

    // --- E2EE Chat States ---
    private val _chatMessages = MutableStateFlow<List<EncryptedMessage>>(emptyList())
    val chatMessages: StateFlow<List<EncryptedMessage>> = _chatMessages.asStateFlow()

    // --- Story States ---
    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()


    // Configuration states
    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _isFirebaseInitialized = MutableStateFlow(false)
    val isFirebaseInitialized: StateFlow<Boolean> = _isFirebaseInitialized.asStateFlow()

    private val _isCloudSyncEnabled = MutableStateFlow(false)
    val isCloudSyncEnabled: StateFlow<Boolean> = _isCloudSyncEnabled.asStateFlow()

    private val prefs by lazy { context.getSharedPreferences("duet_prefs", Context.MODE_PRIVATE) }

    private fun saveWaterStateToPrefs() {
        val target = _waterGoalSelf.value?.dailyGoalMl ?: 2000
        val intake = _waterIntakeSelf.value
        prefs.edit().apply {
            putInt("water_target_ml", target)
            putInt("water_intake_ml", intake)
            putString("water_state_date", java.time.LocalDate.now().toString())
            apply()
        }
        try {
            com.example.widget.Water1x1WidgetProvider.refreshAllWaterWidgets(context)
        } catch (e: Exception) {
            Log.e(tag, "Failed to refresh water widgets", e)
        }
    }

    private fun saveTodosToPrefs(list: List<TodoItem>) {
        try {
            val arr = org.json.JSONArray()
            list.forEach { item ->
                val json = org.json.JSONObject().apply {
                    put("todoId", item.todoId)
                    put("title", item.title)
                    put("isCompleted", item.isCompleted)
                    put("createdBy", item.createdBy)
                    put("createdAt", item.createdAt)
                }
                arr.put(json)
            }
            prefs.edit().putString("cached_todos_json", arr.toString()).apply()
            Log.d(tag, "Saved ${list.size} todos to cached_todos_json")
            try {
                com.example.widget.Todo2x2WidgetProvider.triggerRefresh(context)
            } catch (e: Exception) {
                Log.e(tag, "Failed to refresh todo widgets", e)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to save cached todos", e)
        }
    }

    // Firebase references
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var storage: com.google.firebase.storage.FirebaseStorage? = null
    private var userListener: ListenerRegistration? = null
    private var partnerListener: ListenerRegistration? = null
    
    // Flags to track initial loads of snapshot listeners to avoid notifying on existing items
    private var isInitialEvents = true
    private var isInitialTodos = true
    private var isInitialWaterComments = true
    private var isInitialTodoComments = true
    private var isInitialChat = true
    private var isInitialStories = true
    private var isInitialWaterLogsPartner = true
    private var isInitialMoodsPartner = true
    private var isInitialQuizPartner = true
    private var coupleListener: ListenerRegistration? = null
    private var eventsListener: ListenerRegistration? = null
    private var todosListener: ListenerRegistration? = null
    private var waterListenerSelf: ListenerRegistration? = null
    private var waterListenerPartner: ListenerRegistration? = null

    // Cycle tracking real-time listeners
    private var cycleLogsListenerSelf: ListenerRegistration? = null
    private var cycleLogsListenerPartner: ListenerRegistration? = null
    private var cycleConfigListenerSelf: ListenerRegistration? = null
    private var cycleConfigListenerPartner: ListenerRegistration? = null

    // Water tracking real-time listeners
    private var waterGoalListenerSelf: ListenerRegistration? = null
    private var waterGoalListenerPartner: ListenerRegistration? = null
    private var waterLogsListenerSelf: ListenerRegistration? = null
    private var waterLogsListenerPartner: ListenerRegistration? = null
    private var waterCommentsListener: ListenerRegistration? = null

    // Mood, Quiz, and Todo Comments listeners
    private var moodListenerSelf: ListenerRegistration? = null
    private var moodListenerPartner: ListenerRegistration? = null
    private var quizListenerSelf: ListenerRegistration? = null
    private var quizListenerPartner: ListenerRegistration? = null
    private var todoCommentsListener: ListenerRegistration? = null
    private var chatListener: ListenerRegistration? = null
    private var storiesListener: ListenerRegistration? = null

    // Simple in-memory database for Demo Mode
    private val demoUsers = mutableMapOf<String, UserProfile>()
    private val demoCouples = mutableMapOf<String, Couple>()
    private val demoEvents = mutableListOf<CalendarEvent>()
    private val demoTodos = mutableListOf<TodoItem>()
    private val demoChatMessages = mutableListOf<EncryptedMessage>()
    private val demoStories = mutableListOf<Story>()

    private val demoWaterIntakes = mutableMapOf<String, Int>() // userId_date -> amountMl

    // In-memory containers for Cycle Tracking and Water expanded features
    private val demoCycleLogs = mutableListOf<CycleLog>()
    private val demoCycleConfigs = mutableMapOf<String, CycleConfig>()
    private val demoWaterGoals = mutableMapOf<String, WaterGoal>()
    private val demoWaterLogs = mutableListOf<WaterLog>()
    private val demoWaterComments = mutableListOf<WaterComment>()

    // In-memory containers for Mood, Quiz, and Todo Comments
    private val demoMoodEntries = mutableListOf<MoodEntry>()
    private val demoPromptResponses = mutableListOf<PromptResponse>()
    private val demoTodoComments = mutableListOf<TodoComment>()

    init {
        checkFirebase()
        if (_isDemoMode.value) {
            setupDemoDatabase()
            val currentUid = prefs.getString("demo_uid", "user_alpha_id")
            _currentUser.value = demoUsers[currentUid] ?: demoUsers.values.firstOrNull()
        } else {
            val currentUid = auth?.currentUser?.uid
            if (currentUid != null) {
                setupRealFirebaseListeners(currentUid)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                _todos.collect { list ->
                    saveTodosToPrefs(list)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error collecting todos in init", e)
            }
        }
    }

    private fun checkFirebase() {
        try {
            // Check if Firebase was initialized or can be initialized
            val app = FirebaseApp.getInstance()
            if (app != null) {
                auth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()
                storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                _isFirebaseInitialized.value = true
                Log.d(tag, "Firebase initialized successfully with Storage.")
            }
        } catch (e: Exception) {
            Log.w(tag, "Firebase app is not initialized. Falling back to configuration guidance mode.")
            _isFirebaseInitialized.value = false
        }
        val savedDemoMode = prefs.getBoolean("is_demo_mode_enabled", false)
        _isDemoMode.value = savedDemoMode
    }

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
        prefs.edit().putBoolean("is_demo_mode_enabled", enabled).apply()
        if (enabled) {
            setupDemoDatabase()
            // Reset current user to first demo profile if none is active
            val currentUid = prefs.getString("demo_uid", null)
            if (currentUid != null && demoUsers.containsKey(currentUid)) {
                _currentUser.value = demoUsers[currentUid]
            } else {
                val firstDemoUid = demoUsers.keys.firstOrNull() ?: "user_alpha_id"
                _currentUser.value = demoUsers[firstDemoUid]
                prefs.edit().putString("demo_uid", firstDemoUid).apply()
            }
        } else {
            val currentUid = auth?.currentUser?.uid
            if (currentUid != null) {
                setupRealFirebaseListeners(currentUid)
            } else {
                _currentUser.value = null
            }
        }
    }

    // --- SETUP DEMO SIMULATOR DATABASE ---
    private fun setupDemoDatabase() {
        demoUsers.clear()
        demoCouples.clear()
        demoEvents.clear()
        demoTodos.clear()
        demoWaterIntakes.clear()
        demoCycleLogs.clear()
        demoCycleConfigs.clear()
        demoWaterGoals.clear()
        demoWaterLogs.clear()
        demoWaterComments.clear()

        // Create standard demo profiles
        val userA = UserProfile(
            uid = "user_alpha_id",
            name = "Sarah Jenkins",
            nickname = "Sarah 🌸",
            photoUrl = "avatar_f_1",
            dob = "1998-04-12",
            gender = "Female"
        )
        val userB = UserProfile(
            uid = "user_beta_id",
            name = "Alex Mercer",
            nickname = "Alex 🚀",
            photoUrl = "avatar_m_1",
            dob = "1996-09-24",
            gender = "Male"
        )

        demoUsers[userA.uid] = userA
        demoUsers[userB.uid] = userB

        // Initially we can start paired for a smooth demo, or unpaired to let them pair
        val sampleCouple = Couple(
            coupleId = "couple_shared_id",
            user1Uid = userA.uid,
            user2Uid = userB.uid,
            pairCode = "888888",
            relationshipStartDate = "2024-02-14"
        )
        demoCouples[sampleCouple.coupleId] = sampleCouple

        // Add some default events
        val today = LocalDate.now().toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()

        demoEvents.add(CalendarEvent(
            eventId = "event_1",
            coupleId = sampleCouple.coupleId,
            title = "First Date Anniversary!",
            date = "2024-02-14",
            type = "moment",
            note = "Where we had that amazing pasta and it started raining.",
            createdBy = userA.uid
        ))

        demoEvents.add(CalendarEvent(
            eventId = "event_2",
            coupleId = sampleCouple.coupleId,
            title = "Movie Night 🍿",
            date = today,
            type = "reminder",
            note = "Watch that new sci-fi film we've been talking about.",
            createdBy = userB.uid
        ))

        demoEvents.add(CalendarEvent(
            eventId = "event_3",
            coupleId = sampleCouple.coupleId,
            title = "Weekend Picnic 🧺",
            date = tomorrow,
            type = "moment",
            note = "Pack cheese, strawberries, and that sparkling cider.",
            createdBy = userA.uid
        ))

        // Add some todos
        demoTodos.add(TodoItem(todoId = "todo_1", title = "Buy groceries for dinner", isCompleted = false, createdBy = userA.uid))
        demoTodos.add(TodoItem(todoId = "todo_2", title = "Book weekend tickets", isCompleted = true, createdBy = userB.uid))
        demoTodos.add(TodoItem(todoId = "todo_3", title = "Wash the car", isCompleted = false, createdBy = userB.uid))

        // Add water intakes
        demoWaterIntakes["${userA.uid}_$today"] = 1200
        demoWaterIntakes["${userB.uid}_$today"] = 800

        // PART A: Add standard preloaded cycle logs for Sarah (User A) to demonstrate predictions right away
        // May 10 to May 15
        demoCycleLogs.add(CycleLog(
            cycleId = "cycle_may",
            coupleId = sampleCouple.coupleId,
            userId = userA.uid,
            startDate = "2026-05-10",
            endDate = "2026-05-15",
            periodLength = 5,
            symptoms = listOf("Cramps", "Fatigue"),
            flowIntensity = "Medium",
            notes = "Felt tired on day 1."
        ))
        // June 8 to June 13 (29 days after May 10)
        demoCycleLogs.add(CycleLog(
            cycleId = "cycle_june",
            coupleId = sampleCouple.coupleId,
            userId = userA.uid,
            startDate = "2026-06-08",
            endDate = "2026-06-13",
            periodLength = 5,
            symptoms = listOf("Headache", "Cramps"),
            flowIntensity = "Heavy",
            notes = "Calculated cycle length of 29 days."
        ))

        // Initialize onboarding configs
        demoCycleConfigs[userA.uid] = CycleConfig(userId = userA.uid, avgCycleLength = 29, avgPeriodLength = 5, isPrivacyEnabled = false)
        demoCycleConfigs[userB.uid] = CycleConfig(userId = userB.uid, avgCycleLength = 28, avgPeriodLength = 5, isPrivacyEnabled = true) // Hide male from cycle by default or toggle

        // PART B: Add preloaded water logs for Sarah and Alex
        val logDays = listOf(
            LocalDate.now().minusDays(2).toString(),
            LocalDate.now().minusDays(1).toString(),
            today
        )
        // Sarah logs
        demoWaterLogs.add(WaterLog(UUID.randomUUID().toString(), userA.uid, logDays[0], 1500, System.currentTimeMillis() - 172800000))
        demoWaterLogs.add(WaterLog(UUID.randomUUID().toString(), userA.uid, logDays[1], 1800, System.currentTimeMillis() - 86400000))
        demoWaterLogs.add(WaterLog(UUID.randomUUID().toString(), userA.uid, logDays[2], 1200, System.currentTimeMillis()))

        // Alex logs
        demoWaterLogs.add(WaterLog(UUID.randomUUID().toString(), userB.uid, logDays[0], 2000, System.currentTimeMillis() - 172800000))
        demoWaterLogs.add(WaterLog(UUID.randomUUID().toString(), userB.uid, logDays[1], 1500, System.currentTimeMillis() - 86400000))
        demoWaterLogs.add(WaterLog(UUID.randomUUID().toString(), userB.uid, logDays[2], 800, System.currentTimeMillis()))

        // Set default water goals (Calculated: Sarah 55kg * 35 = 1925 -> 1950ml; Alex 78kg * 35 = 2730 -> 2750ml)
        demoWaterGoals[userA.uid] = WaterGoal(userId = userA.uid, dailyGoalMl = 1950, isManual = false, age = 28, weightKg = 55)
        demoWaterGoals[userB.uid] = WaterGoal(userId = userB.uid, dailyGoalMl = 2750, isManual = false, age = 30, weightKg = 78)

        // Add some preloaded comments
        demoWaterComments.add(WaterComment(
            commentId = "comm_1",
            userId = userB.uid,
            date = today,
            text = "Keep drinking! 🥤 You're doing great!",
            emojiReaction = "❤️",
            timestamp = System.currentTimeMillis()
        ))

        // Commented out to allow fresh profile creation on startup
        // switchDemoUser(userA.uid)
    }

    // Fast simulator switcher so user can play both roles
    fun switchDemoUser(uid: String) {
        if (!_isDemoMode.value) return
        val user = demoUsers[uid] ?: return
        _currentUser.value = user

        // Find couple status
        val coupleObj = demoCouples.values.find { it.user1Uid == uid || it.user2Uid == uid }
        _couple.value = coupleObj

        if (coupleObj != null) {
            val partnerId = if (coupleObj.user1Uid == uid) coupleObj.user2Uid else coupleObj.user1Uid
            _partnerUser.value = demoUsers[partnerId]

            // Synchronize items
            _events.value = demoEvents.filter { it.coupleId == coupleObj.coupleId }.sortedBy { it.date }
            _todos.value = demoTodos.filter { it.createdBy == user.uid || it.createdBy == partnerId }

            // Synchronize Cycle Tracking
            _cycleLogsSelf.value = demoCycleLogs.filter { it.userId == user.uid }.sortedBy { it.startDate }
            _cycleLogsPartner.value = demoCycleLogs.filter { it.userId == partnerId }.sortedBy { it.startDate }
            _cycleConfigSelf.value = demoCycleConfigs[user.uid] ?: CycleConfig(userId = user.uid)
            _cycleConfigPartner.value = demoCycleConfigs[partnerId] ?: CycleConfig(userId = partnerId)

            // Synchronize Water Tracking
            _waterGoalSelf.value = demoWaterGoals[user.uid] ?: WaterGoal(userId = user.uid, dailyGoalMl = 2000)
            _waterGoalPartner.value = demoWaterGoals[partnerId] ?: WaterGoal(userId = partnerId, dailyGoalMl = 2000)
            _waterLogsSelf.value = demoWaterLogs.filter { it.userId == user.uid }.sortedBy { it.timestamp }
            _waterLogsPartner.value = demoWaterLogs.filter { it.userId == partnerId }.sortedBy { it.timestamp }
            _waterComments.value = demoWaterComments.filter { it.date == LocalDate.now().toString() }

            // Synchronize Mood Sharing
            _moodEntriesSelf.value = demoMoodEntries.filter { it.userId == user.uid }.sortedByDescending { it.timestamp }
            _moodEntriesPartner.value = demoMoodEntries.filter { it.userId == partnerId }.sortedByDescending { it.timestamp }

            // Synchronize Daily Partner Quiz
            _promptResponsesSelf.value = demoPromptResponses.filter { it.userId == user.uid }
            _promptResponsesPartner.value = demoPromptResponses.filter { it.userId == partnerId }

            // Synchronize Todo Comments
            _todoComments.value = demoTodoComments.filter { comment ->
                val myTodos = demoTodos.filter { it.createdBy == user.uid || it.createdBy == partnerId }.map { it.todoId }
                comment.todoId in myTodos
            }

            val today = LocalDate.now().toString()
            _waterIntakeSelf.value = _waterLogsSelf.value.filter { it.date == today }.sumOf { it.amountMl }
            _waterIntakePartner.value = _waterLogsPartner.value.filter { it.date == today }.sumOf { it.amountMl }
        } else {
            _partnerUser.value = null
            _events.value = emptyList()
            _todos.value = emptyList()
            _waterIntakeSelf.value = 0
            _waterIntakePartner.value = 0
            _cycleLogsSelf.value = emptyList()
            _cycleLogsPartner.value = emptyList()
            _cycleConfigSelf.value = null
            _cycleConfigPartner.value = null
            _waterGoalSelf.value = null
            _waterGoalPartner.value = null
            _waterLogsSelf.value = emptyList()
            _waterLogsPartner.value = emptyList()
            _waterComments.value = emptyList()
            _moodEntriesSelf.value = emptyList()
            _moodEntriesPartner.value = emptyList()
            _promptResponsesSelf.value = emptyList()
            _promptResponsesPartner.value = emptyList()
            _todoComments.value = emptyList()
        }
    }

    fun getDemoUsersList(): List<UserProfile> {
        return demoUsers.values.toList()
    }

    fun createDemoUser(name: String, nickname: String, dob: String, gender: String, isPartner: Boolean = false): String {
        val newUid = "demo_user_${UUID.randomUUID().toString().take(6)}"
        val newProfile = UserProfile(
            uid = newUid,
            name = name,
            nickname = nickname,
            photoUrl = if (gender.lowercase() == "female") "avatar_f_2" else "avatar_m_2",
            dob = dob,
            gender = gender
        )
        demoUsers[newUid] = newProfile
        if (!isPartner) {
            switchDemoUser(newUid)
        }
        return newUid
    }

    // --- REAL FIREBASE LISTENERS ---
    private fun setupRealFirebaseListeners(uid: String) {
        cleanupRealFirebaseListeners()

        val db = firestore ?: return

        // 1. Listen to current user profile
        userListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to current user: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    _currentUser.value = profile
                }
            }

        // 2. Listen to current user cycle config
        cycleConfigListenerSelf = db.collection("users").document(uid).collection("cycle_configs").document("config")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _cycleConfigSelf.value = snapshot.toObject(CycleConfig::class.java)
                }
            }

        // 3. Listen to current user cycle logs
        cycleLogsListenerSelf = db.collection("users").document(uid).collection("cycle_logs")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _cycleLogsSelf.value = snapshot.toObjects(CycleLog::class.java).sortedBy { it.startDate }
                }
            }

        // 4. Listen to current user water goal
        waterGoalListenerSelf = db.collection("users").document(uid).collection("water_goals").document("goal")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _waterGoalSelf.value = snapshot.toObject(WaterGoal::class.java)
                    saveWaterStateToPrefs()
                }
            }

        // 5. Listen to current user water logs
        waterLogsListenerSelf = db.collection("users").document(uid).collection("water_logs")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val logs = snapshot.toObjects(WaterLog::class.java).sortedBy { it.timestamp }
                    _waterLogsSelf.value = logs
                    val todayStr = LocalDate.now().toString()
                    _waterIntakeSelf.value = logs.filter { it.date == todayStr }.sumOf { it.amountMl }
                    saveWaterStateToPrefs()
                }
            }

        // Listen to current user mood logs
        moodListenerSelf = db.collection("users").document(uid).collection("moods")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _moodEntriesSelf.value = snapshot.toObjects(MoodEntry::class.java).sortedByDescending { it.timestamp }
                }
            }

        // Listen to current user quiz responses
        quizListenerSelf = db.collection("users").document(uid).collection("quiz_responses")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _promptResponsesSelf.value = snapshot.toObjects(PromptResponse::class.java)
                }
            }

        // 6. Listen to couple status (where current user is user1 or user2)
        coupleListener = db.collection("couples")
            .whereIn("user1Uid", listOf(uid))
            .addSnapshotListener { snapshot1, _ ->
                if (snapshot1 != null && !snapshot1.isEmpty) {
                    handleCoupleSnapshot(snapshot1.documents.first(), uid)
                } else {
                    db.collection("couples")
                        .whereIn("user2Uid", listOf(uid))
                        .addSnapshotListener { snapshot2, _ ->
                            if (snapshot2 != null && !snapshot2.isEmpty) {
                                handleCoupleSnapshot(snapshot2.documents.first(), uid)
                            } else {
                                // Not paired
                                _couple.value = null
                                _partnerUser.value = null
                                _events.value = emptyList()
                                cleanupPartnerAndSharedListeners()
                            }
                        }
                }
            }
    }

    private fun handleCoupleSnapshot(doc: DocumentSnapshot, currentUid: String) {
        val coupleObj = doc.toObject(Couple::class.java) ?: return
        _couple.value = coupleObj

        val partnerUid = if (coupleObj.user1Uid == currentUid) coupleObj.user2Uid else coupleObj.user1Uid
        val db = firestore ?: return

        // Cleanup any old listeners first to prevent duplicates/leaks
        cleanupPartnerAndSharedListeners()

        // Listen to shared events
        eventsListener = db.collection("events")
            .whereEqualTo("coupleId", coupleObj.coupleId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.toObjects(CalendarEvent::class.java)
                    _events.value = list.sortedBy { it.date }
                    if (!isInitialEvents) {
                        val lastEvent = list.maxByOrNull { it.createdAt }
                        if (lastEvent != null && lastEvent.createdBy == partnerUid && System.currentTimeMillis() - lastEvent.createdAt < 15000) {
                            postPartnerNotification("📅 Shared Calendar", "${_partnerUser.value?.nickname ?: "Partner"} added an event: ${lastEvent.title}")
                        }
                    } else {
                        isInitialEvents = false
                    }
                }
            }

        // Listen to todos
        todosListener = db.collection("couples").document(coupleObj.coupleId).collection("todos")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.toObjects(TodoItem::class.java)
                    _todos.value = list.sortedByDescending { it.createdAt }
                    if (!isInitialTodos) {
                        val lastTodo = list.maxByOrNull { it.createdAt }
                        if (lastTodo != null && lastTodo.createdBy == partnerUid && System.currentTimeMillis() - lastTodo.createdAt < 15000) {
                            postPartnerNotification("✏️ Couple To-Do List", "${_partnerUser.value?.nickname ?: "Partner"} added a task: ${lastTodo.title}")
                        }
                    } else {
                        isInitialTodos = false
                    }
                }
            }

        // Listen to water comments
        waterCommentsListener = db.collection("couples").document(coupleObj.coupleId).collection("water_comments")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val todayStr = LocalDate.now().toString()
                    val list = snapshot.toObjects(WaterComment::class.java)
                    _waterComments.value = list.filter { it.date == todayStr }.sortedByDescending { it.timestamp }
                    if (!isInitialWaterComments) {
                        val lastComment = list.maxByOrNull { it.timestamp }
                        if (lastComment != null && lastComment.userId == partnerUid && System.currentTimeMillis() - lastComment.timestamp < 15000) {
                            postPartnerNotification("💧 Hydration Cheering", "${_partnerUser.value?.nickname ?: "Partner"} cheered you: ${lastComment.text}")
                        }
                    } else {
                        isInitialWaterComments = false
                    }
                }
            }

        // Listen to shared todo comments
        todoCommentsListener = db.collection("couples").document(coupleObj.coupleId).collection("todo_comments")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.toObjects(TodoComment::class.java)
                    _todoComments.value = list.sortedBy { it.createdAt }
                    if (!isInitialTodoComments) {
                        val lastComment = list.maxByOrNull { it.createdAt }
                        if (lastComment != null && lastComment.userId == partnerUid && System.currentTimeMillis() - lastComment.createdAt < 15000) {
                            postPartnerNotification("✏️ To-Do Comment", "${_partnerUser.value?.nickname ?: "Partner"} commented: ${lastComment.text}")
                        }
                    } else {
                        isInitialTodoComments = false
                    }
                }
            }

        // Listen to E2EE Chat Messages
        chatListener = db.collection("couples").document(coupleObj.coupleId).collection("chat_messages")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.toObjects(EncryptedMessage::class.java)
                    _chatMessages.value = list.sortedBy { it.timestamp }
                    if (!isInitialChat) {
                        val lastMsg = list.maxByOrNull { it.timestamp }
                        if (lastMsg != null && lastMsg.senderId == partnerUid && System.currentTimeMillis() - lastMsg.timestamp < 15000) {
                            val displayText = if (lastMsg.mediaType == "image") "Sent a photo 📸" else if (lastMsg.mediaType == "voice") "Sent a voice message 🎙️" else "New message"
                            postPartnerNotification("💬 Secure Chat", "${_partnerUser.value?.nickname ?: "Partner"}: $displayText", "chat")
                        }
                    } else {
                        isInitialChat = false
                    }
                }
            }

        // Listen to Stories
        storiesListener = db.collection("couples").document(coupleObj.coupleId).collection("stories")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                    val list = snapshot.toObjects(Story::class.java)
                    _stories.value = list.filter { it.timestamp >= twentyFourHoursAgo }.sortedBy { it.timestamp }
                    if (!isInitialStories) {
                        val lastStory = list.maxByOrNull { it.timestamp }
                        if (lastStory != null && lastStory.userId == partnerUid && System.currentTimeMillis() - lastStory.timestamp < 15000) {
                            postPartnerNotification("🎥 Partner Story", "${_partnerUser.value?.nickname ?: "Partner"} posted a new 24h story!")
                        }
                    } else {
                        isInitialStories = false
                    }
                }
            }

        // Deprecated compatibility daily water intakes (legacy listener kept for fallback stability)
        val today = LocalDate.now().toString()
        waterListenerSelf = db.collection("users").document(currentUid).collection("water").document(today)
            .addSnapshotListener { snapshot, _ ->
                val fbVal = snapshot?.getLong("amountMl")?.toInt() ?: 0
                if (fbVal > _waterIntakeSelf.value) {
                    _waterIntakeSelf.value = fbVal
                }
            }

        // ONLY listen to partner collections if there is an active partner paired (partnerUid is not empty)
        if (partnerUid.isNotEmpty()) {
            // Listen to partner profile
            partnerListener = db.collection("users").document(partnerUid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        _partnerUser.value = snapshot.toObject(UserProfile::class.java)
                    }
                }

            // Listen to partner cycle config
            cycleConfigListenerPartner = db.collection("users").document(partnerUid).collection("cycle_configs").document("config")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        _cycleConfigPartner.value = snapshot.toObject(CycleConfig::class.java)
                    }
                }

            // Listen to partner cycle logs
            cycleLogsListenerPartner = db.collection("users").document(partnerUid).collection("cycle_logs")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        _cycleLogsPartner.value = snapshot.toObjects(CycleLog::class.java).sortedBy { it.startDate }
                    }
                }

            // Listen to partner water goal
            waterGoalListenerPartner = db.collection("users").document(partnerUid).collection("water_goals").document("goal")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        _waterGoalPartner.value = snapshot.toObject(WaterGoal::class.java)
                    }
                }

            // Listen to partner water logs
            waterLogsListenerPartner = db.collection("users").document(partnerUid).collection("water_logs")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val logs = snapshot.toObjects(WaterLog::class.java).sortedBy { it.timestamp }
                        _waterLogsPartner.value = logs
                        val todayStr = LocalDate.now().toString()
                        _waterIntakePartner.value = logs.filter { it.date == todayStr }.sumOf { it.amountMl }
                        if (!isInitialWaterLogsPartner) {
                            val lastLog = logs.maxByOrNull { it.timestamp }
                            if (lastLog != null && lastLog.userId == partnerUid && System.currentTimeMillis() - lastLog.timestamp < 15000) {
                                postPartnerNotification("💧 Hydration Tracker", "${_partnerUser.value?.nickname ?: "Partner"} logged ${lastLog.amountMl} ml of water! 🥤")
                            }
                        } else {
                            isInitialWaterLogsPartner = false
                        }
                    }
                }

            waterListenerPartner = db.collection("users").document(partnerUid).collection("water").document(today)
                .addSnapshotListener { snapshot, _ ->
                    val fbVal = snapshot?.getLong("amountMl")?.toInt() ?: 0
                    if (fbVal > _waterIntakePartner.value) {
                        _waterIntakePartner.value = fbVal
                    }
                }

            // Listen to partner mood logs
            moodListenerPartner = db.collection("users").document(partnerUid).collection("moods")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val list = snapshot.toObjects(MoodEntry::class.java)
                        _moodEntriesPartner.value = list.sortedByDescending { it.timestamp }
                        if (!isInitialMoodsPartner) {
                            val lastMood = list.maxByOrNull { it.timestamp }
                            if (lastMood != null && lastMood.userId == partnerUid && System.currentTimeMillis() - lastMood.timestamp < 15000) {
                                postPartnerNotification("💖 Mood & Emotion", "${_partnerUser.value?.nickname ?: "Partner"} logged a new mood: ${lastMood.moodEmoji}")
                            }
                        } else {
                            isInitialMoodsPartner = false
                        }
                    }
                }

            // Listen to partner quiz responses
            quizListenerPartner = db.collection("users").document(partnerUid).collection("quiz_responses")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val list = snapshot.toObjects(PromptResponse::class.java)
                        _promptResponsesPartner.value = list
                        if (!isInitialQuizPartner) {
                            val lastQuiz = list.maxByOrNull { it.submittedAt }
                            if (lastQuiz != null && lastQuiz.userId == partnerUid && System.currentTimeMillis() - lastQuiz.submittedAt < 15000) {
                                postPartnerNotification("❓ Daily Couple Quiz", "${_partnerUser.value?.nickname ?: "Partner"} answered the daily quiz prompt! 💜")
                            }
                        } else {
                            isInitialQuizPartner = false
                        }
                    }
                }
        } else {
            // Reset partner states to defaults when no partner is paired yet
            _partnerUser.value = null
            _cycleConfigPartner.value = null
            _cycleLogsPartner.value = emptyList()
            _waterGoalPartner.value = null
            _waterLogsPartner.value = emptyList()
            _waterIntakePartner.value = 0
            _moodEntriesPartner.value = emptyList()
            _promptResponsesPartner.value = emptyList()
        }
    }

    private fun cleanupPartnerAndSharedListeners() {
        partnerListener?.remove()
        eventsListener?.remove()
        todosListener?.remove()
        waterListenerSelf?.remove()
        waterListenerPartner?.remove()
        cycleConfigListenerPartner?.remove()
        cycleLogsListenerPartner?.remove()
        waterGoalListenerPartner?.remove()
        waterLogsListenerPartner?.remove()
        waterCommentsListener?.remove()
        moodListenerPartner?.remove()
        quizListenerPartner?.remove()
        todoCommentsListener?.remove()
        chatListener?.remove()
        _chatMessages.value = emptyList()
        storiesListener?.remove()
        _stories.value = emptyList()

        // Reset flags
        isInitialEvents = true
        isInitialTodos = true
        isInitialWaterComments = true
        isInitialTodoComments = true
        isInitialChat = true
        isInitialStories = true
        isInitialWaterLogsPartner = true
        isInitialMoodsPartner = true
        isInitialQuizPartner = true
    }

    fun postPartnerNotification(title: String, message: String, navigateTo: String? = null) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "partner_updates"
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Partner Activities",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Updates on what your partner is up to"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val mainIntent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (navigateTo != null) {
                putExtra("navigate_to", navigateTo)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.example.R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun cleanup() {
        cleanupRealFirebaseListeners()
    }

    private fun cleanupRealFirebaseListeners() {
        userListener?.remove()
        coupleListener?.remove()
        cycleConfigListenerSelf?.remove()
        cycleLogsListenerSelf?.remove()
        waterGoalListenerSelf?.remove()
        waterLogsListenerSelf?.remove()
        moodListenerSelf?.remove()
        quizListenerSelf?.remove()
        cleanupPartnerAndSharedListeners()
    }

    // --- INTERACTION APIS ---

    // Sign Up Flow
    suspend fun signUpWithEmail(email: String, password: String, name: String, nickname: String, dob: String, gender: String): Result<UserProfile> {
        return if (_isDemoMode.value) {
            if (_isCloudSyncEnabled.value) {
                val sanitized = sanitizeEmail(email)
                val existingUid = withContext(Dispatchers.IO) { kvdbGet("email_$sanitized") }
                if (existingUid != null && existingUid.isNotEmpty()) {
                    return Result.failure(Exception("An account with this email already exists!"))
                }
                
                val newUid = "user_" + UUID.randomUUID().toString().take(8)
                val newProfile = UserProfile(
                    uid = newUid,
                    name = name,
                    nickname = nickname,
                    dob = dob,
                    gender = gender
                )
                
                val uploadSuccess = withContext(Dispatchers.IO) {
                    val s1 = kvdbPut("email_$sanitized", newUid)
                    val myStateStr = serializeUserSyncState(newProfile, emptyList(), null, null, emptyList(), emptyList(), emptyList())
                    val s2 = kvdbPut("user_${newUid}_state", myStateStr)
                    s1 && s2
                }
                
                if (!uploadSuccess) {
                    return Result.failure(Exception("Failed to register cloud fallback account."))
                }
                
                // Set locally
                demoUsers[newUid] = newProfile
                _currentUser.value = newProfile
                
                // Save session to SharedPreferences
                prefs.edit()
                    .putString("demo_uid", newUid)
                    .putString("demo_email", email)
                    .apply()
                
                // Start background sync
                startCloudSync()
                Result.success(newProfile)
            } else {
                val uid = createDemoUser(name, nickname, dob, gender, isPartner = false)
                Result.success(demoUsers[uid]!!)
            }
        } else {
            val authClient = auth ?: return Result.failure(Exception("Firebase Auth not available"))
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))

            try {
                val result = withContext(Dispatchers.IO) {
                    Tasks.await(authClient.createUserWithEmailAndPassword(email, password), 15, TimeUnit.SECONDS)
                }
                val firebaseUser = result.user ?: return Result.failure(Exception("User creation failed"))
                val newProfile = UserProfile(
                    uid = firebaseUser.uid,
                    name = name,
                    nickname = nickname,
                    dob = dob,
                    gender = gender
                )
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("users").document(firebaseUser.uid).set(newProfile), 15, TimeUnit.SECONDS)
                }
                setupRealFirebaseListeners(firebaseUser.uid)
                Result.success(newProfile)
            } catch (e: Exception) {
                val enhancedMessage = when {
                    e is com.google.firebase.auth.FirebaseAuthException -> {
                        when (e.errorCode) {
                            "ERROR_OPERATION_NOT_ALLOWED", "auth/operation-not-allowed" -> {
                                "Email/Password sign-in provider is disabled in Firebase Console! Go to Firebase Console -> Authentication -> Sign-in Method, enable 'Email/Password', and click Save."
                            }
                            "ERROR_EMAIL_ALREADY_IN_USE" -> {
                                "This email is already registered. If you forgot your password, please use a different email or sign in."
                            }
                            "ERROR_WEAK_PASSWORD" -> {
                                "The password is too weak. Please use at least 6 characters."
                            }
                            "ERROR_INVALID_EMAIL" -> {
                                "The email address is invalid. Please check your spelling."
                            }
                            else -> {
                                "Firebase Auth Exception (${e.errorCode}): ${e.localizedMessage}. Ensure 'Email/Password' provider is Enabled in your Firebase Authentication settings in the Firebase Console."
                            }
                        }
                    }
                    e.message?.contains("API_KEY_SERVICE_SPEC_AND_PROVIDER_DO_NOT_MATCH") == true || e.message?.contains("operation-not-allowed") == true -> {
                        "Email/Password provider is disabled! Enable 'Email/Password' in Firebase Console -> Authentication -> Sign-in Method."
                    }
                    e.message?.contains("sign-in provider") == true || e.message?.contains("disabled") == true -> {
                        "Please enable 'Email/Password' provider in Firebase Console -> Authentication -> Sign-in Method."
                    }
                    else -> {
                        e.localizedMessage ?: "Unknown authentication error"
                    }
                }
                Result.failure(Exception(enhancedMessage, e))
            }
        }
    }

    // Login Flow
    suspend fun loginWithEmail(email: String, password: String): Result<UserProfile> {
        return if (_isDemoMode.value) {
            if (_isCloudSyncEnabled.value) {
                val sanitized = sanitizeEmail(email)
                val uid = withContext(Dispatchers.IO) { kvdbGet("email_$sanitized") }
                if (uid == null || uid.isEmpty()) {
                    return Result.failure(Exception("No account found with this email. Please Sign Up!"))
                }
                
                val stateStr = withContext(Dispatchers.IO) { kvdbGet("user_${uid}_state") }
                if (stateStr == null || stateStr.isEmpty()) {
                    return Result.failure(Exception("Failed to load user profile from cloud."))
                }
                
                // Parse and load
                val syncState = parseUserSyncState(stateStr)
                val profile = syncState.profile ?: return Result.failure(Exception("Invalid cloud profile"))
                
                // Load into local memory
                demoUsers[uid] = profile
                _currentUser.value = profile
                
                // Update local lists
                demoCycleLogs.clear()
                demoCycleLogs.addAll(syncState.cycleLogs)
                _cycleLogsSelf.value = demoCycleLogs.filter { it.userId == uid }
                
                syncState.cycleConfig?.let {
                    demoCycleConfigs[uid] = it
                    _cycleConfigSelf.value = it
                }
                
                syncState.waterGoal?.let {
                    demoWaterGoals[uid] = it
                    _waterGoalSelf.value = it
                }
                
                demoWaterLogs.clear()
                demoWaterLogs.addAll(syncState.waterLogs)
                _waterLogsSelf.value = demoWaterLogs.filter { it.userId == uid }
                val todayStr = LocalDate.now().toString()
                _waterIntakeSelf.value = demoWaterLogs.filter { it.userId == uid && it.date == todayStr }.sumOf { it.amountMl }
                
                demoMoodEntries.clear()
                demoMoodEntries.addAll(syncState.moodEntries)
                _moodEntriesSelf.value = demoMoodEntries.filter { it.userId == uid }.sortedByDescending { it.timestamp }
                
                demoPromptResponses.clear()
                demoPromptResponses.addAll(syncState.promptResponses)
                _promptResponsesSelf.value = demoPromptResponses.filter { it.userId == uid }
                
                // Save session to SharedPreferences
                prefs.edit()
                    .putString("demo_uid", uid)
                    .putString("demo_email", email)
                    .apply()
                
                // Fetch couple state if any
                val savedCoupleId = prefs.getString("demo_couple_id", null)
                if (savedCoupleId != null) {
                    loadCloudCoupleState(savedCoupleId, uid)
                }
                
                // Start background sync
                startCloudSync()
                Result.success(profile)
            } else {
                val firstUser = demoUsers.values.firstOrNull()
                if (firstUser != null) {
                    switchDemoUser(firstUser.uid)
                    Result.success(firstUser)
                } else {
                    Result.failure(Exception("No demo users available. Add one first."))
                }
            }
        } else {
            val authClient = auth ?: return Result.failure(Exception("Firebase Auth not available"))
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))

            try {
                val result = withContext(Dispatchers.IO) {
                    Tasks.await(authClient.signInWithEmailAndPassword(email, password), 15, TimeUnit.SECONDS)
                }
                val firebaseUser = result.user ?: return Result.failure(Exception("Login failed"))
                val snapshot = withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("users").document(firebaseUser.uid).get(), 15, TimeUnit.SECONDS)
                }
                val profile = snapshot.toObject(UserProfile::class.java) ?: UserProfile(uid = firebaseUser.uid, name = firebaseUser.email ?: "User")
                setupRealFirebaseListeners(firebaseUser.uid)
                Result.success(profile)
            } catch (e: Exception) {
                val enhancedMessage = when {
                    e is com.google.firebase.auth.FirebaseAuthException -> {
                        when (e.errorCode) {
                            "ERROR_USER_NOT_FOUND", "auth/user-not-found" -> {
                                "No account found with this email. Please check your spelling or click 'Sign Up' to create one."
                            }
                            "ERROR_WRONG_PASSWORD", "auth/wrong-password" -> {
                                "Incorrect password. Please try again."
                            }
                            "ERROR_INVALID_EMAIL" -> {
                                "The email address is invalid. Please check your spelling."
                            }
                            "ERROR_OPERATION_NOT_ALLOWED" -> {
                                "Email/Password sign-in is disabled in Firebase! Please enable Email/Password provider under Authentication -> Sign-in Method in your Firebase Console."
                            }
                            else -> {
                                "Firebase Auth Exception (${e.errorCode}): ${e.localizedMessage}. Ensure Email/Password sign-in is Enabled in your Firebase Console."
                            }
                        }
                    }
                    e.message?.contains("API_KEY_SERVICE_SPEC_AND_PROVIDER_DO_NOT_MATCH") == true -> {
                        "Email/Password provider is disabled! Enable 'Email/Password' under Authentication -> Sign-in Method in the Firebase Console."
                    }
                    else -> {
                        e.localizedMessage ?: "Unknown login error"
                    }
                }
                Result.failure(Exception(enhancedMessage, e))
            }
        }
    }

    fun signOut() {
        if (_isDemoMode.value) {
            cloudSyncJob?.cancel()
            pairingWatcherJob?.cancel()
            prefs.edit().clear().apply()
            
            _currentUser.value = null
            _partnerUser.value = null
            _couple.value = null
            _events.value = emptyList()
            _todos.value = emptyList()
            demoEvents.clear()
            demoTodos.clear()
            demoTodoComments.clear()
            demoWaterComments.clear()
            demoCycleLogs.clear()
            demoWaterLogs.clear()
            demoMoodEntries.clear()
            demoPromptResponses.clear()
        } else {
            auth?.signOut()
            cleanupRealFirebaseListeners()
            _currentUser.value = null
            _partnerUser.value = null
            _couple.value = null
            _events.value = emptyList()
            _todos.value = emptyList()
        }
    }

    // Pairing Flow
    suspend fun generatePairCode(): Result<String> {
        val code = (100000..999999).random().toString()
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))

        return if (_isDemoMode.value) {
            if (_isCloudSyncEnabled.value) {
                // Online sync generation
                val pendingCouple = Couple(
                    coupleId = "couple_${currentUid}_pending",
                    user1Uid = currentUid,
                    pairCode = code,
                    createdAt = System.currentTimeMillis()
                )
                _couple.value = pendingCouple
                
                // Create the pending invite payload
                val payload = JSONObject()
                payload.put("user1Uid", currentUid)
                payload.put("coupleId", pendingCouple.coupleId)
                payload.put("createdAt", pendingCouple.createdAt)
                _currentUser.value?.let { payload.put("profile", userProfileToJson(it)) }
                
                val success = withContext(Dispatchers.IO) {
                    kvdbPut("paircode_$code", payload.toString())
                }
                
                if (!success) {
                    return Result.failure(Exception("Failed to register pairing code with cloud server."))
                }
                
                // Start background loop to watch for link response
                startPairingCodeResponseWatcher(code)
                Result.success(code)
            } else {
                val existingCouple = demoCouples.values.find { it.user1Uid == currentUid || it.user2Uid == currentUid }
                if (existingCouple != null) {
                    return Result.failure(Exception("You are already paired in a relationship!"))
                }
                val pendingCouple = Couple(
                    coupleId = "temp_${currentUid}",
                    user1Uid = currentUid,
                    pairCode = code,
                    createdAt = System.currentTimeMillis()
                )
                demoCouples[pendingCouple.coupleId] = pendingCouple
                _couple.value = pendingCouple
                Result.success(code)
            }
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                // Check if user is already paired
                val snap1 = withContext(Dispatchers.IO) { Tasks.await(db.collection("couples").whereEqualTo("user1Uid", currentUid).get(), 15, TimeUnit.SECONDS) }
                val snap2 = withContext(Dispatchers.IO) { Tasks.await(db.collection("couples").whereEqualTo("user2Uid", currentUid).get(), 15, TimeUnit.SECONDS) }
                
                if (!snap2.isEmpty) {
                    return Result.failure(Exception("You are already paired in a relationship!"))
                }

                var existingPendingDocId: String? = null
                for (doc in snap1.documents) {
                    val couple = doc.toObject(Couple::class.java)
                    if (couple != null) {
                        if (couple.user2Uid.isNotEmpty()) {
                            return Result.failure(Exception("You are already paired in a relationship!"))
                        } else {
                            existingPendingDocId = couple.coupleId
                        }
                    }
                }

                val coupleId = existingPendingDocId ?: "couple_${UUID.randomUUID().toString().take(8)}"
                val newCouple = Couple(
                    coupleId = coupleId,
                    user1Uid = currentUid,
                    pairCode = code,
                    createdAt = System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("couples").document(coupleId).set(newCouple), 15, TimeUnit.SECONDS)
                }
                Result.success(code)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun pairWithCode(code: String): Result<Couple> {
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))

        if (code.length != 6 || code.toIntOrNull() == null) {
            return Result.failure(Exception("Pairing code must be a 6-digit number"))
        }

        return if (_isDemoMode.value) {
            if (_isCloudSyncEnabled.value) {
                val payloadStr = withContext(Dispatchers.IO) { kvdbGet("paircode_$code") }
                if (payloadStr == null || payloadStr.isEmpty()) {
                    return Result.failure(Exception("Invalid, expired, or non-existent pairing code"))
                }
                
                val payload = JSONObject(payloadStr)
                val user1Uid = payload.getString("user1Uid")
                val user1ProfileJson = payload.getJSONObject("profile")
                val user1Profile = jsonToUserProfile(user1ProfileJson)
                val createdAt = payload.getLong("createdAt")
                
                // Verify 30-day expiration
                if (System.currentTimeMillis() - createdAt > 30L * 24L * 60L * 60L * 1000L) {
                    withContext(Dispatchers.IO) { kvdbPut("paircode_$code", "") } // clear
                    return Result.failure(Exception("This pairing code has expired"))
                }
                
                if (user1Uid == currentUid) {
                    return Result.failure(Exception("You cannot pair with your own code!"))
                }
                
                // Complete pairing link
                val coupleId = "couple_${user1Uid}_${currentUid}"
                val completedCouple = Couple(
                    coupleId = coupleId,
                    user1Uid = user1Uid,
                    user2Uid = currentUid,
                    pairCode = "",
                    relationshipStartDate = LocalDate.now().toString(),
                    createdAt = System.currentTimeMillis()
                )
                
                val uploadSuccess = withContext(Dispatchers.IO) {
                    // Save couple state
                    val stateStr = serializeCoupleSyncState(completedCouple, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
                    val s1 = kvdbPut("couple_${coupleId}_state", stateStr)
                    // Write link notification back for User A
                    val s2 = kvdbPut("paircode_${code}_linked", coupleToJson(completedCouple).toString())
                    // Clear pending code
                    val s3 = kvdbPut("paircode_$code", "")
                    s1 && s2
                }
                
                if (!uploadSuccess) {
                    return Result.failure(Exception("Failed to upload couple link payload."))
                }
                
                // Sync states locally
                _couple.value = completedCouple
                _partnerUser.value = user1Profile
                demoUsers[user1Uid] = user1Profile
                
                prefs.edit()
                    .putString("demo_couple_id", coupleId)
                    .apply()
                
                // Start background sync
                startCloudSync()
                Result.success(completedCouple)
            } else {
                // Find pending couple with code
                val pending = demoCouples.values.find { it.pairCode == code && it.user2Uid.isEmpty() }
                if (pending == null) {
                    return Result.failure(Exception("Invalid or expired pairing code"))
                }

                // Expiry Check (Generous 30 days to avoid clock mismatch issues)
                val isExpired = pending.createdAt > 0L && (System.currentTimeMillis() - pending.createdAt > 30L * 24L * 60L * 60L * 1000L)
                if (isExpired) {
                    demoCouples.remove(pending.coupleId)
                    return Result.failure(Exception("This pairing code has expired (30 days exceeded)"))
                }

                if (pending.user1Uid == currentUid) {
                    return Result.failure(Exception("You cannot pair with your own code!"))
                }

                // Perform pairing
                val completedCouple = pending.copy(
                    user2Uid = currentUid,
                    pairCode = ""
                )
                demoCouples.remove(pending.coupleId)
                demoCouples[completedCouple.coupleId] = completedCouple

                // Sync active demo states
                _couple.value = completedCouple
                _partnerUser.value = demoUsers[completedCouple.user1Uid]
                switchDemoUser(currentUid)

                Result.success(completedCouple)
            }
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                val snap1 = withContext(Dispatchers.IO) { Tasks.await(db.collection("couples").whereEqualTo("user1Uid", currentUid).get(), 15, TimeUnit.SECONDS) }
                val snap2 = withContext(Dispatchers.IO) { Tasks.await(db.collection("couples").whereEqualTo("user2Uid", currentUid).get(), 15, TimeUnit.SECONDS) }
                
                if (!snap2.isEmpty) {
                    return Result.failure(Exception("You are already paired in a relationship!"))
                }

                var pendingDocToDelete: String? = null
                for (doc in snap1.documents) {
                    val couple = doc.toObject(Couple::class.java)
                    if (couple != null) {
                        if (couple.user2Uid.isNotEmpty()) {
                            return Result.failure(Exception("You are already paired in a relationship!"))
                        } else {
                            pendingDocToDelete = couple.coupleId
                        }
                    }
                }

                val querySnapshot = withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("couples").whereEqualTo("pairCode", code).get(), 15, TimeUnit.SECONDS)
                }

                // Robust check: query the 'couples' collection for a document where 'pairCode' matches and 'user2Uid' is null, empty, or blank
                val doc = querySnapshot.documents.firstOrNull { d ->
                    val u2 = d.getString("user2Uid")
                    u2 == null || u2.isEmpty() || u2.isBlank()
                } ?: return Result.failure(Exception("Invalid, expired, or already-paired code. Please verify the code on your partner's device."))

                // Resilient deserialization that safely handles missing/null values without throwing Exceptions
                val pending = try {
                    doc.toObject(Couple::class.java)
                } catch (de: Exception) {
                    null
                } ?: Couple(
                    coupleId = doc.id,
                    user1Uid = doc.getString("user1Uid") ?: "",
                    user2Uid = doc.getString("user2Uid") ?: "",
                    pairCode = doc.getString("pairCode") ?: "",
                    relationshipStartDate = doc.getString("relationshipStartDate"),
                    createdAt = doc.getLong("createdAt") ?: 0L
                )

                // Verify expiration (Generous 30 days to avoid clock mismatch issues)
                val isExpired = pending.createdAt > 0L && (System.currentTimeMillis() - pending.createdAt > 30L * 24L * 60L * 60L * 1000L)
                if (isExpired) {
                    withContext(Dispatchers.IO) {
                        Tasks.await(db.collection("couples").document(pending.coupleId).delete(), 15, TimeUnit.SECONDS)
                    }
                    return Result.failure(Exception("This pairing code has expired"))
                }

                if (pending.user1Uid == currentUid) {
                    return Result.failure(Exception("You cannot pair with your own code!"))
                }

                val completedCouple = pending.copy(
                    user2Uid = currentUid,
                    pairCode = ""
                )

                withContext(Dispatchers.IO) {
                    // Overwrite/update partner's pending document to complete the link
                    Tasks.await(db.collection("couples").document(completedCouple.coupleId).set(completedCouple), 15, TimeUnit.SECONDS)
                    // If we had our own pending document, delete it since we've now paired with our partner
                    if (pendingDocToDelete != null) {
                        try {
                            Tasks.await(db.collection("couples").document(pendingDocToDelete).delete(), 15, TimeUnit.SECONDS)
                        } catch (de: Exception) {
                            Log.w("DuetRepository", "Failed to clean up redundant pending couple document: ${de.message}")
                        }
                    }
                }

                setupRealFirebaseListeners(currentUid)
                Result.success(completedCouple)
            } catch (e: Exception) {
                val enhancedMessage = when {
                    e.message?.contains("PERMISSION_DENIED") == true || e.message?.contains("permission-denied") == true || e.message?.contains("insufficient permissions") == true -> {
                        "Firestore permission denied! Please check your Firestore security rules in the Firebase Console. Make sure you allow authenticated users to read and write to the 'couples' collection. Example:\n\nmatch /couples/{coupleId} { allow read, write: if request.auth != null; }"
                    }
                    else -> {
                        e.localizedMessage ?: "Unknown pairing error"
                    }
                }
                Result.failure(Exception(enhancedMessage, e))
            }
        }
    }

    suspend fun simulatePartnerPairing(code: String): Result<Couple> {
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        if (!_isDemoMode.value) {
            return Result.failure(Exception("Simulation only supported in Demo Mode"))
        }

        val pending = demoCouples.values.find { it.pairCode == code && it.user2Uid.isEmpty() }
            ?: Couple(
                coupleId = "couple_${UUID.randomUUID().toString().take(8)}",
                user1Uid = currentUid,
                pairCode = code,
                createdAt = System.currentTimeMillis()
            )

        val currentGender = _currentUser.value?.gender?.lowercase() ?: "female"
        val partnerGender = if (currentGender == "female") "Male" else "Female"
        val partnerName = if (partnerGender == "Male") "Alex Mercer" else "Sarah Jenkins"
        val partnerNickname = if (partnerGender == "Male") "Alex 🚀" else "Sarah 🌸"
        val partnerDob = if (partnerGender == "Male") "1996-09-24" else "1998-04-12"
        val partnerUid = "demo_partner_${UUID.randomUUID().toString().take(6)}"

        val mockPartner = UserProfile(
            uid = partnerUid,
            name = partnerName,
            nickname = partnerNickname,
            photoUrl = if (partnerGender == "Female") "avatar_f_1" else "avatar_m_1",
            dob = partnerDob,
            gender = partnerGender
        )

        demoUsers[partnerUid] = mockPartner

        val completedCouple = pending.copy(
            user2Uid = partnerUid,
            pairCode = "",
            relationshipStartDate = LocalDate.now().minusDays(120).toString()
        )

        demoCouples.remove(pending.coupleId)
        demoCouples[completedCouple.coupleId] = completedCouple

        demoEvents.removeAll { it.coupleId == completedCouple.coupleId }
        val today = LocalDate.now().toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val nextWeek = LocalDate.now().plusDays(7).toString()

        demoEvents.add(CalendarEvent(
            eventId = "event_sim_1",
            coupleId = completedCouple.coupleId,
            title = "Weekend Picnic 🧺",
            date = tomorrow,
            type = "moment",
            note = "Pack cheese, strawberries, and that sparkling cider.",
            createdBy = currentUid
        ))

        demoEvents.add(CalendarEvent(
            eventId = "event_sim_2",
            coupleId = completedCouple.coupleId,
            title = "Movie Night 🍿",
            date = today,
            type = "reminder",
            note = "Watch that new sci-fi film we've been talking about.",
            createdBy = partnerUid
        ))

        demoEvents.add(CalendarEvent(
            eventId = "event_sim_3",
            coupleId = completedCouple.coupleId,
            title = "Dinner Anniversary! ❤️",
            date = nextWeek,
            type = "moment",
            note = "Table booked at the candlelit bistro.",
            createdBy = partnerUid
        ))

        demoTodos.removeAll { it.createdBy == currentUid || it.createdBy == partnerUid }
        demoTodos.add(TodoItem(todoId = "todo_sim_1", title = "Choose restaurant for anniversary", isCompleted = false, createdBy = currentUid))
        demoTodos.add(TodoItem(todoId = "todo_sim_2", title = "Clean the living room 🧹", isCompleted = true, createdBy = partnerUid))
        demoTodos.add(TodoItem(todoId = "todo_sim_3", title = "Buy grocery list for picnic", isCompleted = false, createdBy = partnerUid))

        demoCycleConfigs[currentUid] = CycleConfig(userId = currentUid, avgCycleLength = 29, avgPeriodLength = 5, isPrivacyEnabled = false)
        demoCycleConfigs[partnerUid] = CycleConfig(userId = partnerUid, avgCycleLength = 28, avgPeriodLength = 5, isPrivacyEnabled = false)

        val femaleUid = if (currentGender == "female") currentUid else partnerUid
        if (currentGender == "female" || partnerGender == "Female") {
            val date1 = LocalDate.now().minusDays(32).toString()
            val date2 = LocalDate.now().minusDays(28).toString()
            demoCycleLogs.add(CycleLog(
                cycleId = "cycle_sim_1",
                coupleId = completedCouple.coupleId,
                userId = femaleUid,
                startDate = date1,
                endDate = date2,
                periodLength = 5,
                symptoms = listOf("Cramps", "Fatigue"),
                flowIntensity = "Medium",
                notes = "Preloaded mock cycle log for simulation."
            ))
        }

        demoWaterGoals[currentUid] = WaterGoal(userId = currentUid, dailyGoalMl = 2000, isManual = false, age = 25, weightKg = 70)
        demoWaterGoals[partnerUid] = WaterGoal(userId = partnerUid, dailyGoalMl = 2000, isManual = false, age = 26, weightKg = 65)

        _couple.value = completedCouple
        _partnerUser.value = mockPartner
        switchDemoUser(currentUid)

        return Result.success(completedCouple)
    }

    suspend fun updateRelationshipStartDate(date: String): Result<Boolean> {
        val coupleObj = _couple.value ?: return Result.failure(Exception("You are not currently paired"))

        return if (_isDemoMode.value) {
            val updated = coupleObj.copy(relationshipStartDate = date)
            demoCouples[coupleObj.coupleId] = updated
            _couple.value = updated
            Result.success(true)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("couples").document(coupleObj.coupleId).update("relationshipStartDate", date))
                }
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun unpairCouple(): Result<Boolean> {
        val coupleObj = _couple.value ?: return Result.failure(Exception("You are not currently paired"))

        return if (_isDemoMode.value) {
            demoCouples.remove(coupleObj.coupleId)
            demoEvents.removeAll { it.coupleId == coupleObj.coupleId }
            _couple.value = null
            _partnerUser.value = null
            _events.value = emptyList()
            _todos.value = emptyList()
            Result.success(true)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("couples").document(coupleObj.coupleId).delete())
                    val eventsSnapshot = Tasks.await(db.collection("events").whereEqualTo("coupleId", coupleObj.coupleId).get())
                    for (doc in eventsSnapshot.documents) {
                        Tasks.await(db.collection("events").document(doc.id).delete())
                    }
                }
                _couple.value = null
                _partnerUser.value = null
                _events.value = emptyList()
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- CALENDAR EVENTS ---
    suspend fun addCalendarEvent(title: String, date: String, type: String, note: String, photoUrl: String? = null): Result<CalendarEvent> {
        val coupleObj = _couple.value ?: return Result.failure(Exception("You must be paired to share calendar events"))
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))

        val newEvent = CalendarEvent(
            eventId = UUID.randomUUID().toString(),
            coupleId = coupleObj.coupleId,
            title = title,
            date = date,
            type = type,
            note = note,
            photoUrl = photoUrl,
            createdBy = currentUid,
            createdAt = System.currentTimeMillis()
        )

        return if (_isDemoMode.value) {
            demoEvents.add(newEvent)
            _events.value = demoEvents.filter { it.coupleId == coupleObj.coupleId }.sortedBy { it.date }
            Result.success(newEvent)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("events").document(newEvent.eventId).set(newEvent))
                }
                Result.success(newEvent)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteCalendarEvent(eventId: String): Result<Boolean> {
        return if (_isDemoMode.value) {
            val coupleObj = _couple.value ?: return Result.failure(Exception("You must be paired"))
            demoEvents.removeAll { it.eventId == eventId }
            _events.value = demoEvents.filter { it.coupleId == coupleObj.coupleId }.sortedBy { it.date }
            Result.success(true)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("events").document(eventId).delete())
                }
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- WATER INTENSIVE ACTIONS ---
    suspend fun updateWaterIntake(amountMl: Int): Result<Int> {
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val today = LocalDate.now().toString()

        return if (_isDemoMode.value) {
            val logId = UUID.randomUUID().toString()
            val newLog = WaterLog(logId, currentUid, today, amountMl, System.currentTimeMillis())
            demoWaterLogs.add(newLog)
            _waterLogsSelf.value = demoWaterLogs.filter { it.userId == currentUid }.sortedBy { it.timestamp }
            
            val totalToday = _waterLogsSelf.value.filter { it.date == today }.sumOf { it.amountMl }
            _waterIntakeSelf.value = totalToday
            saveWaterStateToPrefs()
            Result.success(totalToday)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                val logId = UUID.randomUUID().toString()
                val newLog = WaterLog(logId, currentUid, today, amountMl, System.currentTimeMillis())
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("users").document(currentUid).collection("water_logs").document(logId).set(newLog))
                    // Also update legacy leaf document for back-compat
                    val newVal = _waterIntakeSelf.value + amountMl
                    Tasks.await(db.collection("users").document(currentUid).collection("water").document(today).set(mapOf("amountMl" to newVal)))
                }
                _waterIntakeSelf.value = _waterIntakeSelf.value + amountMl
                saveWaterStateToPrefs()
                Result.success(_waterIntakeSelf.value)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun resetWaterIntake(): Result<Int> {
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val today = LocalDate.now().toString()

        return if (_isDemoMode.value) {
            demoWaterLogs.removeAll { it.userId == currentUid && it.date == today }
            _waterLogsSelf.value = demoWaterLogs.filter { it.userId == currentUid }.sortedBy { it.timestamp }
            _waterIntakeSelf.value = 0
            saveWaterStateToPrefs()
            Result.success(0)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    val todayLogs = db.collection("users").document(currentUid).collection("water_logs").whereEqualTo("date", today).get()
                    val snapshot = Tasks.await(todayLogs)
                    for (doc in snapshot.documents) {
                        Tasks.await(db.collection("users").document(currentUid).collection("water_logs").document(doc.id).delete())
                    }
                    Tasks.await(db.collection("users").document(currentUid).collection("water").document(today).set(mapOf("amountMl" to 0)))
                }
                _waterIntakeSelf.value = 0
                saveWaterStateToPrefs()
                Result.success(0)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Save customized water goal
    suspend fun saveWaterGoal(goal: WaterGoal): Result<Boolean> {
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val updated = goal.copy(userId = currentUid)

        return if (_isDemoMode.value) {
            demoWaterGoals[currentUid] = updated
            _waterGoalSelf.value = updated
            saveWaterStateToPrefs()
            Result.success(true)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("users").document(currentUid).collection("water_goals").document("goal").set(updated))
                }
                _waterGoalSelf.value = updated
                saveWaterStateToPrefs()
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Add a real-time cheering comments for water logs
    suspend fun addWaterComment(text: String, emoji: String): Result<WaterComment> {
        val coupleObj = _couple.value ?: return Result.failure(Exception("You must be paired to leave comments"))
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val today = LocalDate.now().toString()

        val comment = WaterComment(
            commentId = UUID.randomUUID().toString(),
            userId = currentUid,
            date = today,
            text = text,
            emojiReaction = emoji,
            timestamp = System.currentTimeMillis()
        )

        return if (_isDemoMode.value) {
            demoWaterComments.add(comment)
            _waterComments.value = demoWaterComments.filter { it.date == today }.sortedByDescending { it.timestamp }
            Result.success(comment)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("couples").document(coupleObj.coupleId).collection("water_comments").document(comment.commentId).set(comment))
                }
                Result.success(comment)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- PART A - CYCLE ACTIONS ---
    suspend fun saveCycleConfig(config: CycleConfig): Result<Boolean> {
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val updated = config.copy(userId = currentUid, lastUpdated = System.currentTimeMillis())

        return if (_isDemoMode.value) {
            demoCycleConfigs[currentUid] = updated
            _cycleConfigSelf.value = updated
            Result.success(true)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("users").document(currentUid).collection("cycle_configs").document("config").set(updated))
                }
                _cycleConfigSelf.value = updated
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun logCyclePeriod(startDate: String, endDate: String?, symptoms: List<String>, flow: String, notes: String): Result<CycleLog> {
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val coupleObj = _couple.value ?: return Result.failure(Exception("Must be paired"))

        // Standard validation: endDate cannot be before startDate
        if (endDate != null) {
            try {
                val start = LocalDate.parse(startDate)
                val end = LocalDate.parse(endDate)
                if (end.isBefore(start)) {
                    return Result.failure(Exception("End date cannot be before start date"))
                }
            } catch (e: Exception) {
                return Result.failure(Exception("Invalid date format parsed"))
            }
        }

        // Calculate period length if endDate is provided
        val calculatedLength = if (endDate != null) {
            ChronoUnit.DAYS.between(LocalDate.parse(startDate), LocalDate.parse(endDate)).toInt() + 1
        } else {
            _cycleConfigSelf.value?.avgPeriodLength ?: 5
        }

        val log = CycleLog(
            cycleId = UUID.randomUUID().toString(),
            coupleId = coupleObj.coupleId,
            userId = currentUid,
            startDate = startDate,
            endDate = endDate,
            periodLength = calculatedLength,
            symptoms = symptoms,
            flowIntensity = flow,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )

        return if (_isDemoMode.value) {
            // Remove previous logs on the exact same start date to avoid duplication
            demoCycleLogs.removeAll { it.userId == currentUid && it.startDate == startDate }
            demoCycleLogs.add(log)
            _cycleLogsSelf.value = demoCycleLogs.filter { it.userId == currentUid }.sortedBy { it.startDate }
            Result.success(log)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("users").document(currentUid).collection("cycle_logs").document(log.cycleId).set(log))
                }
                Result.success(log)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteCycleLog(cycleId: String): Result<Boolean> {
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))

        return if (_isDemoMode.value) {
            demoCycleLogs.removeAll { it.cycleId == cycleId }
            _cycleLogsSelf.value = demoCycleLogs.filter { it.userId == currentUid }.sortedBy { it.startDate }
            Result.success(true)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("users").document(currentUid).collection("cycle_logs").document(cycleId).delete())
                }
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Predictions algorithms
    fun calculatePredictions(logs: List<CycleLog>, config: CycleConfig): CyclePrediction? {
        if (logs.isEmpty()) return null
        val sortedLogs = logs.sortedBy { it.startDate }
        val latestLog = sortedLogs.last()

        // 1. Calculate average cycle length using standard calendar history difference once enough history exists
        var cycleLength = config.avgCycleLength
        if (sortedLogs.size >= 2) {
            val lengths = mutableListOf<Long>()
            for (i in 0 until sortedLogs.size - 1) {
                try {
                    val start1 = LocalDate.parse(sortedLogs[i].startDate)
                    val start2 = LocalDate.parse(sortedLogs[i + 1].startDate)
                    val days = ChronoUnit.DAYS.between(start1, start2)
                    if (days in 15..45) { // reasonable sanity constraints
                        lengths.add(days)
                    }
                } catch (e: Exception) {
                    // ignore parse failures
                }
            }
            if (lengths.isNotEmpty()) {
                // Adaptive recalculation using average of up to the last 5 logs (last 3-6 cycles)
                val toTake = lengths.takeLast(5)
                cycleLength = toTake.average().toInt()
            }
        }

        // 2. Calculate average period length
        var periodLength = config.avgPeriodLength
        val completedLogs = sortedLogs.filter { it.endDate != null }
        if (completedLogs.isNotEmpty()) {
            val lengths = mutableListOf<Long>()
            for (log in completedLogs) {
                try {
                    val start = LocalDate.parse(log.startDate)
                    val end = LocalDate.parse(log.endDate!!)
                    val days = ChronoUnit.DAYS.between(start, end) + 1
                    if (days in 1..15) {
                        lengths.add(days)
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
            if (lengths.isNotEmpty()) {
                periodLength = lengths.average().toInt()
            }
        }

        // 3. Phase 7 will replace this with a weighted/adaptive model once 3+ real cycles are logged — do not build ML now.
        try {
            val latestStart = LocalDate.parse(latestLog.startDate)
            val predStart = latestStart.plusDays(cycleLength.toLong())
            val predEnd = predStart.plusDays((periodLength - 1).toLong())

            val ovulation = predStart.minusDays(14)
            val fertileStart = ovulation.minusDays(5)
            val fertileEnd = ovulation

            return CyclePrediction(
                predictedStartDate = predStart.toString(),
                predictedEndDate = predEnd.toString(),
                ovulationDate = ovulation.toString(),
                fertileWindowStart = fertileStart.toString(),
                fertileWindowEnd = fertileEnd.toString()
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun getPredictionsSelf(): CyclePrediction? {
        val logs = _cycleLogsSelf.value
        val config = _cycleConfigSelf.value ?: CycleConfig(userId = _currentUser.value?.uid ?: "")
        return calculatePredictions(logs, config)
    }

    fun getPredictionsPartner(): CyclePrediction? {
        // Enforce privacy settings
        val configPartner = _cycleConfigPartner.value ?: return null
        if (configPartner.isPrivacyEnabled) {
            return null // Partner has hidden their cycle details
        }
        val logs = _cycleLogsPartner.value
        return calculatePredictions(logs, configPartner)
    }

    // --- ALARM REMINDER SYSTEM UTILS ---
    fun scheduleWaterReminder(startHour: Int = 8, endHour: Int = 22, intervalHours: Int = 2, force: Boolean = false) {
        try {
            // Save settings to SharedPreferences
            prefs.edit().apply {
                putInt("water_start_hour", startHour)
                putInt("water_end_hour", endHour)
                apply()
            }
            saveWaterStateToPrefs()

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WaterReminderReceiver::class.java)
            
            // Check if alarm already exists
            val existingIntent = PendingIntent.getBroadcast(
                context,
                2002,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            
            if (!force && existingIntent != null) {
                Log.d("WaterReminder", "Water reminder alarm already scheduled, skipping reschedule.")
                return
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                2002,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Compute actual dynamic trigger time instead of a 5-second test alarm!
            val calendar = java.util.Calendar.getInstance()
            val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(java.util.Calendar.MINUTE)

            val targetGoal = prefs.getInt("water_target_ml", 2000)
            val stateDate = prefs.getString("water_state_date", "")
            val todayStr = java.time.LocalDate.now().toString()
            val currentIntake = if (stateDate == todayStr) prefs.getInt("water_intake_ml", 0) else 0
            val remainingMl = (targetGoal - currentIntake).coerceAtLeast(0)

            val isSleepTime = currentHour >= endHour || currentHour < startHour

            val triggerAtMillis: Long
            val debugMsg: String

            if (isSleepTime) {
                // Sleep window: Schedule for the startHour of awake tomorrow (or today if early morning)
                val nextAlarmCalendar = java.util.Calendar.getInstance().apply {
                    if (get(java.util.Calendar.HOUR_OF_DAY) >= endHour) {
                        add(java.util.Calendar.DAY_OF_YEAR, 1)
                    }
                    set(java.util.Calendar.HOUR_OF_DAY, startHour)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                triggerAtMillis = nextAlarmCalendar.timeInMillis
                debugMsg = "Sleep time detected. Scheduled next reminder for wake time: ${nextAlarmCalendar.time}"
            } else {
                if (remainingMl <= 0) {
                    // Target completed! Schedule for 4 hours from now
                    val intervalMs = 4 * 60 * 60 * 1000L
                    triggerAtMillis = System.currentTimeMillis() + intervalMs
                    debugMsg = "Goal already met today! Next reminder scheduled in 4 hours."
                } else {
                    // Compute smart interval based on target progress and hours left until sleep
                    val portionsNeeded = remainingMl / 250.0 // assuming a 250ml cup size
                    val hoursRemaining = (endHour - (currentHour + currentMinute / 60.0)).coerceAtLeast(1.0)
                    
                    val calculatedIntervalHours = if (portionsNeeded > 0) {
                        hoursRemaining / portionsNeeded
                    } else {
                        2.0
                    }
                    
                    // Limit reminder interval between 30 minutes and 4 hours to avoid spamming
                    val finalIntervalHours = calculatedIntervalHours.coerceIn(0.5, 4.0)
                    val intervalMs = (finalIntervalHours * 60 * 60 * 1000L).toLong()
                    triggerAtMillis = System.currentTimeMillis() + intervalMs
                    debugMsg = "Smart interval calculated: $finalIntervalHours hours ($remainingMl ml remaining in $hoursRemaining awake hours)."
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    Log.d("WaterReminder", "Exact alarm scheduled with allow-while-idle: $debugMsg")
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    Log.d("WaterReminder", "Inexact alarm scheduled with allow-while-idle: $debugMsg")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                Log.d("WaterReminder", "Exact alarm scheduled with allow-while-idle (older API): $debugMsg")
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                Log.d("WaterReminder", "Exact alarm scheduled: $debugMsg")
            }
        } catch (e: Exception) {
            Log.e("WaterReminder", "Error scheduling alarm: ${e.message}")
        }
    }

    // --- SHARED TODO LIST TRACKER ---
    suspend fun addTodoItem(title: String): Result<TodoItem> {
        val coupleObj = _couple.value ?: return Result.failure(Exception("You must be paired to share todo items"))
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))

        val newTodo = TodoItem(
            todoId = UUID.randomUUID().toString(),
            title = title,
            isCompleted = false,
            createdBy = currentUid,
            createdAt = System.currentTimeMillis()
        )

        return if (_isDemoMode.value) {
            demoTodos.add(newTodo)
            val partnerId = if (coupleObj.user1Uid == currentUid) coupleObj.user2Uid else coupleObj.user1Uid
            _todos.value = demoTodos.filter { it.createdBy == currentUid || it.createdBy == partnerId }.sortedByDescending { it.createdAt }
            Result.success(newTodo)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(
                        db.collection("couples").document(coupleObj.coupleId)
                            .collection("todos").document(newTodo.todoId).set(newTodo)
                    )
                }
                Result.success(newTodo)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun toggleTodoItem(todoId: String, isCompleted: Boolean): Result<Boolean> {
        val coupleObj = _couple.value ?: return Result.failure(Exception("You must be paired"))
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))

        return if (_isDemoMode.value) {
            val index = demoTodos.indexOfFirst { it.todoId == todoId }
            if (index != -1) {
                val updated = demoTodos[index].copy(isCompleted = isCompleted)
                demoTodos[index] = updated
                val partnerId = if (coupleObj.user1Uid == currentUid) coupleObj.user2Uid else coupleObj.user1Uid
                _todos.value = demoTodos.filter { it.createdBy == currentUid || it.createdBy == partnerId }.sortedByDescending { it.createdAt }
                Result.success(true)
            } else {
                Result.failure(Exception("Todo item not found"))
            }
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(
                        db.collection("couples").document(coupleObj.coupleId)
                            .collection("todos").document(todoId).update("completed", isCompleted)
                    )
                }
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteTodoItem(todoId: String): Result<Boolean> {
        val coupleObj = _couple.value ?: return Result.failure(Exception("You must be paired"))
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))

        return if (_isDemoMode.value) {
            demoTodos.removeAll { it.todoId == todoId }
            val partnerId = if (coupleObj.user1Uid == currentUid) coupleObj.user2Uid else coupleObj.user1Uid
            _todos.value = demoTodos.filter { it.createdBy == currentUid || it.createdBy == partnerId }.sortedByDescending { it.createdAt }
            Result.success(true)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(
                        db.collection("couples").document(coupleObj.coupleId)
                            .collection("todos").document(todoId).delete()
                    )
                }
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- PART C, D, E EXTENDED FEATURE APIS ---

    val staticQuizPrompts = listOf(
        DailyPrompt("1", "What is one thing you appreciate about how we handle disagreements?", "deep"),
        DailyPrompt("2", "If we won a million dollars today, what is the first thing we should buy?", "fun"),
        DailyPrompt("3", "What is a secret dream or fantasy you haven't shared with me yet?", "intimate"),
        DailyPrompt("4", "How should we split our chores and responsibilities this weekend?", "practical"),
        DailyPrompt("5", "Which memory of our early dates always makes you smile?", "fun"),
        DailyPrompt("6", "What is one area of our relationship we can work on improving?", "deep"),
        DailyPrompt("7", "What makes you feel most loved and desired by me?", "intimate"),
        DailyPrompt("8", "Are you happy with our current monthly budget allocation?", "practical"),
        DailyPrompt("9", "If you could pick any place in the world for us to travel next, where?", "fun"),
        DailyPrompt("10", "What is a habit of mine that you find surprisingly endearing?", "fun"),
        DailyPrompt("11", "How do you feel about our current work-life balance?", "deep"),
        DailyPrompt("12", "What is a romantic gesture you would love for me to do more often?", "intimate"),
        DailyPrompt("13", "Should we start a shared savings goal for a major purchase?", "practical"),
        DailyPrompt("14", "What is your absolute favorite physical feature of mine?", "intimate"),
        DailyPrompt("15", "If you had to describe our relationship in three words, what are they?", "deep"),
        DailyPrompt("16", "What's the funniest thing that has ever happened to us together?", "fun"),
        DailyPrompt("17", "What is one household chore you wish you never had to do again?", "practical"),
        DailyPrompt("18", "Do you feel like we spend enough quality time together lately?", "deep"),
        DailyPrompt("19", "When was the last time you felt a strong spark between us?", "intimate"),
        DailyPrompt("20", "What is a meal we should cook together this week?", "practical"),
        DailyPrompt("21", "If we wrote a book about our relationship, what would the title be?", "fun"),
        DailyPrompt("22", "How do you feel our family goals are aligning right now?", "deep"),
        DailyPrompt("23", "What is something simple I can do to make you feel desired?", "intimate"),
        DailyPrompt("24", "What is the best way to handle our grocery shopping more efficiently?", "practical"),
        DailyPrompt("25", "If you could change one thing about our living room setup, what is it?", "practical"),
        DailyPrompt("26", "What's a hobby you've been wanting us to try together?", "fun"),
        DailyPrompt("27", "How has your definition of love changed since we've been together?", "deep"),
        DailyPrompt("28", "What is a sweet nickname or compliment you love hearing from me?", "intimate"),
        DailyPrompt("29", "Do you prefer planning vacations together or having me surprise you?", "fun"),
        DailyPrompt("30", "What is one thing that instantly helps you de-stress after work?", "deep"),
        DailyPrompt("31", "What is an intimate milestone we reached that you cherish?", "intimate"),
        DailyPrompt("32", "How can we better coordinate our morning and evening routines?", "practical"),
        DailyPrompt("33", "If you could swap jobs with me for a day, would you?", "fun"),
        DailyPrompt("34", "What's a piece of advice you've received that you want us to apply?", "deep"),
        DailyPrompt("35", "What is a playful or cheeky challenge you'd love to give me?", "intimate"),
        DailyPrompt("36", "Do we need to upgrade any of our major appliances soon?", "practical"),
        DailyPrompt("37", "What is your favorite memory of us in the rain or cold weather?", "fun"),
        DailyPrompt("38", "How do you think we are doing at supporting each other's career goals?", "deep"),
        DailyPrompt("39", "Where is the most exciting place we have ever kissed?", "intimate"),
        DailyPrompt("40", "How should we organize our weekly meal prep?", "practical"),
        DailyPrompt("41", "What character from a movie or TV show represents each of us?", "fun"),
        DailyPrompt("42", "What is something new you've learned about me recently?", "deep"),
        DailyPrompt("43", "What does perfect physical closeness feel like to you?", "intimate"),
        DailyPrompt("44", "How do you want to handle our holiday plans with family this year?", "practical"),
        DailyPrompt("45", "If we could adopt any pet together, realistic or not, what is it?", "fun"),
        DailyPrompt("46", "What is one fear you have that you'd like me to help support you with?", "deep"),
        DailyPrompt("47", "What outfit or style of mine do you find most attractive?", "intimate"),
        DailyPrompt("48", "Is our current division of home maintenance tasks working?", "practical"),
        DailyPrompt("49", "If you could freeze time and relive one single day of our life, which day?", "fun"),
        DailyPrompt("50", "What is something you are looking forward to building together in the next year?", "deep")
    )

    fun getDailyPromptForToday(): DailyPrompt {
        val dayOfYear = LocalDate.now().dayOfYear
        val index = dayOfYear % staticQuizPrompts.size
        return staticQuizPrompts[index]
    }

    suspend fun logMood(emoji: String, note: String?, stickerId: String?, sexDriveLevel: Int?, isSexDriveShared: Boolean): Result<MoodEntry> {
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val todayStr = LocalDate.now().toString()
        val entry = MoodEntry(
            entryId = UUID.randomUUID().toString(),
            userId = currentUid,
            date = todayStr,
            moodEmoji = emoji,
            note = note,
            stickerId = stickerId,
            sexDriveLevel = sexDriveLevel,
            isSexDriveShared = isSexDriveShared,
            timestamp = System.currentTimeMillis()
        )

        return if (_isDemoMode.value) {
            demoMoodEntries.removeAll { it.userId == currentUid && it.date == todayStr }
            demoMoodEntries.add(entry)
            
            val coupleObj = _couple.value
            if (coupleObj != null) {
                val partnerId = if (coupleObj.user1Uid == currentUid) coupleObj.user2Uid else coupleObj.user1Uid
                _moodEntriesSelf.value = demoMoodEntries.filter { it.userId == currentUid }.sortedByDescending { it.timestamp }
                _moodEntriesPartner.value = demoMoodEntries.filter { it.userId == partnerId }.sortedByDescending { it.timestamp }
            } else {
                _moodEntriesSelf.value = demoMoodEntries.filter { it.userId == currentUid }.sortedByDescending { it.timestamp }
            }
            Result.success(entry)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("users").document(currentUid).collection("moods").document(entry.entryId).set(entry))
                }
                Result.success(entry)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun reactToMood(entryId: String, reactionEmoji: String): Result<Boolean> {
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val coupleObj = _couple.value ?: return Result.failure(Exception("You must be paired"))
        val partnerId = if (coupleObj.user1Uid == currentUid) coupleObj.user2Uid else coupleObj.user1Uid

        return if (_isDemoMode.value) {
            val index = demoMoodEntries.indexOfFirst { it.entryId == entryId }
            if (index != -1) {
                val updated = demoMoodEntries[index].copy(partnerReaction = reactionEmoji)
                demoMoodEntries[index] = updated
                _moodEntriesSelf.value = demoMoodEntries.filter { it.userId == currentUid }.sortedByDescending { it.timestamp }
                _moodEntriesPartner.value = demoMoodEntries.filter { it.userId == partnerId }.sortedByDescending { it.timestamp }
                Result.success(true)
            } else {
                Result.failure(Exception("Mood entry not found"))
            }
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(
                        db.collection("users").document(partnerId).collection("moods").document(entryId).update("partnerReaction", reactionEmoji)
                    )
                }
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun submitPromptResponse(promptId: String, promptDate: String, answer: String): Result<PromptResponse> {
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val coupleObj = _couple.value ?: return Result.failure(Exception("You must be paired"))
        val response = PromptResponse(
            responseId = UUID.randomUUID().toString(),
            coupleId = coupleObj.coupleId,
            promptId = promptId,
            promptDate = promptDate,
            userId = currentUid,
            answer = answer,
            submittedAt = System.currentTimeMillis()
        )

        return if (_isDemoMode.value) {
            demoPromptResponses.removeAll { it.userId == currentUid && it.promptDate == promptDate && it.promptId == promptId && it.coupleId == coupleObj.coupleId }
            demoPromptResponses.add(response)

            val partnerId = if (coupleObj.user1Uid == currentUid) coupleObj.user2Uid else coupleObj.user1Uid
            _promptResponsesSelf.value = demoPromptResponses.filter { it.userId == currentUid }
            _promptResponsesPartner.value = demoPromptResponses.filter { it.userId == partnerId }
            Result.success(response)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("users").document(currentUid).collection("quiz_responses").document(response.responseId).set(response))
                }
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun submitPartnerPromptResponse(promptId: String, promptDate: String, answer: String): Result<PromptResponse> {
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val coupleObj = _couple.value ?: return Result.failure(Exception("You must be paired"))
        val partnerId = if (coupleObj.user1Uid == currentUid) coupleObj.user2Uid else coupleObj.user1Uid ?: "partner_dummy"
        val response = PromptResponse(
            responseId = java.util.UUID.randomUUID().toString(),
            coupleId = coupleObj.coupleId,
            promptId = promptId,
            promptDate = promptDate,
            userId = partnerId,
            answer = answer,
            submittedAt = System.currentTimeMillis()
        )

        return if (_isDemoMode.value) {
            demoPromptResponses.removeAll { it.userId == partnerId && it.promptDate == promptDate && it.promptId == promptId && it.coupleId == coupleObj.coupleId }
            demoPromptResponses.add(response)

            _promptResponsesSelf.value = demoPromptResponses.filter { it.userId == currentUid }
            _promptResponsesPartner.value = demoPromptResponses.filter { it.userId == partnerId }
            Result.success(response)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("users").document(partnerId).collection("quiz_responses").document(response.responseId).set(response))
                }
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun addTodoComment(todoId: String, text: String): Result<TodoComment> {
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val coupleObj = _couple.value ?: return Result.failure(Exception("You must be paired"))
        val comment = TodoComment(
            commentId = UUID.randomUUID().toString(),
            todoId = todoId,
            userId = currentUid,
            text = text,
            createdAt = System.currentTimeMillis()
        )

        return if (_isDemoMode.value) {
            demoTodoComments.add(comment)
            val partnerId = if (coupleObj.user1Uid == currentUid) coupleObj.user2Uid else coupleObj.user1Uid
            _todoComments.value = demoTodoComments.filter { c ->
                val myTodos = demoTodos.filter { it.createdBy == currentUid || it.createdBy == partnerId }.map { it.todoId }
                c.todoId in myTodos
            }
            Result.success(comment)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(
                        db.collection("couples").document(coupleObj.coupleId)
                            .collection("todo_comments").document(comment.commentId).set(comment)
                    )
                }
                Result.success(comment)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- FALLBACK ONLINE KEY-VALUE SYNC ENGINE (KVdb.io) ---
    private fun sanitizeEmail(email: String): String {
        return email.trim().lowercase()
            .replace("@", "_at_")
            .replace(".", "_")
            .replace("-", "_")
            .replace("+", "_plus_")
    }

    private val REGISTRY_ID = "accdddd"
    private val extendsClassMutex = Any()

    private fun kvdbGet(key: String): String? {
        synchronized(extendsClassMutex) {
            return try {
                // Step 1: Fetch the registry
                val registryUrl = java.net.URL("https://extendsclass.com/api/json-storage/bin/$REGISTRY_ID")
                val regConn = registryUrl.openConnection() as java.net.HttpURLConnection
                regConn.requestMethod = "GET"
                regConn.connectTimeout = 8000
                regConn.readTimeout = 8000
                if (regConn.responseCode != 200) {
                    Log.e(tag, "kvdbGet: Failed to fetch registry, status code: ${regConn.responseCode}")
                    return null
                }
                val regStr = regConn.inputStream.bufferedReader().use { it.readText() }
                val registry = JSONObject(regStr)
                if (!registry.has(key)) {
                    Log.d(tag, "kvdbGet: key '$key' not found in registry")
                    return null
                }
                val binId = registry.getString(key)
                
                // Step 2: Fetch the actual bin content
                val binUrl = java.net.URL("https://extendsclass.com/api/json-storage/bin/$binId")
                val binConn = binUrl.openConnection() as java.net.HttpURLConnection
                binConn.requestMethod = "GET"
                binConn.connectTimeout = 8000
                binConn.readTimeout = 8000
                if (binConn.responseCode == 200) {
                    binConn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    Log.e(tag, "kvdbGet: Failed to fetch bin $binId, status code: ${binConn.responseCode}")
                    null
                }
            } catch (e: Exception) {
                Log.e(tag, "kvdbGet error for $key: ${e.message}", e)
                null
            }
        }
    }

    private fun kvdbPut(key: String, value: String): Boolean {
        synchronized(extendsClassMutex) {
            return try {
                // Step 1: Fetch the registry
                val registryUrl = java.net.URL("https://extendsclass.com/api/json-storage/bin/$REGISTRY_ID")
                val regConn = registryUrl.openConnection() as java.net.HttpURLConnection
                regConn.requestMethod = "GET"
                regConn.connectTimeout = 8000
                regConn.readTimeout = 8000
                if (regConn.responseCode != 200) {
                    Log.e(tag, "kvdbPut: Failed to fetch registry, status code: ${regConn.responseCode}")
                    return false
                }
                val regStr = regConn.inputStream.bufferedReader().use { it.readText() }
                val registry = JSONObject(regStr)
                
                val binId = if (registry.has(key)) {
                    registry.getString(key)
                } else {
                    null
                }
                
                if (binId != null) {
                    // Step 2a: Update existing bin via PUT
                    val binUrl = java.net.URL("https://extendsclass.com/api/json-storage/bin/$binId")
                    val binConn = binUrl.openConnection() as java.net.HttpURLConnection
                    binConn.requestMethod = "PUT"
                    binConn.doOutput = true
                    binConn.connectTimeout = 8000
                    binConn.readTimeout = 8000
                    binConn.setRequestProperty("Content-Type", "application/json")
                    binConn.outputStream.use { os ->
                        os.write(value.toByteArray(Charsets.UTF_8))
                    }
                    if (binConn.responseCode == 200) {
                        Log.d(tag, "kvdbPut: Successfully updated bin $binId for key '$key'")
                        true
                    } else {
                        Log.e(tag, "kvdbPut: Failed to update bin $binId, status: ${binConn.responseCode}")
                        false
                    }
                } else {
                    // Step 2b: Create new bin via POST
                    val createUrl = java.net.URL("https://extendsclass.com/api/json-storage/bin")
                    val createConn = createUrl.openConnection() as java.net.HttpURLConnection
                    createConn.requestMethod = "POST"
                    createConn.doOutput = true
                    createConn.connectTimeout = 8000
                    createConn.readTimeout = 8000
                    createConn.setRequestProperty("Content-Type", "application/json")
                    createConn.outputStream.use { os ->
                        os.write(value.toByteArray(Charsets.UTF_8))
                    }
                    if (createConn.responseCode == 201) {
                        val respStr = createConn.inputStream.bufferedReader().use { it.readText() }
                        val respJson = JSONObject(respStr)
                        val newBinId = respJson.getString("id")
                        
                        // Step 3: Update the registry with the new mapping
                        registry.put(key, newBinId)
                        val updateRegConn = registryUrl.openConnection() as java.net.HttpURLConnection
                        updateRegConn.requestMethod = "PUT"
                        updateRegConn.doOutput = true
                        updateRegConn.connectTimeout = 8000
                        updateRegConn.readTimeout = 8000
                        updateRegConn.setRequestProperty("Content-Type", "application/json")
                        updateRegConn.outputStream.use { os ->
                            os.write(registry.toString().toByteArray(Charsets.UTF_8))
                        }
                        if (updateRegConn.responseCode == 200) {
                            Log.d(tag, "kvdbPut: Successfully created bin $newBinId and updated registry for key '$key'")
                            true
                        } else {
                            Log.e(tag, "kvdbPut: Failed to update registry, status: ${updateRegConn.responseCode}")
                            false
                        }
                    } else {
                        Log.e(tag, "kvdbPut: Failed to create new bin, status: ${createConn.responseCode}")
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "kvdbPut error for $key: ${e.message}", e)
                false
            }
        }
    }

    private fun userProfileToJson(p: UserProfile): JSONObject {
        val json = JSONObject()
        json.put("uid", p.uid)
        json.put("name", p.name)
        json.put("nickname", p.nickname)
        json.put("photoUrl", p.photoUrl ?: "")
        json.put("dob", p.dob)
        json.put("gender", p.gender)
        json.put("createdAt", p.createdAt)
        return json
    }

    private fun jsonToUserProfile(json: JSONObject): UserProfile {
        return UserProfile(
            uid = json.optString("uid", ""),
            name = json.optString("name", ""),
            nickname = json.optString("nickname", ""),
            photoUrl = json.optString("photoUrl", "").let { if (it.isEmpty()) null else it },
            dob = json.optString("dob", ""),
            gender = json.optString("gender", ""),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun coupleToJson(c: Couple): JSONObject {
        val json = JSONObject()
        json.put("coupleId", c.coupleId)
        json.put("user1Uid", c.user1Uid)
        json.put("user2Uid", c.user2Uid)
        json.put("pairCode", c.pairCode)
        json.put("relationshipStartDate", c.relationshipStartDate ?: "")
        json.put("createdAt", c.createdAt)
        return json
    }

    private fun jsonToCouple(json: JSONObject): Couple {
        return Couple(
            coupleId = json.optString("coupleId", ""),
            user1Uid = json.optString("user1Uid", ""),
            user2Uid = json.optString("user2Uid", ""),
            pairCode = json.optString("pairCode", ""),
            relationshipStartDate = json.optString("relationshipStartDate", "").let { if (it.isEmpty()) null else it },
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun cycleLogToJson(item: CycleLog): JSONObject {
        val json = JSONObject()
        json.put("cycleId", item.cycleId)
        json.put("coupleId", item.coupleId)
        json.put("userId", item.userId)
        json.put("startDate", item.startDate)
        json.put("endDate", item.endDate ?: "")
        json.put("periodLength", item.periodLength)
        
        val symArr = JSONArray()
        item.symptoms.forEach { symArr.put(it) }
        json.put("symptoms", symArr)
        
        json.put("flowIntensity", item.flowIntensity)
        json.put("notes", item.notes)
        json.put("createdAt", item.createdAt)
        return json
    }

    private fun jsonToCycleLog(json: JSONObject): CycleLog {
        val symList = mutableListOf<String>()
        val symArr = json.optJSONArray("symptoms")
        if (symArr != null) {
            for (i in 0 until symArr.length()) {
                symList.add(symArr.optString(i))
            }
        }
        return CycleLog(
            cycleId = json.optString("cycleId", ""),
            coupleId = json.optString("coupleId", ""),
            userId = json.optString("userId", ""),
            startDate = json.optString("startDate", ""),
            endDate = json.optString("endDate", "").let { if (it.isEmpty()) null else it },
            periodLength = json.optInt("periodLength", 5),
            symptoms = symList,
            flowIntensity = json.optString("flowIntensity", "Medium"),
            notes = json.optString("notes", ""),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun cycleConfigToJson(item: CycleConfig): JSONObject {
        val json = JSONObject()
        json.put("userId", item.userId)
        json.put("avgCycleLength", item.avgCycleLength)
        json.put("avgPeriodLength", item.avgPeriodLength)
        json.put("isPrivacyEnabled", item.isPrivacyEnabled)
        json.put("hideSymptoms", item.hideSymptoms)
        json.put("lastUpdated", item.lastUpdated)
        return json
    }

    private fun jsonToCycleConfig(json: JSONObject): CycleConfig {
        return CycleConfig(
            userId = json.optString("userId", ""),
            avgCycleLength = json.optInt("avgCycleLength", 28),
            avgPeriodLength = json.optInt("avgPeriodLength", 5),
            isPrivacyEnabled = json.optBoolean("isPrivacyEnabled", false),
            hideSymptoms = json.optBoolean("hideSymptoms", false),
            lastUpdated = json.optLong("lastUpdated", System.currentTimeMillis())
        )
    }

    private fun waterGoalToJson(item: WaterGoal): JSONObject {
        val json = JSONObject()
        json.put("userId", item.userId)
        json.put("dailyGoalMl", item.dailyGoalMl)
        json.put("isManual", item.isManual)
        json.put("age", item.age)
        json.put("weightKg", item.weightKg)
        return json
    }

    private fun jsonToWaterGoal(json: JSONObject): WaterGoal {
        return WaterGoal(
            userId = json.optString("userId", ""),
            dailyGoalMl = json.optInt("dailyGoalMl", 2000),
            isManual = json.optBoolean("isManual", false),
            age = json.optInt("age", 25),
            weightKg = json.optInt("weightKg", 70)
        )
    }

    private fun waterLogToJson(item: WaterLog): JSONObject {
        val json = JSONObject()
        json.put("logId", item.logId)
        json.put("userId", item.userId)
        json.put("date", item.date)
        json.put("amountMl", item.amountMl)
        json.put("timestamp", item.timestamp)
        return json
    }

    private fun jsonToWaterLog(json: JSONObject): WaterLog {
        return WaterLog(
            logId = json.optString("logId", ""),
            userId = json.optString("userId", ""),
            date = json.optString("date", ""),
            amountMl = json.optInt("amountMl", 0),
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }

    private fun moodEntryToJson(item: MoodEntry): JSONObject {
        val json = JSONObject()
        json.put("entryId", item.entryId)
        json.put("userId", item.userId)
        json.put("date", item.date)
        json.put("moodEmoji", item.moodEmoji)
        json.put("note", item.note ?: "")
        json.put("stickerId", item.stickerId ?: "")
        json.put("sexDriveLevel", item.sexDriveLevel ?: 0)
        json.put("isSexDriveShared", item.isSexDriveShared)
        json.put("partnerReaction", item.partnerReaction ?: "")
        json.put("timestamp", item.timestamp)
        return json
    }

    private fun jsonToMoodEntry(json: JSONObject): MoodEntry {
        return MoodEntry(
            entryId = json.optString("entryId", ""),
            userId = json.optString("userId", ""),
            date = json.optString("date", ""),
            moodEmoji = json.optString("moodEmoji", ""),
            note = json.optString("note", "").let { if (it.isEmpty()) null else it },
            stickerId = json.optString("stickerId", "").let { if (it.isEmpty()) null else it },
            sexDriveLevel = json.optInt("sexDriveLevel", 0).let { if (it == 0) null else it },
            isSexDriveShared = json.optBoolean("isSexDriveShared", false),
            partnerReaction = json.optString("partnerReaction", "").let { if (it.isEmpty()) null else it },
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }

    private fun promptResponseToJson(item: PromptResponse): JSONObject {
        val json = JSONObject()
        json.put("responseId", item.responseId)
        json.put("coupleId", item.coupleId)
        json.put("promptId", item.promptId)
        json.put("promptDate", item.promptDate)
        json.put("userId", item.userId)
        json.put("answer", item.answer)
        json.put("submittedAt", item.submittedAt)
        return json
    }

    private fun jsonToPromptResponse(json: JSONObject): PromptResponse {
        return PromptResponse(
            responseId = json.optString("responseId", ""),
            coupleId = json.optString("coupleId", ""),
            promptId = json.optString("promptId", ""),
            promptDate = json.optString("promptDate", ""),
            userId = json.optString("userId", ""),
            answer = json.optString("answer", ""),
            submittedAt = json.optLong("submittedAt", System.currentTimeMillis())
        )
    }

    private fun todoCommentToJson(item: TodoComment): JSONObject {
        val json = JSONObject()
        json.put("commentId", item.commentId)
        json.put("todoId", item.todoId)
        json.put("userId", item.userId)
        json.put("text", item.text)
        json.put("createdAt", item.createdAt)
        return json
    }

    private fun jsonToTodoComment(json: JSONObject): TodoComment {
        return TodoComment(
            commentId = json.optString("commentId", ""),
            todoId = json.optString("todoId", ""),
            userId = json.optString("userId", ""),
            text = json.optString("text", ""),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun waterCommentToJson(item: WaterComment): JSONObject {
        val json = JSONObject()
        json.put("commentId", item.commentId)
        json.put("userId", item.userId)
        json.put("date", item.date)
        json.put("text", item.text)
        json.put("emojiReaction", item.emojiReaction)
        json.put("timestamp", item.timestamp)
        return json
    }

    private fun jsonToWaterComment(json: JSONObject): WaterComment {
        return WaterComment(
            commentId = json.optString("commentId", ""),
            userId = json.optString("userId", ""),
            date = json.optString("date", ""),
            text = json.optString("text", ""),
            emojiReaction = json.optString("emojiReaction", ""),
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }

    private fun calendarEventToJson(item: CalendarEvent): JSONObject {
        val json = JSONObject()
        json.put("eventId", item.eventId)
        json.put("coupleId", item.coupleId)
        json.put("title", item.title)
        json.put("date", item.date)
        json.put("type", item.type)
        json.put("note", item.note)
        json.put("photoUrl", item.photoUrl ?: "")
        json.put("createdBy", item.createdBy)
        json.put("createdAt", item.createdAt)
        return json
    }

    private fun jsonToCalendarEvent(json: JSONObject): CalendarEvent {
        return CalendarEvent(
            eventId = json.optString("eventId", ""),
            coupleId = json.optString("coupleId", ""),
            title = json.optString("title", ""),
            date = json.optString("date", ""),
            type = json.optString("type", "moment"),
            note = json.optString("note", ""),
            photoUrl = json.optString("photoUrl", "").let { if (it.isEmpty()) null else it },
            createdBy = json.optString("createdBy", ""),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun todoItemToJson(item: TodoItem): JSONObject {
        val json = JSONObject()
        json.put("todoId", item.todoId)
        json.put("title", item.title)
        json.put("isCompleted", item.isCompleted)
        json.put("createdBy", item.createdBy)
        json.put("createdAt", item.createdAt)
        return json
    }

    private fun jsonToTodoItem(json: JSONObject): TodoItem {
        return TodoItem(
            todoId = json.optString("todoId", ""),
            title = json.optString("title", ""),
            isCompleted = json.optBoolean("isCompleted", false),
            createdBy = json.optString("createdBy", ""),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun serializeUserSyncState(
        profile: UserProfile,
        cycleLogs: List<CycleLog>,
        cycleConfig: CycleConfig?,
        waterGoal: WaterGoal?,
        waterLogs: List<WaterLog>,
        moodEntries: List<MoodEntry>,
        promptResponses: List<PromptResponse>
    ): String {
        val root = JSONObject()
        root.put("profile", userProfileToJson(profile))
        
        val logsArr = JSONArray()
        cycleLogs.forEach { logsArr.put(cycleLogToJson(it)) }
        root.put("cycleLogs", logsArr)
        
        if (cycleConfig != null) {
            root.put("cycleConfig", cycleConfigToJson(cycleConfig))
        }
        
        if (waterGoal != null) {
            root.put("waterGoal", waterGoalToJson(waterGoal))
        }
        
        val waterArr = JSONArray()
        waterLogs.forEach { waterArr.put(waterLogToJson(it)) }
        root.put("waterLogs", waterArr)
        
        val moodArr = JSONArray()
        moodEntries.forEach { moodArr.put(moodEntryToJson(it)) }
        root.put("moodEntries", moodArr)
        
        val quizArr = JSONArray()
        promptResponses.forEach { quizArr.put(promptResponseToJson(it)) }
        root.put("promptResponses", quizArr)
        
        return root.toString()
    }

    private fun parseUserSyncState(jsonStr: String): UserSyncState {
        return try {
            val root = JSONObject(jsonStr)
            val profileJson = root.optJSONObject("profile")
            val profile = profileJson?.let { jsonToUserProfile(it) }
            
            val logsList = mutableListOf<CycleLog>()
            val logsArr = root.optJSONArray("cycleLogs")
            if (logsArr != null) {
                for (i in 0 until logsArr.length()) {
                    logsList.add(jsonToCycleLog(logsArr.getJSONObject(i)))
                }
            }
            
            val cycleConfigJson = root.optJSONObject("cycleConfig")
            val cycleConfig = cycleConfigJson?.let { jsonToCycleConfig(it) }
            
            val waterGoalJson = root.optJSONObject("waterGoal")
            val waterGoal = waterGoalJson?.let { jsonToWaterGoal(it) }
            
            val waterLogsList = mutableListOf<WaterLog>()
            val waterLogsArr = root.optJSONArray("waterLogs")
            if (waterLogsArr != null) {
                for (i in 0 until waterLogsArr.length()) {
                    waterLogsList.add(jsonToWaterLog(waterLogsArr.getJSONObject(i)))
                }
            }
            
            val moodList = mutableListOf<MoodEntry>()
            val moodArr = root.optJSONArray("moodEntries")
            if (moodArr != null) {
                for (i in 0 until moodArr.length()) {
                    moodList.add(jsonToMoodEntry(moodArr.getJSONObject(i)))
                }
            }
            
            val quizList = mutableListOf<PromptResponse>()
            val quizArr = root.optJSONArray("promptResponses")
            if (quizArr != null) {
                for (i in 0 until quizArr.length()) {
                    quizList.add(jsonToPromptResponse(quizArr.getJSONObject(i)))
                }
            }
            
            UserSyncState(profile, logsList, cycleConfig, waterGoal, waterLogsList, moodList, quizList)
        } catch (e: Exception) {
            Log.e(tag, "parseUserSyncState error: ${e.message}")
            UserSyncState()
        }
    }

    private fun storyToJson(item: Story): JSONObject {
        val json = JSONObject()
        json.put("storyId", item.storyId)
        json.put("coupleId", item.coupleId)
        json.put("userId", item.userId)
        json.put("userName", item.userName)
        json.put("mediaType", item.mediaType)
        json.put("textContent", item.textContent)
        json.put("mediaUrl", item.mediaUrl)
        json.put("timestamp", item.timestamp)
        return json
    }

    private fun jsonToStory(json: JSONObject): Story {
        return Story(
            storyId = json.optString("storyId", ""),
            coupleId = json.optString("coupleId", ""),
            userId = json.optString("userId", ""),
            userName = json.optString("userName", ""),
            mediaType = json.optString("mediaType", "text"),
            textContent = json.optString("textContent", ""),
            mediaUrl = if (json.isNull("mediaUrl")) null else json.optString("mediaUrl"),
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }

    private fun serializeCoupleSyncState(
        couple: Couple,
        events: List<CalendarEvent>,
        todos: List<TodoItem>,
        todoComments: List<TodoComment>,
        waterComments: List<WaterComment>,
        stories: List<Story>
    ): String {
        val root = JSONObject()
        root.put("couple", coupleToJson(couple))
        
        val eventsArr = JSONArray()
        events.forEach { eventsArr.put(calendarEventToJson(it)) }
        root.put("events", eventsArr)
        
        val todosArr = JSONArray()
        todos.forEach { todosArr.put(todoItemToJson(it)) }
        root.put("todos", todosArr)
        
        val commentsArr = JSONArray()
        todoComments.forEach { commentsArr.put(todoCommentToJson(it)) }
        root.put("todoComments", commentsArr)
        
        val waterCommentsArr = JSONArray()
        waterComments.forEach { waterCommentsArr.put(waterCommentToJson(it)) }
        root.put("waterComments", waterCommentsArr)

        val storiesArr = JSONArray()
        stories.forEach { storiesArr.put(storyToJson(it)) }
        root.put("stories", storiesArr)
        
        return root.toString()
    }

    private fun parseCoupleSyncState(jsonStr: String): CoupleSyncState {
        return try {
            val root = JSONObject(jsonStr)
            val coupleJson = root.optJSONObject("couple")
            val couple = coupleJson?.let { jsonToCouple(it) }
            
            val eventsList = mutableListOf<CalendarEvent>()
            val eventsArr = root.optJSONArray("events")
            if (eventsArr != null) {
                for (i in 0 until eventsArr.length()) {
                    eventsList.add(jsonToCalendarEvent(eventsArr.getJSONObject(i)))
                }
            }
            
            val todosList = mutableListOf<TodoItem>()
            val todosArr = root.optJSONArray("todos")
            if (todosArr != null) {
                for (i in 0 until todosArr.length()) {
                    todosList.add(jsonToTodoItem(todosArr.getJSONObject(i)))
                }
            }
            
            val commentsList = mutableListOf<TodoComment>()
            val commentsArr = root.optJSONArray("todoComments")
            if (commentsArr != null) {
                for (i in 0 until commentsArr.length()) {
                    commentsList.add(jsonToTodoComment(commentsArr.getJSONObject(i)))
                }
            }
            
            val waterCommentsList = mutableListOf<WaterComment>()
            val waterCommentsArr = root.optJSONArray("waterComments")
            if (waterCommentsArr != null) {
                for (i in 0 until waterCommentsArr.length()) {
                    waterCommentsList.add(jsonToWaterComment(waterCommentsArr.getJSONObject(i)))
                }
            }

            val storiesList = mutableListOf<Story>()
            val storiesArr = root.optJSONArray("stories")
            if (storiesArr != null) {
                for (i in 0 until storiesArr.length()) {
                    storiesList.add(jsonToStory(storiesArr.getJSONObject(i)))
                }
            }
            
            CoupleSyncState(couple, eventsList, todosList, commentsList, waterCommentsList, storiesList)
        } catch (e: Exception) {
            Log.e(tag, "parseCoupleSyncState error: ${e.message}")
            CoupleSyncState()
        }
    }

    private fun saveDemoSession() {
        val editor = prefs.edit()
        _currentUser.value?.let { editor.putString("demo_uid", it.uid) } ?: editor.remove("demo_uid")
        _couple.value?.let { editor.putString("demo_couple_id", it.coupleId) } ?: editor.remove("demo_couple_id")
        editor.apply()
    }

    private fun loadDemoSession() {
        val savedUid = prefs.getString("demo_uid", null)
        val savedCoupleId = prefs.getString("demo_couple_id", null)
        if (savedUid != null) {
            val profile = demoUsers[savedUid] ?: UserProfile(
                uid = savedUid,
                name = prefs.getString("demo_name", "User") ?: "User",
                nickname = prefs.getString("demo_nickname", "User") ?: "User",
                gender = prefs.getString("demo_gender", "Female") ?: "Female"
            )
            demoUsers[savedUid] = profile
            _currentUser.value = profile
        }
    }

    private var pairingWatcherJob: kotlinx.coroutines.Job? = null

    private fun startPairingCodeResponseWatcher(code: String) {
        pairingWatcherJob?.cancel()
        val currentUid = _currentUser.value?.uid ?: return
        
        pairingWatcherJob = CoroutineScope(Dispatchers.IO).launch {
            var attempts = 0
            while (attempts < 180) { // 6 minutes max
                val responseStr = kvdbGet("paircode_${code}_linked")
                if (responseStr != null && responseStr.isNotEmpty()) {
                    try {
                        val coupleJson = JSONObject(responseStr)
                        val completedCouple = jsonToCouple(coupleJson)
                        
                        // We are successfully linked!
                        withContext(Dispatchers.Main) {
                            _couple.value = completedCouple
                            prefs.edit().putString("demo_couple_id", completedCouple.coupleId).apply()
                        }
                        
                        // Clear the notification flag
                        kvdbPut("paircode_${code}_linked", "")
                        
                        // Fetch partner profile to show them linked
                        val partnerUid = if (completedCouple.user1Uid == currentUid) completedCouple.user2Uid else completedCouple.user1Uid
                        val partnerStateStr = kvdbGet("user_${partnerUid}_state")
                        if (partnerStateStr != null) {
                            val partnerSync = parseUserSyncState(partnerStateStr)
                            partnerSync.profile?.let {
                                withContext(Dispatchers.Main) {
                                    _partnerUser.value = it
                                    demoUsers[partnerUid] = it
                                }
                            }
                        }
                        
                        // Start normal background sync
                        startCloudSync()
                        break
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing pairing response: ${e.message}")
                    }
                }
                attempts++
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private var cloudSyncJob: kotlinx.coroutines.Job? = null

    fun startCloudSync() {
        if (!_isDemoMode.value || !_isCloudSyncEnabled.value) return
        val currentUid = _currentUser.value?.uid ?: return
        
        cloudSyncJob?.cancel()
        cloudSyncJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                // 1. Upload our current UserSyncState
                val myProfile = _currentUser.value
                if (myProfile != null) {
                    val myStateStr = serializeUserSyncState(
                        profile = myProfile,
                        cycleLogs = demoCycleLogs.filter { it.userId == currentUid },
                        cycleConfig = _cycleConfigSelf.value,
                        waterGoal = _waterGoalSelf.value,
                        waterLogs = demoWaterLogs.filter { it.userId == currentUid },
                        moodEntries = demoMoodEntries.filter { it.userId == currentUid },
                        promptResponses = demoPromptResponses.filter { it.userId == currentUid }
                    )
                    kvdbPut("user_${currentUid}_state", myStateStr)
                }
                
                // 2. Fetch Couple Shared State and Partner State (if paired)
                val coupleObj = _couple.value
                if (coupleObj != null && coupleObj.user2Uid.isNotEmpty()) {
                    val partnerUid = if (coupleObj.user1Uid == currentUid) coupleObj.user2Uid else coupleObj.user1Uid
                    
                    // Fetch partner user state
                    val partnerStateStr = kvdbGet("user_${partnerUid}_state")
                    if (partnerStateStr != null && partnerStateStr.isNotEmpty()) {
                        try {
                            val partnerState = parseUserSyncState(partnerStateStr)
                            withContext(Dispatchers.Main) {
                                partnerState.profile?.let {
                                    _partnerUser.value = it
                                    demoUsers[partnerUid] = it
                                }
                                
                                // Sync partner's cycle logs
                                val remoteCycleLogs = partnerState.cycleLogs
                                demoCycleLogs.removeAll { it.userId == partnerUid }
                                demoCycleLogs.addAll(remoteCycleLogs)
                                _cycleLogsPartner.value = remoteCycleLogs
                                
                                // Sync partner's cycle config
                                partnerState.cycleConfig?.let {
                                    demoCycleConfigs[partnerUid] = it
                                    _cycleConfigPartner.value = it
                                }
                                
                                // Sync partner's water goal
                                partnerState.waterGoal?.let {
                                    demoWaterGoals[partnerUid] = it
                                    _waterGoalPartner.value = it
                                }
                                
                                // Sync partner's water logs
                                val remoteWaterLogs = partnerState.waterLogs
                                demoWaterLogs.removeAll { it.userId == partnerUid }
                                demoWaterLogs.addAll(remoteWaterLogs)
                                _waterLogsPartner.value = remoteWaterLogs
                                val todayStr = LocalDate.now().toString()
                                _waterIntakePartner.value = remoteWaterLogs.filter { it.date == todayStr }.sumOf { it.amountMl }
                                
                                // Sync partner's mood entries
                                val remoteMoodEntries = partnerState.moodEntries
                                demoMoodEntries.removeAll { it.userId == partnerUid }
                                demoMoodEntries.addAll(remoteMoodEntries)
                                _moodEntriesPartner.value = remoteMoodEntries.sortedByDescending { it.timestamp }
                                
                                // Sync partner's prompt responses
                                val remotePromptResponses = partnerState.promptResponses
                                demoPromptResponses.removeAll { it.userId == partnerUid }
                                demoPromptResponses.addAll(remotePromptResponses)
                                _promptResponsesPartner.value = remotePromptResponses
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Error syncing partner state: ${e.message}")
                        }
                    }
                    
                    // Fetch and Sync Shared Couple State (Events, Todos, Comments, Stories)
                    val coupleStateStr = kvdbGet("couple_${coupleObj.coupleId}_state")
                    if (coupleStateStr != null && coupleStateStr.isNotEmpty()) {
                        try {
                            val remoteCoupleState = parseCoupleSyncState(coupleStateStr)
                            
                            withContext(Dispatchers.Main) {
                                // Conflict-free merge of calendar events (by UUID)
                                val mergedEvents = (demoEvents + remoteCoupleState.events).distinctBy { it.eventId }.sortedBy { it.date }
                                demoEvents.clear()
                                demoEvents.addAll(mergedEvents)
                                _events.value = mergedEvents
                                
                                // Conflict-free merge of todos (by UUID)
                                val mergedTodos = (demoTodos + remoteCoupleState.todos).distinctBy { it.todoId }.sortedByDescending { it.createdAt }
                                demoTodos.clear()
                                demoTodos.addAll(mergedTodos)
                                _todos.value = mergedTodos
                                
                                // Merge comments
                                val mergedComments = (demoTodoComments + remoteCoupleState.todoComments).distinctBy { it.commentId }
                                demoTodoComments.clear()
                                demoTodoComments.addAll(mergedComments)
                                _todoComments.value = mergedComments.filter { c ->
                                    val myTodos = mergedTodos.map { it.todoId }
                                    c.todoId in myTodos
                                }
                                
                                // Merge water comments
                                val mergedWaterComments = (demoWaterComments + remoteCoupleState.waterComments).distinctBy { it.commentId }
                                demoWaterComments.clear()
                                demoWaterComments.addAll(mergedWaterComments)
                                _waterComments.value = mergedWaterComments.filter { it.date == LocalDate.now().toString() }

                                // Merge stories
                                val twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                                val mergedStories = (demoStories + remoteCoupleState.stories)
                                    .distinctBy { it.storyId }
                                    .filter { it.timestamp >= twentyFourHoursAgo }
                                    .sortedBy { it.timestamp }
                                demoStories.clear()
                                demoStories.addAll(mergedStories)
                                _stories.value = mergedStories
                            }
                            
                            // Re-upload merged state if there were any new local items that were not present in remote state
                            val anyNewLocal = demoEvents.any { de -> remoteCoupleState.events.none { re -> re.eventId == de.eventId } } ||
                                              demoTodos.any { dt -> remoteCoupleState.todos.none { rt -> rt.todoId == dt.todoId } } ||
                                              demoTodoComments.any { dc -> remoteCoupleState.todoComments.none { rc -> rc.commentId == dc.commentId } } ||
                                              demoWaterComments.any { dw -> remoteCoupleState.waterComments.none { rw -> rw.commentId == dw.commentId } } ||
                                              demoStories.any { ds -> remoteCoupleState.stories.none { rs -> rs.storyId == ds.storyId } }
                            
                            if (anyNewLocal) {
                                val stateStr = serializeCoupleSyncState(coupleObj, demoEvents, demoTodos, demoTodoComments, demoWaterComments, demoStories)
                                kvdbPut("couple_${coupleObj.coupleId}_state", stateStr)
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Error syncing couple state: ${e.message}")
                        }
                    } else {
                        // If couple_state is completely empty/deleted on the server, upload our current local state to initialize it
                        val stateStr = serializeCoupleSyncState(coupleObj, demoEvents, demoTodos, demoTodoComments, demoWaterComments, demoStories)
                        kvdbPut("couple_${coupleObj.coupleId}_state", stateStr)
                    }
                }
                
                kotlinx.coroutines.delay(1500) // Poll every 1.5 seconds for extremely snappy, real-time chat & stories UX
            }
        }
    }

    private suspend fun loadCloudCoupleState(coupleId: String, currentUid: String) {
        val coupleStateStr = withContext(Dispatchers.IO) { kvdbGet("couple_${coupleId}_state") }
        if (coupleStateStr != null && coupleStateStr.isNotEmpty()) {
            val syncState = parseCoupleSyncState(coupleStateStr)
            val coupleObj = syncState.couple ?: return
            
            withContext(Dispatchers.Main) {
                _couple.value = coupleObj
                
                demoEvents.clear()
                demoEvents.addAll(syncState.events)
                _events.value = syncState.events.sortedBy { it.date }
                
                demoTodos.clear()
                demoTodos.addAll(syncState.todos)
                _todos.value = syncState.todos.sortedByDescending { it.createdAt }
                
                demoTodoComments.clear()
                demoTodoComments.addAll(syncState.todoComments)
                
                demoWaterComments.clear()
                demoWaterComments.addAll(syncState.waterComments)
                _waterComments.value = syncState.waterComments.filter { it.date == LocalDate.now().toString() }

                val twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                demoStories.clear()
                demoStories.addAll(syncState.stories.filter { it.timestamp >= twentyFourHoursAgo })
                _stories.value = demoStories.sortedBy { it.timestamp }
                
                // Fetch partner profile
                val partnerUid = if (coupleObj.user1Uid == currentUid) coupleObj.user2Uid else coupleObj.user1Uid
                CoroutineScope(Dispatchers.IO).launch {
                    val partnerStateStr = kvdbGet("user_${partnerUid}_state")
                    if (partnerStateStr != null) {
                        val partnerSync = parseUserSyncState(partnerStateStr)
                        partnerSync.profile?.let {
                            withContext(Dispatchers.Main) {
                                _partnerUser.value = it
                                demoUsers[partnerUid] = it
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isBase64String(str: String): Boolean {
        if (str.startsWith("data:")) return true
        if (str.length < 100) return false
        return try {
            android.util.Base64.decode(str, android.util.Base64.DEFAULT)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun compressImageUri(uri: android.net.Uri): ByteArray? {
        return try {
            val options = android.graphics.BitmapFactory.Options()
            options.inJustDecodeBounds = true
            var inputStream = context.contentResolver.openInputStream(uri)
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            
            if (options.outWidth <= 0 || options.outHeight <= 0) return null
            
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
            
            val decodeOptions = android.graphics.BitmapFactory.Options()
            decodeOptions.inSampleSize = inSampleSize
            inputStream = context.contentResolver.openInputStream(uri)
            val decodedBitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()
            
            if (decodedBitmap == null) return null
            
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
            
            if (resized != decodedBitmap) {
                resized.recycle()
            }
            decodedBitmap.recycle()
            bytes
        } catch (e: Exception) {
            Log.e(tag, "Error compressing image uri: ${e.message}", e)
            null
        }
    }

    private fun getMediaBytes(uriString: String, mediaType: String): ByteArray? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            if (mediaType == "image") {
                compressImageUri(uri) ?: context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error reading media bytes from uri: ${e.message}", e)
            null
        }
    }

    private suspend fun uploadBytesToStorage(coupleId: String, bytes: ByteArray, mediaType: String): String? {
        val storageRef = storage ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val extension = if (mediaType == "image") "jpg" else "mp4"
                val contentType = if (mediaType == "image") "image/jpeg" else "video/mp4"
                val fileName = "stories/${UUID.randomUUID()}.$extension"
                val fileRef = storageRef.reference.child("couples/$coupleId/$fileName")
                
                val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                    .setContentType(contentType)
                    .build()

                val uploadTask = fileRef.putBytes(bytes, metadata)
                Tasks.await(uploadTask)
                val downloadUrlTask = fileRef.downloadUrl
                val downloadUri = Tasks.await(downloadUrlTask)
                downloadUri.toString()
            } catch (e: Exception) {
                Log.e(tag, "Failed to upload bytes to Firebase Storage: ${e.message}", e)
                null
            }
        }
    }

    private suspend fun uploadMediaToStorage(coupleId: String, mediaUrl: String, mediaType: String): String? {
        val storageRef = storage ?: return null
        return try {
            val (bytes, extension, contentType) = when {
                mediaUrl.startsWith("data:image/jpeg;base64,") -> {
                    val base64Data = mediaUrl.substringAfter("data:image/jpeg;base64,")
                    val decoded = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    Triple(decoded, "jpg", "image/jpeg")
                }
                mediaUrl.startsWith("data:image/png;base64,") -> {
                    val base64Data = mediaUrl.substringAfter("data:image/png;base64,")
                    val decoded = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    Triple(decoded, "png", "image/png")
                }
                mediaUrl.startsWith("data:audio/3gp;base64,") || mediaUrl.startsWith("data:audio/3gpp;base64,") || mediaUrl.startsWith("data:audio/amr;base64,") -> {
                    val base64Data = mediaUrl.substringAfter("base64,")
                    val decoded = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    Triple(decoded, "3gp", "audio/3gpp")
                }
                mediaType == "voice" -> {
                    val base64Data = if (mediaUrl.contains("base64,")) mediaUrl.substringAfter("base64,") else mediaUrl
                    val decoded = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    Triple(decoded, "3gp", "audio/3gpp")
                }
                else -> {
                    val decoded = android.util.Base64.decode(mediaUrl, android.util.Base64.DEFAULT)
                    Triple(decoded, "jpg", "image/jpeg")
                }
            }

            val fileName = "chat_media/${UUID.randomUUID()}.$extension"
            val fileRef = storageRef.reference.child("couples/$coupleId/$fileName")
            
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType(contentType)
                .build()

            withContext(Dispatchers.IO) {
                val uploadTask = fileRef.putBytes(bytes, metadata)
                Tasks.await(uploadTask)
                val downloadUrlTask = fileRef.downloadUrl
                val downloadUri = Tasks.await(downloadUrlTask)
                downloadUri.toString()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to upload media to Firebase Storage: ${e.message}", e)
            null
        }
    }

    suspend fun sendChatMessage(text: String, mediaUrl: String? = null, mediaType: String = "text"): Result<EncryptedMessage> {
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val coupleObj = _couple.value ?: return Result.failure(Exception("You must be paired to chat"))
        
        var finalMediaUrl = mediaUrl
        if (!_isDemoMode.value && mediaUrl != null && isBase64String(mediaUrl)) {
            val uploadedUrl = uploadMediaToStorage(coupleObj.coupleId, mediaUrl, mediaType)
            if (uploadedUrl != null) {
                finalMediaUrl = uploadedUrl
            } else {
                Log.w(tag, "Failed to upload to Firebase Storage, falling back to sending base64-encoded media payload.")
                finalMediaUrl = mediaUrl
            }
        }

        val encryptedText = if (text.isNotBlank()) {
            com.example.util.CryptoUtils.encrypt(text, coupleObj.coupleId)
        } else {
            ""
        }
        
        val encryptedMediaUrl = if (finalMediaUrl != null && finalMediaUrl.isNotBlank()) {
            com.example.util.CryptoUtils.encrypt(finalMediaUrl, coupleObj.coupleId)
        } else {
            null
        }

        val message = EncryptedMessage(
            messageId = UUID.randomUUID().toString(),
            coupleId = coupleObj.coupleId,
            senderId = currentUid,
            encryptedText = encryptedText,
            encryptedMediaUrl = encryptedMediaUrl,
            mediaType = mediaType,
            timestamp = System.currentTimeMillis()
        )

        return if (_isDemoMode.value) {
            demoChatMessages.add(message)
            _chatMessages.value = demoChatMessages.toList()
            Result.success(message)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(
                        db.collection("couples").document(coupleObj.coupleId)
                            .collection("chat_messages").document(message.messageId)
                            .set(message)
                    )
                }
                Result.success(message)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun reactToMessage(messageId: String, reaction: String): Result<Boolean> {
        val coupleObj = _couple.value ?: return Result.failure(Exception("You must be paired to react"))
        return if (_isDemoMode.value) {
            val index = demoChatMessages.indexOfFirst { it.messageId == messageId }
            if (index != -1) {
                demoChatMessages[index] = demoChatMessages[index].copy(reaction = reaction)
                _chatMessages.value = demoChatMessages.toList()
                Result.success(true)
            } else {
                val listIndex = _chatMessages.value.indexOfFirst { it.messageId == messageId }
                if (listIndex != -1) {
                    val updatedList = _chatMessages.value.toMutableList()
                    updatedList[listIndex] = updatedList[listIndex].copy(reaction = reaction)
                    _chatMessages.value = updatedList
                    Result.success(true)
                } else {
                    Result.failure(Exception("Message not found"))
                }
            }
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(
                        db.collection("couples").document(coupleObj.coupleId)
                            .collection("chat_messages").document(messageId)
                            .update("reaction", reaction)
                    )
                }
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun setTypingStatus(isTyping: Boolean): Result<Unit> {
        val coupleObj = _couple.value ?: return Result.failure(Exception("Not paired"))
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        if (_isDemoMode.value) {
            val updatedCouple = if (currentUid == coupleObj.user1Uid) {
                coupleObj.copy(user1Typing = isTyping)
            } else {
                coupleObj.copy(user2Typing = isTyping)
            }
            _couple.value = updatedCouple
            
            // Upload immediately to KVDB so the partner sees it instantly!
            withContext(Dispatchers.IO) {
                val stateStr = serializeCoupleSyncState(updatedCouple, demoEvents, demoTodos, demoTodoComments, demoWaterComments, demoStories)
                kvdbPut("couple_${coupleObj.coupleId}_state", stateStr)
            }
            return Result.success(Unit)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                val field = if (currentUid == coupleObj.user1Uid) "user1Typing" else "user2Typing"
                withContext(Dispatchers.IO) {
                    Tasks.await(db.collection("couples").document(coupleObj.coupleId).update(field, isTyping))
                }
                return Result.success(Unit)
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
    }

    suspend fun markMessagesAsSeen(): Result<Unit> {
        val coupleObj = _couple.value ?: return Result.failure(Exception("Not paired"))
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("Not authenticated"))
        val partnerUid = if (currentUid == coupleObj.user1Uid) coupleObj.user2Uid else coupleObj.user1Uid
        
        if (_isDemoMode.value) {
            var updated = false
            demoChatMessages.forEachIndexed { index, msg ->
                if (msg.senderId == partnerUid && !msg.seen) {
                    demoChatMessages[index] = msg.copy(seen = true)
                    updated = true
                }
            }
            if (updated) {
                _chatMessages.value = demoChatMessages.toList()
            }
            return Result.success(Unit)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    val unreadQuery = db.collection("couples")
                        .document(coupleObj.coupleId)
                        .collection("chat_messages")
                        .whereEqualTo("senderId", partnerUid)
                        .whereEqualTo("seen", false)
                        .get()
                    
                    val snapshot = Tasks.await(unreadQuery)
                    if (!snapshot.isEmpty) {
                        val batch = db.batch()
                        for (doc in snapshot.documents) {
                            batch.update(doc.reference, "seen", true)
                        }
                        Tasks.await(batch.commit())
                    }
                }
                return Result.success(Unit)
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
    }

    suspend fun clearChatMessages(): Result<Unit> {
        val coupleObj = _couple.value ?: return Result.failure(Exception("You must be paired"))
        return if (_isDemoMode.value) {
            demoChatMessages.clear()
            _chatMessages.value = emptyList()
            Result.success(Unit)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    val messages = Tasks.await(db.collection("couples").document(coupleObj.coupleId).collection("chat_messages").get())
                    for (doc in messages.documents) {
                        Tasks.await(db.collection("couples").document(coupleObj.coupleId).collection("chat_messages").document(doc.id).delete())
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun receiveDemoChatMessage(text: String): Result<EncryptedMessage> {
        val coupleObj = _couple.value ?: return Result.failure(Exception("Not paired"))
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("Not authenticated"))
        val partnerUid = if (currentUid == coupleObj.user1Uid) coupleObj.user2Uid else coupleObj.user1Uid
        
        val encryptedText = com.example.util.CryptoUtils.encrypt(text, coupleObj.coupleId)
        val message = EncryptedMessage(
            messageId = java.util.UUID.randomUUID().toString(),
            coupleId = coupleObj.coupleId,
            senderId = partnerUid,
            encryptedText = encryptedText,
            mediaType = "text",
            timestamp = System.currentTimeMillis(),
            seen = false,
            delivered = true
        )
        demoChatMessages.add(message)
        _chatMessages.value = demoChatMessages.toList()
        return Result.success(message)
    }

    suspend fun addStory(mediaType: String, textContent: String, mediaUrl: String? = null): Result<Story> {
        val currentUid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val coupleObj = _couple.value ?: return Result.failure(Exception("You must be paired to post a story"))
        val userName = _currentUser.value?.name ?: "Partner"

        var finalMediaUrl = mediaUrl
        if (mediaUrl != null && (mediaUrl.startsWith("content://") || mediaUrl.startsWith("file://") || mediaUrl.startsWith("/"))) {
            val bytes = getMediaBytes(mediaUrl, mediaType)
            if (bytes != null) {
                if (_isDemoMode.value) {
                    if (mediaType == "image") {
                        val base64String = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT).trim()
                        finalMediaUrl = "data:image/jpeg;base64,$base64String"
                    } else {
                        // Keep original URI for video locally, fallback handles it for partner in UI
                        finalMediaUrl = mediaUrl
                    }
                } else {
                    val uploadedUrl = uploadBytesToStorage(coupleObj.coupleId, bytes, mediaType)
                    if (uploadedUrl != null) {
                        finalMediaUrl = uploadedUrl
                    } else {
                        // Fallback to avoid error
                        if (mediaType == "image") {
                            Log.w(tag, "Failed to upload to Firebase Storage, falling back to base64 encoding")
                            val base64String = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT).trim()
                            finalMediaUrl = "data:image/jpeg;base64,$base64String"
                        } else {
                            Log.w(tag, "Failed to upload to Firebase Storage, falling back to beautiful landscape video loop")
                            finalMediaUrl = "https://assets.mixkit.co/videos/preview/mixkit-forest-stream-in-the-sunlight-529-large.mp4"
                        }
                    }
                }
            } else {
                return Result.failure(Exception("Failed to read media content from device"))
            }
        }

        val story = Story(
            storyId = java.util.UUID.randomUUID().toString(),
            coupleId = coupleObj.coupleId,
            userId = currentUid,
            userName = userName,
            mediaType = mediaType,
            textContent = textContent,
            mediaUrl = finalMediaUrl,
            timestamp = System.currentTimeMillis()
        )

        return if (_isDemoMode.value) {
            demoStories.add(story)
            val twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            val sortedList = demoStories.filter { it.timestamp >= twentyFourHoursAgo }.sortedBy { it.timestamp }
            _stories.value = sortedList
            
            // Upload immediately to KVDB so the partner gets it instantly!
            withContext(Dispatchers.IO) {
                val stateStr = serializeCoupleSyncState(coupleObj, demoEvents, demoTodos, demoTodoComments, demoWaterComments, sortedList)
                kvdbPut("couple_${coupleObj.coupleId}_state", stateStr)
            }
            Result.success(story)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(
                        db.collection("couples").document(coupleObj.coupleId)
                            .collection("stories").document(story.storyId)
                            .set(story)
                    )
                }
                Result.success(story)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteStory(storyId: String): Result<Unit> {
        val coupleObj = _couple.value ?: return Result.failure(Exception("You must be paired to delete a story"))
        return if (_isDemoMode.value) {
            demoStories.removeAll { it.storyId == storyId }
            val twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            val sortedList = demoStories.filter { it.timestamp >= twentyFourHoursAgo }.sortedBy { it.timestamp }
            _stories.value = sortedList
            
            // Upload immediately to KVDB so partner state stays synced!
            withContext(Dispatchers.IO) {
                val stateStr = serializeCoupleSyncState(coupleObj, demoEvents, demoTodos, demoTodoComments, demoWaterComments, sortedList)
                kvdbPut("couple_${coupleObj.coupleId}_state", stateStr)
            }
            Result.success(Unit)
        } else {
            val db = firestore ?: return Result.failure(Exception("Firestore not available"))
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(
                        db.collection("couples").document(coupleObj.coupleId)
                            .collection("stories").document(storyId)
                            .delete()
                    )
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    data class UserSyncState(
        val profile: UserProfile? = null,
        val cycleLogs: List<CycleLog> = emptyList(),
        val cycleConfig: CycleConfig? = null,
        val waterGoal: WaterGoal? = null,
        val waterLogs: List<WaterLog> = emptyList(),
        val moodEntries: List<MoodEntry> = emptyList(),
        val promptResponses: List<PromptResponse> = emptyList()
    )

    data class CoupleSyncState(
        val couple: Couple? = null,
        val events: List<CalendarEvent> = emptyList(),
        val todos: List<TodoItem> = emptyList(),
        val todoComments: List<TodoComment> = emptyList(),
        val waterComments: List<WaterComment> = emptyList(),
        val stories: List<Story> = emptyList()
    )
}
