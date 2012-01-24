package myClient;

public class MyThreadUtil {
    public void sleep(int reconnectInterval) throws InterruptedException {
        Thread.currentThread().sleep(reconnectInterval);
    }
}
