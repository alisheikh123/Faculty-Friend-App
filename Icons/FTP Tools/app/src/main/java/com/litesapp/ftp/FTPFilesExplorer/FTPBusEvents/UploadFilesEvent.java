package com.litesapp.ftp.FTPFilesExplorer.FTPBusEvents;


import com.litesapp.ftp.FTPFilesExplorer.FTPLocalExplorer.LocalFilesFragment;

/**
 * Created by Geri on 27/10/2015.
 */
public class UploadFilesEvent {
    public LocalFilesFragment.LFileMap files;

    public UploadFilesEvent(LocalFilesFragment.LFileMap files){
        this.files = files;
    }
}
