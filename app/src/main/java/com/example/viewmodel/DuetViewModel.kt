package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.model.MoodEntry
import com.example.model.DailyPrompt
import com.example.model.PromptResponse
import com.example.model.QuizData
import com.example.model.QuizTheme
import com.example.model.McqQuestion
import com.example.model.TodoComment
import com.example.model.EncryptedMessage
import com.example.model.Story
import com.example.repository.DuetRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DuetViewModel(application: Application) : AndroidViewModel(application) {

    val repository = DuetRepository(application)

    // Repository-backed states
    val currentUser: StateFlow<UserProfile?> = repository.currentUser
    val partnerUser: StateFlow<UserProfile?> = repository.partnerUser
    val coupleState: StateFlow<Couple?> = repository.couple
    val calendarEvents: StateFlow<List<CalendarEvent>> = repository.events
    val todoList: StateFlow<List<TodoItem>> = repository.todos
    val waterSelf: StateFlow<Int> = repository.waterIntakeSelf
    val waterPartner: StateFlow<Int> = repository.waterIntakePartner
    val isDemoMode: StateFlow<Boolean> = repository.isDemoMode
    val isFirebaseInitialized: StateFlow<Boolean> = repository.isFirebaseInitialized

    // PART A - Cycle Tracking State Flow exposures
    val cycleLogsSelf: StateFlow<List<CycleLog>> = repository.cycleLogsSelf
    val cycleLogsPartner: StateFlow<List<CycleLog>> = repository.cycleLogsPartner
    val cycleConfigSelf: StateFlow<CycleConfig?> = repository.cycleConfigSelf
    val cycleConfigPartner: StateFlow<CycleConfig?> = repository.cycleConfigPartner

    // PART B - Water Intake Tracking State Flow exposures
    val waterGoalSelf: StateFlow<WaterGoal?> = repository.waterGoalSelf
    val waterGoalPartner: StateFlow<WaterGoal?> = repository.waterGoalPartner
    val waterLogsSelf: StateFlow<List<WaterLog>> = repository.waterLogsSelf
    val waterLogsPartner: StateFlow<List<WaterLog>> = repository.waterLogsPartner
    val waterComments: StateFlow<List<WaterComment>> = repository.waterComments

    // PART C - Mood Sharing exposures
    val moodEntriesSelf: StateFlow<List<MoodEntry>> = repository.moodEntriesSelf
    val moodEntriesPartner: StateFlow<List<MoodEntry>> = repository.moodEntriesPartner

    // PART D - Daily Partner Quiz exposures
    val promptResponsesSelf: StateFlow<List<PromptResponse>> = repository.promptResponsesSelf
    val promptResponsesPartner: StateFlow<List<PromptResponse>> = repository.promptResponsesPartner

    // PART E - Shared Todo Comments exposures
    val todoComments: StateFlow<List<TodoComment>> = repository.todoComments

    // E2EE Chat exposures
    val chatMessages: StateFlow<List<EncryptedMessage>> = repository.chatMessages

    // Story exposures
    val stories: StateFlow<List<Story>> = repository.stories

    // Local UI states
    private val _selectedDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _pairingCodeGenerated = MutableStateFlow<String?>(null)
    val pairingCodeGenerated: StateFlow<String?> = _pairingCodeGenerated.asStateFlow()

    // Current app navigation state/tab
    private val _currentTab = MutableStateFlow("home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Background/Theme selection state
    private val _selectedThemeId = MutableStateFlow("warm_rose")
    val selectedThemeId: StateFlow<String> = _selectedThemeId.asStateFlow()

    init {
        // Automatically set a pending code state if a couple document has one and we are user1
        viewModelScope.launch {
            coupleState.collect { couple ->
                if (couple != null && couple.pairCode.isNotEmpty() && couple.user2Uid.isEmpty()) {
                    _pairingCodeGenerated.value = couple.pairCode
                } else {
                    _pairingCodeGenerated.value = null
                }
            }
        }
        // Automatically schedule water reminders on startup
        try {
            repository.scheduleWaterReminder(8, 22, 2)
        } catch (e: Exception) {
            android.util.Log.e("DuetViewModel", "Failed to auto-schedule water reminder: ${e.message}")
        }
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun setThemeId(themeId: String) {
        _selectedThemeId.value = themeId
    }

    fun toggleDemoMode(enabled: Boolean) {
        repository.setDemoMode(enabled)
        showToast(if (enabled) "Switched to offline Demo Mode!" else "Switched to live Firebase Auth/Firestore!")
    }

    fun switchDemoUser(uid: String) {
        repository.switchDemoUser(uid)
        showToast("Switched user profile!")
        val newProfileName = if (uid == "demo_user_1") "Alex 💙" else "Emily 💖"
        val partnerName = if (uid == "demo_user_1") "Emily 💖" else "Alex 💙"
        repository.postPartnerNotification("👤 Profile Switched", "Logged in as $newProfileName. You have new notifications from $partnerName!")
    }

    fun getDemoUsers(): List<UserProfile> {
        return repository.getDemoUsersList()
    }

    private fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.emit(message)
        }
    }

    // AUTH ACTIONS
    fun signUp(email: String, password: String, name: String, nickname: String, dob: String, gender: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.signUpWithEmail(email, password, name, nickname, dob, gender)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    showToast("Account created successfully!")
                },
                onFailure = { error ->
                    showToast("Signup failed: ${error.localizedMessage ?: "Unknown error"}")
                }
            )
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.loginWithEmail(email, password)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    showToast("Welcome back, ${it.nickname}!")
                },
                onFailure = { error ->
                    showToast("Login failed: ${error.localizedMessage ?: "Unknown error"}")
                }
            )
        }
    }

    fun createDemoProfile(name: String, nickname: String, dob: String, gender: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val newUid = repository.createDemoUser(name, nickname, dob, gender, isPartner = false)
            _isLoading.value = false
            showToast("Demo profile '$nickname' created and loaded!")
        }
    }

    fun signOut() {
        repository.signOut()
        showToast("Signed out successfully.")
    }

    // PAIRING ACTIONS
    fun generatePairingCode() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.generatePairCode()
            _isLoading.value = false
            result.fold(
                onSuccess = { code ->
                    _pairingCodeGenerated.value = code
                    showToast("Pairing code generated!")
                },
                onFailure = { error ->
                    showToast(error.localizedMessage ?: "Could not generate pairing code")
                }
            )
        }
    }

    fun pairWithCode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.pairWithCode(code)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    showToast("Successfully paired! Together at last ❤️")
                },
                onFailure = { error ->
                    showToast("Pairing failed: ${error.localizedMessage ?: "Invalid code"}")
                }
            )
        }
    }

    fun simulatePartnerPairing(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.simulatePartnerPairing(code)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    showToast("Partner connected and synced successfully in real-time! ❤️")
                },
                onFailure = { error ->
                    showToast("Simulation failed: ${error.localizedMessage}")
                }
            )
        }
    }

    fun updateRelationshipStartDate(date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateRelationshipStartDate(date)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    showToast("Anniversary date updated!")
                },
                onFailure = { error ->
                    showToast("Failed to update anniversary: ${error.localizedMessage}")
                }
            )
        }
    }

    fun unpairCouple() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.unpairCouple()
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    showToast("Successfully unpaired accounts.")
                },
                onFailure = { error ->
                    showToast("Failed to unpair: ${error.localizedMessage}")
                }
            )
        }
    }

    // CALENDAR ACTIONS
    fun addEvent(title: String, date: String, type: String, note: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addCalendarEvent(title, date, type, note)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    showToast("Moment added successfully! ✨")
                    triggerDemoPartnerNotification("event", title)
                },
                onFailure = { error ->
                    showToast("Failed to add moment: ${error.localizedMessage}")
                }
            )
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteCalendarEvent(eventId)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    showToast("Moment removed.")
                },
                onFailure = { error ->
                    showToast("Failed to delete: ${error.localizedMessage}")
                }
            )
        }
    }

    // WATER TRACKER ACTIONS
    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            val result = repository.updateWaterIntake(amountMl)
            result.fold(
                onSuccess = {
                    showToast("Logged $amountMl ml water!")
                    triggerDemoPartnerNotification("water", "$amountMl")
                    // Auto-reschedule to update smart intervals based on new remaining target
                    repository.scheduleWaterReminder(8, 22, 2)
                },
                onFailure = { error ->
                    showToast(error.localizedMessage ?: "Error logging water")
                }
            )
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            val result = repository.resetWaterIntake()
            result.fold(
                onSuccess = {
                    showToast("Water log reset for today.")
                    // Auto-reschedule to reset smart intervals
                    repository.scheduleWaterReminder(8, 22, 2)
                },
                onFailure = { error ->
                    showToast(error.localizedMessage ?: "Error resetting water")
                }
            )
        }
    }

    fun saveWaterGoal(goal: WaterGoal) {
        viewModelScope.launch {
            val result = repository.saveWaterGoal(goal)
            result.fold(
                onSuccess = {
                    showToast("Daily goal updated!")
                    // Auto-reschedule to apply new goal in smart intervals calculation
                    repository.scheduleWaterReminder(8, 22, 2)
                },
                onFailure = { error -> showToast("Error: ${error.localizedMessage}") }
            )
        }
    }

    fun addWaterComment(text: String, emoji: String) {
        viewModelScope.launch {
            val result = repository.addWaterComment(text, emoji)
            result.fold(
                onSuccess = {
                    showToast("Sent cheer to your partner! 🎉")
                    triggerDemoPartnerNotification("water_comment", text)
                },
                onFailure = { error -> showToast("Error: ${error.localizedMessage}") }
            )
        }
    }

    fun scheduleWaterReminder(startHour: Int = 8, endHour: Int = 22, intervalHours: Int = 2) {
        repository.scheduleWaterReminder(startHour, endHour, intervalHours)
        showToast("Smart reminder active! Dynamic intervals calculated automatically.")
    }

    // --- PART A - CYCLE TRACKING ACTIONS ---
    fun saveCycleConfig(config: CycleConfig) {
        viewModelScope.launch {
            val result = repository.saveCycleConfig(config)
            result.fold(
                onSuccess = { showToast("Saved cycle configuration!") },
                onFailure = { error -> showToast("Error: ${error.localizedMessage}") }
            )
        }
    }

    fun logCyclePeriod(startDate: String, endDate: String?, symptoms: List<String>, flow: String, notes: String) {
        viewModelScope.launch {
            val result = repository.logCyclePeriod(startDate, endDate, symptoms, flow, notes)
            result.fold(
                onSuccess = {
                    showToast("Cycle period logged!")
                    triggerDemoPartnerNotification("cycle")
                },
                onFailure = { error -> showToast("Error: ${error.localizedMessage}") }
            )
        }
    }

    fun deleteCycleLog(cycleId: String) {
        viewModelScope.launch {
            val result = repository.deleteCycleLog(cycleId)
            result.fold(
                onSuccess = { showToast("Period log removed.") },
                onFailure = { error -> showToast("Error: ${error.localizedMessage}") }
            )
        }
    }

    fun getPredictionsSelf(): CyclePrediction? = repository.getPredictionsSelf()
    fun getPredictionsPartner(): CyclePrediction? = repository.getPredictionsPartner()

    // TODO TRACKER ACTIONS
    fun addTodo(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val result = repository.addTodoItem(title)
            result.fold(
                onSuccess = {
                    showToast("Added task!")
                    triggerDemoPartnerNotification("todo", title)
                },
                onFailure = { error ->
                    showToast("Failed to add task: ${error.localizedMessage}")
                }
            )
        }
    }

    fun toggleTodo(todoId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleTodoItem(todoId, isCompleted)
            result.fold(
                onSuccess = {
                    // Success toast or quiet sync
                },
                onFailure = { error ->
                    showToast("Failed to update task: ${error.localizedMessage}")
                }
            )
        }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            val result = repository.deleteTodoItem(todoId)
            result.fold(
                onSuccess = {
                    showToast("Task deleted.")
                },
                onFailure = { error ->
                    showToast("Failed to delete task: ${error.localizedMessage}")
                }
            )
        }
    }

    // HELPER DATE ARITHMETIC
    fun getRelationshipDaysCount(): Long {
        val couple = coupleState.value ?: return 0
        val startDateStr = couple.relationshipStartDate ?: return 0
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = sdf.parse(startDateStr) ?: return 0
            val diffMs = System.currentTimeMillis() - startDate.time
            return (diffMs / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
        } catch (e: Exception) {
            return 0
        }
    }

    // --- MOOD TRACKER ACTIONS ---
    fun logMood(emoji: String, note: String?, stickerId: String?, sexDriveLevel: Int?, isSexDriveShared: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.logMood(emoji, note, stickerId, sexDriveLevel, isSexDriveShared)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    showToast("Mood logged successfully!")
                    triggerDemoPartnerNotification("mood")
                },
                onFailure = { error ->
                    showToast("Failed to log mood: ${error.localizedMessage}")
                }
            )
        }
    }

    fun reactToMood(entryId: String, reactionEmoji: String) {
        viewModelScope.launch {
            val result = repository.reactToMood(entryId, reactionEmoji)
            result.fold(
                onSuccess = {
                    showToast("Reaction sent!")
                },
                onFailure = { error ->
                    showToast("Failed to react: ${error.localizedMessage}")
                }
            )
        }
    }

    // --- DAILY PARTNER QUIZ ACTIONS ---
    fun getDailyPrompt(): DailyPrompt {
        return repository.getDailyPromptForToday()
    }

    fun submitQuizAnswer(answer: String) {
        val todayStr = java.time.LocalDate.now().toString()
        val todayPrompt = getDailyPrompt()
        if (answer.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.submitPromptResponse(todayPrompt.promptId, todayStr, answer)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    showToast("Answer submitted! Reveal when both answer.")
                    triggerDemoPartnerNotification("quiz")
                },
                onFailure = { error ->
                    showToast("Failed to submit answer: ${error.localizedMessage}")
                }
            )
        }
    }

    fun submitMcqAnswer(questionId: String, answer: String) {
        val todayStr = java.time.LocalDate.now().toString()
        if (answer.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.submitPromptResponse(questionId, todayStr, answer)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    showToast("Answer recorded!")
                    triggerDemoPartnerNotification("quiz")
                    // Check if user finished the quiz, if so send notification
                    val activeTheme = QuizData.getThemeForToday()
                    val myResponses = promptResponsesSelf.value.filter { it.promptDate == todayStr }
                    val userFinished = activeTheme.questions.all { q ->
                        myResponses.any { it.promptId == q.questionId } || q.questionId == questionId
                    }
                    if (userFinished) {
                        sendQuizPushNotification()
                    }
                },
                onFailure = { error ->
                    showToast("Failed to record answer: ${error.localizedMessage}")
                }
            )
        }
    }

    fun sendQuizPushNotification() {
        val context = getApplication<Application>().applicationContext
        val currentUserVal = currentUser.value
        val partnerUserVal = partnerUser.value
        val coupleObj = coupleState.value ?: return

        val myName = currentUserVal?.nickname ?: "Your partner"
        val partnerName = partnerUserVal?.nickname ?: "Partner"

        val todayStr = java.time.LocalDate.now().toString()
        val activeTheme = QuizData.getThemeForToday()

        val partnerTodayResponses = promptResponsesPartner.value.filter { it.promptDate == todayStr }
        val partnerFinished = activeTheme.questions.all { q ->
            partnerTodayResponses.any { it.promptId == q.questionId }
        }

        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "partner_quiz_notifications"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Partner Quiz Notifications",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about partner's daily quiz status"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "quiz")
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Duet Quiz Sync! 📝"
        val body = if (partnerFinished) {
            "Check today's quiz answers! 🎉 $myName has completed today's quiz. Tap to see your Bond Score!"
        } else {
            "Attempt today's quiz! 📝 $myName has completed today's quiz. Finish yours to unlock responses!"
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2002, notification)
    }

    fun simulatePartnerQuizAnswers() {
        val todayStr = java.time.LocalDate.now().toString()
        val activeTheme = QuizData.getThemeForToday()
        viewModelScope.launch {
            _isLoading.value = true
            activeTheme.questions.forEach { q ->
                val randomAnswer = q.options.random()
                repository.submitPartnerPromptResponse(q.questionId, todayStr, randomAnswer)
            }
            _isLoading.value = false
            showToast("Partner quiz simulation completed!")
            sendQuizPushNotification()
        }
    }

    // --- SHARED TODO COMMENT ACTIONS ---
    fun addTodoComment(todoId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val result = repository.addTodoComment(todoId, text)
            result.fold(
                onSuccess = {
                    showToast("Comment added.")
                },
                onFailure = { error ->
                    showToast("Failed to add comment: ${error.localizedMessage}")
                }
            )
        }
    }

    // --- E2EE CHAT ACTIONS ---
    fun sendChatMessage(text: String, mediaUrl: String? = null, mediaType: String = "text") {
        viewModelScope.launch {
            val result = repository.sendChatMessage(text, mediaUrl, mediaType)
            result.fold(
                onSuccess = {
                    // Message sent, real-time listener will sync it
                    triggerDemoPartnerNotification("chat")
                },
                onFailure = { error ->
                    showToast("Failed to send encrypted message: ${error.localizedMessage}")
                }
            )
        }
    }

    fun clearChatMessages() {
        viewModelScope.launch {
            val result = repository.clearChatMessages()
            result.fold(
                onSuccess = {
                    showToast("Secure chat logs cleared.")
                },
                onFailure = { error ->
                    showToast("Failed to clear chat logs: ${error.localizedMessage}")
                }
            )
        }
    }

    fun addStory(mediaType: String, textContent: String, mediaUrl: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addStory(mediaType, textContent, mediaUrl)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    showToast("Story shared! It will auto-expire in 24 hours.")
                    triggerDemoPartnerNotification("story")
                },
                onFailure = { error ->
                    showToast("Failed to share story: ${error.localizedMessage}")
                }
            )
        }
    }

    fun triggerDemoPartnerNotification(actionType: String, detail: String = "") {
        if (!repository.isDemoMode.value) return
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            val currentUid = currentUser.value?.uid ?: ""
            val partnerName = if (currentUid == "demo_user_1") "Emily 💖" else "Alex 💙"
            when (actionType) {
                "event" -> repository.postPartnerNotification("📅 Shared Calendar", "$partnerName: I'm looking forward to '$detail'! ❤️")
                "todo" -> repository.postPartnerNotification("✏️ Couple To-Do List", "$partnerName added: Let's do '$detail' together!")
                "water" -> repository.postPartnerNotification("💧 Hydration", "$partnerName cheered: Great job hydrating! ($detail ml logged) 💧")
                "water_comment" -> repository.postPartnerNotification("💧 Hydration Cheering", "$partnerName cheered: '$detail' 🥤")
                "cycle" -> repository.postPartnerNotification("🩺 Cycle Period Log", "$partnerName logged cycle updates.")
                "mood" -> repository.postPartnerNotification("💖 Mood Reacted", "$partnerName reacted to your mood: ❤️")
                "quiz" -> repository.postPartnerNotification("❓ Daily Couple Quiz", "$partnerName completed their quiz answer! Tap to reveal results!")
                "chat" -> repository.postPartnerNotification("💬 Secure Chat", "$partnerName: That's amazing! ❤️", "chat")
                "story" -> repository.postPartnerNotification("🎥 Partner Story", "$partnerName posted a new story!")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}
