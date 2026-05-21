package com.example.comp2100miniproject;

import com.example.comp2100miniproject.ai.OfflinePostCurationStrategy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.ai.AIAnalyticsRepository;
import com.example.comp2100miniproject.ai.AIFeedbackType;
import com.example.comp2100miniproject.ai.AiPostCurationStrategy;
import com.example.comp2100miniproject.ai.AiUserPreferences;
import com.example.comp2100miniproject.ai.CuratedPost;
import com.example.comp2100miniproject.ai.PostCurationStrategy;
import com.example.comp2100miniproject.auth.AuthManager;
import com.example.comp2100miniproject.src.CuratedPostAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import dao.PostDAO;
import dao.model.Post;
import dao.model.User;

public class AiFragment extends Fragment {

    private TabHost host;
    private PostCurationStrategy curationStrategy;

    private EditText inputPreferences;
    private Button buttonSavePreferences;
    private Button buttonCurate;
    private TextView textStatus;
    private ProgressBar progress;
    private RecyclerView recyclerCurated;

    private boolean busy = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof TabHost) {
            host = (TabHost) context;
        } else {
            throw new IllegalStateException("AiFragment requires a TabHost activity.");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        curationStrategy = new OfflinePostCurationStrategy();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_ai, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        inputPreferences = view.findViewById(R.id.inputAiPreferences);
        buttonSavePreferences = view.findViewById(R.id.buttonSavePreferences);
        buttonCurate = view.findViewById(R.id.buttonCurateFeed);
        textStatus = view.findViewById(R.id.textAiStatus);
        progress = view.findViewById(R.id.progressAi);
        recyclerCurated = view.findViewById(R.id.recyclerCurated);
        recyclerCurated.setLayoutManager(new LinearLayoutManager(requireContext()));

        UUID uid = userId();
        if (uid != null) {
            inputPreferences.setText(
                    AiUserPreferences.get(
                            requireContext(),
                            uid
                    )
            );
        }

        hidePreferenceEditor();

        buttonSavePreferences.setOnClickListener(v -> {
            if (inputPreferences.getVisibility() == View.GONE) {
                showPreferenceEditor();
                return;
            }

            savePreferences();
        });

        buttonCurate.setOnClickListener(v -> runCuration());
    }

    private void showPreferenceEditor() {
        inputPreferences.setVisibility(View.VISIBLE);
        buttonSavePreferences.setText("Save preferences");
        inputPreferences.requestFocus();
    }

    private void hidePreferenceEditor() {
        inputPreferences.setVisibility(View.GONE);
        buttonSavePreferences.setText("✏️ Edit preferences");
    }

    private void savePreferences() {
        UUID uid = userId();

        if (uid == null) {
            return;
        }

        String value = inputPreferences.getText().toString();

        AiUserPreferences.set(
                requireContext(),
                uid,
                value
        );

        int messageRes =
                value.trim().isEmpty()
                        ? R.string.ai_preferences_cleared
                        : R.string.ai_preferences_saved;

        Toast.makeText(
                requireContext(),
                messageRes,
                Toast.LENGTH_SHORT
        ).show();

        hidePreferenceEditor();
    }

    private void runCuration() {
        if (busy) {
            return;
        }

        UUID uid = userId();
        if (uid == null) {
            return;
        }

        String preferences = AiUserPreferences.get(requireContext(), uid);
        if (preferences.isEmpty()) {
            showPreferenceEditor();
            Toast.makeText(
                    requireContext(),
                    R.string.ai_preferences_empty_warning,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        List<Post> posts = collectVisiblePosts();
        if (posts.isEmpty()) {
            showStatus(getString(R.string.ai_no_posts));
            recyclerCurated.setAdapter(null);
            return;
        }

        setBusy(true);
        showStatus(getString(R.string.ai_curating, posts.size()));
        recyclerCurated.setAdapter(null);

        User me = host.currentUser();
        AuthManager auth = new AuthManager(requireContext());
        String viewerHint = me == null
                ? "anonymous"
                : auth.getDisplayName(me) + " (" + me.role().name().toLowerCase() + ")";

        curationStrategy.curate(posts, viewerHint, preferences,
                new PostCurationStrategy.Callback() {
                    @Override
                    public void onResult(List<CuratedPost> curated) {
                        if (!isAdded()) {
                            return;
                        }

                        setBusy(false);
                        showCurated(curated);
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (!isAdded()) {
                            return;
                        }

                        setBusy(false);
                        showStatus(getString(
                                R.string.ai_error,
                                error.getMessage() == null
                                        ? error.getClass().getSimpleName()
                                        : error.getMessage()
                        ));
                    }
                });
    }

    private void showCurated(List<CuratedPost> curated) {
        // Record all verdicts (including filtered) in the analytics repository.
        AIAnalyticsRepository.getInstance().recordBatch(curated);

        if (curated == null || curated.isEmpty()) {
            showStatus(getString(R.string.ai_no_results));
            recyclerCurated.setAdapter(null);
            showFilteredSection(new ArrayList<>());
            return;
        }

        // Split into worth-reading and filtered lists.
        List<CuratedPost> worth    = new ArrayList<>();
        List<CuratedPost> filtered = new ArrayList<>();
        for (CuratedPost c : curated) {
            if (c.worthReading) worth.add(c);
            else                filtered.add(c);
        }

        worth.sort(Comparator.<CuratedPost>comparingInt(c -> c.score).reversed());

        if (worth.isEmpty()) {
            showStatus(getString(R.string.ai_no_results));
            recyclerCurated.setAdapter(null);
        } else {
            showStatus(buildAiFeedSummary(worth));
            UUID uid = userId();
            recyclerCurated.setAdapter(new CuratedPostAdapter(worth, this::openPost,
                    (post) -> {
                        // "Not relevant" feedback: AI recommended it but user disagrees.
                        AIAnalyticsRepository.getInstance().recordFeedback(
                                post.id, uid, AIFeedbackType.NOT_RELEVANT);
                        Toast.makeText(requireContext(),
                                R.string.ai_feedback_not_relevant_thanks, Toast.LENGTH_SHORT).show();
                    }));
        }

        showFilteredSection(filtered);
    }

    /** Populate or hide the "Filtered by AI" section below the RecyclerView. */
    private void showFilteredSection(List<CuratedPost> filtered) {
        View root = getView();
        if (root == null) return;
        LinearLayout container = root.findViewById(R.id.containerFilteredPosts);
        View section           = root.findViewById(R.id.sectionFilteredPosts);
        if (container == null || section == null) return;

        container.removeAllViews();
        if (filtered.isEmpty()) {
            section.setVisibility(View.GONE);
            return;
        }
        section.setVisibility(View.VISIBLE);

        UUID uid = userId();
        int dp = (int) (getResources().getDisplayMetrics().density);
        for (CuratedPost c : filtered) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(12 * dp, 8 * dp, 12 * dp, 8 * dp);

            TextView titleTv = new TextView(requireContext());
            String rawTitle = (c.post != null && c.post.topic != null) ? c.post.topic : "—";
            titleTv.setText(rawTitle.length() > 50
                    ? rawTitle.substring(0, 47) + "…" : rawTitle);
            titleTv.setTextSize(13f);
            titleTv.setTextColor(requireContext().getColor(R.color.text_primary));
            row.addView(titleTv);

            if (!c.summary.isEmpty()) {
                TextView sumTv = new TextView(requireContext());
                sumTv.setText(c.summary);
                sumTv.setTextSize(11f);
                sumTv.setTextColor(requireContext().getColor(R.color.text_secondary));
                row.addView(sumTv);
            }

            Button showBtn = new Button(requireContext());
            showBtn.setText(R.string.ai_show_anyway);
            showBtn.setAllCaps(false);
            showBtn.setTextSize(12f);
            showBtn.setOnClickListener(v -> {
                AIAnalyticsRepository.getInstance().recordFeedback(
                        c.post.id, uid, AIFeedbackType.SHOW_ANYWAY);
                openPost(c.post);
            });
            row.addView(showBtn);

            container.addView(row);

            View divider = new View(requireContext());
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(requireContext().getColor(R.color.border));
            container.addView(divider);
        }
    }

    private String buildAiFeedSummary(List<CuratedPost> worth) {
        StringBuilder result = new StringBuilder();

        result.append("🔥 AI Feed Summary");
        result.append("\n\nTop topic: ");
        result.append(findTopTag());

        result.append("\nRecommended focus: ");
        for (String tag : topTags(3)) {
            result.append("#").append(tag).append(" ");
        }

        result.append("\nRecommended posts: ");
        result.append(worth.size());

        result.append("\n\nAI has ranked discussions using your preferences, hashtags and post activity.");

        return result.toString();
    }

    private String findTopTag() {
        HashMap<String, Integer> counts = tagCounts();

        String best = "none";
        int max = 0;

        for (String tag : counts.keySet()) {
            int value = counts.get(tag);
            if (value > max) {
                max = value;
                best = tag;
            }
        }

        return "#" + best;
    }

    private List<String> topTags(int limit) {
        HashMap<String, Integer> counts = tagCounts();
        ArrayList<String> tags = new ArrayList<>(counts.keySet());

        tags.sort((a, b) -> counts.get(b) - counts.get(a));

        if (tags.size() > limit) {
            return tags.subList(0, limit);
        }

        return tags;
    }

    private HashMap<String, Integer> tagCounts() {
        HashMap<String, Integer> counts = new HashMap<>();

        for (Post p : collectVisiblePosts()) {
            for (String tag : p.getHashtags()) {
                counts.put(tag, counts.getOrDefault(tag, 0) + 1);
            }
        }

        return counts;
    }

    private void openPost(Post post) {
        if (post == null) {
            return;
        }

        int index = postIndex(post.id);
        if (index < 0) {
            return;
        }

        Intent intent = new Intent(requireContext(), PostViewerActivity.class);
        intent.putExtra("post_index", index);

        User me = host.currentUser();
        if (me != null) {
            intent.putExtra(AuthManager.EXTRA_USER_ID, me.getUUID().toString());
            intent.putExtra(AuthManager.EXTRA_IS_ADMIN, me.role() == User.Role.Admin);
        }

        startActivity(intent);
    }

    private int postIndex(UUID postId) {
        int i = 0;
        Iterator<Post> it = PostDAO.getInstance().getAll();

        while (it.hasNext()) {
            Post p = it.next();
            if (p.id.equals(postId)) {
                return p.isDeleted() ? -1 : i;
            }
            i++;
        }

        return -1;
    }

    private List<Post> collectVisiblePosts() {
        ArrayList<Post> posts = new ArrayList<>();
        Iterator<Post> it = PostDAO.getInstance().getAll();

        while (it.hasNext()) {
            Post p = it.next();
            if (!p.isDeleted()) {
                posts.add(p);
            }
        }

        return posts;
    }

    @Nullable
    private UUID userId() {
        User me = host.currentUser();
        return me == null ? null : me.getUUID();
    }

    private void setBusy(boolean isBusy) {
        busy = isBusy;
        buttonCurate.setEnabled(!isBusy);
        buttonSavePreferences.setEnabled(!isBusy);
        progress.setVisibility(isBusy ? View.VISIBLE : View.GONE);
    }

    private void showStatus(String message) {
        textStatus.setText(message);
        textStatus.setVisibility(View.VISIBLE);
    }
}