package com.kyant.backdrop.catalog.linkedin

import com.kyant.backdrop.catalog.network.models.PersonInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FindPeopleRankerTest {
    @Test
    fun `shared skills and interests outrank generic complete profiles`() {
        val profile = FindPeopleRankingProfile.fromRaw(
            skills = listOf("Android", "Kotlin"),
            interests = listOf("AI", "Startups"),
            college = "Vormex University",
            branch = "CSE"
        )

        val genericCompleteProfile = person(
            id = "old-complete",
            name = "Old Complete",
            headline = "Student leader",
            bio = "Open to opportunities",
            skills = listOf("Marketing", "Writing", "Public Speaking", "Sales"),
            interests = listOf("Music", "Sports"),
            mutualConnections = 4,
            isOnline = true
        )
        val relevantProfile = person(
            id = "relevant",
            name = "Relevant Match",
            skills = listOf("Kotlin", "Jetpack Compose"),
            interests = listOf("AI"),
            college = "Vormex University",
            branch = "CSE"
        )

        val ranked = FindPeopleRanker.rankPeople(
            listOf(genericCompleteProfile, relevantProfile),
            profile
        )

        assertEquals("relevant", ranked.first().id)
    }

    @Test
    fun `learned request signals promote similar people even without profile skills`() {
        val firstRequest = person(
            id = "sent-request",
            skills = listOf("Flutter", "Firebase"),
            interests = listOf("Hackathons"),
            college = "Vormex University"
        )
        val learned = FindPeopleRanker.mergeSignals(
            existing = LearnedConnectionSignals(),
            addition = FindPeopleRanker.buildSignalsFromPerson(firstRequest)
        )
        val profile = FindPeopleRankingProfile(learnedSignals = learned)

        val similar = person(
            id = "similar",
            name = "Similar Builder",
            skills = listOf("Flutter"),
            interests = listOf("Hackathons")
        )
        val unrelated = person(
            id = "unrelated",
            name = "Unrelated Builder",
            skills = listOf("Sales"),
            interests = listOf("Drama")
        )

        val ranked = FindPeopleRanker.rankPeople(listOf(unrelated, similar), profile)

        assertEquals("similar", ranked.first().id)
    }

    @Test
    fun `connection status still keeps actionable people above already connected people`() {
        val profile = FindPeopleRankingProfile.fromRaw(
            skills = listOf("Android"),
            interests = emptyList(),
            college = null,
            branch = null
        )

        val connectedRelevant = person(
            id = "connected",
            skills = listOf("Android"),
            connectionStatus = "connected"
        )
        val newPerson = person(
            id = "new",
            skills = emptyList(),
            connectionStatus = "none"
        )

        val ranked = FindPeopleRanker.rankPeople(listOf(connectedRelevant, newPerson), profile)

        assertEquals("new", ranked.first().id)
        assertTrue(ranked.last().connectionStatus == "connected")
    }

    private fun person(
        id: String,
        name: String = id,
        headline: String? = null,
        bio: String? = null,
        skills: List<String> = emptyList(),
        interests: List<String> = emptyList(),
        college: String? = null,
        branch: String? = null,
        mutualConnections: Int = 0,
        isOnline: Boolean = false,
        connectionStatus: String = "none"
    ): PersonInfo {
        return PersonInfo(
            id = id,
            name = name,
            headline = headline,
            bio = bio,
            skills = skills,
            interests = interests,
            college = college,
            branch = branch,
            mutualConnections = mutualConnections,
            isOnline = isOnline,
            connectionStatus = connectionStatus
        )
    }
}
