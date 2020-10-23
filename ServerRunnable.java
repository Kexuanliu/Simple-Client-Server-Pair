import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class ServerRunnable extends Thread {
    private DatagramSocket socket = null;
    private DatagramPacket dp = null;
    private int secret;
    private short studentNum;
    private int num3, len3, port;
    private Random rand = new Random();
    private ServerSocket serverSocket;
    private Socket tcpSocket;
    private OutputStream out;

    public ServerRunnable(DatagramSocket socket, DatagramPacket dp) {
        this.socket = socket;
        this.dp = dp;
    }

    @Override
    public void run() {
        if (astage() == false) {
            //socket.close();
            return;
        }
        if (bstage() == false) {
            //socket.close();
            return;
        }
        //socket.close();
        if (cstage() == false) {
            try {
                tcpSocket.close();
                serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();;
            }
            return;
        }
        if (dstage() == false) {
            try {
                tcpSocket.close();
                serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            tcpSocket.close();
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean astage() {
        int size = dp.getLength();
        if (size != 24) {
            return false;
        }

        byte[] data = dp.getData();
        ByteBuffer bb = ByteBuffer.wrap(data);
        boolean result = checkHeader(bb, 12, 0, (short)1);
        if (!result) {
            return false;
        }

        byte[] payload = new byte[12];
        bb.get(payload);
        String hw = "hello world\0";
        if (!hw.equals(new String(payload))) {
            return false;
        }

        num3 = rand.nextInt(20) + 1;
        len3 = rand.nextInt(100) + 1;
        port = rand.nextInt(49151 - 1024) + 1024;
        while (!isValidPort()) {
            port = rand.nextInt(49151 - 1024) + 1024;
        }
        int tmpsecret = rand.nextInt(100);

        ByteBuffer res = ByteBuffer.allocate(28);
        res.putInt(16);
        res.putInt(0);
        res.putShort((short)2);
        res.putShort(studentNum);
        res.putInt(num3);
        res.putInt(len3);
        res.putInt(port);
        res.putInt(tmpsecret);

        byte[] sendData = res.array();

        DatagramPacket response = new DatagramPacket(sendData, sendData.length, dp.getAddress(), dp.getPort());
        try {
            socket.send(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        secret = tmpsecret;
        return true;
    }

    private boolean bstage() {
        int received = 0;
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        int times = 0;

        try {
            socket.setSoTimeout(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (received < num3) {
            try {
                socket.receive(dp);
            } catch (SocketTimeoutException e) {
                return false;
            } catch (Exception e) {
                e.printStackTrace();
            }

            int len1 = pad(len3 + 4);
            int size = dp.getLength();
            if (size != (len1 + 12)) {
                return false;
            }

            byte[] data = dp.getData();
            ByteBuffer bb = ByteBuffer.wrap(data);
            boolean result = checkHeader(bb, len3 + 4, secret, (short)1);
            if (!result) {
                return false;
            }
            int packId = bb.getInt();
            if (packId == received) {
                for (int i = 0; i < len3; i++) {
                    if (bb.get() != (byte)0) {
                        return false;
                    }
                }
                if (times > 0) {
                    if (rand.nextInt(2) == 1) {
                        ByteBuffer resp = ByteBuffer.allocate(16);
                        resp.putInt(4);
                        resp.putInt(secret);
                        resp.putShort((short)1);
                        resp.putShort(studentNum);
                        resp.putInt(received);
                        received++;
                        times = -1;
                        byte[] sendData = resp.array();
                        DatagramPacket response = new DatagramPacket(sendData, sendData.length, dp.getAddress(), dp.getPort());
                        try {
                            socket.send(response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                times++;
            } else {
                return false;
            }
        }

        ByteBuffer res2 = ByteBuffer.allocate(20);
        res2.putInt(8);
        res2.putInt(secret);
        res2.putShort((short)2);
        res2.putShort(studentNum);
        int tmpbse = rand.nextInt(100);
        port = rand.nextInt(49151 - 1024) + 1024;
        while (!isValidPort()) {
            port = rand.nextInt(49151 - 1024) + 1024;
        }
        res2.putInt(port);
        res2.putInt(tmpbse);
        byte[] b2 = res2.array();

        DatagramPacket send2 = new DatagramPacket(b2, b2.length, dp.getAddress(), dp.getPort());
        try {
            socket.send(send2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        secret = tmpbse;
        return true;
    }

    private boolean cstage() {
        try {
            serverSocket = new ServerSocket(port);
            tcpSocket = serverSocket.accept();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            out = tcpSocket.getOutputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ByteBuffer bb = ByteBuffer.allocate(28);
        bb.putInt(13);
        bb.putInt(secret);
        bb.putShort((short)2);
        bb.putShort(studentNum);

        num3 = rand.nextInt(20) + 1;
        len3 = rand.nextInt(100) + 1;
        int tmpS = rand.nextInt(100);

        bb.putInt(num3);
        bb.putInt(len3);
        bb.putInt(tmpS);
        bb.putChar('c');

        try {
            out.write(bb.array());
        } catch (Exception e) {
            e.printStackTrace();
        }
        secret = tmpS;
        return true;
    }

    private boolean dstage() {
        InputStream in;
        try {
            in = tcpSocket.getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        int gotNum = 0;

        try {
            tcpSocket.setSoTimeout(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (gotNum < num3) {
            byte[] data;
            if (gotNum == num3 - 1) {
                data = new byte[pad(len3) + 16];
            } else {
                data = new byte[pad(len3) + 12];
            }

            try {
                if (gotNum == num3 - 1) {
                    if (in.read(data) != (pad(len3) + 12)) {
                        return false;
                    }
                } else {
                    in.read(data);
                }
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                return false;
            } catch (Exception e) {
                e.printStackTrace();
            }

            ByteBuffer bb = ByteBuffer.wrap(data);
            boolean result = checkHeader(bb, len3, secret, (short) 1);
            if (result == false) {
                return false;
            }

            for (int i = 0; i < len3; i++) {
                if (bb.get() != (byte) 'c') {
                    return false;
                }
            }
            gotNum++;
        }

        ByteBuffer bb3 = ByteBuffer.allocate(16);
        bb3.putInt(4);
        bb3.putInt(secret);
        bb3.putShort((short)2);
        bb3.putShort(studentNum);

        secret = rand.nextInt(100);
        bb3.putInt(secret);

        try {
            out.write(bb3.array());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean checkHeader(ByteBuffer bb, int len, int s, short step) {
        int clen = bb.getInt();
        int cs = bb.getInt();
        short cstep = bb.getShort();
        short cnum = bb.getShort();

        if (clen != len || cs != s || cstep != step) {
            return false;
        }
        studentNum = cnum;
        return true;
    }

    private static int pad(int len) {
        if (len % 4 != 0) {
            len = len + (4 - (len % 4));
        }
        return len;
    }

    private boolean isValidPort() {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
            return true;
        } catch (Exception e) {
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                }
            }
        }
        return false;
    }

}
