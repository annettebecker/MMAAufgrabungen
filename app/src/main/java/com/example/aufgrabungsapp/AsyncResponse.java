package com.example.aufgrabungsapp;

/**
 * Interface: to get return values from asynctask
 * according : https://learnandroidtoday.wordpress.com/2013/09/17/how-to-get-results-of-async-task/
 */

public interface AsyncResponse {
    void downloadFinish(String output);
}
