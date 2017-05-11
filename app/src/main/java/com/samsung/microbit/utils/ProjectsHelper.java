package com.samsung.microbit.utils;

import android.os.Environment;
import android.util.Log;

import com.samsung.microbit.data.model.Project;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * Provides functionality to operate with a project items,
 * such as get a number of total saved projects and search
 * projects on a mobile device.
 */
public class ProjectsHelper {
    //TODO: Change to data/data/appName/files MBApp.getContext().getFilesDir();
    public static File HEX_FILE_DIR = Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
    //public static File HEX_FILE_DIR = MBApp.getContext().getFilesDir();

    private ProjectsHelper() {
    }

    public static int getTotalSavedProjects() {
        File sdcardDownloads = HEX_FILE_DIR;
        int totalPrograms = 0;
        if(sdcardDownloads.exists()) {
            File files[] = sdcardDownloads.listFiles();
            if(files != null) {
                for(File file : files) {
                    String fileName = file.getName();
                    if(fileName.endsWith(".hex")) {
                        ++totalPrograms;
                    }
                }
            }
        }
        return totalPrograms;
    }

    public static int findProjectssAndPopulate(HashMap<String, String> prettyFileNameMap, List<Project> list) {
        File sdcardDownloads = HEX_FILE_DIR;
        Log.d("MicroBit", "Searching files in " + sdcardDownloads.getAbsolutePath());

        int totalPrograms = 0;
        if(sdcardDownloads.exists()) {
            File files[] = sdcardDownloads.listFiles();
            for(File file : files) {
                String fileName = file.getName();
                if(fileName.endsWith(".hex")) {

                    //Beautify the filename
                    String parsedFileName;
                    int dot = fileName.lastIndexOf(".");
                    parsedFileName = fileName.substring(0, dot);
                    parsedFileName = parsedFileName.replace('_', ' ');

                    if(prettyFileNameMap != null) {
                        prettyFileNameMap.put(parsedFileName, fileName);
                    }

                    if(list != null) {
                        list.add(new Project(parsedFileName, file.getAbsolutePath(), file.lastModified(),
                                null, false));
                    }

                    ++totalPrograms;
                }
            }
        }
        return totalPrograms;
    }
}
