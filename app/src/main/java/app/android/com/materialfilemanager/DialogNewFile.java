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
 * Created by Cosimo Sguanci on 03/10/2016.
 */
public class DialogNewFile extends DialogFragment {


    private EditText textNewFile;
    private onNameTypedListener listener;

    public DialogNewFile() {
    }

    public static DialogNewFile newIstance(String title) {
        DialogNewFile frag = new DialogNewFile();
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
                textNewFile = (EditText) getDialog().findViewById(R.id.edit_text_newfile);
                listener.onNameTyped(textNewFile.getText().toString());


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

    /*@Override
    public void onResume() {

        Window window = getDialog().getWindow();
        Point size = new Point();
        // Store dimensions of the screen in `size`
        Display display = window.getWindowManager().getDefaultDisplay();
        display.getSize(size);
        window.setLayout((int) (size.x * 0.75), (int) (size.x * 0.50));
        window.setGravity(Gravity.CENTER);
        // Call super onResume after sizing
        super.onResume();

    }*/

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof onNameTypedListener) {
            listener = (onNameTypedListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnFileTypedListener");
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
        getActivity().findViewById(R.id.fabMain).setVisibility(View.VISIBLE);
        ((MainActivity) getActivity()).setFileOrDirToNull();

    }

    public interface onNameTypedListener {
        void onNameTyped(String fileName);

    }
}
