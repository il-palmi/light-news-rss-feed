package com.example.newsrss;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Objects;

public class RemoveFeedDialog extends DialogFragment {
    ArrayList<String> feedNames;
    ArrayList<String> feedUrls;

    public RemoveFeedDialog(ArrayList<String> feed_names, ArrayList<String> feed_urls){
        feedNames = feed_names;
        feedUrls = feed_urls;
    }

    public interface RemoveFeedDialogListener {
        public void onRemoveFeedDialogSelect(DialogFragment dialog, String selected_feed);
    }

    RemoveFeedDialog.RemoveFeedDialogListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            listener = (RemoveFeedDialog.RemoveFeedDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Remove feed")
                .setItems(feedNames.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        listener.onRemoveFeedDialogSelect(
                                RemoveFeedDialog.this,
                                feedUrls.get(which)
                        );
                    }
                });
        return builder.create();
    }
}