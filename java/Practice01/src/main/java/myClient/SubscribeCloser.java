package myClient;

import myDriver.MyDriverException;

import java.io.Closeable;
import java.io.IOException;

public class SubscribeCloser implements Closeable{

    private Integer queryId;
    private MyConnectionSingleThread myConnectionSingleThread;

    public SubscribeCloser(Integer queryId, MyConnectionSingleThread myConnectionSingleThread) {
        this.queryId = queryId;
        this.myConnectionSingleThread = myConnectionSingleThread;
    }

    @Override
    public void close() throws IOException {
        try {
            this.myConnectionSingleThread.removeSubscriber(queryId);
        } catch (MyDriverException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubscribeCloser that = (SubscribeCloser) o;

        if (myConnectionSingleThread != null ? !myConnectionSingleThread.equals(that.myConnectionSingleThread) : that.myConnectionSingleThread != null)
            return false;
        if (queryId != null ? !queryId.equals(that.queryId) : that.queryId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = queryId != null ? queryId.hashCode() : 0;
        result = 31 * result + (myConnectionSingleThread != null ? myConnectionSingleThread.hashCode() : 0);
        return result;
    }
}
