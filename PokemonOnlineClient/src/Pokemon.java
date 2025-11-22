import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Pokemon data class representing a species/instance as requested.
 * Fields are kept simple and public accessor methods are provided.
 * - englishName, koreanName
 * - level
 * - base stats: hp, attack, defense, speed
 * - types as simple Strings (enum can be added later)
 * - front/back image file names
 * - skills: list of Skill (max 4 recommended)
 * - currentHp: remaining HP
 */
public class Pokemon {
    // 종 식별자 (예: "charmander") - 데이터 매핑용
    private String id;
    // 한국어 이름 (표시용, 예: "파이리")
    private String koreanName;
    // 포켓몬 레벨
    private int level;

    // base stats (species values or instance-calculated)
    // 기본 체력(종의 기본 HP)
    private int baseHp;
    // 기본 공격력
    private int baseAttack;
    // 기본 방어력
    private int baseDefense;
    // 기본 속도
    private int baseSpeed;

    // 주 속성 (예: "FIRE")
    private String type1;
    // 보조 속성 (없을 수 있음)
    private String type2; // nullable

    // 앞면 스프라이트 파일명 또는 리소스 경로
    private String frontImageFile;
    // 뒷면 스프라이트 파일명 또는 리소스 경로
    private String backImageFile;

    // 보유 스킬 목록 (전투에서 사용 가능한 스킬들, 보통 최대 4개)
    private final List<Skill> skills = new ArrayList<>();

    // 런타임 상태: 현재 남은 체력
    private int currentHp;

    public Pokemon() {}

    public Pokemon(String id, String koreanName, int level,
                   int baseHp, int baseAttack, int baseDefense, int baseSpeed,
                   String type1, String type2,
                   String frontImageFile, String backImageFile) {
        this.id = id;
        this.koreanName = koreanName;
        this.level = level;
        this.baseHp = baseHp;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.baseSpeed = baseSpeed;
        this.type1 = type1;
        this.type2 = type2;
        this.frontImageFile = frontImageFile;
        this.backImageFile = backImageFile;
        // set current HP to max on create
        this.currentHp = calcMaxHp();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getKoreanName() { return koreanName; }
    public void setKoreanName(String koreanName) { this.koreanName = koreanName; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getBaseHp() { return baseHp; }
    public void setBaseHp(int baseHp) { this.baseHp = baseHp; }

    public int getBaseAttack() { return baseAttack; }
    public void setBaseAttack(int baseAttack) { this.baseAttack = baseAttack; }

    public int getBaseDefense() { return baseDefense; }
    public void setBaseDefense(int baseDefense) { this.baseDefense = baseDefense; }

    public int getBaseSpeed() { return baseSpeed; }
    public void setBaseSpeed(int baseSpeed) { this.baseSpeed = baseSpeed; }

    public String getType1() { return type1; }
    public void setType1(String type1) { this.type1 = type1; }

    public String getType2() { return type2; }
    public void setType2(String type2) { this.type2 = type2; }

    public String getFrontImageFile() { return frontImageFile; }
    public void setFrontImageFile(String frontImageFile) { this.frontImageFile = frontImageFile; }

    public String getBackImageFile() { return backImageFile; }
    public void setBackImageFile(String backImageFile) { this.backImageFile = backImageFile; }

    public List<Skill> getSkills() { return Collections.unmodifiableList(skills); }

    public void addSkill(Skill s) {
        if (s == null) return;
        if (skills.size() >= 4) throw new IllegalStateException("A Pokemon can have up to 4 skills");
        skills.add(s);
    }

    public void removeSkill(Skill s) { skills.remove(s); }

    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int currentHp) { this.currentHp = Math.max(0, Math.min(currentHp, calcMaxHp())); }

    public int calcMaxHp() {
        // very simple formula: baseHp + level * 2 (placeholder)
        return baseHp + Math.max(0, level) * 2;
    }

    public void takeDamage(int d) {
        setCurrentHp(this.currentHp - Math.max(0, d));
    }

    public boolean isFainted() { return this.currentHp <= 0; }

    @Override
    public String toString() {
        return "Pokemon{" +
                "id='" + id + '\'' +
                ", koreanName='" + koreanName + '\'' +
                ", level=" + level +
                ", currentHp=" + currentHp +
                ", skills=" + skills +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pokemon)) return false;
        Pokemon pokemon = (Pokemon) o;
        return Objects.equals(id, pokemon.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
