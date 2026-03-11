package com.example.ihm.network

interface GeminiApi {
    suspend fun processNaturalLanguage(input: String): String
}
