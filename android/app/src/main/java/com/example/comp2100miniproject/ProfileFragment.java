package com.example.comp2100miniproject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
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
import messagestate.MessageDeletionRegistry;

/** Profile tab: edit display name + password, avatar, and list user's posts and replies. */
public class ProfileFragment extends Fragment {
    private static final int PAGE_SIZE = 3;

    private TabHost host;
    private AuthManager authManager;
    private AvatarManager avatarManager;
    private ProfileBackgroundManager profileBackgroundManager;
    private User currentUser;

    private ImageView imageAvatar;
    private ImageView imageProfileBackground;
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
    private ActivityResultLauncher<PickVisualMediaRequest> galleryBackgroundLauncher;
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
        galleryBackgroundLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                this::setGalleryBackground
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
        profileBackgroundManager = new ProfileBackgroundManager(authManager);
        currentUser = host.currentUser();

        imageAvatar = view.findViewById(R.id.imageAvatar);
        imageProfileBackground = view.findViewById(R.id.imageProfileBackground);
        textUsername = view.findViewById(R.id.textUsername);
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

        view.findViewById(R.id.buttonEditProfile).setOnClickListener(v -> showEditProfileChooser());
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
        profileBackgroundManager.displayBackground(currentUser, imageProfileBackground);
        avatarManager.displayAvatar(currentUser, imageAvatar);
        textUsername.setText(getString(R.string.username_value, currentUser.username()));
    }

    private void showEditProfileChooser() {
        String[] options = {
                getString(R.string.change_avatar),
                getString(R.string.change_profile_background),
                getString(R.string.change_display_name),
                getString(R.string.change_password)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_profile)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAvatarSourceChooser();
                    } else if (which == 1) {
                        showProfileBackgroundSourceChooser();
                    } else if (which == 2) {
                        showDisplayNameDialog();
                    } else if (which == 3) {
                        showPasswordDialog();
                    }
                })
                .show();
    }

    private void showDisplayNameDialog() {
        EditText input = dialogInput(R.string.display_name, InputType.TYPE_CLASS_TEXT);
        input.setText(authManager.getDisplayName(currentUser));
        input.setSelectAllOnFocus(true);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.change_display_name)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.submit, (dialog, which) ->
                        saveDisplayName(input.getText().toString()))
                .show();
    }

    private void showPasswordDialog() {
        EditText input = dialogInput(
                R.string.new_password,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.change_password)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.submit, (dialog, which) ->
                        savePassword(input.getText().toString()))
                .show();
    }

    private EditText dialogInput(int hintResId, int inputType) {
        EditText input = new EditText(requireContext());
        input.setHint(hintResId);
        input.setInputType(inputType);
        input.setSingleLine(true);
        return input;
    }

    private void saveDisplayName(String displayName) {
        boolean saved = authManager.updateProfile(
                currentUser.getUUID(),
                displayName,
                ""
        );
        showProfileSaveResult(saved);
    }

    private void savePassword(String newPassword) {
        if (newPassword == null || newPassword.isEmpty()) {
            Toast.makeText(requireContext(), R.string.profile_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean saved = authManager.updateProfile(
                currentUser.getUUID(),
                authManager.getDisplayName(currentUser),
                newPassword
        );
        showProfileSaveResult(saved);
    }

    private void showProfileSaveResult(boolean saved) {
        if (!saved) {
            Toast.makeText(requireContext(), R.string.profile_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        currentUser = authManager.getUser(currentUser.getUUID());
        renderProfile();
        Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
    }

    private void showAvatarSourceChooser() {
        String[] options = {
                getString(R.string.choose_default_avatar),
                getString(R.string.choose_gallery_avatar)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_avatar_source)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showDefaultAvatarChooser();
                    } else if (which == 1) {
                        chooseGalleryAvatar();
                    }
                })
                .show();
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

    private void showProfileBackgroundSourceChooser() {
        String[] options = {
                getString(R.string.choose_default_profile_background),
                getString(R.string.choose_gallery_profile_background)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_profile_background_source)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showDefaultProfileBackgroundChooser();
                    } else if (which == 1) {
                        chooseGalleryProfileBackground();
                    }
                })
                .show();
    }

    private void showDefaultProfileBackgroundChooser() {
        ProfileBackgroundManager.BackgroundOption[] options = profileBackgroundManager.defaultBackgrounds();
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.choose_default_profile_background)
                .setItems(profileBackgroundManager.defaultBackgroundLabels(requireContext()), (dialog, which) -> {
                    if (profileBackgroundManager.setDefaultBackground(currentUser, options[which])) {
                        renderProfile();
                        Toast.makeText(requireContext(), R.string.profile_background_updated, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), R.string.profile_background_update_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void chooseGalleryProfileBackground() {
        galleryBackgroundLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void setGalleryBackground(Uri uri) {
        if (uri == null) return;

        if (profileBackgroundManager.setGalleryBackground(requireContext(), currentUser, uri)) {
            renderProfile();
            Toast.makeText(requireContext(), R.string.profile_background_updated, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), R.string.profile_background_update_failed, Toast.LENGTH_SHORT).show();
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

        MessageDeletionRegistry deletions = MessageDeletionRegistry.getInstance();
        Iterator<Message> messages = PostDAO.getInstance().getAllMessages();
        while (messages.hasNext()) {
            Message message = messages.next();
            if (!deletions.isDeleted(message.id())
                    && currentUser.getUUID().equals(message.poster())
                    && isPostVisible(message.thread())) {
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
                (position, post) -> openPost(post.id), null, this::openUserProfile));
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

    private void openUserProfile(User user) {
        if (user == null || currentUser.getUUID().equals(user.getUUID())) {
            return;
        }

        Intent intent = new Intent(requireContext(), UserProfileActivity.class);
        intent.putExtra(UserProfileActivity.EXTRA_PROFILE_USER_ID, user.getUUID().toString());
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
