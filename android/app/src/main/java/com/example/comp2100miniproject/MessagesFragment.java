package com.example.comp2100miniproject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.comp2100miniproject.auth.AuthManager;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import dao.PostDAO;
import dao.UserDAO;
import dao.model.Post;
import dao.model.User;
import notification.MentionNotificationRegistry;

public class MessagesFragment extends Fragment {
    private TabHost host;
    private AuthManager authManager;
    private LinearLayout listMessages;
    private LinearLayout emptyMessages;
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof TabHost) {
            host = (TabHost) context;
        } else {
            throw new IllegalStateException("MessagesFragment requires a TabHost activity.");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authManager = new AuthManager(requireContext());
        listMessages = view.findViewById(R.id.listMessages);
        emptyMessages = view.findViewById(R.id.emptyMessages);
        renderMessages();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listMessages != null) {
            renderMessages();
        }
    }

    private void renderMessages() {
        listMessages.removeAllViews();
        User currentUser = host.currentUser();
        if (currentUser == null) {
            emptyMessages.setVisibility(View.VISIBLE);
            return;
        }

        List<MentionNotificationRegistry.MentionNotification> notifications =
                MentionNotificationRegistry.getInstance().mentionsFor(currentUser.getUUID());
        emptyMessages.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);

        for (MentionNotificationRegistry.MentionNotification notification : notifications) {
            listMessages.addView(notificationRow(notification));
        }
    }

    private View notificationRow(MentionNotificationRegistry.MentionNotification notification) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card);
        row.setClickable(true);
        row.setFocusable(true);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView title = new TextView(requireContext());
        title.setText(getString(R.string.mention_notification_title, senderName(notification.sender())));
        title.setTextColor(requireContext().getColor(R.color.text_primary));
        title.setTextSize(16f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        row.addView(title, matchWrap());

        TextView preview = new TextView(requireContext());
        preview.setText(getString(R.string.mention_notification_body, notification.preview()));
        preview.setTextColor(requireContext().getColor(R.color.text_secondary));
        preview.setTextSize(14f);
        preview.setMaxLines(2);
        LinearLayout.LayoutParams previewParams = matchWrap();
        previewParams.topMargin = dp(6);
        row.addView(preview, previewParams);

        TextView time = new TextView(requireContext());
        time.setText(timeFormat.format(notification.timestamp()));
        time.setTextColor(requireContext().getColor(R.color.text_secondary));
        time.setTextSize(12f);
        LinearLayout.LayoutParams timeParams = matchWrap();
        timeParams.topMargin = dp(8);
        row.addView(time, timeParams);

        row.setOnClickListener(v -> openMention(notification));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(12);
        row.setLayoutParams(params);
        return row;
    }

    private void openMention(MentionNotificationRegistry.MentionNotification notification) {
        int postIndex = indexOfPost(notification.post());
        if (postIndex < 0) {
            Toast.makeText(requireContext(), R.string.post_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        User currentUser = host.currentUser();
        Intent intent = new Intent(requireContext(), PostViewerActivity.class);
        intent.putExtra("post_index", postIndex);
        intent.putExtra(PostViewerActivity.EXTRA_TARGET_MESSAGE_ID, notification.message().toString());
        intent.putExtra(AuthManager.EXTRA_USER_ID, currentUser.getUUID().toString());
        intent.putExtra(AuthManager.EXTRA_IS_ADMIN, currentUser.role() == User.Role.Admin);
        startActivity(intent);
    }

    private int indexOfPost(UUID postId) {
        int index = 0;
        Iterator<Post> posts = PostDAO.getInstance().getAll();
        while (posts.hasNext()) {
            Post post = posts.next();
            if (post.getUUID().equals(postId)) return index;
            index++;
        }
        return -1;
    }

    private String senderName(UUID senderId) {
        User sender = UserDAO.getInstance().getByUUID(senderId);
        return sender == null ? getString(R.string.unknown_user) : authManager.getDisplayName(sender);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
