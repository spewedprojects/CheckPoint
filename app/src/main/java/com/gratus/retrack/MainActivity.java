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
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private TextView tvDaysFree, tvCountdown, tvMotivation, tvStaticLabel, tvStreak;
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
    private String originalBtnText = "Relapse";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        // Capture history button reference
        historyBtn = findViewById(R.id.history_space);
        historyBtn.setOnClickListener(v -> {
            HistoryBottomSheet bottomSheet = new HistoryBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "HistorySheet");
        });

        View historyBtn = findViewById(R.id.history_space);
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

        // 4. Setup Button Listener
        btnAction.setOnClickListener(v -> {
            if (!isJourneyStarted) {
                startJourney();
            } else {
                showRelapseDialog();
            }
        });

        // 5. Setup Long Click Listeners for Editing
        tvMotivation.setOnLongClickListener(v -> {
            showFieldEditor(tvMotivation, KEY_TEXT_MOTIVATION, "Change Quote");
            return true;
        });

        tvStaticLabel.setOnLongClickListener(v -> {
            showFieldEditor(tvStaticLabel, KEY_TEXT_LABEL, "Change Unit");
            return true;
        });

        updateBestStreakDisplay();
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
        btnAction.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
        btnAction.setTextColor(Color.WHITE);
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

    // --- DIALOGS ---

    private void showRelapseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.reset_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        // 2.1. Set Transparent Background & Dim Amount
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(DIALOG_DIM_AMOUNT); // Applied Dimming
        }

        MaterialButton btnCancel = dialogView.findViewById(R.id.dialog_cancel);
        MaterialButton btnReset = dialogView.findViewById(R.id.dialog_relapse);
        TextInputEditText etReason = dialogView.findViewById(R.id.reason_input);
        TextInputEditText etSteps = dialogView.findViewById(R.id.next_steps_input);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnReset.setOnClickListener(v -> {
            // 1. Gather Data
            long endTime = System.currentTimeMillis();
            long startTime = prefs.getLong(KEY_START_TIME, endTime);
            String reason = etReason.getText() != null ? etReason.getText().toString() : "";
            String steps = etSteps.getText() != null ? etSteps.getText().toString() : "";

            // 2. Save to DB
            RelapseDbHelper dbHelper = new RelapseDbHelper(this);
            dbHelper.addRelapse(startTime, endTime, reason, steps);

            // 3. Reset Timer
            prefs.edit().putLong(KEY_START_TIME, endTime).apply();
            updateTimerDisplay();

            // 4. Refresh Best Streak Display
            updateBestStreakDisplay();
            // 2.2. Check visibility again since we just added a record
            updateHistoryButtonVisibility();

            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Reusable method to edit text fields using your fields_editor.xml layout
     */
    private void showFieldEditor(TextView targetView, String prefsKey, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.fields_editor, null); // Using your file
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        // 2.1. Set Transparent Background & Dim Amount
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(DIALOG_DIM_AMOUNT); // Applied Dimming
        }

        // Initialize Editor Views
        TextView tvTitle = dialogView.findViewById(R.id.editorTitle);
        TextInputEditText editText = dialogView.findViewById(R.id.editorEditText);
        MaterialButton btnCancel = dialogView.findViewById(R.id.cancelBtn);
        MaterialButton btnSave = dialogView.findViewById(R.id.saveBtn);

        // Setup UI
        tvTitle.setText(title);
        editText.setText(targetView.getText()); // Pre-fill with current text
        editText.requestFocus(); // Optional: Focus cursor immediately

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newText = editText.getText().toString().trim();
            if (!newText.isEmpty()) {
                // 1. Update UI
                targetView.setText(newText);
                // 2. Save to Storage
                prefs.edit().putString(prefsKey, newText).apply();
            }
            dialog.dismiss();
        });

        dialog.show();
    }
}