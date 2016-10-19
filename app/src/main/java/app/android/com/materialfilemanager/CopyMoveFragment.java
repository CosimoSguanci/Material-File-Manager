package app.android.com.materialfilemanager;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
 * Created by Cosimo Sguanci on 12/10/2016.
 */

public class CopyMoveFragment extends Fragment {
    private static final String TAG_FRAGMENT = "TAG_FRAGMENT";
    private static final int VERTICAL_ITEM_SPACE = 48;
    private List<Item> dir;
    private File currentDir;
    private FileAdapter adapter;
    private RecyclerView recyclerView;
    private Stack<String> pathStack;
    private TextView textCurrentPath;
    private OnFolderSelectedListener listener;


    public static CopyMoveFragment newIstance() {
        return new CopyMoveFragment();
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FloatingActionMenu fabMain = (FloatingActionMenu) getActivity().findViewById(R.id.fabMain);
        fabMain.setVisibility(View.INVISIBLE);
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();


        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {


                        if (!pathStack.isEmpty()) {
                            currentDir = new File(pathStack.pop());
                            setupData(currentDir);
                            adapter.notifyDataSetChanged();
                            textCurrentPath.setText(currentDir.getAbsolutePath());
                        } else {
                            FragmentManager fragmentManager = getFragmentManager();
                            FragmentTransaction ft = fragmentManager.beginTransaction().replace(R.id.fragmentContainer, MainFragment.newIstance(), TAG_FRAGMENT);
                            ft.commit();
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

        return inflater.inflate(R.layout.fragment_copy, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        View parentLayout = getActivity().findViewById(R.id.rootView);
        Snackbar snackbar = Snackbar
                .make(parentLayout, "Choose the copy/move folder", Snackbar.LENGTH_LONG);

        snackbar.show();

        getView().findViewById(R.id.doneFab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onFolderSelected(currentDir.getAbsolutePath());
            }
        });
        pathStack = new Stack<>();
        textCurrentPath = (TextView) getView().findViewWithTag("textViewCurrentDir");
        textCurrentPath.setText(Environment.getExternalStorageDirectory().getAbsolutePath());
        recyclerView = (RecyclerView) getView().findViewById(R.id.recycler_view);
        currentDir = Environment.getExternalStorageDirectory();
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new VerticalSpaceRecycler(VERTICAL_ITEM_SPACE));
        recyclerView.addItemDecoration(new DividerItemRecycler(getActivity(), R.drawable.divider));
        setupData(currentDir);

    }

    /**
     * Method that creates the ArrayList for showing the directory to the user
     *
     * @param f
     */
    public void setupData(File f) {
        File[] dirs = f.listFiles();
        dir = new ArrayList<>(); // Only 1 ArrayList is needed, as we want show only directories
        try {
            for (File ff : dirs) {
                Date lastModDate = new Date(ff.lastModified());
                DateFormat formater = DateFormat.getDateTimeInstance();
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
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.sort(dir);
        setupAdapter();


    }

    private void setupAdapter() {

        adapter = new FileAdapter(getContext(), R.layout.recycler_row, dir);
        adapter.setOnFileClickedListener(new FileAdapter.onFileClickedListener() {
            @Override
            public void onFileClick(String newPath) {
                if (currentDir != null) {
                    pathStack.push(currentDir.getPath());
                }
                currentDir = new File(newPath);
                setupData(currentDir);
                adapter.notifyDataSetChanged();
                textCurrentPath.setText(currentDir.getAbsolutePath());


            }

            @Override
            public void onLongFileClick(String path) {
                // The App hasn't to react to a LongClick here
            }
        });


        recyclerView.setAdapter(adapter);
    }

    /**
     * Communication between Fragment and Activity
     *
     * @param activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof DialogLongPress.onLongPressActions) {
            listener = (CopyMoveFragment.OnFolderSelectedListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnFolderSelected");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface OnFolderSelectedListener {
        void onFolderSelected(String path);
    }
}