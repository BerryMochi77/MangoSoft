package com.example.comp2100miniproject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.auth.AuthManager;
import com.example.comp2100miniproject.src.PostAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import dao.PostDAO;
import dao.model.Post;
import dao.model.User;
import hashtag.HashtagService;
import hashtag.TagCount;
import hashtag.search.HashtagSearchStrategy;
import hashtag.search.PostSearchStrategy;

/**
 * Trends tab: trending hashtag chips, plus an inline results list when a tag
 * is selected. Replaces HashtagSearchActivity. The Feed tab routes hashtag
 * clicks here via {@link TabHost#showTrendsForTag(String)}.
 */
public class TrendsFragment extends Fragment {

    private final PostSearchStrategy searchStrategy = new HashtagSearchStrategy();

    private TabHost host;

    private TextView textSearchTitle;
    private TextView textResultsHeader;
    private TextView textNoResults;
    private ChipGroup chipGroupTrending;
    private RecyclerView recyclerSearchResults;

    /**
     * The tag to filter by. {@code null} means "just show trending chips".
     * Set via {@link #applyTagFilter(String)}; survives configuration changes.
     */
    @Nullable
    private String pendingTag;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof TabHost) {
            host = (TabHost) context;
        } else {
            throw new IllegalStateException("TrendsFragment requires a TabHost activity.");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textSearchTitle = view.findViewById(R.id.textSearchTitle);
        textResultsHeader = view.findViewById(R.id.textResultsHeader);
        textNoResults = view.findViewById(R.id.textNoResults);
        chipGroupTrending = view.findViewById(R.id.chipGroupTrending);
        recyclerSearchResults = view.findViewById(R.id.recyclerSearchResults);

        recyclerSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadTrending();
        renderForCurrentTag();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTrending();
        renderForCurrentTag();
    }

    /** Set the tag to filter by; reset to {@code null} to clear the filter. */
    public void applyTagFilter(@Nullable String tag) {
        this.pendingTag = tag;
        if (isAdded() && getView() != null) {
            renderForCurrentTag();
        }
    }

    private void renderForCurrentTag() {
        if (pendingTag != null && !pendingTag.isBlank()) {
            filterByTag(pendingTag);
        } else {
            textSearchTitle.setText(R.string.trending_tags_title);
            textResultsHeader.setVisibility(View.GONE);
            textNoResults.setVisibility(View.GONE);
            recyclerSearchResults.setVisibility(View.GONE);
            recyclerSearchResults.setAdapter(null);
        }
    }

    private void loadTrending() {
        chipGroupTrending.removeAllViews();
        List<TagCount> trending = HashtagService.getInstance().getTrending();

        for (TagCount tc : trending) {
            Chip chip = new Chip(requireContext());
            chip.setText(String.format("#%s — %d", tc.getTag(), tc.getCount()));
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setChipBackgroundColorResource(R.color.chip_hashtag_bg);
            chip.setTextColor(requireContext().getColor(R.color.accent));
            chip.setTextSize(12f);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setOnClickListener(v -> {
                pendingTag = tc.getTag();
                filterByTag(tc.getTag());
            });
            chipGroupTrending.addView(chip);
        }

        if (trending.isEmpty()) {
            Chip empty = new Chip(requireContext());
            empty.setText(R.string.no_trending_yet);
            empty.setEnabled(false);
            chipGroupTrending.addView(empty);
        }
    }

    private void filterByTag(String tag) {
        textSearchTitle.setText(getString(R.string.posts_tagged_with, "#" + tag));
        textResultsHeader.setText(R.string.results_header);
        textResultsHeader.setVisibility(View.VISIBLE);

        List<Post> results = new ArrayList<>(searchStrategy.search(tag));

        if (results.isEmpty()) {
            textNoResults.setVisibility(View.VISIBLE);
            recyclerSearchResults.setVisibility(View.GONE);
            recyclerSearchResults.setAdapter(null);
        } else {
            textNoResults.setVisibility(View.GONE);
            recyclerSearchResults.setVisibility(View.VISIBLE);
            recyclerSearchResults.setAdapter(new PostAdapter(
                    requireContext(),
                    results,
                    (position, post) -> openPost(post),
                    nextTag -> {
                        pendingTag = nextTag;
                        filterByTag(nextTag);
                    },
                    this::openUserProfile
            ));
        }
    }

    private void openPost(Post post) {
        int index = findPostIndex(post);
        if (index < 0) return;
        Intent intent = new Intent(requireContext(), PostViewerActivity.class);
        intent.putExtra("post_index", index);
        User user = host.currentUser();
        intent.putExtra(AuthManager.EXTRA_USER_ID, user.getUUID().toString());
        intent.putExtra(AuthManager.EXTRA_IS_ADMIN, user.role() == User.Role.Admin);
        startActivity(intent);
    }

    private void openUserProfile(User user) {
        if (user == null || host.currentUser().getUUID().equals(user.getUUID())) {
            return;
        }

        Intent intent = new Intent(requireContext(), UserProfileActivity.class);
        User currentUser = host.currentUser();
        intent.putExtra(UserProfileActivity.EXTRA_PROFILE_USER_ID, user.getUUID().toString());
        intent.putExtra(AuthManager.EXTRA_USER_ID, currentUser.getUUID().toString());
        intent.putExtra(AuthManager.EXTRA_IS_ADMIN, currentUser.role() == User.Role.Admin);
        startActivity(intent);
    }

    private int findPostIndex(Post target) {
        int index = 0;
        for (java.util.Iterator<Post> it = PostDAO.getInstance().getAll(); it.hasNext(); ) {
            Post p = it.next();
            if (p.id.equals(target.id)) return p.isDeleted() ? -1 : index;
            index++;
        }
        return -1;
    }
}
