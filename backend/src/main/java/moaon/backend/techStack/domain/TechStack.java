package moaon.backend.techStack.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(of = "id")
@ToString
public class TechStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    public TechStack(Long id, String name) {
        this.id = id;
        this.name = normalize(name);
    }

    public TechStack(String name) {
        this(null, name);
    }

    public static String normalize(String rawName) {
        String[] words = rawName.strip().toLowerCase().split("[\\s_-]+");
        StringBuilder result = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            result.append(Character.toUpperCase(words[i].charAt(0)))
                    .append(words[i].substring(1));
        }
        return result.toString();
    }
}
