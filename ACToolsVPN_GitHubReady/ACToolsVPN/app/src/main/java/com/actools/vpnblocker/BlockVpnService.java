package com.actools.vpnblocker;

import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class BlockVpnService extends VpnService implements Runnable {
    private Thread thread;
    private ParcelFileDescriptor vpnInterface;

    @Override
    public int onStartCommand(android.content.Intent intent, int flags, int startId) {
        Builder builder = new Builder();
        builder.setSession("ACTools VPN")
               .addAddress("10.0.0.2", 24)
               .addDnsServer("8.8.8.8")
               .addRoute("0.0.0.0", 0);

        vpnInterface = builder.establish();

        thread = new Thread(this, "VPNThread");
        thread.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
            }
        } catch (Exception ignored) {}
        if (thread != null) {
            thread.interrupt();
        }
        super.onDestroy();
    }

    @Override
    public void run() {
        FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
        FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());
        ByteBuffer packet = ByteBuffer.allocate(32767);

        while (!Thread.interrupted()) {
            try {
                int length = in.read(packet.array());
                if (length > 0) {
                    packet.limit(length);

                    // Simple domain/IP block check (example: block by IP)
                    byte[] data = packet.array();
                    if (containsBlockedAddress(data)) {
                        // Drop packet
                        packet.clear();
                        continue;
                    }

                    out.write(packet.array(), 0, length);
                    packet.clear();
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private boolean containsBlockedAddress(byte[] data) {
        // Placeholder for real IP/host detection logic
        // IP address 44.231.172.6 in bytes: [44, 231, 172, 6]
        return (data.length > 0 &&
                (data[16] == 44 && data[17] == (byte)231 &&
                 data[18] == (byte)172 && data[19] == 6));
    }
}