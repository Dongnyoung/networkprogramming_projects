import javax.sound.sampled.*;
import java.io.File;

public class BgmPlayer {
    private Clip bgmClip;
    private long pausePosition = 0L;

    public synchronized void playLoop(String path, float volumeDb) {
        stopBgm();
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(path));
            bgmClip = AudioSystem.getClip();
            bgmClip.open(ais);
            setVolume(bgmClip, volumeDb);

            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();
            pausePosition = 0L;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void stopBgm() {
        try {
            if (bgmClip != null) {
                bgmClip.stop();
                bgmClip.close();
                bgmClip = null;
            }
        } catch (Exception ignore) {}
        pausePosition = 0L;
    }

    public synchronized void pauseBgm() {
        try {
            if (bgmClip != null && bgmClip.isRunning()) {
                pausePosition = bgmClip.getMicrosecondPosition();
                bgmClip.stop();
            }
        } catch (Exception ignore) {}
    }

    public synchronized void resumeBgm() {
        try {
            if (bgmClip != null && !bgmClip.isRunning()) {
                bgmClip.setMicrosecondPosition(pausePosition);
                bgmClip.start();
            }
        } catch (Exception ignore) {}
    }

    public void playSfxOnce(String path, float volumeDb, Runnable onDone) {
        new Thread(() -> {
            Clip sfx = null;
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(new File(path));
                sfx = AudioSystem.getClip();
                sfx.open(ais);
                setVolume(sfx, volumeDb);

                Clip finalSfx = sfx;
                sfx.addLineListener(ev -> {
                    if (ev.getType() == LineEvent.Type.STOP) {
                        try { finalSfx.close(); } catch (Exception ignore) {}
                        if (onDone != null) onDone.run();
                    }
                });

                sfx.start();
            } catch (Exception e) {
                if (sfx != null) try { sfx.close(); } catch (Exception ignore) {}
                if (onDone != null) onDone.run();
            }
        }, "SFX-Thread").start();
    }

    public synchronized boolean isPlaying() {
        return bgmClip != null && bgmClip.isRunning();
    }

    private void setVolume(Clip clip, float volumeDb) {
        try {
            if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(volumeDb);
            }
        } catch (Exception ignore) {}
    }
}
