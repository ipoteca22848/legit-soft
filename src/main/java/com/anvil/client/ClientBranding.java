package com.anvil.client;
public final class ClientBranding {
 public static final String VERSION=net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("anvil").orElseThrow().getMetadata().getVersion().getFriendlyString();
 private ClientBranding(){}
}
