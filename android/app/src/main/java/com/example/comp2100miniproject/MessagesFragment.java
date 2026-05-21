package com.example.comp2100miniproject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
    private enum Folder {
        HOME,
        REPLIES,
        MENTIONS,
        CHATS
    }

    private TabHost host;
    private AuthManager authManager;
    private TextView textMessagesTitle;
    private LinearLayout listMessages;
    private LinearLayout emptyMessages;
    private Folder folder = Folder.HOME;
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
        textMessagesTitle = view.findViewById(R.id.textMessagesTitle);
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

        if (folder == Folder.HOME) {
            renderFolders(currentUser);
        } else {
            renderFolderDetails(currentUser);
        }
    }

    private void renderFolders(User currentUser) {
        textMessagesTitle.setText(R.string.messages);
        textMessagesTitle.setOnClickListener(null);
        textMessagesTitle.setClickable(false);
        emptyMessages.setVisibility(View.GONE);

        List<MentionNotificationRegistry.MentionNotification> replies =
                MentionNotificationRegistry.getInstance().repliesFor(currentUser.getUUID());
        List<MentionNotificationRegistry.MentionNotification> mentions =
                MentionNotificationRegistry.getInstance().mentionsFor(currentUser.getUUID());

        listMessages.addView(folderRow(
                getString(R.string.reply_notifications),
                latestPreview(replies, R.string.no_reply_notifications),
                unreadCount(replies),
                () -> {
                    folder = Folder.REPLIES;
                    renderMessages();
                }
        ));
        listMessages.addView(folderRow(
                getString(R.string.mention_notifications),
                latestPreview(mentions, R.string.no_mention_notifications),
                unreadCount(mentions),
                () -> {
                    folder = Folder.MENTIONS;
                    renderMessages();
                }
        ));
        listMessages.addView(folderRow(
                getString(R.string.chat_messages),
                getString(R.string.chat_messages_placeholder),
                0,
                () -> {
                    folder = Folder.CHATS;
                    renderMessages();
                }
        ));
    }

    private void renderFolderDetails(User currentUser) {
        List<MentionNotificationRegistry.MentionNotification> notifications =
                folder == Folder.CHATS
                        ? java.util.Collections.emptyList()
                        : folder == Folder.REPLIES
                        ? MentionNotificationRegistry.getInstance().repliesFor(currentUser.getUUID())
                        : MentionNotificationRegistry.getInstance().mentionsFor(currentUser.getUUID());

        if (folder == Folder.CHATS) {
            textMessagesTitle.setText(R.string.folder_title_chats);
        } else {
            textMessagesTitle.setText(getString(
                    folder == Folder.REPLIES
                            ? R.string.folder_title_replies
                            : R.string.folder_title_mentions));
        }
        textMessagesTitle.setClickable(true);
        textMessagesTitle.setOnClickListener(v -> {
            folder = Folder.HOME;
            renderMessages();
        });
        emptyMessages.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);

        for (MentionNotificationRegistry.MentionNotification notification : notifications) {
            listMessages.addView(notificationRow(notification, !notification.isRead()));
        }

        markCurrentFolderRead(currentUser);
    }

    private String latestPreview(List<MentionNotificationRegistry.MentionNotification> notifications,
                                 int emptyResId) {
        if (notifications.isEmpty()) return getString(emptyResId);
        MentionNotificationRegistry.MentionNotification latest = notifications.get(0);
        return getString(
                latest.type() == MentionNotificationRegistry.NotificationType.REPLY
                        ? R.string.reply_notification_preview
                        : R.string.mention_notification_preview,
                senderName(latest.sender()),
                latest.preview()
        );
    }

    private View folderRow(String titleText, String previewText, int count, Runnable onClick) {
        FrameLayout container = new FrameLayout(requireContext());
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.bottomMargin = dp(12);
        container.setLayoutParams(containerParams);

        LinearLayout row = baseCardRow();
        row.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView title = new TextView(requireContext());
        title.setText(titleText);
        title.setTextColor(requireContext().getColor(R.color.text_primary));
        title.setTextSize(18f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView badge = new TextView(requireContext());
        badge.setText(">");
        badge.setTextColor(requireContext().getColor(R.color.text_secondary));
        badge.setTextSize(16f);
        header.addView(badge);
        row.addView(header, matchWrap());

        TextView preview = new TextView(requireContext());
        preview.setText(previewText);
        preview.setTextColor(requireContext().getColor(R.color.text_secondary));
        preview.setTextSize(14f);
        preview.setMaxLines(2);
        LinearLayout.LayoutParams previewParams = matchWrap();
        previewParams.topMargin = dp(6);
        row.addView(preview, previewParams);

        row.setOnClickListener(v -> onClick.run());
        container.addView(row);

        if (count > 0) {
            TextView unreadBadge = new TextView(requireContext());
            unreadBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            unreadBadge.setGravity(android.view.Gravity.CENTER);
            unreadBadge.setTextColor(requireContext().getColor(android.R.color.white));
            unreadBadge.setTextSize(10f);
            unreadBadge.setTypeface(unreadBadge.getTypeface(), android.graphics.Typeface.BOLD);
            unreadBadge.setBackgroundResource(R.drawable.bg_notification_dot);
            FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dp(count > 9 ? 24 : 18), dp(18));
            dotParams.leftMargin = dp(8);
            dotParams.topMargin = dp(8);
            container.addView(unreadBadge, dotParams);
        }

        return container;
    }

    private View notificationRow(MentionNotificationRegistry.MentionNotification notification,
                                 boolean unread) {
        FrameLayout container = new FrameLayout(requireContext());
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.bottomMargin = dp(12);
        container.setLayoutParams(containerParams);

        LinearLayout row = baseCardRow();
        row.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(requireContext());
        title.setText(getString(
                notification.type() == MentionNotificationRegistry.NotificationType.REPLY
                        ? R.string.reply_notification_title
                        : R.string.mention_notification_title,
                senderName(notification.sender())));
        title.setTextColor(requireContext().getColor(R.color.text_primary));
        title.setTextSize(16f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        row.addView(title, matchWrap());

        TextView preview = new TextView(requireContext());
        preview.setText(notification.preview());
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

        row.setOnClickListener(v -> openNotification(notification));
        container.addView(row);

        if (unread) {
            View unreadDot = new View(requireContext());
            unreadDot.setBackgroundResource(R.drawable.bg_notification_dot);
            FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dp(8), dp(8));
            dotParams.leftMargin = dp(10);
            dotParams.topMargin = dp(10);
            container.addView(unreadDot, dotParams);
        }

        return container;
    }

    private LinearLayout baseCardRow() {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card);
        row.setClickable(true);
        row.setFocusable(true);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(12);
        row.setLayoutParams(params);
        return row;
    }

    private void openNotification(MentionNotificationRegistry.MentionNotification notification) {
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

    private int unreadCount(List<MentionNotificationRegistry.MentionNotification> notifications) {
        int count = 0;
        for (MentionNotificationRegistry.MentionNotification notification : notifications) {
            if (!notification.isRead()) count++;
        }
        return count;
    }

    private void markCurrentFolderRead(User currentUser) {
        if (folder == Folder.REPLIES) {
            MentionNotificationRegistry.getInstance().markRepliesRead(currentUser.getUUID());
        } else if (folder == Folder.MENTIONS) {
            MentionNotificationRegistry.getInstance().markMentionsRead(currentUser.getUUID());
        } else {
            return;
        }
        host.refreshNotificationBadges();
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
