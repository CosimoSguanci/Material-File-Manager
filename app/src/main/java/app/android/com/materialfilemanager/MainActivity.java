package app.android.com.materialfilemanager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.TouchDelegate;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by Cosimo Sguanci on 08/09/2016.
 */
public class MainActivity extends AppCompatActivity implements DialogNewFile.onNameTypedListener, DialogLongPress.onLongPressActions, CopyMoveFragment.OnFolderSelectedListener, DialogSearch.onSearchListener {
    private final static String TAG_FRAGMENT = "TAG_FRAGMENT";
    private Fragment fragment = null;
    private SettingsFragment settingsFragment = null;
    private boolean hasToTransaction = false; // Boolean to check if the fragment has to change/transact (should check)
    private Toolbar myToolbar;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private ImageView searchImageView;
    private String currentFragment = "First"; // String to check to not change the Fragment if the user tap on it again
    private String fileOrDir = null; // Strings used to determine if the file has typed a new file name or directory name.
    private List<String> onLongPressPaths; // Contains the path of the long pressed file
    private String searchName;
    private boolean copyClicked = false;
    private boolean onSearchItem = false;

    /**
     * Getting the free space on internal storage.
     *
     * @param f -the root directory
     * @return -the free space, truncated at two decimals
     */
    public static String getSpaceAvailable(File f) {
        StatFs stat = new StatFs(f.getPath());
        DecimalFormat df = new DecimalFormat("##.##");
        df.setRoundingMode(RoundingMode.DOWN);
        long bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        if (bytesAvailable <= 0) return "0" + " Byte";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytesAvailable) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(bytesAvailable / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**
         * Navigation View Setup
         */
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = setupDrawerToggle();
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        selectDrawerItem(item);
                        return true;
                    }
                }
        );
        myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.setTitleTextColor(Color.BLACK);
        ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.mipmap.ic_menu_black_24dp);
        ab.setDisplayHomeAsUpEnabled(true);
        searchImageView = (ImageView) findViewById(R.id.searchImageView);
        View parent = findViewById(R.id.toolbarLinearLayout);
        /**
         * Expanding the touchable area of the search image view on the toolbar.
         */
        parent.post(new Runnable() {
            public void run() {
                Rect delegateArea = new Rect();
                ImageView delegate = searchImageView;
                delegate.getHitRect(delegateArea);
                delegateArea.top -= 200;
                delegateArea.bottom += 200;
                delegateArea.left -= 250;
                delegateArea.right += 200;
                TouchDelegate expandedArea = new TouchDelegate(delegateArea,
                        delegate);
                if (View.class.isInstance(delegate.getParent())) {
                    ((View) delegate.getParent())
                            .setTouchDelegate(expandedArea);
                }
            }
        });
        searchImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = getSupportFragmentManager();
                DialogSearch dialog = DialogSearch.newIstance("Global Search");
                dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
                dialog.show(fm, "fragment_search");
                findViewById(R.id.fabMain).setVisibility(View.INVISIBLE);
            }
        });
        drawerLayout.addDrawerListener(drawerToggle);
        View header = navigationView.getHeaderView(0);
        TextView textAvailableSpace = (TextView) header.findViewById(R.id.freeSpaceTextView);
        String space = getSpaceAvailable(Environment.getExternalStorageDirectory());
        textAvailableSpace.setText(getString(R.string.free_space) + " " + space);
        /**
         * Checking permissions (WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE)
         */
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
            int permsRequestCode = 100;
            ActivityCompat.requestPermissions(this, permissions, permsRequestCode);

        } else {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.fragmentContainer, new MainFragment(), TAG_FRAGMENT).commit();
            fragment = MainFragment.newIstance();
        }
        findViewById(R.id.fabNF).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileOrDir = new String("File");
                FragmentManager fm = getSupportFragmentManager();
                DialogNewFile dialog = DialogNewFile.newIstance("New File");
                dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
                dialog.show(fm, "fragment_newfile");
                findViewById(R.id.fabMain).setVisibility(View.INVISIBLE);


            }
        });

        findViewById(R.id.fabND).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileOrDir = new String("Dir");
                FragmentManager fm = getSupportFragmentManager();
                DialogNewFile dialog = DialogNewFile.newIstance("New Directory");
                dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
                dialog.show(fm, "fragment_newDirectory");

            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100:
                if (grantResults.length > 0) {

                    boolean readStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (readStorageAccepted && writeStorageAccepted) {
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        fragmentManager.beginTransaction().replace(R.id.fragmentContainer, new MainFragment(), TAG_FRAGMENT).commit();
                        fragment = MainFragment.newIstance();
                    } else
                        finish(); // Should show an explanation dialog
                }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Avoid Exception when deny storage access and allow after
    }

    public ActionBarDrawerToggle setupDrawerToggle() {
        return new ActionBarDrawerToggle(this, drawerLayout, myToolbar, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state.
             *  I switch the fragments (if needed) here to improve performance,
             *  loading them after the drawer closing animation.
             */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                Class fragmentClass = MainFragment.class;
                switch (currentFragment) {
                    case "First":
                        fragmentClass = MainFragment.class;
                        break;
                    case "Images":
                        fragmentClass = ImagesDrawerFragment.class;
                        break;
                    case "Music":
                        fragmentClass = MusicDrawerFragment.class;
                        break;
                    case "Downloads":
                        fragmentClass = DownloadDrawerFragment.class;
                        break;
                    case "Settings":

                        break;

                }

                if (hasToTransaction) {
                    FloatingActionMenu fabMain = (FloatingActionMenu) findViewById(R.id.fabMain);
                    fabMain.close(true);
                    if (currentFragment.equals("Settings")) {
                        fabMain.setVisibility(View.GONE);
                        settingsFragment = new SettingsFragment();
                        getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, settingsFragment, TAG_FRAGMENT).commit();


                    } else {

                        try {
                            fragment = (Fragment) fragmentClass.newInstance();
                        } catch (Exception e) {
                            e.printStackTrace();

                        }
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        FragmentTransaction ft = fragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment, TAG_FRAGMENT);
                        ft.commit();
                        getSupportFragmentManager().executePendingTransactions();
                    }
                }
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                FloatingActionMenu fabMain = (FloatingActionMenu) findViewById(R.id.fabMain);
                fabMain.close(true);

            }
        };
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { // Menu icon clicked
        FloatingActionMenu fabMain = (FloatingActionMenu) findViewById(R.id.fabMain);
        fabMain.close(true);


        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * The user has selected an item from the Navigation View drawer.
     * I set "currentFragment" to switch to the right fragment in onDrawerClosed() method.
     *
     * @param menuItem
     */
    public void selectDrawerItem(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.rootDirectory:
                if (!currentFragment.equals("First")) {
                    currentFragment = "First";
                    hasToTransaction = true;
                }
                break;

            case R.id.images_selection:
                if (!currentFragment.equals("Images")) {
                    currentFragment = "Images";
                    hasToTransaction = true;
                }
                break;

            case R.id.music_selection:
                if (!currentFragment.equals("Music")) {
                    currentFragment = "Music";
                    hasToTransaction = true;
                }
                break;

            case R.id.downloads_selection:
                if (!currentFragment.equals("Downloads")) {
                    currentFragment = "Downloads";
                    hasToTransaction = true;
                }
                break;
            case R.id.settings_drawer:
                if (!currentFragment.equals("Settings")) {
                    currentFragment = "Settings";
                    hasToTransaction = true;
                }
                break;


        }
        menuItem.setChecked(true);
        drawerLayout.closeDrawers();

    }


    @Override
    public void onResume() {
        super.onResume();


    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
            drawerLayout.closeDrawers();

        }
    }

    /**
     * User has typed a new File or Directory name
     *
     * @param fileName
     */
    @Override
    public void onNameTyped(String fileName) {
        /**
         * Should I use the switch statement only to check the fragment type?
         */
        File newFile;
        switch (currentFragment) {

            case "First":
                MainFragment mFrag = (MainFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                /**
                 * Check if the user want to create a new file or directory
                 */
                try {
                    if (fileOrDir.equals("File")) {
                        newFile = new File(mFrag.getCurrentDir().getAbsolutePath(), fileName);
                        if (!newFile.exists()) {
                            try {
                                newFile.createNewFile();


                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        /**
                         * If the file already exists, a copy gets created
                         */
                        else {
                            newFile = new File(mFrag.getCurrentDir().getAbsolutePath(), fileName + "-copy");
                            try {
                                newFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        mFrag.setupData(mFrag.getCurrentDir());
                        View parentLayout = findViewById(R.id.rootView);
                        Snackbar snackbar = Snackbar.make(parentLayout, R.string.file_created, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    } else if (fileOrDir.equals("Dir")) {
                        newFile = new File(mFrag.getCurrentDir() + "/" + fileName);
                        if (!newFile.exists()) {
                            newFile.mkdir();
                        } else {
                            newFile = new File(mFrag.getCurrentDir() + "/" + fileName + "-copy");
                            newFile.mkdir();
                        }
                        mFrag.setupData(mFrag.getCurrentDir());
                        View parentLayout = findViewById(R.id.rootView);
                        Snackbar snackbar = Snackbar.make(parentLayout, R.string.directory_created, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                } catch (NullPointerException e) {
                    /**
                     * if fileOrDir is null, user pressed Rename Button
                     */
                    File f = new File(onLongPressPaths.get(0));
                    f.renameTo(new File(mFrag.getCurrentDir() + "/" + fileName));
                    mFrag.setupData(mFrag.getCurrentDir());
                    View parentLayout = findViewById(R.id.rootView);
                    Snackbar snackbar = Snackbar.make(parentLayout, R.string.file_renamed, Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
                break;

            case "Images":
                ImagesDrawerFragment iFrag = (ImagesDrawerFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                try {
                    if (fileOrDir.equals("File")) {
                        newFile = new File(iFrag.getCurrentDir().getAbsolutePath(), fileName);
                        if (!newFile.exists()) {
                            try {
                                newFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            newFile = new File(iFrag.getCurrentDir().getAbsolutePath(), fileName + "-copy");
                            try {
                                newFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        iFrag.setupData(iFrag.getCurrentDir());
                        View parentLayout = findViewById(R.id.rootView);
                        Snackbar snackbar = Snackbar.make(parentLayout, R.string.file_created, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    } else if (fileOrDir.equals("Dir")) {
                        newFile = new File(iFrag.getCurrentDir() + "/" + fileName);
                        if (!newFile.exists()) {
                            newFile.mkdir();
                        } else {
                            newFile = new File(iFrag.getCurrentDir() + "/" + fileName + "-copy");
                            newFile.mkdir();
                        }
                        iFrag.setupData(iFrag.getCurrentDir());
                        View parentLayout = findViewById(R.id.rootView);
                        Snackbar snackbar = Snackbar.make(parentLayout, R.string.directory_created, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                } catch (NullPointerException e) {
                    /**
                     * if fileOrDir is null, user pressed Rename Button
                     */
                    File f = new File(onLongPressPaths.get(0));
                    f.renameTo(new File(iFrag.getCurrentDir() + "/" + fileName));
                    iFrag.setupData(iFrag.getCurrentDir());
                    View parentLayout = findViewById(R.id.rootView);
                    Snackbar snackbar = Snackbar.make(parentLayout, R.string.file_renamed, Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
                break;

            case "Music":
                MusicDrawerFragment muFrag = (MusicDrawerFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                try {
                    if (fileOrDir.equals("File")) {
                        newFile = new File(muFrag.getCurrentDir().getAbsolutePath(), fileName);
                        if (!newFile.exists()) {
                            try {
                                newFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            newFile = new File(muFrag.getCurrentDir().getAbsolutePath(), fileName + "-copy");
                            try {
                                newFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        muFrag.setupData(muFrag.getCurrentDir());
                        View parentLayout = findViewById(R.id.rootView);
                        Snackbar snackbar = Snackbar.make(parentLayout, R.string.file_created, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    } else if (fileOrDir.equals("Dir")) {
                        newFile = new File(muFrag.getCurrentDir() + "/" + fileName);
                        if (!newFile.exists()) {
                            newFile.mkdir();
                        } else {
                            newFile = new File(muFrag.getCurrentDir() + "/" + fileName + "-copy");
                            newFile.mkdir();
                        }
                        muFrag.setupData(muFrag.getCurrentDir());
                        View parentLayout = findViewById(R.id.rootView);
                        Snackbar snackbar = Snackbar.make(parentLayout, R.string.directory_created, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                } catch (NullPointerException e) {
                    /**
                     * if fileOrDir is null, user pressed Rename Button
                     */
                    File f = new File(onLongPressPaths.get(0));
                    f.renameTo(new File(muFrag.getCurrentDir() + "/" + fileName));
                    muFrag.setupData(muFrag.getCurrentDir());
                    View parentLayout = findViewById(R.id.rootView);
                    Snackbar snackbar = Snackbar.make(parentLayout, R.string.file_renamed, Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
                break;

            case "Downloads":
                DownloadDrawerFragment dFrag = (DownloadDrawerFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                try {
                    if (fileOrDir.equals("File")) {
                        newFile = new File(dFrag.getCurrentDir().getAbsolutePath(), fileName);
                        if (!newFile.exists()) {
                            try {
                                newFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            newFile = new File(dFrag.getCurrentDir().getAbsolutePath(), fileName + "-copy");
                            try {
                                newFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        dFrag.setupData(dFrag.getCurrentDir());
                        View parentLayout = findViewById(R.id.rootView);
                        Snackbar snackbar = Snackbar.make(parentLayout, R.string.file_created, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    } else if (fileOrDir.equals("Dir")) {
                        newFile = new File(dFrag.getCurrentDir() + "/" + fileName);
                        if (!newFile.exists()) {
                            newFile.mkdir();
                        } else {
                            newFile = new File(dFrag.getCurrentDir() + "/" + fileName + "-copy");
                            newFile.mkdir();
                        }
                        dFrag.setupData(dFrag.getCurrentDir());
                        View parentLayout = findViewById(R.id.rootView);
                        Snackbar snackbar = Snackbar.make(parentLayout, R.string.directory_created, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                } catch (NullPointerException e) {
                    /**
                     * if fileOrDir is null, user pressed Rename Button
                     */
                    File f = new File(onLongPressPaths.get(0));
                    f.renameTo(new File(dFrag.getCurrentDir() + "/" + fileName));
                    dFrag.setupData(dFrag.getCurrentDir());
                    View parentLayout = findViewById(R.id.rootView);
                    Snackbar snackbar = Snackbar.make(parentLayout, R.string.file_renamed, Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
                break;

            case "Search":
                SearchResultsFragment sFrag = (SearchResultsFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);


                File f = new File(onLongPressPaths.get(0));
                String previousName = f.getName();
                int n = onLongPressPaths.get(0).length();
                int x = f.getName().length();
                f.renameTo(new File(onLongPressPaths.get(0).substring(0, n - x) + "/" + fileName));
                sFrag.nameChanged(previousName, fileName);
                View parentLayout = findViewById(R.id.rootView);
                Snackbar snackbar = Snackbar.make(parentLayout, R.string.file_renamed, Snackbar.LENGTH_LONG);
                snackbar.show();

                break;
        }


    }


    @Override
    public void onDeleteClicked() {
        for (String path : onLongPressPaths) {
            File deleteFile = new File(path);
            switch (currentFragment) {
                case "First":
                    MainFragment mFrag = (MainFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                    if (deleteFile.isDirectory()) {
                        File[] lf = deleteFile.listFiles();
                        if (!(lf.length == 0))
                            for (File f : lf) {
                                f.delete();
                            }
                    }
                    deleteFile.delete();
                    mFrag.setupData(mFrag.getCurrentDir());

                    break;
                case "Images":
                    ImagesDrawerFragment iFrag = (ImagesDrawerFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                    if (deleteFile.isDirectory()) {
                        File[] lf = deleteFile.listFiles();
                        if (!(lf.length == 0))
                            for (File f : lf) {
                                f.delete();
                            }
                    }
                    deleteFile.delete();
                    iFrag.setupData(iFrag.getCurrentDir());
                    break;
                case "Music":
                    MusicDrawerFragment muFrag = (MusicDrawerFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                    if (deleteFile.isDirectory()) {
                        File[] lf = deleteFile.listFiles();
                        if (!(lf.length == 0))
                            for (File f : lf) {
                                f.delete();
                            }
                    }
                    deleteFile.delete();
                    muFrag.setupData(muFrag.getCurrentDir());
                    break;
                case "Downloads":
                    DownloadDrawerFragment dFrag = (DownloadDrawerFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                    if (deleteFile.isDirectory()) {
                        File[] lf = deleteFile.listFiles();
                        if (!(lf.length == 0))
                            for (File f : lf) {
                                f.delete();
                            }
                    }
                    deleteFile.delete();
                    dFrag.setupData(dFrag.getCurrentDir());
                    break;
                case "Search":
                    SearchResultsFragment sFrag = (SearchResultsFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                    if (deleteFile.isDirectory()) {
                        File[] lf = deleteFile.listFiles();
                        if (!(lf.length == 0))
                            for (File f : lf) {
                                f.delete();
                            }
                    }
                    deleteFile.delete();
                    sFrag.deleteItemFromArrayResults(onLongPressPaths);
                    break;

            }
            if (deleteFile.exists()) {
                View parentLayout = findViewById(R.id.rootView);
                Snackbar snackbar = Snackbar.make(parentLayout, R.string.no_authorization, Snackbar.LENGTH_LONG);
                snackbar.show();
            } else {
                View parentLayout = findViewById(R.id.rootView);
                Snackbar snackbar = Snackbar.make(parentLayout, R.string.file_deleted, Snackbar.LENGTH_LONG);
                snackbar.show();
            }

        }

    }

    @Override
    public void onRenameClicked() {
        FragmentManager fm = getSupportFragmentManager();
        DialogNewFile dialog = DialogNewFile.newIstance("Rename");
        dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
        dialog.show(fm, "fragment_newfile");
        findViewById(R.id.fabMain).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCopyClicked() {
        copyClicked = true;
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction().replace(R.id.fragmentContainer, CopyMoveFragment.newIstance(), TAG_FRAGMENT);
        ft.commit();
    }

    @Override
    public void onMoveClicked() {
        copyClicked = false;
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction().replace(R.id.fragmentContainer, CopyMoveFragment.newIstance(), TAG_FRAGMENT);
        ft.commit();
    }

    @Override
    public void onFolderSelected(String path) {
        FloatingActionMenu fabMain = (FloatingActionMenu) findViewById(R.id.fabMain);
        fabMain.setVisibility(View.VISIBLE);
        View parentLayout = findViewById(R.id.rootView);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft;
        /**
         * After the choice of the folder where the file/directory has to be copied,
         * I return to the previous fragment checking "currentFragment".
         */
        switch (currentFragment) {
            case "First":
                ft = fragmentManager.beginTransaction().replace(R.id.fragmentContainer, MainFragment.newIstance(), TAG_FRAGMENT);
                ft.commit();
                break;
            case "Search":
                ft = fragmentManager.beginTransaction().replace(R.id.fragmentContainer, MainFragment.newIstance(), TAG_FRAGMENT);
                ft.commit();
                break;
            case "Images":
                ft = fragmentManager.beginTransaction().replace(R.id.fragmentContainer, ImagesDrawerFragment.newIstance(), TAG_FRAGMENT);
                ft.commit();
                break;
            case "Music":
                ft = fragmentManager.beginTransaction().replace(R.id.fragmentContainer, MusicDrawerFragment.newIstance(), TAG_FRAGMENT);
                ft.commit();
                break;
            case "Downloads":
                ft = fragmentManager.beginTransaction().replace(R.id.fragmentContainer, DownloadDrawerFragment.newIstance(), TAG_FRAGMENT);
                ft.commit();
                break;
        }
        /**
         * User clicked copy button
         */
        if (copyClicked) {
            for (String p : onLongPressPaths) {
                if (new File(p).isFile()) {
                    try {
                        FileUtils.copyFileToDirectory(new File(p), new File(path));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        String dirName = new File(p).getName();
                        FileUtils.copyDirectory(new File(p), new File(path + "/" + dirName));
                    } catch (IOException e) {
                        e.printStackTrace();

                    }

                }
                Snackbar snackbar = Snackbar.make(parentLayout, R.string.file_copied, Snackbar.LENGTH_LONG);
                snackbar.show();
            }

        } else {
            /**
             * User clicked move button
             */
            for (String p : onLongPressPaths) {
                if (new File(p).isFile()) {
                    try {
                        FileUtils.moveFileToDirectory(new File(p), new File(path), true);
                        new File(p).delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        String dirName = new File(p).getName();
                        FileUtils.copyDirectory(new File(p), new File(path + "/" + dirName));
                        new File(p).delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                Snackbar snackbar = Snackbar.make(parentLayout, R.string.file_moved, Snackbar.LENGTH_SHORT);
                snackbar.show();
            }


        }
    }

    @Override
    public void onZipClicked() {

        try {
            ZipFile zipFile = new ZipFile(onLongPressPaths.get(0) + ".zip"); // How should I set the name? Now I use the name of the first file.
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
            parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
            for (String s : onLongPressPaths) {
                File f = new File(s);
                zipFile.addFile(f, parameters);
            }

        } catch (ZipException e) {
            e.printStackTrace();
        }
        switch (currentFragment) {
            case "First":
                MainFragment mFrag = (MainFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                mFrag.setupData(mFrag.getCurrentDir());
                break;
            case "Images":
                ImagesDrawerFragment iFrag = (ImagesDrawerFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                iFrag.setupData(iFrag.getCurrentDir());
                break;
            case "Music":
                MusicDrawerFragment muFrag = (MusicDrawerFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                muFrag.setupData(muFrag.getCurrentDir());
                break;
            case "Downloads":
                DownloadDrawerFragment dFrag = (DownloadDrawerFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                dFrag.setupData(dFrag.getCurrentDir());
                break;
            /**
             * Should also manage "Search" case.
             */

        }
        View parentLayout = findViewById(R.id.rootView);
        Snackbar snackbar = Snackbar.make(parentLayout, R.string.file_zipped, Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    @Override
    public void onUnZipClicked() {
        File f = new File(onLongPressPaths.get(0));
        if (f.getName().substring(f.getName().lastIndexOf('.') + 1).equals("zip")) {
            int n = onLongPressPaths.get(0).length();
            int x = f.getName().length();
            String destPath = onLongPressPaths.get(0).substring(0, (n - x));
            try {
                ZipFile zipFile = new ZipFile(onLongPressPaths.get(0));
                zipFile.extractAll(destPath);
            } catch (ZipException e) {
                e.printStackTrace();
            }
            switch (currentFragment) {
                case "First":
                    MainFragment mFrag = (MainFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                    mFrag.setupData(mFrag.getCurrentDir());
                    break;
                case "Images":
                    ImagesDrawerFragment iFrag = (ImagesDrawerFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                    iFrag.setupData(iFrag.getCurrentDir());
                    break;
                case "Music":
                    MusicDrawerFragment muFrag = (MusicDrawerFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                    muFrag.setupData(muFrag.getCurrentDir());
                    break;
                case "Downloads":
                    DownloadDrawerFragment dFrag = (DownloadDrawerFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                    dFrag.setupData(dFrag.getCurrentDir());
                    break;
                /**
                 * Should also manage "Search" case.
                 */

            }
            View parentLayout = findViewById(R.id.rootView);
            Snackbar snackbar = Snackbar.make(parentLayout, R.string.file_unzipped, Snackbar.LENGTH_SHORT);
            snackbar.show();
        } else {
            View parentLayout = findViewById(R.id.rootView);
            Snackbar snackbar = Snackbar.make(parentLayout, R.string.error_unzip, Snackbar.LENGTH_SHORT);
            snackbar.show();
        }

    }

    /**
     * Setting the file path of the item which has been long pressed
     *
     * @param paths
     */
    public void setOnLongPressPaths(List<String> paths) {

        this.onLongPressPaths = paths;
    }

    public void onNameSearchTyped(String nameTyped) {
        onSearchItem = true;
        currentFragment = "Search";
        setSearchName(nameTyped);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction().replace(R.id.fragmentContainer, SearchResultsFragment.newIstance(), TAG_FRAGMENT);
        ft.commit();


    }

    public String getSearchName() {
        return searchName;
    }

    public void setSearchName(String searchName) {
        this.searchName = searchName;
    }

    public void setFalseOnSearchItem() {
        onSearchItem = false;
    }

    public boolean getOnSearchItem() {
        return onSearchItem;
    }

    public void setFileOrDirToNull() {
        fileOrDir = null;
    }

    public String getCurrentFragment() {
        return currentFragment;
    }

    public void setCurrentFragment(String currentFragment) {
        this.currentFragment = currentFragment;
    }

    public void launchGithubPage(View v) {
        Intent webPageIntent = new Intent(Intent.ACTION_VIEW);
        webPageIntent.setData(Uri.parse("https://github.com/CosimoSguanci"));
        startActivity(webPageIntent);
    }


}

