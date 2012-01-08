package myClient;

import myDriver.MyDriver;
import myDriver.MyDriverException;

import java.io.Closeable;

public class MyConnection {

    public final static int RECONNECT_INTERVAL = 3000;
    private MyDriverWrapper currentMyDriver;
    private String[] uris;
    private MyDriverRepository myDriverRepository;

    public MyConnection(String[] uris) {
        this(uris, new MyDriverRepository());
    }

    MyConnection(String[] uris, MyDriverRepository myDriverRepository) {
        this.uris = uris;
        this.myDriverRepository = myDriverRepository;
    }

    public void open() {
        while(true){
            if (connectOneRound()) return;
        }

    }

    private boolean connectOneRound() {
        for (String uri : uris) {
            try {
                currentMyDriver = myDriverRepository.getMyDriver(uri);
                currentMyDriver.connect();
                return true;
            } catch (MyDriverException e) {
                currentMyDriver.close();
            }
        }
        return false;
    }

    public void close() {
        throw new RuntimeException("Not implemented");
    }

    public Closeable subscribe(int queryId, MySubscriber subscriber) {
        throw new RuntimeException("Not implemented");
    }

    public synchronized void addConnectionListener(MyConnectionEventListener listener) {
        throw new RuntimeException("Not implemented");
    }

    public synchronized void removeConnectionListener(MyConnectionEventListener listener) {
        throw new RuntimeException("Not implemented");
    }
}
