package net.agolyakov.tetrisclockble.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.agolyakov.tetrisclockble.data.BleDeviceRepository
import net.agolyakov.tetrisclockble.preferences.DevicePreferences
import net.agolyakov.tetrisclockble.service.BluetoothAdapterProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MainModule {

    @Provides
    @Singleton
    fun provideBleDeviceRepository(
        @ApplicationContext context: Context
    ) = BleDeviceRepository()

    @Provides
    @Singleton
    fun provideBluetoothAdapterProvider(
        @ApplicationContext context: Context
    ) = BluetoothAdapterProvider(context = context)

    @Provides
    @Singleton
    fun provideDevicePreferences(
        @ApplicationContext context: Context
    ): DevicePreferences {
        return DevicePreferences(context)
    }
}