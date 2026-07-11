package net.agolyakov.tetrisclockble.data.remote.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class GitHubRequestInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", "TetrisClock-Android-App") // GitHub requires it
            .build()

        return chain.proceed(request)
    }
}
