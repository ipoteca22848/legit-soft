package dev.redstones.mediaplayerinfo;

import java.util.List;

public interface MediaPlayerInfo {
    MediaPlayerInfo INSTANCE = MediaPlayerInfo$SystemMediaPlayerInfo.getInstance();

    List<IMediaSession> getMediaSessions();
}
