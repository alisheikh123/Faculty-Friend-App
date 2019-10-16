package com.litesapp.ftp.FTPFilesExplorer.FTPRemoteExplorer;


import android.content.Context;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;


import com.litesapp.ftp.FTPFilesExplorer.FilesAdapter;
import com.litesapp.ftp.FTPFilesExplorer.FilesFragment;
import com.litesapp.ftp.R;

import org.apache.commons.net.ftp.FTPFile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Objects;



/**
 * Created by Geri on 24/10/2015.
 */
public class RemoteFilesAdapter  extends FilesAdapter<FTPFile> {

    private static final String TAG = "REMOTE_FILES_ADAPTER";

    public Context context;

    public RemoteFilesAdapter(FilesFragment fragment, Context context) {
        super(fragment);

        this.context = context;
    }

    @Override
    protected void sort(int mode) {
        ArrayList<FTPFile> sorted = new ArrayList<>(dataset);
        Collections.sort(sorted, new FTPFileComparator(mode));
        animateTo(sorted);
    }

    private class FTPFileComparator extends FileComparator{

        public FTPFileComparator(int mode){
            super(mode);
        }

        @Override
        public int compare(FTPFile lhs, FTPFile rhs) {
            if(lhs.isDirectory() && rhs.isFile()){
                return -1;
            }
            else if (lhs.isFile() && rhs.isDirectory())
                return 1;
            else {
                int res = 0;
                switch (super.mode) {
                    case BY_NAME:
                        res = sortName(lhs, rhs);
                        if(res != 0) return res;
                        res = sortType(lhs, rhs);
                        if(res != 0) return res;
                        res = sortTime(lhs, rhs);
                        if(res != 0) return res;
                        return sortSize(lhs, rhs);
                    case BY_SIZE:
                        res = sortSize(lhs, rhs);
                        if(res != 0) return res;
                        res  = sortName(lhs, rhs);
                        if(res != 0) return res;
                        res = sortType(lhs, rhs);
                        if(res != 0) return res;
                        return sortTime(lhs, rhs);
                    case BY_TYPE:
                        res = sortType(lhs, rhs);
                        if(res != 0) return res;
                        res  = sortName(lhs, rhs);
                        if(res != 0) return res;
                        res = sortTime(lhs, rhs);
                        if(res != 0) return res;
                        return sortSize(lhs, rhs);
                    case BY_TIME:
                        res = sortTime(lhs, rhs);
                        if(res != 0) return res;
                        res = sortName(lhs, rhs);
                        if(res != 0) return res;
                        res = sortType(lhs, rhs);
                        if(res != 0) return res;
                        return sortSize(lhs, rhs);
                    default:
                        return 0;
                }
            }
        }

        private int sortName(FTPFile lhs, FTPFile rhs){
            String name1 = getName(lhs.getName());
            String name2 = getName(rhs.getName());
            return name1.compareTo(name2);
        }

        private int sortSize(FTPFile lhs, FTPFile rhs){
            long  res = lhs.getSize() - rhs.getSize();
            if(res > 0) return 1;
            else if (res < 0) return -1;
            return 0;
        }

        private int sortTime(FTPFile lhs, FTPFile rhs){
            long res = lhs.getTimestamp().getTimeInMillis() - rhs.getTimestamp().getTimeInMillis();
            if(res > 0) return 1;
            else if (res < 0) return -1;
            return 0;
        }

        private int sortType(FTPFile lhs, FTPFile rhs){
            String type1 = getExt(lhs.getName());
            String type2 = getExt(rhs.getName());
            return type1.compareTo(type2);
        }
    }


    @Override
    public ArrayList<Integer> getSelectedIndices() {
        ArrayList<Integer> items =
                new ArrayList<Integer>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    @Override
    public ArrayList<FTPFile> getSelectedItems() {
        ArrayList<FTPFile> items =
                new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(dataset.get(selectedItems.keyAt(i)));
        }
        return items;
    }

    @Override
    public ArrayList<String> getSelectedNames() {
        ArrayList<String> names =
                new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            names.add(dataset.get(selectedItems.keyAt(i)).getName());
        }
        return names;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        FTPFile ftpFile = dataset.get(position);
        holder.getNameTextView().setText(ftpFile.getName());
        String size = convertToStringRepresentation(ftpFile.getSize());
        Calendar ctime = ftpFile.getTimestamp();
        SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy hh:mm a", java.util.Locale.getDefault());
        String time = formatter.format(ctime.getTime());
        if(ftpFile.isFile())
            holder.getInfoTextView().setText(size + " - " + time);
        else
            holder.getInfoTextView().setText(time);
        fileOnClickListener listener = new fileOnClickListener(fragment, position);
        holder.itemView.setOnClickListener(listener);
        holder.itemView.setOnLongClickListener(listener);
        if(ftpFile.isFile()){
                    Log.d(TAG,"File Type is :"+ftpFile.getType());
            holder.getImageView().setImageResource(R.mipmap.ic_file);
        }
        else
            holder.getImageView().setImageResource(R.mipmap.ic_folder);
        boolean activate = (isSelected(position) || isCut(ftpFile.getName()));
        holder.itemView.setActivated(activate);
    }

    public boolean isDirectory(FTPFile dir){
        return dir.isDirectory();
    }

    protected class fileOnClickListener implements View.OnClickListener, View.OnLongClickListener {
       public FilesFragment fragment;
        FTPFile file;
        int pos;

        public fileOnClickListener(FilesFragment fragment, int pos) {
            this.pos = pos;
            this.file = dataset.get(pos);
            this.fragment = fragment;
        }

        @Override
        public boolean onLongClick(View v) {
            Log.d(TAG, (file.isDirectory() ? "Directory ": "File ") + file.getName() + " long clicked.");
            if(!fragment.isPasteMode()) {
                if (isSelecting() && file.isDirectory()) {
                    final View view = v;
                    PopupMenu menu = new PopupMenu(fragment.getActivity(), v);
                    menu.getMenuInflater().inflate(R.menu.folder_popup_menu, menu.getMenu());
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.folderPopupOpen:
                                    fragment.openDirecory(file.getName());
                                    return true;
                                case R.id.folderPopupSelect:
                                    fragment.selectItem(view);
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                    menu.show();
                } else {
                    fragment.selectItem(v);
                }
            }
            /* old
            if (isDirectory(file)) {
                Log.d(TAG, "Directory " + file.getName() + " long clicked.");
                DownloadDialog dialog = DownloadDialog.newInstance(file);
                dialog.setTargetFragment(fragment, 1);
                FragmentManager fm = fragment.getActivity().getSupportFragmentManager();
                dialog.show(fm, "Download");
            }
            else {
                Log.d(TAG, "File " + file.getName() + " long clicked.");
                fragment.selectItem(v);
            }
            */
            return true;
        }

        @Override
        public void onClick(View v) {
            if(isSelecting() && !fragment.isPasteMode()) {
                fragment.selectItem(v);
            }
            else {
                if (isDirectory(file)) {
                    if(!isCut(file.getName())) {
                        Log.d(TAG, "Directory " + file.getName() + " clicked.");
                        fragment.openDirecory(file.getName());
                    }
                } else {
                    if(!fragment.isPasteMode()) {
                        Toast.makeText(context, "Long CLick to download", Toast.LENGTH_SHORT).show();
//                        try {
//                            Log.d(TAG, "File " + file.getName() + " clicked.");
//                            DownloadDialog dialog = DownloadDialog.newInstance(file);
//                            dialog.setTargetFragment(fragment, 1);
//                            FragmentManager fm = Objects.requireNonNull(fragment.getActivity()).getSupportFragmentManager();
//                            dialog.show(fm, "Download");
//                        } catch (Exception e){
//                            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
//                        }

                    }
                }
            }
        }
    }
}
