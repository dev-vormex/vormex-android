package com.kyant.backdrop.catalog.linkedin

enum class TalentEngineMode(val label: String) {
    TALENT("Talent"),
    OPERATOR("Operator")
}

enum class TalentEngineTrack(val label: String) {
    DEVELOPERS("Developers"),
    CREATORS("Creators")
}

enum class TalentEngineStage {
    SELECT,
    TRAIN,
    TASK,
    PLACE,
    OPERATE
}

enum class TalentEngineTone {
    BLUE,
    GREEN,
    AMBER,
    PURPLE,
    ROSE
}

data class TalentGraphPosition(
    val x: Float,
    val y: Float
)

data class TalentEngineMetric(
    val label: String,
    val value: String,
    val tone: TalentEngineTone = TalentEngineTone.BLUE
)

data class TalentEngineInfoSection(
    val title: String,
    val body: String
)

data class TalentEngineNode(
    val id: String,
    val title: String,
    val subtitle: String,
    val stage: TalentEngineStage,
    val body: String,
    val status: String,
    val nextAction: String,
    val recognition: String,
    val position: TalentGraphPosition,
    val tone: TalentEngineTone,
    val metrics: List<TalentEngineMetric>,
    val detailSections: List<TalentEngineInfoSection>
)

data class TalentEngineEdge(
    val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val label: String
)

data class TalentEngineStickyNote(
    val title: String,
    val body: String,
    val position: TalentGraphPosition,
    val width: Float = 260f,
    val estimatedHeight: Float = 124f,
    val tone: TalentEngineTone = TalentEngineTone.AMBER
)

data class TalentEngineGraph(
    val mode: TalentEngineMode,
    val track: TalentEngineTrack,
    val title: String,
    val subtitle: String,
    val coreMessage: String,
    val announcementHeadline: String,
    val announcementBody: String,
    val callToAction: String,
    val acceptanceRate: String,
    val cohortSize: String,
    val overviewSections: List<TalentEngineInfoSection>,
    val storySections: List<TalentEngineInfoSection>,
    val faqSections: List<TalentEngineInfoSection>,
    val nodes: List<TalentEngineNode>,
    val edges: List<TalentEngineEdge>,
    val stickyNotes: List<TalentEngineStickyNote>
)

data class TalentGraphBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

data class TalentCanvasViewport(
    val zoom: Float,
    val offsetX: Float,
    val offsetY: Float
)

const val TalentCanvasMinZoom = 0.05f
const val TalentCanvasMaxZoom = 64f

fun clampTalentCanvasZoom(
    zoom: Float,
    minZoom: Float = TalentCanvasMinZoom,
    maxZoom: Float = TalentCanvasMaxZoom
): Float = zoom.coerceIn(minZoom, maxZoom)

fun calculateTalentGraphBounds(
    graph: TalentEngineGraph,
    nodeWidth: Float = 224f,
    nodeHeight: Float = 124f,
    padding: Float = 88f
): TalentGraphBounds {
    if (graph.nodes.isEmpty() && graph.stickyNotes.isEmpty()) {
        return TalentGraphBounds(0f, 0f, padding * 2f, padding * 2f)
    }
    val lefts = graph.nodes.map { it.position.x } + graph.stickyNotes.map { it.position.x }
    val tops = graph.nodes.map { it.position.y } + graph.stickyNotes.map { it.position.y }
    val rights = graph.nodes.map { it.position.x + nodeWidth } +
        graph.stickyNotes.map { it.position.x + it.width }
    val bottoms = graph.nodes.map { it.position.y + nodeHeight } +
        graph.stickyNotes.map { it.position.y + it.estimatedHeight }
    return TalentGraphBounds(
        left = (lefts.minOrNull() ?: 0f) - padding,
        top = (tops.minOrNull() ?: 0f) - padding,
        right = (rights.maxOrNull() ?: 0f) + padding,
        bottom = (bottoms.maxOrNull() ?: 0f) + padding
    )
}

fun calculateTalentFitViewport(
    containerWidth: Float,
    containerHeight: Float,
    bounds: TalentGraphBounds,
    minZoom: Float = TalentCanvasMinZoom,
    maxZoom: Float = TalentCanvasMaxZoom
): TalentCanvasViewport {
    val safeWidth = containerWidth.coerceAtLeast(1f)
    val safeHeight = containerHeight.coerceAtLeast(1f)
    val targetZoom = minOf(
        safeWidth / bounds.width.coerceAtLeast(1f),
        safeHeight / bounds.height.coerceAtLeast(1f)
    )
    val zoom = clampTalentCanvasZoom(targetZoom * 0.92f, minZoom, maxZoom)
    return TalentCanvasViewport(
        zoom = zoom,
        offsetX = (safeWidth - bounds.width * zoom) / 2f - bounds.left * zoom,
        offsetY = (safeHeight - bounds.height * zoom) / 2f - bounds.top * zoom
    )
}

fun talentGraphHasRequiredTalentFlow(graph: TalentEngineGraph): Boolean {
    val stagesByNode = graph.nodes.associate { it.id to it.stage }
    val connectedStages = graph.edges.mapNotNull { edge ->
        val fromStage = stagesByNode[edge.fromNodeId] ?: return@mapNotNull null
        val toStage = stagesByNode[edge.toNodeId] ?: return@mapNotNull null
        fromStage to toStage
    }.toSet()
    return TalentEngineStage.SELECT to TalentEngineStage.TRAIN in connectedStages &&
        TalentEngineStage.TRAIN to TalentEngineStage.TASK in connectedStages &&
        TalentEngineStage.TASK to TalentEngineStage.PLACE in connectedStages
}

fun buildTalentEngineGraph(
    mode: TalentEngineMode,
    track: TalentEngineTrack
): TalentEngineGraph {
    return when (mode) {
        TalentEngineMode.TALENT -> buildTalentJourneyGraph(track)
        TalentEngineMode.OPERATOR -> buildOperatorJourneyGraph(track)
    }
}

private data class TrackCopy(
    val challenge: String,
    val proof: String,
    val task: String,
    val opportunity: String,
    val portfolio: String,
    val badge: String,
    val mentor: String
)

private fun trackCopy(track: TalentEngineTrack): TrackCopy {
    return when (track) {
        TalentEngineTrack.DEVELOPERS -> TrackCopy(
            challenge = "coding challenge",
            proof = "GitHub proof",
            task = "sprint task",
            opportunity = "internship or startup role",
            portfolio = "repo, demo, and shipped issue",
            badge = "Verified Developer Talent",
            mentor = "senior builder mentor"
        )
        TalentEngineTrack.CREATORS -> TrackCopy(
            challenge = "portfolio review",
            proof = "content proof",
            task = "collab brief",
            opportunity = "brand or freelance deal",
            portfolio = "case study, analytics, and media kit",
            badge = "Verified Creator Talent",
            mentor = "creator growth mentor"
        )
    }
}

private const val TalentCoreMessage =
    "Vormex doesn't just connect you - it builds you, backs you, and places you."

private const val TalentAnnouncementHeadline =
    "Introducing the Vormex Talent Engine"

private const val TalentAnnouncementBody =
    "Your skills deserve more than a profile. The Talent Engine is Vormex's curated pipeline: " +
        "we select you, train you with mentors, give you real tasks, track your progress, " +
        "award verified badges, and connect you to internships, startup roles, creator collabs, " +
        "brand deals, and freelance work."

private const val TalentReputationLine =
    "On Vormex, your reliability becomes your reputation - and your reputation unlocks opportunities."

private const val TalentCallToAction = "Explore the Talent Engine"

private fun talentStorySections(copy: TrackCopy): List<TalentEngineInfoSection> {
    return listOf(
        TalentEngineInfoSection("Apply", "Raise your hand to join a cohort by completing your profile and starter ${copy.challenge}. Vormex uses this to understand your direction, skill level, and seriousness."),
        TalentEngineInfoSection("Selected", "Get picked based on skills, intent, and ${copy.proof}. This is meant to feel earned, because selected talent should stand out from a normal social profile."),
        TalentEngineInfoSection("Commitment", "Accept the commitment before the program starts. This is the promise that you will show up, respond on time, and complete what you take on."),
        TalentEngineInfoSection("Train", "Move through self-paced content with a ${copy.mentor} in your corner. The goal is practical readiness, not passive watching."),
        TalentEngineInfoSection("Task", "Receive a real ${copy.task} with a deadline. This is where your work ethic, quality, and reliability become visible."),
        TalentEngineInfoSection("Submit -> Feedback", "Submit ${copy.portfolio}, then get reviewed. Feedback helps you improve, and the review trail becomes part of your trust record."),
        TalentEngineInfoSection("Badge", "Earn the ${copy.badge} badge when your work and delivery meet the bar. The badge signals that Vormex has seen proof, not just claims."),
        TalentEngineInfoSection("Opportunity -> Placed", "Get matched to an ${copy.opportunity} when your proof, mentor feedback, and Reliability Score are strong enough.")
    )
}

private fun talentFaqSections(): List<TalentEngineInfoSection> {
    return listOf(
        TalentEngineInfoSection("Is it free?", "Joining and applying is free in v1. If future gates are added, the app can show them clearly before the user commits."),
        TalentEngineInfoSection("Who can apply?", "Any Vormex user can apply, starting with Developers and Creators tracks."),
        TalentEngineInfoSection("What do I get?", "Mentorship, real tasks, verified badges, portfolio proof, and direct access to internships, startup roles, creator collabs, brand deals, and freelance opportunities."),
        TalentEngineInfoSection("What's expected of me?", "Stay active and complete your commitments. That is how you build Reliability Score, earn trust, and unlock placements.")
    )
}

private fun talentGraphStickyNotes(
    copy: TrackCopy,
    topTitle: String,
    topBody: String
): List<TalentEngineStickyNote> {
    return listOf(
        TalentEngineStickyNote(
            title = topTitle,
            body = topBody,
            position = TalentGraphPosition(950f, 510f),
            width = 260f,
            estimatedHeight = 124f,
            tone = TalentEngineTone.AMBER
        ),
        TalentEngineStickyNote(
            title = "Talent Engine copy",
            body = talentEngineFullCopy(copy),
            position = TalentGraphPosition(950f, 665f),
            width = 560f,
            estimatedHeight = 980f,
            tone = TalentEngineTone.PURPLE
        )
    )
}

private fun talentEngineFullCopy(copy: TrackCopy): String {
    val story = talentStorySections(copy).mapIndexed { index, section ->
        "${index + 1}. ${section.title} - ${section.body}"
    }.joinToString("\n")
    val faq = talentFaqSections().joinToString("\n") { section ->
        "${section.title}: ${section.body}"
    }

    return listOf(
        "Core message: $TalentCoreMessage",
        "$TalentAnnouncementHeadline\n$TalentAnnouncementBody\nCTA: $TalentCallToAction",
        "Push copy:\nNew on Vormex: the Talent Engine is live. Get selected, get trained, get placed. Tap to see your path.\nYour next opportunity starts here. Explore the Vormex Talent Engine.\nApply -> Train -> Task -> Placed. See how Vormex turns talent into opportunities.",
        "Social copy:\nMost apps connect you and leave you there. Vormex builds you. Introducing the Talent Engine - get selected, trained by mentors, given real tasks, earn verified badges, and get placed into internships, startup roles, brand deals, and more.\n\nLinkedIn: The Vormex Talent Engine is a curated pipeline that selects promising students and creators, trains them with mentors, assigns real tasks, verifies their work, and places them into internships, startup roles, creator collaborations, brand deals, and freelance opportunities.\n\nX: Vormex Talent Engine is live. Selected -> Trained -> Tasked -> Placed. Real mentors. Real tasks. Real opportunities. Your reliability becomes your reputation.",
        "Community broadcast:\nWe launched the Vormex Talent Engine. Instead of just networking, users can get selected into curated cohorts, trained by mentors, given real tasks, earn verified badges on their profile, and get connected to internships, startup roles, brand deals, and freelance gigs. It is selective and serious: when you commit to a task, you are expected to deliver.",
        "How it works:\n$story",
        "FAQ:\n$faq",
        "Prestige line: $TalentReputationLine"
    ).joinToString("\n\n")
}

private fun buildTalentJourneyGraph(track: TalentEngineTrack): TalentEngineGraph {
    val copy = trackCopy(track)
    val nodes = listOf(
        talentNode("apply", "Apply", "Profile + ${copy.challenge}", TalentEngineStage.SELECT, 40f, 90f, TalentEngineTone.BLUE, "Submitted", "Complete the starter ${copy.challenge}.", "Application review started.", "Profile data, goals, skills, and proof-of-work are pulled into one candidate packet.", "Decision", "24h"),
        talentNode("selected", "Selected", "Curated cohort", TalentEngineStage.SELECT, 330f, 90f, TalentEngineTone.GREEN, "Accepted", "Accept the cohort commitment.", "Membership feels earned.", "Vormex shows cohort size and acceptance rate before the commitment step.", "Acceptance", "18%"),
        talentNode("commitment", "Commitment", "Timestamped promise", TalentEngineStage.SELECT, 620f, 90f, TalentEngineTone.AMBER, "Required", "Sign the cohort commitment.", "Trust signal unlocked.", "Every accepted talent explicitly accepts the commitment before training begins.", "Commitment", "Pending"),
        talentNode("train", "Train", "Track modules", TalentEngineStage.TRAIN, 910f, 90f, TalentEngineTone.PURPLE, "In progress", "Finish module one.", "Learning streak started.", "Self-paced lessons and mentor-led checkpoints keep the track practical.", "Progress", "42%"),
        talentNode("mentor", "Mentor", copy.mentor, TalentEngineStage.TRAIN, 910f, 300f, TalentEngineTone.BLUE, "Assigned", "Book the next check-in.", "No active talent is alone.", "Mentor notes and accountability check-ins keep momentum visible to the Vormex team.", "SLA", "Booked"),
        talentNode("task", "Task", copy.task, TalentEngineStage.TASK, 620f, 300f, TalentEngineTone.AMBER, "Allocated", "Accept task commitment.", "Real work begins.", "Tasks have deadlines, reminders, escalation, and consequences if commitments are missed.", "Due", "48h"),
        talentNode("submit", "Submit", copy.portfolio, TalentEngineStage.TASK, 330f, 300f, TalentEngineTone.GREEN, "Ready", "Upload proof and reflection.", "Portfolio proof captured.", "Submission stores the work, context, files, and self-review for operator feedback.", "Quality", "Review"),
        talentNode("feedback", "Feedback", "SLA review loop", TalentEngineStage.TASK, 40f, 300f, TalentEngineTone.ROSE, "Queued", "Wait for mentor feedback.", "No ghosting guarantee.", "Every submission receives feedback within SLA or gets escalated to an operator.", "Feedback SLA", "24h"),
        talentNode("badge", "Badge", copy.badge, TalentEngineStage.PLACE, 40f, 510f, TalentEngineTone.PURPLE, "Earned", "Attach badge to profile.", "Public trust signal.", "Quality completion creates verified portfolio items and public skill badges.", "Reliability", "91"),
        talentNode("opportunity", "Opportunity", copy.opportunity, TalentEngineStage.PLACE, 330f, 510f, TalentEngineTone.BLUE, "Matched", "Review intro packet.", "Vormex opens doors.", "Operators match talent using skills, verified proof, cohort outcomes, and Reliability Score.", "Match", "Manual"),
        talentNode("placed", "Placed", "Outcome captured", TalentEngineStage.PLACE, 620f, 510f, TalentEngineTone.GREEN, "Tracked", "Confirm start details.", "Cohort success recorded.", "Placement outcomes capture role, start date, status, and future ISA-ready fields.", "Status", "Placed")
    )
    return TalentEngineGraph(
        mode = TalentEngineMode.TALENT,
        track = track,
        title = "${track.label} Talent Engine",
        subtitle = "A selective journey from application to real opportunity.",
        coreMessage = TalentCoreMessage,
        announcementHeadline = TalentAnnouncementHeadline,
        announcementBody = TalentAnnouncementBody,
        callToAction = TalentCallToAction,
        acceptanceRate = "18%",
        cohortSize = "24 seats",
        overviewSections = listOf(
            TalentEngineInfoSection(
                title = "Core message",
                body = TalentCoreMessage
            ),
            TalentEngineInfoSection(
                title = "Launch announcement",
                body = "$TalentAnnouncementBody See the full journey on an interactive map - from Apply all the way to Placed."
            ),
            TalentEngineInfoSection(
                title = "What this means for talent",
                body = "The Talent Engine is Vormex's curated path for serious students and creators. It is not just a feed or a jobs board. Vormex selects people, trains them with track-specific work, assigns real tasks, monitors delivery, and then opens opportunity conversations when the proof is strong enough."
            ),
            TalentEngineInfoSection(
                title = "Why staying active matters",
                body = "Progress is monitored through commitments, training progress, task submissions, feedback SLAs, and Reliability Score. Active users build a trusted record that helps Vormex decide who is ready for internships, startup roles, freelance gigs, creator collabs, brand deals, and other opportunities."
            ),
            TalentEngineInfoSection(
                title = "How opportunities unlock",
                body = "The opportunity step is based on proof, not noise. Your profile, verified work, mentor feedback, task quality, and reliability all become signals that make you easier to recommend to companies and clients."
            ),
            TalentEngineInfoSection(
                title = "The line that sells it",
                body = TalentReputationLine
            )
        ),
        storySections = talentStorySections(copy),
        faqSections = talentFaqSections(),
        nodes = nodes,
        edges = sequentialEdges(nodes),
        stickyNotes = talentGraphStickyNotes(
            copy = copy,
            topTitle = "Stay active",
            topBody = "Vormex team is monitoring your progress. Stay active, complete commitments, and unlock opportunities."
        )
    )
}

private fun buildOperatorJourneyGraph(track: TalentEngineTrack): TalentEngineGraph {
    val copy = trackCopy(track)
    val nodes = listOf(
        talentNode("review", "Review Queue", "Score applicants", TalentEngineStage.OPERATE, 40f, 90f, TalentEngineTone.BLUE, "Live", "Review pending candidates.", "Every applicant gets a reasoned decision.", "Operators compare profile strength, ${copy.challenge}, goals, and proof before accepting, waitlisting, or rejecting.", "Pending", "36"),
        talentNode("cohorts", "Cohorts", "Roster + commitment", TalentEngineStage.SELECT, 330f, 90f, TalentEngineTone.GREEN, "Open", "Lock the pilot roster.", "Membership stays selective.", "Cohorts expose size, acceptance rate, and commitment acceptance status.", "Seats", "24"),
        talentNode("mentors", "Mentors", "Assigned guidance", TalentEngineStage.TRAIN, 620f, 90f, TalentEngineTone.PURPLE, "Assigned", "Balance mentor load.", "No active talent is unowned.", "Mentors own check-ins, notes, accountability nudges, and session outcomes.", "Coverage", "100%"),
        talentNode("task-sla", "Task SLA", copy.task, TalentEngineStage.TASK, 910f, 90f, TalentEngineTone.AMBER, "Watching", "Nudge due-soon work.", "Commitments have teeth.", "Reminder, mentor nudge, operator flag, and consequence chain are visible in one place.", "Breaches", "2"),
        talentNode("reliability", "Reliability", "0-100 score", TalentEngineStage.TASK, 910f, 300f, TalentEngineTone.ROSE, "Auditable", "Review low-score cases.", "Trust becomes measurable.", "On-time quality raises score; missed deadlines, ghosting, and no-shows lower it.", "Avg score", "86"),
        talentNode("manual-match", "Manual Match", "Operator-led intros", TalentEngineStage.PLACE, 620f, 300f, TalentEngineTone.BLUE, "Ready", "Pick the next intro.", "Humans stay in the loop.", "Operators match vetted talent to companies using proof, reliability, track outcomes, and fit.", "Match mode", "Manual"),
        talentNode("company-leads", "Company Leads", "Approved opportunities", TalentEngineStage.PLACE, 330f, 300f, TalentEngineTone.GREEN, "Sourced", "Approve company posts.", "Supply stays trusted.", "Companies can post opportunities, but operators approve and can curate opportunities for them.", "Response SLA", "48h"),
        talentNode("outcomes", "Placement Outcomes", "Offer + start details", TalentEngineStage.PLACE, 40f, 300f, TalentEngineTone.PURPLE, "Tracked", "Update placed talent.", "The funnel becomes visible.", "Operator analytics track applied, accepted, trained, tasked, matched, and placed.", "Placed", "7")
    )
    return TalentEngineGraph(
        mode = TalentEngineMode.OPERATOR,
        track = track,
        title = "${track.label} Operator Console",
        subtitle = "Run cohorts, monitor accountability, and place vetted talent.",
        coreMessage = TalentCoreMessage,
        announcementHeadline = "Vormex is monitoring the full talent pipeline",
        announcementBody = "Operator mode shows how the Vormex team keeps the Talent Engine serious: selected cohorts, mentor coverage, real tasks, SLA reviews, Reliability Score, company leads, and placement outcomes.",
        callToAction = "Run the Talent Engine",
        acceptanceRate = "18%",
        cohortSize = "24 seats",
        overviewSections = listOf(
            TalentEngineInfoSection(
                title = "Operating principle",
                body = "Commitment is real. When talent accepts a task, the system expects delivery, tracks the promise, and turns reliability into a visible reputation signal."
            ),
            TalentEngineInfoSection(
                title = "How Vormex runs the engine",
                body = "Operator mode shows the internal operating system behind the program. The Vormex team reviews applicants, forms cohorts, assigns mentors, allocates tasks, watches SLAs, and manually matches people to opportunities while the product is still concierge-led."
            ),
            TalentEngineInfoSection(
                title = "Why Company Leads matter",
                body = "Company Leads are the supply side of the Talent Engine. Operators approve self-posted opportunities and can also curate roles, briefs, collabs, internships, startup projects, and brand deals on behalf of companies so talent is matched only to trusted leads."
            ),
            TalentEngineInfoSection(
                title = "What gets monitored",
                body = "The team watches acceptance decisions, commitment acceptance, mentor coverage, task deadlines, feedback turnaround, reliability drops, company response SLAs, intro status, and placement outcomes. This is how Vormex keeps the program high-trust."
            ),
            TalentEngineInfoSection(
                title = "Outcome story",
                body = "The operator graph turns the public promise into a real operating system: selected -> trained -> tasked -> reviewed -> badged -> matched -> placed."
            )
        ),
        storySections = talentStorySections(copy),
        faqSections = talentFaqSections(),
        nodes = nodes,
        edges = sequentialEdges(nodes),
        stickyNotes = talentGraphStickyNotes(
            copy = copy,
            topTitle = "Accountability spine",
            topBody = "Commitments, Reliability Score, SLAs, and no-ghosting guarantees are the core operating system."
        )
    )
}

private fun talentNode(
    id: String,
    title: String,
    subtitle: String,
    stage: TalentEngineStage,
    x: Float,
    y: Float,
    tone: TalentEngineTone,
    status: String,
    nextAction: String,
    recognition: String,
    body: String,
    metricLabel: String,
    metricValue: String
): TalentEngineNode {
    return TalentEngineNode(
        id = id,
        title = title,
        subtitle = subtitle,
        stage = stage,
        body = body,
        status = status,
        nextAction = nextAction,
        recognition = recognition,
        position = TalentGraphPosition(x, y),
        tone = tone,
        metrics = listOf(
            TalentEngineMetric(metricLabel, metricValue, tone),
            TalentEngineMetric("SLA", if (stage == TalentEngineStage.PLACE) "Active" else "Tracked", TalentEngineTone.GREEN)
        ),
        detailSections = talentNodeDetailSections(
            id = id,
            subtitle = subtitle,
            body = body,
            metricLabel = metricLabel,
            metricValue = metricValue,
            nextAction = nextAction,
            recognition = recognition
        )
    )
}

private fun talentNodeDetailSections(
    id: String,
    subtitle: String,
    body: String,
    metricLabel: String,
    metricValue: String,
    nextAction: String,
    recognition: String
): List<TalentEngineInfoSection> {
    val stepStory = when (id) {
        "apply" -> "Apply is the user's first signal that they want more than networking. They raise their hand for a cohort, show their profile, add proof-of-work, and complete the starter requirement: $subtitle. The message is clear: joining starts with intent."
        "selected" -> "Selected means Vormex has reviewed the user's skills, intent, and proof, then placed them into a curated cohort. This is where prestige starts: the program should feel selective, serious, and outcome-driven."
        "commitment" -> "Commitment turns interest into a promise. The user accepts that they will stay active, respond, complete tasks, and respect deadlines. This is the moment Vormex can say the pipeline is real."
        "train" -> "Train gives the user a practical path instead of leaving them with a static profile. Lessons, checkpoints, and mentor notes help talent become ready for real work."
        "mentor" -> "Mentor keeps the user from moving alone. A mentor can check progress, unblock the user, review direction, and add human confidence before tasks and opportunities."
        "task" -> "Task is where Vormex becomes different from a normal community app. The user receives real work with a deadline, and delivery becomes evidence."
        "submit" -> "Submit captures the proof: work files, links, reflection, portfolio evidence, and context. This makes talent easier to verify and easier to recommend."
        "feedback" -> "Feedback is the improvement loop. Vormex reviews the work, gives direction, escalates missed reviews, and helps the user level up instead of disappearing after submission."
        "badge" -> "Badge is the public trust signal. A verified badge should mean Vormex has seen real proof, real delivery, and a record of reliability."
        "opportunity" -> "Opportunity is the outcome layer. Vormex can match serious talent to internships, startup roles, creator collabs, brand deals, freelance work, and company leads."
        "placed" -> "Placed records the result. The user's journey moves from selected and trained to actually connected with a real opportunity."
        "review" -> "Review Queue is where Vormex protects quality. Operators compare profiles, starter work, goals, and proof before accepting, waitlisting, or rejecting applicants."
        "cohorts" -> "Cohorts turn scattered users into a managed batch. Operators can see seats, acceptance rate, commitment status, and who is ready to start."
        "mentors" -> "Mentors give every active candidate ownership. Operator mode helps balance mentor load, check coverage, and avoid leaving active talent unmanaged."
        "task-sla" -> "Task SLA makes commitments enforceable. Deadlines, nudges, escalation, and consequence chains help keep the program serious."
        "reliability" -> "Reliability makes reputation measurable. On-time delivery, response behavior, quality, missed deadlines, and ghosting all affect trust."
        "manual-match" -> "Manual Match keeps humans in the loop for high-stakes introductions. Operators use verified proof, fit, reliability, and track outcomes before making intros."
        "company-leads" -> "Company Leads are the opportunity supply side. Operators approve leads, curate roles or briefs, and keep the pipeline trusted for both companies and talent."
        "outcomes" -> "Placement Outcomes close the loop. Operators track who applied, trained, submitted, matched, started, and successfully placed."
        else -> body
    }

    return listOf(
        TalentEngineInfoSection(
            title = "What this node means",
            body = stepStory
        ),
        TalentEngineInfoSection(
            title = "Why it matters",
            body = "$body $TalentReputationLine"
        ),
        TalentEngineInfoSection(
            title = "What Vormex monitors",
            body = "The team watches progress, commitment state, SLA health, quality, response behavior, and delivery proof. The visible signal here is $metricLabel: $metricValue."
        ),
        TalentEngineInfoSection(
            title = "User next action",
            body = nextAction
        ),
        TalentEngineInfoSection(
            title = "Recognition unlocked",
            body = recognition
        )
    )
}

private fun sequentialEdges(nodes: List<TalentEngineNode>): List<TalentEngineEdge> {
    return nodes.zipWithNext().mapIndexed { index, (from, to) ->
        TalentEngineEdge(
            id = "edge-${index + 1}",
            fromNodeId = from.id,
            toNodeId = to.id,
            label = when {
                from.stage != to.stage -> "${from.stage.name.lowercase()} -> ${to.stage.name.lowercase()}"
                else -> "next"
            }
        )
    }
}
