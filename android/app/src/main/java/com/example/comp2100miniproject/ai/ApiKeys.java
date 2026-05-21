package com.example.comp2100miniproject.ai;

/**
 * Centralised home for third-party API credentials.
 *
 * <p><b>Security note (Hackathon 2):</b> the DeepSeek key below is committed
 * to the team repository on purpose so every teammate can run the AI tab
 * without per-developer setup. The key will be rotated immediately after
 * grading. <b>Do not</b> reuse this pattern for production code — switch to
 * {@code local.properties} + {@code BuildConfig} fields, a remote
 * secrets-store fetch, or a backend proxy that holds the real key.</p>
 *
 * <p>To rotate / disable the AI tab, just blank out {@link #DEEPSEEK_API_KEY}
 * and {@link AiPostCurationStrategy} will fail fast with a clear "no key"
 * error instead of leaking traffic.</p>
 */
public final class ApiKeys {

    /**
     * Project-wide DeepSeek key. Hackathon-only — rotate after grading.
     */
    public static final String DEEPSEEK_API_KEY =
            "sk-3d14ce133efa4a30af16e09b0356ab73";

    /** OpenAI-compatible chat completions endpoint exposed by DeepSeek. */
    public static final String DEEPSEEK_CHAT_COMPLETIONS_URL =
            "https://api.deepseek.com/v1/chat/completions";

    /**
     * Model used by {@link AiPostCurationStrategy}. Changing models is a
     * one-line edit here — no other code needs to know.
     */
    public static final String DEEPSEEK_DEFAULT_MODEL = "deepseek-v4-flash";

    private ApiKeys() {
    }
}
