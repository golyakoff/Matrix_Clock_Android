package net.agolyakov.tetrisclockble.data.remote.api

import net.agolyakov.tetrisclockble.data.model.github.GithubRelease
import retrofit2.http.GET

interface GitHubApiService {
    @GET("repos/golyakoff/Matrix_Clock_ESP32/releases/latest")
    suspend fun getLatestRelease(): GithubRelease

    @GET("repos/golyakoff/Matrix_Clock_ESP32/releases")
    suspend fun getAllReleases(): List<GithubRelease>
}
