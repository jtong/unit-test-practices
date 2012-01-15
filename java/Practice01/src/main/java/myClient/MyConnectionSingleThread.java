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
    private List<MyConnectionEventListener> listeners;
    private Set<Integer> queryIds;

    public MyConnectionSingleThread(String[] uris, MyDriverRepository myDriverRepository, MyThreadUtil myThreadUtil) {
        this.uris = uris;
        this.myDriverRepository = myDriverRepository;
        this.myThreadUtil = myThreadUtil;
        this.listeners = new ArrayList<MyConnectionEventListener>();
        this.queryIds = new LinkedHashSet<Integer>();
    }

    public void open() throws InterruptedException {
        currentMyDriver = start(uris, myDriverRepository);
    }

    private MyDriverWrapper start(String[] uris, MyDriverRepository myDriverRepository) throws InterruptedException {
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

    public Closeable subscribe(int queryId, MySubscriber mySubscriber) {
        try {
            MyData myData = this.currentMyDriver.receive();
            if (queryId == myData.queryId) {
                if ("begin".equals(myData.value)) {
                    mySubscriber.onBegin();
                } else {
                    mySubscriber.onMessage(myData.value);
                }
            }
        } catch (MyDriverException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    public void register(int queryId) {
        checkIdExists(queryId);
        this.queryIds.add(queryId);
        try {
            this.currentMyDriver.addQuery(queryId);
        } catch (MyDriverException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void checkIdExists(int queryId) {
        if (queryIds.contains(queryId)) {
            throw new RuntimeException(String.format("Query Id %s already exists", queryId));
        }
    }
}
