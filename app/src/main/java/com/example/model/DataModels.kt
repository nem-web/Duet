package com.example.model

import java.util.UUID

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val nickname: String = "",
    val photoUrl: String? = null,
    val dob: String = "", // YYYY-MM-DD
    val gender: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Couple(
    val coupleId: String = "",
    val user1Uid: String = "",
    val user2Uid: String = "",
    val pairCode: String = "",
    val relationshipStartDate: String? = null, // YYYY-MM-DD
    val createdAt: Long = System.currentTimeMillis()
)

data class CalendarEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val coupleId: String = "",
    val title: String = "",
    val date: String = "", // YYYY-MM-DD
    val type: String = "moment", // "moment" or "reminder"
    val note: String = "",
    val photoUrl: String? = null,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class TodoItem(
    val todoId: String = UUID.randomUUID().toString(),
    val title: String = "",
    val isCompleted: Boolean = false,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// PART A — Cycle Tracking Models
data class CycleLog(
    val cycleId: String = "",
    val coupleId: String = "",
    val userId: String = "",
    val startDate: String = "", // YYYY-MM-DD
    val endDate: String? = null, // YYYY-MM-DD (nullable until logged)
    val periodLength: Int = 5,
    val symptoms: List<String> = emptyList(), // e.g. ["Cramps", "Headache", "Mood swings"]
    val flowIntensity: String = "Medium", // "Light", "Medium", "Heavy"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class CycleConfig(
    val userId: String = "",
    val avgCycleLength: Int = 28,
    val avgPeriodLength: Int = 5,
    val isPrivacyEnabled: Boolean = false, // true = hide from partner, false = visible/shared
    val hideSymptoms: Boolean = false, // true = hide symptoms from partner view
    val lastUpdated: Long = System.currentTimeMillis()
)

data class CyclePrediction(
    val predictedStartDate: String = "", // YYYY-MM-DD
    val predictedEndDate: String = "", // YYYY-MM-DD
    val ovulationDate: String = "", // YYYY-MM-DD
    val fertileWindowStart: String = "", // YYYY-MM-DD
    val fertileWindowEnd: String = "" // YYYY-MM-DD
)

// PART B — Water Intake Tracking Models
data class WaterLog(
    val logId: String = "",
    val userId: String = "",
    val date: String = "", // YYYY-MM-DD
    val amountMl: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class WaterGoal(
    val userId: String = "",
    val dailyGoalMl: Int = 2000,
    val isManual: Boolean = false,
    val age: Int = 25,
    val weightKg: Int = 70
)

data class WaterComment(
    val commentId: String = "",
    val userId: String = "", // writer of the comment
    val date: String = "", // YYYY-MM-DD (linked to that day's log)
    val text: String = "",
    val emojiReaction: String = "", // e.g. "👍", "❤️"
    val timestamp: Long = System.currentTimeMillis()
)

// PART C — Mood & Emotion Sharing Models
data class MoodEntry(
    val entryId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val date: String = "", // YYYY-MM-DD
    val moodEmoji: String = "",
    val note: String? = null,
    val stickerId: String? = null, // e.g., "sparkles", "cloud", "heart"
    val sexDriveLevel: Int? = null, // 1-5, private/nullable
    val isSexDriveShared: Boolean = false, // explicit per-field toggle
    val partnerReaction: String? = null, // emoji reaction from partner
    val timestamp: Long = System.currentTimeMillis()
)

// PART D — Daily Partner Quiz Models
data class DailyPrompt(
    val promptId: String = "",
    val questionText: String = "",
    val category: String = "fun" // fun, deep, intimate, practical
)

data class PromptResponse(
    val responseId: String = UUID.randomUUID().toString(),
    val coupleId: String = "",
    val promptId: String = "", // to identify the specific MCQ question in the daily set
    val promptDate: String = "", // YYYY-MM-DD
    val userId: String = "",
    val answer: String = "",
    val submittedAt: Long = System.currentTimeMillis()
)

data class McqQuestion(
    val questionId: String = "",
    val text: String = "",
    val options: List<String> = emptyList()
)

data class QuizTheme(
    val themeId: String = "",
    val name: String = "",
    val category: String = "",
    val iconName: String = "",
    val description: String = "",
    val questions: List<McqQuestion> = emptyList()
)

// PART E — Shared Todo Comments Models
data class TodoComment(
    val commentId: String = UUID.randomUUID().toString(),
    val todoId: String = "",
    val userId: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class EncryptedMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val coupleId: String = "",
    val senderId: String = "",
    val encryptedText: String = "",
    val encryptedMediaUrl: String? = null,
    val mediaType: String = "text", // "text", "image", "voice"
    val timestamp: Long = System.currentTimeMillis()
)

data class Story(
    val storyId: String = UUID.randomUUID().toString(),
    val coupleId: String = "",
    val userId: String = "",
    val userName: String = "",
    val mediaType: String = "text", // "text", "image", "video"
    val textContent: String = "",
    val mediaUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)


