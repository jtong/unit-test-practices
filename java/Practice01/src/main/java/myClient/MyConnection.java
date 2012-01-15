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
        this.myConnectionSingleThread.close();
        for (MyConnectionEventListener myConnectionEventListener : eventListener) {
            myConnectionEventListener.disconnected(new EventObject(this));
        }
    }

    public Closeable subscribe(int queryId, MySubscriber subscriber) {
        this.myConnectionSingleThread.register(queryId);
        return this.myConnectionSingleThread.subscribe(queryId, subscriber);
    }

    public synchronized void addConnectionListener(MyConnectionEventListener listener) {
        this.eventListener.add(listener);
        this.myConnectionSingleThread.addListener(listener);
    }

    public synchronized void removeConnectionListener(MyConnectionEventListener listener) {
        throw new RuntimeException("Not implemented");
    }
}
