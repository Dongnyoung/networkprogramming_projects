import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class JplBallTransition extends JPanel {

    private Image ballImg;
    private int cols = 10;  // 가로 몇 칸
    private int rows = 6;   // 세로 몇 칸
    private int visibleCount = 0;  // 현재 화면에 보이는 볼 개수
    private int phase = 0;  // 0: 채우는 중, 1: 꽉 찬 상태 유지, 2: 하나씩 사라짐
    private int holdTicks = 20; // 꽉 찬 상태로 몇 틱 유지할지
    private Timer timer;
    private Runnable onTransitionEnd; // 연출 끝났을 때 호출할 콜백

    public JplBallTransition(Runnable onTransitionEnd) {
        this.onTransitionEnd = onTransitionEnd;
        setBackground(Color.BLACK);

        // 몬스터볼 이미지 로드 (네 sprite 경로에 맞춰 수정)
        try {
            BufferedImage sprite = ImageIO.read(new File("Images/ball.png"));
            // ball.png가 가로로 여러 프레임일 수 있으니 첫 프레임만 사용
            int fw = sprite.getWidth() / 4; // 4프레임 가정
            int fh = sprite.getHeight();
            ballImg = sprite.getSubimage(0, 0, fw, fh);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int delay = 40; // 40ms마다 한 번씩 그림 갱신 (부드럽게)
        timer = new Timer(delay, ev -> {
            int total = cols * rows;

            if (phase == 0) { // 채우는 단계
                visibleCount++;
                if (visibleCount >= total) {
                    visibleCount = total;
                    phase = 1; // 꽉 찼으니 hold 단계로
                }
            } else if (phase == 1) { // 꽉 찬 상태 유지
                holdTicks--;
                if (holdTicks <= 0) {
                    phase = 2;
                }
            } else if (phase == 2) { // 하나씩 사라지는 단계
                visibleCount--;
                if (visibleCount <= 0) {
                    visibleCount = 0;
                    timer.stop();
                    if (onTransitionEnd != null) {
                        onTransitionEnd.run(); // 연출 끝 -> 배틀 패널로
                    }
                }
            }

            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (ballImg == null) return;

        int panelW = getWidth();
        int panelH = getHeight();

        int cellW = panelW / cols;
        int cellH = panelH / rows;

        // 볼 크기: 셀보다 약간 작게
        int ballW = (int) (cellW * 0.9);
        int ballH = (int) (cellH * 0.9);

        Image scaled = ballImg.getScaledInstance(ballW, ballH, Image.SCALE_SMOOTH);

        for (int i = 0; i < visibleCount; i++) {
            int r = i / cols;
            int c = i % cols;

            int x = c * cellW + (cellW - ballW) / 2;
            int y = r * cellH + (cellH - ballH) / 2;

            g.drawImage(scaled, x, y, null);
        }
    }
}
