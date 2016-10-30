package app.android.com.materialfilemanager;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * Created by Cosimo Sguanci on 10/10/2016.
 */

public class DialogLongPress extends DialogFragment {


    private static boolean multipleSelection = false;
    private onLongPressActions listener;

    public DialogLongPress() {
    }


    public static DialogLongPress newIstance(String title, boolean multiple_Selection) {
        multipleSelection = multiple_Selection;
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
        if (!multipleSelection)
            alertDialogBuilder.setView(R.layout.fragment_long_click);
        else
            alertDialogBuilder.setView(R.layout.fragment_long_click_multipleselection);
        return alertDialogBuilder.create();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedIstanceState) {
        if (!multipleSelection)
            return inflater.inflate(R.layout.fragment_long_click, container);
        else {
            return inflater.inflate(R.layout.fragment_long_click_multipleselection, container);
        }

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
        if (!multipleSelection) {
            getDialog().findViewById(R.id.renameButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onRenameClicked();
                    dismiss();
                }
            });
            getDialog().findViewById(R.id.unzipButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onUnZipClicked();
                    dismiss();
                }
            });
        }
        getDialog().findViewById(R.id.zipButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onZipClicked();
                dismiss();
            }
        });


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

        void onZipClicked();

        void onUnZipClicked();
    }


}

