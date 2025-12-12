import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class JplBattlePanel extends JPanel {

    private static final int PANEL_W = 1024;
    private static final int PANEL_H = 768;

    private final Pokemon myPokemon;
    private Pokemon enemyPokemon;           // 처음엔 null일 수 있음
    private final String opponentName;
    private final Socket socket;
    private final DataInputStream dis;
    private final DataOutputStream dos;

    private Image bg;

    // 정보창 이미지
    private Image infoMeImg;
    private Image infoOpponentImg;

    // 내 포켓몬(뒷모습) 이미지
    private Image myBackImg;
    private int myImgW = 256;
    private int myImgH = 256;

    // 상대 포켓몬(앞모습) 이미지
    private Image enemyFrontImg;
    private int enemyImgW = 256;
    private int enemyImgH = 256;

    // 현재 좌표 & 타겟 좌표
    private int myX, myY;
    private int myTargetX, myTargetY;

    private int enemyX, enemyY;
    private int enemyTargetX, enemyTargetY;

    private Timer entryTimer;
    private boolean animMy = false;
    private boolean animEnemy = false;

    // 체력바 애니메이션 변수
    private double myHpRatio = 0.0;  // 현재 표시 중인 HP 비율 (0.0 ~ 1.0)
    private double enemyHpRatio = 0.0;
    private Timer hpBarTimer;
    private boolean hpBarAnimStarted = false;

    // 커튼 효과 변수
    private Image curtainBgImg;
    private int curtainTopY = 0;  // 상단 커튼 Y 위치
    private int curtainBottomY = 384;  // 하단 커튼 Y 위치
    private int curtainTargetTopY = -385;  // 상단 커튼 목표 위치
    private int curtainTargetBottomY = 768;  // 하단 커튼 목표 위치
    private boolean curtainActive = true; // 커튼 효과 활성 여부
    private Timer curtainTimer;

    public JplBattlePanel(Pokemon myPokemon,
    					  Pokemon enemyPokemon,
                          String opponentName,
                          Socket socket,
                          DataInputStream dis,
                          DataOutputStream dos,
                          int backgroundNumber) {

        this.myPokemon = myPokemon;
        this.enemyPokemon = enemyPokemon;
        this.opponentName = opponentName;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;

        setBackground(Color.BLACK);
        setLayout(null);          // 나중에 스킬 버튼 깔기 편하게
        setDoubleBuffered(true);

        loadBackground(backgroundNumber);
        loadCurtainImage();
        loadInfoImages();
        loadMyPokemonImage();
        initMyPositionAndStartAnim();
        
        // 커튼 효과 시작
        startCurtainEffect();
        
        // 상대 포켓몬은 아직 모를 수 있으니 여기서는 안 건드림
        // 나중에 서버에서 id 받으면 setEnemyPokemon() 호출
        if (this.enemyPokemon != null) {
            loadEnemyPokemonImage();
            initEnemyPositionAndStartAnim();
        }
    }

    private void loadBackground(int bgNumber) {
        try {
            String path = "Images/bg_arena" + bgNumber + ".png";
            bg = new ImageIcon(path).getImage();
        } catch (Exception e) {
            System.err.println("Failed to load background: bg_arena" + bgNumber + ".png");
            bg = null;
        }
    }

    private void loadCurtainImage() {
        try {
            curtainBgImg = new ImageIcon("Images/motion_bg.png").getImage();
        } catch (Exception e) {
            System.err.println("Failed to load motion_bg.png for curtain");
            e.printStackTrace();
        }
    }

    private void loadInfoImages() {
        try {
            infoMeImg = new ImageIcon("Images/info_me.png").getImage();
        } catch (Exception e) {
            System.err.println("Failed to load info_me.png");
            e.printStackTrace();
        }

        try {
            infoOpponentImg = new ImageIcon("Images/info_opponent.png").getImage();
        } catch (Exception e) {
            System.err.println("Failed to load info_opponent.png");
            e.printStackTrace();
        }
    }

    private void loadMyPokemonImage() {
        if (myPokemon == null) return;
        try {
            String back = myPokemon.getBackImageFile();   // 뒷모습 스프라이트
            if (back != null) {
                ImageIcon raw = new ImageIcon(back);
                myBackImg = raw.getImage().getScaledInstance(myImgW, myImgH, Image.SCALE_SMOOTH);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadEnemyPokemonImage() {
        if (enemyPokemon == null) return;
        try {
            String front = enemyPokemon.getFrontImageFile(); // 앞모습 스프라이트
            if (front != null) {
                ImageIcon raw = new ImageIcon(front);
                enemyFrontImg = raw.getImage().getScaledInstance(enemyImgW, enemyImgH, Image.SCALE_SMOOTH);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initMyPositionAndStartAnim() {
        if (myBackImg == null) return;

        // 아래 왼쪽 근처가 타겟
        myTargetX =120;
        myTargetY = PANEL_H - myImgH - 136;

        // 시작 위치: 화면 "오른쪽" 밖
        myX = PANEL_W+20;          // 또는 PANEL_W + 100 정도
        myY = myTargetY;

        animMy = true;

        startEntryTimerIfNeeded();
    }

    private void initEnemyPositionAndStartAnim() {
        if (enemyFrontImg == null) return;

        // 위 오른쪽 근처가 타겟
        enemyTargetX = PANEL_W - enemyImgW - 160;
        enemyTargetY = 120;

        // 시작 위치: 화면 "왼쪽" 밖
        enemyX = -enemyImgW;    // 또는 -200 같은 값
        enemyY = enemyTargetY;

        animEnemy = true;

        startEntryTimerIfNeeded();
    }

    private void startEntryTimerIfNeeded() {
        if (entryTimer != null && entryTimer.isRunning()) return;

        int delay = 16;   // ~60fps
        int speed = 20;   // 한 프레임 이동량

        entryTimer = new Timer(delay, e -> {
            boolean doneMy = !animMy;
            boolean doneEnemy = !animEnemy;

            // 내 포켓몬 슬라이드
            if (animMy && myBackImg != null) {
                if (myX > myTargetX) {
                    myX -= speed;
                    if (myX < myTargetX) myX = myTargetX;
                }
                if (myX == myTargetX) {
                    animMy = false;
                    doneMy = true;
                }
            }

            // 상대 포켓몬 슬라이드
            if (animEnemy && enemyFrontImg != null) {
                if (enemyX < enemyTargetX) {
                    enemyX += speed;
                    if (enemyX > enemyTargetX) enemyX = enemyTargetX;
                }
                if (enemyX == enemyTargetX) {
                    animEnemy = false;
                    doneEnemy = true;
                }
            }

            if (doneMy && doneEnemy) {
                entryTimer.stop();
                // 포켓몬 이동 완료 후 체력바 애니메이션 시작
                startHpBarAnimation();
            }

            repaint();
        });

        entryTimer.start();
    }

    private void startHpBarAnimation() {
        if (hpBarAnimStarted) return;
        hpBarAnimStarted = true;

        int delay = 16;  // ~60fps
        final double[] progress = {0.0};  // 진행도 0.0 ~ 1.0

        hpBarTimer = new Timer(delay, e -> {
            boolean myComplete = false;
            boolean enemyComplete = false;

            // 진행도 증가 (제곱 계산으로 처음 빠르다가 점점 느려짐)
            progress[0] += 0.02;
            if (progress[0] > 1.0) progress[0] = 1.0;
            
            // ease-out quadratic: progress^2 형태로 처음 빠르다가 점점 느려짐
            double easedProgress = 1.0 - Math.pow(1.0 - progress[0], 2);

            // 내 포켓몬 HP 바 증가
            if (myPokemon != null) {
                int maxHp = myPokemon.calcMaxHp();
                int curHp = myPokemon.getCurrentHp();
                double targetRatio = maxHp == 0 ? 0 : (double) curHp / maxHp;

                myHpRatio = easedProgress * targetRatio;
                
                if (progress[0] >= 1.0) {
                    myHpRatio = targetRatio;
                    myComplete = true;
                } else {
                    myComplete = false;
                }
            } else {
                myComplete = true;
            }

            // 상대 포켓몬 HP 바 증가
            if (enemyPokemon != null) {
                int maxHp = enemyPokemon.calcMaxHp();
                int curHp = enemyPokemon.getCurrentHp();
                double targetRatio = maxHp == 0 ? 0 : (double) curHp / maxHp;

                enemyHpRatio = easedProgress * targetRatio;
                
                if (progress[0] >= 1.0) {
                    enemyHpRatio = targetRatio;
                    enemyComplete = true;
                } else {
                    enemyComplete = false;
                }
            } else {
                enemyComplete = true;
            }

            // 둘 다 완료되면 타이머 정지
            if (myComplete && enemyComplete) {
                hpBarTimer.stop();
            }

            repaint();
        });

        hpBarTimer.start();
    }

    private void startCurtainEffect() {
        int delay = 8;   // ~60fps
        int speed = 7;   // 한 프레임 이동량

        curtainTimer = new Timer(delay, e -> {
            boolean curtainComplete = true;

            // 상단 커튼 위로 이동
            if (curtainTopY > curtainTargetTopY) {
                curtainTopY -= speed;
                if (curtainTopY < curtainTargetTopY) curtainTopY = curtainTargetTopY;
                curtainComplete = false;
            }

            // 하단 커튼 아래로 이동
            if (curtainBottomY < curtainTargetBottomY) {
                curtainBottomY += speed;
                if (curtainBottomY > curtainTargetBottomY) curtainBottomY = curtainTargetBottomY;
                curtainComplete = false;
            }

            // 커튼 효과 완료
            if (curtainComplete) {
                curtainActive = false;
                curtainTimer.stop();
            }

            repaint();
        });

        curtainTimer.start();
    }

    /**
     * 서버에서 상대 포켓몬 id를 받은 후,
     * FrmSeverConnect 에서 enemyPokemon 만들어서 호출해줄 메소드.
     */
    public void setEnemyPokemon(Pokemon enemyPokemon) {
        this.enemyPokemon = enemyPokemon;
        loadEnemyPokemonImage();
        initEnemyPositionAndStartAnim();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 배경
        if (bg != null) {
            g.drawImage(bg, 0, 0, PANEL_W, PANEL_H, this);
        } else {
            g.setColor(new Color(10, 20, 40));
            g.fillRect(0, 0, PANEL_W, PANEL_H);
        }

        // 내 포켓몬(뒷모습)
        if (myBackImg != null) {
            g.drawImage(myBackImg, myX, myY, this);
        }

        // 상대 포켓몬(앞모습)
        if (enemyFrontImg != null) {
            g.drawImage(enemyFrontImg, enemyX, enemyY, this);
        }

        // 정보창 이미지 표시
        drawInfoContainers(g);

        // 커튼 효과: 상단과 하단에서 motion_bg 이미지 그리기
        if (curtainActive && curtainBgImg != null) {
            // 상단 커튼 (0, curtainTopY)
            g.drawImage(curtainBgImg, 0, curtainTopY, 1024, 385, this);
            
            // 하단 커튼 (0, curtainBottomY)
            g.drawImage(curtainBgImg, 0, curtainBottomY, 1024, 385, this);
        }
    }

    private void drawInfoContainers(Graphics g) {
        // 내 포켓몬 정보창 (오른쪽에 배치)
        if (myPokemon != null && infoMeImg != null) {
            int infoX = myX + myImgW + 90;
            int infoY = myY + 20;
            g.drawImage(infoMeImg, infoX, infoY, this);
            
            int infoW = infoMeImg.getWidth(this);
            
            // 포켓몬 이름 라벨 (이미지 내부 위치) - 그림자
            g.setColor(new Color(212,204,171));
            g.setFont(new Font("PF Stardust Bold", Font.PLAIN, 36));
            g.drawString(myPokemon.getKoreanName(), infoX + 57 + 2, infoY + 53 + 2);
            // 포켓몬 이름 라벨 - 실제 텍스트
            g.setColor(Color.BLACK);
            g.setFont(new Font("PF Stardust Bold", Font.PLAIN, 36));
            g.drawString(myPokemon.getKoreanName(), infoX + 57, infoY + 53);
            
            // 포켓몬 레벨 라벨 (정보창 이미지 오른쪽) - 그림자
            String levelText = "Lv" + myPokemon.getLevel();
            g.setColor(new Color(212,204,171));
            g.setFont(new Font("PF Stardust Bold", Font.PLAIN, 36));
            g.drawString(levelText, infoX + infoW - 140 + 2, infoY + 53 + 2);
            // 포켓몬 레벨 라벨 - 실제 텍스트
            g.setColor(Color.BLACK);
            g.setFont(new Font("PF Stardust Bold", Font.PLAIN, 36));
            g.drawString(levelText, infoX + infoW - 140, infoY + 53);
            
            // 체력바 (정보창 내부 172, 80 위치)
            int barX = infoX + 273;
            int barY = infoY + 80;
            int barW = 192;
            int barH = 8;
            
            // 현재 HP (초록색) - 애니메이션된 비율 사용
            int hpW = (int)(barW * myHpRatio);
            g.setColor(new Color(80, 220, 80));
            g.fillRect(barX, barY, hpW, barH);
            
            // HP 수치 표시 (현재체력 / 최대체력) - 오른쪽 정렬
            String hpText = myPokemon.getCurrentHp() + " / " + myPokemon.getMaxHp();
            g.setFont(new Font("PF Stardust Bold", Font.PLAIN, 36));
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(hpText);
            // 그림자
            g.setColor(new Color(212, 204, 171));
            g.drawString(hpText, infoX + 470 - textWidth + 2, infoY + 133 + 2);
            // 실제 텍스트
            g.setColor(Color.BLACK);
            g.drawString(hpText, infoX + 470 - textWidth, infoY + 133);
        }

        // 상대 포켓몬 정보창 (왼쪽에 배치)
        if (enemyPokemon != null && infoOpponentImg != null) {
            int infoW = infoOpponentImg.getWidth(this);
            int infoX = enemyX - infoW - 50;
            int infoY = enemyY - 50;
            g.drawImage(infoOpponentImg, infoX, infoY, this);
            
            // 포켓몬 이름 라벨 (이미지 내부 위치) - 그림자
            g.setColor(new Color(212,204,171));
            g.setFont(new Font("PF Stardust Bold", Font.PLAIN, 36));
            g.drawString(enemyPokemon.getKoreanName(), infoX + 25 + 2, infoY + 50 + 2);
            // 포켓몬 이름 라벨 - 실제 텍스트
            g.setColor(Color.BLACK);
            g.setFont(new Font("PF Stardust Bold", Font.PLAIN, 36));
            g.drawString(enemyPokemon.getKoreanName(), infoX + 25, infoY + 50);
            
            // 포켓몬 레벨 라벨 (정보창 이미지 오른쪽) - 그림자
            String levelText = "Lv" + enemyPokemon.getLevel();
            g.setColor(new Color(212,204,171));
            g.setFont(new Font("PF Stardust Bold", Font.PLAIN, 36));
            g.drawString(levelText, infoX + infoW - 155 + 2, infoY + 50 + 2);
            // 포켓몬 레벨 라벨 - 실제 텍스트
            g.setColor(Color.BLACK);
            g.setFont(new Font("PF Stardust Bold", Font.PLAIN, 36));
            g.drawString(levelText, infoX + infoW - 155, infoY + 50);
            
            // 체력바 (정보창 내부 236, 80 위치)
            int barX = infoX + 237;
            int barY = infoY + 80;
            int barW = 192;
            int barH = 8;
            
            // 현재 HP (초록색) - 애니메이션된 비율 사용
            int hpW = (int)(barW * enemyHpRatio);
            g.setColor(new Color(80, 220, 80));
            g.fillRect(barX, barY, hpW, barH);
        }
    }



    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PANEL_W, PANEL_H);
    }
}
