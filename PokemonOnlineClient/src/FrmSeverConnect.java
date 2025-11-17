import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

public class FrmSeverConnect extends JFrame {

	private static final long serialVersionUID = 1L;
    
	//배경 이미지 저장할 변수와 가로, 세로 길이
	private Image bg;
	private int w;
	private int h;
	private final JTextField txtIP = new JTextField();
	private final JTextField txtPort = new JTextField();
	private final JTextField txtName = new JTextField();
	private final JLabel lblIP = new JLabel("IP: ");
	private final JLabel lblPort = new JLabel("Port: ");
	private final JLabel lblName = new JLabel("Name: ");
	private final JButton btnConnect = new JButton("Connect");
	private final JButton btnExit = new JButton("Exit");
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					FrmSeverConnect frame = new FrmSeverConnect();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public FrmSeverConnect() {
		txtIP.setColumns(10);
		// 텍스트필드를 투명하게 하고 테두리 제거
		txtIP.setOpaque(false);
		txtIP.setBorder(null);
		txtIP.setFont(new Font("PF Stardust Bold", Font.BOLD, 28));
		// 입력 제한: IP 형식에 맞는 문자만 허용하고, 완성된 IP는 포커스 아웃 시 검증
		((AbstractDocument) txtIP.getDocument()).setDocumentFilter(new IPDocumentFilter());
		txtIP.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusLost(java.awt.event.FocusEvent e) {
				String s = txtIP.getText().trim();
				if (!s.isEmpty() && !isValidFullIP(s)) {
					JOptionPane.showMessageDialog(FrmSeverConnect.this,
							"유효한 IP 형식이 아닙니다. 예: 192.168.0.1",
							"입력 오류", JOptionPane.WARNING_MESSAGE);
					txtIP.requestFocus();
				}
			}
		});
		
		txtPort.setColumns(10);
		// 텍스트필드를 투명하게 하고 테두리 제거
		txtPort.setOpaque(false);
		txtPort.setBorder(null);
		txtPort.setFont(new Font("PF Stardust Bold", Font.BOLD, 28));
		// 포트는 숫자만 허용 (최대 5자리)
		((AbstractDocument) txtPort.getDocument()).setDocumentFilter(new NumericDocumentFilter(5));
		txtPort.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusLost(java.awt.event.FocusEvent e) {
				String s = txtPort.getText().trim();
				if (!s.isEmpty() && !isValidPort(s)) {
					JOptionPane.showMessageDialog(FrmSeverConnect.this,
							"유효한 포트 범위가 아닙니다. 1-65535 사이의 숫자를 입력하세요.",
							"입력 오류", JOptionPane.WARNING_MESSAGE);
					txtPort.requestFocus();
				}
			}
		});
		
		txtName.setColumns(10);
		// 텍스트필드를 투명하게 하고 테두리 제거
		txtName.setOpaque(false);
		txtName.setBorder(null);
		txtName.setFont(new Font("PF Stardust Bold", Font.BOLD, 28));
		
		setTitle("Pokemon Online");
		setIconImage(Toolkit.getDefaultToolkit().getImage("Images/icon.png"));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		ImageIcon bgImage = new ImageIcon("Images/bg_main.png");
		//가로 세로길이 얻기
		w = bgImage.getIconWidth();
		h = bgImage.getIconHeight();
		bg = bgImage.getImage();

		ImagePanel PnlBackGround = new ImagePanel();
		// 절대 위치 레이아웃 사용하여 이미지 위에 컴포넌트 배치
		PnlBackGround.setLayout(null);
		// 클래스 필드 textField 사용하여 추가
		// 라벨과 텍스트필드 폰트 통일
		Font fieldFont = txtIP.getFont();
		lblIP.setFont(fieldFont);
		lblPort.setFont(fieldFont);
		lblName.setFont(fieldFont);
		lblIP.setOpaque(false);
		lblPort.setOpaque(false);
		lblName.setOpaque(false);

		// 위치: 라벨은 왼쪽에, 텍스트필드는 오른쪽에
		lblIP.setBounds(313, 400, 100, 36);
		txtIP.setBounds(373, 400, 350, 36);
		lblPort.setBounds(313, 480, 100, 36);
		txtPort.setBounds(398, 480, 315, 36);
		lblName.setBounds(313, 560, 100, 36);
		txtName.setBounds(406, 560, 307, 36);

		btnConnect.setFont(fieldFont);
	    btnConnect.setBounds(313, 630, 190, 50);
	    
	    btnExit.setFont(fieldFont);
	    btnExit.setBounds(523, 630, 190, 50); 
	    
	    MyAction action = new MyAction();
	    btnConnect.addActionListener(action);
	    txtIP.addActionListener(action);
	    txtPort.addActionListener(action);
	    txtName.addActionListener(action);
	    
	    // 나가기 버튼 액션 (그냥 프로그램 종료)
	    btnExit.addActionListener(e -> {
	        // 전체 프로그램 종료
	        System.exit(0);
	    });
	    
		PnlBackGround.add(lblIP);
		PnlBackGround.add(txtIP);
		PnlBackGround.add(lblPort);
		PnlBackGround.add(txtPort);
		PnlBackGround.add(lblName);
		PnlBackGround.add(txtName);
		PnlBackGround.add(btnConnect);
		PnlBackGround.add(btnExit); 
		setContentPane(PnlBackGround);
		pack();
		setLocationRelativeTo(null);
		//setVisible(true);
		
	}
	class ImagePanel extends JPanel {
		// 이건 이클립스 경고 메세지를 없애기 위한 버전
		private static final long serialVersionUID = 1L;

		// 다시 그리는 paintComponent를 오버라이드
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(bg, 0, 0, w, h, this);
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(w , h);
		}
	}

	// DocumentFilter: IP 입력을 부분적으로 검증하여 숫자와 마침표(.)만 허용하고
	// 각 세그먼트가 0~255 범위, 길이는 0~3자리를 넘지 않도록 제어합니다 (부분 입력 허용).
	class IPDocumentFilter extends DocumentFilter {
		private boolean isValidPartial(String s) {
			if (s == null) return false;
			// 허용 문자만
			if (!s.matches("[0-9.]*")) return false;
			// 마침표로 분리
			String[] parts = s.split("\\.", -1);
			if (parts.length > 4) return false;
			for (int i = 0; i < parts.length; i++) {
				String p = parts[i];
				// 중간 세그먼트(마지막이 아닌데) 비어있으면 허용하지 않음
				if (i < parts.length - 1 && p.length() == 0) return false;
				if (p.length() > 3) return false;
				if (p.length() > 0) {
					try {
						int v = Integer.parseInt(p);
						if (v < 0 || v > 255) return false;
					} catch (NumberFormatException ex) {
						return false;
					}
				}
			}
			return true;
		}

		@Override
		public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
				throws BadLocationException {
			StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
			sb.insert(offset, string);
			if (isValidPartial(sb.toString())) {
				super.insertString(fb, offset, string, attr);
			}
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
				throws BadLocationException {
			StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
			sb.replace(offset, offset + length, text == null ? "" : text);
			if (isValidPartial(sb.toString())) {
				super.replace(fb, offset, length, text, attrs);
			}
		}

		@Override
		public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
			StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
			sb.delete(offset, offset + length);
			if (isValidPartial(sb.toString())) {
				super.remove(fb, offset, length);
			}
		}
	}

	// 숫자 전용 DocumentFilter (포트 입력용) - 숫자만 허용, 길이 최대 5
	class NumericDocumentFilter extends DocumentFilter {
		private final int maxLength;
		NumericDocumentFilter(int maxLength) { this.maxLength = maxLength; }

		private boolean isValid(String s) {
			if (s == null) return false;
			if (!s.matches("\\d*")) return false;
			if (s.length() > maxLength) return false;
			return true;
		}

		@Override
		public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
			StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
			sb.insert(offset, string);
			if (isValid(sb.toString())) super.insertString(fb, offset, string, attr);
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
			StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
			sb.replace(offset, offset + length, text == null ? "" : text);
			if (isValid(sb.toString())) super.replace(fb, offset, length, text, attrs);
		}

		@Override
		public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
			StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
			sb.delete(offset, offset + length);
			if (isValid(sb.toString())) super.remove(fb, offset, length);
		}
	}

	// 포트 범위 검증(포커스 아웃 시)
	private boolean isValidPort(String s) {
		if (s == null || s.isEmpty()) return false;
		try {
			int p = Integer.parseInt(s);
			return p >= 1 && p <= 65535;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	// 포커스 아웃 시 전체 IP 형식 검증
	private boolean isValidFullIP(String s) {
		if (s == null) return false;
		// 완전한 IPv4 정규식
		String ipRegex = "^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)$";
		return Pattern.matches(ipRegex, s);
	}
	class MyAction implements ActionListener {
	    @Override
	    public void actionPerformed(ActionEvent e) {
	        String username = txtName.getText().trim();
	        String ip = txtIP.getText().trim();
	        String port = txtPort.getText().trim();

	        // 간단 입력 체크 (필요하면 더 추가)
	        if (username.isEmpty() || ip.isEmpty() || port.isEmpty()) {
	            JOptionPane.showMessageDialog(
	                FrmSeverConnect.this,
	                "IP, Port, Name을 모두 입력해 주세요.",
	                "입력 오류",
	                JOptionPane.WARNING_MESSAGE
	            );
	            return;
	        }

	        // 이미 만든 유효성 검사 재활용 가능
	        if (!isValidFullIP(ip)) {
	            JOptionPane.showMessageDialog(
	                FrmSeverConnect.this,
	                "유효한 IP 형식이 아닙니다. 예: 192.168.0.1",
	                "입력 오류",
	                JOptionPane.WARNING_MESSAGE
	            );
	            txtIP.requestFocus();
	            return;
	        }

	        if (!isValidPort(port)) {
	            JOptionPane.showMessageDialog(
	                FrmSeverConnect.this,
	                "유효한 포트 범위가 아닙니다. 1-65535 사이의 숫자를 입력하세요.",
	                "입력 오류",
	                JOptionPane.WARNING_MESSAGE
	            );
	            txtPort.requestFocus();
	            return;
	        }
	        //포켓몬 고르기 + 대기창
	        //WaitingRoomFrame waiting = new WaitingRoomFrame(username, ip, port);
	        //waiting.setVisible(true);

	        // 이 창은 숨기거나 종료
	        // setVisible(false);
	        dispose(); // 자원까지 정리하고 싶으면 이쪽이 더 깔끔
	    }
	}


}
