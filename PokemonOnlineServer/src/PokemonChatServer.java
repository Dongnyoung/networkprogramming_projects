import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class PokemonChatServer extends JFrame {

    private static final long serialVersionUID = 1L;

    private JPanel contentPane;
    JTextArea textArea;
    private JTextField txtPortNumber;

    private ServerSocket socket; // 서버소켓
    private boolean isServerRunning = false; // 서버 실행 상태 플래그

    private Socket client_socket; // accept() 에서 생성된 client 소켓

    private Vector<UserService> UserVec = new Vector<>(); // 연결된 사용자를 저장할 벡터

    private static final int BUF_LEN = 128; // Windows 처럼 BUF_LEN 을 정의

    public PokemonChatServer() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 338, 386);

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(12, 10, 300, 244);
        contentPane.add(scrollPane);

        textArea = new JTextArea();
        textArea.setEditable(false);
        scrollPane.setViewportView(textArea);

        JLabel lblNewLabel = new JLabel("Port Number");
        lblNewLabel.setBounds(12, 264, 87, 26);
        contentPane.add(lblNewLabel);

        txtPortNumber = new JTextField();
        txtPortNumber.setHorizontalAlignment(SwingConstants.CENTER);
        txtPortNumber.setText("7777");
        txtPortNumber.setBounds(111, 264, 199, 26);
        contentPane.add(txtPortNumber);
        txtPortNumber.setColumns(10);

        JButton btnServerStart = new JButton("Server Start");
        btnServerStart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startServer();
            }
        });
        btnServerStart.setBounds(12, 300, 300, 35);
        contentPane.add(btnServerStart);

        // GUI가 완전히 로드된 후 서버 자동 시작
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                startServer();
                btnServerStart.setText("Chat Server Running..");
                btnServerStart.setEnabled(false);
                txtPortNumber.setEnabled(false);
            }
        });
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    PokemonChatServer frame = new PokemonChatServer();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // UserVec getter
    public Vector<UserService> getUserVec() {
        return UserVec;
    }

    // 서버 시작 메서드
    private void startServer() {
        if (isServerRunning) {
            AppendText("서버가 이미 실행 중입니다.");
            return;
        }

        try {
            int port = Integer.parseInt(txtPortNumber.getText());
            socket = new ServerSocket(port);
            isServerRunning = true;
            AppendText("Chat Server Running on port " + port + "..");
            AcceptServer accept_server = new AcceptServer();   // 멀티 스레드 객체 생성
            accept_server.start();
        } catch (NumberFormatException e1) {
            AppendText("서버 시작 실패: 잘못된 포트 번호입니다. " + e1.getMessage());
            e1.printStackTrace();
        } catch (IOException e1) {
            AppendText("서버 시작 실패: " + e1.getMessage());
            isServerRunning = false;
            e1.printStackTrace();
        }
    }

    // 새로운 참가자 accept() 하고 user thread를 새로 생성한다.
    class AcceptServer extends Thread {
        public void run() {
            while (isServerRunning && socket != null && !socket.isClosed()) {
                try {
                    AppendText("Waiting clients ...");
                    client_socket = socket.accept(); // accept가 일어나기 전까지는 무한 대기중
                    AppendText("새로운 참가자 from " + client_socket);

                    // User 당 하나씩 Thread 생성
                    UserService new_user = new UserService(client_socket, PokemonChatServer.this);
                    UserVec.add(new_user);
                    AppendText("사용자 입장. 현재 참가자 수 " + UserVec.size());
                    new_user.start();
                } catch (IOException e) {
                    if (isServerRunning) {
                        AppendText("!!!! accept 에러 발생... !!!!");
                    }
                    break;
                }
            }
            AppendText("AcceptServer 스레드가 종료되었습니다.");
        }
    }

    public void AppendText(String str) {
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            textArea.append(str + "\n");
            textArea.setCaretPosition(textArea.getText().length());
        } else {
            final String message = str;
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    textArea.append(message + "\n");
                    textArea.setCaretPosition(textArea.getText().length());
                }
            });
        }
    }
}
