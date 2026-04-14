package com.example.brazzers;

/**
 * Centralized API configuration.
 *
 * TODO: For production, move this key to a secure backend proxy.
 *       The app should call YOUR server, which then calls Gemini.
 *       Never ship API keys in production APKs.
 */
public final class ApiConfig {
    private ApiConfig() {}

    // Gemini API key — replace or proxy in production
    public static final String GEMINI_API_KEY = "AIzaSyCB5TdSVk6WTyIGqYanUHpQCI9M7-BdPXg";

    public static final String GEMINI_MODEL = "gemini-1.5-flash";

    public static String getGeminiUrl() {
        return "https://generativelanguage.googleapis.com/v1beta/models/"
                + GEMINI_MODEL + ":generateContent?key=" + GEMINI_API_KEY;
    }
}
