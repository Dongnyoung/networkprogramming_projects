import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

//임시버전
public class JplBattlePanel extends JPanel {

    private Pokemon myPokemon;
    private String opponentName;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    public JplBattlePanel(Pokemon myPokemon,
                          String opponentName,
                          Socket socket,
                          DataInputStream dis,
                          DataOutputStream dos) {

        this.myPokemon = myPokemon;
        this.opponentName = opponentName;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;

        setBackground(Color.BLACK);
        setLayout(new BorderLayout());

        JLabel label = new JLabel(
                "<html><body style='text-align:center;'>"
                        + "배틀 화면!<br/>"
                        + "내 포켓몬 index: " + myPokemon.getKoreanName() + "<br/>"
                        + "상대: " + opponentName
                        + "</body></html>",
                SwingConstants.CENTER
        );
        label.setForeground(Color.WHITE);
        label.setFont(new Font("PF Stardust Bold", Font.BOLD, 30));

        add(label, BorderLayout.CENTER);
    }
}
