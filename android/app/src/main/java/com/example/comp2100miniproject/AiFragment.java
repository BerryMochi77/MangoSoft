package com.example.comp2100miniproject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.ai.AiPostCurationStrategy;
import com.example.comp2100miniproject.ai.AiUserPreferences;
import com.example.comp2100miniproject.ai.CuratedPost;
import com.example.comp2100miniproject.ai.PostCurationStrategy;
import com.example.comp2100miniproject.auth.AuthManager;
import com.example.comp2100miniproject.src.CuratedPostAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import dao.PostDAO;
import dao.model.Post;
import dao.model.User;

/**
 * AI curator tab. Two halves:
 *
 * <ol>
 *   <li><b>Preferences editor</b> — multiline text the viewer writes
 *       once, persisted per-user in {@link AiUserPreferences}. This is
 *       the ONLY filter criterion the AI uses.</li>
 *   <li><b>Curate</b> — sends every visible post plus the saved
 *       preferences to {@link AiPostCurationStrategy}, then shows the
 *       posts the AI says match.</li>
 * </ol>
 *
 * <p>Design pattern: <b>Strategy</b>. The fragment talks to the
 * abstract {@link PostCurationStrategy}; a future offline / keyword
 * fallback can swap in without touching this code.</p>
 *
 * <p>The Hackathon brief's per-message SOLID constraint does not
 * apply: the result lives only in this screen's in-memory list, the
 * preferences are per-user (not per-message), and nothing here
 * modifies the {@link dao.model.Message} domain object.</p>
 */
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
        // Strategy injected here so a test or alternate build could swap it.
        curationStrategy = new AiPostCurationStrategy();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inputPreferences = view.findViewById(R.id.inputAiPreferences);
        buttonSavePreferences = view.findViewById(R.id.buttonSavePreferences);
        buttonCurate = view.findViewById(R.id.buttonCurateFeed);
        textStatus = view.findViewById(R.id.textAiStatus);
        progress = view.findViewById(R.id.progressAi);
        recyclerCurated = view.findViewById(R.id.recyclerCurated);
        recyclerCurated.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Load the user's saved preferences into the editor so they can
        // tweak rather than rewrite from scratch every time.
        UUID uid = userId();
        if (uid != null) {
            inputPreferences.setText(AiUserPreferences.get(requireContext(), uid));
        }

        buttonSavePreferences.setOnClickListener(v -> savePreferences());
        buttonCurate.setOnClickListener(v -> runCuration());
    }

    private void savePreferences() {
        UUID uid = userId();
        if (uid == null) return;
        String value = inputPreferences.getText().toString();
        AiUserPreferences.set(requireContext(), uid, value);
        int messageRes = value.trim().isEmpty()
                ? R.string.ai_preferences_cleared
                : R.string.ai_preferences_saved;
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
    }

    private void runCuration() {
        if (busy) return;
        UUID uid = userId();
        if (uid == null) return;

        String preferences = AiUserPreferences.get(requireContext(), uid);
        if (preferences.isEmpty()) {
            // Nudge the user to set preferences before the AI runs, otherwise
            // the result is just "generic taste" which is what we are trying
            // to avoid.
            Toast.makeText(requireContext(),
                    R.string.ai_preferences_empty_warning,
                    Toast.LENGTH_SHORT).show();
            inputPreferences.requestFocus();
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
                : (auth.getDisplayName(me) + " (" + me.role().name().toLowerCase() + ")");

        curationStrategy.curate(posts, viewerHint, preferences,
                new PostCurationStrategy.Callback() {
            @Override
            public void onResult(List<CuratedPost> curated) {
                if (!isAdded()) return;
                setBusy(false);
                showCurated(curated);
            }

            @Override
            public void onError(Throwable error) {
                if (!isAdded()) return;
                setBusy(false);
                showStatus(getString(R.string.ai_error,
                        error.getMessage() == null
                                ? error.getClass().getSimpleName()
                                : error.getMessage()));
            }
        });
    }

    private void showCurated(List<CuratedPost> curated) {
        if (curated == null || curated.isEmpty()) {
            showStatus(getString(R.string.ai_no_results));
            recyclerCurated.setAdapter(null);
            return;
        }
        // Highest-scoring "worth reading" first; nothing else makes the cut.
        List<CuratedPost> worth = new ArrayList<>();
        for (CuratedPost c : curated) {
            if (c.worthReading) worth.add(c);
        }
        worth.sort(Comparator.<CuratedPost>comparingInt(c -> c.score).reversed());

        if (worth.isEmpty()) {
            showStatus(getString(R.string.ai_no_results));
            recyclerCurated.setAdapter(null);
            return;
        }
        hideStatus();
        recyclerCurated.setAdapter(new CuratedPostAdapter(worth, this::openPost));
    }

    private void openPost(Post post) {
        if (post == null) return;
        int index = postIndex(post.id);
        if (index < 0) return;
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
            if (p.id.equals(postId)) return p.isDeleted() ? -1 : i;
            i++;
        }
        return -1;
    }

    private List<Post> collectVisiblePosts() {
        ArrayList<Post> posts = new ArrayList<>();
        Iterator<Post> it = PostDAO.getInstance().getAll();
        while (it.hasNext()) {
            Post p = it.next();
            if (!p.isDeleted()) posts.add(p);
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

    private void hideStatus() {
        textStatus.setVisibility(View.GONE);
    }
}
