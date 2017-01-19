package joystick.network;

import joystick.shared.JoystickState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketThread extends Thread {
    private final Socket socket;
    private final ObjectOutputStream outputStream;

    public SocketThread(String host, int port) throws IOException {
        super("SocketThread");
        socket = new Socket(host, port);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        BufferedReader inFromServer;
        try {
            inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                String line = inFromServer.readLine();
                System.out.println("FROM SERVER: " + line);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void sendState(JoystickState state) throws IOException {
        System.out.println(state);
        outputStream.writeObject(state);
    }
}
