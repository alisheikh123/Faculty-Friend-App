package com.litesapp.ftp.FTPClientMain;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;


import com.litesapp.ftp.FTPConnectionsList.ConnectionsFragment;
import com.litesapp.ftp.FTPConnectionsList.EditConnectionFragment;
import com.litesapp.ftp.FTPFilesExplorer.FTPLocalExplorer.LocalFilesFragment;
import com.litesapp.ftp.FTPFilesExplorer.FTPRemoteExplorer.RemoteFilesFragment;
import com.litesapp.ftp.FTPFilesExplorer.FTPViewPager;
import com.litesapp.ftp.FTPFilesExplorer.FilesFragment;
import com.litesapp.ftp.R;


import java.io.Serializable;






public class MainActivityClient extends AppCompatActivity implements View.OnClickListener{

    private final String TAG = "MAINACTIVITY";

    private final int MY_EXTERNAL_STORAGE = 401;

    public boolean isRemoteAlive = false;
    public boolean isLocalAlive = false;
    public RemoteFilesFragment remote;
    public LocalFilesFragment local;

    public Toast commonToast;

    private String savedTitle = null;

    public FloatingActionButton fab;

    private ConnectionsFragment cf;
    private String errorMessage;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main_client);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        Window window = this.getWindow();

// clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

// add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

// finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this,R.color.colorPrimaryDarker));




        commonToast = Toast.makeText(MainActivityClient.this, "", Toast.LENGTH_SHORT);

        Log.d(TAG, "before replace");
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                int stackHeight = getSupportFragmentManager().getBackStackEntryCount();
                if (stackHeight > 0) { // if we have something on the stack (doesn't include the current shown fragment)
                    getSupportActionBar().setHomeButtonEnabled(true);
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                } else {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    getSupportActionBar().setHomeButtonEnabled(false);
                }
            }

        });


        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        cf = new ConnectionsFragment();
        ft.replace(R.id.main_placeholder, cf, "CONNECTIONS_FRAGMENT");
        ft.commit();
        Log.d(TAG, "after replace");
        fab = (FloatingActionButton) findViewById(R.id.connections_fab);





    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean requestStoragePermission(String errorMessage) {
        int sdk = Build.VERSION.SDK_INT;
        if(sdk >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, MY_EXTERNAL_STORAGE);
                this.errorMessage = errorMessage;
                return false;
            }
            else
                return true;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        MenuItem settings = menu.findItem(R.id.action_settings);

        settings.setVisible(false);

        MenuItem paste = menu.findItem(R.id.action_paste_file);
        if(getActiveFragment()!=null && paste != null ){
            Log.d(TAG, "paste menu item found");
            if(getActiveFragment().isPasteMode()) {
                Log.d(TAG, "in paste mode");
                savedTitle = getTitle().toString();
                String state = (getActiveFragment().isCopy() ? "Copy" : "Cut");
                setTitle(state + ": "+getActiveFragment().filesAdapter.getCutItemCount()+ " File(s).");

            }
            else if (savedTitle != null)
                setTitle(savedTitle);
            MenuItem home = menu.findItem(android.R.id.home);
            int icon = ((getActiveFragment() != null && getActiveFragment().isPasteMode()) ? R.drawable.ic_cancel : R.drawable.ic_back);

            getSupportActionBar().setHomeAsUpIndicator(icon);
            paste.setVisible(getActiveFragment().isPasteMode());
            return true;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    public void setRemoteFragment(RemoteFilesFragment frag){
        this.remote = frag;
    }

    public void setLocalFragment(LocalFilesFragment frag){
        this.local = frag;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_paste_file:
                getActiveFragment().pasteFiles();
                invalidateOptionsMenu();
                return true;
            case android.R.id.home:
                Log.d(TAG, "clicked home");
                    if(getActiveFragment()!=null) {
                        if(getActiveFragment().isPasteMode()){
                            getActiveFragment().pasteMode(false);
                            return true;
                        }
                    } else {
                        return true;
                    }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        boolean back = false;
        if(remote != null && isRemoteAlive && !isLocalAlive){
            Log.d(TAG, "back pressed on remote");
            back = remote.pressBack();
        }
        else if(local != null && !isRemoteAlive && isLocalAlive){
            back = local.pressBack();
        }
        else{
            super.onBackPressed();
            /*Log.d(TAG, remote == null ? "remote is null in main" : "remote is not null in main");
            Log.d(TAG, isRemoteAlive ? "remote is alive in main" : "remote is not alive in main");
            Log.d(TAG, isLocalAlive ? "local is alive in main" : "local is not alive in main");*/
        }
        if (back) {
            super.onBackPressed();
        }

    }


    protected void onResume(){
        super.onResume();
        /*
        FTPConnection test = new FTPConnection();
        view= ((ListFragment)getFragmentManager().findFragmentById(R.id.fragment)).getListView();
        adapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_list_item_activated_1);
        view.setAdapter(adapter);
        if(client == null)
            client = new FTPClient();
        new FTPConnectTask().execute(test);
        */
    }
  //  FTPConnection connection
    public void  connectTo(FTPConnection connection){



            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            FTPViewPager pager = FTPViewPager.newInstance(connection);
            ft.replace(R.id.main_placeholder, pager);
            ft.addToBackStack("CONNECTION_PAGER");
            ft.commit();
            isRemoteAlive = true;
            isLocalAlive = false;




    }

    public void startEditConnection(FTPConnection connection){
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        EditConnectionFragment editCF = EditConnectionFragment.newInstance(connection);
        ft.replace(R.id.main_placeholder, editCF);
        ft.addToBackStack("EDIT_CONNECTION");
        ft.commit();

    }

    public void finishEditConnection(FTPConnection old, FTPConnection edited){
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        View v = getCurrentFocus();
        if(v != null)
        inputManager.hideSoftInputFromWindow(v.getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);


        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.main_placeholder, cf, "CONNECTIONS_FRAGMENT");
            ft.commit();
            cf.editDatabase(old, edited);
        }
    }

    public FilesFragment<? extends Serializable> getActiveFragment(){
        if(remote!=null && isRemoteAlive && !isLocalAlive)
            return remote;
        else
            return local;
    }

    @Override
    public void onClick(View v) {

        //TODO: here is click listener for handle fab

        if(isRemoteAlive && !isLocalAlive){
                showAlertForNewFolderRemote();
           // Toast.makeText(this, "remote!", Toast.LENGTH_SHORT).show();
        }
        else if(isLocalAlive && !isRemoteAlive){


           // Toast.makeText(this, "local folder created!", Toast.LENGTH_SHORT).show();

           showAlertForNewFolderLocal();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case MY_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(isRemoteAlive)
                        Toast.makeText(MainActivityClient.this, getString(R.string.try_again), Toast.LENGTH_SHORT).show();
                    if(local != null)
                        local.refreshDir();
                }
                else
                    Toast.makeText(MainActivityClient.this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }



    public void showAlertForNewFolderLocal(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        final EditText edittext = new EditText(this);
       // alert.setMessage("Enter Your Message");
        alert.setTitle("Enter Your Folder Name");

        alert.setView(edittext);

        alert.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                String folderName = edittext.getText().toString();

                local.createDirectory(folderName);

                Toast.makeText(MainActivityClient.this, "Folder created successfully", Toast.LENGTH_SHORT).show();

            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // what ever you want to do with No option.
            }
        });

        alert.show();
    }
    public void showAlertForNewFolderRemote(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        final EditText edittext = new EditText(this);
        // alert.setMessage("Enter Your Message");
        alert.setTitle("Enter Your Folder Name");

        alert.setView(edittext);

        alert.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                String folderName = edittext.getText().toString();

                remote.createDirectory(folderName);

                Toast.makeText(MainActivityClient.this, "Folder created successfully", Toast.LENGTH_SHORT).show();

            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // what ever you want to do with No option.
            }
        });

        alert.show();
    }
}
