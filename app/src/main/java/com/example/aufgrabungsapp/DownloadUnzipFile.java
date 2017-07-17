package com.example.aufgrabungsapp;

/**
 * according: https://stackoverflow.com/questions/9324103/download-and-extract-zip-file-in-android
 * according: https://stackoverflow.com/questions/23056804/android-java-how-to-download-zip-file-from-url
 */

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class DownloadUnzipFile  extends AsyncTask <Object, Integer, String>{

    File file;
    File SDCardRoot = Environment.getExternalStorageDirectory();
    MainActivity mainActivity;
    public AsyncResponse delegatResult = null;

    @Override
    protected String doInBackground(Object[] params) {
        mainActivity = (MainActivity)params[0];
        //start download
        boolean downloadEnd = download();
        //if download finished, unzip folder
        if(downloadEnd)
            unpackZip(SDCardRoot.getPath(),"aufgrabungen.zip");
        //return filepath
        return SDCardRoot.getPath()+"/"+"aufgrabungen.shp";
    }


    /**
     * download zipFile
     * @return
     */
    public boolean download()
    {
        try {
            //set the download URL, a url that points to a file on the internet
            //this is the file to be downloaded
            URL url = new URL("https://www.stadt-muenster.de/ows/mapserv621/odaufgrabserv?REQUEST=GetFeature&SERVICE=WFS&VERSION=1.1.0&TYPENAME=aufgrabungen&srsName=EPSG:25832&outputFormat=SHAPEZIP");

            //create the new connection
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            //set up some things on the connection
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);

            //and connect!
            urlConnection.connect();


            //create a new file, specifying the path, and the filename
            //which we want to save the file as.
            file = new File(SDCardRoot,"aufgrabungen.zip");

            //this will be used to write the downloaded data into the file we created
            FileOutputStream fileOutput = new FileOutputStream(file);

            //this will be used in reading the data from the internet
            InputStream inputStream = urlConnection.getInputStream();

            //this is the total size of the file
            int totalSize = urlConnection.getContentLength();
            //variable to store total downloaded bytes
            int downloadedSize = 0;

            //create a buffer...
            byte[] buffer = new byte[1024];
            int bufferLength = 0; //used to store a temporary size of the buffer

            //now, read through the input buffer and write the contents to the file
            while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
                //add the data in the buffer to the file in the file output stream (the file on the sd card
                fileOutput.write(buffer, 0, bufferLength);
                //add up the size so we know how much is downloaded
                downloadedSize += bufferLength;
            }
            //close the output stream when done
            fileOutput.close();
            Log.w("Download", "finished");
            //catch some possible errors...
        } catch (MalformedURLException e) {
            Log.e("Download", "error");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("Download", "error");
            e.printStackTrace();
        }
        return true;
    }


    /**
     * unzip download-Folder
     * @param path
     * @param zipname
     * @return
     */
    private void unpackZip(String path, String zipname)
    {
        InputStream is;
        ZipInputStream zis;
        try
        {
            String filename;
            is = new FileInputStream(path +"/"+ zipname);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null)
            {
                filename = ze.getName();

                // Need to create directories if not exists, or
                // it will generate an Exception...
                if (ze.isDirectory()) {
                    File fmd = new File(path + filename);
                    fmd.mkdirs();
                    continue;
                }
                if(MainActivity.verifyStoragePermissions(mainActivity)){
                    FileOutputStream fout = new FileOutputStream(path +"/"+ filename);

                    // unzip file
                    while ((count = zis.read(buffer)) != -1)
                    {
                        fout.write(buffer, 0, count);
                    }

                    fout.close();
                }
                zis.closeEntry();
            }

            zis.close();
            Log.e("Unzip", "finished");
        }
        catch(IOException e)
        {
            Log.e("Unzip", "error");
            e.printStackTrace();

        }
    }


    /**
     * deliver result
     * @param result
     */
    @Override
    protected void onPostExecute(String result){
        delegatResult.downloadFinish(result);
    }

}