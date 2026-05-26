package com.kyant.backdrop.catalog.linkedin

enum class PostShareTargetSource {
    RecentChat,
    RecentConnection,
    Connection,
    Recommended
}

data class PostShareTarget(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val avatar: String? = null,
    val headline: String? = null,
    val reason: String,
    val source: PostShareTargetSource
)
