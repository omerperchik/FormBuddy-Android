package com.formbuddy.android.di

import android.content.Context
import com.formbuddy.android.data.local.encryption.EncryptionManager
import com.formbuddy.android.data.local.preferences.PreferencesManager
import com.formbuddy.android.data.remote.firebase.FirebaseManager
import com.formbuddy.android.domain.analysis.FillablePdfParser
import com.formbuddy.android.domain.analysis.FieldTypeInferencer
import com.formbuddy.android.domain.analysis.FormAnalyzer
import com.formbuddy.android.domain.analysis.GeminiFieldRefiner
import com.formbuddy.android.domain.analysis.OcrService
import com.formbuddy.android.domain.analysis.PdfExporter
import com.formbuddy.android.domain.filling.ConversationManager
import com.formbuddy.android.domain.filling.ResponseClassifier
import com.formbuddy.android.domain.speech.SpeechRecognitionService
import com.formbuddy.android.domain.tts.TextToSpeechService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseManager(): FirebaseManager {
        return FirebaseManager()
    }

    @Provides
    @Singleton
    fun provideFillablePdfParser(): FillablePdfParser {
        return FillablePdfParser()
    }

    @Provides
    @Singleton
    fun provideOcrService(@ApplicationContext context: Context): OcrService {
        return OcrService(context)
    }

    @Provides
    @Singleton
    fun provideFieldTypeInferencer(): FieldTypeInferencer {
        return FieldTypeInferencer()
    }

    @Provides
    @Singleton
    fun provideGeminiFieldRefiner(): GeminiFieldRefiner {
        return GeminiFieldRefiner()
    }

    @Provides
    @Singleton
    fun provideFormAnalyzer(
        fillablePdfParser: FillablePdfParser,
        ocrService: OcrService,
        fieldTypeInferencer: FieldTypeInferencer,
        geminiFieldRefiner: GeminiFieldRefiner,
        firebaseManager: FirebaseManager,
        @ApplicationContext context: Context
    ): FormAnalyzer {
        return FormAnalyzer(fillablePdfParser, ocrService, fieldTypeInferencer, geminiFieldRefiner, firebaseManager, context)
    }

    @Provides
    @Singleton
    fun providePdfExporter(@ApplicationContext context: Context): PdfExporter {
        return PdfExporter(context)
    }

    @Provides
    @Singleton
    fun provideConversationManager(): ConversationManager {
        return ConversationManager()
    }

    @Provides
    @Singleton
    fun provideResponseClassifier(): ResponseClassifier {
        return ResponseClassifier()
    }

    @Provides
    @Singleton
    fun provideTextToSpeechService(
        @ApplicationContext context: Context,
        firebaseManager: FirebaseManager,
        preferencesManager: PreferencesManager
    ): TextToSpeechService {
        return TextToSpeechService(context, firebaseManager, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideSpeechRecognitionService(@ApplicationContext context: Context): SpeechRecognitionService {
        return SpeechRecognitionService(context)
    }
}
