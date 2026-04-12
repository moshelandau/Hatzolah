package com.hatzolah.app.data.repository

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time responder sync via Firestore.
 *
 * Document model:
 *   /dispatches/{cad}
 *     - address, callType, units, updatedAt
 *   /dispatches/{cad}/responders/{memberId}
 *     - memberId, unitNumber, name, phone, status ("BLUE"|"GREEN"|"RED"),
 *       latitude, longitude, updatedAt
 *
 * This class degrades gracefully when Firebase is NOT configured (no
 * google-services.json). All methods become no-ops and [observeResponders]
 * emits an empty list. Check [isAvailable] to know whether Firebase is set up.
 */
@Singleton
class FirebaseResponderRepository @Inject constructor() {

    companion object {
        private const val TAG = "FirebaseResponderRepo"
        private const val COLLECTION_DISPATCHES = "dispatches"
        private const val COLLECTION_RESPONDERS = "responders"
    }

    data class Responder(
        val memberId: Long = 0,
        val unitNumber: String = "",
        val name: String = "",
        val phone: String = "",
        val status: String = "BLUE", // BLUE | GREEN | RED
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val updatedAt: Long = 0L
    )

    private val firestore: FirebaseFirestore? by lazy {
        try {
            // Will throw IllegalStateException if no google-services.json
            FirebaseApp.getInstance()
            FirebaseFirestore.getInstance()
        } catch (_: Throwable) {
            Log.w(TAG, "Firebase not configured - real-time responder sync disabled")
            null
        }
    }

    val isAvailable: Boolean get() = firestore != null

    /**
     * Signs in anonymously if Firebase Auth is configured and no user yet.
     * Safe to call repeatedly. No-op if Firebase is unavailable.
     */
    suspend fun ensureAuth() {
        if (firestore == null) return
        try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
        } catch (_: Throwable) { /* swallow — anonymous auth may be disabled in console */ }
    }

    /**
     * Adds or updates the local responder on the given CAD call.
     */
    suspend fun joinCall(
        cad: String,
        memberId: Long,
        unitNumber: String,
        name: String,
        phone: String,
        status: String = "BLUE"
    ) {
        if (cad.isBlank() || memberId <= 0) return
        val db = firestore ?: return
        try {
            val data = mapOf(
                "memberId" to memberId,
                "unitNumber" to unitNumber,
                "name" to name,
                "phone" to phone,
                "status" to status,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_DISPATCHES).document(cad)
                .collection(COLLECTION_RESPONDERS).document(memberId.toString())
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Throwable) {
            Log.w(TAG, "joinCall failed: ${e.message}")
        }
    }

    /**
     * Updates the responder's status and location for the given CAD.
     */
    suspend fun updateStatus(
        cad: String,
        memberId: Long,
        status: String,
        lat: Double,
        lng: Double
    ) {
        if (cad.isBlank() || memberId <= 0) return
        val db = firestore ?: return
        try {
            val data = mapOf(
                "status" to status,
                "latitude" to lat,
                "longitude" to lng,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_DISPATCHES).document(cad)
                .collection(COLLECTION_RESPONDERS).document(memberId.toString())
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Throwable) {
            Log.w(TAG, "updateStatus failed: ${e.message}")
        }
    }

    /**
     * Removes the local responder from the given CAD call (e.g. on dismiss).
     */
    suspend fun leaveCall(cad: String, memberId: Long) {
        if (cad.isBlank() || memberId <= 0) return
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_DISPATCHES).document(cad)
                .collection(COLLECTION_RESPONDERS).document(memberId.toString())
                .delete()
                .await()
        } catch (e: Throwable) {
            Log.w(TAG, "leaveCall failed: ${e.message}")
        }
    }

    /**
     * Observes responders for a CAD in real time. Emits an empty list when
     * Firebase is not configured.
     */
    fun observeResponders(cad: String): Flow<List<Responder>> {
        if (cad.isBlank()) return flowOf(emptyList())
        val db = firestore ?: return flowOf(emptyList())
        return callbackFlow {
            val registration: ListenerRegistration = db.collection(COLLECTION_DISPATCHES)
                .document(cad)
                .collection(COLLECTION_RESPONDERS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            Responder(
                                memberId = (doc.getLong("memberId") ?: 0L),
                                unitNumber = doc.getString("unitNumber") ?: "",
                                name = doc.getString("name") ?: "",
                                phone = doc.getString("phone") ?: "",
                                status = doc.getString("status") ?: "BLUE",
                                latitude = doc.getDouble("latitude") ?: 0.0,
                                longitude = doc.getDouble("longitude") ?: 0.0,
                                updatedAt = doc.getLong("updatedAt") ?: 0L
                            )
                        } catch (_: Throwable) { null }
                    }.orEmpty()
                    trySend(list)
                }
            awaitClose { registration.remove() }
        }
    }
}
