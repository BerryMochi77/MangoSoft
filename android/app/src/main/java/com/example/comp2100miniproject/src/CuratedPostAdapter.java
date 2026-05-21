package com.example.comp2100miniproject.src;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.R;
import com.example.comp2100miniproject.ai.CuratedPost;

import java.util.ArrayList;
import java.util.List;

import dao.model.Post;
import hashtag.HashtagParser;

/**
 * Adapter for the AI-curated post list. Rows show the post title (with
 * hashtags stripped) + the AI's one-line summary + score; tapping any
 * row hands the {@link Post} back to the host via {@link OnPostClick}.
 *
 * Each row also has a "Not relevant" button so users can flag AI mistakes.
 * Tapping it calls {@link OnFeedbackClick} which is wired to
 * {@link com.example.comp2100miniproject.ai.AIAnalyticsRepository} via AiFragment.
 */
public class CuratedPostAdapter
        extends RecyclerView.Adapter<CuratedPostAdapter.ViewHolder> {

    public interface OnPostClick {
        void onPostClick(Post post);
    }

    /** Called when user taps "Not relevant" on a shown curated post. */
    public interface OnFeedbackClick {
        void onFeedback(Post post);
    }

    private final CuratedPost[] items;
    private final OnPostClick   listener;
    private final OnFeedbackClick feedbackListener;

    /** Legacy constructor — feedback listener is null (no feedback button). */
    public CuratedPostAdapter(List<CuratedPost> items, OnPostClick listener) {
        this(items, listener, null);
    }

    public CuratedPostAdapter(List<CuratedPost> items, OnPostClick listener,
                              OnFeedbackClick feedbackListener) {
        this.items = items == null
                ? new CuratedPost[0]
                : items.toArray(new CuratedPost[0]);
        this.listener         = listener;
        this.feedbackListener = feedbackListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView summary;
        private final TextView score;
        private final Button   buttonNotRelevant;

        public ViewHolder(View view) {
            super(view);
            title            = view.findViewById(R.id.textCuratedTitle);
            summary          = view.findViewById(R.id.textCuratedSummary);
            score            = view.findViewById(R.id.textCuratedScore);
            buttonNotRelevant = view.findViewById(R.id.buttonNotRelevant);
        }

        void bind(CuratedPost item, OnPostClick listener, OnFeedbackClick feedbackListener) {
            String topic = item.post == null || item.post.topic == null
                    ? "" : item.post.topic;
            title.setText(HashtagParser.stripTags(topic));
            String summaryText = item.summary == null || item.summary.isEmpty()
                    ? itemView.getContext().getString(R.string.ai_summary_prefix, "")
                    : itemView.getContext().getString(R.string.ai_summary_prefix, item.summary);
            summary.setText(summaryText);
            score.setText(itemView.getContext().getString(R.string.ai_score_label, item.score));

            itemView.setOnClickListener(v -> {
                if (listener != null && item.post != null) listener.onPostClick(item.post);
            });

            if (buttonNotRelevant != null) {
                if (feedbackListener != null && item.post != null) {
                    buttonNotRelevant.setVisibility(View.VISIBLE);
                    buttonNotRelevant.setOnClickListener(v ->
                            feedbackListener.onFeedback(item.post));
                } else {
                    buttonNotRelevant.setVisibility(View.GONE);
                }
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_curated_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items[position], listener, feedbackListener);
    }

    @Override
    public int getItemCount() {
        return items.length;
    }

    /** Convenience: build an adapter from already-filtered items. */
    public static CuratedPostAdapter ofWorthReading(
            List<CuratedPost> all, OnPostClick listener) {
        List<CuratedPost> filtered = new ArrayList<>();
        if (all != null) {
            for (CuratedPost c : all) {
                if (c.worthReading) filtered.add(c);
            }
        }
        return new CuratedPostAdapter(filtered, listener);
    }
}
