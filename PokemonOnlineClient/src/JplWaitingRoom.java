import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.Socket;

import javax.swing.ImageIcon;
import javax.swing.Timer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class JplWaitingRoom extends JPanel {
	private static final long serialVersionUID = 1L;
	
	// 배경 이미지
	private Image bg;
	private int w;
	private int h;
	
	// 포켓몬 선택 버튼들
	private JButton btnPokemon1;
	private JButton btnPokemon2;
	private JButton btnPokemon3;
	private JButton btnPokemon4; // 랜덤 볼

	// ball sprite frames and animation state
	private ImageIcon[] ballFrames;
	// select sprite frames (4분할된 select.png)
	private ImageIcon[] selectFrames;
	// select overlay label shown above buttons
	private JLabel lblSelectOverlay;
	private boolean hover1 = false, hover2 = false, hover3 = false;
	private boolean hover4 = false;
	private Timer animTimer;
	private Timer randomPreviewTimer;
	private int animFrame = 0;
	private int randomIndex = 0;
	
	// 선택된 포켓몬 (1, 2, 3 중 하나, 0은 미선택)
	private int selectedPokemon = 0;
	
	// (준비/게임 시작 버튼은 UI에서 제거됨)
	
	// 상태 표시 라벨
	private JLabel lblStatus;
	private JLabel lblMyStatus;
	private JLabel lblOpponentStatus;
	// 우상단 남은시간 라벨
	private JLabel lblTimer;

	private Timer waitingTimer;
	private int waitingSeconds =15;
	
	private Timer startCountdownTimer;
	private int startCountdown=0;
	
	// 상태 라벨 타이핑 애니메이션
	private Timer statusTypingTimer;
	private int typingIndex = 0;
	private String typingTarget = "";
	
	// 서버 통신 관련
	private Socket socket;
	private DataInputStream dis;
	private DataOutputStream dos;
	private ClientService clientService;
	private volatile boolean running = true;
	private java.util.function.Consumer<String> messageHandler; // 현재 화면에 메시지 전달

	private boolean gameStarting = false;

	
	private String username;
	private String opponentName = "";
	private boolean myReady = false;
	private boolean opponentReady = false;
	
	//상대 포켓몬 아이디 추가
	private String opponentPokemonId;
	// 배경 번호 (서버에서 받음)
	private int backgroundNumber = 1;
	// 포켓몬 이름들 (이미지 파일명과 매칭)
	private String[] pokemonNames = {"이상해씨", "파이리", "꼬부기"};
	
	private String[] pokemonIds = {"bulbasaur", "charmander", "squirtle"};
	
	// 중앙 미리보기 라벨(배경)과 몬스터 이미지 라벨
	private JLabel lblCenter;
	private JLabel lblCenterMonster;
	// 선택 확정 상태
	private boolean selectionLocked = false;
	
	private BattleStartListener battleStartListener;
	private FrmSeverConnect frmParent; // 프레임 참조
	
	private final BgmPlayer bgmPlayer = new BgmPlayer();
	
	public JplWaitingRoom(String username, String ip, String port, BattleStartListener battleStartListener, FrmSeverConnect frame) {
		this.username = username;
		this.battleStartListener = battleStartListener;
		this.frmParent = frame;
		setLayout(null);
		
		// 배경 이미지 로드
		ImageIcon bgImage = new ImageIcon("Images/bg_waiting.png");
		if (bgImage.getIconWidth() == -1) {
			// 이미지가 없으면 기본 배경 사용
			bgImage = new ImageIcon("Images/bg_main.png");
		}
		w = bgImage.getIconWidth();
		h = bgImage.getIconHeight();
		bg = bgImage.getImage();
		
		// 서버 연결
		try {
			int portInt = Integer.parseInt(port);
			socket = new Socket(ip, portInt);
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			
			// 로그인 메시지 전송
			dos.writeUTF("/login " + username);
			
			// 클라이언트 서비스 스레드 시작
			clientService = new ClientService();
			setMessageHandler(this::handleServerMessage); // waitingRoom의 핸들러 세팅
			clientService.start();


			
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					"서버 연결에 실패했습니다: " + e.getMessage(),
					"연결 오류", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		
		// 폰트 설정
		Font fieldFont = new Font("PF Stardust Bold", Font.BOLD, 28);
		
		// 포켓몬 선택 버튼들 생성 (가로로 배치)
		btnPokemon1 = new JButton();
		btnPokemon1.setBounds(230, 330, 96, 80);
		btnPokemon1.setOpaque(false);
		btnPokemon1.setContentAreaFilled(false);
		btnPokemon1.setBorderPainted(false);
		btnPokemon1.setFocusPainted(false);
		btnPokemon1.addActionListener(e -> confirmAndSelect(1));
		btnPokemon1.addMouseListener(new MouseAdapter() {
			@Override public void mouseEntered(MouseEvent e){
				hover1 = true;
				if (!selectionLocked) showPreviewFor(1);
				if (!selectionLocked) showSelectOverlay(1);
			}
			@Override public void mouseExited(MouseEvent e){
				hover1 = false;
				if (!selectionLocked) hidePreview();
				if (!selectionLocked) hideSelectOverlay();
			}
		});

		
		btnPokemon2 = new JButton();
		btnPokemon2.setBounds(373, 410, 96, 80);
		btnPokemon2.setOpaque(false);
		btnPokemon2.setContentAreaFilled(false);
		btnPokemon2.setBorderPainted(false);
		btnPokemon2.setFocusPainted(false);
		btnPokemon2.addActionListener(e -> confirmAndSelect(2));
		btnPokemon2.addMouseListener(new MouseAdapter() {
			@Override public void mouseEntered(MouseEvent e){
				hover2 = true;
				if (!selectionLocked) showPreviewFor(2);
				if (!selectionLocked) showSelectOverlay(2);
			}
			@Override public void mouseExited(MouseEvent e){
				hover2 = false;
				if (!selectionLocked) hidePreview();
				if (!selectionLocked) hideSelectOverlay();
			}
		});

		
		btnPokemon3 = new JButton();
		btnPokemon3.setBounds(555, 410, 96, 80);
		btnPokemon3.setOpaque(false);
		btnPokemon3.setContentAreaFilled(false);
		btnPokemon3.setBorderPainted(false);
		btnPokemon3.setFocusPainted(false);
		btnPokemon3.addActionListener(e -> confirmAndSelect(3));
		btnPokemon3.addMouseListener(new MouseAdapter() {
			@Override public void mouseEntered(MouseEvent e){
				hover3 = true;
				if (!selectionLocked) showPreviewFor(3);
				if (!selectionLocked) showSelectOverlay(3);
			}
			@Override public void mouseExited(MouseEvent e){
				hover3 = false;
				if (!selectionLocked) hidePreview();
				if (!selectionLocked) hideSelectOverlay();
			}
		});


		// 랜덤 몬스터볼 버튼
		btnPokemon4 = new JButton();
		btnPokemon4.setBounds(700, 330, 96, 80);
		btnPokemon4.setOpaque(false);
		btnPokemon4.setContentAreaFilled(false);
		btnPokemon4.setBorderPainted(false);
		btnPokemon4.setFocusPainted(false);
		btnPokemon4.addActionListener(e -> confirmAndSelectRandom());
		btnPokemon4.addMouseListener(new MouseAdapter() {
			@Override public void mouseEntered(MouseEvent e){
				hover4 = true;
				if (!selectionLocked) startRandomPreview();
				if (!selectionLocked) showSelectOverlay(4);
			}
			@Override public void mouseExited(MouseEvent e){
				hover4 = false;
				if (!selectionLocked) stopRandomPreview();
				if (!selectionLocked) hideSelectOverlay();
			}
		});

		
		// ball sprite 로드 및 애니메이션 초기화
		loadBallSprite();
		
		// (준비/게임 시작 버튼 제거 — 선택 즉시 준비 처리)
		
		// 상태 표시 라벨
		lblStatus = new JLabel("");
		lblStatus.setFont(fieldFont);
		lblStatus.setBounds(100, 620, 800, 40);
		lblStatus.setOpaque(false);
		
		lblMyStatus = new JLabel("■ " + username+"(나): 포켓몬 선택중");
		lblMyStatus.setFont(new Font("PF Stardust Bold", Font.BOLD, 24));
		lblMyStatus.setBounds(13, 10, 360, 34);
		lblMyStatus.setOpaque(false);
		lblMyStatus.setForeground(new Color(245, 245, 220)); // 베이지
		
		lblOpponentStatus = new JLabel("■ " + "상대: 상대의 접속을 기다리는중...");
		lblOpponentStatus.setFont(new Font("PF Stardust Bold", Font.BOLD, 24));
		lblOpponentStatus.setBounds(13, 46, 360, 34);
		lblOpponentStatus.setOpaque(false);
		lblOpponentStatus.setForeground(new Color(245, 245, 220)); // 베이지

		// 우상단 남은시간 라벨 초기화 (초기값 15)
		lblTimer = new JLabel("15");
		lblTimer.setFont(new Font("PF Stardust Bold", Font.BOLD, 50));
		lblTimer.setForeground(new Color(245, 245, 220)); // 베이지
		int tx = Math.max(0, w - 90);
		lblTimer.setBounds(tx, 20, 100, 50);
		lblTimer.setOpaque(false);
		
		waitingSeconds = 15;
		lblTimer.setText(String.valueOf(waitingSeconds));
		waitingTimer = new Timer(1000, e -> {
		    // 이미 둘 다 ready 되면 대기 타이머는 의미 없으니 정지
		    if (myReady && opponentReady) {
		        ((Timer) e.getSource()).stop();
		        return;
		    }

		    waitingSeconds--;
		    if (waitingSeconds <= 0) {
		        waitingSeconds = 0;
		        lblTimer.setText("0");
		        ((Timer) e.getSource()).stop();
		        
		        // 시간 초과: 아직 선택 안 했으면 랜덤 강제 선택
		        if (!myReady && selectedPokemon == 0) {
		            int randomChoice = (int)(Math.random() * 3) + 1; // 1, 2, 3 중 랜덤
		            selectPokemon(randomChoice);
		        }
		    } else {
		        lblTimer.setText(String.valueOf(waitingSeconds));
		    }
		});
		// 타이머는 상대가 들어올 때 시작 (자동 시작 X)
		// 중앙에 표시할 이미지 라벨 (pnl_unselect.png)
		int cw = 244, ch = 244; // 중앙 라벨 크기
		int cx = Math.max(0, (w - cw) / 2);
		int cy = Math.max(0, (h - ch) / 2) - 150;
		lblCenter = new JLabel();
		lblCenter.setBounds(cx, cy, cw, ch);
		lblCenter.setOpaque(false);
		lblCenter.setVisible(false);
		try {
			ImageIcon rawCenter = new ImageIcon("Images/pnl_unselect.png");
			java.awt.Image cimg = rawCenter.getImage().getScaledInstance(cw, ch, java.awt.Image.SCALE_SMOOTH);
			lblCenter.setIcon(new ImageIcon(cimg));
		} catch (Exception ex) {
			// 이미지가 없거나 로드 실패하면 무시
		}
		// 몬스터 이미지를 올릴 라벨 (위에 올라오도록 별도 라벨로 추가)
		lblCenterMonster = new JLabel();
		lblCenterMonster.setBounds(cx, cy, cw, ch);
		lblCenterMonster.setOpaque(false);
		lblCenterMonster.setVisible(false);
		// select overlay 라벨 (버튼 위에 표시할 스프라이트)
		lblSelectOverlay = new JLabel();
		lblSelectOverlay.setOpaque(false);
		lblSelectOverlay.setVisible(false);
		add(lblCenter);
		add(lblCenterMonster);
		add(lblSelectOverlay);
		add(btnPokemon4);
		
		add(btnPokemon1);
		add(btnPokemon2);
		add(btnPokemon3);
		add(lblStatus);
		add(lblMyStatus);
		add(lblOpponentStatus);
		add(lblTimer);

		bindClickSfxToAllButtons();
		// 중앙 프리뷰가 버튼에 가려지지 않도록 최상위로 올립니다.
		try {
			// 우상단 타이머와 select overlay를 최상단으로 배치
			if (lblTimer != null) setComponentZOrder(lblTimer, 0);
			if (lblOpponentStatus != null) setComponentZOrder(lblOpponentStatus, 1);
			if (lblSelectOverlay != null) setComponentZOrder(lblSelectOverlay, 2);
			setComponentZOrder(lblCenterMonster, 3);
			setComponentZOrder(lblCenter, 4);
		} catch (Exception ex) {
			// 무시
		}

		// 상태 라벨 타이핑 애니메이션 시작
		startStatusTyping("포켓몬을 선택하세요.");

		// 랜덤 프리뷰 타이머 (빠르게 전환)
		randomPreviewTimer = new Timer(80, ev -> {
			randomIndex = (randomIndex + 1) % pokemonIds.length;
			// 직접 이미지 설정 (파일 경로는 species front 경로 사용)
			String id = pokemonIds[randomIndex];
			Pokemon p = PokemonRepository.getInstance().getById(id);
			if (p != null) {
				String front = p.getFrontImageFile();
				try {
					ImageIcon raw = new ImageIcon(front);
					int mw = 220, mh = 220;
					java.awt.Image mimg = raw.getImage().getScaledInstance(mw, mh, java.awt.Image.SCALE_SMOOTH);
					lblCenterMonster.setIcon(new ImageIcon(mimg));
					lblCenter.setVisible(true);
					lblCenterMonster.setVisible(true);
				} catch (Exception ex) {
					// ignore
				}
			}
		});

		// select sprite 로드 (Images/select.png, 4프레임 가로분할)
		try {
			BufferedImage selectSprite = ImageIO.read(new File("Images/select.png"));
			int frames = 4;
			int fw = selectSprite.getWidth() / frames;
			int fh = selectSprite.getHeight();
			selectFrames = new ImageIcon[frames];
			for (int i = 0; i < frames; i++) {
				BufferedImage sub = selectSprite.getSubimage(i * fw, 0, fw, fh);
				// 버튼 크기에 맞춰 스케일 (가로 -> 96)
				int targetW = 96;
				int targetH = (int) ((double) fh * targetW / fw);
				java.awt.Image scaled = sub.getScaledInstance(targetW, targetH, java.awt.Image.SCALE_SMOOTH);
				selectFrames[i] = new ImageIcon(scaled);
			}
		} catch (Exception ex) {
			// select sprite 로드 실패 시 무시
		}

		// 보장: 시작 시 상대 상태는 '상대를 찾는중...'으로 보이게 하고 최상단 근처로 올려둡니다.
		if (lblOpponentStatus != null) {
			lblOpponentStatus.setText("■ 상대를 찾는중...");
			lblOpponentStatus.setForeground(new Color(245, 245, 220));
			lblOpponentStatus.setVisible(true);
			try { setComponentZOrder(lblOpponentStatus, 1); } catch (Exception ex) {}
		}
	}

	private void bindClickSfxToAllButtons() {
		 java.awt.Container root = this; // 현재 패널
		 ButtonSfxBinder.bindAllButtons(root, bgmPlayer, "bgm/ding.wav", -10.0f);
	}

	/**
	 * 호버 시 중앙에 미리보기(포켓몬 전면 이미지)를 표시합니다.
	 */
	private void showPreviewFor(int pokemonNum) {
		if (pokemonNum < 1 || pokemonNum > pokemonIds.length) return;
		String id = pokemonIds[pokemonNum - 1];
		System.out.println("showPreviewFor called for pokemonNum=" + pokemonNum + ", id=" + id);
		Pokemon p = PokemonRepository.getInstance().getById(id);
		if (p == null) {
			System.out.println("PokemonRepository.getById returned null for id=" + id);
			return;
		}
		String front = p.getFrontImageFile();
		System.out.println("front image path: " + front);
		try {
			ImageIcon raw = new ImageIcon(front);
			int mw = 220, mh = 220;
			java.awt.Image mimg = raw.getImage().getScaledInstance(mw, mh, java.awt.Image.SCALE_SMOOTH);
			lblCenterMonster.setIcon(new ImageIcon(mimg));
			System.out.println("preview image set for id=" + id);
			// 가운데에 맞추기
			int cx = lblCenter.getX();
			int cy = lblCenter.getY();
			int cw = lblCenter.getWidth();
			int ch = lblCenter.getHeight();
			int mx = cx + (cw - mw) / 2;
			int my = cy + (ch - mh) / 2;
			lblCenterMonster.setBounds(mx, my, mw, mh);
			lblCenter.setVisible(true);
			lblCenterMonster.setVisible(true);
		} catch (Exception ex) {
			// 무시
		}
	}

	private void hidePreview() {
		lblCenterMonster.setVisible(false);
		lblCenter.setVisible(false);
	}

	private void showSelectOverlay(int pokemonNum) {
		if (selectFrames == null) return;
		if (pokemonNum < 1 || pokemonNum > selectFrames.length) return;
		// 버튼 위치에 맞춰 overlay 크기와 위치를 설정
		javax.swing.JButton target = null;
		switch (pokemonNum) {
			case 1: target = btnPokemon1; break;
			case 2: target = btnPokemon2; break;
			case 3: target = btnPokemon3; break;
			case 4: target = btnPokemon4; break;
		}
		if (target == null) return;
		int bw = target.getWidth();
		int bh = target.getHeight();
		int bx = target.getX();
		int by = target.getY() - 20;
		ImageIcon icon = selectFrames[pokemonNum - 1];
		int iw = icon.getIconWidth();
		int ih = icon.getIconHeight();
		// overlay는 버튼 위에 중앙 정렬, 약간 위로 띄워 표시
		int ox = bx + (bw - iw) / 2;
		int oy = by - ih + (bh / 2);
		lblSelectOverlay.setIcon(icon);
		lblSelectOverlay.setBounds(ox, oy, iw, ih);
		lblSelectOverlay.setVisible(true);
	}

	private void hideSelectOverlay() {
		if (lblSelectOverlay != null) lblSelectOverlay.setVisible(false);
	}

	/**
	 * 상태 라벨을 한 글자씩 표시하는 타이핑 애니메이션을 시작합니다.
	 */
	private void startStatusTyping(String message) {
		stopStatusTyping();
		typingTarget = (message == null) ? "" : message;
		typingIndex = 0;
		lblStatus.setText("");
		int delay = 40; // ms per character
		statusTypingTimer = new Timer(delay, ev -> {
			if (typingIndex <= typingTarget.length()) {
				typingIndex++;
				int end = Math.min(typingIndex, typingTarget.length());
				lblStatus.setText(typingTarget.substring(0, end));
				if (end >= typingTarget.length()) {
					stopStatusTyping();
				}
			}
		});
		statusTypingTimer.setInitialDelay(delay);
		statusTypingTimer.start();
	}

	private void stopStatusTyping() {
		if (statusTypingTimer != null && statusTypingTimer.isRunning()) {
			statusTypingTimer.stop();
		}
		statusTypingTimer = null;
	}

	private void startRandomPreview() {
		randomIndex = 0;
		if (randomPreviewTimer != null && !randomPreviewTimer.isRunning()) randomPreviewTimer.start();
	}

	private void stopRandomPreview() {
		if (randomPreviewTimer != null && randomPreviewTimer.isRunning()) randomPreviewTimer.stop();
		lblCenterMonster.setVisible(false);
		lblCenter.setVisible(false);
	}

	private void confirmAndSelectRandom() {
		int res = JOptionPane.showConfirmDialog(this,
				"랜덤 포켓몬을 선택하시겠습니까?",
				"랜덤 선택", JOptionPane.YES_NO_OPTION);
		if (res == JOptionPane.YES_OPTION) {
			// pick a random index among 0..pokemonIds.length-1
			int r = (int)(Math.random() * pokemonIds.length);
			selectPokemon(r + 1); // selectPokemon expects 1-based index
		}
	}

	/**
	 * 클릭 시 확인 후 선택 확정 처리
	 */
	private void confirmAndSelect(int pokemonNum) {
		if (pokemonNum < 1 || pokemonNum > pokemonNames.length) return;
		String pname = pokemonNames[pokemonNum - 1];
		int res = JOptionPane.showConfirmDialog(this,
				pname + "을(를) 선택하시겠습니까?",
				"포켓몬 선택", JOptionPane.YES_NO_OPTION);
		if (res == JOptionPane.YES_OPTION) {
			selectPokemon(pokemonNum); // 기존 selectPokemon 재사용
		}
	}

	
	private void loadBallSprite() {
		// ball 스프라이트 시트 로드 및 프레임 분할
		try {
			BufferedImage sprite = ImageIO.read(new File("Images/ball.png"));
			int frames = 4;
			int fw = sprite.getWidth() / frames;
			int fh = sprite.getHeight();
			ballFrames = new ImageIcon[frames];
			for (int i = 0; i < frames; i++) {
				BufferedImage sub = sprite.getSubimage(i * fw, 0, fw, fh);
				Image scaled = sub.getScaledInstance(96, 80, Image.SCALE_SMOOTH);
				ballFrames[i] = new ImageIcon(scaled);
			}
			// 초기 아이콘 할당
			btnPokemon1.setIcon(ballFrames[0]);
			btnPokemon2.setIcon(ballFrames[0]);
			btnPokemon3.setIcon(ballFrames[0]);
			// 랜덤 볼도 기본 아이콘 할당
			if (btnPokemon4 != null) btnPokemon4.setIcon(ballFrames[0]);
			// 비활성화 시에도 아이콘이 회색으로 변하지 않도록 disabledIcon을 동일하게 설정
			btnPokemon1.setDisabledIcon(ballFrames[0]);
			btnPokemon2.setDisabledIcon(ballFrames[0]);
			btnPokemon3.setDisabledIcon(ballFrames[0]);
			if (btnPokemon4 != null) btnPokemon4.setDisabledIcon(ballFrames[0]);
			// 애니메이션 타이머
				animTimer = new Timer(120, ev -> {
					animFrame = (animFrame + 1) % ballFrames.length;
					if (hover1) btnPokemon1.setIcon(ballFrames[animFrame]); else btnPokemon1.setIcon(ballFrames[0]);
					if (hover2) btnPokemon2.setIcon(ballFrames[animFrame]); else btnPokemon2.setIcon(ballFrames[0]);
					if (hover3) btnPokemon3.setIcon(ballFrames[animFrame]); else btnPokemon3.setIcon(ballFrames[0]);
					// 랜덤 볼 애니메이션 처리 (hover4에 따라)
					if (btnPokemon4 != null) {
						if (hover4) btnPokemon4.setIcon(ballFrames[animFrame]); else btnPokemon4.setIcon(ballFrames[0]);
					}
					// select overlay 애니메이션: hover 중이면 selectFrames를 animFrame으로 순환하여 표시
					if (selectFrames != null && !selectionLocked) {
						int active = 0;
						if (hover1) active = 1;
						else if (hover2) active = 2;
						else if (hover3) active = 3;
						else if (hover4) active = 4;
						if (active > 0) {
							int sf = animFrame % selectFrames.length;
							// 위치 재설정 (버튼에 맞춰)
							showSelectOverlay(active);
							lblSelectOverlay.setIcon(selectFrames[sf]);
							lblSelectOverlay.setVisible(true);
						} else {
							if (lblSelectOverlay != null) lblSelectOverlay.setVisible(false);
						}
					}
				});
			animTimer.start();
		} catch (Exception e) {
			btnPokemon1.setText(pokemonNames[0]);
			btnPokemon2.setText(pokemonNames[1]);
			btnPokemon3.setText(pokemonNames[2]);
		}
	}
	
	private void selectPokemon(int pokemonNum) {
		// 선택 확정 처리
		// 중간에 타이핑 애니메이션이 돌고 있으면 중지
		stopStatusTyping();
		selectedPokemon = pokemonNum;
		selectionLocked = true; // 이후 다른 볼은 반응하지 않음
		// stop any preview/timers that might still be running
		stopRandomPreview();
		if (animTimer != null && animTimer.isRunning()) animTimer.stop();
		// 즉시 서버에 준비 상태 전송
		sendReady();
			// 모든 몬스터볼 비활성화(선택 고정)
			if (btnPokemon1 != null) {
				btnPokemon1.setDisabledIcon((ImageIcon)btnPokemon1.getIcon());
				btnPokemon1.setEnabled(false);
			}
			if (btnPokemon2 != null) {
				btnPokemon2.setDisabledIcon((ImageIcon)btnPokemon2.getIcon());
				btnPokemon2.setEnabled(false);
			}
			if (btnPokemon3 != null) {
				btnPokemon3.setDisabledIcon((ImageIcon)btnPokemon3.getIcon());
				btnPokemon3.setEnabled(false);
			}
			if (btnPokemon4 != null) {
				btnPokemon4.setDisabledIcon((ImageIcon)btnPokemon4.getIcon());
				btnPokemon4.setEnabled(false);
			}
		// lblCenter를 선택 상태 이미지로 교체
		try {
			ImageIcon rawCenter = new ImageIcon("Images/pnl_select.png");
			int cw = lblCenter.getWidth();
			int ch = lblCenter.getHeight();
			java.awt.Image cimg = rawCenter.getImage().getScaledInstance(cw, ch, java.awt.Image.SCALE_SMOOTH);
			lblCenter.setIcon(new ImageIcon(cimg));
		} catch (Exception ex) {
			// 실패해도 무시
		}
		// 선택한 포켓몬의 전면 이미지를 계속 표시
		showPreviewFor(pokemonNum);
		// 다른 hover 플래그 제거
		hover1 = (pokemonNum == 1);
		hover2 = (pokemonNum == 2);
		hover3 = (pokemonNum == 3);
		hover4 = false;
		// overlay 숨김
		hideSelectOverlay();
	}
	
	private void sendReady() {
		if (selectedPokemon == 0) {
			JOptionPane.showMessageDialog(this,
					"포켓몬을 먼저 선택하세요.",
					"선택 오류", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		try {
			if (dos != null) {
				// 준비완료와 함께 선택한 포켓몬 정보 전송
				String pokemonName = pokemonNames[selectedPokemon - 1];
				dos.writeUTF("/ready " + pokemonName);
				myReady = true;
				// btnReady 제거됨
				lblMyStatus.setText("■ " + username + "(나): 포켓몬 선택 완료");
				lblMyStatus.setForeground(new Color(0, 190, 0)); // 초록
				startStatusTyping(pokemonName + "(이)가 당신의 파트너로 선택 되었습니다.");
				if (battleStartListener instanceof FrmSeverConnect) {
		            ((FrmSeverConnect) battleStartListener).pauseBgm(); 
		        }

		        // ready브금
		        bgmPlayer.playSfxOnce("bgm/ready.wav", -6.0f, () -> {
		            SwingUtilities.invokeLater(() -> {
		                // 전투 시작 전이면 메인 브금 재개
		                if (!gameStarting && battleStartListener instanceof FrmSeverConnect) {
		                    ((FrmSeverConnect) battleStartListener).resumeBgm();
		                }
		            });
		        });
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					"서버 통신 오류: " + e.getMessage(),
					"통신 오류", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	// sendStart removed — server will control game start via /start_game
	
	public void handleServerMessage(String message) {
	    // 서버로부터 받은 메시지 처리
	    message = message.trim();
	    System.out.println("[DEBUG from server] '" + message + "'");

	    // 0) 입장 브로드캐스트: "[이름]님이 입장 하였습니다."
	    if (message.startsWith("[") && message.contains("님이 입장 하였습니다.")) {
	        int left = message.indexOf('[');
	        int right = message.indexOf(']');

	        if (left >= 0 && right > left) {
	            String name = message.substring(left + 1, right).trim();

	            // 나 자신이면 상대로 취급 X
	            if (!name.equals(username)) {
	                opponentName = name;
	                opponentReady = false;

	                lblOpponentStatus.setText("■ " + opponentName + ": 포켓몬 선택중");
	                lblOpponentStatus.setForeground(new Color(245, 245, 220)); // 베이지
	            }
	        }
	        return;
	    }

	    if (message.startsWith("/opponent_info ")) {
	        String[] parts = message.split("\\s+");
	        if (parts.length >= 3) {
	            String name   = parts[1].trim();
	            String status = parts[2].trim();
	            //포켓몬은 안보

	            // 자기 자신이면 무시
	            if (name.equals(username)) return;

	            opponentName = name;

	            // 상대 포켓몬 이
	            if (parts.length >= 4) {
	                String enemyKorPokemon = parts[3].trim();  // 상대포켓몬 
	                opponentPokemonId = koreanNameToId(enemyKorPokemon); // "charmander" 등
	                System.out.println("[DEBUG] opponent_info 포켓몬: "
	                        + enemyKorPokemon + " -> id=" + opponentPokemonId);
	            }
	            
	            // 상대방 정보를 받았다 = 2명이 된 것 -> 15초 타이머 시작
	            if (waitingTimer != null && !waitingTimer.isRunning() && !myReady) {
	                waitingTimer.start();
	            }

	            if ("ready".equalsIgnoreCase(status)) {
	                opponentReady = true;
	                lblOpponentStatus.setText("■ " + opponentName + ": 포켓몬 선택 완료");
	                lblOpponentStatus.setForeground(new Color(0, 190, 0)); // 초록
	                if (myReady && opponentReady) {
	                    onBothReady();
	                }
	            } else {
	                opponentReady = false; 
	                lblOpponentStatus.setText("■ " + opponentName + ": 포켓몬 선택중");
	                lblOpponentStatus.setForeground(new Color(245, 245, 220)); // 베이지
	            }
	        }
	        return;
	    }

	    // 2) 상대 준비완료 신호
	    //  이름만 상태에 적
	    if (message.startsWith("/opponent_ready")) {
	        opponentReady = true;
	        
	        //포켓몬 한글 이름 파
	        String[] parts = message.split("\\s+", 2);
	        if (parts.length >= 2) {
	            String enemyKorName = parts[1].trim();   // "꼬부기"
	            opponentPokemonId = koreanNameToId(enemyKorName);  // "squirtle"
	            System.out.println("[DEBUG] opponent_ready 포켓몬: " + enemyKorName
	                    + " -> id=" + opponentPokemonId);
	        }
	        
	        String displayName = (opponentName == null || opponentName.isBlank())
	                ? "상대"
	                : opponentName;

	        lblOpponentStatus.setText("■ " + displayName + ": 포켓몬 선택 완료");
	        lblOpponentStatus.setForeground(new Color(0, 190, 0)); // 초록

	        if (myReady && opponentReady) {
	            onBothReady();
	        }
	        return;
	    }

	    // 3) 브로드캐스트 형식 준비완료:
	    //    여기서도 포켓몬 이름은 적용 안
	    if (message.startsWith("[") && message.contains("님이 준비완료했습니다.")) {
	        int left = message.indexOf('[');
	        int right = message.indexOf(']');
	        if (left >= 0 && right > left) {
	            String name = message.substring(left + 1, right).trim();
	            
	            // 포켓몬 한글이름 파싱: "포켓몬: 꼬부기" -> "꼬부기" 만 뽑
	            String enemyKorPokemon = null;
	            int idx = message.indexOf("포켓몬:");
	            if (idx >= 0) {
	                int start = idx + "포켓몬:".length();
	                int end = message.indexOf(')', start);
	                if (end < 0) end = message.length();
	                enemyKorPokemon = message.substring(start, end).trim();
	            }

	            // 내 이름이면 무시
	            if (name.equals(username)) return;

	            opponentName = name;
	            opponentReady = true;
	            
	            if (enemyKorPokemon != null && !enemyKorPokemon.isBlank()) {
	                opponentPokemonId = koreanNameToId(enemyKorPokemon);
	                System.out.println("[DEBUG] broadcast ready 포켓몬: "
	                        + enemyKorPokemon + " -> id=" + opponentPokemonId);
	            }
	            
	            lblOpponentStatus.setText("■ " + opponentName + ": 포켓몬 선택 완료");
	            lblOpponentStatus.setForeground(new Color(0, 190, 0)); // 초록

	            if (myReady && opponentReady) {
	            	onBothReady();
	            }
	        }
	        return;
	    }

	    // 4) 제한시간 숫자: "/time <seconds>" -> 우상단 라벨 업데이트
	    if (message.startsWith("/time ")) {
	        String[] parts = message.split(" ", 2);
	        if (parts.length >= 2) {
	            String t = parts[1].trim();
	            try {
	                Integer.parseInt(t); // 숫자인지만 체크
	                lblTimer.setText(t);
	            } catch (NumberFormatException ex) {
	                // 무시
	            }
	        }
	        return;
	    }

	    // 5) 게임 시작 신호
	    if (message.startsWith("/start_game")) {
	    	// 배경 번호 추출 (예: "/start_game 3")
	    	String[] parts = message.split(" ");
	    	if (parts.length >= 2) {
	    		try {
	    			backgroundNumber = Integer.parseInt(parts[1]);
	    		} catch (NumberFormatException e) {
	    			backgroundNumber = 1; // 기본값
	    		}
	    	} else {
	    		backgroundNumber = 1; // 기본값
	    	}
	    	if (gameStarting) return;
	        gameStarting = true;
	        startGameCountdown();
	        return;
	    }

    // 6) 퇴장 메시지: "[이름]님이 퇴장 하였습니다."
    if (message.contains("님이 퇴장 하였습니다.")) {
        // 배틀 중이면 상대방 연결 끊김 처리
        if (frmParent != null && frmParent.battlePanel != null) {
            frmParent.battlePanel.handleOpponentDisconnect();
        }
        return;
    }

    // 그 외 메시지 (로그용)
	    System.out.println("[INFO] Unhandled message: " + message);
	}

	public void disconnect() {
	    try {
	        if (dos != null) dos.writeUTF("/exit");
	    } catch (IOException ignore) {}
	    stopService(); // 여기서 close까지 다 함
	}

	
	// 서버와 통신하는 스레드
	class ClientService extends Thread {
	    @Override
	    public void run() {
	        try {
	            while (running) {
	                String msg = dis.readUTF();

	                java.util.function.Consumer<String> handler = messageHandler;
	                if (handler != null) {
	                    javax.swing.SwingUtilities.invokeLater(() -> handler.accept(msg));
	                } else {
	                    // 핸들러 없으면 기본은 waitingroom이 처리
	                    javax.swing.SwingUtilities.invokeLater(() -> handleServerMessage(msg));
	                }
	            }
	        } catch (IOException e) {
	            if (!running) return;
	            javax.swing.SwingUtilities.invokeLater(() -> {
	                if (frmParent != null && frmParent.battlePanel != null) {
	                    frmParent.battlePanel.handleOpponentDisconnect();
	                } else {
	                    JOptionPane.showMessageDialog(
	                        JplWaitingRoom.this,
	                        "서버와의 연결이 끊어졌습니다.",
	                        "연결 오류",
	                        JOptionPane.ERROR_MESSAGE
	                    );
	                }
	            });
	        }
	    }
	}

	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (bg != null) {
			int panelW = getWidth();
			int panelH = getHeight();
			if (panelW <= 0) panelW = w;
			if (panelH <= 0) panelH = h;
			g.drawImage(bg, 0, 0, panelW, panelH, this);
		}
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(w, h);
	}
	private void onBothReady() {
	    // 여기서는 그냥 라벨/문구만…
	    stopStatusTyping();
	    startStatusTyping("양쪽 모두 준비완료! 서버에서 시작 신호를 기다리는 중...");
	}

	private void startGameCountdown() {
	    // 이미 카운트다운 중이면 중복 시작 방지
	    if (startCountdownTimer != null && startCountdownTimer.isRunning()) {
	        return;
	    }

	    // 상태 문구 갱신
	    stopStatusTyping();
	    startStatusTyping("양쪽 모두 준비완료! 3초 후에 게임을 시작합니다...");

	    // 3초부터 시작
	    startCountdown = 3;
	    lblTimer.setText(String.valueOf(startCountdown));

	    startCountdownTimer = new Timer(1000, e -> {
	        startCountdown--;

	        if (startCountdown <= 0) {  
	            startCountdownTimer.stop();
	            lblTimer.setText("0");
	            bgmPlayer.playLoop("bgm/pokemon-battle.wav", -12.0f);
	            if (battleStartListener instanceof FrmSeverConnect) {
	                ((FrmSeverConnect) battleStartListener).stopBgm();
	            }
				if (battleStartListener != null) {
					String myPokemonId = null;
					if (selectedPokemon >= 1 && selectedPokemon <= pokemonIds.length) {
						myPokemonId = pokemonIds[selectedPokemon - 1];
					}
					// 여기서 상대 포켓몬 id와 배경 번호까지 넘김
					battleStartListener.onBattleStartRequest(
							username,
							myPokemonId,
							opponentPokemonId,
							opponentName,
							socket,
							dis,
							dos,
							backgroundNumber,
							this
					);	            
				}
	        } else {
	            lblTimer.setText(String.valueOf(startCountdown));
	        }
	    });

	    // 1초 뒤에 첫 감소(3을 1초 보여주고 → 2 → 1 → 0)
	    startCountdownTimer.setInitialDelay(1000);
	    startCountdownTimer.start();
	}
	private String koreanNameToId(String korName) {
	    if (korName == null || korName.isBlank()) return null;

	    // 1) 지금 쓰는 배열 기반 매핑
	    for (int i = 0; i < pokemonNames.length; i++) {
	        if (korName.equals(pokemonNames[i])) {
	            return pokemonIds[i];
	        }
	    }
	    return null;
	}

	public void setMessageHandler(java.util.function.Consumer<String> handler) {
	    this.messageHandler = handler;
	}

	public void stopService() {
	    running = false;
	    try { if (dis != null) dis.close(); } catch (Exception ignore) {}
	    try { if (dos != null) dos.close(); } catch (Exception ignore) {}
	    try { if (socket != null) socket.close(); } catch (Exception ignore) {}

	    if (clientService != null) {
	        clientService.interrupt();
	    }
	}

	public void stopBattleBgm() {
		bgmPlayer.stopBgm();
	}

	public BgmPlayer getBgmPlayer() {
		return bgmPlayer;
	}

}

