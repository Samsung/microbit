package com.samsung.microbit.core;

import android.util.Log;

import com.samsung.microbit.utils.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

//TODO: consider to use somewhere or remove
public class DownloadManager {

    volatile boolean cancelled = false;

    private static final String TAG = DownloadManager.class.getSimpleName();

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public long download(String sourceUrl, String destinationFile) {
        long objectSize = 0L;

        try {
            URL url = new URL(sourceUrl);
            URLConnection urlConnection = url.openConnection();
            InputStream is = urlConnection.getInputStream();
            OutputStream os = new FileOutputStream(new File(destinationFile));
            objectSize = IOUtils.copy(is, os);
            os.close();
            is.close();
        } catch(IOException ex) {
            Log.e(TAG, ex.toString());
        }

        return objectSize;
    }
}
