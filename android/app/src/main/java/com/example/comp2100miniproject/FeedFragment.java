package com.example.comp2100miniproject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.auth.AuthManager;
import com.example.comp2100miniproject.src.PostAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import dao.PostDAO;
import dao.model.Post;
import dao.model.User;
import moderation.BanRepository;

/** Feed tab: list of posts + create post + admin entry. */
public class FeedFragment extends Fragment {

    private TabHost host;
    private AuthManager authManager;
    private RecyclerView recyclerPosts;
    private EditText inputSearchPosts;

    private final ArrayList<Post> allPosts = new ArrayList<>();
    private String currentQuery = "";
    private SortMode sortMode = SortMode.TIME;

    private enum SortMode {
        TIME,
        HOT
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof TabHost) {
            host = (TabHost) context;
        } else {
            throw new IllegalStateException("FeedFragment requires a TabHost activity.");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authManager = new AuthManager(requireContext());
        User currentUser = host.currentUser();

        Button buttonAdminReports = view.findViewById(R.id.buttonAdminReports);
        buttonAdminReports.setVisibility(currentUser.role() == User.Role.Admin ? View.VISIBLE : View.GONE);
        buttonAdminReports.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AdminReportsActivity.class);
            putCurrentUser(intent);
            startActivity(intent);
        });

        view.findViewById(R.id.buttonNewPost).setOnClickListener(v -> openCreatePost());

        inputSearchPosts = view.findViewById(R.id.inputSearchPosts);
        inputSearchPosts.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                currentQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                renderPosts();
            }
        });

        ImageButton buttonSearchToggle = view.findViewById(R.id.buttonSearchToggle);
        buttonSearchToggle.setOnClickListener(v -> toggleSearchInput());

        Button buttonSortTime = view.findViewById(R.id.buttonSortTime);
        Button buttonSortHot = view.findViewById(R.id.buttonSortHot);
        buttonSortTime.setOnClickListener(v -> setSortMode(SortMode.TIME));
        buttonSortHot.setOnClickListener(v -> setSortMode(SortMode.HOT));
        updateSortButtons(buttonSortTime, buttonSortHot);

        recyclerPosts = view.findViewById(R.id.recyclerPosts);
        recyclerPosts.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadPosts();
    }

    private void toggleSearchInput() {
        boolean nowVisible = inputSearchPosts.getVisibility() != View.VISIBLE;
        inputSearchPosts.setVisibility(nowVisible ? View.VISIBLE : View.GONE);
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (nowVisible) {
            inputSearchPosts.requestFocus();
            if (imm != null) imm.showSoftInput(inputSearchPosts, InputMethodManager.SHOW_IMPLICIT);
        } else {
            inputSearchPosts.setText("");
            if (imm != null) imm.hideSoftInputFromWindow(inputSearchPosts.getWindowToken(), 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPosts();
    }

    private void loadPosts() {
        allPosts.clear();
        Iterator<Post> it = PostDAO.getInstance().getAll();
        while (it.hasNext()) {
            Post post = it.next();
            if (!post.isDeleted()) allPosts.add(post);
        }
        renderPosts();
    }

    private void renderPosts() {
        List<Post> visible;
        if (currentQuery.isEmpty()) {
            visible = allPosts;
        } else {
            visible = new ArrayList<>();
            for (Post post : allPosts) {
                if (matchesQuery(post, currentQuery)) visible.add(post);
            }
        }

        recyclerPosts.setAdapter(new PostAdapter(
                requireContext(),
                sortedPosts(visible),
                (position, post) -> openPost(post.id),
                tag -> host.showTrendsForTag(tag)));
    }

    private ArrayList<Post> sortedPosts(List<Post> posts) {
        ArrayList<Post> sorted = new ArrayList<>(posts);
        if (sortMode == SortMode.HOT) {
            sorted.sort(Comparator.comparingInt(PostEngagement::hotScore).reversed());
        } else {
            sorted.sort(Comparator.comparingLong(Post::getCreatedAt).reversed());
        }
        return sorted;
    }

    private void setSortMode(SortMode sortMode) {
        this.sortMode = sortMode;
        View view = getView();
        if (view != null) {
            updateSortButtons(
                    view.findViewById(R.id.buttonSortTime),
                    view.findViewById(R.id.buttonSortHot));
        }
        renderPosts();
    }

    private void updateSortButtons(Button buttonSortTime, Button buttonSortHot) {
        buttonSortTime.setSelected(sortMode == SortMode.TIME);
        buttonSortHot.setSelected(sortMode == SortMode.HOT);
        buttonSortTime.setAlpha(sortMode == SortMode.TIME ? 1f : 0.68f);
        buttonSortHot.setAlpha(sortMode == SortMode.HOT ? 1f : 0.68f);
    }

    private boolean matchesQuery(Post post, String query) {
        if (post.topic != null && post.topic.toLowerCase(Locale.ROOT).contains(query)) return true;
        String body = post.getBody();
        if (!body.isEmpty() && body.toLowerCase(Locale.ROOT).contains(query)) return true;
        for (String tag : post.getHashtags()) {
            if (tag.toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        return false;
    }

    private void openCreatePost() {
        Intent intent = new Intent(requireContext(), CreatePostActivity.class);
        putCurrentUser(intent);
        startActivity(intent);
    }

    private void openPost(UUID postId) {
        int index = postIndex(postId);
        if (index < 0) {
            Toast.makeText(requireContext(), R.string.post_deleted_unavailable, Toast.LENGTH_SHORT).show();
            loadPosts();
            return;
        }

        Intent intent = new Intent(requireContext(), PostViewerActivity.class);
        intent.putExtra("post_index", index);
        putCurrentUser(intent);
        startActivity(intent);
    }

    private int postIndex(UUID postId) {
        int index = 0;
        Iterator<Post> posts = PostDAO.getInstance().getAll();
        while (posts.hasNext()) {
            Post post = posts.next();
            if (post.id.equals(postId)) return post.isDeleted() ? -1 : index;
            index++;
        }
        return -1;
    }

    private void putCurrentUser(Intent intent) {
        User user = host.currentUser();
        intent.putExtra(AuthManager.EXTRA_USER_ID, user.getUUID().toString());
        intent.putExtra(AuthManager.EXTRA_IS_ADMIN, user.role() == User.Role.Admin);
    }
}
