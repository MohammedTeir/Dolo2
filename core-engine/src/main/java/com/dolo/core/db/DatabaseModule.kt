package com.dolo.core.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDoloDatabase(
        @ApplicationContext context: Context
    ): DoloDatabase {
        return Room.databaseBuilder(
            context,
            DoloDatabase::class.java,
            "dolo_database.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideDownloadDao(database: DoloDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    fun provideLibraryItemDao(database: DoloDatabase): LibraryItemDao {
        return database.libraryItemDao()
    }
}
