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

    private TextView tvDaysFree, tvCountdown, tvMotivation, tvStaticLabel;
    private MaterialButton btnAction;
    private ViewGroup rootLayout;

    // Timer components
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private boolean isJourneyStarted = false;

    // Persistence
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "CheckpointPrefs";
    private static final String KEY_START_TIME = "startTime";
    private static final String KEY_IS_RUNNING = "isRunning";

    // New Keys for Custom Text
    private static final String KEY_TEXT_MOTIVATION = "textMotivation";
    private static final String KEY_TEXT_LABEL = "textLabel";

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
        btnAction = findViewById(R.id.start_relapseButton);

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
            showFieldEditor(tvMotivation, KEY_TEXT_MOTIVATION, "Edit Quote");
            return true;
        });

        tvStaticLabel.setOnLongClickListener(v -> {
            showFieldEditor(tvStaticLabel, KEY_TEXT_LABEL, "Edit Label");
            return true;
        });
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
        tvCountdown.setText("00:00:00");
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
                "%02d:%02d:%02d", hours, minutes, seconds);
        tvCountdown.setText(timeFormatted);
    }

    // --- DIALOGS ---

    private void showRelapseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.reset_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        MaterialButton btnCancel = dialogView.findViewById(R.id.dialog_cancel);
        MaterialButton btnReset = dialogView.findViewById(R.id.dialog_relapse);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnReset.setOnClickListener(v -> {
            long newStartTime = System.currentTimeMillis();
            prefs.edit().putLong(KEY_START_TIME, newStartTime).apply();
            updateTimerDisplay();
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
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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