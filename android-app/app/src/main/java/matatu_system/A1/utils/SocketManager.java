package matatu_system.A1.utils;

import android.util.Log;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private static final String SOCKET_URL = "http://10.0.2.2:5000"; // Same as API BASE_URL
    private static Socket mSocket;

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

    public static void establishConnection() {
        Socket socket = getSocket();
        if (socket != null && !socket.connected()) {
            socket.connect();
        }
    }

    public static void closeConnection() {
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket = null;
        }
    }
}
