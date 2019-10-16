package com.litesapp.ftp.FTPFilesExplorer.FTPLocalExplorer;


import android.content.Context;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;


import com.litesapp.ftp.FTPFilesExplorer.FilesAdapter;
import com.litesapp.ftp.FTPFilesExplorer.FilesFragment;
import com.litesapp.ftp.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;




/**
 * Created by Geri on 25/10/2015.
 */
public class LocalFilesAdapter extends FilesAdapter<File> {

    private static final String TAG = "LOCAL_FILES_ADAPTER";

    public Context context;

    public LocalFilesAdapter(FilesFragment fragment, Context context) {
        super(fragment);
        this.context = context;
    }


    @Override
    public ArrayList<File> getSelectedItems() {
        ArrayList<File> items =
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
    protected void sort(int mode) {
        ArrayList<File> sorted = new ArrayList<>(dataset);
        Collections.sort(sorted, new LocalFileComparator(mode));
        animateTo(sorted);
    }

    private class LocalFileComparator extends FileComparator{

        public LocalFileComparator(int mode){
            super(mode);
        }

        @Override
        public int compare(File lhs, File rhs) {
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

        private int sortName(File lhs, File rhs){
            String name1 = getName(lhs.getName());
            String name2 = getName(rhs.getName());
            return name1.compareTo(name2);
        }

        private int sortSize(File lhs, File rhs){
            long  res = lhs.length() - rhs.length();
            if(res > 0) return 1;
            else if (res < 0) return -1;
            return 0;
        }

        private int sortTime(File lhs, File rhs){
            long res = lhs.lastModified() - rhs.lastModified();
            if(res > 0) return 1;
            else if (res < 0) return -1;
            return 0;
        }

        private int sortType(File lhs, File rhs){
            String type1 = getExt(lhs.getName());
            String type2 = getExt(rhs.getName());
            return type1.compareTo(type2);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.getNameTextView().setText(dataset.get(position).getName());

        String size = convertToStringRepresentation(dataset.get(position).length());
        SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy hh:mm a", java.util.Locale.getDefault());
        String time = formatter.format(dataset.get(position).lastModified());
        if (dataset.get(position).isDirectory())
            holder.getInfoTextView().setText(time);
        else
            holder.getInfoTextView().setText(size + " - " + time);
        fileOnClickListener listener = new fileOnClickListener(fragment, dataset.get(position));
        holder.itemView.setOnLongClickListener(listener);
        holder.itemView.setOnClickListener(listener);
        if(dataset.get(position).isFile()){
            String path = dataset.get(position).getAbsolutePath();
            String ext = path.substring(path.indexOf("."),path.length());
            Log.d(TAG, "onBindViewHolder: File Type: "+ext);
            holder.getImageView().setImageResource(R.mipmap.ic_file);
        }
        else if (dataset.get(position).isDirectory())
            holder.getImageView().setImageResource(R.mipmap.ic_folder);
        holder.itemView.setActivated(isSelected(position));
    }

    @Override
    public boolean isDirectory(File dir) {
        return dir.isDirectory();
    }

    protected class fileOnClickListener implements View.OnClickListener, View.OnLongClickListener {
        FilesFragment fragment;
        File file;

        public fileOnClickListener(FilesFragment fragment, File file) {
            this.file = file;
            this.fragment = fragment;
        }

        @Override
        public boolean onLongClick(View v) {
            Log.d(TAG, (file.isDirectory() ? "Directory ": "File ") + file.getName() + " long clicked.");
            if(isSelecting() && file.isDirectory()){
                final View view = v;
                PopupMenu menu = new PopupMenu(fragment.getActivity(), v);
                menu.getMenuInflater().inflate(R.menu.folder_popup_menu, menu.getMenu());
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch(item.getItemId()){
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
            }
            else {
                fragment.selectItem(v);
            }
            /*
            if (isDirectory(file)) {
                Log.d(TAG, "Directory " + file.getName() + " long clicked.");
                UploadDialog dialog = UploadDialog.newInstance(file);
                dialog.setTargetFragment(fragment, 1);
                FragmentManager fm = fragment.getActivity().getSupportFragmentManager();
                dialog.show(fm, "Upload");
            }
            else {
                Log.d(TAG, "File " + file.getName() + " long clicked.");
            }
            */
            return true;
        }

        @Override
        public void onClick(View v) {

            if(isSelecting())
                fragment.selectItem(v);
            else {
                if (isDirectory(file)) {
                    Log.d(TAG, "Directory " + file.getName() + " clicked.");
                    fragment.openDirecory(file.getName());
                } else {
                    try {
//                        Log.d(TAG, "File " + file.getName() + " clicked.");
//                        UploadDialog dialog = UploadDialog.newInstance(file);
//                        dialog.setTargetFragment(fragment, 1);
//                        FragmentManager fm = fragment.getActivity().getSupportFragmentManager();
//                        dialog.show(fm, "Upload");
                    } catch (Exception e){
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                }
            }
        }
    }


}
