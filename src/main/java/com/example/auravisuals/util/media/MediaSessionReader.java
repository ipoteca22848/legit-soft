package com.example.auravisuals.util.media;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class MediaSessionReader {
    private static final MediaSessionReader INSTANCE = new MediaSessionReader();
    private volatile String title = "";
    private volatile String artist = "";
    private volatile long positionMs = 0L;
    private volatile long durationMs = 0L;
    private volatile boolean playing = false;
    private volatile String source = "";
    private volatile boolean hasMedia = false;
    private volatile long pollTime = 0L;
    private volatile int noneCount = 0;
    private volatile boolean thumbnailUpdated = false;
    private volatile String currentThumbKey = "";
    private volatile IMediaSession activeNativeSession;
    private volatile long powerShellMetadataAt;
    private long stablePositionMs = -1L;
    private long stablePositionReadNanos;
    private String stablePositionTrack = "";
    private Path scriptFile;
    private Path thumbFile;
    private Process process;
    private Thread readerThread;
    private Thread metadataThread;
    private Thread watchdogThread;
    private volatile boolean running = false;
    private volatile long lastHeartbeat = 0L;

    public static MediaSessionReader getInstance() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (!this.running) {
            this.running = true;
            this.noneCount = 0;
            this.lastHeartbeat = System.currentTimeMillis();

            try {
                Path tempDir = Files.createTempDirectory("fv-media-");
                this.scriptFile = tempDir.resolve("query.ps1");
                this.thumbFile = tempDir.resolve("thumb.png");
                Files.writeString(this.scriptFile, this.buildScript(), StandardCharsets.UTF_8);
            } catch (IOException var2) {
                this.scriptFile = null;
                this.thumbFile = null;
            }

            this.readerThread = new Thread(this::runLoop, "MediaSessionReader");
            this.readerThread.setDaemon(true);
            this.readerThread.start();
            if (this.scriptFile != null) {
                this.metadataThread = new Thread(this::runPowerShellMetadataLoop, "MediaSessionMetadata");
                this.metadataThread.setDaemon(true);
                this.metadataThread.setPriority(1);
                this.metadataThread.start();
            }

            this.watchdogThread = new Thread(() -> {
                while (this.running) {
                    if (System.currentTimeMillis() - this.lastHeartbeat > 12000L && this.process != null && this.process.isAlive()) {
                        this.process.destroyForcibly();
                    }

                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException var2x) {
                        break;
                    }
                }
            }, "MediaSessionWatchdog");
            this.watchdogThread.setDaemon(true);
            this.watchdogThread.start();
        }
    }

    public synchronized void stop() {
        this.running = false;
        if (this.watchdogThread != null) {
            this.watchdogThread.interrupt();
            this.watchdogThread = null;
        }

        if (this.process != null) {
            this.process.destroyForcibly();
            this.process = null;
        }

        if (this.readerThread != null) {
            this.readerThread.interrupt();
            this.readerThread = null;
        }

        if (this.metadataThread != null) {
            this.metadataThread.interrupt();
            this.metadataThread = null;
        }

        this.hasMedia = false;
        this.activeNativeSession = null;
    }

    private void runLoop() {
        while (this.running) {
            try {
                this.lastHeartbeat = System.currentTimeMillis();
                this.pollNativeSessions();
                Thread.sleep(350L);
            } catch (InterruptedException var4) {
                break;
            } catch (Throwable var5) {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException var3) {
                    break;
                }
            }
        }
    }

    private void runPowerShellMetadataLoop() {
        while (this.running) {
            try {
                this.startProcess();
                this.readOutput();
            } catch (Exception var2) {
            }

            if (this.running) {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException var3) {
                    break;
                }
            }
        }
    }

    private void pollNativeSessions() throws IOException {
        List<IMediaSession> sessions = MediaPlayerInfo.INSTANCE.getMediaSessions();
        IMediaSession selected = this.selectSession(sessions);
        if (selected != null && selected.getMedia() != null) {
            MediaInfo info = selected.getMedia();
            String newTitle = this.cleanMetadata(info.getTitle()).replaceFirst("(?i)\\s*\\|\\s*soundcloud track\\s*$", "");
            String newArtist = this.cleanMetadata(info.getArtist());
            if (!newTitle.isBlank()) {
                this.noneCount = 0;
                this.activeNativeSession = selected;
                if (System.currentTimeMillis() - this.powerShellMetadataAt > 2500L) {
                    this.title = newTitle;
                    this.artist = newArtist;
                }

                this.positionMs = this.mediaTimeToMillis(info.getPosition());
                this.durationMs = this.mediaTimeToMillis(info.getDuration());
                this.playing = info.isPlaying();
                this.source = this.safe(selected.getOwner());
                this.hasMedia = true;
                this.pollTime = System.currentTimeMillis();
                this.lastHeartbeat = this.pollTime;
                String newKey = newTitle + "|" + newArtist;
                if (!newKey.equals(this.currentThumbKey)) {
                    this.currentThumbKey = newKey;
                    byte[] artwork = info.getArtworkPng();
                    if (artwork != null && artwork.length > 100 && this.thumbFile != null) {
                        Files.write(this.thumbFile, artwork);
                    } else if (this.thumbFile != null) {
                        Files.deleteIfExists(this.thumbFile);
                    }

                    this.thumbnailUpdated = true;
                }
            }
        } else {
            this.activeNativeSession = null;
            this.noneCount++;
            if (this.noneCount >= 3) {
                this.hasMedia = false;
                this.title = "";
                this.artist = "";
                this.source = "";
                this.playing = false;
            }
        }
    }

    private IMediaSession selectSession(List<IMediaSession> sessions) {
        if (sessions != null && !sessions.isEmpty()) {
            IMediaSession best = null;
            int bestScore = Integer.MIN_VALUE;

            for (IMediaSession session : sessions) {
                if (session != null && session.getMedia() != null) {
                    MediaInfo info = session.getMedia();
                    String sessionTitle = this.safe(info.getTitle());
                    if (!sessionTitle.isBlank()) {
                        int score = 1;
                        if (info.isPlaying()) {
                            score += 1000;
                        }

                        if (!this.safe(info.getArtist()).isBlank()) {
                            score += 120;
                        }

                        if (info.getDuration() >= 30L) {
                            score += 60;
                        }

                        if (info.getDuration() >= 120L) {
                            score += 20;
                        }

                        String owner = this.safe(session.getOwner()).toLowerCase(Locale.ROOT);
                        if (owner.contains("spotify")
                            || owner.contains("music")
                            || owner.contains("soundcloud")
                            || owner.contains("yandex")
                            || owner.contains("youtube")) {
                            score += 80;
                        }

                        if (sessionTitle.matches("^\\(\\d+\\).+")) {
                            score -= 50;
                        }

                        if (score > bestScore) {
                            bestScore = score;
                            best = session;
                        }
                    }
                }
            }

            return best;
        } else {
            return null;
        }
    }

    private long mediaTimeToMillis(long value) {
        return value <= 0L ? 0L : value * 1000L;
    }

    private String safe(String value) {
        return value == null ? "" : value.strip();
    }

    private String cleanMetadata(String value) {
        String text = this.safe(value);
        if ((text.indexOf(1056) >= 0 || text.indexOf(1057) >= 0) && text.indexOf(65533) < 0) {
            try {
                String repaired = new String(text.getBytes("windows-1251"), StandardCharsets.UTF_8);
                if (repaired.indexOf(65533) < 0 && repaired.indexOf(63) < 0) {
                    return repaired;
                }
            } catch (Exception var4) {
            }
        }

        return text;
    }

    private void startProcess() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
            this.powershellExecutable(),
            "-NoProfile",
            "-NonInteractive",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            this.scriptFile.toAbsolutePath().toString(),
            this.thumbFile.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);
        this.process = pb.start();
    }

    private void readOutput() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.process.getInputStream(), StandardCharsets.UTF_8))) {
            String tempTitle = "";
            String tempArtist = "";
            String tempSource = "";
            long tempPos = 0L;
            long tempDur = 0L;
            boolean tempPlaying = false;
            boolean tempHasMedia = false;
            boolean thumbOk = false;

            String line;
            while (this.running && (line = reader.readLine()) != null) {
                this.lastHeartbeat = System.currentTimeMillis();
                line = line.trim();
                if (line.equals("---END---")) {
                    if (tempHasMedia) {
                        this.noneCount = 0;
                        String cleanTempTitle = this.cleanMetadata(tempTitle).replaceFirst("(?i)\\s*\\|\\s*soundcloud track\\s*$", "");
                        if (this.activeNativeSession == null) {
                            this.title = cleanTempTitle;
                            this.artist = this.cleanMetadata(tempArtist);
                            this.positionMs = tempPos;
                            this.durationMs = tempDur;
                            this.playing = tempPlaying;
                            this.source = tempSource;
                            this.hasMedia = true;
                            this.pollTime = System.currentTimeMillis();
                            this.powerShellMetadataAt = this.pollTime;
                        } else if (tempDur > 0L && cleanTempTitle.equalsIgnoreCase(this.title)) {
                            this.durationMs = tempDur;
                        }

                        String newKey = tempTitle + "|" + tempArtist;
                        if (thumbOk && !newKey.equals(this.currentThumbKey)) {
                            this.currentThumbKey = newKey;
                            this.thumbnailUpdated = true;
                        }
                    } else if (this.activeNativeSession == null) {
                        this.noneCount++;
                        if (this.noneCount >= 5) {
                            this.hasMedia = false;
                            this.title = "";
                            this.artist = "";
                        }
                    }

                    tempTitle = "";
                    tempArtist = "";
                    tempSource = "";
                    tempPos = 0L;
                    tempDur = 0L;
                    tempPlaying = false;
                    tempHasMedia = false;
                    thumbOk = false;
                } else if (line.equals("NONE")) {
                    tempHasMedia = false;
                } else if (line.startsWith("TITLE:")) {
                    tempTitle = line.substring(6);
                    tempHasMedia = true;
                } else if (line.startsWith("ARTIST:")) {
                    tempArtist = line.substring(7);
                } else if (line.startsWith("POSITION:")) {
                    tempPos = this.parseLong(line.substring(9));
                } else if (line.startsWith("DURATION:")) {
                    tempDur = this.parseLong(line.substring(9));
                } else if (line.startsWith("PLAYING:")) {
                    tempPlaying = line.substring(8).equalsIgnoreCase("True");
                } else if (line.startsWith("SOURCE:")) {
                    tempSource = line.substring(7);
                } else if (line.equals("THUMB:OK")) {
                    thumbOk = true;
                }
            }
        }
    }

    private long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException var3) {
            return 0L;
        }
    }

    public String getTitle() {
        return this.title;
    }

    public String getArtist() {
        return this.artist;
    }

    public synchronized long getPositionMs() {
        long candidate = this.immediatePositionMs();
        String currentTrack = this.title + "\n" + this.artist;
        long now = System.nanoTime();
        if (currentTrack.equals(this.stablePositionTrack) && this.stablePositionMs >= 0L) {
            long elapsed = Math.max(0L, (now - this.stablePositionReadNanos) / 1000000L);
            long expected = this.stablePositionMs + (this.playing ? elapsed : 0L);
            long error = candidate - expected;
            long corrected;
            if (Math.abs(error) > 1800L) {
                corrected = candidate;
            } else {
                long correctionLimit = Math.max(0L, Math.round(elapsed * 0.3));
                long wantedCorrection = Math.round(error * 0.08);
                long correction = Math.max(-correctionLimit, Math.min(correctionLimit, wantedCorrection));
                corrected = expected + correction;
            }

            this.stablePositionMs = Math.max(0L, this.durationMs > 0L ? Math.min(corrected, this.durationMs) : corrected);
            this.stablePositionReadNanos = now;
            return this.stablePositionMs;
        } else {
            this.stablePositionTrack = currentTrack;
            this.stablePositionMs = Math.max(0L, candidate);
            this.stablePositionReadNanos = now;
            return this.stablePositionMs;
        }
    }

    public synchronized long getImmediatePositionMs() {
        return this.immediatePositionMs();
    }

    private long immediatePositionMs() {
        long candidate = this.positionMs;
        if (this.playing && this.pollTime != 0L) {
            candidate += Math.max(0L, System.currentTimeMillis() - this.pollTime);
        }

        return Math.max(0L, this.durationMs > 0L ? Math.min(candidate, this.durationMs) : candidate);
    }

    public long getRawPositionMs() {
        return this.positionMs;
    }

    public long getDurationMs() {
        return this.durationMs;
    }

    public boolean isPlaying() {
        return this.playing;
    }

    public boolean hasMedia() {
        return this.hasMedia;
    }

    public boolean isRunning() {
        return this.running;
    }

    public String getSource() {
        return this.source;
    }

    public Path getThumbnailPath() {
        return this.thumbFile;
    }

    public boolean consumeThumbnailUpdate() {
        if (this.thumbnailUpdated) {
            this.thumbnailUpdated = false;
            return true;
        } else {
            return false;
        }
    }

    public void playPause() {
        IMediaSession session = this.activeNativeSession;
        if (session != null) {
            try {
                session.playPause();
                return;
            } catch (Throwable var3) {
            }
        }

        this.sendCommand("TryTogglePlayPauseAsync");
    }

    public void next() {
        IMediaSession session = this.activeNativeSession;
        if (session != null) {
            try {
                session.next();
                return;
            } catch (Throwable var3) {
            }
        }

        this.sendCommand("TrySkipNextAsync");
    }

    public void prev() {
        IMediaSession session = this.activeNativeSession;
        if (session != null) {
            try {
                session.previous();
                return;
            } catch (Throwable var3) {
            }
        }

        this.sendCommand("TrySkipPreviousAsync");
    }

    private void sendCommand(String method) {
        new Thread(
                () -> {
                    try {
                        String script = "$ErrorActionPreference='SilentlyContinue';Add-Type -AssemblyName System.Runtime.WindowsRuntime;$atm=([System.WindowsRuntimeSystemExtensions].GetMethods()|?{$_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'})[0];function WA($o,$t){$task=$atm.MakeGenericMethod($t).Invoke($null,@($o));if($task.Wait(2000)){$task.Result}else{$null}};[void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime];$mgr=WA ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]);$s=$null;$sessions=$mgr.GetSessions();if($sessions){ foreach($sess in $sessions){ if($sess.GetPlaybackInfo().PlaybackStatus.ToString() -eq 'Playing'){ $s=$sess; break } } };if(-not $s){ $s=$mgr.GetCurrentSession() };if(-not $s -and $sessions -and $sessions.Count -gt 0){ $s=$sessions[0] };if($s){ $s."
                            + method
                            + "() }";
                        ProcessBuilder pb = new ProcessBuilder(
                            this.powershellExecutable(), "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command", script
                        );
                        pb.start();
                    } catch (Exception var4) {
                    }
                }
            )
            .start();
    }

    private String powershellExecutable() {
        String root = System.getenv("SystemRoot");
        if (root == null || root.isBlank()) {
            root = "C:\\Windows";
        }

        Path executable = Path.of(root, "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
        return Files.isRegularFile(executable) ? executable.toString() : "powershell.exe";
    }

    private String buildScript() {
        return "$ErrorActionPreference='SilentlyContinue'\r\n[Console]::OutputEncoding=[System.Text.Encoding]::UTF8\r\ntry{\r\nAdd-Type -AssemblyName System.Runtime.WindowsRuntime\r\n$atm=([System.WindowsRuntimeSystemExtensions].GetMethods()|?{$_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'})[0]\r\nif(-not $atm){Write-Output 'ERROR:AsTask not found';Write-Output '---END---';exit 1}\r\nfunction WA($o,$t){$task=$atm.MakeGenericMethod($t).Invoke($null,@($o));if($task.Wait(2000)){$task.Result}else{$null}}\r\n[void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]\r\nfunction GetMgr { return WA ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]) }\r\n$mgr=GetMgr\r\n}catch{\r\nwhile($true){Write-Output 'ERROR:init';Write-Output '---END---';[Console]::Out.Flush();Start-Sleep -Seconds 5}\r\n}\r\n$tp=$args[0]\r\nwhile($true){\r\ntry{\r\n    if (-not $mgr) { $mgr = GetMgr }\r\n    $s=$null\r\n    if ($mgr) { $sessions=$mgr.GetSessions() }\r\n    if($sessions){\r\n        foreach($sess in $sessions){\r\n            $pb=$sess.GetPlaybackInfo()\r\n            if($pb -and $pb.PlaybackStatus.ToString() -eq 'Playing'){\r\n                $s=$sess\r\n                break\r\n            }\r\n        }\r\n    }\r\n    if(-not $s -and $mgr){ $s=$mgr.GetCurrentSession() }\r\n    if(-not $s -and $sessions -and $sessions.Count -gt 0){ $s=$sessions[0] }\r\n    if(-not $s) { $mgr = GetMgr } # Если сессия не найдена, принудительно обновляем менеджер\r\n\r\nif($s){\r\n$p=WA ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])\r\n$tl=$s.GetTimelineProperties()\r\n$pb=$s.GetPlaybackInfo()\r\n$isPlaying = $false\r\nif ($pb) { $isPlaying = ($pb.PlaybackStatus.ToString() -eq 'Playing') }\r\n$pos = 0\r\nif ($tl) {\r\n    $pos = $tl.Position.TotalMilliseconds\r\n    if ($isPlaying -and $tl.LastUpdatedTime) {\r\n        $diff = ([DateTimeOffset]::UtcNow - $tl.LastUpdatedTime).TotalMilliseconds\r\n        if ($diff -gt 0) { $pos += $diff }\r\n    }\r\n}\r\nWrite-Output \"TITLE:$($p.Title)\"\r\nWrite-Output \"ARTIST:$($p.Artist)\"\r\nWrite-Output \"POSITION:$([long]$pos)\"\r\nWrite-Output \"DURATION:$([long]$tl.EndTime.TotalMilliseconds)\"\r\nWrite-Output \"PLAYING:$isPlaying\"\r\nWrite-Output \"SOURCE:$($s.SourceAppUserModelId)\"\r\nif($tp -and $p.Thumbnail){\r\ntry{\r\n$st=WA ($p.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])\r\n$asm=[System.IO.WindowsRuntimeStreamExtensions].GetMethods()|?{$_.Name -eq 'AsStream' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IRandomAccessStream'}\r\n$ns=$asm.Invoke($null,@($st));$fs=[IO.File]::Create($tp);$ns.CopyTo($fs);$fs.Close();$ns.Close()\r\nWrite-Output 'THUMB:OK'\r\n}catch{Write-Output 'THUMB:NONE'}\r\n}else{Write-Output 'THUMB:NONE'}\r\n}else{Write-Output 'NONE'}\r\n}catch{Write-Output \"ERROR:$($_.Exception.Message)\"}\r\nWrite-Output '---END---'\r\n[Console]::Out.Flush()\r\nStart-Sleep -Milliseconds 800\r\n}\r\n";
    }
}
