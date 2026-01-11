package com.gratus.retrack;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class HistoryBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetBehavior<View> behavior;
    private DialogBlurHelper blurHelper;
    private static final float BLUR_INTENSITY = 2f; // Adjust intensity here

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Initialize the Blur Helper
        if (context instanceof Activity) {
            blurHelper = new DialogBlurHelper((Activity) context, BLUR_INTENSITY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_ui, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Close button logic
        view.findViewById(R.id.close_history).setOnClickListener(v -> dismiss());

        // --- NEW CODE START ---
        RecyclerView recyclerView = view.findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Load Data
        RelapseDbHelper dbHelper = new RelapseDbHelper(getContext());
        List<RelapseLog> logs = dbHelper.getAllRelapses();

        // Set Adapter
        HistoryAdapter adapter = new HistoryAdapter(logs);
        recyclerView.setAdapter(adapter);
        // --- NEW CODE END ---

        ///*
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.bottom_sheet_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    systemBars.left,
                    0,
                    systemBars.right,
                    0);
            return insets;
        });
        //*/
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {

                // 1. Apply Blur
                if (blurHelper != null) {
                    blurHelper.applyBlur();
                }

                // 1. Handle Transparent Background & Dimming
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                //window.setBackgroundDrawable(new ColorDrawable(Color.argb(15, 0, 0, 0))); // makes it look like a sheet is going up.
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                WindowInsetsControllerCompat insetsController =
                        new WindowInsetsControllerCompat(window, window.getDecorView());

                // Detect current night mode
                int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                    // Dark mode → keep icons light (white)
                    insetsController.setAppearanceLightStatusBars(false);
                    insetsController.setAppearanceLightNavigationBars(false);
                } else {
                    // Light mode → use dark icons (black/gray)
                    insetsController.setAppearanceLightStatusBars(true);
                    insetsController.setAppearanceLightNavigationBars(true);
                }

                // 2. Configure Bottom Sheet Behavior
                View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    bottomSheet.setBackgroundColor(Color.TRANSPARENT);
                    behavior = BottomSheetBehavior.from(bottomSheet);

                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    behavior.setSkipCollapsed(true);
                    behavior.setDraggable(true);

                    // 👉 Sync nav bar color with sheet background
                    Drawable bg = bottomSheet.getBackground();
                    if (bg instanceof ColorDrawable) {
                        int sheetColor = ((ColorDrawable) bg).getColor();
                        window.setNavigationBarColor(sheetColor);
                    } else {
                        // fallback if drawable isn't a ColorDrawable
                        window.setNavigationBarColor(ContextCompat.getColor(requireContext(), R.color.card_bg));
                    }
                }
            }
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        // Remove Blur when sheet closes
        if (blurHelper != null) {
            blurHelper.removeBlur();
        }
        super.onDismiss(dialog);
    }

}