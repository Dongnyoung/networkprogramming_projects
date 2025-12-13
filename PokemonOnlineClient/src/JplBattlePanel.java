import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;
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
    private Pokemon enemyPokemon;           // 처음엔 null일 수 있음
    private final String myUsername;
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
    private double myHpRatio = 0.0; // 현재 표시 중인 HP 비율 (0.0 ~ 1.0)
    private double enemyHpRatio = 0.0;
    private Timer hpBarTimer;
    private boolean hpBarAnimStarted = false;

    // 로그 라벨 및 타이핑 애니메이션
    private JLabel lblLog;
    private Timer logTypingTimer;
    private int logTypingIndex = 0;
    private String logTypingTarget = "";

    // 스킬 선택 패널
    private JPanel skillPanel;
    private Image skillPanelBg;
    private JLabel lblSkillPointer; // "▶" 포인터 라벨
    private Timer skillPanelTimer;
    private int skillPanelTargetX = 404;
    private int skillPanelStartX = 1024;

    // 커튼 효과 변수
    private Image curtainBgImg; 
    private int curtainTopY = 0; // 상단 커튼 Y 위치
    private int curtainBottomY = 384; // 하단 커튼 Y 위치
    private int curtainTargetTopY = -385; // 상단 커튼 목표 위치
    private int curtainTargetBottomY = 768; // 하단 커튼 목표 위치
    private boolean curtainActive = true; // 커튼 효과 활성 여부
    private Timer curtainTimer;
    
    // 게임 종료 플래그
    private boolean gameEnded = false;
    
    // 재연결을 위한 정보 저장
    private String serverIp;
    private int serverPort;
    
    // 피격 깜빡임 애니메이션 변수
    private boolean myBlinking = false;
    private boolean enemyBlinking = false;
    private boolean myBlinkVisible = true;
    private boolean enemyBlinkVisible = true;
    
    // 쓰러지기 애니메이션 변수
    private boolean myFainting = false;
    private boolean enemyFainting = false;
    private int myFaintOffset = 0;  // 아래로 이동한 픽셀 수
    private int enemyFaintOffset = 0;

    private final Runnable stopBattleBgm;
    private BgmPlayer bgmPlayer; // 효과음용
    public void setBgmPlayer(BgmPlayer bgmPlayer) {
        this.bgmPlayer = bgmPlayer;
        bindClickSfxToAllButtons();
    }
    public JplBattlePanel(Pokemon myPokemon,
    					  Pokemon enemyPokemon,
                          String myUsername,
                          String opponentName,
                          Socket socket,
                          DataInputStream dis,
                          DataOutputStream dos,
                          int backgroundNumber,
                          Runnable stopBattleBgm) {

        this.myPokemon = myPokemon;
        this.enemyPokemon = enemyPokemon;
        this.myUsername = myUsername;
        this.opponentName = opponentName;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;
        this.stopBattleBgm = stopBattleBgm;
        // 재연결을 위한 서버 정보 저장
        try {
            this.serverIp = socket.getInetAddress().getHostAddress();
            this.serverPort = socket.getPort();
        } catch (Exception e) {
            this.serverIp = "localhost";
            this.serverPort = 30000;
        }
        
        // 포켓몬 HP 초기화 (이전 게임 데이터 제거)
        myPokemon.setCurrentHp(myPokemon.calcMaxHp());
        if (enemyPokemon != null) {
            enemyPokemon.setCurrentHp(enemyPokemon.calcMaxHp());
        }

        setBackground(Color.BLACK);
        setLayout(null);   // 나중에 스킬 버튼 깔기 편하게
        setDoubleBuffered(true);

        loadBackground(backgroundNumber);
        loadCurtainImage();
        loadInfoImages();
        loadMyPokemonImage();
        initMyPositionAndStartAnim();
        
        // 로그 라벨 초기화
        initLogLabel();
        
        // 스킬 패널 초기화
        initSkillPanel();
        
        // 커튼 효과 시작
        startCurtainEffect();
        
        if (this.enemyPokemon != null) {
            loadEnemyPokemonImage();
            initEnemyPositionAndStartAnim();
        }
        
        // 1초 대기 후 배틀 시작 메시지 표시
        Timer initialMessageTimer = new Timer(1000, e -> {
            String enemyPokemonName = (this.enemyPokemon != null) ? this.enemyPokemon.getKoreanName() : "포켓몬";
            String message = this.opponentName + "의 " + enemyPokemonName + "(이)가 승부를 걸어왔다!";
            
            // 타이핑 완료 후 0.5초 뒤 다음 메시지 표시
            startLogTyping(message, () -> {
                Timer delayTimer = new Timer(500, ev -> {
                    String myPokemonName = myPokemon.getKoreanName();
                    startLogTyping(myPokemonName + "(은)는 무엇을 할까?", () -> {
                        // "무엇을 할까?" 멘트 끝난 후 스킬 패널 표시
                        showSkillPanel();
                    });
                    ((Timer)ev.getSource()).stop();
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
            });
            
            ((Timer)e.getSource()).stop();
        });
        initialMessageTimer.setRepeats(false);
        initialMessageTimer.start();
    }
    
    private void bindClickSfxToAllButtons() {
    	ButtonSfxBinder.bindAllButtons(this, bgmPlayer, "bgm/ding.wav", -10.0f);
	}
	private void stopBgmSafely() {
    	if(stopBattleBgm != null) stopBattleBgm.run();
    }
    
    // 배틀 결과 처리 (JplWaitingRoom에서 호출)
    public void handleBattleResult(String message) {
        // 프로토콜: "/battle_result 선공유저명\n유저1닉네임,skillName,damage,hit\n유저2닉네임,skillName,damage,hit"
        String[] lines = message.split("\n");
        if (lines.length < 3) {
            System.err.println("배틀 결과 파싱 실패: " + message);
            return;
        }
        
        // 첫 줄에서 선공 유저명 추출
        String firstUser = lines[0].substring("/battle_result ".length()).trim();
        boolean iAmFirst = firstUser.equals(myUsername);
        
        // 유저1 데이터 파싱
        String[] p1Data = lines[1].split(",");
        String user1Name = p1Data[0];
        String user1SkillName = p1Data[1];
        int user1Damage = Integer.parseInt(p1Data[2]);
        boolean user1Hit = p1Data[3].equals("1");
        
        // 유저2 데이터 파싱
        String[] p2Data = lines[2].split(",");
        String user2Name = p2Data[0];
        String user2SkillName = p2Data[1];
        int user2Damage = Integer.parseInt(p2Data[2]);
        boolean user2Hit = p2Data[3].equals("1");
        
        // 내 데이터 확인
        boolean p1IsMe = user1Name.equals(myUsername);
        
        // 내가 준/받은 데미지 및 기술 이름 결정
        String mySkillName = p1IsMe ? user1SkillName : user2SkillName;
        int myDamageGiven = p1IsMe ? user1Damage : user2Damage;
        boolean myHit = p1IsMe ? user1Hit : user2Hit;
        String enemySkillName = p1IsMe ? user2SkillName : user1SkillName;
        int enemyDamageGiven = p1IsMe ? user2Damage : user1Damage;
        boolean enemyHit = p1IsMe ? user2Hit : user2Hit;
        
        System.out.println("배틀 결과: 선공=" + (iAmFirst ? "나" : "상대"));
        System.out.println("내가 준 데미지: " + myDamageGiven + ", 내 명중: " + myHit);
        System.out.println("상대가 준 데미지: " + enemyDamageGiven + ", 상대 명중: " + enemyHit);
        
        // 공격 순서에 따라 연출
        if (iAmFirst) {
            // 내가 선공 - 내가 공격하면 상대가 내가 준 데미지를 입음
            animateAttack(true, myHit, myDamageGiven, mySkillName, () -> {
                // 상대 HP 체크
                if (enemyPokemon.getCurrentHp() <= 0) {
                    showVictory();
                    return;
                }
                // 상대 후공 - 상대가 공격하면 내가 상대가 준 데미지를 입음
                animateAttack(false, enemyHit, enemyDamageGiven, enemySkillName, () -> {
                    // 내 HP 체크
                    if (myPokemon.getCurrentHp() <= 0) {
                        showDefeat();
                        return;
                    }
                    // 다음 턴 - 메시지 후 스킬 패널 표시
                    startLogTyping(myPokemon.getKoreanName() + "은(는) 무엇을 할까?", () -> {
                        showSkillPanel();
                    });
                });
            });
        } else {
            // 상대가 선공 - 상대가 공격하면 내가 상대가 준 데미지를 입음
            animateAttack(false, enemyHit, enemyDamageGiven, enemySkillName, () -> {
                // 내 HP 체크
                if (myPokemon.getCurrentHp() <= 0) {
                    showDefeat();
                    return;
                }
                // 내가 후공 - 내가 공격하면 상대가 내가 준 데미지를 입음
                animateAttack(true, myHit, myDamageGiven, mySkillName, () -> {
                    // 상대 HP 체크
                    if (enemyPokemon.getCurrentHp() <= 0) {
                        showVictory();
                        return;
                    }
                    // 다음 턴 - 메시지 후 스킬 패널 표시
                    startLogTyping(myPokemon.getKoreanName() + "은(는) 무엇을 할까?", () -> {
                        showSkillPanel();
                    });
                });
            });
        }
    }
    
    // 공격 애니메이션
    private void animateAttack(boolean iAmAttacker, boolean hit, int damage, String skillName, Runnable onComplete) {
        String attacker = iAmAttacker ? myPokemon.getKoreanName() : ("상대 " + enemyPokemon.getKoreanName());
        
        if (!hit) {
            // 빗나감
            startLogTyping(attacker + "의 " + skillName + " 공격!", () -> {
                Timer missTimer = new Timer(1000, e -> {
                    startLogTyping("하지만 빗나갔다!", onComplete);
                    ((Timer)e.getSource()).stop();
                });
                missTimer.setRepeats(false);
                missTimer.start();
            });
        } else {
            // 명중 - 깜빡임 애니메이션 후 HP 감소
            startLogTyping(attacker + "의 " + skillName + " 공격!", () -> {
                Timer attackDelayTimer = new Timer(500, e -> {
                	// ouch 효과음
                	if (bgmPlayer != null) {
                        bgmPlayer.playSfxOnce("bgm/ouch.wav", -10.0f, null);
                    }
                    // 깜빡임 애니메이션 (맞는 포켓몬)
                    animateBlink(!iAmAttacker, () -> {
                        // HP 감소
                        if (iAmAttacker) {
                            enemyPokemon.setCurrentHp(Math.max(0, enemyPokemon.getCurrentHp() - damage));
                            animateHpDecrease(false, onComplete);
                        } else {
                            myPokemon.setCurrentHp(Math.max(0, myPokemon.getCurrentHp() - damage));
                            animateHpDecrease(true, onComplete);
                        }
                    });
                    ((Timer)e.getSource()).stop();
                });
                attackDelayTimer.setRepeats(false);
                attackDelayTimer.start();
            });
        }
    }
    
    // HP 감소 애니메이션
    private void animateHpDecrease(boolean isMyPokemon, Runnable onComplete) {
        Timer hpTimer = new Timer(16, null);
        hpTimer.addActionListener(e -> {
            double targetRatio;
            double currentRatio;
            
            if (isMyPokemon) {
                targetRatio = (double)myPokemon.getCurrentHp() / myPokemon.getMaxHp();
                currentRatio = myHpRatio;
                
                if (Math.abs(currentRatio - targetRatio) < 0.01) {
                    myHpRatio = targetRatio;
                    repaint();
                    ((Timer)e.getSource()).stop();
                    if (onComplete != null) onComplete.run();
                } else {
                    myHpRatio += (targetRatio - currentRatio) * 0.1;
                    repaint();
                }
            } else {
                targetRatio = (double)enemyPokemon.getCurrentHp() / enemyPokemon.getMaxHp();
                currentRatio = enemyHpRatio;
                
                if (Math.abs(currentRatio - targetRatio) < 0.01) {
                    enemyHpRatio = targetRatio;
                    repaint();
                    ((Timer)e.getSource()).stop();
                    if (onComplete != null) onComplete.run();
                } else {
                    enemyHpRatio += (targetRatio - currentRatio) * 0.1;
                    repaint();
                }
            }
        });
        hpTimer.start();
    }
    
    // 깜빡임 애니메이션 (피격 시)
    private void animateBlink(boolean isMyPokemon, Runnable onComplete) {
        final int[] blinkCount = {0};
        final int totalBlinks = 4;  // 4번 깜빡임
        final int blinkDelay = 60; // 더 빠른 깜빡임
        
        if (isMyPokemon) {
            myBlinking = true;
            myBlinkVisible = true;
        } else {
            enemyBlinking = true;
            enemyBlinkVisible = true;
        }
        
        Timer blinkTimer = new Timer(blinkDelay, null);
        blinkTimer.addActionListener(e -> {
            // 토글 (보임/안보임)
            if (isMyPokemon) {
                myBlinkVisible = !myBlinkVisible;
            } else {
                enemyBlinkVisible = !enemyBlinkVisible;
            }
            
            blinkCount[0]++;
            
            // 8회 토글 = 4번 깜빡임 완료
            if (blinkCount[0] >= totalBlinks * 2) {
                if (isMyPokemon) {
                    myBlinking = false;
                    myBlinkVisible = true;
                } else {
                    enemyBlinking = false;
                    enemyBlinkVisible = true;
                }
                ((Timer)e.getSource()).stop();
                repaint();
                if (onComplete != null) onComplete.run();
            } else {
                repaint();
            }
        });
        blinkTimer.start();
    }
    
    // 쓰러지기 애니메이션
    private void animateFaint(boolean isMyPokemon, Runnable onComplete) {
        final int maxOffset = 200;  // 실제 포켓몬 높이만큼 내려감 (PNG 여백 제외)
        final int speed = 16;  // 한 프레임당 이동량 (더 빠르게)
        
        if (isMyPokemon) {
            myFainting = true;
            myFaintOffset = 0;
        } else {
            enemyFainting = true;
            enemyFaintOffset = 0;
        }
        
        Timer faintTimer = new Timer(16, null);  // ~60fps
        faintTimer.addActionListener(e -> {
            if (isMyPokemon) {
                myFaintOffset += speed;
                if (myFaintOffset >= maxOffset) {
                    myFaintOffset = maxOffset;
                    ((Timer)e.getSource()).stop();
                    if (onComplete != null) onComplete.run();
                }
            } else {
                enemyFaintOffset += speed;
                if (enemyFaintOffset >= maxOffset) {
                    enemyFaintOffset = maxOffset;
                    ((Timer)e.getSource()).stop();
                    if (onComplete != null) onComplete.run();
                }
            }
            repaint();
        });
        faintTimer.start();
    }
    
    // 승리 처리
    private void showVictory() {
        if (gameEnded) return; // 이미 게임이 끝났으면 무시
        gameEnded = true;
        
        // 서버에 게임 종료 신호 전송
        sendBattleEndSignal();
        
        // 상대 포켓몬 쓰러지는 모션
        animateFaint(false, () -> {
        	stopBgmSafely();
        	if (bgmPlayer != null) {
                bgmPlayer.playSfxOnce("bgm/victory.wav", -10.0f, null);
            }
            // "상대 {포켓몬 이름}이(가) 쓰러졌다"
            startLogTyping("상대 " + enemyPokemon.getKoreanName() + "이(가) 쓰러졌다!", () -> {
                Timer victoryTimer = new Timer(1000, e -> {
                    // "상대 {닉네임} 과의 대결에서 승리했다!"
                    startLogTyping("상대 " + opponentName + " 과의 대결에서 승리했다!", () -> {
                        // 메시지 후 결과 화면 표시
                        Timer resultTimer = new Timer(1500, ev -> {
                            showResultPanel();
                            ((Timer)ev.getSource()).stop();
                        });
                        resultTimer.setRepeats(false);
                        resultTimer.start();
                    });
                    ((Timer)e.getSource()).stop();
                });
                victoryTimer.setRepeats(false);
                victoryTimer.start();
            });
        });
    }
    
    // 패배 처리
    private void showDefeat() {
        if (gameEnded) return; // 이미 게임이 끝났으면 무시
        gameEnded = true;
        
        // 서버에 게임 종료 신호 전송
        sendBattleEndSignal();
        
        // 내 포켓몬 쓰러지는 모션
        animateFaint(true, () -> {
        	stopBgmSafely();
            // "{포켓몬 이름}이(가) 쓰러졌다"
            startLogTyping(myPokemon.getKoreanName() + "이(가) 쓰러졌다!", () -> {
                Timer defeatTimer = new Timer(1000, e -> {
                    // "상대 {닉네임} 과의 대결에서 패배했다..."
                    startLogTyping("상대 " + opponentName + " 과의 대결에서 패배했다...", () -> {
                        // 메시지 후 결과 화면 표시
                        Timer resultTimer = new Timer(1500, ev -> {
                            showResultPanel();
                            ((Timer)ev.getSource()).stop();
                        });
                        resultTimer.setRepeats(false);
                        resultTimer.start();
                    });
                    ((Timer)e.getSource()).stop();
                });
                defeatTimer.setRepeats(false);
                defeatTimer.start();
            });
        });
    }
    
    // 상대방 연결 끊김 처리
    public void handleOpponentDisconnect() {
        if (gameEnded) return; // 이미 게임이 끝났으면 무시
        gameEnded = true;
        
        // 서버에 게임 종료 신호 전송
        sendBattleEndSignal();
        
        // 스킬 패널이 띄워져 있으면 숨기기
        if (skillPanel != null && skillPanel.isVisible()) {
            hideSkillPanel();
        }
        
        startLogTyping("상대방의 연결이 끊어졌습니다.", () -> {
            Timer disconnectTimer = new Timer(1500, e -> {
                // 승리 메시지
                startLogTyping("상대 " + opponentName + " 과의 대결에서 승리했다!", () -> {
                    // 메시지 후 결과 화면 표시
                    Timer resultTimer = new Timer(1500, ev -> {
                        showResultPanel();
                        ((Timer)ev.getSource()).stop();
                    });
                    resultTimer.setRepeats(false);
                    resultTimer.start();
                });
                ((Timer)e.getSource()).stop();
            });
            disconnectTimer.setRepeats(false);
            disconnectTimer.start();
        });
    }

    // 결과 화면 패널 표시 (메인으로, 다시하기 버튼)
    private void showResultPanel() {
        if (skillPanel == null) return;
        
        // 기존 버튼 제거
        skillPanel.removeAll();
        
        // "▶" 포인터 라벨 다시 추가
        lblSkillPointer.setVisible(false);
        skillPanel.add(lblSkillPointer);
        
        int buttonWidth = 240;
        int buttonHeight = 50;
        int gapX = 40;
        int startX = (620 - (buttonWidth * 2 + gapX)) / 2;
        int startY = 70;
        
        // "메인으로" 버튼
        JButton btnMainMenu = new JButton("메인으로");
        btnMainMenu.setBounds(startX, startY, buttonWidth, buttonHeight);
        btnMainMenu.setFont(new Font("PF Stardust Bold", Font.PLAIN, 32));
        btnMainMenu.setForeground(Color.BLACK);
        btnMainMenu.setContentAreaFilled(false);
        btnMainMenu.setBorderPainted(false);
        btnMainMenu.setFocusPainted(false);
        
        // 마우스 오버 시 "▶" 표시
        btnMainMenu.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                lblSkillPointer.setBounds(startX + 20, startY + 5, 30, 40);
                lblSkillPointer.setVisible(true);
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                lblSkillPointer.setVisible(false);
            }
        });
        
        btnMainMenu.addActionListener(e -> {
        	stopBgmSafely(); //브금끄기
            // 소켓 연결 끊기
            try {
                if (dos != null) dos.writeUTF("/exit");
                if (socket != null) socket.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            // 타이틀 화면으로 (프레임 초기화)
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof JFrame) {
                JFrame frame = (JFrame) window;
                frame.getContentPane().removeAll();
                // FrmSeverConnect의 초기 패널 다시 설정 필요
                frame.dispose();
                // 새 프레임 실행
                SwingUtilities.invokeLater(() -> {
                    try {
                        FrmSeverConnect newFrame = new FrmSeverConnect();
                        newFrame.setVisible(true);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        });
        
        // "다시하기" 버튼
        JButton btnRetry = new JButton("다시하기");
        btnRetry.setBounds(startX + buttonWidth + gapX, startY, buttonWidth, buttonHeight);
        btnRetry.setFont(new Font("PF Stardust Bold", Font.PLAIN, 32));
        btnRetry.setForeground(Color.BLACK);
        btnRetry.setContentAreaFilled(false);
        btnRetry.setBorderPainted(false);
        btnRetry.setFocusPainted(false);
        
        // 마우스 오버 시 "▶" 표시
        final int btnX = startX + buttonWidth + gapX;
        btnRetry.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                lblSkillPointer.setBounds(btnX + 20, startY + 5, 30, 40);
                lblSkillPointer.setVisible(true);
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                lblSkillPointer.setVisible(false);
            }
        });
        
        btnRetry.addActionListener(e -> {
        	
            // 기존 소켓 연결 닫기
            try {
                if (dos != null) dos.writeUTF("/exit");
                if (socket != null) socket.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            stopBgmSafely();// 브금끄기
            // 저장된 정보로 재연결하여 대기실로 이동
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof JFrame) {
                JFrame frame = (JFrame) window;
                frame.dispose();
                
                SwingUtilities.invokeLater(() -> {
                    try {
                        // 새 프레임 생성 후 자동 연결
                        FrmSeverConnect newFrame = new FrmSeverConnect();
                        newFrame.setVisible(true);
                        
                        // 자동으로 연결 시도 (약간의 딥레이 후)
                        Timer autoConnectTimer = new Timer(500, ev -> {
                            newFrame.autoConnect(serverIp, String.valueOf(serverPort), myUsername);
                            ((Timer)ev.getSource()).stop();
                        });
                        autoConnectTimer.setRepeats(false);
                        autoConnectTimer.start();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        });
        
        skillPanel.add(btnMainMenu);
        skillPanel.add(btnRetry);
        skillPanel.revalidate();
        skillPanel.repaint();
        
        // 패널 표시
        showSkillPanel();
        ButtonSfxBinder.bindAllButtons(skillPanel, bgmPlayer, "bgm/ding.wav", -10.0f);
    }

    private void loadBackground(int bgNumber) {
        try {
            String path = "Images/bg_arena" + bgNumber + ".png";
            bg = new ImageIcon(path).getImage();
        } catch (Exception e) {
            System.err.println("Failed to load background: bg_arena" + bgNumber + ".png");
            bg = null;
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

    private void initLogLabel() {
        lblLog = new JLabel();
        lblLog.setBounds(40, 613, 944, 120);
        lblLog.setFont(new Font("PF Stardust Bold", Font.PLAIN, 32));
        lblLog.setForeground(Color.BLACK);
        lblLog.setVerticalAlignment(SwingConstants.TOP);
        lblLog.setText("");
        add(lblLog);
    }

    private void initSkillPanel() {
        // 스킬 패널 배경 이미지 로드
        try {
            skillPanelBg = new ImageIcon("Images/pnl_skill.png").getImage();
        } catch (Exception e) {
            System.err.println("Failed to load pnl_skill.png");
            e.printStackTrace();
        }

        // 커스텀 패널 생성
        skillPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (skillPanelBg != null) {
                    g.drawImage(skillPanelBg, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        
        skillPanel.setBounds(404, 576, 620, 192);
        skillPanel.setLayout(null);
        skillPanel.setOpaque(false);
        skillPanel.setVisible(false); // 초기에는 숨김
        
        // "▶" 포인터 라벨 생성
        lblSkillPointer = new JLabel("▶");
        lblSkillPointer.setFont(new Font("PF Stardust Bold", Font.PLAIN, 32));
        lblSkillPointer.setForeground(Color.BLACK);
        lblSkillPointer.setVisible(false);
        skillPanel.add(lblSkillPointer);
        
        // 스킬 버튼 추가 (2x2 그리드)
        List<Skill> skills = myPokemon.getSkills();
        int buttonWidth = 240;
        int buttonHeight = 50;
        int gapX = 20;
        int gapY = 16;
        int startX = 45;
        int startY = 40;
        
        for (int i = 0; i < Math.min(4, skills.size()); i++) {
            Skill skill = skills.get(i);
            int row = i / 2;
            int col = i % 2;
            
            final int btnX = startX + col * (buttonWidth + gapX);
            final int btnY = startY + row * (buttonHeight + gapY);
            
            JButton btnSkill = new JButton(skill.getKoreanName());
            btnSkill.setBounds(btnX, btnY, buttonWidth, buttonHeight);
            btnSkill.setFont(new Font("PF Stardust Bold", Font.PLAIN, 32));
            
            // 타입에 따른 글자 색 설정
            Color textColor = Color.BLACK; // 기본 색상 (노말 타입)
            String type = skill.getType();
            if (type != null) {
                if (type.equalsIgnoreCase("GRASS") || type.equalsIgnoreCase("풀")) {
                    textColor = new Color(34, 139, 34); // 초록
                } else if (type.equalsIgnoreCase("FIRE") || type.equalsIgnoreCase("불꽃")) {
                    textColor = new Color(220, 20, 20); // 빨강
                } else if (type.equalsIgnoreCase("WATER") || type.equalsIgnoreCase("물")) {
                    textColor = new Color(30, 144, 255); // 파랑
                }
            }
            btnSkill.setForeground(textColor);
            
            btnSkill.setContentAreaFilled(false);
            btnSkill.setBorderPainted(false);
            btnSkill.setFocusPainted(false);
            btnSkill.setHorizontalAlignment(SwingConstants.LEFT);
            
            // 마우스 오버 시 "▶" 표시
            btnSkill.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    lblSkillPointer.setBounds(btnX - 10, btnY + 5, 30, 40);
                    lblSkillPointer.setVisible(true);
                }
                
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    lblSkillPointer.setVisible(false);
                }
            });
            
            // 스킬 버튼 클릭 이벤트
            btnSkill.addActionListener(e -> {
                System.out.println("스킬 선택: " + skill.getKoreanName());
                // 패널 숨기기 (오른쪽으로 슬라이드)
                hideSkillPanel();
                // 서버에 스킬 사용 전송
                sendSkillToServer(skill);
            });
            
            skillPanel.add(btnSkill);
        }
        
        add(skillPanel);
    }

    private void showSkillPanel() {
        if (skillPanel == null) return;
        
        // 시작 위치를 화면 오른쪽 밖으로 설정
        skillPanel.setBounds(skillPanelStartX, 576, 620, 192);
        skillPanel.setVisible(true);
        
        // 최상위로 이동 (컴포넌트 순서를 맨 앞으로)
        remove(skillPanel);
        add(skillPanel, 0);
        
        // 애니메이션 시작
        int delay = 16; // ~60fps
        final double[] progress = {0.0};
        
        skillPanelTimer = new Timer(delay, e -> {
            progress[0] += 0.04; // 진행도 증가
            if (progress[0] > 1.0) progress[0] = 1.0;
            
            // ease-out: 처음 빠르다가 점점 느려짐
            double easedProgress = 1.0 - Math.pow(1.0 - progress[0], 2);
            
            // 현재 X 위치 계산
            int currentX = (int)(skillPanelStartX + (skillPanelTargetX - skillPanelStartX) * easedProgress);
            skillPanel.setBounds(currentX, 576, 620, 192);
            
            // 애니메이션 완료
            if (progress[0] >= 1.0) {
                skillPanelTimer.stop();
            }
            
            repaint();
        });
        
        skillPanelTimer.start();
    }
    
    private void hideSkillPanel() {
        if (skillPanel == null || !skillPanel.isVisible()) return;
        
        // 현재 위치에서 오른쪽으로 사라짐
        int delay = 16; // ~60fps
        final double[] progress = {0.0};
        final int startX = skillPanel.getX();
        
        skillPanelTimer = new Timer(delay, e -> {
            progress[0] += 0.05; // 사라질 때는 조금 더 빠르게
            if (progress[0] > 1.0) progress[0] = 1.0;
            
            // ease-in: 처음 느리다가 점점 빠르게
            double easedProgress = Math.pow(progress[0], 2);
            
            // 현재 X 위치 계산 (오른쪽으로 이동)
            int currentX = (int)(startX + (skillPanelStartX - startX) * easedProgress);
            skillPanel.setBounds(currentX, 576, 620, 192);
            
            // 애니메이션 완료
            if (progress[0] >= 1.0) {
                skillPanelTimer.stop();
                skillPanel.setVisible(false);
            }
            
            repaint();
        });
        
        skillPanelTimer.start();
    }

    private void startLogTyping(String message) {
        startLogTyping(message, null);
    }

    private void startLogTyping(String message, Runnable onComplete) {
        stopLogTyping();
        logTypingTarget = (message == null) ? "" : message;
        logTypingIndex = 0;
        lblLog.setText("");
        int delay = 40; // ms per character
        logTypingTimer = new Timer(delay, ev -> {
            if (logTypingIndex <= logTypingTarget.length()) {
                logTypingIndex++;
                int end = Math.min(logTypingIndex, logTypingTarget.length());
                lblLog.setText(logTypingTarget.substring(0, end));
                if (end >= logTypingTarget.length()) {
                    stopLogTyping();
                    // 타이핑 완료 후 콜백 실행
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        });
        logTypingTimer.setInitialDelay(delay);
        logTypingTimer.start();
    }

    private void stopLogTyping() {
        if (logTypingTimer != null && logTypingTimer.isRunning()) {
            logTypingTimer.stop();
        }
        logTypingTimer = null;
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

        // 내 포켓몬(뒷모습)
        if (myBackImg != null) {
            if (myFainting) {
                // 쓰러지는 모션: 아래로 이동하면서 바닥 아래로 가라앉음
                int drawY = myY + myFaintOffset;  // 아래로 이동
                int bottomY = myY + 200;  // 실제 포켓몬 바닥 위치 (PNG 여백 제외)
                int visibleHeight = bottomY - drawY;  // 바닥 위에 보이는 높이
                if (visibleHeight > 0) {
                    // 이미지의 위쪽 visibleHeight만큼만 그림
                    g.drawImage(myBackImg, myX, drawY, myX + myImgW, bottomY,
                              0, 0, myImgW, visibleHeight, this);
                }
            } else if (!myBlinking || myBlinkVisible) {
                // 깜빡임: 보이지 않을 때는 그리지 않음
                g.drawImage(myBackImg, myX, myY, this);
            }
        }

        // 상대 포켓몬(앞모습)
        if (enemyFrontImg != null) {
            if (enemyFainting) {
                // 쓰러지는 모션: 아래로 이동하면서 바닥 아래로 가라앉음
                int drawY = enemyY + enemyFaintOffset;  // 아래로 이동
                int bottomY = enemyY + 200;  // 실제 포켓몬 바닥 위치 (PNG 여백 제외)
                int visibleHeight = bottomY - drawY;  // 바닥 위에 보이는 높이
                if (visibleHeight > 0) {
                    // 이미지의 위쪽 visibleHeight만큼만 그림
                    g.drawImage(enemyFrontImg, enemyX, drawY, enemyX + enemyImgW, bottomY,
                              0, 0, enemyImgW, visibleHeight, this);
                }
            } else if (!enemyBlinking || enemyBlinkVisible) {
                // 깜빡임: 보이지 않을 때는 그리지 않음
                g.drawImage(enemyFrontImg, enemyX, enemyY, this);
            }
        }

        // 정보창 이미지 표시
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

        if (message.startsWith("/battle_result ")) {
            handleBattleResult(message);
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

    // 서버에 게임 종료 신호 전송
    private void sendBattleEndSignal() {
        try {
            dos.writeUTF("/battle_end");
            dos.flush();
            System.out.println("서버로 게임 종료 신호 전송");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // 서버에 스킬 사용 전송
    private void sendSkillToServer(Skill skill) {
        try {
            // 프로토콜: /battle pokemonId skillName skillType pokemonType1 pokemonType2 skillPower skillAccuracy attack defense speed level
            String pokemonType1 = (myPokemon.getType1() != null) ? myPokemon.getType1() : "";
            String pokemonType2 = (myPokemon.getType2() != null) ? myPokemon.getType2() : "";
            String skillType = (skill.getType() != null) ? skill.getType() : "";
            
            String msg = "/battle " + myPokemon.getId() + " " 
                    + skill.getKoreanName() + " " 
                    + skillType + " "
                    + pokemonType1 + " "
                    + pokemonType2 + " "
                    + skill.getPower() + " " 
                    + skill.getAccuracy() + " " 
                    + myPokemon.getAttack() + " " 
                    + myPokemon.getDefense() + " " 
                    + myPokemon.getSpeed() + " "
                    + myPokemon.getLevel();
            dos.writeUTF(msg);
            dos.flush();
            System.out.println("서버로 전송: " + msg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PANEL_W, PANEL_H);
    }
}
