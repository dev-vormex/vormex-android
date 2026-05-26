package com.kyant.backdrop.catalog.linkedin

import com.kyant.backdrop.catalog.network.models.NearbyUser
import com.kyant.backdrop.catalog.network.models.PersonInfo
import com.kyant.backdrop.catalog.network.models.SmartMatch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

data class LearnedConnectionSignals(
    val skillWeights: Map<String, Int> = emptyMap(),
    val interestWeights: Map<String, Int> = emptyMap(),
    val collegeWeights: Map<String, Int> = emptyMap(),
    val branchWeights: Map<String, Int> = emptyMap()
) {
    val hasSignals: Boolean
        get() = skillWeights.isNotEmpty() ||
            interestWeights.isNotEmpty() ||
            collegeWeights.isNotEmpty() ||
            branchWeights.isNotEmpty()
}

data class FindPeopleRankingProfile(
    val skillWeights: Map<String, Int> = emptyMap(),
    val interestWeights: Map<String, Int> = emptyMap(),
    val college: String? = null,
    val branch: String? = null,
    val learnedSignals: LearnedConnectionSignals = LearnedConnectionSignals()
) {
    companion object {
        fun fromRaw(
            skills: List<String>,
            interests: List<String>,
            college: String?,
            branch: String?,
            learnedSignals: LearnedConnectionSignals = LearnedConnectionSignals()
        ): FindPeopleRankingProfile {
            return FindPeopleRankingProfile(
                skillWeights = FindPeopleRanker.tokenWeights(skills, weight = 1),
                interestWeights = FindPeopleRanker.tokenWeights(interests, weight = 1),
                college = FindPeopleRanker.normalizeToken(college),
                branch = FindPeopleRanker.normalizeToken(branch),
                learnedSignals = learnedSignals
            )
        }
    }
}

object FindPeopleRanker {
    fun rankPeople(
        people: List<PersonInfo>,
        profile: FindPeopleRankingProfile
    ): List<PersonInfo> {
        return people
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<PersonInfo> { personScore(it, profile) }
                    .thenBy { it.name.orEmpty().lowercase(Locale.getDefault()) }
            )
    }

    fun rankNearbyPeople(
        people: List<NearbyUser>,
        profile: FindPeopleRankingProfile
    ): List<NearbyUser> {
        return people
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<NearbyUser> { nearbyScore(it, profile) }
                    .thenBy { it.distance }
                    .thenBy { it.name.orEmpty().lowercase(Locale.getDefault()) }
            )
    }

    fun rankSmartMatches(
        matches: List<SmartMatch>,
        profile: FindPeopleRankingProfile
    ): List<SmartMatch> {
        return matches
            .distinctBy { it.user.id }
            .sortedWith(
                compareByDescending<SmartMatch> { smartMatchScore(it, profile) }
                    .thenBy { it.user.name.orEmpty().lowercase(Locale.getDefault()) }
            )
    }

    fun buildSignalsFromPerson(person: PersonInfo): LearnedConnectionSignals {
        return LearnedConnectionSignals(
            skillWeights = tokenWeights(person.skills, weight = 1),
            interestWeights = tokenWeights(person.interests, weight = 1),
            collegeWeights = tokenWeights(listOfNotNull(person.college), weight = 1),
            branchWeights = tokenWeights(listOfNotNull(person.branch), weight = 1)
        )
    }

    fun mergeSignals(
        existing: LearnedConnectionSignals,
        addition: LearnedConnectionSignals,
        maxEntriesPerType: Int = 48,
        maxWeight: Int = 12
    ): LearnedConnectionSignals {
        return LearnedConnectionSignals(
            skillWeights = mergeWeights(existing.skillWeights, addition.skillWeights, maxEntriesPerType, maxWeight),
            interestWeights = mergeWeights(existing.interestWeights, addition.interestWeights, maxEntriesPerType, maxWeight),
            collegeWeights = mergeWeights(existing.collegeWeights, addition.collegeWeights, maxEntriesPerType, maxWeight),
            branchWeights = mergeWeights(existing.branchWeights, addition.branchWeights, maxEntriesPerType, maxWeight)
        )
    }

    fun tokenWeights(values: List<String>, weight: Int): Map<String, Int> {
        return values
            .mapNotNull { normalizeToken(it) }
            .distinct()
            .associateWith { weight.coerceAtLeast(1) }
    }

    fun normalizeToken(value: String?): String? {
        val normalized = value
            ?.trim()
            ?.lowercase(Locale.getDefault())
            ?.replace(Regex("\\s+"), " ")
            ?.takeIf { it.isNotBlank() }

        return normalized
    }

    private fun personScore(person: PersonInfo, profile: FindPeopleRankingProfile): Int {
        val skillTokens = tokenSet(person.skills)
        val interestTokens = tokenSet(person.interests)
        val collegeToken = normalizeToken(person.college)
        val branchToken = normalizeToken(person.branch)
        val searchableText = listOfNotNull(person.headline, person.bio, person.name)
            .joinToString(" ")
            .lowercase(Locale.getDefault())

        var score = connectionPriority(person.connectionStatus) * 10_000
        score += weightedOverlap(skillTokens, profile.skillWeights, perWeight = 280, cap = 1_400)
        score += weightedOverlap(interestTokens, profile.interestWeights, perWeight = 230, cap = 1_150)
        score += weightedOverlap(skillTokens, profile.learnedSignals.skillWeights, perWeight = 95, cap = 950)
        score += weightedOverlap(interestTokens, profile.learnedSignals.interestWeights, perWeight = 80, cap = 800)
        score += textAffinity(searchableText, profile.skillWeights.keys, perMatch = 36, cap = 216)
        score += textAffinity(searchableText, profile.interestWeights.keys, perMatch = 30, cap = 180)
        score += exactTokenBonus(collegeToken, profile.college, bonus = 520)
        score += exactTokenBonus(branchToken, profile.branch, bonus = 320)
        score += learnedExactTokenBonus(collegeToken, profile.learnedSignals.collegeWeights, perWeight = 140, cap = 560)
        score += learnedExactTokenBonus(branchToken, profile.learnedSignals.branchWeights, perWeight = 110, cap = 440)
        score += min(person.mutualConnections, 8) * 115
        score += if (person.isOnline) 90 else 0
        score += profileStrength(person)
        score += stableExplorationBoost(person.id)

        return score
    }

    private fun nearbyScore(person: NearbyUser, profile: FindPeopleRankingProfile): Int {
        val skillTokens = tokenSet(person.skills)
        val interestTokens = tokenSet(person.interests)
        val cityToken = normalizeToken(person.location?.city)
        val searchableText = listOfNotNull(person.headline, person.name, person.location?.city)
            .joinToString(" ")
            .lowercase(Locale.getDefault())
        val distancePenalty = min((person.distance * 6).toInt(), 360)

        var score = 0
        score += weightedOverlap(skillTokens, profile.skillWeights, perWeight = 280, cap = 1_400)
        score += weightedOverlap(interestTokens, profile.interestWeights, perWeight = 230, cap = 1_150)
        score += weightedOverlap(skillTokens, profile.learnedSignals.skillWeights, perWeight = 95, cap = 950)
        score += weightedOverlap(interestTokens, profile.learnedSignals.interestWeights, perWeight = 80, cap = 800)
        score += learnedExactTokenBonus(cityToken, profile.learnedSignals.collegeWeights, perWeight = 50, cap = 150)
        score += textAffinity(searchableText, profile.skillWeights.keys, perMatch = 36, cap = 216)
        score += if (person.isOnline) 110 else 0
        score += (person.skills.size + person.interests.size) * 16
        score += stableExplorationBoost(person.id)
        score -= distancePenalty

        return score
    }

    private fun smartMatchScore(match: SmartMatch, profile: FindPeopleRankingProfile): Int {
        val user = match.user
        val skillTokens = tokenSet(user.skills)
        val interestTokens = tokenSet(user.interests)
        val collegeToken = normalizeToken(user.college)
        val branchToken = normalizeToken(user.branch)
        val searchableText = listOfNotNull(user.headline, user.bio, user.name)
            .joinToString(" ")
            .lowercase(Locale.getDefault())

        var score = match.matchPercentage * 1_000 + match.score.toInt() * 10
        score += weightedOverlap(skillTokens, profile.skillWeights, perWeight = 260, cap = 1_300)
        score += weightedOverlap(interestTokens, profile.interestWeights, perWeight = 220, cap = 1_100)
        score += weightedOverlap(skillTokens, profile.learnedSignals.skillWeights, perWeight = 90, cap = 900)
        score += weightedOverlap(interestTokens, profile.learnedSignals.interestWeights, perWeight = 76, cap = 760)
        score += exactTokenBonus(collegeToken, profile.college, bonus = 460)
        score += exactTokenBonus(branchToken, profile.branch, bonus = 280)
        score += learnedExactTokenBonus(collegeToken, profile.learnedSignals.collegeWeights, perWeight = 130, cap = 520)
        score += learnedExactTokenBonus(branchToken, profile.learnedSignals.branchWeights, perWeight = 100, cap = 400)
        score += textAffinity(searchableText, profile.skillWeights.keys, perMatch = 32, cap = 192)
        score += (user.stats?.xp ?: 0) * 2
        score += user.skills.size * 22
        score += user.interests.size * 18
        score += match.tags.size * 18
        score += match.reasons.size * 14
        score += if (user.githubConnected) 40 else 0
        score += if (!user.headline.isNullOrBlank()) 25 else 0
        score += if (!user.bio.isNullOrBlank()) 15 else 0
        score += stableExplorationBoost(user.id)

        return score
    }

    private fun tokenSet(values: List<String>): Set<String> {
        return values.mapNotNull { normalizeToken(it) }.toSet()
    }

    private fun weightedOverlap(
        tokens: Set<String>,
        weights: Map<String, Int>,
        perWeight: Int,
        cap: Int
    ): Int {
        if (tokens.isEmpty() || weights.isEmpty()) return 0
        val score = tokens.sumOf { token -> (weights[token] ?: 0) * perWeight }
        return min(score, cap)
    }

    private fun textAffinity(
        text: String,
        tokens: Collection<String>,
        perMatch: Int,
        cap: Int
    ): Int {
        if (text.isBlank() || tokens.isEmpty()) return 0
        val score = tokens.count { token -> token.length >= 3 && text.contains(token) } * perMatch
        return min(score, cap)
    }

    private fun exactTokenBonus(candidate: String?, target: String?, bonus: Int): Int {
        if (candidate.isNullOrBlank() || target.isNullOrBlank()) return 0
        return if (candidate == target) bonus else 0
    }

    private fun learnedExactTokenBonus(
        candidate: String?,
        weights: Map<String, Int>,
        perWeight: Int,
        cap: Int
    ): Int {
        if (candidate.isNullOrBlank() || weights.isEmpty()) return 0
        return min((weights[candidate] ?: 0) * perWeight, cap)
    }

    private fun connectionPriority(status: String): Int {
        return when (status) {
            "none" -> 4
            "pending_received" -> 3
            "pending_sent" -> 2
            "connected" -> 1
            else -> 0
        }
    }

    private fun profileStrength(person: PersonInfo): Int {
        return person.skills.size * 14 +
            person.interests.size * 12 +
            person.mutualConnections * 10 +
            if (!person.headline.isNullOrBlank()) 60 else 0 +
            if (!person.bio.isNullOrBlank()) 45 else 0 +
            if (!person.college.isNullOrBlank()) 35 else 0 +
            if (!person.branch.isNullOrBlank()) 25 else 0
    }

    private fun stableExplorationBoost(id: String): Int {
        return abs(id.hashCode() % 73)
    }

    private fun mergeWeights(
        existing: Map<String, Int>,
        addition: Map<String, Int>,
        maxEntries: Int,
        maxWeight: Int
    ): Map<String, Int> {
        val merged = existing.toMutableMap()
        addition.forEach { (token, weight) ->
            if (token.isBlank()) return@forEach
            merged[token] = ((merged[token] ?: 0) + weight).coerceAtMost(maxWeight)
        }
        return merged.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { it.key }
            )
            .take(maxEntries)
            .associate { it.key to it.value }
    }
}
