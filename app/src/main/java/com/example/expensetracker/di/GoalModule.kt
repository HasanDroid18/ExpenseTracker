package com.example.expensetracker.di

import android.content.Context
import com.example.expensetracker.AppScreens.Goals.ExpenseGoalDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing goal-related dependencies
 * Provides ExpenseGoalDataStore as a singleton
 */
@Module
@InstallIn(SingletonComponent::class)
object GoalModule {

    /**
     * Provide ExpenseGoalDataStore singleton
     * @param context Application context
     * @return ExpenseGoalDataStore instance
     */
    @Provides
    @Singleton
    fun provideExpenseGoalDataStore(
        @ApplicationContext context: Context
    ): ExpenseGoalDataStore {
        return ExpenseGoalDataStore(context)
    }
}

