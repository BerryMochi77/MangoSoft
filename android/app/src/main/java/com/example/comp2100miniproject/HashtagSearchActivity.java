package com.example.comp2100miniproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.auth.AuthManager;
import com.example.comp2100miniproject.src.PostAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import dao.model.Post;
import hashtag.HashtagService;
import hashtag.TagCount;
import hashtag.search.HashtagSearchStrategy;
import hashtag.search.PostSearchStrategy;

/**
 * Displays posts filtered by a hashtag and shows trending tags.
 *
 * Launched from:
 *  - A hashtag chip on a post card (EXTRA_HASHTAG = "spam")
 *  - The "Trending" button in MainActivity (EXTRA_HASHTAG = null → trending-only view)
 *
 * Moderators use this to filter #spam / #harassment / #abuse posts quickly.
 * Regular users use it for content discovery.
 *
 * Demonstrates Strategy pattern: search is performed via PostSearchStrategy,
 * so the search algorithm can be swapped without changing this Activity.
 */
public class HashtagSearchActivity extends AppCompatActivity {

    public static final String EXTRA_HASHTAG = "hashtag_tag";

    private final PostSearchStrategy searchStrategy = new HashtagSearchStrategy();

    private String currentTag;
    private AuthManager authManager;

    private TextView textSearchTitle;
    private TextView textResultsHeader;
    private TextView textNoResults;
    private ChipGroup chipGroupTrending;
    private RecyclerView recyclerSearchResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_hashtag_search);

        authManager = new AuthManager(this);

        textSearchTitle = findViewById(R.id.textSearchTitle);
        textResultsHeader = findViewById(R.id.textResultsHeader);
        textNoResults = findViewById(R.id.textNoResults);
        chipGroupTrending = findViewById(R.id.chipGroupTrending);
        recyclerSearchResults = findViewById(R.id.recyclerSearchResults);

        recyclerSearchResults.setLayoutManager(new LinearLayoutManager(this));

        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        currentTag = getIntent().getStringExtra(EXTRA_HASHTAG);

        loadTrending();

        if (currentTag != null && !currentTag.isBlank()) {
            filterByTag(currentTag);
        } else {
            textSearchTitle.setText(R.string.trending_tags_title);
            textResultsHeader.setVisibility(View.GONE);
            textNoResults.setVisibility(View.GONE);
            recyclerSearchResults.setVisibility(View.GONE);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadTrending() {
        chipGroupTrending.removeAllViews();
        List<TagCount> trending = HashtagService.getInstance().getTrending();

        for (TagCount tc : trending) {
            Chip chip = new Chip(this);
            chip.setText(String.format("#%s — %d", tc.getTag(), tc.getCount()));
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setChipBackgroundColorResource(R.color.chip_hashtag_bg);
            chip.setTextColor(getColor(R.color.accent));
            chip.setTextSize(12f);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setOnClickListener(v -> filterByTag(tc.getTag()));
            chipGroupTrending.addView(chip);
        }

        if (trending.isEmpty()) {
            Chip empty = new Chip(this);
            empty.setText(R.string.no_trending_yet);
            empty.setEnabled(false);
            chipGroupTrending.addView(empty);
        }
    }

    private void filterByTag(String tag) {
        currentTag = tag;
        textSearchTitle.setText(getString(R.string.posts_tagged_with, "#" + tag));
        textResultsHeader.setText(R.string.results_header);
        textResultsHeader.setVisibility(View.VISIBLE);

        List<Post> results = new ArrayList<>(searchStrategy.search(tag));

        if (results.isEmpty()) {
            textNoResults.setVisibility(View.VISIBLE);
            recyclerSearchResults.setVisibility(View.GONE);
        } else {
            textNoResults.setVisibility(View.GONE);
            recyclerSearchResults.setVisibility(View.VISIBLE);
            recyclerSearchResults.setAdapter(
                    new PostAdapter(
                            this,
                            results,
                            (position, post) -> openPost(post),
                            this::filterByTag
                    )
            );
        }
    }

    private void openPost(Post post) {
        int index = findPostIndex(post);
        if (index < 0) return;
        Intent intent = new Intent(this, PostViewerActivity.class);
        intent.putExtra("post_index", index);
        String userId = getIntent().getStringExtra(AuthManager.EXTRA_USER_ID);
        boolean isAdmin = getIntent().getBooleanExtra(AuthManager.EXTRA_IS_ADMIN, false);
        intent.putExtra(AuthManager.EXTRA_USER_ID, userId);
        intent.putExtra(AuthManager.EXTRA_IS_ADMIN, isAdmin);
        startActivity(intent);
    }

    private int findPostIndex(Post target) {
        int index = 0;
        for (java.util.Iterator<Post> it = dao.PostDAO.getInstance().getAll(); it.hasNext(); ) {
            Post p = it.next();
            if (p.id.equals(target.id)) return p.isDeleted() ? -1 : index;
            index++;
        }
        return -1;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrending();
        if (currentTag != null && !currentTag.isBlank()) {
            filterByTag(currentTag);
        }
    }
}
