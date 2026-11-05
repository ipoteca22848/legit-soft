package com.example.auravisuals.util;

import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerInfo.ServerType;

public final class ServerPinHelper {
    public static final String PINNED_SERVER_NAME = "Domino Grief";
    public static final String PINNED_SERVER_IP = "mc.dominogrief.ru";

    private ServerPinHelper() {
    }

    public static boolean isPinned(ServerInfo serverInfo) {
        if (serverInfo == null) {
            return false;
        }

        String addr;
        return serverInfo.address == null
                || !(addr = serverInfo.address.trim().toLowerCase()).equals("mc.dominogrief.ru") && !addr.startsWith("mc.dominogrief.ru:")
            ? serverInfo.name != null && serverInfo.name.equalsIgnoreCase("Domino Grief")
            : true;
    }

    public static ServerInfo createPinnedServer() {
        return new ServerInfo("Domino Grief", "mc.dominogrief.ru", ServerType.OTHER);
    }
}
