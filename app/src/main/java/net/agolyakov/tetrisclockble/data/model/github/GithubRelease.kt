package net.agolyakov.tetrisclockble.data.model.github

import com.google.gson.annotations.SerializedName

data class GithubRelease(
    @SerializedName("tag_name")
    val tagName: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("published_at")
    val publishedAt: String,

    @SerializedName("prerelease")
    val prerelease: Boolean = false,

    @SerializedName("assets")
    val assets: List<GithubAsset> = emptyList(),

    @SerializedName("body")
    val body: String? = null,

    @SerializedName("html_url")
    val htmlUrl: String? = null
)