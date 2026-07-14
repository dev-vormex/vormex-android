package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import com.kyant.backdrop.catalog.network.models.FullProfileResponse
import com.kyant.backdrop.catalog.network.models.Post
import com.kyant.backdrop.catalog.network.models.ProfileConnectionItem
import com.kyant.backdrop.catalog.network.models.ProfileFollowingItem
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt

data class HomeFeedRecommendationProfile(
    val currentUserId: String? = null,
    val skillWeights: Map<String, Int> = emptyMap(),
    val interestWeights: Map<String, Int> = emptyMap(),
    val connectionAuthorIds: Set<String> = emptySet(),
    val followingAuthorIds: Set<String> = emptySet(),
    val recentProfileWeights: Map<String, Int> = emptyMap()
) {
    val hasPersonalSignals: Boolean
        get() = skillWeights.isNotEmpty() ||
            interestWeights.isNotEmpty() ||
            connectionAuthorIds.isNotEmpty() ||
            followingAuthorIds.isNotEmpty() ||
            recentProfileWeights.isNotEmpty()

    fun withCurrentUserId(userId: String?): HomeFeedRecommendationProfile =
        if (currentUserId != null || userId == null) this else copy(currentUserId = userId)
}

object HomeFeedRecommendationProfileBuilder {
    fun from(
        currentUserId: String?,
        profile: FullProfileResponse?,
        connections: List<ProfileConnectionItem>,
        following: List<ProfileFollowingItem>,
        recentProfileWeights: Map<String, Int>
    ): HomeFeedRecommendationProfile {
        val profileSkills = profile?.skills.orEmpty().map { it.skill.name }
        val experienceSkills = profile?.experiences.orEmpty().flatMap { it.skills }
        val projectSkills = profile?.projects.orEmpty().flatMap { it.techStack }
        val skillHints = profileSkills + experienceSkills + projectSkills

        val interestHints = profile?.user?.interests.orEmpty() +
            listOfNotNull(profile?.user?.branch, profile?.user?.degree, profile?.user?.college)

        return HomeFeedRecommendationProfile(
            currentUserId = currentUserId,
            skillWeights = tokenWeights(skillHints, baseWeight = 2),
            interestWeights = tokenWeights(interestHints, baseWeight = 1),
            connectionAuthorIds = connections.map { it.user.id }.toSet(),
            followingAuthorIds = following.map { it.user.id }.toSet(),
            recentProfileWeights = recentProfileWeights
        )
    }

    private fun tokenWeights(values: List<String>, baseWeight: Int): Map<String, Int> {
        val weights = linkedMapOf<String, Int>()
        values.forEach { value ->
            val token = normalizeToken(value) ?: return@forEach
            weights[token] = ((weights[token] ?: 0) + baseWeight).coerceAtMost(8)
        }
        return weights
    }
}

object HomeFeedRanker {
    fun rankPosts(
        posts: List<Post>,
        profile: HomeFeedRecommendationProfile,
        nowMillis: Long = System.currentTimeMillis()
    ): List<Post> {
        if (posts.size <= 1) return posts.distinctBy { it.id }

        val ranked = posts
            .distinctBy { it.id }
            .mapIndexed { index, post ->
                RankedPost(
                    post = post,
                    originalIndex = index,
                    createdAtMillis = parseCreatedAtMillis(post.createdAt, nowMillis),
                    score = scorePost(post, profile, nowMillis)
                )
            }
            .sortedWith(
                compareByDescending<RankedPost> { it.score }
                    .thenByDescending { it.createdAtMillis }
                    .thenBy { it.originalIndex }
            )

        return spreadAuthors(ranked).map { it.post }
    }

    private fun scorePost(
        post: Post,
        profile: HomeFeedRecommendationProfile,
        nowMillis: Long
    ): Int {
        val searchText = postSearchText(post)
        val tagTokens = post.articleTags.mapNotNull { normalizeToken(it) }.toSet()
        val hoursOld = hoursOld(post.createdAt, nowMillis)
        val recencyScore = (exp(-0.035 * hoursOld) * 1_350).roundToInt()
        val engagement = post.likesCount + post.commentsCount * 3 + post.sharesCount * 5 + post.savesCount * 4

        var score = recencyScore
        score += min(engagement * 18, 1_900)
        score += if (post.isLiked) 360 else 0
        score += if (post.isSaved) 520 else 0
        score += relationshipScore(post, profile)
        score += affinityScore(searchText, profile.skillWeights, perWeight = 1_180, cap = 4_720)
        score += affinityScore(searchText, profile.interestWeights, perWeight = 860, cap = 3_440)
        score += exactTagScore(tagTokens, profile.skillWeights, perWeight = 1_360, cap = 4_080)
        score += exactTagScore(tagTokens, profile.interestWeights, perWeight = 920, cap = 2_760)
        score += contentCompletenessScore(post)
        score += stableExplorationBoost(post.id)

        return score
    }

    private fun relationshipScore(post: Post, profile: HomeFeedRecommendationProfile): Int {
        var score = 0
        val authorId = post.authorId
        if (authorId == profile.currentUserId) score += 900
        if (profile.followingAuthorIds.contains(authorId)) score += 2_600
        if (profile.connectionAuthorIds.contains(authorId)) score += 2_050
        score += (profile.recentProfileWeights[authorId] ?: 0) * 240
        if (post.visibility.equals("CONNECTIONS", ignoreCase = true)) score += 260
        return score
    }

    private fun affinityScore(
        text: String,
        weights: Map<String, Int>,
        perWeight: Int,
        cap: Int
    ): Int {
        if (text.isBlank() || weights.isEmpty()) return 0
        val score = weights.entries.sumOf { (token, weight) ->
            if (containsToken(text, token)) weight * perWeight else 0
        }
        return min(score, cap)
    }

    private fun exactTagScore(
        tags: Set<String>,
        weights: Map<String, Int>,
        perWeight: Int,
        cap: Int
    ): Int {
        if (tags.isEmpty() || weights.isEmpty()) return 0
        val score = tags.sumOf { token -> (weights[token] ?: 0) * perWeight }
        return min(score, cap)
    }

    private fun contentCompletenessScore(post: Post): Int {
        var score = 0
        if (!post.content.isNullOrBlank()) score += 90
        if (post.mediaUrls.isNotEmpty()) score += 120
        if (!post.videoUrl.isNullOrBlank() || !post.defaultVideoId.isNullOrBlank()) score += 150
        if (!post.documentUrl.isNullOrBlank()) score += 130
        if (!post.linkUrl.isNullOrBlank()) score += 80
        if (post.pollOptions.isNotEmpty()) score += 110
        if (!post.articleTitle.isNullOrBlank()) score += 120
        return score
    }

    private fun postSearchText(post: Post): String {
        return listOfNotNull(
            post.content,
            post.author.name,
            post.author.headline,
            post.articleTitle,
            post.articleTags.joinToString(" "),
            post.linkTitle,
            post.linkDescription,
            post.linkDomain,
            post.documentName,
            post.celebrationType,
            post.celebrationBadge
        )
            .joinToString(" ")
            .lowercase(Locale.getDefault())
    }

    private fun spreadAuthors(posts: List<RankedPost>): List<RankedPost> {
        val remaining = posts.toMutableList()
        val result = ArrayList<RankedPost>(posts.size)
        var lastAuthorId: String? = null

        while (remaining.isNotEmpty()) {
            val nextIndex = remaining.indexOfFirst { it.post.authorId != lastAuthorId }
                .takeIf { it >= 0 }
                ?: 0
            val next = remaining.removeAt(nextIndex)
            result.add(next)
            lastAuthorId = next.post.authorId
        }

        return result
    }

    private fun hoursOld(createdAt: String, nowMillis: Long): Double {
        val createdAtMillis = parseCreatedAtMillis(createdAt, nowMillis)
        val ageMillis = (nowMillis - createdAtMillis).coerceAtLeast(0L)
        return ageMillis / 3_600_000.0
    }

    private fun parseCreatedAtMillis(createdAt: String, fallbackMillis: Long): Long {
        val raw = createdAt.trim()
        if (raw.isBlank()) return fallbackMillis
        runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()?.let { return it }
        runCatching { OffsetDateTime.parse(raw).toInstant().toEpochMilli() }.getOrNull()?.let { return it }
        return fallbackMillis
    }

    private fun containsToken(text: String, token: String): Boolean {
        if (token.isBlank()) return false
        if (token.length <= 2) {
            val escaped = Regex.escape(token)
            return Regex("(?<![a-z0-9])$escaped(?![a-z0-9])").containsMatchIn(text)
        }
        return text.contains(token)
    }

    private fun stableExplorationBoost(id: String): Int = abs(id.hashCode() % 97)

    private data class RankedPost(
        val post: Post,
        val originalIndex: Int,
        val createdAtMillis: Long,
        val score: Int
    )
}

object HomeFeedInteractionMemory {
    private const val PrefsName = "vormex_home_feed_recommendations"
    private const val RecentProfilesKey = "recent_profile_ids"
    private const val MaxRecentProfiles = 24

    fun recordProfileVisit(context: Context, profileUserId: String, currentUserId: String?) {
        if (profileUserId.isBlank() || profileUserId == currentUserId) return
        val prefs = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        val updated = listOf(profileUserId) + recentProfileIds(context).filterNot { it == profileUserId }
        prefs.edit()
            .putString(RecentProfilesKey, updated.take(MaxRecentProfiles).joinToString(","))
            .apply()
    }

    fun recentProfileWeights(context: Context): Map<String, Int> {
        return recentProfileIds(context)
            .take(MaxRecentProfiles)
            .mapIndexed { index, userId -> userId to (MaxRecentProfiles - index).coerceAtLeast(1) }
            .toMap()
    }

    private fun recentProfileIds(context: Context): List<String> {
        return context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
            .getString(RecentProfilesKey, null)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
    }
}

private fun normalizeToken(value: String?): String? {
    return value
        ?.trim()
        ?.lowercase(Locale.getDefault())
        ?.replace(Regex("\\s+"), " ")
        ?.takeIf { it.isNotBlank() }
}
