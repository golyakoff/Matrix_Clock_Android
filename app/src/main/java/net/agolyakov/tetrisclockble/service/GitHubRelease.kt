package net.agolyakov.tetrisclockble.service

data class GithubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val assets: List<GithubAsset>,
    val prerelease: Boolean
)
