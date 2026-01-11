package com.gratus.retrack;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class UnifiedDialogFragment extends DialogFragment {

    // Logic constants
    private static final String ARG_TYPE = "dialog_type";
    private static final int TYPE_RELAPSE = 1;
    private static final int TYPE_EDITOR = 2;

    // Data constants
    private static final String ARG_TITLE = "title";
    private static final String ARG_PREFS_KEY = "prefs_key";
    private static final String ARG_CURRENT_TEXT = "current_text";

    // UI Customization
    private static final float DIM_AMOUNT = 0.0f;
    private static final float BLUR_INTENSITY = 12f; // Change this value to adjust blur strength

    // Helper
    private DialogBlurHelper blurHelper;
    private DialogListener listener;

    public interface DialogListener {
        void onRelapseConfirmed(String reason, String nextSteps);
        void onFieldSaved(String prefsKey, String newValue);
    }

    // --- Factory Methods --- (Unchanged)
    public static UnifiedDialogFragment newRelapseInstance() {
        UnifiedDialogFragment fragment = new UnifiedDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, TYPE_RELAPSE);
        fragment.setArguments(args);
        return fragment;
    }

    public static UnifiedDialogFragment newEditorInstance(String title, String currentText, String prefsKey) {
        UnifiedDialogFragment fragment = new UnifiedDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, TYPE_EDITOR);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_CURRENT_TEXT, currentText);
        args.putString(ARG_PREFS_KEY, prefsKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (DialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement UnifiedDialogFragment.DialogListener");
        }

        // Initialize Blur Helper
        if (context instanceof android.app.Activity) {
            blurHelper = new DialogBlurHelper((android.app.Activity) context, BLUR_INTENSITY);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        int type = getArguments().getInt(ARG_TYPE);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View view;
        if (type == TYPE_RELAPSE) {
            view = setupRelapseDialog(inflater);
        } else {
            view = setupEditorDialog(inflater);
        }

        builder.setView(view);
        AlertDialog dialog = builder.create();

        // Note: Window configuration happens in onStart() for reliable transparency/dimming

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                // 1. Handle Transparent Background & Dimming
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                // 2. Ensure Edge-to-Edge and Clean Flags (Similar to YMPicker)
                WindowCompat.setDecorFitsSystemWindows(window, false);
                window.setStatusBarColor(Color.TRANSPARENT);
                window.setNavigationBarColor(Color.TRANSPARENT);

                // 3. Apply Blur
                if (blurHelper != null) {
                    blurHelper.applyBlur();
                }
            }
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        // Remove Blur
        if (blurHelper != null) {
            blurHelper.removeBlur();
        }
        super.onDismiss(dialog);
    }

    // --- Setup Helpers --- (Unchanged)
    private View setupRelapseDialog(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.reset_dialog, null);

        MaterialButton btnCancel = view.findViewById(R.id.dialog_cancel);
        MaterialButton btnReset = view.findViewById(R.id.dialog_relapse);
        TextInputEditText etReason = view.findViewById(R.id.reason_input);
        TextInputEditText etSteps = view.findViewById(R.id.next_steps_input);

        btnCancel.setOnClickListener(v -> dismiss());

        btnReset.setOnClickListener(v -> {
            String reason = etReason.getText() != null ? etReason.getText().toString() : "";
            String steps = etSteps.getText() != null ? etSteps.getText().toString() : "";
            listener.onRelapseConfirmed(reason, steps);
            dismiss();
        });

        return view;
    }

    private View setupEditorDialog(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.fields_editor, null);

        TextView tvTitle = view.findViewById(R.id.editorTitle);
        TextInputEditText editText = view.findViewById(R.id.editorEditText);
        MaterialButton btnCancel = view.findViewById(R.id.cancelBtn);
        MaterialButton btnSave = view.findViewById(R.id.saveBtn);

        if (getArguments() != null) {
            tvTitle.setText(getArguments().getString(ARG_TITLE));
            editText.setText(getArguments().getString(ARG_CURRENT_TEXT));
            editText.requestFocus();
        }

        btnCancel.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {
            String newText = editText.getText().toString().trim();
            String key = getArguments().getString(ARG_PREFS_KEY);
            if (!newText.isEmpty()) {
                listener.onFieldSaved(key, newText);
            }
            dismiss();
        });

        return view;
    }
}