import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public interface BattleStartListener {
    void onBattleStartRequest(String myUsername, String myPokemonId, String enemyPokemonId, String opponentName,
                              Socket socket, DataInputStream dis, DataOutputStream dos, int backgroundNumber, JplWaitingRoom waitingRoom);
                              
    // int selectedPokemon -> String myPokemonId to map with PokemonRepository
}
