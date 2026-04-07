package com.hatzolah.app.di

import android.content.Context
import androidx.room.Room
import com.hatzolah.app.data.database.HatzolahDatabase
import com.hatzolah.app.data.database.dao.CallLogDao
import com.hatzolah.app.data.database.dao.HospitalDao
import com.hatzolah.app.data.database.dao.MemberDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HatzolahDatabase {
        return Room.databaseBuilder(
            context,
            HatzolahDatabase::class.java,
            "hatzolah_database"
        ).build()
    }

    @Provides
    fun provideMemberDao(database: HatzolahDatabase): MemberDao = database.memberDao()

    @Provides
    fun provideHospitalDao(database: HatzolahDatabase): HospitalDao = database.hospitalDao()

    @Provides
    fun provideCallLogDao(database: HatzolahDatabase): CallLogDao = database.callLogDao()
}
