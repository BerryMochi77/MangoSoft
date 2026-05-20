package com.example.comp2100miniproject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.util.List;
import java.util.Locale;
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
    private EditText inputSearchPosts;
    private EditText activeComposerInput;
    private ActivityResultLauncher<PickVisualMediaRequest> composerImageLauncher;

    /** Cached list of non-deleted posts. Filtered on every text change. */
    private final ArrayList<Post> allPosts = new ArrayList<>();
    /** Lower-cased current search query; empty means "no filter". */
    private String currentQuery = "";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof TabHost) {
            host = (TabHost) context;
        } else {
            throw new IllegalStateException("FeedFragment requires a TabHost activity.");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        composerImageLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                this::insertSelectedImage
        );
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

        ImageButton buttonNewPostIcon = view.findViewById(R.id.buttonNewPostIcon);
        buttonNewPostIcon.setOnClickListener(v -> showCreatePostDialog());

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

        recyclerPosts = view.findViewById(R.id.recyclerPosts);
        recyclerPosts.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadPosts();
    }

    private void toggleSearchInput() {
        boolean nowVisible = inputSearchPosts.getVisibility() != View.VISIBLE;
        inputSearchPosts.setVisibility(nowVisible ? View.VISIBLE : View.GONE);
        if (nowVisible) {
            inputSearchPosts.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(inputSearchPosts, InputMethodManager.SHOW_IMPLICIT);
        } else {
            // Collapsing the search bar also clears any active filter so the
            // user gets the full feed back without an extra step.
            inputSearchPosts.setText("");
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
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

    /**
     * Push the (possibly filtered) post list to the RecyclerView. Filter is
     * a case-insensitive substring search across title, body and hashtags
     * so users can find a post by any visible text on its card.
     */
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
                new ArrayList<>(visible),
                (position, post) -> openPost(post.id),
                tag -> host.showTrendsForTag(tag)));
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

    private void showCreatePostDialog() {
        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);

        EditText inputTitle = new EditText(requireContext());
        inputTitle.setHint(R.string.post_title_hint);
        inputTitle.setSingleLine(true);

        EditText inputBody = new EditText(requireContext());
        inputBody.setHint(R.string.post_body_hint);
        inputBody.setMinLines(3);
        inputBody.setMaxLines(6);
        inputBody.setGravity(Gravity.TOP | Gravity.START);

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp16, dp16 / 2, dp16, 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp16 / 2;

        container.addView(inputTitle, params);
        container.addView(inputBody, params);

        ImageButton moreButton = composerMoreButton();
        LinearLayout toolRow = new LinearLayout(requireContext());
        toolRow.setGravity(Gravity.END);
        toolRow.addView(moreButton);
        container.addView(toolRow);
        moreButton.setOnClickListener(v -> showComposerMenu(inputBody));

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.create_post)
                .setView(container)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.submit, (dialog, which) ->
                        createPost(inputTitle.getText().toString(), inputBody.getText().toString()))
                .show();
    }

    private void createPost(String title, String body) {
        String cleanTitle = title == null ? "" : title.trim();
        if (cleanTitle.isEmpty()) {
            Toast.makeText(requireContext(), R.string.empty_content, Toast.LENGTH_SHORT).show();
            return;
        }
        String cleanBody = body == null ? "" : body.trim();

        User currentUser = host.currentUser();
        Post post = new Post(UUID.randomUUID(), currentUser.getUUID(), cleanTitle);
        post.setBody(cleanBody);
        // Extract hashtags from both title and body so #tags in content are indexed.
        post.setHashtags(HashtagParser.extract(cleanTitle + " " + cleanBody));
        PostDAO.getInstance().add(post);
        HashtagService.getInstance().indexPost(post);
        Toast.makeText(requireContext(), R.string.post_created, Toast.LENGTH_SHORT).show();
        loadPosts();
    }

    private ImageButton composerMoreButton() {
        ImageButton button = new ImageButton(requireContext());
        int size = (int) (44 * getResources().getDisplayMetrics().density);
        int padding = (int) (10 * getResources().getDisplayMetrics().density);
        button.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        button.setBackgroundResource(R.drawable.bg_fab_circle);
        button.setImageResource(R.drawable.ic_add_format);
        button.setContentDescription(getString(R.string.more_composer_options));
        button.setPadding(padding, padding, padding, padding);
        return button;
    }

    private void showComposerMenu(EditText input) {
        activeComposerInput = input;
        String[] options = {
                getString(R.string.add_image),
                getString(R.string.add_emoji)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.more_composer_options)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        chooseComposerImage();
                    } else if (which == 1) {
                        showEmojiChooser(input);
                    }
                })
                .show();
    }

    private void chooseComposerImage() {
        composerImageLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void insertSelectedImage(Uri uri) {
        if (activeComposerInput == null || uri == null) return;

        Uri copied = ComposerFormatManager.copyImage(requireContext(), uri);
        if (copied == null) {
            Toast.makeText(requireContext(), R.string.image_attach_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        ComposerFormatManager.insertImage(activeComposerInput, copied);
        Toast.makeText(requireContext(), R.string.image_attached, Toast.LENGTH_SHORT).show();
    }

    private void showEmojiChooser(EditText input) {
        String[] emojis = {"🙂", "😂", "😍", "👍", "🔥", "🎉"};
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_emoji)
                .setItems(emojis, (dialog, which) ->
                        ComposerFormatManager.insertEmoji(input, emojis[which]))
                .show();
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
