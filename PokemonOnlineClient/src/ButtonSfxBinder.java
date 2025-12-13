import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public final class ButtonSfxBinder {
    private ButtonSfxBinder(){}

    public static void bindAllButtons(Container root, BgmPlayer player, String sfxPath, float volDb) {
        for (Component c : root.getComponents()) {
            if (c instanceof AbstractButton btn) {
                // 중복 방지용 플래그
                if (Boolean.TRUE.equals(btn.getClientProperty("SFX_BOUND"))) continue;
                btn.putClientProperty("SFX_BOUND", true);

                btn.addActionListener(e -> {
                    player.playSfxOnce(sfxPath, volDb, null);
                });
            }
            if (c instanceof Container child) {
                bindAllButtons(child, player, sfxPath, volDb);
            }
        }
    }
}
