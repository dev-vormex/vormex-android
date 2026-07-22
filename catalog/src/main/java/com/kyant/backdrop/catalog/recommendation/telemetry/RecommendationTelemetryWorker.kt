package com.kyant.backdrop.catalog.recommendation.telemetry

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.RecommendationApiService
import com.kyant.backdrop.catalog.network.models.RecommendationEventsRequest
import java.util.concurrent.TimeUnit

class RecommendationTelemetryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    private val repository = RecommendationTelemetryRepository(appContext)

    override suspend fun doWork(): Result {
        val ownerUserId = ApiClient.getCurrentUserId(applicationContext)?.trim()?.takeIf { it.isNotEmpty() }
            ?: return Result.success()
        val batch = repository.nextBatch(ownerUserId)
        if (batch.isEmpty()) return Result.success()
        val eventIds = batch.map { it.eventId }
        repository.recordAttempt(eventIds)
        return RecommendationApiService.sendEvents(
            applicationContext,
            RecommendationEventsRequest(batch.map { it.toNetworkModel() }),
            expectedUserId = ownerUserId
        ).fold(
            onSuccess = {
                // A successful response is terminal for this idempotent batch; rejected rows are invalid.
                repository.delete(eventIds)
                if (repository.nextBatch(ownerUserId).isNotEmpty()) enqueue(applicationContext, immediate = true)
                Result.success()
            },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        private const val WORK_NAME = "recommendation-telemetry-outbox"

        fun enqueue(context: Context, immediate: Boolean = false) {
            val builder = OneTimeWorkRequestBuilder<RecommendationTelemetryWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            if (!immediate) builder.setInitialDelay(10, TimeUnit.SECONDS)
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                WORK_NAME,
                if (immediate) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                builder.build()
            )
        }
    }
}
