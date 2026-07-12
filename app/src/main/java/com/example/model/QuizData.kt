package com.example.model

import java.time.LocalDate

object QuizData {
    val intimacyTheme = QuizTheme(
        themeId = "intimacy",
        name = "Intimacy & Connection",
        category = "intimate",
        iconName = "Favorite",
        description = "Strengthen your emotional spark and explore physical, romantic, and emotional affection.",
        questions = listOf(
            McqQuestion(
                questionId = "intimacy_1",
                text = "What is your favorite way to reconnect after a long, stressful week?",
                options = listOf(
                    "Cuddling with a movie",
                    "Having a deep conversation",
                    "Romantic dinner date",
                    "Quiet time side-by-side"
                )
            ),
            McqQuestion(
                questionId = "intimacy_2",
                text = "Which love language makes you feel most valued and secure?",
                options = listOf(
                    "Words of Affirmation",
                    "Quality Time",
                    "Physical Touch",
                    "Acts of Service"
                )
            ),
            McqQuestion(
                questionId = "intimacy_3",
                text = "What is our biggest strength when it comes to emotional intimacy?",
                options = listOf(
                    "Fearless honest talk",
                    "Support for dreams",
                    "Physical warmth",
                    "Intuitive understanding"
                )
            ),
            McqQuestion(
                questionId = "intimacy_4",
                text = "How do you feel about the current level of physical affection we share?",
                options = listOf(
                    "Perfect as is",
                    "More physical closeness",
                    "More sweet gestures",
                    "More romantic dates"
                )
            ),
            McqQuestion(
                questionId = "intimacy_5",
                text = "What is your ideal romantic getaway for just the two of us?",
                options = listOf(
                    "Cozy mountain cabin",
                    "Bustling city trip",
                    "Relaxing beach resort",
                    "Spontaneous road trip"
                )
            )
        )
    )

    val conflictTheme = QuizTheme(
        themeId = "conflict",
        name = "Conflict & Arguments",
        category = "deep",
        iconName = "Handshake",
        description = "Understand each other's communication styles during disagreements and build healthier resolutions.",
        questions = listOf(
            McqQuestion(
                questionId = "conflict_1",
                text = "When we have a disagreement, what is your typical initial reaction?",
                options = listOf(
                    "Quiet time to cool down",
                    "Resolve it immediately",
                    "Need sweet reassurance",
                    "Focus on logical facts"
                )
            ),
            McqQuestion(
                questionId = "conflict_2",
                text = "What is one area of our conflict resolution we could improve the most?",
                options = listOf(
                    "Keeping calm tones",
                    "Not bringing up the past",
                    "Full active listening",
                    "Shorter cold-shoulders"
                )
            ),
            McqQuestion(
                questionId = "conflict_3",
                text = "After an argument, what is the best way for us to make up?",
                options = listOf(
                    "Warm, sincere hug",
                    "Sincere apology",
                    "Humor and laughter",
                    "Peaceful quiet space"
                )
            ),
            McqQuestion(
                questionId = "conflict_4",
                text = "How do we handle differences in opinion on major lifestyle choices?",
                options = listOf(
                    "Quick compromise",
                    "Passionate debate",
                    "Deference for peace",
                    "Discuss over time"
                )
            ),
            McqQuestion(
                questionId = "conflict_5",
                text = "What helps you feel most safe when we discuss difficult topics?",
                options = listOf(
                    "Calm, soft tone",
                    "Physical closeness",
                    "No interruptions",
                    "Team reassurance"
                )
            )
        )
    )

    val futureTheme = QuizTheme(
        themeId = "future",
        name = "Future & Life Goals",
        category = "practical",
        iconName = "TrendingUp",
        description = "Align your visions for life, career, finances, and long-term dreams together.",
        questions = listOf(
            McqQuestion(
                questionId = "future_1",
                text = "Where do you see us living in the next five years?",
                options = listOf(
                    "Cozy suburban house",
                    "Downtown city flat",
                    "Scenic countryside",
                    "Nomad traveling"
                )
            ),
            McqQuestion(
                questionId = "future_2",
                text = "What is our primary joint goal for the upcoming year?",
                options = listOf(
                    "Financial stability",
                    "More joint travel",
                    "Stronger daily habits",
                    "Career advancement"
                )
            ),
            McqQuestion(
                questionId = "future_3",
                text = "How do you feel our personal life goals align right now?",
                options = listOf(
                    "Perfectly aligned",
                    "Mostly aligned",
                    "Fully supportive",
                    "Need shared vision"
                )
            ),
            McqQuestion(
                questionId = "future_4",
                text = "What does a successful, fulfilling retirement look like for us?",
                options = listOf(
                    "Quiet relaxing life",
                    "World travel",
                    "Creative projects",
                    "Dream destination"
                )
            ),
            McqQuestion(
                questionId = "future_5",
                text = "What is your biggest hope or dream for our future relationship?",
                options = listOf(
                    "House of laughter",
                    "Financial freedom",
                    "Grow old together",
                    "Unbreakable team"
                )
            )
        )
    )

    val playfulnessTheme = QuizTheme(
        themeId = "playfulness",
        name = "Playfulness & Chores",
        category = "fun",
        iconName = "SentimentSatisfied",
        description = "Keep things fun, celebrate silly moments, and share daily home life evenly.",
        questions = listOf(
            McqQuestion(
                questionId = "playfulness_1",
                text = "If we had a completely free Saturday with zero chores, what should we do?",
                options = listOf(
                    "Sleep in & binge-watch",
                    "Outdoor adventure",
                    "Fun social game night",
                    "DIY home project"
                )
            ),
            McqQuestion(
                questionId = "playfulness_2",
                text = "Who is most likely to initiate a silly, playful dance in the kitchen?",
                options = listOf(
                    "Definitely you",
                    "Definitely me",
                    "Both of us equally",
                    "Neither; we're quiet"
                )
            ),
            McqQuestion(
                questionId = "playfulness_3",
                text = "What is your favorite way for us to share daily home responsibilities?",
                options = listOf(
                    "By personal preference",
                    "Side-by-side together",
                    "Alternating weekly",
                    "Spontaneous help"
                )
            ),
            McqQuestion(
                questionId = "playfulness_4",
                text = "If we were characters in a classic sitcom, which couple would we be?",
                options = listOf(
                    "Romantic and sweet",
                    "Witty best friends",
                    "Opposites attract",
                    "Chaotic & fun-loving"
                )
            ),
            McqQuestion(
                questionId = "playfulness_5",
                text = "What is your favorite meal for us to cook or order together?",
                options = listOf(
                    "Homemade pizza/pasta",
                    "Spicy takeout",
                    "Breakfast in bed",
                    "New complex recipe"
                )
            )
        )
    )

    val communicationTheme = QuizTheme(
        themeId = "communication",
        name = "Communication & Trust",
        category = "deep",
        iconName = "Chat",
        description = "Enhance active listening, open sharing, trust, and mutual security.",
        questions = listOf(
            McqQuestion(
                questionId = "communication_1",
                text = "When I am having a rough day, how can I best support you?",
                options = listOf(
                    "Just listen and vent",
                    "Hugs & reassurance",
                    "Brainstorm solutions",
                    "Quiet alone space"
                )
            ),
            McqQuestion(
                questionId = "communication_2",
                text = "How do you feel we do at expressing daily appreciation for each other?",
                options = listOf(
                    "Great constantly",
                    "Could say thanks more",
                    "Actions over words",
                    "Need daily check-ins"
                )
            ),
            McqQuestion(
                questionId = "communication_3",
                text = "What is the key foundation of trust in our relationship?",
                options = listOf(
                    "Transparent honesty",
                    "Mutual protection",
                    "Keeping promises",
                    "Respecting privacy"
                )
            ),
            McqQuestion(
                questionId = "communication_4",
                text = "How comfortable do you feel sharing your vulnerable worries or insecurities?",
                options = listOf(
                    "100% comfortable",
                    "Open, takes a bit",
                    "Self-process first",
                    "Working to open up"
                )
            ),
            McqQuestion(
                questionId = "communication_5",
                text = "What is our favorite way to check in on our relationship's emotional health?",
                options = listOf(
                    "Bedtime pillow talk",
                    "Structured check-ins",
                    "Chats during walks",
                    "Answering quiz prompts"
                )
            )
        )
    )

    val allThemes = listOf(
        intimacyTheme,
        conflictTheme,
        futureTheme,
        playfulnessTheme,
        communicationTheme
    )

    fun getThemeForToday(): QuizTheme {
        val dayOfYear = LocalDate.now().dayOfYear
        val index = dayOfYear % allThemes.size
        return allThemes[index]
    }
}
