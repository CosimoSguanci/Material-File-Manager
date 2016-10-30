package app.android.com.materialfilemanager;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Stack;

/**
 * Created by shind on 16/10/2016.
 */

public class SearchResultsFragment extends Fragment {
    private final static String TAG_FRAGMENT = "TAG_FRAGMENT";
    private static final int VERTICAL_ITEM_SPACE = 48;
    private static LinearLayout parentLayout;
    private RecyclerView recyclerView;
    private TextView textCurrentPath; // String used to display the current path visited
    private String searchName;
    private TextView noResultsTextView;
    private TextView emptyTextView;
    private List<Item> results;
    private File currentDir = null;
    private FileAdapter adapter;
    private String tmpPathBuff;
    private boolean exceptionLaunched = false;
    private Stack<String> pathStack;
    private List<Item> fls;
    private List<String> checkedPaths;


    public static SearchResultsFragment newIstance() {
        return new SearchResultsFragment();
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        /**
         * When the back button is pressed, SHOULD show the previous directory.
         */
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {

                        ((MainActivity) getActivity()).setFalseOnSearchItem();
                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction ft = fragmentManager.beginTransaction().replace(R.id.fragmentContainer, MainFragment.newIstance(), TAG_FRAGMENT);
                        ft.commit();

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
        emptyTextView = new TextView(getContext());
        parentLayout = (LinearLayout) getActivity().findViewById(R.id.rootView);
        emptyTextView.setVisibility(View.VISIBLE);
        parentLayout.addView(emptyTextView);
        textCurrentPath = (TextView) getView().findViewWithTag("textViewCurrentDir");
        textCurrentPath.setVisibility(View.GONE);
        FloatingActionMenu fabMain = (FloatingActionMenu) getActivity().findViewById(R.id.fabMain);
        fabMain.setVisibility(View.GONE);
        recyclerView = (RecyclerView) getView().findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new VerticalSpaceRecycler(VERTICAL_ITEM_SPACE));
        recyclerView.addItemDecoration(new DividerItemRecycler(getActivity(), R.drawable.divider));
        searchName = ((MainActivity) getActivity()).getSearchName();
        new SetupSearchResults(recyclerView, Environment.getExternalStorageDirectory().listFiles(), searchName).execute(); // Get an error: No adapter attached; skipping layout


    }

    public void deleteItemFromArrayResults(List<String> paths) {
        for (String path : paths) {
            int i = 0;
            while (!path.equals(results.get(i).getPath())) {
                i++;
            }
            results.remove(i);
        }

        setupAdapter();

    }

    public void nameChanged(String previousName, String newName) {
        int i = 0;
        while (!previousName.equals(results.get(i).getName())) {
            i++;
        }
        results.get(i).setName(newName);
        setupAdapter();
    }

    private void setupAdapter() {
        exceptionLaunched = false;
        adapter = new FileAdapter(getContext(), R.layout.recycler_row, results);
        adapter.setOnFileClickedListener(new FileAdapter.onFileClickedListener() {
            /**
             * Recycler View item clicked
             * @param newPath
             */
            @Override
            public void onFileClick(String newPath) {
                FloatingActionMenu fabMain = (FloatingActionMenu) getActivity().findViewById(R.id.fabMain);
                fabMain.close(true);
                if (currentDir != null) {
                    tmpPathBuff = currentDir.getAbsolutePath();
                }
                currentDir = new File(newPath);
                if (!currentDir.isDirectory()) {
                    /**
                     * If file isn't a directory, here is decided the intent to be used based on Mime Type from extension
                     */
                    try {
                        String fileName = currentDir.getName();
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

            /**
             * Recycler View item long clicked, Multiple Actions fragment launched
             * @param itemsChecked
             */
            @Override
            public void onLongFileClick(List<Item> itemsChecked) {
                checkedPaths = new ArrayList<>();
                int i = 0;
                FloatingActionMenu fabMain = (FloatingActionMenu) getActivity().findViewById(R.id.fabMain);
                fabMain.close(true);
                for (Item it : itemsChecked) {
                    checkedPaths.add(itemsChecked.get(i).getPath());
                    i++;
                }
                ((MainActivity) getActivity()).setOnLongPressPaths(checkedPaths);
                FragmentManager fm = getFragmentManager();
                DialogLongPress dialog = DialogLongPress.newIstance("Actions", itemsChecked.size() > 1);
                dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
                dialog.show(fm, "fragment_LongPress");

            }
        });

        recyclerView.setAdapter(adapter);
    }

    /**
     * Method that creates the ArrayList that has to be passed to the adapter to populate the RecyclerView
     * This is necessary in the AsyncTask too, to open the folders found by the search Algorithm.
     *
     * @param f
     */
    public void setupData(File f) {
        textCurrentPath.setVisibility(View.VISIBLE);
        File[] dirs = f.listFiles();
        results = new ArrayList<>();
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
                    results.add(new Item(ff.getName(), num_item, date_modify, ff.getAbsolutePath(), "directory_icon"));
                } else {
                    String extension = ff.getName().substring(ff.getName().lastIndexOf('.') + 1);

                    /**
                     * Checking the file extension to decide the icon to set
                     */

                    if (extension.equals("jpg") || extension.equals("png") || extension.equals("bmp"))
                        fls.add(new Item(ff.getName(), getFileSize(ff.length()) + " Byte", date_modify, ff.getAbsolutePath(), "image_icon"));

                    else if (extension.equals("mp3") || extension.equals("flac"))
                        fls.add(new Item(ff.getName(), getFileSize(ff.length()), date_modify, ff.getAbsolutePath(), "music_icon"));

                    else
                        fls.add(new Item(ff.getName(), getFileSize(ff.length()), date_modify, ff.getAbsolutePath(), "file_icon"));

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.sort(results);
        Collections.sort(fls);
        results.addAll(fls);
        if (results.isEmpty()) {
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


    /**
     * AsyncTask used to load asynchronously the results of the User research of file.
     */
    class SetupSearchResults extends AsyncTask<Void, Void, List<Item>> {

        private File[] files;
        private String searchName;
        private ProgressDialog progress;

        public SetupSearchResults(RecyclerView recyclerView_passed, File[] files, String searchName) {
            recyclerView = recyclerView_passed;
            this.files = files;
            this.searchName = searchName;
            results = new ArrayList<>();
            progress = new ProgressDialog(getActivity());
            pathStack = new Stack<>();
        }

        protected void onPreExecute() {
            super.onPreExecute();
            progress.setTitle("");
            progress.setMessage("Searching...");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.show();

        }

        protected List<Item> doInBackground(Void... params) {
            return setupSearchResults(searchName, files);

        }

        protected void onPostExecute(List<Item> results) {
            if (results.isEmpty()) {
                Snackbar snackbar = Snackbar.make(parentLayout, R.string.no_results, Snackbar.LENGTH_LONG);
                snackbar.show();
                noResultsTextView = new TextView(getContext());
                noResultsTextView.setText(getString(R.string.no_results));
                noResultsTextView.setTextSize(20);
                noResultsTextView.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                noResultsTextView.setLayoutParams(params);
                parentLayout.addView(noResultsTextView);

            }
            setupAdapter();
            progress.dismiss();

        }

        /**
         * Recursive method for the file search
         *
         * @param searchName -the name of the file that has to be found.
         * @param files      -the list of directory's files.
         * @return results   -ArrayList that contained the Items that correspond to the name searched.
         */
        private List<Item> setupSearchResults(String searchName, File[] files) {
            for (File f : files) {
                if (f.getName().contains(searchName) || f.getName().contains(searchName.toLowerCase()) || f.getName().contains(searchName.toUpperCase())) {
                    Date lastModDate = new Date(f.lastModified());
                    DateFormat formater = DateFormat.getDateInstance();
                    String date_modify = formater.format(lastModDate);
                    if (f.isDirectory()) {
                        File[] dirFls = f.listFiles();
                        int buf;
                        if (dirFls != null) {
                            buf = dirFls.length;
                        } else buf = 0;
                        String num_item = String.valueOf(buf);
                        if (buf == 1)
                            num_item = num_item + " item";
                        else
                            num_item = num_item + " items";
                        results.add(new Item(f.getName(), num_item, date_modify, f.getAbsolutePath(), "directory_icon"));
                    } else {
                        String extension = f.getName().substring(f.getName().lastIndexOf('.') + 1);

                        /**
                         * Checking the file extension to decide the icon to set
                         */

                        if (extension.equals("jpg") || extension.equals("png") || extension.equals("bmp"))
                            results.add(new Item(f.getName(), getFileSize(f.length()), date_modify, f.getAbsolutePath(), "image_icon"));

                        else if (extension.equals("mp3") || extension.equals("flac"))
                            results.add(new Item(f.getName(), getFileSize(f.length()), date_modify, f.getAbsolutePath(), "music_icon"));

                        else
                            results.add(new Item(f.getName(), getFileSize(f.length()), date_modify, f.getAbsolutePath(), "file_icon"));

                    }

                }
                if (f.isDirectory()) {
                    /**
                     *  Recursion only if ff (the file examinated) is a directory.
                     */
                    setupSearchResults(searchName, f.listFiles());
                }

            }
            return results;

        }


    }
}
