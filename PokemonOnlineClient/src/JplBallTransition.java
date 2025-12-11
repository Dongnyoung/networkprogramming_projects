import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class JplBallTransition extends JPanel {

    private Image ballImg;
    private Image bgImg;
    
    // 1단계 변수
    private int bgX = -1024;  // bg 시작 위치
    private int ballX = -76;  // ball 시작 위치
    private int bgTargetX = 0;    // bg 목표 위치
    private int ballTargetX = 948; // ball 목표 위치
    
    // 2단계 변수
    private int bgX2 = 1024;  // 2단계 bg 시작 위치 (오른쪽 끝)
    private int ballX2 = 1024 - 76;  // 2단계 ball 시작 위치
    private int bgTargetX2 = 0;  // 2단계 bg 목표 위치
    private int ballTargetX2 = -76;  // 2단계 ball 목표 위치 (화면 왼쪽 밖)
    
    // 3단계 변수
    private int bgX3 = -1024;  // 3단계 bg 시작 위치 (왼쪽 밖)
    private int ballX3 = -76;  // 3단계 ball 시작 위치
    private int bgTargetX3 = 0;  // 3단계 bg 목표 위치
    private int ballTargetX3 = 948;  // 3단계 ball 목표 위치 (화면 오른쪽)
    
    // 4단계 변수 (1단계 볼이 중앙을 지날 때 시작)
    private int bgX4 = 1024;  // 4단계 bg 시작 위치 (오른쪽 끝)
    private int ballX4 = 1024 - 76;  // 4단계 ball 시작 위치
    private int bgTargetX4 = 0;  // 4단계 bg 목표 위치
    private int ballTargetX4 = -76;  // 4단계 ball 목표 위치 (화면 왼쪽 밖)
    private boolean phase4Started = false; // 4단계 시작 여부
    
    // 5단계 변수 (4단계 완료 후)
    private int bgX5 = -1024;  // 5단계 bg 시작 위치 (왼쪽 밖)
    private int ballX5 = -76;  // 5단계 ball 시작 위치
    private int bgTargetX5 = 0;  // 5단계 bg 목표 위치
    private int ballTargetX5 = 948;  // 5단계 ball 목표 위치 (화면 오른쪽)
    private boolean phase5Started = false; // 5단계 시작 여부
    
    private int phase = 0;  // 0: 1단계, 1: 2단계, 2: 3단계, 3: 대기, 4: 종료
    private int holdTicks = 20; // 멈춘 상태로 몇 틱 유지할지
    private double ballRotation = 0; // 볼 회전 각도 (라디안)
    private double ballRotation2 = 0; // 2단계 볼 회전 각도
    private double ballRotation3 = 0; // 3단계 볼 회전 각도
    private double ballRotation4 = 0; // 4단계 볼 회전 각도
    private double ballRotation5 = 0; // 5단계 볼 회전 각도
    private boolean ballVisible = true; // 1단계 볼 표시 여부
    private boolean ballVisible2 = true; // 2단계 볼 표시 여부
    private boolean ballVisible3 = true; // 3단계 볼 표시 여부
    private boolean ballVisible4 = true; // 4단계 볼 표시 여부
    private boolean ballVisible5 = true; // 5단계 볼 표시 여부
    private Timer timer;
    private Runnable onTransitionEnd; // 연출 끝났을 때 호출할 콜백

    public JplBallTransition(Runnable onTransitionEnd) {
        this.onTransitionEnd = onTransitionEnd;
        setOpaque(false); // 투명하게 설정

        // 배경 이미지 로드
        try {
            bgImg = ImageIO.read(new File("Images/motion_bg.png"));
        } catch (Exception e) {
            System.err.println("Failed to load motion_bg.png");
            e.printStackTrace();
        }

        // 몬스터볼 이미지 로드
        try {
            ballImg = ImageIO.read(new File("Images/motion_ball.png"));
        } catch (Exception e) {
            System.err.println("Failed to load motion_ball.png");
            e.printStackTrace();
        }

        int delay = 7; // 약 60fps (16ms마다 갱신)
        int moveSpeed = 20; // 한 프레임당 이동 픽셀
        
        timer = new Timer(delay, ev -> {
            if (phase == 0) { // 1단계: 왼쪽에서 오른쪽으로 (y=0)
                // bg를 오른쪽으로 이동
                if (bgX < bgTargetX) {
                    bgX += moveSpeed;
                    if (bgX > bgTargetX) bgX = bgTargetX;
                }
                
                // ball을 오른쪽으로 이동
                if (ballX < ballTargetX) {
                    ballX += moveSpeed;
                    if (ballX > ballTargetX) ballX = ballTargetX;
                    
                    // 볼 회전 (시계방향)
                    ballRotation += Math.toRadians(10); // 프레임당 10도 회전
                }
                
                // 1단계 볼이 중앙(화면 절반 정도)을 지날 때 4단계 시작
                if (ballX >= 512 && !phase4Started) {
                    phase4Started = true;
                    ballVisible4 = true;
                }
                
                // 목표 위치에 도달하면 볼 숨기기
                if (ballX >= ballTargetX) {
                    ballVisible = false;
                }
                
                // 1단계 완료 -> 2단계로 전환
                if (bgX >= bgTargetX && ballX >= ballTargetX) {
                    phase = 1;
                    ballVisible2 = true; // 2단계 볼 보이기
                }
            } else if (phase == 1) { // 2단계: 오른쪽에서 왼쪽으로 (y=118)
                // bg를 왼쪽으로 이동
                if (bgX2 > bgTargetX2) {
                    bgX2 -= moveSpeed;
                    if (bgX2 < bgTargetX2) bgX2 = bgTargetX2;
                }
                
                // ball을 왼쪽으로 이동
                if (ballX2 > ballTargetX2) {
                    ballX2 -= moveSpeed;
                    if (ballX2 < ballTargetX2) ballX2 = ballTargetX2;
                    
                    // 볼 회전 (시계방향)
                    ballRotation2 += Math.toRadians(-15);
                }
                
                // 목표 위치에 도달하면 볼 숨기기
                if (ballX2 <= ballTargetX2) {
                    ballVisible2 = false;
                }
                
                // 2단계 완료 -> 3단계로 전환
                if (bgX2 <= bgTargetX2 && ballX2 <= ballTargetX2) {
                    phase = 2;
                    ballVisible3 = true; // 3단계 볼 보이기
                }
            } else if (phase == 2) { // 3단계: 왼쪽에서 오른쪽으로 (y=231)
                // bg를 오른쪽으로 이동
                if (bgX3 < bgTargetX3) {
                    bgX3 += moveSpeed;
                    if (bgX3 > bgTargetX3) bgX3 = bgTargetX3;
                }
                
                // ball을 오른쪽으로 이동
                if (ballX3 < ballTargetX3) {
                    ballX3 += moveSpeed;
                    if (ballX3 > ballTargetX3) ballX3 = ballTargetX3;
                    
                    // 볼 회전 (시계방향)
                    ballRotation3 += Math.toRadians(15);
                }
                
                // 목표 위치에 도달하면 볼 숨기기
                if (ballX3 >= ballTargetX3) {
                    ballVisible3 = false;
                }
                
                // 3단계 완료 -> 대기 단계로
                if (bgX3 >= bgTargetX3 && ballX3 >= ballTargetX3) {
                    phase = 3;
                }
            }
            
            // 4단계: 오른쪽에서 왼쪽으로 (y=463) - 1단계 볼이 중앙 지날 때부터 시작
            if (phase4Started && phase < 3) {
                // bg를 왼쪽으로 이동
                if (bgX4 > bgTargetX4) {
                    bgX4 -= moveSpeed;
                    if (bgX4 < bgTargetX4) bgX4 = bgTargetX4;
                }
                
                // ball을 왼쪽으로 이동
                if (ballX4 > ballTargetX4) {
                    ballX4 -= moveSpeed;
                    if (ballX4 < ballTargetX4) ballX4 = ballTargetX4;
                    
                    // 볼 회전 (시계방향)
                    ballRotation4 += Math.toRadians(-15);
                }
                
                // 목표 위치에 도달하면 볼 숨기기
                if (ballX4 <= ballTargetX4) {
                    ballVisible4 = false;
                }
                
                // 4단계 완료 -> 5단계 시작
                if (bgX4 <= bgTargetX4 && ballX4 <= ballTargetX4 && !phase5Started) {
                    phase5Started = true;
                    ballVisible5 = true;
                }
            }
            
            // 5단계: 왼쪽에서 오른쪽으로 (y=347) - 4단계 완료 후 시작
            if (phase5Started && phase < 3) {
                // bg를 오른쪽으로 이동
                if (bgX5 < bgTargetX5) {
                    bgX5 += moveSpeed;
                    if (bgX5 > bgTargetX5) bgX5 = bgTargetX5;
                }
                
                // ball을 오른쪽으로 이동
                if (ballX5 < ballTargetX5) {
                    ballX5 += moveSpeed;
                    if (ballX5 > ballTargetX5) ballX5 = ballTargetX5;
                    
                    // 볼 회전 (시계방향)
                    ballRotation5 += Math.toRadians(15);
                }
                
                // 목표 위치에 도달하면 볼 숨기기
                if (ballX5 >= ballTargetX5) {
                    ballVisible5 = false;
                }
            }
            
            if (phase == 3) { // 멈춘 상태 유지
                holdTicks--;
                if (holdTicks <= 0) {
                    timer.stop();
                    if (onTransitionEnd != null) {
                        onTransitionEnd.run();
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

        int panelW = getWidth();
        int panelH = getHeight();

        // 1단계 bg 이미지 그리기 (y=0) - 항상 표시
        if (bgImg != null) {
            g.drawImage(bgImg, bgX, -1, null);
        }

        // 1단계 ball 이미지 그리기 (y=1)
        if (ballImg != null && ballVisible) {
            Graphics2D g2d = (Graphics2D) g.create();
            
            int ballW = ballImg.getWidth(null);
            int ballH = ballImg.getHeight(null);
            
            int centerX = ballX + ballW / 2;
            int centerY = 1 +ballH / 2;
            
            g2d.rotate(ballRotation, centerX, centerY);
            g2d.drawImage(ballImg, ballX, 1, ballW, ballH, null);
            
            g2d.dispose();
        }

        // 2단계 bg 이미지 그리기 (y=153)
        if (bgImg != null && phase >= 1) {
            g.drawImage(bgImg, bgX2, 153, null);
        }

        // 2단계 ball 이미지 그리기 (y=155)
        if (ballImg != null && ballVisible2 && phase >= 1) {
            Graphics2D g2d = (Graphics2D) g.create();
            
            int ballW = ballImg.getWidth(null);
            int ballH = ballImg.getHeight(null);
            
            int centerX = ballX2 + ballW / 2;
            int centerY = 155 + ballH / 2;
            
            g2d.rotate(ballRotation2, centerX, centerY);
            g2d.drawImage(ballImg, ballX2, 155, ballW, ballH, null);
            
            g2d.dispose();
        }

        // 3단계 bg 이미지 그리기 (y=307)
        if (bgImg != null && phase >= 2) {
            g.drawImage(bgImg, bgX3, 307, null);
        }

        // 3단계 ball 이미지 그리기 (y=309)
        if (ballImg != null && ballVisible3 && phase >= 2) {
            Graphics2D g2d = (Graphics2D) g.create();
            
            int ballW = ballImg.getWidth(null);
            int ballH = ballImg.getHeight(null);
            
            int centerX = ballX3 + ballW / 2;
            int centerY = 309 + ballH / 2;
            
            g2d.rotate(ballRotation3, centerX, centerY);
            g2d.drawImage(ballImg, ballX3, 309, ballW, ballH, null);
            
            g2d.dispose();
        }

        // 4단계 bg 이미지 그리기 (y=615)
        if (bgImg != null && phase4Started) {
            g.drawImage(bgImg, bgX4, 615, null);
        }

        // 4단계 ball 이미지 그리기 (y=617)
        if (ballImg != null && ballVisible4 && phase4Started) {
            Graphics2D g2d = (Graphics2D) g.create();
            
            int ballW = ballImg.getWidth(null);
            int ballH = ballImg.getHeight(null);
            
            int centerX = ballX4 + ballW / 2;
            int centerY = 617 + ballH / 2;
            
            g2d.rotate(ballRotation4, centerX, centerY);
            g2d.drawImage(ballImg, ballX4, 617, ballW, ballH, null);
            
            g2d.dispose();
        }

        // 5단계 bg 이미지 그리기 (y=461)
        if (bgImg != null && phase5Started) {
            g.drawImage(bgImg, bgX5, 461, null);
        }

        // 5단계 ball 이미지 그리기 (y=463)
        if (ballImg != null && ballVisible5 && phase5Started) {
            Graphics2D g2d = (Graphics2D) g.create();
            
            int ballW = ballImg.getWidth(null);
            int ballH = ballImg.getHeight(null);
            
            int centerX = ballX5 + ballW / 2;
            int centerY = 463 + ballH / 2;
            
            g2d.rotate(ballRotation5, centerX, centerY);
            g2d.drawImage(ballImg, ballX5, 463, ballW, ballH, null);
            
            g2d.dispose();
        }
    }
}
