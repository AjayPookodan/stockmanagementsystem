package com.mycompany.billingsystem.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Utility class to find the local IP address of the machine.
 */
public class NetworkUtil {
    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // Filters out loopback and non-running interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Explicitly check for an IPv4 address and ensure it's a site-local address.
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            // Instead of crashing the app, print an error message to the console.
            // The UI will show "IP Not Found", which is a better user experience.
            System.err.println("Error while finding IP address: " + e.getMessage());
        }
        return "IP Not Found";
    }
}

