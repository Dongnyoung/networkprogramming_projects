import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
 

public class SkillRepository {
    private static SkillRepository instance = new SkillRepository();
    private Map<String, Skill> byId = new HashMap<>();
    private Map<String, Skill> byName = new HashMap<>();

    private SkillRepository() {
        loadFromResource("/Resources/skills.json");
    }

    public static SkillRepository getInstance() {
        return instance;
    }

    private void loadFromResource(String resourcePath) {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            java.io.File f = new java.io.File("Resources/skills.json");
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
            // crude parser without external libraries: find object blocks and extract fields
            int idx = 0;
            while (true) {
                int start = text.indexOf('{', idx);
                if (start < 0) break;
                int end = text.indexOf('}', start);
                if (end < 0) break;
                String objText = text.substring(start + 1, end);

                String id = extractQuotedValue(objText, "id");
                String kor = extractQuotedValue(objText, "koreanName");
                String type = extractQuotedValue(objText, "type");
                String powerStr = extractNumberValue(objText, "power");
                String accStr = extractNumberValue(objText, "accuracy");
                if (id == null || kor == null) {
                    idx = end + 1;
                    continue;
                }
                double powerD = powerStr != null ? Double.parseDouble(powerStr) : 0.0;
                double accuracyD = accStr != null ? Double.parseDouble(accStr) : 1.0;
                int power = (int) Math.round(powerD);
                Skill s = new Skill(id, kor, type, power, accuracyD);
                byId.put(id, s);
                byName.put(kor, s);

                idx = end + 1;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String extractQuotedValue(String objText, String key) {
        String marker = "\"" + key + "\"";
        int p = objText.indexOf(marker);
        if (p < 0) return null;
        int colon = objText.indexOf(':', p + marker.length());
        if (colon < 0) return null;
        int firstQ = objText.indexOf('"', colon + 1);
        if (firstQ < 0) return null;
        int secondQ = objText.indexOf('"', firstQ + 1);
        if (secondQ < 0) return null;
        return objText.substring(firstQ + 1, secondQ);
    }

    private String extractNumberValue(String objText, String key) {
        String marker = "\"" + key + "\"";
        int p = objText.indexOf(marker);
        if (p < 0) return null;
        int colon = objText.indexOf(':', p + marker.length());
        if (colon < 0) return null;
        int i = colon + 1;
        while (i < objText.length() && Character.isWhitespace(objText.charAt(i))) i++;
        int j = i;
        while (j < objText.length() && (Character.isDigit(objText.charAt(j)) || objText.charAt(j) == '.')) j++;
        if (j <= i) return null;
        return objText.substring(i, j);
    }

    public Skill getById(String id) {
        return byId.get(id);
    }

    public Skill getByName(String name) {
        return byName.get(name);
    }

    public Collection<Skill> getAll() {
        return Collections.unmodifiableCollection(byId.values());
    }

}
