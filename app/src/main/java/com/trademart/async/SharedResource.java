package com.trademart.async;

import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Component;

import com.trademart.db.DatabaseController;

@Component
public class SharedResource {

    private Semaphore semaphore;
    private DatabaseController databaseController;

    public SharedResource(DatabaseController databaseController) {
        this.semaphore = new Semaphore(1);
        this.databaseController = databaseController;
        initResources();
    }

    private void initResources() {
        databaseController.connect();
    }

    public void lock() throws InterruptedException{
        semaphore.acquire();
    }

    public boolean tryLock(){
        return semaphore.tryAcquire();
    }

    public void unlock(){
        semaphore.release();
    }

    public DatabaseController getDatabaseController() {
        return databaseController;
    }

}
