package myClient;

import myDriver.MyDriverException;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import static myClient.MyConnection.RECONNECT_INTERVAL;

public class MyConnectionSingleThread {
    private String[] uris;
    private MyDriverRepository myDriverRepository;
    private MyThreadUtil myThreadUtil;
    private MyDriverWrapper currentMyDriver;
    private List<MyConnectionEventListener> listeners;

    public MyConnectionSingleThread(String[] uris, MyDriverRepository myDriverRepository, MyThreadUtil myThreadUtil) {
        this.uris = uris;
        this.myDriverRepository = myDriverRepository;
        this.myThreadUtil = myThreadUtil;
        this.listeners = new ArrayList<MyConnectionEventListener>();
    }

     public void open() throws InterruptedException {
        currentMyDriver = start(uris, myDriverRepository);
    }

    private MyDriverWrapper start(String[] uris, MyDriverRepository myDriverRepository1) throws InterruptedException {
        while (true) {
            for (String uri : uris) {
                MyDriverWrapper currentMyDriver = myDriverRepository1.getMyDriver(uri);
                try {
                    currentMyDriver.connect();
                    return currentMyDriver;
                } catch (MyDriverException e) {
                    currentMyDriver.close();
                    this.myThreadUtil.sleep(RECONNECT_INTERVAL);
                    for (MyConnectionEventListener listener : listeners) {
                        listener.connectionFailed(new EventObject(this));
                    }
                }
            }
        }
    }

    public void addListener(MyConnectionEventListener listener) {
        this.listeners.add(listener);
    }

    public void close() {
        this.currentMyDriver.close();
    }
}
