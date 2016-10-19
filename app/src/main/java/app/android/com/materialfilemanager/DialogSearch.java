package app.android.com.materialfilemanager;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;

import com.github.clans.fab.FloatingActionMenu;

/**
 * Created by Cosimo Sguanci on 16/10/2016.
 */

public class DialogSearch extends DialogFragment {


    private EditText searchText;
    private onSearchListener listener;

    public DialogSearch() {
    }

    public static DialogSearch newIstance(String title) {
        DialogSearch frag = new DialogSearch();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        FloatingActionMenu fabMain = (FloatingActionMenu) getActivity().findViewById(R.id.fabMain);
        fabMain.close(true);
        String title = getArguments().getString("title");
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setView(R.layout.fragment_newfile);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                searchText = (EditText) getDialog().findViewById(R.id.edit_text_newfile);
                listener.onNameSearchTyped(searchText.getText().toString());


            }
        });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return alertDialogBuilder.create();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedIstanceState) {

        return inflater.inflate(R.layout.fragment_newfile, container);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedIstanceState) {
        super.onViewCreated(view, savedIstanceState);
        String title = getArguments().getString("title", "Enter name");
        getDialog().setTitle(title);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void onStart() {
        /**
         * Setting Dialog size
         */
        /*
        Window window = getDialog().getWindow();
        window.setLayout(50,200);
        window.setGravity(Gravity.CENTER);
         */
        // Call super onResume after sizing
        super.onStart();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof onSearchListener) {
            listener = (onSearchListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnSearchListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!((MainActivity) getActivity()).getOnSearchItem())
            getActivity().findViewById(R.id.fabMain).setVisibility(View.VISIBLE);


    }

    public interface onSearchListener {
        void onNameSearchTyped(String fileName);

    }
}

