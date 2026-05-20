package com.example.comp2100miniproject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.example.comp2100miniproject.src.ProfileReplyAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import dao.PostDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.User;

/** Profile tab: edit display name + password, avatar, and list user's posts and replies. */
public class ProfileFragment extends Fragment {
    private static final int PAGE_SIZE = 3;

    private TabHost host;
    private AuthManager authManager;
    private AvatarManager avatarManager;
    private User currentUser;

    private ImageView imageAvatar;
    private EditText inputDisplayName;
    private EditText inputNewPassword;
    private TextView textUsername;
    private TextView textNoMyPosts;
    private TextView textNoMyReplies;
    private TextView textPostsPage;
    private TextView textRepliesPage;
    private RecyclerView recyclerMyPosts;
    private RecyclerView recyclerMyReplies;
    private Button buttonPrevPosts;
    private Button buttonNextPosts;
    private Button buttonPrevReplies;
    private Button buttonNextReplies;

    private ActivityResultLauncher<PickVisualMediaRequest> galleryAvatarLauncher;
    private ActivityResultLauncher<Intent> avatarCropLauncher;

    private final ArrayList<Post> myPosts = new ArrayList<>();
    private final ArrayList<Message> myReplies = new ArrayList<>();
    private int postsPage = 0;
    private int repliesPage = 0;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof TabHost) {
            host = (TabHost) context;
        } else {
            throw new IllegalStateException("ProfileFragment requires a TabHost activity.");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        galleryAvatarLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                this::setGalleryAvatar
        );
        avatarCropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) {
                        return;
                    }
                    String croppedUri = result.getData().getStringExtra(AvatarCropActivity.EXTRA_CROPPED_URI);
                    if (croppedUri != null) {
                        applyCroppedAvatar(Uri.parse(croppedUri));
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authManager = new AuthManager(requireContext());
        avatarManager = new AvatarManager(authManager);
        currentUser = host.currentUser();

        imageAvatar = view.findViewById(R.id.imageAvatar);
        textUsername = view.findViewById(R.id.textUsername);
        inputDisplayName = view.findViewById(R.id.inputDisplayName);
        inputNewPassword = view.findViewById(R.id.inputNewPassword);
        textNoMyPosts = view.findViewById(R.id.textNoMyPosts);
        textNoMyReplies = view.findViewById(R.id.textNoMyReplies);
        textPostsPage = view.findViewById(R.id.textPostsPage);
        textRepliesPage = view.findViewById(R.id.textRepliesPage);
        recyclerMyPosts = view.findViewById(R.id.recyclerMyPosts);
        recyclerMyReplies = view.findViewById(R.id.recyclerMyReplies);
        buttonPrevPosts = view.findViewById(R.id.buttonPrevPosts);
        buttonNextPosts = view.findViewById(R.id.buttonNextPosts);
        buttonPrevReplies = view.findViewById(R.id.buttonPrevReplies);
        buttonNextReplies = view.findViewById(R.id.buttonNextReplies);

        recyclerMyPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerMyReplies.setLayoutManager(new LinearLayoutManager(requireContext()));
        collectUserContent();
        renderProfile();
        renderContentPages();

        view.findViewById(R.id.buttonSaveProfile).setOnClickListener(v -> saveProfile());
        view.findViewById(R.id.buttonChooseDefaultAvatar).setOnClickListener(v -> showDefaultAvatarChooser());
        view.findViewById(R.id.buttonChooseGalleryAvatar).setOnClickListener(v -> chooseGalleryAvatar());
        buttonPrevPosts.setOnClickListener(v -> {
            postsPage--;
            renderPostsPage();
        });
        buttonNextPosts.setOnClickListener(v -> {
            postsPage++;
            renderPostsPage();
        });
        buttonPrevReplies.setOnClickListener(v -> {
            repliesPage--;
            renderRepliesPage();
        });
        buttonNextReplies.setOnClickListener(v -> {
            repliesPage++;
            renderRepliesPage();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            refreshContent();
        }
    }

    private void renderProfile() {
        avatarManager.displayAvatar(currentUser, imageAvatar);
        textUsername.setText(getString(R.string.username_value, currentUser.username()));
        inputDisplayName.setText(authManager.getDisplayName(currentUser));
    }

    private void showDefaultAvatarChooser() {
        AvatarManager.AvatarOption[] options = avatarManager.defaultAvatars();
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.choose_default_avatar)
                .setItems(avatarManager.defaultAvatarLabels(requireContext()), (dialog, which) -> {
                    if (avatarManager.setDefaultAvatar(currentUser, options[which])) {
                        renderProfile();
                        Toast.makeText(requireContext(), R.string.avatar_updated, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), R.string.avatar_update_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void chooseGalleryAvatar() {
        galleryAvatarLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void setGalleryAvatar(Uri uri) {
        if (uri == null) return;

        Intent intent = new Intent(requireContext(), AvatarCropActivity.class);
        intent.putExtra(AvatarCropActivity.EXTRA_SOURCE_URI, uri.toString());
        intent.putExtra(AvatarCropActivity.EXTRA_USER_ID, currentUser.getUUID().toString());
        avatarCropLauncher.launch(intent);
    }

    private void applyCroppedAvatar(Uri uri) {
        if (avatarManager.setGalleryAvatar(requireContext(), currentUser, uri)) {
            renderProfile();
            Toast.makeText(requireContext(), R.string.avatar_updated, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), R.string.avatar_update_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void collectUserContent() {
        myPosts.clear();
        myReplies.clear();

        Iterator<Post> posts = PostDAO.getInstance().getAll();
        while (posts.hasNext()) {
            Post post = posts.next();
            if (!post.isDeleted() && currentUser.getUUID().equals(post.poster)) {
                myPosts.add(post);
            }
        }

        Iterator<Message> messages = PostDAO.getInstance().getAllMessages();
        while (messages.hasNext()) {
            Message message = messages.next();
            if (!message.isDeleted() && currentUser.getUUID().equals(message.poster()) && isPostVisible(message.thread())) {
                myReplies.add(message);
            }
        }
    }

    private void renderContentPages() {
        postsPage = clampPage(postsPage, myPosts.size());
        repliesPage = clampPage(repliesPage, myReplies.size());
        renderPostsPage();
        renderRepliesPage();
    }

    private void renderPostsPage() {
        if (myPosts.isEmpty()) {
            textNoMyPosts.setVisibility(View.VISIBLE);
            recyclerMyPosts.setVisibility(View.GONE);
            recyclerMyPosts.setAdapter(null);
            textPostsPage.setText(getString(R.string.page_format, 0, 0));
            buttonPrevPosts.setEnabled(false);
            buttonNextPosts.setEnabled(false);
            return;
        }

        postsPage = clampPage(postsPage, myPosts.size());
        textNoMyPosts.setVisibility(View.GONE);
        recyclerMyPosts.setVisibility(View.VISIBLE);
        recyclerMyPosts.setAdapter(new PostAdapter(requireContext(), pagePosts(),
                (position, post) -> openPost(post.id), null));
        updatePager(postsPage, myPosts.size(), textPostsPage, buttonPrevPosts, buttonNextPosts);
    }

    private void renderRepliesPage() {
        if (myReplies.isEmpty()) {
            textNoMyReplies.setVisibility(View.VISIBLE);
            recyclerMyReplies.setVisibility(View.GONE);
            recyclerMyReplies.setAdapter(null);
            textRepliesPage.setText(getString(R.string.page_format, 0, 0));
            buttonPrevReplies.setEnabled(false);
            buttonNextReplies.setEnabled(false);
            return;
        }

        repliesPage = clampPage(repliesPage, myReplies.size());
        textNoMyReplies.setVisibility(View.GONE);
        recyclerMyReplies.setVisibility(View.VISIBLE);
        recyclerMyReplies.setAdapter(new ProfileReplyAdapter(requireContext(), pageReplies(), reply -> openPost(reply.thread())));
        updatePager(repliesPage, myReplies.size(), textRepliesPage, buttonPrevReplies, buttonNextReplies);
    }

    private List<Post> pagePosts() {
        int start = postsPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, myPosts.size());
        return new ArrayList<>(myPosts.subList(start, end));
    }

    private List<Message> pageReplies() {
        int start = repliesPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, myReplies.size());
        return new ArrayList<>(myReplies.subList(start, end));
    }

    private void updatePager(int page, int itemCount, TextView label, Button prev, Button next) {
        int totalPages = totalPages(itemCount);
        label.setText(getString(R.string.page_format, page + 1, totalPages));
        prev.setEnabled(page > 0);
        next.setEnabled(page + 1 < totalPages);
    }

    private int clampPage(int page, int itemCount) {
        int totalPages = totalPages(itemCount);
        if (totalPages == 0) return 0;
        return Math.max(0, Math.min(page, totalPages - 1));
    }

    private int totalPages(int itemCount) {
        if (itemCount == 0) return 0;
        return (itemCount + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    private void saveProfile() {
        boolean saved = authManager.updateProfile(
                currentUser.getUUID(),
                inputDisplayName.getText().toString(),
                inputNewPassword.getText().toString()
        );
        if (!saved) {
            Toast.makeText(requireContext(), R.string.profile_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        currentUser = authManager.getUser(currentUser.getUUID());
        inputNewPassword.setText("");
        renderProfile();
        Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
    }

    private void openPost(UUID postId) {
        int index = postIndex(postId);
        if (index < 0) {
            Toast.makeText(requireContext(), R.string.post_deleted_unavailable, Toast.LENGTH_SHORT).show();
            refreshContent();
            return;
        }

        Intent intent = new Intent(requireContext(), PostViewerActivity.class);
        intent.putExtra("post_index", index);
        intent.putExtra(AuthManager.EXTRA_USER_ID, currentUser.getUUID().toString());
        intent.putExtra(AuthManager.EXTRA_IS_ADMIN, currentUser.role() == User.Role.Admin);
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

    private void refreshContent() {
        collectUserContent();
        renderContentPages();
    }

    private boolean isPostVisible(UUID postId) {
        Iterator<Post> posts = PostDAO.getInstance().getAll();
        while (posts.hasNext()) {
            Post post = posts.next();
            if (post.id.equals(postId)) return !post.isDeleted();
        }
        return false;
    }
}
