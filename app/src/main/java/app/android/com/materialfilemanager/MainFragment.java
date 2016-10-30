package app.android.com.materialfilemanager;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Stack;

/**
 * Created by Cosimo Sguanci on 08/09/2016.
 */
public class MainFragment extends Fragment {
    private static final int VERTICAL_ITEM_SPACE = 48;
    private static LinearLayout parentLayout;
    private List<Item> dir;
    private List<Item> fls;
    private String fileName = null;
    private File currentDir;
    private FileAdapter adapter;
    private RecyclerView recyclerView;
    private Stack<String> pathStack; // Stack that contains the navigation history
    private TextView textCurrentPath; // String used to display the current path visited
    private File[] filesDirs;
    private TextView emptyTextView;
    private String tmpPathBuff; // String to keep the previous path before pushing it on the stack
    private boolean exceptionLaunched = false; // String to check if ActivityNotFoundException has been launched
    private boolean hasExternalSD = false;
    private List<String> checkedPaths;
    private int scrollPosition;


    public static MainFragment newIstance() {
        return new MainFragment();
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        /**
         * When the back button is pressed, the previous directory is shown
         */
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (emptyTextView != null) {
                            emptyTextView.setVisibility(View.GONE);
                        }
                        FloatingActionMenu fabMain = (FloatingActionMenu) getActivity().findViewById(R.id.fabMain);
                        fabMain.close(true);
                        if (!pathStack.isEmpty()) {
                            currentDir = new File(pathStack.pop());
                            setupData(currentDir);
                            adapter.notifyDataSetChanged();
                            textCurrentPath.setText(currentDir.getAbsolutePath());
                        } else {
                            /**
                             * Should handle the case hasRemovableSD==true, and return to the initial choose bewteen the SDs
                             */
                            getActivity().finish();
                        }
                        return true;
                    }
                }
                return false;
            }
        });


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        ((MainActivity) getActivity()).setCurrentFragment("First");
        pathStack = new Stack<>();
        emptyTextView = new TextView(getContext());
        parentLayout = (LinearLayout) getActivity().findViewById(R.id.rootView);
        emptyTextView.setVisibility(View.VISIBLE);
        parentLayout.addView(emptyTextView);
        FloatingActionMenu fabMain = (FloatingActionMenu) getActivity().findViewById(R.id.fabMain);
        fabMain.setVisibility(View.VISIBLE);
        textCurrentPath = (TextView) getView().findViewWithTag("textViewCurrentDir");
        recyclerView = (RecyclerView) getView().findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new VerticalSpaceRecycler(VERTICAL_ITEM_SPACE));
        recyclerView.addItemDecoration(new DividerItemRecycler(getActivity(), R.drawable.divider));
        // Should Hide FAB on Scroll of RecyclerView?
        filesDirs = ContextCompat.getExternalFilesDirs(getContext(), null);
        /**
         * Checking if there is removable sd card available
         */
        if (filesDirs.length > 1) {
            hasExternalSD = true;
            textCurrentPath.setVisibility(View.GONE);
            fabMain.setVisibility(View.GONE);
            setupMultipleStorage(filesDirs);
        } else {

            currentDir = Environment.getExternalStorageDirectory(); // Getting root directory (internal storage)
            textCurrentPath.setText(currentDir.getAbsolutePath());
            setupData(currentDir);
        }

    }

    /**
     * Method that creates the ArrayList that has to be passed to the adapter to populate the RecyclerView
     *
     * @param f
     */
    public void setupData(File f) {
        textCurrentPath.setVisibility(View.VISIBLE);
        getActivity().findViewById(R.id.fabMain).setVisibility(View.VISIBLE);
        File[] dirs = f.listFiles();
        dir = new ArrayList<>();
        fls = new ArrayList<>();
        try {
            for (File ff : dirs) {
                Date lastModDate = new Date(ff.lastModified());
                DateFormat formater = DateFormat.getDateInstance();
                String date_modify = formater.format(lastModDate);
                if (ff.isDirectory()) {
                    File[] dirFls = ff.listFiles();
                    int buf;
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

                    /**
                     * Checking the file extension to decide the icon to set
                     */

                    if (fileName.equals("jpg") || fileName.equals("png") || fileName.equals("bmp"))
                        fls.add(new Item(ff.getName(), getFileSize(ff.length()), date_modify, ff.getAbsolutePath(), "image_icon"));

                    else if (fileName.equals("mp3") || fileName.equals("flac"))
                        fls.add(new Item(ff.getName(), getFileSize(ff.length()), date_modify, ff.getAbsolutePath(), "music_icon"));

                    else
                        fls.add(new Item(ff.getName(), getFileSize(ff.length()), date_modify, ff.getAbsolutePath(), "file_icon"));

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.sort(dir);
        Collections.sort(fls);
        dir.addAll(fls);
        if (dir.isEmpty()) {
            Snackbar snackbar = Snackbar.make(parentLayout, R.string.empty_folder, Snackbar.LENGTH_SHORT);
            snackbar.show();
            emptyTextView.setText(getString(R.string.empty_folder));
            emptyTextView.setTextSize(20);
            emptyTextView.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            emptyTextView.setLayoutParams(params);
            emptyTextView.setVisibility(View.VISIBLE);
        } else if (emptyTextView.isShown())
            emptyTextView.setVisibility(View.GONE);

        setupAdapter();

    }

    private String getFileSize(long size) {

        if (size <= 0) return "0" + " Byte";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];


    }


    private void setupMultipleStorage(File[] roots) {
        dir = new ArrayList<>();
        String[] rootPaths = new String[2];
        int i = 0;
        for (File f : roots) {
            if (f != null) {
                String path = f.getAbsolutePath();
                int indexMountRoot = path.indexOf("/Android/data/");
                if (indexMountRoot >= 0 && indexMountRoot <= path.length()) {
                    //Get the root path for the external directory
                    rootPaths[i] = path.substring(0, indexMountRoot);
                }
                i++;
            }
        }
        dir.add(new Item(getString(R.string.internal_storage), "", "", rootPaths[0], "directory_icon"));
        dir.add(new Item(getString(R.string.removable_sd), "", "", rootPaths[1], "directory_icon"));
        setupAdapter();
    }


    private void setupAdapter() {

        exceptionLaunched = false;

        adapter = new FileAdapter(getContext(), R.layout.recycler_row, dir, hasExternalSD);

        adapter.setOnFileClickedListener(new FileAdapter.onFileClickedListener() {
            /**
             * Recycler View item clicked
             * @param newPath
             */
            @Override
            public void onFileClick(String newPath) {
                scrollPosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
                /** Variable that is needed if there's a removable sd to add the path to the history folder stack only when the user has chosen the storage.*/
                boolean hadSDbefore = false;
                File fileToOpen;
                FloatingActionMenu fabMain = (FloatingActionMenu) getActivity().findViewById(R.id.fabMain);
                fabMain.close(true);
                if (currentDir != null && hasExternalSD == false) {
                    /**
                     * tmpPathBuff is used to push the path in pathStack
                     * ONLY if ActivityNotFound isn't launched. Otherwise,
                     * when back is pressed the first time, it would remain
                     * in the same folder.
                     */
                    tmpPathBuff = currentDir.getAbsolutePath();
                }
                fileToOpen = new File(newPath);

                if (!fileToOpen.isDirectory()) {
                    /**
                     * If file isn't a directory, here is decided the intent to be used based on Mime Type from extension
                     */
                    try {
                        fileName = fileToOpen.getName();
                        fileName = fileName.substring(fileName.lastIndexOf('.') + 1);
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        Uri uri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID + ".provider", fileToOpen);
                        intent.setDataAndType(uri, MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } catch (Exception e) {
                        Snackbar snackbar = Snackbar.make(parentLayout, R.string.acitivty_not_found, Snackbar.LENGTH_SHORT);
                        snackbar.show();
                        exceptionLaunched = true;
                    }


                } else {
                    currentDir = new File(newPath);
                    hadSDbefore = hasExternalSD;
                    if (pathStack.isEmpty() && hasExternalSD)
                        hasExternalSD = false;
                    setupData(currentDir);
                    adapter.notifyDataSetChanged();
                    textCurrentPath.setText(currentDir.getAbsolutePath());
                }

                if (!exceptionLaunched && hadSDbefore == false && fileToOpen.isDirectory())
                    pathStack.push(tmpPathBuff);
                else if (hasExternalSD)
                    hasExternalSD = false;

            }

            /**
             * Recycler View item long clicked, Multiple Actions fragment gets created
             * @param itemsChecked
             */
            @Override
            public void onLongFileClick(List<Item> itemsChecked) {
                if (!hasExternalSD) {
                    checkedPaths = new ArrayList<>();
                    int i = 0;
                    FloatingActionMenu fabMain = (FloatingActionMenu) getActivity().findViewById(R.id.fabMain);
                    fabMain.close(true);
                    for (Item it : itemsChecked) {
                        checkedPaths.add(itemsChecked.get(i).getPath());
                        Log.w("ItemsChecked", ": " + itemsChecked.get(i).getName());
                        i++;
                    }
                    ((MainActivity) getActivity()).setOnLongPressPaths(checkedPaths);
                    FragmentManager fm = getFragmentManager();
                    DialogLongPress dialog = DialogLongPress.newIstance("Actions", itemsChecked.size() > 1);
                    dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
                    dialog.show(fm, "fragment_LongPress");
                }
            }
        });
        (recyclerView.getLayoutManager()).scrollToPosition(scrollPosition);
        recyclerView.setAdapter(adapter);
    }


    public File getCurrentDir() {
        return currentDir;
    }


}
