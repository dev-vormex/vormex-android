package com.kyant.backdrop.catalog.linkedin

// User data
data class User(
    val id: String,
    val name: String,
    val headline: String,
    val avatarInitials: String,
    val connections: Int = 0,
    val isConnected: Boolean = false
)

// Post data
data class Post(
    val id: String,
    val author: User,
    val content: String,
    val timeAgo: String,
    val likes: Int,
    val comments: Int,
    val reposts: Int,
    val hasImage: Boolean = false
)

// Job data
data class Job(
    val id: String,
    val title: String,
    val company: String,
    val location: String,
    val postedAgo: String,
    val isEasyApply: Boolean = true,
    val salary: String? = null
)

// Notification data
data class Notification(
    val id: String,
    val title: String,
    val description: String,
    val timeAgo: String,
    val type: NotificationType
)

enum class NotificationType {
    LIKE, COMMENT, CONNECTION, JOB, MENTION, VIEW
}

// Mock data
object MockData {
    val currentUser = User(
        id = "1",
        name = "Alex Johnson",
        headline = "Senior Software Engineer at Google | Kotlin & Android Expert",
        avatarInitials = "AJ",
        connections = 847
    )

    val users = listOf(
        User("2", "Sarah Chen", "Product Manager at Meta", "SC", 1234, false),
        User("3", "Michael Brown", "Engineering Lead at Netflix", "MB", 892, true),
        User("4", "Emily Davis", "UX Designer at Apple", "ED", 567, false),
        User("5", "James Wilson", "CTO at Startup Inc", "JW", 2341, true),
        User("6", "Lisa Wang", "Data Scientist at Amazon", "LW", 789, false),
        User("7", "David Kim", "Mobile Developer at Spotify", "DK", 456, false),
        User("8", "Rachel Green", "HR Director at Microsoft", "RG", 1567, true)
    )

    val posts = listOf(
        Post(
            "1",
            users[0],
            "Excited to announce that our team just shipped a major feature update! 🚀 This has been months in the making and I'm so proud of everyone involved. Remember: great products are built by great teams.\n\n#ProductManagement #Tech #Launch",
            "2h",
            234,
            45,
            12,
            true
        ),
        Post(
            "2",
            users[1],
            "Just published my thoughts on scaling engineering teams from 10 to 100 engineers. The challenges are real but the growth is incredible. Link in comments! 👇",
            "4h",
            567,
            89,
            34
        ),
        Post(
            "3",
            users[2],
            "Design tip of the day: White space is not empty space, it's breathing room for your design. Don't be afraid to let your elements breathe.\n\n#UXDesign #DesignTips",
            "6h",
            189,
            23,
            8
        ),
        Post(
            "4",
            users[3],
            "We're hiring! Looking for passionate engineers to join our growing team. Remote-first, great culture, exciting problems. DM me if interested!",
            "8h",
            345,
            67,
            45
        ),
        Post(
            "5",
            users[4],
            "Machine learning is not magic - it's math, data, and a lot of experimentation. Here's what I learned building ML pipelines at scale over the last 5 years...",
            "12h",
            678,
            112,
            56,
            true
        )
    )

    val jobs = listOf(
        Job("1", "Senior Android Developer", "Google", "Mountain View, CA (Remote)", "1d", true, "$180k - $250k"),
        Job("2", "Staff Software Engineer", "Meta", "Menlo Park, CA (Hybrid)", "2d", true, "$200k - $300k"),
        Job("3", "Mobile Tech Lead", "Netflix", "Los Gatos, CA", "3d", false, "$220k - $320k"),
        Job("4", "Principal Engineer", "Amazon", "Seattle, WA (Remote)", "4d", true, "$190k - $280k"),
        Job("5", "Android Platform Engineer", "Apple", "Cupertino, CA", "5d", false, "$210k - $290k"),
        Job("6", "Senior Kotlin Developer", "Spotify", "Stockholm (Remote)", "1w", true, "$150k - $200k")
    )

    val notifications = listOf(
        Notification("1", "Sarah Chen liked your post", "Your post about Kotlin best practices", "5m", NotificationType.LIKE),
        Notification("2", "New connection request", "Michael Brown wants to connect", "1h", NotificationType.CONNECTION),
        Notification("3", "Your profile was viewed", "15 people viewed your profile this week", "2h", NotificationType.VIEW),
        Notification("4", "Jobs you might like", "5 new Senior Android Developer jobs", "4h", NotificationType.JOB),
        Notification("5", "Emily Davis commented", "Great insights! I especially loved...", "6h", NotificationType.COMMENT),
        Notification("6", "James Wilson mentioned you", "In a post about mobile development", "1d", NotificationType.MENTION)
    )
    
    // ==================== Find People Mock Data ====================
    
    data class MockPersonInfo(
        val id: String,
        val username: String?,
        val name: String?,
        val profileImage: String?,
        val bannerImageUrl: String?,
        val headline: String?,
        val college: String?,
        val branch: String?,
        val bio: String?,
        val skills: List<String>,
        val interests: List<String>,
        val isOnline: Boolean,
        val connectionStatus: String,
        val mutualConnections: Int
    )
    
    data class MockNearbyUser(
        val id: String,
        val name: String?,
        val username: String?,
        val profileImage: String?,
        val bannerImage: String?,
        val headline: String?,
        val skills: List<String>,
        val interests: List<String>,
        val distance: Double,
        val isOnline: Boolean,
        val lat: Double,
        val lng: Double,
        val city: String?,
        val state: String?
    )
    
    val mockPeople = listOf(
        MockPersonInfo(
            id = "p1",
            username = "priya_dev",
            name = "Priya Sharma",
            profileImage = "https://i.pravatar.cc/150?img=1",
            bannerImageUrl = null,
            headline = "Software Engineer at Microsoft | Kotlin & Android",
            college = "IIT Delhi",
            branch = "Computer Science",
            bio = "Building awesome mobile experiences",
            skills = listOf("Kotlin", "Android", "Jetpack Compose", "MVVM"),
            interests = listOf("Technology", "Photography", "Travel"),
            isOnline = true,
            connectionStatus = "none",
            mutualConnections = 12
        ),
        MockPersonInfo(
            id = "p2",
            username = "rahul_coder",
            name = "Rahul Verma",
            profileImage = "https://i.pravatar.cc/150?img=3",
            bannerImageUrl = null,
            headline = "Full Stack Developer | React & Node.js",
            college = "IIT Bombay",
            branch = "Information Technology",
            bio = "Passionate about building scalable web apps",
            skills = listOf("React", "Node.js", "TypeScript", "MongoDB"),
            interests = listOf("Open Source", "Gaming", "Music"),
            isOnline = false,
            connectionStatus = "none",
            mutualConnections = 8
        ),
        MockPersonInfo(
            id = "p3",
            username = "ananya.ui",
            name = "Ananya Gupta",
            profileImage = "https://i.pravatar.cc/150?img=5",
            bannerImageUrl = null,
            headline = "UX Designer at Google | Creating delightful experiences",
            college = "NID Ahmedabad",
            branch = "Design",
            bio = "Design is not just what it looks like, it's how it works",
            skills = listOf("Figma", "UI/UX", "Prototyping", "User Research"),
            interests = listOf("Design", "Art", "Reading"),
            isOnline = true,
            connectionStatus = "none",
            mutualConnections = 15
        ),
        MockPersonInfo(
            id = "p4",
            username = "vikram_ml",
            name = "Vikram Singh",
            profileImage = "https://i.pravatar.cc/150?img=7",
            bannerImageUrl = null,
            headline = "ML Engineer at Amazon | AI/ML Enthusiast",
            college = "IIT Madras",
            branch = "Computer Science",
            bio = "Making machines learn, one model at a time",
            skills = listOf("Python", "TensorFlow", "PyTorch", "Computer Vision"),
            interests = listOf("AI", "Research", "Chess"),
            isOnline = false,
            connectionStatus = "pending_sent",
            mutualConnections = 5
        ),
        MockPersonInfo(
            id = "p5",
            username = "neha_backend",
            name = "Neha Patel",
            profileImage = "https://i.pravatar.cc/150?img=9",
            bannerImageUrl = null,
            headline = "Backend Engineer at Flipkart | Go & Microservices",
            college = "BITS Pilani",
            branch = "Computer Science",
            bio = "Building robust backend systems at scale",
            skills = listOf("Go", "Kubernetes", "Docker", "PostgreSQL"),
            interests = listOf("System Design", "Hiking", "Photography"),
            isOnline = true,
            connectionStatus = "none",
            mutualConnections = 10
        ),
        MockPersonInfo(
            id = "p6",
            username = "arjun_ios",
            name = "Arjun Reddy",
            profileImage = "https://i.pravatar.cc/150?img=11",
            bannerImageUrl = null,
            headline = "iOS Developer at Swiggy | Swift Expert",
            college = "VIT Vellore",
            branch = "Software Engineering",
            bio = "Crafting beautiful iOS apps with SwiftUI",
            skills = listOf("Swift", "SwiftUI", "iOS", "Xcode"),
            interests = listOf("Mobile Development", "Cricket", "Cooking"),
            isOnline = false,
            connectionStatus = "connected",
            mutualConnections = 20
        )
    )
    
    val mockNearbyPeople = listOf(
        MockNearbyUser(
            id = "n1",
            name = "Aditya Kumar",
            username = "aditya_dev",
            profileImage = "https://i.pravatar.cc/150?img=12",
            bannerImage = null,
            headline = "Frontend Developer | React Native",
            skills = listOf("React Native", "JavaScript", "TypeScript"),
            interests = listOf("Mobile Apps", "Gaming"),
            distance = 1.2,
            isOnline = true,
            lat = 17.4399,
            lng = 78.4983,
            city = "Hyderabad",
            state = "Telangana"
        ),
        MockNearbyUser(
            id = "n2",
            name = "Sneha Reddy",
            username = "sneha_design",
            profileImage = "https://i.pravatar.cc/150?img=13",
            bannerImage = null,
            headline = "Product Designer at Razorpay",
            skills = listOf("UI Design", "Figma", "Design Systems"),
            interests = listOf("Design", "Art", "Travel"),
            distance = 2.5,
            isOnline = false,
            lat = 17.4456,
            lng = 78.3889,
            city = "Hyderabad",
            state = "Telangana"
        ),
        MockNearbyUser(
            id = "n3",
            name = "Karthik Nair",
            username = "karthik_ml",
            profileImage = "https://i.pravatar.cc/150?img=14",
            bannerImage = null,
            headline = "Data Scientist at PhonePe",
            skills = listOf("Python", "ML", "Data Analysis"),
            interests = listOf("AI", "Finance", "Books"),
            distance = 3.8,
            isOnline = true,
            lat = 17.4326,
            lng = 78.4071,
            city = "Hyderabad",
            state = "Telangana"
        ),
        MockNearbyUser(
            id = "n4",
            name = "Meera Iyer",
            username = "meera_backend",
            profileImage = "https://i.pravatar.cc/150?img=15",
            bannerImage = null,
            headline = "Backend Engineer at Zoho",
            skills = listOf("Java", "Spring Boot", "AWS"),
            interests = listOf("Technology", "Music", "Yoga"),
            distance = 5.2,
            isOnline = false,
            lat = 17.4512,
            lng = 78.3814,
            city = "Hyderabad",
            state = "Telangana"
        ),
        MockNearbyUser(
            id = "n5",
            name = "Rohan Mehta",
            username = "rohan_devops",
            profileImage = "https://i.pravatar.cc/150?img=16",
            bannerImage = null,
            headline = "DevOps Engineer at Freshworks",
            skills = listOf("Kubernetes", "Docker", "Terraform"),
            interests = listOf("Cloud", "Automation", "Cricket"),
            distance = 7.1,
            isOnline = true,
            lat = 17.4245,
            lng = 78.4527,
            city = "Hyderabad",
            state = "Telangana"
        )
    )
}
