package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector

enum class PromptCategory(
    val id: String,
    val titleEn: String,
    val titleTa: String,
    val description: String,
    val samplePrompts: List<SamplePrompt>
) {
    CODING(
        id = "coding",
        titleEn = "Coding",
        titleTa = "நிரலாக்கம்",
        description = "Code generation, debugging, algorithms & architecture",
        samplePrompts = listOf(
            SamplePrompt("Kotlin Coroutines", "Explain Kotlin Coroutines and StateFlow with a simple, clear Android example."),
            SamplePrompt("பைதான் அடிப்படை", "பைத்தானில் Functions மற்றும் Loops எப்படி வேலை செய்கிறது? எளிய உதாரணத்துடன் விளக்குங்கள்."),
            SamplePrompt("Debug Error", "How to fix 'NullPointerException' and what are safe practices to avoid it?"),
            SamplePrompt("SQL vs NoSQL", "SQL மற்றும் NoSQL தரவுத்தளங்களுக்கு இடையேயான முக்கிய வேறுபாடுகளை விளக்குங்கள்.")
        )
    ),
    LEARNING(
        id = "learning",
        titleEn = "Learning",
        titleTa = "கற்றல்",
        description = "Concepts, literature, math, science & knowledge",
        samplePrompts = listOf(
            SamplePrompt("திருக்குறள் விளக்கம்", "திருக்குறள் 'கற்க கசடறக் கற்பவை...' (குறள் 391) விளக்கத்தையும் அதன் நடைமுறைப் பயனையும் கூறுங்கள்."),
            SamplePrompt("Machine Learning 101", "Explain Machine Learning and Neural Networks using an intuitive real-world analogy."),
            SamplePrompt("பாரதியார் பாடல்", "மகாகவி பாரதியாரின் 'அச்சமில்லை அச்சமில்லை' பாடலின் கருத்து மற்றும் உத்வேகத்தை விளக்குங்கள்."),
            SamplePrompt("Quantum Computing", "குவாண்டம் கம்ப்யூட்டிங் என்றால் என்ன? சாதாரண கணினியிலிருந்து இது எப்படி வேறுபடுகிறது?")
        )
    ),
    BRAINSTORMING(
        id = "brainstorming",
        titleEn = "Brainstorming",
        titleTa = "சிந்தனை",
        description = "Creative ideas, startup blueprints, and strategies",
        samplePrompts = listOf(
            SamplePrompt("AI App Ideas", "Brainstorm 5 innovative AI app ideas tailored for students and creators in Tamil Nadu."),
            SamplePrompt("Hackathon Project", "Give me 3 impactful hackathon project ideas tackling education or sustainability."),
            SamplePrompt("ஸ்டார்ட்-அப் யோசனை", "உள்ளூர் கைவினைஞர்கள் மற்றும் விவசாயிகளுக்கு உதவும் ஒரு புதுமையான மொபைல் ஆப் ஐடியா கூறுங்கள்."),
            SamplePrompt("Career Roadmap", "Suggest a 6-month roadmap to transition into modern Mobile & AI Development.")
        )
    ),
    CREATIVE(
        id = "creative",
        titleEn = "Creative",
        titleTa = "படைப்பாற்றல்",
        description = "Poetry, stories, speeches & engaging content",
        samplePrompts = listOf(
            SamplePrompt("தமிழ் கவிதை", "இயற்கை அழகு, மழைத்துளி மற்றும் நம்பிக்கை பற்றிய ஓர் அழகிய தமிழ் கவிதை இயற்றித் தாருங்கள்."),
            SamplePrompt("Motivational Speech", "Write a 2-minute inspiring speech for a college tech symposium about embracing innovation."),
            SamplePrompt("Short Story", "Write a captivating sci-fi short story set in Chennai in the year 2080."),
            SamplePrompt("மேடைப் பேச்சு", "மாணவர்களின் தன்னம்பிக்கை மற்றும் தலைமைப் பண்பு பற்றி 3 நிமிட தமிழ் உரை எழுதுங்கள்.")
        )
    );

    val icon: ImageVector
        get() = when (this) {
            CODING -> Icons.Default.Code
            LEARNING -> Icons.Default.School
            BRAINSTORMING -> Icons.Default.Lightbulb
            CREATIVE -> Icons.Default.AutoAwesome
        }
}

data class SamplePrompt(
    val title: String,
    val prompt: String
)

enum class LanguageMode(val id: String, val label: String, val promptInstruction: String) {
    AUTO("auto", "Auto / இயல்பான", ""),
    TAMIL("tamil", "தமிழ்", "Please answer purely and elegantly in Tamil (தமிழ் எழுத்து வடிவம்)."),
    TANGLISH("tanglish", "Tanglish", "Please answer in friendly Tanglish (Tamil written in English/Latin alphabets, e.g. 'Vanakam, epdi irukinga? Idhoda meaning enna na...')."),
    ENGLISH("english", "English", "Please answer in clear, structured, and engaging English.")
}
