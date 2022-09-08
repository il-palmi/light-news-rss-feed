package com.example.newsrss;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;


public class AddFeedDialog extends DialogFragment{
    public interface AddFeedDialogListener {
        public void onAddFeedDialogPositiveClick(DialogFragment dialog, String feedName, String feedUrl);
    }

    AddFeedDialogListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = (AddFeedDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(getActivity().toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_feed, null);

        builder.setView(view)
                .setTitle("Add new feed")
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText editTextFeedName = view.findViewById(R.id.editTextFeedName);
                        EditText editTextFeedUrl = view.findViewById(R.id.editTextFeedUrl);
                        listener.onAddFeedDialogPositiveClick(AddFeedDialog.this,
                                editTextFeedName.getText().toString(),
                                editTextFeedUrl.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Objects.requireNonNull(AddFeedDialog.this.getDialog()).cancel();
                    }
                });

        return builder.create();

    }
}
