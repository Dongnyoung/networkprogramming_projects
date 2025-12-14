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
    private boolean validUser = false;   // 로그인 성공한 진짜 유저만 true

    private Vector<UserService> user_vc; // 제네릭 타입 사용
    private PokemonChatServer server;    // 서버 객체 참조

    String UserName = "";
    String selectedPokemon = ""; // 선택한 포켓몬
    boolean isReady = false;     // 준비 상태
    
    // 배틀 관련 변수
    private String battlePokemonId = "";
    private String battleSkillName = ""; // 기술 이름
    private String battleSkillType = ""; // 기술 타입
    private String battlePokemonType1 = ""; // 포켓몬 타입1
    private String battlePokemonType2 = ""; // 포켓몬 타입2 (없으면 "")
    private int battleSkillPower = 0;
    private double battleSkillAccuracy = 1.0;
    private int battleAttack = 0;
    private int battleDefense = 0;
    private int battleSpeed = 0;
    private int battleLevel = 50;
    private boolean battleDataReceived = false;

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
            // 제일 처음 연결되면 "/login UserName" 또는 "/probe" 가 들어올 수 있음
            String line1 = dis.readUTF().trim();

            if (line1.equals("/probe")) {
                server.AppendText("-----------");
                server.AppendText("[PROBE] 서버 연결 테스트 접속 확인 (" + client_socket.getInetAddress() + ")");
                server.AppendText("-----------");
                try { client_socket.close(); } catch (Exception ignore) {}
                return; // validUser는 false 그대로
            }


            //  2) 정상 로그인 처리
            String[] msg = line1.split(" ");
            if (msg.length < 2 || !msg[0].equals("/login")) {
                server.AppendText("[WARN] 잘못된 첫 메시지: " + line1);
                try { client_socket.close(); } catch (Exception ignore) {}
                return;
            }

            // 3) 인원 제한 체크 (2명까지만)
            if (user_vc.size() >= 2) {
                server.AppendText("[WARN] 접속 인원 초과 - 연결 거부");
                try {
                    dos.writeUTF("/server_full");
                    dos.flush();
                    Thread.sleep(100); // 메시지 전송 대기
                } catch (Exception ignore) {}
                try { client_socket.close(); } catch (Exception ignore) {}
                return;
            }

            UserName = msg[1].trim();
            validUser = true;    
            user_vc.add(this);
            server.AppendText("새로운 참가자 " + UserName + " 입장.");
            server.AppendText("사용자 입장. 현재 참가자 수 " + user_vc.size());
            // 이미 접속 중인 사용자 정보를 새 참가자에게 전달
            for (int i = 0; i < user_vc.size(); i++) {
            	UserService existing = user_vc.get(i);
                if (existing == this) continue;

                String status = existing.isReady ? "ready" : "waiting";
                String pokemonInfo = (existing.selectedPokemon == null || existing.selectedPokemon.isEmpty())
                        ? "-" : existing.selectedPokemon;

                // 새로 들어온 나에게: 기존 유저들 정보
                WriteOne("/opponent_info " + existing.UserName + " " + status + " " + pokemonInfo);

                // 기존 유저들에게: 새 유저(나) 정보
                existing.WriteOne("/opponent_info " + UserName + " waiting -");

            }
            

            String br_msg = "[" + UserName + "]님이 입장 하였습니다.\n";
            WriteAll(br_msg); // 아직 user_vc에 본인은 포함되지 않음
        } catch (Exception e) {
        	server.AppendText("[UserService 생성자] error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            try { client_socket.close(); } catch (Exception ignore) {}
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
    
    // 배틀 처리: 두 플레이어의 스킬 선택이 모두 완료되면 계산
    private void processBattle() {
        if (user_vc.size() != 2) {
            server.AppendText("[배틀] 플레이어 수 부족: " + user_vc.size());
            return;
        }
        
        UserService user1 = user_vc.get(0);
        UserService user2 = user_vc.get(1);
        
        // 두 사용자 모두 배틀 데이터를 보냈는지 확인
        if (!user1.battleDataReceived || !user2.battleDataReceived) {
            server.AppendText("[배틀] 대기중 - user1: " + user1.battleDataReceived + ", user2: " + user2.battleDataReceived);
            return;
        }
        
        server.AppendText("=== 배틀 처리 시작 ===");
        server.AppendText("[배틀] " + user1.UserName + " vs " + user2.UserName);
        
        // 스탯 정보 로그
        server.AppendText("[" + user1.UserName + "] 스탯 - 공:" + user1.battleAttack + " 방:" + user1.battleDefense + " 속:" + user1.battleSpeed);
        server.AppendText("[" + user1.UserName + "] 스킬 - 위력:" + user1.battleSkillPower + " 명중:" + user1.battleSkillAccuracy);
        server.AppendText("[" + user2.UserName + "] 스탯 - 공:" + user2.battleAttack + " 방:" + user2.battleDefense + " 속:" + user2.battleSpeed);
        server.AppendText("[" + user2.UserName + "] 스킬 - 위력:" + user2.battleSkillPower + " 명중:" + user2.battleSkillAccuracy);
        
        // 선공 결정: 스피드 비교 (같으면 랜덤)
        boolean user1First;
        if (user1.battleSpeed > user2.battleSpeed) {
            user1First = true;
        } else if (user2.battleSpeed > user1.battleSpeed) {
            user1First = false;
        } else {
            user1First = Math.random() < 0.5;
        }
        server.AppendText("[배틀] 선공: " + (user1First ? user1.UserName : user2.UserName) + " (속도: " + user1.battleSpeed + " vs " + user2.battleSpeed + ")");
        
        // 타입 상성 계산
        double typeEffectiveness1to2 = getTypeEffectiveness(user1.battleSkillType, user2.battlePokemonType1, user2.battlePokemonType2);
        double typeEffectiveness2to1 = getTypeEffectiveness(user2.battleSkillType, user1.battlePokemonType1, user1.battlePokemonType2);
        
        server.AppendText("[배틀] 타입 상성 - " + user1.battleSkillType + " → (" + user2.battlePokemonType1 + "," + user2.battlePokemonType2 + "): " + typeEffectiveness1to2 + "배");
        server.AppendText("[배틀] 타입 상성 - " + user2.battleSkillType + " → (" + user1.battlePokemonType1 + "," + user1.battlePokemonType2 + "): " + typeEffectiveness2to1 + "배");
        
        // 데미지 계산 (타입 상성 포함)
        int damage1to2 = calculateDamage(user1.battleLevel, user1.battleSkillPower, 
                                        user1.battleAttack, user2.battleDefense, typeEffectiveness1to2);
        int damage2to1 = calculateDamage(user2.battleLevel, user2.battleSkillPower, 
                                        user2.battleAttack, user1.battleDefense, typeEffectiveness2to1);
        
        server.AppendText("[배틀] 데미지 계산 - " + user1.UserName + "→" + user2.UserName + ": " + damage1to2);
        server.AppendText("[배틀] 데미지 계산 - " + user2.UserName + "→" + user1.UserName + ": " + damage2to1);
        
        // 명중 판정
        boolean hit1 = Math.random() < user1.battleSkillAccuracy;
        boolean hit2 = Math.random() < user2.battleSkillAccuracy;
        
        server.AppendText("[배틀] 명중 판정 - " + user1.UserName + ": " + (hit1 ? "명중" : "빗나감"));
        server.AppendText("[배틀] 명중 판정 - " + user2.UserName + ": " + (hit2 ? "명중" : "빗나감"));
        
        // 빗나가면 데미지 0
        if (!hit1) damage1to2 = 0;
        if (!hit2) damage2to1 = 0;
        
        // 결과 전송: "선공유저명\n유저1닉네임,skillName,damage,hit\n유저2닉네임,skillName,damage,hit"
        // damage는 각 유저가 **준** 데미지
        String firstUser = user1First ? user1.UserName : user2.UserName;
        String result = "/battle_result " + firstUser + "\n";
        result += user1.UserName + "," + user1.battleSkillName + "," + damage1to2 + "," + (hit1 ? "1" : "0") + "\n";
        result += user2.UserName + "," + user2.battleSkillName + "," + damage2to1 + "," + (hit2 ? "1" : "0");
        
        server.AppendText("[배틀] 최종 결과 전송: " + result.replace("\n", " | "));
        
        // 두 클라이언트에게 결과 전송
        user1.WriteOne(result + "\n");
        user2.WriteOne(result + "\n");

        // 배틀 데이터 초기화
        user1.battleDataReceived = false;
        user1.battlePokemonId = "";
        user1.battleSkillName = "";
        user1.battleSkillType = "";
        user1.battlePokemonType1 = "";
        user1.battlePokemonType2 = "";
        user1.battleSkillPower = 0;
        user1.battleSkillAccuracy = 1.0;
        user1.battleAttack = 0;
        user1.battleDefense = 0;
        user1.battleSpeed = 0;
        user1.battleLevel = 50;
        
        user2.battleDataReceived = false;
        user2.battlePokemonId = "";
        user2.battleSkillName = "";
        user2.battleSkillType = "";
        user2.battlePokemonType1 = "";
        user2.battlePokemonType2 = "";
        user2.battleSkillPower = 0;
        user2.battleSkillAccuracy = 1.0;
        user2.battleAttack = 0;
        user2.battleDefense = 0;
        user2.battleSpeed = 0;
        user2.battleLevel = 50;
        
        server.AppendText("=== 배틀 처리 완료 ===");
    }
    
    // 데미지 계산 공식: (((2×level÷5+2) × power × (attack/defense)) ÷ 50 + 2) × modifier × typeEffectiveness
    private int calculateDamage(int level, int power, int attack, int defense, double typeEffectiveness) {
        double baseDamage = (((2.0 * level / 5.0 + 2) * power * ((double)attack / defense)) / 50.0 + 2);
        
        // 보정: 0.8 ~ 1.2 랜덤
        double modifier = 0.8 + (Math.random() * 0.4);
        
        // 타입 상성 적용
        int damage = (int)(baseDamage * modifier * typeEffectiveness);
        
        // 타입 상성이 0이면 데미지 0, 아니면 최소 1
        return typeEffectiveness == 0 ? 0 : Math.max(1, damage);
    }
    
    // 타입 인덱스 매핑
    private static final String[] TYPE_NAMES = {
        "NORMAL", "FIRE", "WATER", "GRASS", "ELECTRIC", "ICE",
        "FIGHTING", "POISON", "GROUND", "FLYING", "PSYCHIC", "BUG",
        "ROCK", "GHOST", "DRAGON", "DARK", "STEEL", "FAIRY"
    };
    
    // 타입 상성표 [공격타입][방어타입] = 배율
    private static final double[][] TYPE_CHART = {
        // 공격: 노말
        {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 0.0, 1.0, 1.0, 0.5, 1.0},
        // 공격: 불꽃
        {1.0, 0.5, 0.5, 2.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 0.5, 1.0, 0.5, 1.0, 2.0, 1.0},
        // 공격: 물
        {1.0, 2.0, 0.5, 0.5, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 2.0, 1.0, 0.5, 1.0, 1.0, 1.0},
        // 공격: 풀
        {1.0, 0.5, 2.0, 0.5, 1.0, 1.0, 1.0, 0.5, 2.0, 0.5, 1.0, 0.5, 2.0, 1.0, 0.5, 1.0, 0.5, 1.0},
        // 공격: 전기
        {1.0, 1.0, 2.0, 0.5, 0.5, 1.0, 1.0, 1.0, 0.0, 2.0, 1.0, 1.0, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0},
        // 공격: 얼음
        {1.0, 0.5, 0.5, 2.0, 1.0, 0.5, 1.0, 1.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 0.5, 1.0},
        // 공격: 격투
        {2.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 0.5, 1.0, 0.5, 0.5, 0.5, 2.0, 0.0, 1.0, 2.0, 2.0, 0.5},
        // 공격: 독
        {1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 0.5, 0.5, 1.0, 1.0, 1.0, 0.5, 0.5, 1.0, 1.0, 0.0, 2.0},
        // 공격: 땅
        {1.0, 2.0, 1.0, 0.5, 2.0, 1.0, 1.0, 2.0, 1.0, 0.0, 1.0, 0.5, 2.0, 1.0, 1.0, 1.0, 2.0, 1.0},
        // 공격: 비행
        {1.0, 1.0, 1.0, 2.0, 0.5, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 2.0, 0.5, 1.0, 1.0, 1.0, 0.5, 1.0},
        // 공격: 에스퍼
        {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0, 1.0, 0.0, 0.5, 1.0},
        // 공격: 벌레
        {1.0, 0.5, 1.0, 2.0, 1.0, 1.0, 0.5, 0.5, 1.0, 0.5, 2.0, 1.0, 1.0, 0.5, 1.0, 2.0, 0.5, 0.5},
        // 공격: 바위
        {1.0, 2.0, 1.0, 1.0, 1.0, 2.0, 0.5, 1.0, 0.5, 2.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 0.5, 1.0},
        // 공격: 고스트
        {0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 2.0, 1.0, 0.5, 1.0, 1.0},
        // 공격: 드래곤
        {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 0.5, 0.0},
        // 공격: 악
        {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 2.0, 1.0, 0.5, 1.0, 0.5},
        // 공격: 강철
        {1.0, 0.5, 0.5, 1.0, 0.5, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 0.5, 2.0},
        // 공격: 페어리
        {1.0, 0.5, 1.0, 1.0, 1.0, 1.0, 2.0, 0.5, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 0.5, 1.0}
    };
    
    // 타입 상성 계산
    private double getTypeEffectiveness(String attackType, String defenseType1, String defenseType2) {
        double multiplier1 = getTypeSingle(attackType, defenseType1);
        double multiplier2 = defenseType2.isEmpty() ? 1.0 : getTypeSingle(attackType, defenseType2);
        return multiplier1 * multiplier2;
    }
    
    // 단일 타입 상성 (2차원 배열 기반)
    private double getTypeSingle(String attackType, String defenseType) {
        if (attackType.isEmpty() || defenseType.isEmpty()) return 1.0;
        
        int attackIndex = getTypeIndex(attackType.toUpperCase());
        int defenseIndex = getTypeIndex(defenseType.toUpperCase());
        
        if (attackIndex == -1 || defenseIndex == -1) return 1.0;
        
        return TYPE_CHART[attackIndex][defenseIndex];
    }
    
    // 타입 이름으로 인덱스 찾기
    private int getTypeIndex(String typeName) {
        for (int i = 0; i < TYPE_NAMES.length; i++) {
            if (TYPE_NAMES[i].equals(typeName)) {
                return i;
            }
        }
        return -1; // 없으면 -1
    }

    @Override
    public void run() {
    	if(!validUser) return; //probe 또는 오류  연결은 스레드 즉시종료 
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
                   
                    case "/chat": {
                        String body = (msg.length() >= 6) ? msg.substring(6).trim() : "";
                        if (!body.isEmpty()) {
                            WriteAll("/chat [" + UserName + "] " + body); // \n 붙이지 말기 추천
                        }
                        break;
                    }

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

                    case "/battle": // 배틀 스킬 사용
                        // 프로토콜: /battle pokemonId skillName skillType pokemonType1 pokemonType2 skillPower skillAccuracy attack defense speed level
                        if (args.length >= 12) {
                            battlePokemonId = args[1];
                            battleSkillName = args[2];
                            battleSkillType = args[3];
                            battlePokemonType1 = args[4];
                            battlePokemonType2 = args[5];
                            battleSkillPower = Integer.parseInt(args[6]);
                            battleSkillAccuracy = Double.parseDouble(args[7]);
                            battleAttack = Integer.parseInt(args[8]);
                            battleDefense = Integer.parseInt(args[9]);
                            battleSpeed = Integer.parseInt(args[10]);
                            battleLevel = Integer.parseInt(args[11]);
                            battleDataReceived = true;
                            
                            server.AppendText(UserName + "님이 스킬 사용 (" + battleSkillName + "[" + battleSkillType + "], 위력: " + battleSkillPower + ")");
                            
                            // 두 명의 플레이어가 모두 스킬을 선택했는지 확인
                            processBattle();
                        }
                        break;

                    case "/battle_end": // 배틀 종료 신호
                        // 배틀 관련 변수 초기화
                        battleDataReceived = false;
                        battlePokemonId = "";
                        battleSkillName = "";
                        battleSkillType = "";
                        battlePokemonType1 = "";
                        battlePokemonType2 = "";
                        battleSkillPower = 0;
                        battleSkillAccuracy = 1.0;
                        battleAttack = 0;
                        battleDefense = 0;
                        battleSpeed = 0;
                        battleLevel = 50;
                        server.AppendText(UserName + "님의 배틀 종료");
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
