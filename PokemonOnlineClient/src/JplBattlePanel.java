import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;

public class JplBattlePanel extends JPanel {

    private static final int PANEL_W = 1024;
    private static final int PANEL_H = 768;

    private final Pokemon myPokemon;
    private Pokemon enemyPokemon;
    private final String opponentName;
    private final Socket socket;
    private final DataInputStream dis;
    private final DataOutputStream dos;

    private Image spriteSheet;
    private ImageIcon[] chatFrames;          // 원본 프레임(회전 전)
    private Timer chatHoverTimer;
    private int chatFrameIndex = 0;
    private Rectangle chatBtnBaseBounds;

    private Timer chatEntryTimer;
    private boolean chatBtnEntering = false;
    private int chatBtnX, chatBtnY;
    private final int chatBtnW = 50, chatBtnH = 50;
    private int chatBtnTargetX = 10;
    private int chatBtnTargetY = 130;

    private double chatAngleDeg = 0.0;       // 회전각(도)
    private double chatAngleSpeed = 18.0;    // 프레임당 회전량 (조절 가능)
    private int chatSlideSpeed = 12;         // 프레임당 이동량 (조절 가능)

    private JPanel chatPanel;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton chatSendBtn;
    private JButton chatToggleBtn;

    private boolean chatOpen = false;

    private Image bg;
    private Image infoMeImg;
    private Image infoOpponentImg;


    private Image myBackImg;
    private int myImgW = 256;
    private int myImgH = 256;


    private Image enemyFrontImg;
    private int enemyImgW = 256;
    private int enemyImgH = 256;

    private int myX, myY;
    private int myTargetX, myTargetY;

    private int enemyX, enemyY;
    private int enemyTargetX, enemyTargetY;

    private Timer entryTimer;
    private boolean animMy = false;
    private boolean animEnemy = false;

    // HP 
    private double myHpRatio = 0.0;
    private double enemyHpRatio = 0.0;
    private Timer hpBarTimer;
    private boolean hpBarAnimStarted = false;

    // 커튼 효과 
    private Image curtainBgImg;
    private int curtainTopY = 0;
    private int curtainBottomY = 384;
    private int curtainTargetTopY = -385;
    private int curtainTargetBottomY = 768;
    private boolean curtainActive = true;
    private Timer curtainTimer;

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
        setLayout(null);
        setDoubleBuffered(true);

        loadBackground();
        loadCurtainImage();
        loadInfoImages();
        loadMyPokemonImage();
        initMyPositionAndStartAnim();

        startCurtainEffect();

        if (this.enemyPokemon != null) {
            loadEnemyPokemonImage();
            initEnemyPositionAndStartAnim();
        }

        initChatUI();
    }

    private void loadChatButtonSprite() {
        ImageIcon raw = new ImageIcon("Images/ball.png");
        spriteSheet = raw.getImage();

        int frameCount = 4;
        int sheetW = spriteSheet.getWidth(null);
        int sheetH = spriteSheet.getHeight(null);

        int fw = sheetW / frameCount;
        int fh = sheetH;

        chatFrames = new ImageIcon[frameCount];

        for (int i = 0; i < frameCount; i++) {
            Image frame = createImage(new FilteredImageSource(
                    spriteSheet.getSource(),
                    new CropImageFilter(i * fw, 0, fw, fh)
            ));
            frame = frame.getScaledInstance(chatBtnW, chatBtnH, Image.SCALE_SMOOTH);
            chatFrames[i] = new ImageIcon(frame);
        }
    }

  
    private void initChatUI() {
        loadChatButtonSprite();

        // 시작 위치: 오른쪽 밖에서 시작(적은 왼쪽에서 오니까 채팅은 오른쪽에서 오는 느낌)
        chatBtnX = PANEL_W + 60;
        chatBtnY = chatBtnTargetY;

        // 초기 아이콘(회전 적용)
        chatToggleBtn = new JButton(rotateIcon(chatFrames[0], chatAngleDeg));
        chatToggleBtn.setBounds(chatBtnX, chatBtnY, chatBtnW, chatBtnH);

        // hover 기준(일단 목표 좌표로 잡아두고, entry 끝나면 현재 bounds로 확정)
        chatBtnBaseBounds = new Rectangle(chatBtnTargetX, chatBtnTargetY, chatBtnW, chatBtnH);

        chatToggleBtn.setBorderPainted(false);
        chatToggleBtn.setContentAreaFilled(false);
        chatToggleBtn.setFocusPainted(false);
        chatToggleBtn.setOpaque(false);

        // hover 애니메이션: 프레임 순환 + 둥실
        chatHoverTimer = new Timer(80, e -> {
            chatFrameIndex = (chatFrameIndex + 1) % chatFrames.length;

            // hover일 때는 회전 없이(너무 정신없음) 프레임만
            // 원하면 여기서도 rotateIcon(...) 적용 가능
            chatToggleBtn.setIcon(chatFrames[chatFrameIndex]);

            int dy = (chatFrameIndex % 2 == 0) ? -2 : 0;
            chatToggleBtn.setBounds(chatBtnBaseBounds.x, chatBtnBaseBounds.y + dy,
                    chatBtnBaseBounds.width, chatBtnBaseBounds.height);
        });

        chatToggleBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (chatBtnEntering) return; // 진입 중 hover 금지
                chatFrameIndex = 0;
                chatHoverTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                chatHoverTimer.stop();
                // hover 끝나면 원래 아이콘 으로 복귀(회전은 유지 안 함)
                chatToggleBtn.setIcon(chatFrames[0]);
                chatToggleBtn.setBounds(chatBtnBaseBounds);
            }
        });

        chatToggleBtn.addActionListener(e -> toggleChat());
        add(chatToggleBtn);

        // 채팅 패널
        chatPanel = new JPanel(null);
        chatPanel.setBounds(10, 170, 350, 300);
        chatPanel.setBackground(new Color(0, 0, 0, 180));
        chatPanel.setVisible(false);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(chatArea);
        scroll.setBounds(10, 10, 330, 220);

        chatInput = new JTextField();
        chatInput.setBounds(10, 240, 260, 30);
        chatInput.addActionListener(e -> sendChat());

        chatSendBtn = new JButton("전송");
        chatSendBtn.setBounds(280, 240, 60, 30);
        chatSendBtn.addActionListener(e -> sendChat());

        chatPanel.add(scroll);
        chatPanel.add(chatInput);
        chatPanel.add(chatSendBtn);

        add(chatPanel);

        // 스르륵 + 구르기 시작
        startChatButtonEntry();
    }

    private void startChatButtonEntry() {
        if (chatEntryTimer != null && chatEntryTimer.isRunning()) return;

        chatBtnEntering = true;
        chatAngleDeg = 0.0;

        int delay = 16; // ~60fps

        chatEntryTimer = new Timer(delay, e -> {
            // (1) 이동: 오른쪽 -> 목표 X로
            if (chatBtnX > chatBtnTargetX) {
                chatBtnX -= chatSlideSpeed;
                if (chatBtnX < chatBtnTargetX) chatBtnX = chatBtnTargetX;
            }

            // (2) 회전(구르기): 이동 중 계속 회전
            if (chatBtnEntering) {
            	chatAngleDeg -= chatAngleSpeed;
                if (chatAngleDeg < 0) chatAngleDeg += 360; 
            }

            // (3) 아이콘 회전 적용 (hover 중이면 hoverTimer가 아이콘을 바꾸니까 entry 중에만 적용)
            if (chatBtnEntering) {
                chatToggleBtn.setIcon(rotateIcon(chatFrames[0], chatAngleDeg));
            }

            chatToggleBtn.setBounds(chatBtnX, chatBtnY, chatBtnW, chatBtnH);

            // (4) 도착 처리
            if (chatBtnX == chatBtnTargetX) {
                chatBtnEntering = false;
                chatEntryTimer.stop();

                // 도착 후 아이콘 정착(기본 프레임)
                chatToggleBtn.setIcon(chatFrames[0]);

                // hover 기준 좌표 확정
                chatBtnBaseBounds = chatToggleBtn.getBounds();
            }

            repaint();
        });

        chatEntryTimer.start();
    }

    private ImageIcon rotateIcon(ImageIcon src, double angleDeg) {
        if (src == null || src.getIconWidth() <= 0 || src.getIconHeight() <= 0) return src;

        int w = src.getIconWidth();
        int h = src.getIconHeight();

        BufferedImage srcImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = srcImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(src.getImage(), 0, 0, null);
        g2.dispose();

        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform at = new AffineTransform();
        at.translate(w / 2.0, h / 2.0);
        at.rotate(Math.toRadians(angleDeg));
        at.translate(-w / 2.0, -h / 2.0);

        g.drawImage(srcImg, at, null);
        g.dispose();

        return new ImageIcon(dst);
    }

    private void toggleChat() {
        chatOpen = !chatOpen;
        chatPanel.setVisible(chatOpen);
        repaint();
    }

    private void sendChat() {
        String msg = chatInput.getText().trim();
        if (msg.isEmpty()) return;

        try {
            dos.writeUTF("/chat " + msg);
            dos.flush();
            chatInput.setText("");
        } catch (Exception e) {
            appendChat("[SYSTEM] 채팅 전송 실패");
        }
    }

    private void loadBackground() {
        try {
            int n = 1 + (int) (Math.random() * 4);
            String path = "Images/bg_arena" + n + ".png";
            bg = new ImageIcon(path).getImage();
        } catch (Exception e) {
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
            String back = myPokemon.getBackImageFile();
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
            String front = enemyPokemon.getFrontImageFile();
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

        myTargetX = 120;
        myTargetY = PANEL_H - myImgH - 136;

        myX = PANEL_W + 20;
        myY = myTargetY;

        animMy = true;
        startEntryTimerIfNeeded();
    }

    private void initEnemyPositionAndStartAnim() {
        if (enemyFrontImg == null) return;

        enemyTargetX = PANEL_W - enemyImgW - 160;
        enemyTargetY = 120;

        enemyX = -enemyImgW;
        enemyY = enemyTargetY;

        animEnemy = true;
        startEntryTimerIfNeeded();
    }

    private void startEntryTimerIfNeeded() {
        if (entryTimer != null && entryTimer.isRunning()) return;

        int delay = 16;
        int speed = 20;

        entryTimer = new Timer(delay, e -> {
            boolean doneMy = !animMy;
            boolean doneEnemy = !animEnemy;

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
                startHpBarAnimation();
            }

            repaint();
        });

        entryTimer.start();
    }

    private void startHpBarAnimation() {
        if (hpBarAnimStarted) return;
        hpBarAnimStarted = true;

        int delay = 16;
        final double[] progress = {0.0};

        hpBarTimer = new Timer(delay, e -> {
            boolean myComplete;
            boolean enemyComplete;

            progress[0] += 0.02;
            if (progress[0] > 1.0) progress[0] = 1.0;

            double easedProgress = 1.0 - Math.pow(1.0 - progress[0], 2);

            if (myPokemon != null) {
                int maxHp = myPokemon.calcMaxHp();
                int curHp = myPokemon.getCurrentHp();
                double targetRatio = maxHp == 0 ? 0 : (double) curHp / maxHp;
                myHpRatio = easedProgress * targetRatio;
                myComplete = (progress[0] >= 1.0);
                if (myComplete) myHpRatio = targetRatio;
            } else {
                myComplete = true;
            }

            if (enemyPokemon != null) {
                int maxHp = enemyPokemon.calcMaxHp();
                int curHp = enemyPokemon.getCurrentHp();
                double targetRatio = maxHp == 0 ? 0 : (double) curHp / maxHp;
                enemyHpRatio = easedProgress * targetRatio;
                enemyComplete = (progress[0] >= 1.0);
                if (enemyComplete) enemyHpRatio = targetRatio;
            } else {
                enemyComplete = true;
            }

            if (myComplete && enemyComplete) hpBarTimer.stop();
            repaint();
        });

        hpBarTimer.start();
    }

    private void startCurtainEffect() {
        int delay = 8;
        int speed = 7;

        curtainTimer = new Timer(delay, e -> {
            boolean curtainComplete = true;

            if (curtainTopY > curtainTargetTopY) {
                curtainTopY -= speed;
                if (curtainTopY < curtainTargetTopY) curtainTopY = curtainTargetTopY;
                curtainComplete = false;
            }

            if (curtainBottomY < curtainTargetBottomY) {
                curtainBottomY += speed;
                if (curtainBottomY > curtainTargetBottomY) curtainBottomY = curtainTargetBottomY;
                curtainComplete = false;
            }

            if (curtainComplete) {
                curtainActive = false;
                curtainTimer.stop();
            }

            repaint();
        });

        curtainTimer.start();
    }

    public void setEnemyPokemon(Pokemon enemyPokemon) {
        this.enemyPokemon = enemyPokemon;
        loadEnemyPokemonImage();
        initEnemyPositionAndStartAnim();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (bg != null) {
            g.drawImage(bg, 0, 0, PANEL_W, PANEL_H, this);
        } else {
            g.setColor(new Color(10, 20, 40));
            g.fillRect(0, 0, PANEL_W, PANEL_H);
        }

        if (myBackImg != null) g.drawImage(myBackImg, myX, myY, this);
        if (enemyFrontImg != null) g.drawImage(enemyFrontImg, enemyX, enemyY, this);

        drawInfoContainers(g);

        if (curtainActive && curtainBgImg != null) {
            g.drawImage(curtainBgImg, 0, curtainTopY, 1024, 385, this);
            g.drawImage(curtainBgImg, 0, curtainBottomY, 1024, 385, this);
        }
    }

    private void drawInfoContainers(Graphics g) {
        if (myPokemon != null && infoMeImg != null) {
            int infoX = myX + myImgW + 90;
            int infoY = myY + 20;
            g.drawImage(infoMeImg, infoX, infoY, this);

            int infoW = infoMeImg.getWidth(this);

            g.setColor(new Color(212, 204, 171));
            g.setFont(new Font("PF Stardust Bold", Font.PLAIN, 36));
            g.drawString(myPokemon.getKoreanName(), infoX + 59, infoY + 55);

            g.setColor(Color.BLACK);
            g.drawString(myPokemon.getKoreanName(), infoX + 57, infoY + 53);

            String levelText = "Lv" + myPokemon.getLevel();
            g.setColor(new Color(212, 204, 171));
            g.drawString(levelText, infoX + infoW - 138, infoY + 55);
            g.setColor(Color.BLACK);
            g.drawString(levelText, infoX + infoW - 140, infoY + 53);

            int barX = infoX + 273;
            int barY = infoY + 80;
            int barW = 192;
            int barH = 8;

            int hpW = (int) (barW * myHpRatio);
            g.setColor(new Color(80, 220, 80));
            g.fillRect(barX, barY, hpW, barH);
        }

        if (enemyPokemon != null && infoOpponentImg != null) {
            int infoW = infoOpponentImg.getWidth(this);
            int infoX = enemyX - infoW - 50;
            int infoY = enemyY - 50;
            g.drawImage(infoOpponentImg, infoX, infoY, this);

            g.setColor(new Color(212, 204, 171));
            g.setFont(new Font("PF Stardust Bold", Font.PLAIN, 36));
            g.drawString(enemyPokemon.getKoreanName(), infoX + 27, infoY + 52);

            g.setColor(Color.BLACK);
            g.drawString(enemyPokemon.getKoreanName(), infoX + 25, infoY + 50);

            String levelText = "Lv" + enemyPokemon.getLevel();
            g.setColor(new Color(212, 204, 171));
            g.drawString(levelText, infoX + infoW - 153, infoY + 52);
            g.setColor(Color.BLACK);
            g.drawString(levelText, infoX + infoW - 155, infoY + 50);

            int barX = infoX + 237;
            int barY = infoY + 80;
            int barW = 192;
            int barH = 8;

            int hpW = (int) (barW * enemyHpRatio);
            g.setColor(new Color(80, 220, 80));
            g.fillRect(barX, barY, hpW, barH);
        }
    }

    public void handleServerMessage(String message) {
        message = message.trim();
        System.out.println("[BATTLE from server] " + message);

        if (message.startsWith("/enemy ")) {
            String[] parts = message.split("\\s+");
            if (parts.length >= 2) {
                String enemyId = parts[1].trim();
                Pokemon p = PokemonRepository.getInstance().getById(enemyId);
                if (p != null) setEnemyPokemon(p);
            }
            return;
        }

        if (message.startsWith("/chat ")) {
            String chatMsg = message.substring(6);
            appendChat(chatMsg);
        }
    }

    private void appendChat(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (chatArea == null) return;
            chatArea.append(msg + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PANEL_W, PANEL_H);
    }
}
