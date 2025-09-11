package net.agolyakov.tetrisclockble.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.agolyakov.tetrisclockble.data.model.github.GithubRelease
import net.agolyakov.tetrisclockble.data.remote.api.GitHubApiService
import java.io.IOException
import javax.inject.Inject

class GitHubRepository @Inject constructor(
    private val githubApiService: GitHubApiService
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