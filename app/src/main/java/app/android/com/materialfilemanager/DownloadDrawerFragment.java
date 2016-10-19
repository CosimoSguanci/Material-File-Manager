package app.android.com.materialfilemanager;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Stack;

/**
 * Created by Cosimo Sguanci on 13/09/2016.
 */
public class DownloadDrawerFragment extends Fragment {
    private static final int VERTICAL_ITEM_SPACE = 48;
    private static LinearLayout parentLayout;
    private RecyclerView recyclerViewDownloads;
    private File currentDir;
    private Stack<String> pathStack;
    private List<Item> dir;
    private List<Item> fls;
    private FileAdapter adapter;
    private String fileName = null;
    private TextView textCurrentPath;
    private TextView emptyTextView;
    private String tmpPathBuff;
    private boolean exceptionLaunched = false;

    public static DownloadDrawerFragment newIstance() {
        return new DownloadDrawerFragment();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, parent, false);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        pathStack = new Stack<>();
        parentLayout = (LinearLayout) getActivity().findViewById(R.id.rootView);
        FloatingActionMenu fabMain = (FloatingActionMenu) getActivity().findViewById(R.id.fabMain);
        fabMain.setVisibility(View.VISIBLE);
        textCurrentPath = (TextView) getView().findViewWithTag("textViewCurrentDir");
        textCurrentPath.setText(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
        recyclerViewDownloads = (RecyclerView) getView().findViewById(R.id.recycler_view);
        currentDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); // Setting current directory to Downloads default directory
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerViewDownloads.setLayoutManager(mLayoutManager);
        recyclerViewDownloads.setItemAnimator(new DefaultItemAnimator());
        recyclerViewDownloads.addItemDecoration(new VerticalSpaceRecycler(VERTICAL_ITEM_SPACE));
        recyclerViewDownloads.addItemDecoration(new DividerItemRecycler(getActivity(), R.drawable.divider));
        setupData(currentDir);
        setupAdapter();


    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();

        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (emptyTextView != null) {
                            emptyTextView.setVisibility(View.GONE);
                        }
                        if (!pathStack.isEmpty()) {
                            currentDir = new File(pathStack.pop());
                            setupData(currentDir);
                            adapter.notifyDataSetChanged();
                            textCurrentPath.setText(currentDir.getAbsolutePath());
                        } else {
                            getActivity().finish();
                        }

                        return true;
                    }
                }
                return false;
            }
        });
    }

    public void setupData(File f) {

        File[] dirs = f.listFiles();
        dir = new ArrayList<>();
        fls = new ArrayList<>();


        try {
            for (File ff : dirs) {
                Date lastModDate = new Date(ff.lastModified());
                DateFormat formater = DateFormat.getDateTimeInstance();
                String date_modify = formater.format(lastModDate);
                if (ff.isDirectory()) {
                    File[] dirFls = ff.listFiles();
                    int buf = 0;
                    if (dirFls != null) {
                        buf = dirFls.length;
                    } else buf = 0;
                    String num_item = String.valueOf(buf);
                    if (buf == 1)
                        num_item = num_item + " item";
                    else
                        num_item = num_item + " items";
                    dir.add(new Item(ff.getName(), num_item, date_modify, ff.getAbsolutePath(), "directory_icon"));
                } else {
                    fileName = ff.getName();
                    fileName = fileName.substring(fileName.lastIndexOf('.') + 1);
                    if (fileName.equals("jpg") || fileName.equals("png") || fileName.equals("bmp"))
                        fls.add(new Item(ff.getName(), ff.length() + " Byte", date_modify, ff.getAbsolutePath(), "image_icon"));
                    else
                        fls.add(new Item(ff.getName(), ff.length() + " Byte", date_modify, ff.getAbsolutePath(), "file_icon"));
                }


            }
        } catch (Exception e) {
        }
        Collections.sort(dir);
        Collections.sort(fls);
        dir.addAll(fls);
        if (dir.isEmpty()) {
            Snackbar snackbar = Snackbar.make(parentLayout, R.string.empty_folder, Snackbar.LENGTH_SHORT);
            snackbar.show();
            emptyTextView = new TextView(getContext());
            emptyTextView.setText(getString(R.string.empty_folder));
            emptyTextView.setTextSize(20);
            emptyTextView.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            emptyTextView.setLayoutParams(params);
            emptyTextView.setVisibility(View.VISIBLE);
            parentLayout.addView(emptyTextView);
        }
        setupAdapter();

    }

    private void setupAdapter() {
        exceptionLaunched = false;
        adapter = new FileAdapter(getContext(), R.layout.recycler_row, dir);

        adapter.setOnFileClickedListener(new FileAdapter.onFileClickedListener() {
            @Override
            public void onFileClick(String newPath) {
                if (currentDir != null) {
                    tmpPathBuff = currentDir.getAbsolutePath();
                }
                currentDir = new File(newPath);
                if (!currentDir.isDirectory()) {

                    try {
                        fileName = currentDir.getName();
                        fileName = fileName.substring(fileName.lastIndexOf('.') + 1);
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        Uri uri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID + ".provider", currentDir);
                        intent.setDataAndType(uri, MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Snackbar snackbar = Snackbar.make(parentLayout, R.string.acitivty_not_found, Snackbar.LENGTH_SHORT);
                        snackbar.show();
                        exceptionLaunched = true;
                    }


                } else {
                    setupData(currentDir);
                    adapter.notifyDataSetChanged();
                    textCurrentPath.setText(currentDir.getAbsolutePath());

                }
                if (!exceptionLaunched)
                    pathStack.push(tmpPathBuff);


            }

            @Override
            public void onLongFileClick(String path) {
                FloatingActionMenu fabMain = (FloatingActionMenu) getActivity().findViewById(R.id.fabMain);
                fabMain.close(true);
                ((MainActivity) getActivity()).setOnLongPressPath(path);
                FragmentManager fm = getFragmentManager();
                DialogLongPress dialog = DialogLongPress.newIstance("Actions");
                dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
                dialog.show(fm, "fragment_LongPress");
            }
        });
        recyclerViewDownloads.setAdapter(adapter);
    }


    public File getCurrentDir() {
        return currentDir;
    }
}
