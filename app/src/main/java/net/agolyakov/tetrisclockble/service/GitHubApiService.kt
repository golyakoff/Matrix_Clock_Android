package net.agolyakov.tetrisclockble.service

import retrofit2.http.GET
import retrofit2.http.Path

interface GithubApiService {
    @GET("repos/golyakoff/Matrix_Clock_ESP32/releases/latest")
    suspend fun getLatestRelease(): GithubRelease

    @GET("repos/golyakoff/Matrix_Clock_ESP32/releases")
    suspend fun getAllReleases(): List<GithubRelease>
}
