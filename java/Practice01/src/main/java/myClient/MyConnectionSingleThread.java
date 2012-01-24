package myClient;

import myDriver.MyData;
import myDriver.MyDriverException;

import java.io.Closeable;
import java.util.*;

import static myClient.MyConnection.RECONNECT_INTERVAL;

public class MyConnectionSingleThread {
    private String[] uris;
    private MyDriverRepository myDriverRepository;
    private MyThreadUtil myThreadUtil;
    private MyDriverWrapper currentMyDriver;
    private Set<MyConnectionEventListener> listeners;
    private Map<Integer, MySubscriber> subscriberMap;
    private boolean start;

    public MyConnectionSingleThread(String[] uris, MyDriverRepository myDriverRepository, MyThreadUtil myThreadUtil) {
        this.uris = uris;
        this.myDriverRepository = myDriverRepository;
        this.myThreadUtil = myThreadUtil;
        this.listeners = new LinkedHashSet<MyConnectionEventListener>();
        this.subscriberMap = new LinkedHashMap<Integer, MySubscriber>();
    }

    public void open() throws InterruptedException {
        currentMyDriver = startOpenConnection(uris, myDriverRepository);
    }

    private MyDriverWrapper startOpenConnection(String[] uris, MyDriverRepository myDriverRepository) throws InterruptedException {
        while (true) {
            for (String uri : uris) {
                MyDriverWrapper currentMyDriver = myDriverRepository.getMyDriver(uri);
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

    public Closeable register(int queryId, MySubscriber mySubscriber) {
        checkIdExists(queryId);
        this.subscriberMap.put(queryId, mySubscriber);
        try {
            this.currentMyDriver.addQuery(queryId);
        } catch (MyDriverException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return new SubscribeCloser(queryId, this);
    }

    private void checkIdExists(int queryId) {
        if (subscriberMap.keySet().contains(queryId)) {
            throw new RuntimeException(String.format("Query Id %s already exists", queryId));
        }
    }

    public void receive() throws InterruptedException {
         try {
            MyData myData = this.currentMyDriver.receive();
            if (this.subscriberMap.containsKey(myData.queryId)) {
                if ("begin".equals(myData.value)) {
                    this.subscriberMap.get(myData.queryId).onBegin();
                } else {
                    this.subscriberMap.get(myData.queryId).onMessage(myData.value);
                }
            }
        } catch (MyDriverException e) {
            open();
        }
    }

    public synchronized boolean isStart() {
        if(!start)
        {
            markStart();
            return false;
        }
        return start;
    }

    private void markStart() {
        this.start = true;
    }

    public void removeSubscriber(Integer queryId) throws MyDriverException {
        this.subscriberMap.remove(queryId);
        currentMyDriver.removeQuery(queryId);
    }

    public void removeListener(MyConnectionEventListener listener) {
        this.listeners.remove(listener);
    }
}
