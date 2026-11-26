package com.example.cci.data.remote

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): RuntimeException {
    val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
    val providerInfo = JSONArray().apply {
        currentUser?.providerData?.forEach { provider ->
            put(
                JSONObject().apply {
                    put("providerId", provider.providerId)
                    put("displayName", provider.displayName ?: JSONObject.NULL)
                    put("email", provider.email ?: JSONObject.NULL)
                    put("photoUrl", provider.photoUrl?.toString() ?: JSONObject.NULL)
                }
            )
        }
    }

    val payload = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path ?: JSONObject.NULL)
        put(
            "authInfo",
            JSONObject().apply {
                put("userId", currentUser?.uid ?: JSONObject.NULL)
                put("email", currentUser?.email ?: JSONObject.NULL)
                put("emailVerified", currentUser?.isEmailVerified ?: false)
                put("isAnonymous", currentUser?.isAnonymous ?: false)
                put("tenantId", currentUser?.tenantId ?: JSONObject.NULL)
                put("providerInfo", providerInfo)
            }
        )
    }

    val jsonString = payload.toString()
    Log.e("FirestoreError", jsonString, exception)
    return RuntimeException("Firestore Error: $jsonString", exception)
}
