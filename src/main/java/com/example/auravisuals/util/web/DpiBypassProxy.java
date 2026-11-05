package com.example.auravisuals.util.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DpiBypassProxy {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static volatile int port = -1;

    private DpiBypassProxy() {
    }

    public static int port() {
        return port;
    }

    public static void enable() {
        if (STARTED.compareAndSet(false, true)) {
            try {
                ServerSocket server = new ServerSocket();
                server.setReuseAddress(true);
                server.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
                port = server.getLocalPort();
                Thread acceptor = new Thread(() -> acceptLoop(server), "AuraDPIBypass-accept");
                acceptor.setDaemon(true);
                acceptor.start();
                installCefProxy();
            } catch (Throwable var2) {
                STARTED.set(false);
                port = -1;
            }
        }
    }

    private static void acceptLoop(ServerSocket server) {
        while (!server.isClosed()) {
            Socket client;
            try {
                client = server.accept();
            } catch (IOException var3) {
                return;
            }

            Thread t = new Thread(() -> handle(client), "AuraDPIBypass-conn");
            t.setDaemon(true);
            t.start();
        }
    }

    private static void handle(Socket client) {
        Socket remote = null;

        try {
            client.setTcpNoDelay(true);
            InputStream cin = client.getInputStream();
            String requestLine = readLine(cin);
            if (requestLine == null || requestLine.isEmpty()) {
                client.close();
                return;
            }

            StringBuilder rawHeaders = new StringBuilder();

            String header;
            while ((header = readLine(cin)) != null && !header.isEmpty()) {
                rawHeaders.append(header).append("\r\n");
            }

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                client.close();
                return;
            }

            String method = parts[0];
            if ("CONNECT".equalsIgnoreCase(method)) {
                String[] hp = parts[1].split(":");
                String host = hp[0];
                int rport = hp.length > 1 ? safeInt(hp[1], 443) : 443;
                remote = new Socket();
                remote.setTcpNoDelay(true);
                remote.connect(new InetSocketAddress(host, rport), 8000);
                OutputStream cout = client.getOutputStream();
                cout.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                cout.flush();
                pump(client, remote, true);
                return;
            }

            String url = parts[1];
            int rport = 80;
            String path = "/";
            int schemeIdx = url.indexOf("://");
            String hostPart = schemeIdx >= 0 ? url.substring(schemeIdx + 3) : url;
            int slash = hostPart.indexOf(47);
            if (slash >= 0) {
                path = hostPart.substring(slash);
                hostPart = hostPart.substring(0, slash);
            }

            int colon = hostPart.indexOf(58);
            String host;
            if (colon >= 0) {
                rport = safeInt(hostPart.substring(colon + 1), 80);
                host = hostPart.substring(0, colon);
            } else {
                host = hostPart;
            }

            remote = new Socket();
            remote.setTcpNoDelay(true);
            remote.connect(new InetSocketAddress(host, rport), 8000);
            OutputStream rout = remote.getOutputStream();
            rout.write((method + " " + path + " " + (parts.length > 2 ? parts[2] : "HTTP/1.1") + "\r\n").getBytes(StandardCharsets.US_ASCII));
            rout.write(rawHeaders.toString().getBytes(StandardCharsets.US_ASCII));
            rout.write("\r\n".getBytes(StandardCharsets.US_ASCII));
            rout.flush();
            pump(client, remote, false);
        } catch (Throwable var17) {
            closeQuietly(remote);
            closeQuietly(client);
        }
    }

    private static void pump(Socket client, Socket remote, boolean fragmentFirst) {
        Socket c = client;
        Socket r = remote;
        Thread up = new Thread(() -> {
            try {
                relayUpstream(client.getInputStream(), remote.getOutputStream(), fragmentFirst);
            } catch (Throwable var7) {
            } finally {
                closeQuietly(client);
                closeQuietly(remote);
            }
        }, "AuraDPIBypass-up");
        up.setDaemon(true);
        up.start();

        try {
            copy(r.getInputStream(), c.getOutputStream());
        } catch (Throwable var10) {
        } finally {
            closeQuietly(client);
            closeQuietly(remote);
        }
    }

    private static void relayUpstream(InputStream in, OutputStream out, boolean fragmentFirst) throws IOException {
        byte[] buf = new byte[16384];
        boolean first = fragmentFirst;

        int n;
        while ((n = in.read(buf)) != -1) {
            if (first) {
                first = false;
                if (writeFragmented(out, buf, n)) {
                    continue;
                }
            }

            out.write(buf, 0, n);
            out.flush();
        }
    }

    private static boolean writeFragmented(OutputStream out, byte[] data, int len) {
        try {
            int splitAt = sniSplitPosition(data, len);
            if (splitAt > 0 && splitAt < len) {
                out.write(data, 0, splitAt);
                out.flush();
                out.write(data, splitAt, len - splitAt);
                out.flush();
                return true;
            } else {
                return false;
            }
        } catch (IOException var4) {
            return false;
        }
    }

    private static int sniSplitPosition(byte[] b, int len) {
        try {
            if (len < 45) {
                return -1;
            }

            if ((b[0] & 255) != 22) {
                return -1;
            }

            if ((b[5] & 255) != 1) {
                return -1;
            }

            int p = 9;
            p += 2;
            p += 32;
            int sessionLen = b[p] & 255;
            p += 1 + sessionLen;
            int cipherLen = (b[p] & 255) << 8 | b[p + 1] & 255;
            p += 2 + cipherLen;
            int compLen = b[p] & 255;
            p += 1 + compLen;
            if (p + 2 > len) {
                return -1;
            }

            int extTotal = (b[p] & 255) << 8 | b[p + 1] & 255;
            p += 2;
            int extEnd = Math.min(len, p + extTotal);

            while (p + 4 <= extEnd) {
                int type = (b[p] & 255) << 8 | b[p + 1] & 255;
                int extLen = (b[p + 2] & 255) << 8 | b[p + 3] & 255;
                int body = p + 4;
                if (type == 0) {
                    int q = body + 2 + 1;
                    if (q + 2 > len) {
                        return -1;
                    }

                    int nameLen = (b[q] & 255) << 8 | b[q + 1] & 255;
                    int nameStart = q + 2;
                    if (nameLen > 1 && nameStart + nameLen <= len) {
                        return nameStart + nameLen / 2;
                    }

                    return -1;
                }

                p = body + extLen;
            }
        } catch (Throwable var14) {
        }

        return -1;
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[16384];

        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
            out.flush();
        }
    }

    private static void installCefProxy() {
        if (port > 0) {
            try {
                Class<?> cefAppClass = Class.forName("org.cef.CefApp");
                Class<?> handlerItf = Class.forName("org.cef.handler.CefAppHandler");
                String proxyValue = "http://127.0.0.1:" + port;
                Object handler = Proxy.newProxyInstance(handlerItf.getClassLoader(), new Class[]{handlerItf}, (proxy, method, args) -> {
                    if ("onBeforeCommandLineProcessing".equals(method.getName()) && args != null && args.length == 2) {
                        Object commandLine = args[1];

                        try {
                            Method append = commandLine.getClass().getMethod("appendSwitchWithValue", String.class, String.class);
                            append.invoke(commandLine, "proxy-server", proxyValue);
                        } catch (Throwable var6) {
                        }
                    }

                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) {
                        return false;
                    } else if (rt == int.class) {
                        return 0;
                    } else {
                        return rt == long.class ? 0L : null;
                    }
                });
                Method addAppHandler = cefAppClass.getMethod("addAppHandler", handlerItf);
                addAppHandler.invoke(null, handler);
            } catch (Throwable var5) {
            }
        }
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int prev = -1;

        int c;
        while ((c = in.read()) != -1) {
            if (c == 10) {
                if (prev == 13 && sb.length() > 0) {
                    sb.setLength(sb.length() - 1);
                }

                return sb.toString();
            }

            sb.append((char)c);
            prev = c;
            if (sb.length() > 8192) {
                break;
            }
        }

        return sb.length() == 0 ? null : sb.toString();
    }

    private static int safeInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception var3) {
            return def;
        }
    }

    private static void closeQuietly(Socket s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException var2) {
            }
        }
    }
}
