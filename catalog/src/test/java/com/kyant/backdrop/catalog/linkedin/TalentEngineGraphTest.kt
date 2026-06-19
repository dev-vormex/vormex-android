package com.kyant.backdrop.catalog.linkedin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TalentEngineGraphTest {
    @Test
    fun `talent graph contains required select train task place flow`() {
        val graph = buildTalentEngineGraph(TalentEngineMode.TALENT, TalentEngineTrack.DEVELOPERS)

        assertTrue(talentGraphHasRequiredTalentFlow(graph))
        assertTrue(graph.nodes.any { it.title == "Apply" && it.stage == TalentEngineStage.SELECT })
        assertTrue(graph.nodes.any { it.title == "Train" && it.stage == TalentEngineStage.TRAIN })
        assertTrue(graph.nodes.any { it.title == "Task" && it.stage == TalentEngineStage.TASK })
        assertTrue(graph.nodes.any { it.title == "Opportunity" && it.stage == TalentEngineStage.PLACE })
    }

    @Test
    fun `both modes produce valid linked graphs`() {
        TalentEngineMode.values().forEach { mode ->
            val graph = buildTalentEngineGraph(mode, TalentEngineTrack.DEVELOPERS)

            assertTrue(graph.nodes.isNotEmpty())
            assertTrue(graph.edges.isNotEmpty())
            graph.edges.forEach { edge ->
                assertTrue(graph.nodes.any { it.id == edge.fromNodeId })
                assertTrue(graph.nodes.any { it.id == edge.toNodeId })
            }
        }
    }

    @Test
    fun `developer and creator tracks use different seeded content`() {
        val developerGraph = buildTalentEngineGraph(TalentEngineMode.TALENT, TalentEngineTrack.DEVELOPERS)
        val creatorGraph = buildTalentEngineGraph(TalentEngineMode.TALENT, TalentEngineTrack.CREATORS)

        assertNotEquals(developerGraph.title, creatorGraph.title)
        assertTrue(developerGraph.nodes.any { it.subtitle.contains("coding challenge", ignoreCase = true) })
        assertTrue(creatorGraph.nodes.any { it.subtitle.contains("portfolio review", ignoreCase = true) })
    }

    @Test
    fun `talent graph carries full announcement story faq and node detail copy`() {
        val graph = buildTalentEngineGraph(TalentEngineMode.TALENT, TalentEngineTrack.DEVELOPERS)
        val applyNode = graph.nodes.first { it.id == "apply" }
        val placedNode = graph.nodes.first { it.id == "placed" }

        assertTrue(graph.coreMessage.contains("builds you", ignoreCase = true))
        assertTrue(graph.announcementBody.contains("verified badges", ignoreCase = true))
        assertTrue(graph.overviewSections.any { it.body.contains("interactive map", ignoreCase = true) })
        assertTrue(graph.storySections.any { it.title == "Submit -> Feedback" })
        assertTrue(graph.faqSections.any { it.title == "What do I get?" })
        assertTrue(applyNode.detailSections.any { it.body.contains("raise their hand", ignoreCase = true) })
        assertTrue(placedNode.detailSections.any { it.body.contains("reliability becomes your reputation", ignoreCase = true) })
    }

    @Test
    fun `full text copy is placed in graph notes below stay active`() {
        val graph = buildTalentEngineGraph(TalentEngineMode.TALENT, TalentEngineTrack.DEVELOPERS)
        val stayActive = graph.stickyNotes.first { it.title == "Stay active" }
        val copyNote = graph.stickyNotes.first { it.title == "Talent Engine copy" }

        assertTrue(copyNote.position.y > stayActive.position.y)
        assertTrue(copyNote.body.contains("Community broadcast", ignoreCase = true))
        assertTrue(copyNote.body.contains("How it works", ignoreCase = true))
        assertTrue(copyNote.body.contains("FAQ", ignoreCase = true))
    }

    @Test
    fun `zoom clamping keeps canvas inside wide supported bounds`() {
        assertEquals(TalentCanvasMinZoom, clampTalentCanvasZoom(0.01f))
        assertEquals(1.4f, clampTalentCanvasZoom(1.4f))
        assertEquals(9f, clampTalentCanvasZoom(9f))
        assertEquals(TalentCanvasMaxZoom, clampTalentCanvasZoom(128f))
    }

    @Test
    fun `fit viewport returns bounded zoom for graph bounds`() {
        val graph = buildTalentEngineGraph(TalentEngineMode.TALENT, TalentEngineTrack.DEVELOPERS)
        val bounds = calculateTalentGraphBounds(graph)
        val viewport = calculateTalentFitViewport(
            containerWidth = 390f,
            containerHeight = 520f,
            bounds = bounds
        )

        assertTrue(viewport.zoom in TalentCanvasMinZoom..TalentCanvasMaxZoom)
        assertFalse(viewport.offsetX.isNaN())
        assertFalse(viewport.offsetY.isNaN())
    }
}
