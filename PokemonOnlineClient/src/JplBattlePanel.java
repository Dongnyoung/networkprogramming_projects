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

    // 내 포켓몬(뒷모습) 이미지
    private Image myBackImg;
    private int myImgW = 220;
    private int myImgH = 220;

    // 상대 포켓몬(앞모습) 이미지
    private Image enemyFrontImg;
    private int enemyImgW = 220;
    private int enemyImgH = 220;

    // 현재 좌표 & 타겟 좌표
    private int myX, myY;
    private int myTargetX, myTargetY;

    private int enemyX, enemyY;
    private int enemyTargetX, enemyTargetY;

    private Timer entryTimer;
    private boolean animMy = false;
    private boolean animEnemy = false;

    public JplBattlePanel(Pokemon myPokemon,
    					  Pokemon enemyPokemon,
                          String opponentName,
                          Socket socket,
                          DataInputStream dis,
                          DataOutputStream dos) {

        this.myPokemon = myPokemon;
        this.enemyPokemon = enemyPokemon;
        this.opponentName = opponentName;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;

        setBackground(Color.BLACK);
        setLayout(null);          // 나중에 스킬 버튼 깔기 편하게
        setDoubleBuffered(true);

        loadBackground();
        loadMyPokemonImage();
        initMyPositionAndStartAnim();
        
        // 상대 포켓몬은 아직 모를 수 있으니 여기서는 안 건드림
        // 나중에 서버에서 id 받으면 setEnemyPokemon() 호출
        if (this.enemyPokemon != null) {
            loadEnemyPokemonImage();
            initEnemyPositionAndStartAnim();
        }
    }

    private void loadBackground() {
        try {
            int n = 1 + (int)(Math.random() * 4);  // 1~4 랜덤
            String path = "Images/bg_arena" + n + ".png";

            bg = new ImageIcon(path).getImage();
        } catch (Exception e) {
            bg = null;
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
        myTargetY = PANEL_H - myImgH - 210;

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
        enemyTargetY = 80;

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
                // 여기서 스킬 버튼 활성화 같은 “전투 준비 완료” 처리
                
            }

            repaint();
        });

        entryTimer.start();
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

        // 이름 / HP 바 표시 (대충 감성만)
        drawStatusBars((Graphics2D) g);
    }

    private void drawStatusBars(Graphics2D g2) {

        if (myPokemon != null) {
            int barW = 260;
            int barH = 20;

            //  내 포켓몬 오른쪽에 배치
            int barX = myX + myImgW + 150;
            int barY = myY;   // 살짝 위로

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("PF Stardust Bold", Font.BOLD, 20));
            g2.drawString(myPokemon.getKoreanName() + " Lv." + myPokemon.getLevel(),
                          barX, barY - 8);

            int maxHp = myPokemon.calcMaxHp();
            int curHp = myPokemon.getCurrentHp();
            double ratio = maxHp == 0 ? 0 : (double) curHp / maxHp;

            g2.setColor(Color.DARK_GRAY);
            g2.fillRoundRect(barX, barY, barW, barH, 10, 10);

            int hpW = (int)(barW * ratio);
            g2.setColor(new Color(80, 220, 80));
            g2.fillRoundRect(barX, barY, hpW, barH, 10, 10);
        }

        
        if (enemyPokemon != null) {
            int barW = 260;
            int barH = 20;

            // 상대 포켓몬 왼쪽에 배치
            int barX = enemyX - barW - 50;
            int barY = enemyY + 20;

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("PF Stardust Bold", Font.BOLD, 20));
            g2.drawString(enemyPokemon.getKoreanName() + " Lv." + enemyPokemon.getLevel(),
                          barX, barY - 8);

            int maxHp = enemyPokemon.calcMaxHp();
            int curHp = enemyPokemon.getCurrentHp();
            double ratio = maxHp == 0 ? 0 : (double) curHp / maxHp;

            g2.setColor(Color.DARK_GRAY);
            g2.fillRoundRect(barX, barY, barW, barH, 10, 10);

            int hpW = (int)(barW * ratio);
            g2.setColor(new Color(220, 80, 80));
            g2.fillRoundRect(barX, barY, hpW, barH, 10, 10);
        }
    }



    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PANEL_W, PANEL_H);
    }
}
