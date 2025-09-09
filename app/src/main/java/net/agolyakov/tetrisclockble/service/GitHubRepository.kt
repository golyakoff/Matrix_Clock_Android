package net.agolyakov.tetrisclockble.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class GithubRepository @Inject constructor(
    private val githubApiService: GithubApiService
) {
    suspend fun getLatestRelease(includePreReleases: Boolean): GithubRelease {
        return withContext(Dispatchers.IO) {
            if (includePreReleases) {
                githubApiService.getAllReleases().firstOrNull()
                    ?: throw IOException("No releases found") as Throwable
            } else {
                githubApiService.getLatestRelease()
            }
        }
    }
}