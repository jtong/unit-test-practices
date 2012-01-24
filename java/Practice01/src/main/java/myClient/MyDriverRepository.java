package myClient;

import myDriver.MyDriver;

public class MyDriverRepository {
    public MyDriverWrapper getMyDriver(String uri) {
        return new MyDriverWrapper(new MyDriver(uri));
    }
}
