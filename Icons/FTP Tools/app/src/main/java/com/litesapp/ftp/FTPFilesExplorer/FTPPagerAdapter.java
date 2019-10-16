package com.litesapp.ftp.FTPFilesExplorer;




import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.litesapp.ftp.FTPClientMain.FTPConnection;
import com.litesapp.ftp.FTPClientMain.MainActivityClient;
import com.litesapp.ftp.FTPFilesExplorer.FTPLocalExplorer.LocalFilesFragment;
import com.litesapp.ftp.FTPFilesExplorer.FTPRemoteExplorer.RemoteFilesFragment;

import org.apache.commons.net.ftp.FTPClient;



/**
 * Created by Geri on 19/10/2015.
 */
public class FTPPagerAdapter extends FragmentPagerAdapter {

    private FTPClient client;
    private MainActivityClient activity;
    private FTPConnection connection;

    @Override
    public Fragment getItem(int position) {
        switch(position){
            //remote
            case 0:
                RemoteFilesFragment remote = new RemoteFilesFragment();
                remote.setClient(client);
                remote.connection = connection;
                activity.setRemoteFragment(remote);
                return remote;
            //local
            case 1:
                LocalFilesFragment local = new LocalFilesFragment();
                local.connection = connection;
                activity.setLocalFragment(local);
                return local;
            default:
                return new Fragment();
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    public FTPPagerAdapter(FTPClient client, FTPConnection connection, MainActivityClient activity , FragmentManager mgr){
        super(mgr);
        this.client = client;
        this.connection = connection;
        this.activity = activity;
    }

}
