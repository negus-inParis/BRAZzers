package com.example.brazzers;

/**
 * Centralized API configuration.
 *
 * Using Groq — 100% FREE, no credit card required.
 * Sign up at https://console.groq.com and create an API key.
 * Free tier: 30 requests/min, 14,400 requests/day.
 *
 * TODO: For production, move this key to a secure backend proxy.
 *       Never ship API keys in production APKs.
 */
public final class ApiConfig {
    private ApiConfig() {}

    // Groq API key — get yours FREE at https://console.groq.com/keys
    // ⚠️ Replace the placeholder below with your own Groq API key before running.
    public static final String API_KEY = "YOUR_GROQ_API_KEY_HERE";

    // Llama 3.3 70B: powerful, fast, and completely free
    public static final String MODEL = "llama-3.3-70b-versatile";

    public static String getApiUrl() {
        return "https://api.groq.com/openai/v1/chat/completions";
    }
}
