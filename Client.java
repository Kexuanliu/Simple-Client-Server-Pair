import java.net.*;
import java.nio.ByteBuffer;
import java.io.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Client {
    static short id = 639;
    public static void main(String[]args) throws Exception {
        //stage a
        int port = 12235;
        InetAddress address = InetAddress.getByName("attu2.cs.washington.edu");
        //InetAddress address = InetAddress.getLocalHost();
        DatagramSocket socket = new DatagramSocket();

        String hw = "hello world\0";
        byte[] payload = hw.getBytes();
        int len = payload.length;
        len = pad(len);

        ByteBuffer buf = ByteBuffer.allocate(12 + len);
        buf.putInt(payload.length);
        buf.putInt(0);
        buf.putShort((short)1);
        buf.putShort(id);
        buf.put(payload);
        byte[] buff = buf.array();

        DatagramPacket packet = new DatagramPacket(buff, buff.length,
                                address, port);
        socket.send(packet);

        byte[] res = new byte[28];

        packet = new DatagramPacket(res, res.length);
        socket.receive(packet);
        

        // secret
        ByteBuffer bb = ByteBuffer.wrap(res);
        checkHeader(bb, 16, 0, (short)2);
        int[] Asecret = new int[4];
        for (int i = 0; i < 4; i++) {
            Asecret[i] = bb.getInt();
        }

        // stage b
        int num = Asecret[0];
        int len1 = pad(Asecret[1] + 4);
        port = Asecret[2];
        int asecret = Asecret[3];
        System.out.println("Asecret: " + asecret);
        socket.setSoTimeout(500);

        int acked = 0;
        while (acked < num) {
            ByteBuffer bb2 = ByteBuffer.allocate(len1 + 12);
            bb2.putInt(Asecret[1] + 4);
            bb2.putInt(asecret);
            bb2.putShort((short)1);
            bb2.putShort(id);
            bb2.putInt(acked);
            buff = bb2.array();
            packet = new DatagramPacket(buff, buff.length,
                    address, port);
            socket.send(packet);
            try {
                res = new byte[16];
                socket.receive(new DatagramPacket(res, res.length));
                bb = ByteBuffer.wrap(res);
                checkHeader(bb, 4, asecret, (short)1);
                int receivedNum = bb.getInt();
                if (receivedNum == acked) {
                    acked++;
                }
            } catch (SocketTimeoutException e) {
            }
        }

        res = new byte[20];
        socket.receive(new DatagramPacket(res, res.length));
        bb = ByteBuffer.wrap(res);
        checkHeader(bb, 8, asecret, (short)2);
        port = bb.getInt();
        int bsecret = bb.getInt();
        System.out.println("BSecret: " + bsecret);
        socket.close();

        // stage c
        Socket socket2 = new Socket(address, port);
        InputStream is = socket2.getInputStream();
        res = new byte[28];
        int size1 = is.read(res);

        if (size1 == 28) {
            bb = ByteBuffer.wrap(res);
            checkHeader(bb, 13, bsecret, (short)2);

            int num2 = bb.getInt();
            int len2 = bb.getInt();
            int csecret = bb.getInt();
            char c = bb.getChar();
            System.out.println("CSecret: " + csecret);

            //stage d
            OutputStream os = socket2.getOutputStream();
            for (int i = 0; i < num2; i++) {
                ByteBuffer bb3 = ByteBuffer.allocate(pad(len2) + 12);
                bb3.putInt(len2);
                bb3.putInt(csecret);
                bb3.putShort((short) 1);
                bb3.putShort(id);
                byte[] cs = new byte[len2];
                Arrays.fill(cs, (byte) c);
                bb3.put(cs);
                buff = bb3.array();
                os.write(buff);
            }
            res = new byte[16];
            int size = is.read(res);
            if (size == 16) {
                bb = ByteBuffer.wrap(res);
                checkHeader(bb, 4, csecret, (short)2);

                int dsecret = bb.getInt();
                System.out.println("DSecret: " + dsecret);
            }
        }
        socket2.close();
    }

    public static void checkHeader(ByteBuffer bb, int len, int ps, short step) {
        int clen = bb.getInt();
        int cps = bb.getInt();
        short cstep = bb.getShort();
        short snum = bb.getShort();

        if (clen != len) {
            System.out.println(" length error");
        }
        if (cps != ps) {
            System.out.println("pre-secret error");
        }
        if (cstep != step) {
            System.out.println("step error");
        }
        if (snum != id) {
            System.out.println("student number error");
        }
    }

    public static int pad(int len) {
        if (len % 4 != 0) {
            len = len + (4 - (len % 4));
        }
        return len;
    }
}