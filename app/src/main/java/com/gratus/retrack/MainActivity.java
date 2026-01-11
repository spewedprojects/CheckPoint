package com.gratus.retrack;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.gratus.retrack.ThemeManager;

public class MainActivity extends AppCompatActivity implements UnifiedDialogFragment.DialogListener {

    private TextView tvDaysFree, tvCountdown, tvMotivation, tvStaticLabel, tvStreak;
    private ImageButton lightButton, darkButton, autoButton;
    private MaterialButton btnAction;
    private ViewGroup rootLayout;
    private View historyBtn; // Reference for toggling visibility

    // Timer components
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private boolean isJourneyStarted = false;

    // Persistence
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "ReTrack_config_Prefs";
    private static final String KEY_START_TIME = "startTime";
    private static final String KEY_IS_RUNNING = "isRunning";

    // New Keys for Custom Text
    private static final String KEY_TEXT_MOTIVATION = "textMotivation";
    private static final String KEY_TEXT_LABEL = "textLabel";

    // Configuration
    private static final float DIALOG_DIM_AMOUNT = 0.2f; // Set your dim amount here (0.0 to 1.0)

    // Original button styles to revert to
    private ColorStateList originalBtnBackground;
    private ColorStateList originalBtnTextColor;
    private String originalBtnText = "Reset";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 1. Initialize Views
        rootLayout = findViewById(R.id.main);
        tvDaysFree = findViewById(R.id.days_free);
        tvCountdown = findViewById(R.id.countdown);
        tvMotivation = findViewById(R.id.motivation_text); // Ensure ID is added in XML
        tvStaticLabel = findViewById(R.id.static_text);
        tvStreak = findViewById(R.id.bestStreak_days);
        btnAction = findViewById(R.id.start_relapseButton);
        //tvEditfields = findViewById(R.id.editorTitle);

        lightButton = findViewById(R.id.theme_light);
        darkButton = findViewById(R.id.theme_dark);
        autoButton = findViewById(R.id.theme_auto);

        // Capture history button reference
        historyBtn = findViewById(R.id.history_space);
        historyBtn.setOnClickListener(v -> {
            HistoryBottomSheet bottomSheet = new HistoryBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "HistorySheet");
        });

        // Store original styles from XML
        originalBtnBackground = btnAction.getBackgroundTintList();
        originalBtnTextColor = btnAction.getTextColors();
        originalBtnText = btnAction.getText().toString();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 2. Initialize Persistence
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 3. Check State and Setup UI
        checkStateAndInit();
        loadCustomTexts(); // Load the edited texts
        updateHistoryButtonVisibility(); // Check history on load

        // 4. UPDATED Button Listener
        btnAction.setOnClickListener(v -> {
            if (!isJourneyStarted) {
                startJourney();
            } else {
                // Show the Relapse Dialog Fragment
                UnifiedDialogFragment.newRelapseInstance()
                        .show(getSupportFragmentManager(), "RelapseDialog");
            }
        });

        // 5. UPDATED Long Click Listeners
        tvMotivation.setOnLongClickListener(v -> {
            UnifiedDialogFragment.newEditorInstance(
                    "Change Quote",
                    tvMotivation.getText().toString(),
                    KEY_TEXT_MOTIVATION
            ).show(getSupportFragmentManager(), "EditorDialog");
            return true;
        });

        tvStaticLabel.setOnLongClickListener(v -> {
            UnifiedDialogFragment.newEditorInstance(
                    "Change Unit",
                    tvStaticLabel.getText().toString(),
                    KEY_TEXT_LABEL
            ).show(getSupportFragmentManager(), "EditorDialog");
            return true;
        });

        updateBestStreakDisplay();

        // Initialize theme buttons
        setupThemeButtons();
        updateButtonVisibility();
    }

    /**
     * Set up the theme toggle buttons.
     */
    private void setupThemeButtons() {

        lightButton.setOnClickListener(v -> {
            ThemeManager.setLight(this);
            //updateButtonVisibility();
        });

        darkButton.setOnClickListener(v -> {
            ThemeManager.setDark(this);
            //updateButtonVisibility();
        });

        autoButton.setOnClickListener(v -> {
            ThemeManager.setAuto(this);
            updateButtonVisibility();
        });
    }

    /**
     * Update the visibility of the theme buttons based on the current theme.
     */
    private void updateButtonVisibility() {
        String mode = ThemeManager.getSavedMode(this);

        if (ThemeManager.MODE_LIGHT.equals(mode)) {
            lightButton.setVisibility(View.GONE);
            lightButton.setAlpha(0.1f);
            darkButton.setVisibility(View.VISIBLE);
            autoButton.setVisibility(View.GONE);
            System.out.println("Light Mode: Gone (Light button, Auto button), Visible (Dark button)");

        } else if (ThemeManager.MODE_DARK.equals(mode)) {
            lightButton.setVisibility(View.GONE);
            darkButton.setVisibility(View.GONE);
            autoButton.setVisibility(View.VISIBLE);
            autoButton.setAlpha(0.1f);
            System.out.println("Dark Mode: Gone (Light button, Dark button), Visible (Auto button)");

        } else {
            lightButton.setVisibility(View.VISIBLE);
            lightButton.setAlpha(0.1f);
            darkButton.setVisibility(View.GONE);
            autoButton.setVisibility(View.GONE);
            System.out.println("Auto Mode: Gone (Auto button, Dark button), Visible (Light button)");
        }
    }


    private void updateHistoryButtonVisibility() {
        RelapseDbHelper dbHelper = new RelapseDbHelper(this);
        if (dbHelper.hasRecords()) {
            if (historyBtn.getVisibility() != View.VISIBLE) {
                TransitionManager.beginDelayedTransition(rootLayout); // Animate appearance
                historyBtn.setVisibility(View.VISIBLE);
            }
        } else {
            historyBtn.setVisibility(View.GONE);
        }
    }

    private void checkStateAndInit() {
        isJourneyStarted = prefs.getBoolean(KEY_IS_RUNNING, false);

        if (isJourneyStarted) {
            setRelapseUIState(false);
            startTimerTick();
        } else {
            setStartUIState();
        }
    }

    private void loadCustomTexts() {
        // Load saved text or keep default if empty
        String savedMotivation = prefs.getString(KEY_TEXT_MOTIVATION, null);
        String savedLabel = prefs.getString(KEY_TEXT_LABEL, null);

        if (savedMotivation != null) tvMotivation.setText(savedMotivation);
        if (savedLabel != null) tvStaticLabel.setText(savedLabel);
    }

    private void setStartUIState() {
        // Use ContextCompat to resolve the color based on the current theme
        int bgColor = androidx.core.content.ContextCompat.getColor(this, R.color.relapse_button_bg);
        int textColor = androidx.core.content.ContextCompat.getColor(this, R.color.relapse_button_text);

        btnAction.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        btnAction.setTextColor(textColor);
        btnAction.setText("Start Journey");
        tvDaysFree.setText("0");
        tvCountdown.setText("0h 0m 0s");
        tvStreak.setText("\uD83C\uDFC6 0 days");
    }


    private void setRelapseUIState(boolean animate) {
        if (animate) {
            TransitionManager.beginDelayedTransition(rootLayout);
        }
        btnAction.setBackgroundTintList(originalBtnBackground);
        btnAction.setTextColor(originalBtnTextColor);
        btnAction.setText(originalBtnText);
    }

    private void startJourney() {
        long startTime = System.currentTimeMillis();
        prefs.edit()
                .putLong(KEY_START_TIME, startTime)
                .putBoolean(KEY_IS_RUNNING, true)
                .apply();

        isJourneyStarted = true;
        setRelapseUIState(true);
        startTimerTick();
    }

    private void startTimerTick() {
        timerHandler.removeCallbacks(timerRunnable);
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimerDisplay();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
        updateBestStreakDisplay();
    }

    private void updateTimerDisplay() {
        long startTime = prefs.getLong(KEY_START_TIME, 0);
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - startTime;
        if (diff < 0) diff = 0;

        long days = TimeUnit.MILLISECONDS.toDays(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;

        tvDaysFree.setText(String.valueOf(days));
        String timeFormatted = String.format(Locale.getDefault(),
                "%01dh %01dm %01ds", hours, minutes, seconds);
        tvCountdown.setText(timeFormatted);
    }

    private void updateBestStreakDisplay() {
        RelapseDbHelper dbHelper = new RelapseDbHelper(this);
        long bestDurationMs = dbHelper.getBestStreakDuration();

        // Also check if current running streak is the best
        long currentStart = prefs.getLong(KEY_START_TIME, System.currentTimeMillis());
        long currentDuration = System.currentTimeMillis() - currentStart;

        if (currentDuration > bestDurationMs) {
            bestDurationMs = currentDuration;
        }

        long days = TimeUnit.MILLISECONDS.toDays(bestDurationMs);
        tvStreak.setText("\uD83C\uDFC6 " + days + " days"); // 🏆 {days} days
    }

    @Override
    public void onRelapseConfirmed(String reason, String steps) {
        // Logic moved here from old showRelapseDialog
        long endTime = System.currentTimeMillis();
        long startTime = prefs.getLong(KEY_START_TIME, endTime);

        // 1. Save to DB
        RelapseDbHelper dbHelper = new RelapseDbHelper(this);
        dbHelper.addRelapse(startTime, endTime, reason, steps);

        // 2. Reset Timer
        prefs.edit().putLong(KEY_START_TIME, endTime).apply();
        updateTimerDisplay();

        // 3. Refresh UI
        updateBestStreakDisplay();
        updateHistoryButtonVisibility();
    }

    @Override
    public void onFieldSaved(String prefsKey, String newValue) {
        // Logic moved here from old showFieldEditor

        // 1. Update the UI immediately
        if (KEY_TEXT_MOTIVATION.equals(prefsKey)) {
            tvMotivation.setText(newValue);
        } else if (KEY_TEXT_LABEL.equals(prefsKey)) {
            tvStaticLabel.setText(newValue);
        }

        // 2. Save to Persistence
        prefs.edit().putString(prefsKey, newValue).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtonVisibility(); // THIS IS THE FIX
    }
}