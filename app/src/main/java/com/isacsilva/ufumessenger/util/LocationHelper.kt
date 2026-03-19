package com.isacsilva.ufumessenger.util

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

@SuppressLint("MissingPermission") // Só chamamos essa função após o usuário aprovar a tela de permissão
fun getCurrentLocation(
    context: Context,
    onLocationFetched: (latitude: Double, longitude: Double) -> Unit,
    onError: (Exception) -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Pede a localização atualizada em alta precisão
    fusedLocationClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        CancellationTokenSource().token
    ).addOnSuccessListener { location ->
        if (location != null) {
            onLocationFetched(location.latitude, location.longitude)
        } else {
            onError(Exception("Localização não encontrada. O GPS está ligado?"))
        }
    }.addOnFailureListener { exception ->
        onError(exception)
    }
}