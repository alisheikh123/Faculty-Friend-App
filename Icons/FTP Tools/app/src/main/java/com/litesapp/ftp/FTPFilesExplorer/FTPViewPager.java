package com.litesapp.ftp.FTPFilesExplorer;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.litesapp.ftp.FTPClientMain.FTPConnection;
import com.litesapp.ftp.FTPClientMain.MainActivityClient;
import com.litesapp.ftp.R;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.util.Objects;


import de.greenrobot.event.EventBus;


public class FTPViewPager extends Fragment {
    private static final String CONNECT = "connect";
    private static final String TAG = "FTP_PAGER";

    private FTPConnection connection;
    private FTPPagerAdapter adapter;
    private FTPClient client;
    private View v;

    private EventBus bus = EventBus.getDefault();

    private Menu mMenu;

    public static FTPViewPager newInstance(FTPConnection connection) {
        FTPViewPager fragment = new FTPViewPager();
        Bundle args = new Bundle();
        args.putSerializable(CONNECT, connection);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            this.connection = (FTPConnection)getArguments().getSerializable(CONNECT);
            //do something when on start
            if(connection == null){

            }
            else{
                if(client==null)
                    client = new FTPClient();
                new FTPConnectTask().execute(connection);
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_ftpview_pager, container, false);

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MainActivityClient activity =(MainActivityClient)getActivity();
        switch (item.getItemId()) {
            case android.R.id.home:
                FragmentManager fm = getActivity().getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class FTPConnectTask extends AsyncTask<FTPConnection, Void,FTPClient> {

        private final String TAG = "CONNECT_TASK";

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected void onPostExecute(FTPClient c) {
            super.onPostExecute(c);
            if (c != null) {
                if(c.isConnected()) {
                    ViewPager pager = (ViewPager) FTPViewPager.this.v.findViewById(R.id.ftpViewPager);
                    pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                        final MainActivityClient activity = ((MainActivityClient) getActivity());
                        FloatingActionButton fab = activity.fab;





                        Animation show = AnimationUtils.loadAnimation(getContext(), R.anim.fab_show);
                        Animation hide = AnimationUtils.loadAnimation(getContext(), R.anim.fab_hide);
                        boolean shown = true;

                        @Override
                        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                            //Log.d(TAG, "scroll position: " + position + ", offset: " + positionOffset);
                            if(!shown && positionOffset == 0) {
                                Log.d(TAG, "showing fab");
                                shown = true;
                                fab.startAnimation(show);

                                fab.show();

                                // TODO: HERE I comment the fab events


                            }
                            else if (shown && positionOffset!= 0){
                                shown = false;
                                Log.d(TAG, "hiding fab");
                                fab.startAnimation(hide);
                                fab.hide();
                            }
                        }

                        @Override
                        public void onPageSelected(int position) {
                            if (position == 0) {
                                activity.setTitle("Remote");
                                Log.d(FTPViewPager.TAG, "remote alive 1");
                                activity.isRemoteAlive = true;
                                activity.isLocalAlive = false;
                            } else {
                                activity.setTitle("Local");
                                Log.d(FTPViewPager.TAG, "local alive 1");
                                activity.isRemoteAlive = false;
                                activity.isLocalAlive = true;
                                if(activity.requestStoragePermission(getString(R.string.cant_show_files)))
                                    activity.getActiveFragment().refreshDir();
                            }
                            activity.getActiveFragment().ensurePathIsSowhn();
                            //Log.d(TAG, "selected");
                            fab.show();
                        }

                        @Override
                        public void onPageScrollStateChanged(int state) {
                        }
                    });
                    pager.setVisibility(View.VISIBLE);
                    ProgressBar progress = (ProgressBar) FTPViewPager.this.v.findViewById(R.id.loadConnectionProgressBar);
                    progress.setVisibility(View.GONE);
                    adapter = new FTPPagerAdapter(c, connection, (MainActivityClient) getActivity(), getChildFragmentManager());
                    pager.setAdapter(adapter);
                    FloatingActionButton fab = ((MainActivityClient) getActivity()).fab;
                    fab.setOnClickListener(((MainActivityClient)getActivity()));
                } else {
                    Toast t = ((MainActivityClient) Objects.requireNonNull(getActivity())).commonToast;
                    t.setText("Authentication failed...");
                    t.show();
                    Objects.requireNonNull(getActivity()).onBackPressed();
                }
            } else if (c == null) {
                Toast t = ((MainActivityClient) Objects.requireNonNull(getActivity())).commonToast;
                t.setText("Failed to connect...");
                t.show();
                Objects.requireNonNull(getActivity()).onBackPressed();
            }
        }

        @Override
        protected FTPClient doInBackground(FTPConnection... params) {
            FTPConnection connection = params[0];
            boolean suc = false;
            try {
                FTPClient client = new FTPClient();
                client.connect(connection.getHost(), connection.getPort());

                if (FTPReply.isPositiveCompletion(client.getReplyCode())) {
                    Log.d(TAG, "connected!");
                    boolean status = client.login(connection.getUser(), connection.getPass());
                    client.setFileType(FTP.BINARY_FILE_TYPE);
                    client.enterLocalPassiveMode();
                    suc = status;
                }
                if (suc) {
                    Log.d(TAG, "connected and authenticated!");
                    return client;
                } else {
                    Log.d(TAG, "failed to authenticate!");
                    client.disconnect();
                    return client;
                }

            } catch (Exception e) {
                Log.e(TAG, e.toString());
                Log.d(TAG, "Error: could not connect to host " + connection.getHost());
            }

            return null;
        }

    }

}
