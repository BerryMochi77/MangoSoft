package com.example.comp2100miniproject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.auth.AuthManager;
import com.example.comp2100miniproject.src.PostAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import dao.PostDAO;
import dao.model.Post;
import dao.model.User;
import hashtag.HashtagParser;
import hashtag.HashtagService;

/** Feed tab: list of posts + create post + admin entry. */
public class FeedFragment extends Fragment {

    private TabHost host;
    private AuthManager authManager;
    private RecyclerView recyclerPosts;

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

        TextView textCurrentUser = view.findViewById(R.id.textCurrentUser);
        textCurrentUser.setText(getString(R.string.signed_in_as, authManager.getDisplayName(currentUser)));

        Button buttonAdminReports = view.findViewById(R.id.buttonAdminReports);
        buttonAdminReports.setVisibility(currentUser.role() == User.Role.Admin ? View.VISIBLE : View.GONE);
        buttonAdminReports.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AdminReportsActivity.class);
            putCurrentUser(intent);
            startActivity(intent);
        });

        Button buttonNewPost = view.findViewById(R.id.buttonNewPost);
        buttonNewPost.setOnClickListener(v -> showCreatePostDialog());

        recyclerPosts = view.findViewById(R.id.recyclerPosts);
        recyclerPosts.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadPosts();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPosts();
    }

    private void loadPosts() {
        ArrayList<Post> posts = new ArrayList<>();
        Iterator<Post> it = PostDAO.getInstance().getAll();
        while (it.hasNext()) {
            Post post = it.next();
            if (!post.isDeleted()) posts.add(post);
        }

        recyclerPosts.setAdapter(new PostAdapter(
                requireContext(),
                posts,
                (position, post) -> openPost(post.id),
                tag -> host.showTrendsForTag(tag)));
    }

    private void showCreatePostDialog() {
        EditText input = new EditText(requireContext());
        input.setHint(R.string.post_title_hint);
        input.setSingleLine(true);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.create_post)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.submit, (dialog, which) -> createPost(input.getText().toString()))
                .show();
    }

    private void createPost(String topic) {
        String cleanTopic = topic == null ? "" : topic.trim();
        if (cleanTopic.isEmpty()) {
            Toast.makeText(requireContext(), R.string.empty_content, Toast.LENGTH_SHORT).show();
            return;
        }

        User currentUser = host.currentUser();
        Post post = new Post(UUID.randomUUID(), currentUser.getUUID(), cleanTopic);
        post.setHashtags(HashtagParser.extract(cleanTopic));
        PostDAO.getInstance().add(post);
        HashtagService.getInstance().indexPost(post);
        Toast.makeText(requireContext(), R.string.post_created, Toast.LENGTH_SHORT).show();
        loadPosts();
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
