package myClient;

import java.io.Closeable;
import java.util.*;

public class MyConnection {

    public final static int RECONNECT_INTERVAL = 3000;
    private MyConnectionSingleThread myConnectionSingleThread;
    private Set<MyConnectionEventListener> eventListener;

    public MyConnection(String[] uris) {
        this(new MyConnectionSingleThread(uris, new MyDriverRepository(), new MyThreadUtil()));
    }

    public MyConnection(MyConnectionSingleThread myConnectionSingleThread) {
        this.myConnectionSingleThread = myConnectionSingleThread;
        this.eventListener = new LinkedHashSet<MyConnectionEventListener>();
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

            private void dispatchConnectedEvent(Set<MyConnectionEventListener> eventListener) {
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
        Closeable closeable = this.myConnectionSingleThread.register(queryId, subscriber);
        if(!this.myConnectionSingleThread.isStart())
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true){
                        try {
                            myConnectionSingleThread.receive();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }).start();

        }
        return closeable;
    }

    public synchronized void addConnectionListener(MyConnectionEventListener listener) {
        this.eventListener.add(listener);
        this.myConnectionSingleThread.addListener(listener);
    }

    public synchronized void removeConnectionListener(MyConnectionEventListener listener) {
        this.eventListener.remove(listener);
        this.myConnectionSingleThread.removeListener(listener);
    }
}
