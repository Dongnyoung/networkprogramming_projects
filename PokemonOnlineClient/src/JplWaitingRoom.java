import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

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
	
	// 선택된 포켓몬 (1, 2, 3 중 하나, 0은 미선택)
	private int selectedPokemon = 0;
	
	// 준비완료 버튼
	private JButton btnReady;
	
	// 게임 시작 버튼
	private JButton btnStart;
	
	// 상태 표시 라벨
	private JLabel lblStatus;
	private JLabel lblMyStatus;
	private JLabel lblOpponentStatus;
	
	// 서버 통신 관련
	private Socket socket;
	private DataInputStream dis;
	private DataOutputStream dos;
	private ClientService clientService;
	private String username;
	private String opponentName = "";
	private boolean myReady = false;
	private boolean opponentReady = false;
	
	// 포켓몬 이름들 (이미지 파일명과 매칭)
	private String[] pokemonNames = {"이상해씨", "파이리", "꼬부기"};
	
	public JplWaitingRoom(String username, String ip, String port) {
		this.username = username;
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
			clientService.start();
			
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					"서버 연결에 실패했습니다: " + e.getMessage(),
					"연결 오류", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		
		// 폰트 설정
		Font fieldFont = new Font("PF Stardust Bold", Font.BOLD, 24);
		
		// 포켓몬 선택 버튼들 생성 (가로로 배치)
		btnPokemon1 = new JButton();
		btnPokemon1.setBounds(200, 300, 150, 150);
		btnPokemon1.setOpaque(false);
		btnPokemon1.setContentAreaFilled(false);
		btnPokemon1.setBorderPainted(false);
		btnPokemon1.addActionListener(e -> selectPokemon(1));
		
		btnPokemon2 = new JButton();
		btnPokemon2.setBounds(400, 300, 150, 150);
		btnPokemon2.setOpaque(false);
		btnPokemon2.setContentAreaFilled(false);
		btnPokemon2.setBorderPainted(false);
		btnPokemon2.addActionListener(e -> selectPokemon(2));
		
		btnPokemon3 = new JButton();
		btnPokemon3.setBounds(600, 300, 150, 150);
		btnPokemon3.setOpaque(false);
		btnPokemon3.setContentAreaFilled(false);
		btnPokemon3.setBorderPainted(false);
		btnPokemon3.addActionListener(e -> selectPokemon(3));
		
		// 포켓몬 이미지 로드
		loadPokemonImages();
		
		// 준비완료 버튼
		btnReady = new JButton("준비완료");
		btnReady.setFont(fieldFont);
		btnReady.setBounds(350, 500, 150, 50);
		btnReady.setEnabled(false);
		btnReady.addActionListener(e -> sendReady());
		
		// 게임 시작 버튼
		btnStart = new JButton("게임 시작");
		btnStart.setFont(fieldFont);
		btnStart.setBounds(350, 570, 150, 50);
		btnStart.setEnabled(false);
		btnStart.addActionListener(e -> sendStart());
		
		// 상태 표시 라벨
		lblStatus = new JLabel("포켓몬을 선택하세요");
		lblStatus.setFont(fieldFont);
		lblStatus.setBounds(300, 200, 400, 40);
		lblStatus.setOpaque(false);
		
		lblMyStatus = new JLabel("나: "+ username+"(대기중)");
		lblMyStatus.setFont(new Font("PF Stardust Bold", Font.BOLD, 20));
		lblMyStatus.setBounds(200, 100, 300, 30);
		lblMyStatus.setOpaque(false);
		
		lblOpponentStatus = new JLabel("상대방: 대기중");
		lblOpponentStatus.setFont(new Font("PF Stardust Bold", Font.BOLD, 20));
		lblOpponentStatus.setBounds(500, 100, 300, 30);
		lblOpponentStatus.setOpaque(false);
		
		add(btnPokemon1);
		add(btnPokemon2);
		add(btnPokemon3);
		add(btnReady);
		add(btnStart);
		add(lblStatus);
		add(lblMyStatus);
		add(lblOpponentStatus);
	}
	
	private void loadPokemonImages() {
		try {
			ImageIcon icon1 = new ImageIcon("Images/Isanghaessi.png");
			if (icon1.getIconWidth() != -1) {
				Image img1 = icon1.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
				btnPokemon1.setIcon(new ImageIcon(img1));
			} else {
				btnPokemon1.setText(pokemonNames[0]);
			}
			
			ImageIcon icon2 = new ImageIcon("Images/fire.png");
			if (icon2.getIconWidth() != -1) {
				Image img2 = icon2.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
				btnPokemon2.setIcon(new ImageIcon(img2));
			} else {
				btnPokemon2.setText(pokemonNames[1]);
			}
			
			ImageIcon icon3 = new ImageIcon("Images/GGoboogie.png");
			if (icon3.getIconWidth() != -1) {
				Image img3 = icon3.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
				btnPokemon3.setIcon(new ImageIcon(img3));
			} else {
				btnPokemon3.setText(pokemonNames[2]);
			}
		} catch (Exception e) {
			// 이미지 로드 실패 시 텍스트로 표시
			btnPokemon1.setText(pokemonNames[0]);
			btnPokemon2.setText(pokemonNames[1]);
			btnPokemon3.setText(pokemonNames[2]);
		}
	}
	
	private void selectPokemon(int pokemonNum) {
		selectedPokemon = pokemonNum;
		
		// 선택된 포켓몬 강조 (버튼 테두리 표시)
		btnPokemon1.setBorderPainted(selectedPokemon == 1);
		btnPokemon2.setBorderPainted(selectedPokemon == 2);
		btnPokemon3.setBorderPainted(selectedPokemon == 3);
		
		// 준비완료 버튼 활성화
		btnReady.setEnabled(true);
		lblStatus.setText(pokemonNames[pokemonNum - 1] + " 선택됨");
		
		// 포켓몬 선택 정보는 준비완료 버튼을 눌렀을 때 서버로 전송
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
				btnReady.setEnabled(false);
				lblMyStatus.setText("나: 준비완료 (" + pokemonName + ")");
				lblStatus.setText("준비완료! 상대방을 기다리는 중...");
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					"서버 통신 오류: " + e.getMessage(),
					"통신 오류", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	private void sendStart() {
		try {
			if (dos != null) {
				dos.writeUTF("/start");
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					"서버 통신 오류: " + e.getMessage(),
					"통신 오류", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	public void handleServerMessage(String message) {
		// 서버로부터 받은 메시지 처리
		message = message.trim();
		
		if (message.startsWith("/opponent_info")) {
			// 서버가 기존 참가자 정보를 알려줌
			String[] parts = message.split(" ", 4);
			if (parts.length >= 3) {
				opponentName = parts[1];
				String status = parts[2];
				String pokemon = (parts.length >= 4) ? parts[3] : "-";
				if ("ready".equalsIgnoreCase(status)) {
					opponentReady = true;
					if (!pokemon.equals("-")) {
						lblOpponentStatus.setText("상대방: 준비완료 (" + pokemon + ")");
					} else {
						lblOpponentStatus.setText("상대방: 준비완료");
					}
				} else {
					opponentReady = false;
					if (!pokemon.equals("-")) {
						lblOpponentStatus.setText("상대방: " + opponentName + " (선택: " + pokemon + ")");
					} else {
						lblOpponentStatus.setText("상대방: " + opponentName + " (대기중)");
					}
				}
			}
		} else if (message.startsWith("[")) {
			// 일반 채팅 메시지 (입장/퇴장 등)
			if (message.contains("입장")) {
				// 상대방 입장 메시지 파싱
				int startIdx = message.indexOf("[");
				int endIdx = message.indexOf("]");
				if (startIdx != -1 && endIdx != -1) {
					String name = message.substring(startIdx + 1, endIdx);
					if (!name.equals(username)) {
						opponentName = name;
						lblOpponentStatus.setText("상대방: " + opponentName + " (대기중)");
					}
				}
			} else if (message.contains("퇴장")) {
				opponentReady = false;
				lblOpponentStatus.setText("상대방: 퇴장");
				btnStart.setEnabled(false);
			} else if (message.contains("선택했습니다") || message.contains("선택하였습니다")) {
				// 상대방 포켓몬 선택 메시지 파싱
				int startIdx = message.indexOf("[");
				int endIdx = message.indexOf("]");
				if (startIdx != -1 && endIdx != -1) {
					String name = message.substring(startIdx + 1, endIdx);
					if (!name.equals(username)) {
						// 상대방이 포켓몬을 선택했다는 정보 표시
						lblOpponentStatus.setText("상대방: " + name + " (포켓몬 선택 완료)");
					}
				}
			}
		} else if (message.startsWith("/opponent_ready")) {
			// 상대방 준비완료
			opponentReady = true;
			// 포켓몬 정보도 함께 받기
			String[] parts = message.split(" ");
			if (parts.length >= 2) {
				String opponentPokemon = parts[1].trim();
				lblOpponentStatus.setText("상대방: 준비완료 (" + opponentPokemon + ")");
			} else {
				lblOpponentStatus.setText("상대방: " + opponentName + " (준비완료)");
			}
			
			// 양쪽 모두 준비완료면 게임 시작 버튼 활성화
			if (myReady && opponentReady) {
				btnStart.setEnabled(true);
				lblStatus.setText("양쪽 모두 준비완료! 게임을 시작할 수 있습니다.");
			}
		} else if (message.startsWith("/start_game")) {
			// 게임 시작 신호 (서버에서 보냄)
			// TODO: 게임 화면으로 전환
			JOptionPane.showMessageDialog(this,
					"게임을 시작합니다!",
					"게임 시작", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	public void disconnect() {
		try {
			if (dos != null) {
				dos.writeUTF("/exit");
			}
			if (dis != null) dis.close();
			if (dos != null) dos.close();
			if (socket != null) socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// 서버와 통신하는 스레드
	class ClientService extends Thread {
		@Override
		public void run() {
			try {
				while (true) {
					String msg = dis.readUTF();
					// Swing 이벤트 스레드에서 UI 업데이트
					javax.swing.SwingUtilities.invokeLater(() -> {
						handleServerMessage(msg);
					});
				}
			} catch (IOException e) {
				javax.swing.SwingUtilities.invokeLater(() -> {
					JOptionPane.showMessageDialog(JplWaitingRoom.this,
							"서버 연결이 끊어졌습니다.",
							"연결 종료", JOptionPane.ERROR_MESSAGE);
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
}

