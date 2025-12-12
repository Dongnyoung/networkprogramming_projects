import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Vector;

public class UserService extends Thread {

    private InputStream is;
    private OutputStream os;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Socket client_socket;

    private Vector<UserService> user_vc; // 제네릭 타입 사용
    private PokemonChatServer server;    // 서버 객체 참조

    String UserName = "";
    String selectedPokemon = ""; // 선택한 포켓몬
    boolean isReady = false;     // 준비 상태

    public UserService(Socket client_socket, PokemonChatServer server) {
        this.client_socket = client_socket;
        this.server = server;
        this.user_vc = server.getUserVec();

        try {
            is = client_socket.getInputStream();
            dis = new DataInputStream(is);
            os = client_socket.getOutputStream();
            dos = new DataOutputStream(os);

            // 제일 처음 연결되면 "/login UserName" 문자열이 들어옴
            String line1 = dis.readUTF();
            String[] msg = line1.split(" ");
            UserName = msg[1].trim();

            server.AppendText("새로운 참가자 " + UserName + " 입장.");

            // 이미 접속 중인 사용자 정보를 새 참가자에게 전달
            for (int i = 0; i < user_vc.size(); i++) {
                UserService existing = user_vc.get(i);
                String status = existing.isReady ? "ready" : "waiting";
                String pokemonInfo =
                        (existing.selectedPokemon == null || existing.selectedPokemon.isEmpty())
                                ? "-" : existing.selectedPokemon;
                WriteOne("/opponent_info " + existing.UserName + " " + status + " " + pokemonInfo + "\n");
                existing.WriteOne("/opponent_info " + UserName + " waiting -\n");

            }
            

            String br_msg = "[" + UserName + "]님이 입장 하였습니다.\n";
            WriteAll(br_msg); // 아직 user_vc에 본인은 포함되지 않음
        } catch (Exception e) {
            server.AppendText("userService error");
        }
    }

    public void logout() {
        user_vc.removeElement(this);
        String br_msg = "[" + UserName + "]님이 퇴장 하였습니다.\n";
        WriteAll(br_msg);
        server.AppendText("사용자 퇴장. 현재 참가자 수 " + user_vc.size());
    }

    // 클라이언트로 메시지 전송
    public void WriteOne(String msg) {
        try {
            dos.writeUTF(msg);
        } catch (IOException e) {
            server.AppendText("dos.write() error");
            try {
                dos.close();
                dis.close();
                client_socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            logout();
        }
    }

    // 모든 다중 클라이언트에게 순차적으로 채팅 메시지 전달
    public void WriteAll(String str) {
        for (int i = 0; i < user_vc.size(); i++) {
            UserService user = user_vc.get(i);
            user.WriteOne(str);
        }
    }

    // 다른 사용자에게만 메시지 전송 (자신 제외)
    public void WriteOthers(String str) {
        for (int i = 0; i < user_vc.size(); i++) {
            UserService user = user_vc.get(i);
            if (user != this) {
                user.WriteOne(str);
            }
        }
    }

    // 모든 사용자가 준비완료인지 확인
    private boolean allUsersReady() {
        if (user_vc.size() < 2) return false; // 최소 2명 필요
        for (int i = 0; i < user_vc.size(); i++) {
            UserService user = user_vc.get(i);
            if (!user.isReady) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String msg = dis.readUTF();
                msg = msg.trim();
                server.AppendText(msg);

                String[] args = msg.split(" ");
                if (args.length < 1) continue;

                switch (args[0]) {
                    case "/exit": // 종료 명령
                        logout();
                        return;

                    case "/list": // 접속자 목록 보기
                        WriteOne("**현재 사용자 목록**\n");
                        for (int i = 0; i < user_vc.size(); i++) {
                            UserService user = user_vc.get(i);
                            WriteOne("- " + user.UserName + "\n");
                        }
                        break;

                    case "/to": // 귓속말 처리, [홍길동] /to 신데렐라 안녕~ 반갑다. ^^
                        if (args.length < 4) {
                            WriteOne("사용법: /to [username] [message]\n");
                            break;
                        }
                        String targetUser = args[2];
                        String privateMessage = "";
                        for (int i = 3; i < args.length; i++) {
                            privateMessage += args[i];
                            if (i < args.length - 1) privateMessage += " ";
                        }
                        boolean found = false;
                        for (int i = 0; i < user_vc.size(); i++) {
                            UserService user = user_vc.get(i);
                            if (user.UserName.equals(targetUser)) {
                                user.WriteOne("[" + UserName + "님의 귓속말] " + privateMessage + "\n");
                                WriteOne("[" + UserName + "님의 귓속말] " + privateMessage + "\n");
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            WriteOne("사용자 " + targetUser + "를 찾을 수 없습니다.\n");
                        }
                        break;

                    case "/pokemon": // 포켓몬 선택
                        if (args.length >= 2) {
                            selectedPokemon = args[1];
                            String pokemonMsg =
                                    "[" + UserName + "]님이 " + selectedPokemon + "을 선택했습니다.\n";
                            WriteOthers(pokemonMsg);
                            server.AppendText(UserName + "님이 " + selectedPokemon + "을 선택했습니다.");
                        }
                        break;

                    case "/ready": // 준비완료
                        isReady = true;
                        if (args.length >= 2) {
                            selectedPokemon = args[1];
                            String readyMsg =
                                    "[" + UserName + "]님이 준비완료했습니다. (포켓몬: " + selectedPokemon + ")\n";
                            WriteOthers(readyMsg);
                            WriteOthers("/opponent_ready " + selectedPokemon + "\n");
                            server.AppendText(
                                    UserName + "님이 준비완료했습니다. (포켓몬: " + selectedPokemon + ")");
                        } else {
                            WriteOthers("/opponent_ready\n");
                            server.AppendText(UserName + "님이 준비완료했습니다.");
                        }

                        if (allUsersReady()) {
                            // 배경 번호 랜덤 생성 (1~4)
                            int bgNumber = 1 + (int)(Math.random() * 4);
                            WriteAll("/start_game " + bgNumber + "\n");
                            server.AppendText("모든 사용자가 준비완료! 게임을 시작합니다. (배경: " + bgNumber + ")");
                        }
                        break;

                    default: // 일반 메시지 처리
                        WriteAll(msg + "\n");
                        break;
                }
            } catch (IOException e) {
                server.AppendText("dis.readUTF() error");
                try {
                    dos.close();
                    dis.close();
                    client_socket.close();
                    logout();
                    break;
                } catch (Exception ee) {
                    break;
                }
            }
        }
    }
}
