package net.agolyakov.tetrisclockble.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.agolyakov.tetrisclockble.data.local.TetrisClockPreferences
import net.agolyakov.tetrisclockble.data.repository.DeviceRepository
import net.agolyakov.tetrisclockble.data.repository.FirmwareRepository
import net.agolyakov.tetrisclockble.data.repository.GitHubRepository
import net.agolyakov.tetrisclockble.domain.repository.PreferencesRepository
import net.agolyakov.tetrisclockble.domain.usecase.LoadDeviceWithNameUseCase
import net.agolyakov.tetrisclockble.service.bluetooth.BluetoothAdapterProvider
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager
import net.agolyakov.tetrisclockble.service.bluetooth.BluetoothService
import net.agolyakov.tetrisclockble.data.remote.api.GitHubApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MainModule {

    // Region: Context Providers
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    // Region: Bluetooth Dependencies
    @Provides
    @Singleton
    fun provideBluetoothAdapterProvider(
        @ApplicationContext context: Context
    ): BluetoothAdapterProvider = BluetoothAdapterProvider(context)

    @Provides
    @Singleton
    fun provideBluetoothService(
        bluetoothAdapterProvider: BluetoothAdapterProvider
    ): BluetoothService = BluetoothService(bluetoothAdapterProvider)

    // Region: Preferences
    @Provides
    @Singleton
    fun provideTetrisClockPreferences(
        @ApplicationContext context: Context
    ): TetrisClockPreferences = TetrisClockPreferences(context)

    @Provides
    @Singleton
    fun providePreferencesRepository(
        preferences: TetrisClockPreferences
    ): PreferencesRepository = preferences

    // Region: Repositories
    @Provides
    @Singleton
    fun provideDeviceRepository(): DeviceRepository = DeviceRepository()

    @Provides
    @Singleton
    fun provideFirmwareRepository(
        bluetoothService: BluetoothService
    ): FirmwareRepository = FirmwareRepository(bluetoothService)

    @Provides
    @Singleton
    fun provideGitHubRepository(
        githubApiService: GitHubApiService
    ): GitHubRepository = GitHubRepository(githubApiService)

    // Region: Use Cases
    @Provides
    @Singleton
    fun provideLoadDeviceWithNameUseCase(
        preferencesRepository: PreferencesRepository
    ): LoadDeviceWithNameUseCase = LoadDeviceWithNameUseCase(preferencesRepository)

    // Region: Network
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideGitHubApiService(retrofit: Retrofit): GitHubApiService =
        retrofit.create(GitHubApiService::class.java)
}