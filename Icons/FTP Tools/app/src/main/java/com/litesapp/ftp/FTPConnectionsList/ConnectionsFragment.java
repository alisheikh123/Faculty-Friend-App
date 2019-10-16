package com.litesapp.ftp.FTPConnectionsList;

//import android.app.ListFragment;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.litesapp.ftp.FTPClientMain.FTPConnection;
import com.litesapp.ftp.FTPClientMain.MainActivityClient;
import com.litesapp.ftp.MainActivity;
import com.litesapp.ftp.R;


/**
 * Created by Geri on 12/10/2015.
 */
public class ConnectionsFragment extends ListFragment {
    private SimpleCursorAdapter adapter;
    private FTPConnectionsDBHelper dbHelper;
    public static final String TAG = "CONNECTION_FRAGMENT";

    public ConnectionsFragment(){}

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        dbHelper = new FTPConnectionsDBHelper(getActivity());
        dbHelper.open();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_connections, container, false);

    }

    @Override
    public void onResume() {
        super.onResume();
        final MainActivityClient activity = ((MainActivityClient) getActivity());
        FloatingActionButton fab = activity.fab;
        fab.show();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.startEditConnection(null);
            }
        });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);


        TextView empty=(TextView)view.findViewById(R.id.connections_empty);
        ListView list = getListView();
        View.OnClickListener listener =  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getListView().getAdapter().isEmpty())
                    ((MainActivityClient)getActivity()).startEditConnection(null);
            }
        };
        view.setOnClickListener(listener);
        empty.setOnClickListener(listener);
        list.setEmptyView(empty);

        Cursor cursor = dbHelper.fetchAllData();

        String[] columns = new String[] {
                FTPConnectionsDBHelper.KEY_NAME,
                FTPConnectionsDBHelper.KEY_USER,
                FTPConnectionsDBHelper.KEY_HOST
        };

        int[] to = new int[] {
                R.id.connectionNameTextView,
                R.id.connectionUserTextView,
                R.id.connectionHostTextView
        };


        adapter = new ConnectionCursorAdapter(dbHelper, list, getActivity(), R.layout.ftpconnection_item, cursor, columns, to,
                0);

        list.setAdapter(adapter);


        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> listView, View view,
                                    final int position, long id) {

                                Cursor cursor = (Cursor) listView.getItemAtPosition(position);
                                int cid = cursor.getInt(cursor.getColumnIndexOrThrow(FTPConnectionsDBHelper.KEY_ROWID));
                                String name = cursor.getString(cursor.getColumnIndexOrThrow(FTPConnectionsDBHelper.KEY_NAME));
                                String host = cursor.getString(cursor.getColumnIndexOrThrow(FTPConnectionsDBHelper.KEY_HOST));
                                String user = cursor.getString(cursor.getColumnIndexOrThrow(FTPConnectionsDBHelper.KEY_USER));
                                String pass = cursor.getString(cursor.getColumnIndexOrThrow(FTPConnectionsDBHelper.KEY_PASS));
                                int port = cursor.getInt(cursor.getColumnIndexOrThrow(FTPConnectionsDBHelper.KEY_PORT));
                                String protocol = cursor.getString(cursor.getColumnIndexOrThrow(FTPConnectionsDBHelper.KEY_PROTOCOL));

                                FTPConnection connection = new FTPConnection(cid, name, host, user, pass, port, protocol);

                                Toast t = ((MainActivityClient)getActivity()).commonToast;
                                t.setText("Connecting...");
                                t.show();
                                //connection
                                //Toast.makeText(getActivity(), "Connecting...", Toast.LENGTH_SHORT).show();
                                ((MainActivityClient)getActivity()).connectTo(connection);





            }
        });
    }

    public void editDatabase(FTPConnection old, FTPConnection edited){
        if(old == null)
            dbHelper.insertFTPConnection(edited);
        else {
            ContentValues cv = new ContentValues();
            cv.put(FTPConnectionsDBHelper.KEY_NAME, edited.getName());
            cv.put(FTPConnectionsDBHelper.KEY_HOST, edited.getHost());
            cv.put(FTPConnectionsDBHelper.KEY_USER, edited.getUser());
            cv.put(FTPConnectionsDBHelper.KEY_PASS, edited.getPass());
            cv.put(FTPConnectionsDBHelper.KEY_PORT, edited.getPort());
            cv.put(FTPConnectionsDBHelper.KEY_PROTOCOL, edited.getStringProtocol());

            dbHelper.updateFTPConnection(old.getId(), cv);
        }
        adapter.changeCursor(dbHelper.fetchAllData());
        adapter.notifyDataSetChanged();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}
