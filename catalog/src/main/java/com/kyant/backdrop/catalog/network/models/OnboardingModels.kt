package com.kyant.backdrop.catalog.network.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

// ==================== Onboarding API Models ====================

@Serializable
data class OnboardingData(
    val id: String = "",
    val userId: String = "",
    val isCompleted: Boolean = false,
    val completedAt: String? = null,
    val currentStep: Int = 0,
    val primaryGoal: String? = null,
    val secondaryGoals: List<String> = emptyList(),
    val wantToLearn: List<String> = emptyList(),
    val canTeach: List<String> = emptyList(),
    val lookingFor: List<String> = emptyList(),
    val availability: String? = null,
    val hoursPerWeek: Int? = null,
    val communicationPref: String? = null
)

@Serializable
data class OnboardingResponse(
    val onboarding: OnboardingData
)

@Serializable
data class OnboardingStepResponse(
    val onboarding: OnboardingData,
    val nextStep: Int
)

@Serializable
data class OnboardingCompleteResponse(
    val onboarding: OnboardingData,
    val message: String
)

@Serializable
data class OnboardingStepRequest(
    val step: Int,
    val data: Map<String, @Serializable(with = OnboardingValueSerializer::class) Any?>
)

// Custom serializer for mixed values in onboarding data
object OnboardingValueSerializer : KSerializer<Any?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any")
    
    override fun serialize(encoder: Encoder, value: Any?) {
        val jsonEncoder = encoder as JsonEncoder
        val jsonElement = when (value) {
            null -> JsonNull
            is String -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is List<*> -> JsonArray(value.map { 
                when (it) {
                    is String -> JsonPrimitive(it)
                    else -> JsonNull
                }
            })
            else -> JsonPrimitive(value.toString())
        }
        jsonEncoder.encodeJsonElement(jsonElement)
    }
    
    override fun deserialize(decoder: Decoder): Any? {
        val jsonDecoder = decoder as JsonDecoder
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonNull -> null
            is JsonPrimitive -> element.content
            is JsonArray -> element.map { (it as? JsonPrimitive)?.content ?: "" }
            else -> null
        }
    }
}

// ==================== Onboarding Matches ====================

@Serializable
data class OnboardingMatch(
    val user: MatchUser,
    val matchPercentage: Int,
    val reasons: List<String> = emptyList(),
    val sharedInterests: List<String> = emptyList()
)

@Serializable
data class MatchUser(
    val id: String,
    val name: String,
    val username: String? = null,
    val profileImage: String? = null,
    val headline: String? = null,
    val college: String? = null
)

@Serializable
data class OnboardingMatchesResponse(
    val matches: List<OnboardingMatch>
)

// ==================== Predefined Options ====================

data class GoalOption(
    val id: String,
    val label: String,
    val emoji: String
)

data class LookingForOption(
    val id: String,
    val label: String
)

data class InterestGroup(
    val label: String,
    val items: List<String>
)

// Goal option with Material Icon (for glass theme)
data class GoalOptionWithIcon(
    val id: String,
    val label: String,
    val icon: ImageVector
)

// Predefined goals matching web
val GOALS = listOf(
    GoalOption("learn_coding", "Coding & Tech", "💻"),
    GoalOption("start_business", "Business & Startups", "🚀"),
    GoalOption("design", "Design & Creative", "🎨"),
    GoalOption("get_internship", "Career & Internships", "💼"),
    GoalOption("competitive_programming", "Competitive Coding", "🏆"),
    GoalOption("ai_ml", "AI & Data Science", "🤖"),
    GoalOption("content_creation", "Content Creation", "🎬"),
    GoalOption("research", "Research & Academics", "🔬"),
    GoalOption("sports_fitness", "Sports & Fitness", "⚽"),
    GoalOption("music_arts", "Music & Arts", "🎵"),
    GoalOption("photography", "Photography & Film", "📸"),
    GoalOption("freelance", "Freelancing", "💰")
)

// Goals with Material Icons (for glass theme)
val GOALS_WITH_ICONS = listOf(
    GoalOptionWithIcon("learn_coding", "Coding & Tech", Icons.Filled.PhoneAndroid),
    GoalOptionWithIcon("start_business", "Business & Startups", Icons.Filled.TrendingUp),
    GoalOptionWithIcon("design", "Design & Creative", Icons.Filled.Palette),
    GoalOptionWithIcon("get_internship", "Career & Internships", Icons.Filled.Work),
    GoalOptionWithIcon("competitive_programming", "Competitive Coding", Icons.Filled.EmojiEvents),
    GoalOptionWithIcon("ai_ml", "AI & Data Science", Icons.Filled.Psychology),
    GoalOptionWithIcon("content_creation", "Content Creation", Icons.Filled.VideoLibrary),
    GoalOptionWithIcon("research", "Research & Academics", Icons.Filled.School),
    GoalOptionWithIcon("sports_fitness", "Sports & Fitness", Icons.Filled.DirectionsRun),
    GoalOptionWithIcon("music_arts", "Music & Arts", Icons.Filled.MusicNote),
    GoalOptionWithIcon("photography", "Photography & Film", Icons.Filled.PhotoCamera),
    GoalOptionWithIcon("freelance", "Freelancing", Icons.Filled.MonetizationOn)
)

// Predefined looking for options matching web
val LOOKING_FOR = listOf(
    LookingForOption("study_partner", "Study Partner"),
    LookingForOption("project_collaborator", "Project Mate"),
    LookingForOption("mentor", "Mentor"),
    LookingForOption("co_founder", "Co-Founder"),
    LookingForOption("accountability_buddy", "Accountability Buddy"),
    LookingForOption("hackathon_team", "Hackathon Teammate")
)

// Predefined interest groups matching web
val INTEREST_GROUPS = listOf(
    InterestGroup(
        "Tech & Engineering",
        listOf("Python", "JavaScript", "React", "AI/ML", "Data Science", "Cybersecurity", "Cloud/DevOps", "Mobile Dev", "Blockchain", "Game Dev", "DSA", "Web Dev")
    ),
    InterestGroup(
        "Business & Finance",
        listOf("Startups", "Marketing", "Finance", "Investing", "E-Commerce", "Product Management", "Sales", "Accounting")
    ),
    InterestGroup(
        "Creative & Design",
        listOf("UI/UX Design", "Graphic Design", "Video Editing", "Photography", "Animation", "Illustration", "Figma", "3D Modeling")
    ),
    InterestGroup(
        "Content & Media",
        listOf("YouTube", "Blogging", "Podcasting", "Social Media", "Copywriting", "Filmmaking", "Journalism", "Public Speaking")
    ),
    InterestGroup(
        "Music & Arts",
        listOf("Guitar", "Singing", "Piano", "Music Production", "Dancing", "Painting", "Poetry", "Theatre")
    ),
    InterestGroup(
        "Sports & Fitness",
        listOf("Cricket", "Football", "Basketball", "Badminton", "Swimming", "Gym/Fitness", "Yoga", "Running", "Chess", "Table Tennis")
    ),
    InterestGroup(
        "Life & Growth",
        listOf("Leadership", "Communication", "Time Management", "Networking", "Mental Health", "Reading", "Volunteering", "Travel")
    ),
    InterestGroup(
        "Academic",
        listOf("Research", "Competitive Exams", "GATE", "GRE", "CAT", "Interview Prep", "Resume Building", "Placements")
    )
)
