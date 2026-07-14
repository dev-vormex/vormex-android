package com.kyant.backdrop.catalog.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kyant.backdrop.catalog.network.models.ProximitySample
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import java.util.UUID
import kotlin.coroutines.resume

object CrossedPathsLocationSampler {
    suspend fun sample(context: Context): Result<ProximitySample> = suspendCancellableCoroutine { continuation ->
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) { continuation.resume(Result.failure(SecurityException("Location permission denied"))); return@suspendCancellableCoroutine }
        val cancellation = CancellationTokenSource(); continuation.invokeOnCancellation { cancellation.cancel() }
        LocationServices.getFusedLocationProviderClient(context).getCurrentLocation(
            if (fine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token
        ).addOnSuccessListener { location ->
            val now = System.currentTimeMillis()
            if (location == null || !location.latitude.isFinite() || !location.longitude.isFinite() || location.accuracy > 100f ||
                location.time < now - 5 * 60_000L || location.time > now + 30_000L) {
                continuation.resume(Result.failure(IllegalStateException("A fresh location with 100 m accuracy is required")))
            } else continuation.resume(Result.success(ProximitySample(UUID.randomUUID().toString(), Instant.ofEpochMilli(location.time).toString(),
                location.latitude, location.longitude, location.accuracy.toDouble(), location.speed.takeIf { location.hasSpeed() }?.toDouble())))
        }.addOnFailureListener { continuation.resume(Result.failure(it)) }
    }
}
