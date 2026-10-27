package com.moovieflix.app.features.profiles

internal expect object ProfileStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}