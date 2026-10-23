package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AirAiService
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import com.example.data.model.LanguageMode
import com.example.data.model.PromptCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "ChatViewModel"

sealed interface GenerationState {
    data object Idle : GenerationState
    data object Loading : GenerationState
    data class Streaming(val partialText: String) : GenerationState
    data class Error(val message: String) : GenerationState
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val chatDao = database.chatDao()
    private val aiService = AirAiService()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    val sessions: StateFlow<List<ChatSessionEntity>> = chatDao.getAllSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedMessages: StateFlow<List<ChatMessageEntity>> = chatDao.getBookmarkedMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    private val _languageMode = MutableStateFlow(LanguageMode.AUTO)
    val languageMode: StateFlow<LanguageMode> = _languageMode.asStateFlow()

    private val _selectedCategory = MutableStateFlow(PromptCategory.CODING)
    val selectedCategory: StateFlow<PromptCategory> = _selectedCategory.asStateFlow()

    private var generationJob: Job? = null
    private var observeMessagesJob: Job? = null

    init {
        initializeSession()
    }

    private fun initializeSession() {
        viewModelScope.launch {
            val sessionsList = chatDao.getAllSessionsFlow()
            // Check existing or create first session
            val count = chatDao.getSessionCount()
            if (count == 0) {
                createNewSession(PromptCategory.CODING, "Welcome to AIR AI")
            } else {
                val current = _currentSessionId.value
                if (current == null) {
                    val firstSession = database.chatDao().getAllSessionsFlow()
                    // Will be set once flow collects or create
                    createNewSession(PromptCategory.CODING, "New Chat")
                }
            }
        }
    }

    fun selectSession(sessionId: String) {
        if (_currentSessionId.value == sessionId) return
        _currentSessionId.value = sessionId
        observeMessages(sessionId)
    }

    private fun observeMessages(sessionId: String) {
        observeMessagesJob?.cancel()
        observeMessagesJob = viewModelScope.launch {
            chatDao.getMessagesForSessionFlow(sessionId).collect { list ->
                _messages.value = list
            }
        }
    }

    fun createNewSession(category: PromptCategory = _selectedCategory.value, title: String = "New Chat") {
        viewModelScope.launch {
            val newSession = ChatSessionEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                category = category.id,
                createdAt = System.currentTimeMillis(),
                lastUpdatedAt = System.currentTimeMillis()
            )
            chatDao.insertSession(newSession)
            _currentSessionId.value = newSession.id
            observeMessages(newSession.id)

            // Insert friendly greeting message
            val welcomeText = when (category) {
                PromptCategory.CODING -> "Vanakkam! I'm **AIR AI**, your coding and tech companion built by **AIR**.\n\nAsk me anything about Kotlin, Python, debugging, architecture, or algorithmic problem solving in Tamil or English! 🚀"
                PromptCategory.LEARNING -> "Vanakkam! I'm **AIR AI**, built by **AIR** to help you learn and explore knowledge.\n\nWhether it's science, mathematics, world history, or Tamil literature like **திருக்குறள்** and **பாரதியார் பாடல்கள்**, I'm here for you! 📚"
                PromptCategory.BRAINSTORMING -> "Vanakkam! I'm **AIR AI**, built by **AIR** to spark your creativity and strategic ideas.\n\nLet's brainstorm startup concepts, hackathon projects, innovative apps, or career blueprints! 💡"
                PromptCategory.CREATIVE -> "Vanakkam! I'm **AIR AI**, built by **AIR** for creative writing and artistic expression.\n\nAsk me for heartfelt Tamil poems (கவிதைகள்), inspiring short stories, stage speeches (மேடைப் பேச்சு), or engaging scripts! ✍️"
            }

            val welcomeMessage = ChatMessageEntity(
                sessionId = newSession.id,
                role = "model",
                content = welcomeText,
                category = category.id,
                languageMode = _languageMode.value.id
            )
            chatDao.insertMessage(welcomeMessage)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatDao.deleteMessagesForSession(sessionId)
            chatDao.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                createNewSession()
            }
        }
    }

    fun setLanguageMode(mode: LanguageMode) {
        _languageMode.value = mode
    }

    fun setCategory(category: PromptCategory) {
        _selectedCategory.value = category
    }

    fun toggleBookmark(message: ChatMessageEntity) {
        viewModelScope.launch {
            chatDao.setBookmark(message.id, !message.isBookmarked)
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        _generationState.value = GenerationState.Idle
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val sessionId = _currentSessionId.value ?: return

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val userMsg = ChatMessageEntity(
                sessionId = sessionId,
                role = "user",
                content = trimmed,
                category = _selectedCategory.value.id,
                languageMode = _languageMode.value.id
            )
            chatDao.insertMessage(userMsg)

            // Update session title if it's the first user message
            val currentMsgs = _messages.value
            if (currentMsgs.none { it.role == "user" }) {
                val previewTitle = if (trimmed.length > 32) trimmed.take(29) + "..." else trimmed
                chatDao.getSessionById(sessionId)?.let { session ->
                    chatDao.updateSession(session.copy(title = previewTitle, lastUpdatedAt = System.currentTimeMillis()))
                }
            } else {
                chatDao.getSessionById(sessionId)?.let { session ->
                    chatDao.updateSession(session.copy(lastUpdatedAt = System.currentTimeMillis()))
                }
            }

            _generationState.value = GenerationState.Loading
            val accumulated = StringBuilder()

            try {
                // Collect streaming chunks from Firebase AI Logic
                val currentHistory = chatDao.getMessagesForSession(sessionId)
                aiService.streamChat(
                    history = currentHistory,
                    userPrompt = trimmed,
                    languageMode = _languageMode.value
                ).collect { chunk ->
                    accumulated.append(chunk)
                    _generationState.value = GenerationState.Streaming(accumulated.toString())
                }

                val finalContent = accumulated.toString().trim()
                if (finalContent.isNotEmpty()) {
                    val aiMsg = ChatMessageEntity(
                        sessionId = sessionId,
                        role = "model",
                        content = finalContent,
                        category = _selectedCategory.value.id,
                        languageMode = _languageMode.value.id
                    )
                    chatDao.insertMessage(aiMsg)
                    _generationState.value = GenerationState.Idle
                } else {
                    _generationState.value = GenerationState.Error("AIR AI received an empty response. Please try again.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AIR AI request timed out", e)
                _generationState.value = GenerationState.Error("Request timed out after 60 seconds. Please check your connection and try again.")
            } catch (e: CancellationException) {
                // Cooperative coroutine cancellation
                _generationState.value = GenerationState.Idle
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AIR AI generation error", e)
                val errorMessage = e.localizedMessage ?: "Failed to generate response."
                _generationState.value = GenerationState.Error(errorMessage)
            }
        }
    }
}
