package matatu_system.A1.utils;

import android.util.Log;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private static final String SOCKET_URL = "https://android-project-gb6e.onrender.com";
    private static Socket mSocket;
    private static int refCount = 0;

    public static synchronized Socket getSocket() {
        if (mSocket == null) {
            try {
                mSocket = IO.socket(SOCKET_URL);
            } catch (URISyntaxException e) {
                Log.e(TAG, "Socket initialization error", e);
            }
        }
        return mSocket;
    }

    public static synchronized void establishConnection() {
        refCount++;
        Socket socket = getSocket();
        if (socket != null && !socket.connected()) {
            socket.connect();
        }
    }

    public static synchronized void releaseConnection() {
        refCount = Math.max(0, refCount - 1);
        if (refCount == 0 && mSocket != null) {
            mSocket.disconnect();
            mSocket = null;
        }
    }

    public static synchronized void closeConnection() {
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket = null;
        }
        refCount = 0;
    }
}
