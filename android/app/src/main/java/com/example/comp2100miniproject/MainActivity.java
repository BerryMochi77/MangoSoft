package com.example.comp2100miniproject;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.comp2100miniproject.auth.AuthManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.UUID;

import dao.PostDAO;
import dao.RandomContentGenerator;
import dao.model.User;

/**
 * Single-Activity host for the bottom-nav tabs. Tab taps swap the
 * visible Fragment via show/hide so each tab keeps its scroll position and
 * internal state — no "enter / exit" Activity transitions, no rebuilt UI.
 *
 * Each tab Fragment talks back to this Activity through the {@link TabHost}
 * interface for cross-tab routing (hashtag → Trends) and logout.
 */
public class MainActivity extends AppCompatActivity implements TabHost {

    /**
     * Intent extra used by deep pages (e.g. PostViewerActivity) to ask
     * MainActivity to surface the Trends tab pre-filtered by a hashtag.
     */
    public static final String EXTRA_TRENDS_TAG = "trends_tag";

    private static final String TAG_FEED = "tab_feed";
    private static final String TAG_TRENDS = "tab_trends";
    private static final String TAG_AI = "tab_ai";
    private static final String TAG_MESSAGES = "tab_messages";
    private static final String TAG_PROFILE = "tab_profile";
    private static final String TAG_SETTINGS = "tab_settings";

    private AuthManager authManager;
    private User currentUser;

    private FeedFragment feedFragment;
    private TrendsFragment trendsFragment;
    private AiFragment aiFragment;
    private MessagesFragment messagesFragment;
    private ProfileFragment profileFragment;
    private SettingsFragment settingsFragment;

    private BottomNavigationView bottomNav;
    private int currentTabId = 0;
    /** True while {@link SettingsFragment} is overlaid on top of the current tab. */
    private boolean settingsOverlayShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        authManager = new AuthManager(this);
        currentUser = authManager.getUser(readCurrentUserId());
        if (currentUser == null) {
            openLogin();
            return;
        }

        boolean loadedSavedPosts = AndroidPostStore.loadAll(this);
        if (!loadedSavedPosts && !PostDAO.getInstance().getAll().hasNext()) {
            RandomContentGenerator.populateRandomData();
        }
        RandomContentGenerator.repairSeededData();
        AndroidPostStore.saveAll(this);
        DemoEngagementSeeder.seedIfNeeded();

        setupFragments(savedInstanceState);

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId(), null, false);
            return true;
        });
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.navFeed);
        } else {
            currentTabId = bottomNav.getSelectedItemId();
        }

        // Pad the root for the status bar and side gesture areas, but NOT
        // for the bottom: we want the BottomNavigationView's background to
        // extend all the way to the screen edge (under the gesture handle).
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Pad the BottomNavigationView's own bottom by the gesture-nav inset
        // so its icons and labels stay above the gesture handle while its
        // background fills that area in the bar's surface color.
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        handleTrendsIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // A deep page (PostViewer) sent us back here with a hashtag - route
        // straight to the Trends tab without rebuilding the activity.
        setIntent(intent);
        handleTrendsIntent(intent);
    }

    private void handleTrendsIntent(@Nullable Intent intent) {
        if (intent == null) return;
        String tag = intent.getStringExtra(EXTRA_TRENDS_TAG);
        if (tag == null || tag.isEmpty()) return;
        // Consume the extra so a later configuration change (e.g. rotation)
        // does not silently re-trigger this jump.
        intent.removeExtra(EXTRA_TRENDS_TAG);
        showTrendsForTag(tag);
    }

    /**
     * Either re-attach the fragments restored by the system, or create them
     * for the first time. All four are added up front and toggled via
     * show/hide so each tab preserves state between switches.
     */
    private void setupFragments(@Nullable Bundle savedInstanceState) {
        FragmentManager fm = getSupportFragmentManager();

        feedFragment = (FeedFragment) fm.findFragmentByTag(TAG_FEED);
        trendsFragment = (TrendsFragment) fm.findFragmentByTag(TAG_TRENDS);
        aiFragment = (AiFragment) fm.findFragmentByTag(TAG_AI);
        messagesFragment = (MessagesFragment) fm.findFragmentByTag(TAG_MESSAGES);
        profileFragment = (ProfileFragment) fm.findFragmentByTag(TAG_PROFILE);
        settingsFragment = (SettingsFragment) fm.findFragmentByTag(TAG_SETTINGS);

        if (savedInstanceState != null) {
            // System restored the fragments; nothing else to do.
            return;
        }

        feedFragment = new FeedFragment();
        trendsFragment = new TrendsFragment();
        aiFragment = new AiFragment();
        messagesFragment = new MessagesFragment();
        profileFragment = new ProfileFragment();
        settingsFragment = new SettingsFragment();

        FragmentTransaction tx = fm.beginTransaction();
        tx.add(R.id.fragmentContainer, feedFragment, TAG_FEED);
        tx.add(R.id.fragmentContainer, trendsFragment, TAG_TRENDS);
        tx.add(R.id.fragmentContainer, aiFragment, TAG_AI);
        tx.add(R.id.fragmentContainer, messagesFragment, TAG_MESSAGES);
        tx.add(R.id.fragmentContainer, profileFragment, TAG_PROFILE);
        tx.add(R.id.fragmentContainer, settingsFragment, TAG_SETTINGS);
        tx.hide(trendsFragment);
        tx.hide(aiFragment);
        tx.hide(messagesFragment);
        tx.hide(profileFragment);
        tx.hide(settingsFragment);
        tx.commit();
    }

    private void showTab(@IdRes int itemId, @Nullable String trendsTag, boolean forceTrendsFilter) {
        boolean sameTab = itemId == currentTabId && !settingsOverlayShown;
        Fragment target;
        if (itemId == R.id.navFeed) {
            target = feedFragment;
        } else if (itemId == R.id.navTrending) {
            target = trendsFragment;
            if (trendsFragment != null && (forceTrendsFilter || trendsTag != null)) {
                trendsFragment.applyTagFilter(trendsTag);
            }
        } else if (itemId == R.id.navAi) {
            target = aiFragment;
        } else if (itemId == R.id.navMessages) {
            target = messagesFragment;
        } else if (itemId == R.id.navProfile) {
            target = profileFragment;
        } else {
            return;
        }

        if (sameTab && !forceTrendsFilter) {
            return;
        }

        swapVisibleFragmentTo(target);
        settingsOverlayShown = false;
        currentTabId = itemId;
    }

    private void swapVisibleFragmentTo(Fragment target) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        tx.setReorderingAllowed(true);
        for (Fragment f : new Fragment[]{
                feedFragment,
                trendsFragment,
                aiFragment,
                messagesFragment,
                profileFragment,
                settingsFragment
        }) {
            if (f == null) continue;
            if (f == target) tx.show(f);
            else tx.hide(f);
        }
        tx.commit();
    }

    // === TabHost ===

    @Override
    public User currentUser() {
        return currentUser;
    }

    @Override
    public void showTrendsForTag(String tag) {
        // Keep the BottomNavigationView as the stable Activity-level chrome.
        // The selected state is updated without rebuilding the nav; only the
        // fragment visibility changes.
        showTab(R.id.navTrending, tag, true);
        if (bottomNav.getSelectedItemId() != R.id.navTrending) {
            bottomNav.setSelectedItemId(R.id.navTrending);
        }
    }

    @Override
    public void requestLogout() {
        openLogin();
    }

    @Override
    public void openSettings() {
        if (settingsFragment == null) return;
        swapVisibleFragmentTo(settingsFragment);
        settingsOverlayShown = true;
        // Leave currentTabId pointing at the tab we came FROM so the
        // bottom nav still highlights the previous tab. Tapping any nav
        // item (including the previous one) then dismisses the overlay
        // via showTab(...), because sameTab is false while
        // settingsOverlayShown is true.
    }

    @Override
    public void closeSettings() {
        if (!settingsOverlayShown) return;
        // Return to whatever tab the user was on when they opened
        // Settings. If currentTabId is unset (edge case), default to
        // Profile because that's where the entry point lives.
        int restoreId = currentTabId == 0 ? R.id.navProfile : currentTabId;
        settingsOverlayShown = false;
        if (bottomNav.getSelectedItemId() != restoreId) {
            bottomNav.setSelectedItemId(restoreId);
        } else {
            // Same id: setSelectedItemId is a no-op, so route manually.
            currentTabId = 0;
            showTab(restoreId, null, false);
        }
    }

    // === Helpers ===

    private UUID readCurrentUserId() {
        String value = getIntent().getStringExtra(AuthManager.EXTRA_USER_ID);
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
