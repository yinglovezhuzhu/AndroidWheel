/*
 * Copyright (C) 2014. The Android Open Source Project.
 *
 *         yinglovezhuzhu@gmail.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.opensource.wheel.demo.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.Formatter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-06-06.
 */
public class FileUtil {

    private static final String TAG = "FileUtils";

    private FileUtil() {}

    /**
     * Clean cache directory
     * @param context
     */
    public static void cleanInternalCache(Context context) {
        deleteFile(context.getCacheDir());
    }

    /**
     * Clean database directory
     * @param context
     */
    public static void cleanDatabases(Context context) {
        String [] databases = context.databaseList();
        if(databases != null) {
            for (String database : databases) {
                context.deleteDatabase(database);
            }
        }
    }

    /**
     * Delete database directory
     * @param context
     */
    public static void deleteDatabasesDir(Context context) {
        deleteFile(new File(context.getApplicationInfo().dataDir + "/databases"));
    }

    /**
     * Clean shared_prefs directory
     * @param context
     */
    public static void cleanSharedPreference(Context context) {
        deleteFile(new File(context.getApplicationInfo().dataDir + "/shared_prefs"));
    }

    /**
     * Delete database by name.
     * @param context
     * @param dbName
     */
    public static void cleanDatabaseByName(Context context, String dbName) {
        context.deleteDatabase(dbName);
    }



    /**
     * Clean files directory
     * @param context
     */
    public static void cleanFiles(Context context) {
        deleteFile(context.getFilesDir());
    }

    /**
     * Clean external cache directory.
     * @param context
     */
    @SuppressLint("NewApi")
    public static void cleanExternalCache(Context context) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            deleteFile(context.getExternalCacheDir());
        }
    }

    /**
     * Clean all data
     * @param context
     * @param filepath
     */
    public static void cleanApplicationData(Context context, String... filepath) {
        cleanInternalCache(context);
        cleanExternalCache(context);
        cleanDatabases(context);
        cleanSharedPreference(context);
        cleanFiles(context);
        for(String file : filepath) {
            deleteFile(new File(file));
        }
    }

    /**
     * Delete file(include not empty directory)
     * @param file
     */
    public static void deleteFile(File file) {
        if(file.exists()) {
            if(file.isDirectory()) {
                File [] files = file.listFiles();
                for (File file2 : files) {
                    deleteFile(file2);
                }
            }
            if(!file.delete()) {
                LogUtil.e(TAG, "Delete file failed, path:" + file.getPath());
            }
        }
    }

    /**
     * Delete file
     * @param path
     * @return
     */
    public static boolean deleteFile(String path) {
        if(StringUtil.isEmpty(path)) {
            return false;
        }
        File file = new File(path);
        boolean result = file.delete();
        if(!result) {
            LogUtil.e(TAG, "Delete file failed, path:" + file.getPath());
        }
        return result;
    }

    /**
     * Get file size(include directory)
     * @param file
     * @return
     */
    public static long getFileSize(File file) {
        long size = 0L;
        if(file.exists()) {
            size += file.length();
            if(file.isDirectory()) {
                File [] files = file.listFiles();
                for (File file2 : files) {
                    size += getFileSize(file2);
                }
            }
        }
        return size;
    }

    /**
     * Change byte to KB/MB/GB...（keep two float point）
     * @param context
     * @param size
     * @return
     */
    public static String formatByte(Context context, long size){
        return Formatter.formatFileSize(context, size);// Change byte to KB or MB, etc.
    }

    /**
     * Change byte to KB/MB/GB...(Keep Integer)
     * @param size
     * @return
     */
    public static String formatByteFixed(long size){
        if(size <= 0) return "0B";
        if(size < 1024) return size + "B"; else size = size/1024;
        if(size < 1024) return size + "KB"; else size = size/1024;
        if(size < 1024) return size + "MB"; else size = size/1024;
        if(size < 1024) return size + "GB"; else size = size/1024;
        if(size < 1024) return size + "TB"; else size = size/1024;
        if(size < 1024) return size + "PB"; else size = size/1024;
        if(size < 1024) return size + "EB"; else size = size/1024;
        if(size < 1024) return size + "ZB"; else size = size/1024;
        if(size < 1024) return size + "YB"; else size = size/1024;
        if(size < 1024) return size + "NB"; else size = size/1024;
        if(size < 1024) return size + "DB"; else size = size/1024; return size + "CB";
    }

    /**
     * Parse a content uri to a file.
     * Some file manager return Uri like "file:///sdcard/test.mp4",
     * In this case Uri.getPath() get the file path in file system,
     * so can create a file object with this path, if this file is exists,
     * means parse file success.
     * Some file manager such as Gallery, return Uri like "content://video/8323",
     * In this case Uri.getPath() can't get file path in file system,
     * but can user ContentResolver to get file path from media database.
     * @param uri
     * @return
     */
    public static File parseUriToFile(Context context, Uri uri) {
        if(uri == null) {
            return null;
        }
        File file = null;
        String path = uri.getPath();
        file = new File(path); //If this file is exists, means parse file success.
        if(!file.exists()) {
            //Use ContentResolver to get file path from media database.
            ContentResolver cr = context.getContentResolver();
            String [] pro = new String [] {MediaStore.MediaColumns.DATA, };
            Cursor cursor = cr.query(uri, pro, null, null, null);
            if(cursor != null) {
                String [] cs = cursor.getColumnNames();
                for (String string : cs) {
                    System.out.println(string);
                }
                if(cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                    path = cursor.getString(index);
                    if(!StringUtil.isEmpty(path)) {
                        file = new File(path);
                        if(!file.exists()) {
                            file = null;
                        }
                    }
                }
                cursor.close();
            }
        }
        return file;
    }


    /**
     * 把raw中文件复制到指定目录下的文件
     * @param context
     * @param id
     * @param dist
     * @return
     */
    public static boolean copyRaw2Dir(Context context, int id, File dist) {

        try {
            InputStream is = context.getResources().openRawResource(id);
            FileOutputStream fos = new FileOutputStream(dist);
            byte [] buffer = new byte[1024*1024];
            int size;
            while((size = is.read(buffer)) != -1) {
                fos.write(buffer, 0, size);
            }
            fos.flush();
            fos.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     *
     * @param context
     * @param name
     * @param dist
     * @return
     */
    public static boolean copyAssets2Dir(Context context, String name, File dist) {
        try {
            InputStream is = context.getResources().getAssets().open(name);
            FileOutputStream fos = new FileOutputStream(dist);
            byte [] buffer = new byte[1024*1024];
            int size;
            while((size = is.read(buffer)) != -1) {
                fos.write(buffer, 0, size);
            }
            fos.flush();
            fos.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 读取文本文件内容到String中
     * @param filePath
     * @return
     */
    public static String readStringFromFile(String filePath) {
        File file = new File(filePath);
        if(!file.exists()) {
            return null;
        }
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        try {
            fileInputStream = new FileInputStream(file);
            inputStreamReader = new InputStreamReader(fileInputStream);
            StringBuilder sb = new StringBuilder();
            char [] buffer = new char[1024*1024];
            int len = 0;
            while((len = inputStreamReader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            return sb.toString();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {

        } finally {
            if(null != inputStreamReader) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(null != fileInputStream) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    public static String readStringFromAssetFile(Context context, String name) {
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        int len = 0;
        try {
            inputStream = context.getResources().getAssets().open(name);
            inputStreamReader = new InputStreamReader(inputStream);
            StringBuilder sb = new StringBuilder();
            char [] buffer =  new char[1024*1024];
            while((len = inputStreamReader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(null != inputStreamReader) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    
    /**
     * 保存bitmap
     * @param bitmap
     * @param dir
     * @param fileName
     * @return
     */
    public static String savePicToSdcard(Bitmap bitmap, String dir, String fileName) {
        String filePath = "";
        if (bitmap == null) {
            return filePath;
        } else {
            filePath = dir + File.separator + fileName;
            File destFile = new File(filePath);
            OutputStream os = null;
            try {
                os = new FileOutputStream(destFile);
                bitmap.compress(CompressFormat.JPEG, 80, os);
                os.flush();
                os.close();
            } catch (IOException e) {
                filePath = "";
            }
        }
        return filePath;
    }
    
    public static void dump(String data, String path) {
        if (data != null) {
            File destFile = new File(path);
            if (destFile.exists()) {
                destFile.delete();
            }
            FileWriter fileWriter = null;
            try {
                fileWriter = new FileWriter(destFile);
                fileWriter.write(data);
                fileWriter.flush();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fileWriter != null) {
                        fileWriter.close();
                        fileWriter = null;
                    }
                } catch (Exception e2) {
                }
                
            }
        }
    }
}
