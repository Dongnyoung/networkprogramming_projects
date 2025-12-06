import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.awt.Color;
import java.awt.Cursor;
import java.net.Socket;
import java.net.InetSocketAddress;
import javax.swing.SwingWorker;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.SwingUtilities;

public class FrmSeverConnect extends JFrame implements BattleStartListener{

    private static final long serialVersionUID = 1L;

    // 배경 이미지 저장할 변수와 가로, 세로 길이
    private Image bg;
    private int w;
    private int h;

    private final JTextField txtIP = new JTextField();
    private final JTextField txtPort = new JTextField();
    private final JTextField txtName = new JTextField();
    private final JLabel lblIP = new JLabel("IP: ");
    private final JLabel lblPort = new JLabel("Port: ");
    private final JLabel lblName = new JLabel("Name: ");
    private final JButton btnConnect = new JButton("접속");
    private final JButton btnExit = new JButton("나가기");
    private final JButton btnTest = new JButton("테스트");

    // 현재 화면 패널 / 대기실 패널
    private JPanel currentPanel;
    private JplWaitingRoom waitingPanel;   // JPanel 버전의 대기실

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

    // 파일 경로로부터 이미지 아이콘을 읽어 버튼 크기에 맞게 스케일해서 반환
    private ImageIcon createScaledIcon(String filePath, int width, int height) {
        try {
            ImageIcon raw = new ImageIcon(filePath);
            Image img = raw.getImage();
            Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + filePath);
            return null;
        }
    }

    // (이전의 Auto 버튼 처리 코드는 제거됨)

    // 선호 로컬 IPv4를 결정: 우선 순위 -> Wi-Fi/Ethernet(이름에 wifi/wlan/wi/eth/ethernet 포함), site-local, 그 외
    private String getPreferredLocalIPv4() {
        try {
            // 첫번째로 인터페이스명/표시명에 특정 키워드가 포함된 것
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            List<String> siteLocals = new ArrayList<>();
            List<String> others = new ArrayList<>();
            while (nets.hasMoreElements()) {
                NetworkInterface ni = nets.nextElement();
                try {
                    if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                } catch (Exception ex) {
                    continue;
                }
                String name = ni.getName() == null ? "" : ni.getName().toLowerCase();
                String dname = ni.getDisplayName() == null ? "" : ni.getDisplayName().toLowerCase();
                boolean isPreferredIfName = name.contains("wi") || name.contains("wlan") || name.contains("wifi") || name.contains("eth") || dname.contains("wi") || dname.contains("wireless") || dname.contains("wifi") || dname.contains("ethernet");

                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        String ip = addr.getHostAddress();
                        if (isPreferredIfName) return ip; // 우선 반환
                        if (addr.isSiteLocalAddress()) siteLocals.add(ip);
                        else others.add(ip);
                    }
                }
            }
            if (!siteLocals.isEmpty()) return siteLocals.get(0);
            if (!others.isEmpty()) return others.get(0);
        } catch (Exception e) {
            // 무시
        }
        return null;
    }

    /**
     * Create the frame.
     */
    public FrmSeverConnect() {
        // 앱 시작 시 기본 포켓몬 종들을 로드(전역 레포지토리)
        try { PokemonRepository.getInstance().loadDefaults(); } catch (Throwable t) { /* 무시 */ }
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
        // 가로 세로길이 얻기
        w = bgImage.getIconWidth();
        h = bgImage.getIconHeight();
        bg = bgImage.getImage();

        ImagePanel PnlBackGround = new ImagePanel();
        // 절대 위치 레이아웃 사용하여 이미지 위에 컴포넌트 배치
        PnlBackGround.setLayout(null);
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
        txtIP.setBounds(373, 400, 405, 36);
        lblPort.setBounds(313, 480, 100, 36);
        txtPort.setBounds(398, 480, 315, 36);
        lblName.setBounds(313, 560, 100, 36);
        txtName.setBounds(406, 560, 307, 36);

        btnConnect.setFont(fieldFont);
        btnConnect.setBounds(523, 624, 208, 68);
        // 플랫 스타일: 배경/테두리 제거, 포커스 표시 제거, 커서 변경
        btnConnect.setContentAreaFilled(false);
        btnConnect.setBorderPainted(false);
        btnConnect.setFocusPainted(false);
        btnConnect.setOpaque(false);
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnExit.setFont(fieldFont);
        btnExit.setBounds(296, 624, 208, 68);
        // 플랫 스타일
        btnExit.setContentAreaFilled(false);
        btnExit.setBorderPainted(false);
        btnExit.setFocusPainted(false);
        btnExit.setOpaque(false);
        btnExit.setForeground(Color.WHITE);
        btnExit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnTest.setFont(fieldFont);
        btnTest.setBounds(750, 624, 208, 68);
        // 플랫 스타일
        btnTest.setContentAreaFilled(false);
        btnTest.setBorderPainted(false);
        btnTest.setFocusPainted(false);
        btnTest.setOpaque(false);
        btnTest.setForeground(Color.WHITE);
        btnTest.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 버튼 아이콘(Images/btn_up.png) 적용 - 버튼 크기에 맞춰 스케일
        int btnW = 208;
        int btnH = 68;
        ImageIcon iconUp = createScaledIcon("Images/btn_up.png", btnW, btnH);
        ImageIcon iconDown = createScaledIcon("Images/btn_down.png", btnW, btnH); // rollover/pressed
        if (iconUp != null) {
            // 기본 아이콘 및 텍스트 설정
            btnConnect.setIcon(iconUp);
            btnConnect.setText("접속");
            btnConnect.setHorizontalTextPosition(SwingConstants.CENTER);
            btnConnect.setVerticalTextPosition(SwingConstants.CENTER);
            btnConnect.setIconTextGap(0);
            btnConnect.setFont(fieldFont.deriveFont(Font.BOLD, 28f));
            btnConnect.setForeground(Color.BLACK);
            btnConnect.setRolloverEnabled(true);
            if (iconDown != null) {
                btnConnect.setRolloverIcon(iconDown);
                btnConnect.setPressedIcon(iconDown);
            }

            btnExit.setIcon(iconUp);
            btnExit.setText("나가기");
            btnExit.setHorizontalTextPosition(SwingConstants.CENTER);
            btnExit.setVerticalTextPosition(SwingConstants.CENTER);
            btnExit.setIconTextGap(0);
            btnExit.setFont(fieldFont.deriveFont(Font.BOLD, 28f));
            btnExit.setForeground(Color.BLACK);
            btnExit.setRolloverEnabled(true);
            if (iconDown != null) {
                btnExit.setRolloverIcon(iconDown);
                btnExit.setPressedIcon(iconDown);
            }

            btnTest.setIcon(iconUp);
            btnTest.setText("테스트");
            btnTest.setHorizontalTextPosition(SwingConstants.CENTER);
            btnTest.setVerticalTextPosition(SwingConstants.CENTER);
            btnTest.setIconTextGap(0);
            btnTest.setFont(fieldFont.deriveFont(Font.BOLD, 28f));
            btnTest.setForeground(Color.BLACK);
            btnTest.setRolloverEnabled(true);
            if (iconDown != null) {
                btnTest.setRolloverIcon(iconDown);
                btnTest.setPressedIcon(iconDown);
            }
        }

        // 자동 IP 선택: 생성시 자동으로 로컬 LAN 주소를 채웁니다.
        String preferred = getPreferredLocalIPv4();
        if (preferred != null) txtIP.setText(preferred);

        MyAction action = new MyAction();
        btnConnect.addActionListener(action);
        txtIP.addActionListener(action);
        txtPort.addActionListener(action);
        txtName.addActionListener(action);

        // 나가기 버튼 액션 (그냥 프로그램 종료)
        btnExit.addActionListener(e -> {
            System.exit(0);
        });

        // 테스트 버튼 액션 (몬스터볼 모션 및 배틀 화면 테스트)
        btnTest.addActionListener(e -> {
            // 임의의 포켓몬 2마리로 배틀 시작 squirtle / bulbasaur / charmander
            onBattleStartRequest("bulbasaur", "bulbasaur", "테스트상대", null, null, null);
        });

        PnlBackGround.add(lblIP);
        PnlBackGround.add(txtIP);
        PnlBackGround.add(lblPort);
        PnlBackGround.add(txtPort);
        PnlBackGround.add(lblName);
        PnlBackGround.add(txtName);
        PnlBackGround.add(btnConnect);
        PnlBackGround.add(btnExit);
        PnlBackGround.add(btnTest);
        // (Auto 버튼 제거 — 자동으로 초기값을 채움)

        currentPanel = PnlBackGround;     // 현재 화면
        setContentPane(currentPanel);
        pack();
        setLocationRelativeTo(null);
    }

    class ImagePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(bg, 0, 0, w, h, this);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(w, h);
        }
    }

    // DocumentFilter: IP 입력을 부분적으로 검증
    class IPDocumentFilter extends DocumentFilter {
        private boolean isValidPartial(String s) {
            if (s == null) return false;
            if (!s.matches("[0-9.]*")) return false;
            String[] parts = s.split("\\.", -1);
            if (parts.length > 4) return false;
            for (int i = 0; i < parts.length; i++) {
                String p = parts[i];
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
        String ipRegex = "^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)$";
        return Pattern.matches(ipRegex, s);
    }

    class MyAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String username = txtName.getText().trim();
            String ip = txtIP.getText().trim();
            String port = txtPort.getText().trim();

            // 간단 입력 체크
            if (username.isEmpty() || ip.isEmpty() || port.isEmpty()) {
                JOptionPane.showMessageDialog(
                        FrmSeverConnect.this,
                        "IP, Port, Name을 모두 입력해 주세요.",
                        "입력 오류",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

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

            // 실제로 서버에 연결 가능한지 백그라운드에서 확인한 뒤 성공 시에만 대기실로 전환
            btnConnect.setEnabled(false);
            txtIP.setEnabled(false);
            txtPort.setEnabled(false);
            txtName.setEnabled(false);

            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    int p;
                    try {
                        p = Integer.parseInt(port);
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                    try (Socket sock = new Socket()) {
                        sock.connect(new InetSocketAddress(ip, p), 2000); // 2초 타임아웃
                        return true;
                    } catch (Exception ex) {
                        return false;
                    }
                }

                @Override
                protected void done() {
                    boolean ok = false;
                    try {
                        ok = get();
                    } catch (Exception ex) {
                        ok = false;
                    }
                    btnConnect.setEnabled(true);
                    txtIP.setEnabled(true);
                    txtPort.setEnabled(true);
                    txtName.setEnabled(true);

                    if (ok) {
                        // 연결 성공: 대기실로 전환
                        waitingPanel = new JplWaitingRoom(username, ip, port, FrmSeverConnect.this);
                        setContentPane(waitingPanel);
                        pack();
                        setLocationRelativeTo(null);
                        revalidate();
                        repaint();
                    } else {
                        JOptionPane.showMessageDialog(FrmSeverConnect.this,
                                "서버에 연결할 수 없습니다. IP와 포트가 올바르고 서버가 실행 중인지 확인하세요.",
                                "연결 실패", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();

         
        }
    }

    @Override
    public void onBattleStartRequest(String myPokemonId, 
    								 String enemyPokemonId,
                                     String opponentName,
                                     Socket socket,
                                     DataInputStream dis,
                                     DataOutputStream dos) {

        System.out.println("[DEBUG] 배틀 시작 요청: selectedPokemon="
                + myPokemonId + ", opponent=" + opponentName);

        Pokemon myPokemon = PokemonRepository.getInstance().getById(myPokemonId);
        Pokemon enemyPokemon =PokemonRepository.getInstance().getById(enemyPokemonId);
        if (myPokemon == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "선택한 포켓몬 데이터를 찾을 수 없습니다: " + myPokemonId,
                    "데이터 오류",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        // 1) 몬스터볼 연출 패널 생성 (배열로 감싸서 람다에서 접근 가능하도록)
        final JplBallTransition[] transitionPanelHolder = new JplBallTransition[1];
        
        transitionPanelHolder[0] = new JplBallTransition(() -> {
            // 이 Runnable은 "연출이 완전히 끝난 뒤"에 호출됨

            // 2) 여기서 배틀 패널 생성 후 화면 전환
            SwingUtilities.invokeLater(() -> {
                // 먼저 트랜지션 패널 제거
                if (transitionPanelHolder[0] != null) {
                    getLayeredPane().remove(transitionPanelHolder[0]);
                }
                
                JplBattlePanel battlePanel = new JplBattlePanel(
                        myPokemon,
                        enemyPokemon,
                        opponentName,
                        socket,
                        dis,
                        dos
                );

                setContentPane(battlePanel);
                setLocationRelativeTo(null);
                revalidate();
                repaint();
            });
        });

        // 3) LayeredPane을 사용하여 현재 패널 위에 오버레이
        transitionPanelHolder[0].setBounds(0, 0, getWidth(), getHeight());
        getLayeredPane().add(transitionPanelHolder[0], JLayeredPane.POPUP_LAYER);
        revalidate();
        repaint();
    }

}
