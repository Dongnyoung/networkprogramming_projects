import java.util.Objects;

/**
 * Simple Skill data class.
 * Fields based on the provided sketch: name, type, power, accuracy.
 */
public class Skill {
    // 고유 식별자 (예: "ember") - 데이터 매핑/레퍼런스로 사용
    private String id;
    // 한글 이름 (예: "불꽃세례") - 표시용
    private String koreanName;
    // 속성(타입) 문자열 (예: "FIRE") - 추후 enum으로 교체 권장
    private String type;
    // 기술의 위력 (물리/특수 공격 파워, 상태 기술은 0)
    private int power;
    // 명중률 (0.0 ~ 1.0)
    private double accuracy;

    public Skill() {}

    public Skill(String id, String koreanName, String type, int power, double accuracy) {
        this.id = id;
        this.koreanName = koreanName;
        this.type = type;
        this.power = power;
        this.accuracy = accuracy;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getKoreanName() { return koreanName; }
    public void setKoreanName(String koreanName) { this.koreanName = koreanName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getPower() { return power; }
    public void setPower(int power) { this.power = power; }

    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

    @Override
    public String toString() {
        return "Skill{" +
                "id='" + id + '\'' +
                ", koreanName='" + koreanName + '\'' +
                ", type='" + type + '\'' +
                ", power=" + power +
                ", accuracy=" + accuracy +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Skill)) return false;
        Skill skill = (Skill) o;
        return Objects.equals(id, skill.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
