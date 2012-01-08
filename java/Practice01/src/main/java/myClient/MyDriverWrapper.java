package myClient;

import myDriver.MyData;
import myDriver.MyDriver;
import myDriver.MyDriverException;

public class MyDriverWrapper {
    private MyDriver myDriver;

    public void connect() throws MyDriverException {
        myDriver.connect();
    }

    public void addQuery(int id) throws MyDriverException {
        myDriver.addQuery(id);
    }

    public void removeQuery(int id) throws MyDriverException {
        myDriver.removeQuery(id);
    }

    public void close() {
        myDriver.close();
    }

    public MyData receive() throws MyDriverException {
        return myDriver.receive();
    }

    public MyDriverWrapper(MyDriver myDriver)
    {
        this.myDriver = myDriver;
    }

}
