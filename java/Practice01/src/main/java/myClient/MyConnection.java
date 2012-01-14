package myClient;

import myDriver.MyDriverException;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

public class MyConnection {

    public final static int RECONNECT_INTERVAL = 3000;
    private MyConnectionSingleThread myConnectionSingleThread;
    private List<MyConnectionEventListener> eventListener;

    public MyConnection(String[] uris) {
        this(new MyConnectionSingleThread(uris, new MyDriverRepository(), new MyThreadUtil()));
    }

    public MyConnection(MyConnectionSingleThread myConnectionSingleThread) {
        this.myConnectionSingleThread = myConnectionSingleThread;
        this.eventListener = new ArrayList<MyConnectionEventListener>();
    }

    public void open() {
        new Thread(new Runnable() {


            @Override
            public void run() {
                try {
                    myConnectionSingleThread.open();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                dispatchConnectedEvent(eventListener);
            }

            private void dispatchConnectedEvent(List<MyConnectionEventListener> eventListener) {
                for (MyConnectionEventListener myConnectionEventListener : eventListener) {
                    myConnectionEventListener.connected(new EventObject(MyConnection.this));
                }
            }
        }).start();
    }

    public void close() {
        throw new RuntimeException("Not implemented");
    }

    public Closeable subscribe(int queryId, MySubscriber subscriber) {
        throw new RuntimeException("Not implemented");
    }

    public synchronized void addConnectionListener(MyConnectionEventListener listener) {
        this.eventListener.add(listener);
        this.myConnectionSingleThread.addListener(listener);
    }

    public synchronized void removeConnectionListener(MyConnectionEventListener listener) {
        throw new RuntimeException("Not implemented");
    }
}
