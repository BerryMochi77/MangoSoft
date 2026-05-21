package com.example.comp2100miniproject.ai;

/** Type of user feedback on an AI curation decision. */
public enum AIFeedbackType {
    /** User says "AI wrongly recommended this post to me — not relevant." */
    NOT_RELEVANT,
    /** User says "AI wrongly hid this post — I want to see it." */
    SHOW_ANYWAY
}
