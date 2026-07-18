package moaon.backend.techStack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
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

    @OneToMany(mappedBy = "techStack", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<ProjectTechStack> projectTechStacks = new ArrayList<>();

    @OneToMany(mappedBy = "techStack", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<ArticleTechStack> articleTechStacks = new ArrayList<>();

    public TechStack(String name) {
        this.name = name;
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
