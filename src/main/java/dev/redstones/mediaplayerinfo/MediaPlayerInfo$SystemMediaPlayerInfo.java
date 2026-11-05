package dev.redstones.mediaplayerinfo;

import dev.redstones.mediaplayerinfo.impl.DummyMediaPlayerInfo;
import dev.redstones.mediaplayerinfo.impl.win.WindowsMediaPlayerInfo;

public class MediaPlayerInfo$SystemMediaPlayerInfo {
    static final MediaPlayerInfo instance = System.getProperty("os.name").toLowerCase().startsWith("windows")
        ? new WindowsMediaPlayerInfo()
        : new DummyMediaPlayerInfo();

    public static MediaPlayerInfo getInstance() {
        return instance;
    }
}
