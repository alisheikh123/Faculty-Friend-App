package com.litesapp.ftp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.view.View;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import com.google.android.material.navigation.NavigationView;
import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.litesapp.ftp.FTPClientMain.MainActivityClient;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    Context context;
    private static final int MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 0;
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 0;
    WifiApManager wifiap;

    String CHANNEL_ID = "my_channel_01";// The id of the channel.
    CharSequence name = "name";// The user-visible name of the channel.
    int importance = NotificationManager.IMPORTANCE_HIGH;
    NotificationChannel mChannel;

    NotificationCompat.Builder mBuilder;
    NotificationManager mNotificationManager;
    ToggleButton toggleFtpServer;
    TextView txtStatus, txtInfo, txtUrl;
    FtpServer server;
    FtpServerFactory serverFactory;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        ///Start Button
        toggleFtpServer = findViewById(R.id.toggleFtpServer);
        toggleFtpServer.setChecked(false);

        //IP Address Status
        txtStatus = findViewById(R.id.txtStatus);

        //IP Address Url
        txtUrl = findViewById(R.id.txtUrl);


        txtUrl.setVisibility(View.INVISIBLE);

        txtInfo = findViewById(R.id.txtInfo);
        txtInfo.setVisibility(View.INVISIBLE);


        wifiap = new WifiApManager(getApplicationContext());

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                resultIntent, 0);


        mBuilder = new NotificationCompat.Builder(this, "1");
        mBuilder.setOngoing(true);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setContentTitle("FTP Tools Service");
        mBuilder.setChannelId(CHANNEL_ID);
        mBuilder.setContentIntent(pendingIntent);
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mNotificationManager.createNotificationChannel(mChannel);
        }

        serverFactory = new FtpServerFactory();
        server = serverFactory.createServer();

        if (checkpermissions()) {
            initFtp();
            checkThread controller = new checkThread();
            controller.start();
        } else {
            requestPermission();
        }


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle("FTP Tools");
            builder.setMessage("Do you want to exit? ");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    MainActivity.this.finish();
                    server.suspend();

                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            builder.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_rate) {
          try  {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + this.getPackageName())));
            } catch (android.content.ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_openRemote){
            startActivity(new Intent(MainActivity.this,MainActivityClient.class));

        }

        if (id == R.id.nav_tutorial) {
            Intent intent=new Intent(this, TutorialActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_privacy) {
            Uri uri = Uri.parse(getString(R.string.policy_link));
            startActivity( new Intent( Intent.ACTION_VIEW, uri ) );

        } else if (id == R.id.nav_share) {
            {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBodyText = "FTP Tools - Wireless File Transfers\n https://play.google.com/store/apps/details?id=com.litesapp.ftp";
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Download FTP App");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBodyText);
                startActivity(Intent.createChooser(sharingIntent, "Sharing Option"));
                return true;
            }

        } else if (id == R.id.nav_send) {
            Intent mailIntent = new Intent(Intent.ACTION_VIEW);
            Uri data = Uri.parse("mailto:?subject=" + "FTP Tools"+ "&body=" + "Hi FTP Tools App Support: " + "&to=" + "litesapp@gmail.com");
            mailIntent.setData(data);
            startActivity(Intent.createChooser(mailIntent, "Send mail..."));

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }








    public int getPort() {
        return 2121;
    }

    //check permission
    public boolean checkpermissions() {
        int permissionCheckWrite = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE);
        
        int permissionCheckRead = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE);

        return permissionCheckWrite == PackageManager.PERMISSION_GRANTED && permissionCheckRead == PackageManager.PERMISSION_GRANTED;
    }

    //request permission
    public void requestPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED

        ) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);

                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);

            } else {
                ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
                ActivityCompat.requestPermissions(this,
                        new String[]{READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_READ_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        int p = 0;

        if (requestCode == MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted.
                p++;

            }
        }
        if (requestCode == MY_PERMISSIONS_READ_EXTERNAL_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted.
                p++;
            }
            if (p == 2) {
                initFtp();
            }
        }
    }

    //ftp
    public void initFtp() {
        ListenerFactory factory = new ListenerFactory();
        //Port seting
        factory.setPort(getPort());
        serverFactory.addListener("default", factory.createListener());


        //Utente wapp
        ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
        connectionConfigFactory.setAnonymousLoginEnabled(true);


        serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());


        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());



        if (folderUsable(getFileLocation())) {
            BaseUser user = new BaseUser();
            user.setName("anonymous");
            user.setHomeDirectory(getFileLocation());

            user.setAuthorities(authorities);
            try {
                serverFactory.getUserManager().save(user);
            } catch (FtpException e) {
                e.printStackTrace();
            }
        }

        try {
            server.start();
            server.suspend();
        } catch (FtpException e) {
            Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            txtStatus.setText(getString(R.string.error_on_start_server));
            txtStatus.setTextColor(getColor(R.color.red));
        }
    }
    //on request permission

    public String getFileLocation() {
        String res;
        File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        if (root.exists()) {
            res = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            res = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return res;
    }

    public boolean folderUsable(String f) {
        File folder = new File(f);
        return folder.isDirectory() && folder.canRead() && folder.canWrite();
    }

    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    public String getIp() {
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = manager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        String ip = null;
        try {
            ip = InetAddress.getByAddress(
                    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array())
                    .getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return ip;
    }

    private boolean checkWifiOnAndConnected() {
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON

            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            return wifiInfo.getNetworkId() == -1;
        } else {
            return true; // Wi-Fi adapter is OFF
        }
    }

    public void onToggleServerClick(View v) {
        if (!checkpermissions()) {
            txtStatus.setText(getString(R.string.Read_Write_on_external_devices_enided));
            txtStatus.setTextColor(getColor(R.color.red));
            toggleFtpServer.setChecked(false);
            return;
        }
        if (checkWifiOnAndConnected() & wifiap.isWifiApEnabled()) {
            txtStatus.setText(getString(R.string.Turn_On_Wifi_or_Hotspot));
            txtStatus.setTextColor(getColor(R.color.red));
            txtUrl.setText("");
            toggleFtpServer.setChecked(false);
            txtInfo.setVisibility(View.INVISIBLE);
            mNotificationManager.cancel(1);
            return;
        }


        if (server.isStopped()) {
            txtStatus.setText(getString(R.string.error_on_start_server));
            txtStatus.setTextColor(getColor(R.color.red));

            return;
        }
        if (server.isSuspended()) {
            server.resume();
            if (!server.isSuspended()) {

                txtStatus.setText(getString(R.string.Server_started));
                txtStatus.setTextColor(ContextCompat.getColor(this,R.color.green));
                if (!checkWifiOnAndConnected()) {
                    mBuilder.setContentText("Server is running  " + getString(R.string.ftp) + getIp() + getString(R.string.colon) + getPort());
                    txtUrl.setText(getString(R.string.ftp) + getIp() + getString(R.string.colon) + getPort());
                    txtUrl.setVisibility(View.VISIBLE);
                    txtInfo.setText(getString(R.string.dtext));
                    txtInfo.setVisibility(View.VISIBLE);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    mBuilder.setContentText("Server is running  " + getString(R.string.ftp) + "192.168.43.1" + getString(R.string.colon) + getPort());
                    txtUrl.setText(getString(R.string.ftp) + "192.168.43.1" + getString(R.string.colon) + getPort());
                    txtUrl.setVisibility(View.VISIBLE);
                    txtInfo.setText(getString(R.string.btext));
                    txtInfo.setVisibility(View.VISIBLE);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                mNotificationManager.notify(1, mBuilder.build());

            }
        } else {
            server.suspend();
            if (server.isSuspended()) {
                txtStatus.setText(getString(R.string.Server_stopped));
                txtStatus.setTextColor(getColor(R.color.yellow));
            }
            mNotificationManager.cancel(1);

            txtUrl.setText("");
            txtUrl.setVisibility(View.INVISIBLE);
            txtInfo.setVisibility(View.INVISIBLE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNotificationManager.cancel(1);

    }


    public enum WIFI_AP_STATE {
        WIFI_AP_STATE_DISABLING,
        WIFI_AP_STATE_DISABLED,
        WIFI_AP_STATE_ENABLING,
        WIFI_AP_STATE_ENABLED,
        WIFI_AP_STATE_FAILED
    }

//

    public class checkThread extends Thread {

        public void run() {

            for (; ; ) {
                if (checkWifiOnAndConnected()) {
                    server.suspend();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            toggleFtpServer.setChecked(false);
                            txtUrl.setVisibility(View.INVISIBLE);
                            txtInfo.setVisibility(View.INVISIBLE);
                            ;
                        }
                    });
                    return;
                }
                if (wifiap.isWifiApEnabled()) {
                    server.suspend();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            toggleFtpServer.setChecked(false);
                            txtUrl.setVisibility(View.INVISIBLE);
                            txtInfo.setVisibility(View.INVISIBLE);
                        }
                    });
                    return;
                }

                try {
                    sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }





    public class WifiApManager {

        private final WifiManager mWifiManager;

        public WifiApManager(Context context) {
            mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        }

        /*the following method is for getting the wifi hotspot state*/

        public WIFI_AP_STATE getWifiApState() {
            try {
                Method method = mWifiManager.getClass().getMethod("getWifiApState");

                int tmp = ((Integer) method.invoke(mWifiManager));

                // Fix for Android 4
                if (tmp > 10) {
                    tmp = tmp - 10;
                }

                return WIFI_AP_STATE.class.getEnumConstants()[tmp];
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                return WIFI_AP_STATE.WIFI_AP_STATE_FAILED;
            }
        }

        boolean isWifiApEnabled() {
            return getWifiApState() != WIFI_AP_STATE.WIFI_AP_STATE_ENABLED;
        }

    }





}
