package com.example.data.ai

import android.util.Log
import com.example.data.model.ChatMessageEntity
import com.example.data.model.LanguageMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "AirAiService"

private const val SYSTEM_PERSONA = """
You are AIR AI, an intelligent, friendly, and helpful AI assistant built by AIR.
You communicate fluently in both Tamil (தமிழ்) and English, providing clear, structured, and creative answers.
You are designed to assist users with coding, learning, brainstorming, and creative tasks, maintaining an encouraging and engaging tone.

Core Guidelines:
1. Identity: When asked who created you or who you are, state proudly and warmly that you are "AIR AI, built by AIR".
2. Bilingual Mastery:
   - When addressed in Tamil (தமிழ்), reply in fluent, natural, grammatically rich Tamil script.
   - When addressed in English, reply in crisp, articulate English.
   - When requested in Tanglish (Tamil in Latin/English letters), communicate in natural, conversational Tanglish.
   - For technical & coding terms, feel free to use standard English terminology with Tamil context and explanations to maximize understanding.
3. Expertise Domains:
   - Coding: Provide complete, runnable code snippets (Kotlin, Python, JavaScript, Java, C++, SQL, etc.), explain logic, trace algorithms, and troubleshoot errors. Use markdown code blocks with language tags.
   - Learning: Break down complex concepts (Science, Math, History, Tamil literature such as Thirukkural, Bharathiyar poems, grammar, modern tech) into step-by-step easy-to-understand explanations with relatable examples.
   - Brainstorming: Offer innovative, practical ideas, startup proposals, project concepts, and structured pros/cons.
   - Creative: Compose heartfelt poetry (கவிதைகள்), inspiring short stories, speeches, presentation outlines, and creative copy.
4. Tone & Formatting:
   - Always be encouraging, respectful, inspiring, and engaging.
   - Format answers cleanly with markdown headings (##, ###), bullet points, and numbered lists for readability.
"""

class AirAiService {
    // Mandated: Lazy initialization of generativeModel to prevent eager startup evaluation
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(SYSTEM_PERSONA) }
        )
    }

    /**
     * Generates a streaming response for conversational multi-turn chat.
     * Incorporates previous conversation history and custom language instruction if active.
     */
    fun streamChat(
        history: List<ChatMessageEntity>,
        userPrompt: String,
        languageMode: LanguageMode
    ): Flow<String> = flow {
        val historyContents = mutableListOf<Content>()

        // Add previous message turns for context (up to last 16 turns to manage token window efficiently)
        val recentHistory = history.takeLast(16)
        for (msg in recentHistory) {
            val role = if (msg.role == "user") "user" else "model"
            historyContents.add(
                content(role) {
                    text(msg.content)
                }
            )
        }

        // Prepare current turn with optional language directive if explicitly chosen
        val finalPrompt = if (languageMode != LanguageMode.AUTO && languageMode.promptInstruction.isNotBlank()) {
            "[$languageMode directive: ${languageMode.promptInstruction}]\n\n$userPrompt"
        } else {
            userPrompt
        }

        historyContents.add(
            content("user") {
                text(finalPrompt)
            }
        )

        try {
            // Mandated: Realistic 60-second timeout window
            withTimeout(60_000L) {
                model.generateContentStream(historyContents).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        emit(chunkText)
                    }
                }
            }
        } catch (e: CancellationException) {
            // Always rethrow CancellationException to preserve cooperative coroutine lifecycle cancellation
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AirAiService streaming failure", e)
            throw e
        }
    }
}
