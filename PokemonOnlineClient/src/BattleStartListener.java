import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public interface BattleStartListener {
    void onBattleStartRequest(String myPokemonId, String enemyPokemonId,String opponentName,
                              Socket socket, DataInputStream dis, DataOutputStream dos, int backgroundNumber);

    //int selectedPokemon → String myPokemonId 로 바꿔서 PokemonRepository 매핑하게 만들면 된다.
}
