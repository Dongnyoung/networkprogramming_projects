import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 전역 포켓몬 레포지토리: 애플리케이션 시작 시 기본 종들을 등록하고
 * 다른 클래스에서 언제든 참조할 수 있도록 싱글턴으로 제공합니다.
 */
public class PokemonRepository {
    private static final PokemonRepository INSTANCE = new PokemonRepository();

    private final Map<String, Pokemon> speciesById = new HashMap<>();
    private final List<Pokemon> speciesList = new ArrayList<>();

    private PokemonRepository() {}

    public static PokemonRepository getInstance() { return INSTANCE; }

    public List<Pokemon> getAllSpecies() { return Collections.unmodifiableList(speciesList); }
    public Pokemon getById(String id) { return speciesById.get(id); }

    public void addSpecies(String id, Pokemon p) {
        if (id == null || p == null) return;
        speciesById.put(id, p);
        speciesList.remove(p);
        speciesList.add(p);
    }

    public void clear() {
        speciesById.clear();
        speciesList.clear();
    }

    /**
     * 기본 샘플 포켓몬들(파이리, 꼬부기, 이상해씨)을 로드합니다. 이미 로드되어 있다면 덮어쓰지 않습니다.
     */
    public void loadDefaults() {
        if (!speciesList.isEmpty()) return; // 이미 초기화됨

        // load from Resources/pokemon_species.json
        InputStream is = getClass().getResourceAsStream("/Resources/pokemon_species.json");
        if (is == null) {
            java.io.File f = new java.io.File("Resources/pokemon_species.json");
            if (f.exists()) {
                try {
                    is = new java.io.FileInputStream(f);
                } catch (Exception ex) {
                    return;
                }
            } else {
                return;
            }
        }
        try (InputStream in = is) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Pattern objPat = Pattern.compile("\\{([^}]*)\\}");
            Matcher objM = objPat.matcher(text);
            Pattern skillsPat = Pattern.compile("\"skills\"\\s*:\\s*\\[([^\\]]*)\\]");

            while (objM.find()) {
                String objText = objM.group(1);
                String id = findStringField(objText, "id");
                String kor = findStringField(objText, "koreanName");
                int level = findIntField(objText, "level", 5);
                int hp = findIntField(objText, "baseHp", 0);
                int atk = findIntField(objText, "baseAttack", 0);
                int def = findIntField(objText, "baseDefense", 0);
                int spd = findIntField(objText, "baseSpeed", 0);
                String t1 = findStringField(objText, "type1");
                String t2 = findStringField(objText, "type2");
                String front = findStringField(objText, "front");
                String back = findStringField(objText, "back");

                Pokemon p = new Pokemon(id, kor, level, hp, atk, def, spd, t1, t2, front, back);

                // parse skills array
                Matcher skM = skillsPat.matcher(objText);
                if (skM.find()) {
                    String inner = skM.group(1);
                    // split by comma and strip quotes/whitespace
                    List<String> parts = Arrays.asList(inner.split("\\s*,\\s*"));
                    for (String raw : parts) {
                        String s = raw.trim();
                        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
                            s = s.substring(1, s.length() - 1);
                        }
                        s = s.trim();
                        if (s.isEmpty()) continue;
                        Skill sk = SkillRepository.getInstance().getById(s);
                        if (sk == null) sk = SkillRepository.getInstance().getByName(s);
                        if (sk != null) p.addSkill(sk);
                    }
                }

                if (id != null) addSpecies(id, p);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String findStringField(String text, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }

    private int findIntField(String text, String key, int def) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(text);
        if (m.find()) return Integer.parseInt(m.group(1));
        return def;
    }
}
