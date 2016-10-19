package app.android.com.materialfilemanager;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by Cosimo Sguanci on 10/10/2016.
 */

public class DialogLongPress extends DialogFragment {


    private onLongPressActions listener;

    public DialogLongPress() {
    }


    public static DialogLongPress newIstance(String title) {
        DialogLongPress frag = new DialogLongPress();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString("title");
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setView(R.layout.fragment_long_click);
        return alertDialogBuilder.create();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedIstanceState) {

        return inflater.inflate(R.layout.fragment_long_click, container);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedIstanceState) {
        super.onViewCreated(view, savedIstanceState);
        String title = getArguments().getString("title", "Enter name");
        getDialog().setTitle(title);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

    }

    @Override
    public void onResume() {
        getDialog().findViewById(R.id.deleteButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onDeleteClicked();
                dismiss();
            }
        });
        getDialog().findViewById(R.id.copyButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onCopyClicked();
                dismiss();
            }
        });
        getDialog().findViewById(R.id.moveButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onMoveClicked();
                dismiss();
            }
        });
        getDialog().findViewById(R.id.renameButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onRenameClicked();
                dismiss();
            }
        });

        /**
         * Setting Dialog size
         */
        Window window = getDialog().getWindow();
        Point size = new Point();
        // Store dimensions of the screen in `size`
        Display display = window.getWindowManager().getDefaultDisplay();
        display.getSize(size);
        window.setLayout((int) (size.y * 0.40), (int) (size.x * 0.60));
        window.setGravity(Gravity.CENTER);
        // Call super onResume after sizing
        super.onResume();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof onLongPressActions) {
            listener = (onLongPressActions) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnLongPressActions");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface onLongPressActions {
        void onDeleteClicked();

        void onMoveClicked();

        void onCopyClicked();

        void onRenameClicked();
    }


}

