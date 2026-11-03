package moaon.backend.article.domain;

import jakarta.persistence.*;
import lombok.*;
import moaon.backend.global.domain.BaseTimeEntity;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.project.domain.Project;
import moaon.backend.techStack.domain.ArticleTechStack;
import moaon.backend.techStack.domain.TechStack;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(of = "id")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Table(indexes = {
        @Index(name = "idx_article_created_at_id", columnList = "createdAt DESC, id DESC"),
        @Index(name = "idx_article_clicks_id", columnList = "clicks DESC, id DESC")
})
public class Article extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String summary;

    // Article <> Content 연관관계의 PK-FK 공유 구조에서
    // Content가 연관관계의 주인이 되는 게 자연스러운데, 이때
    // EAGER FETCH를 피할 수 없으므로 JPA 연관관계를 설정하지 않음.

    @Column(nullable = false, length = 500)
    private String articleUrl;

    @Column(nullable = false)
    private int clicks;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @Fetch(FetchMode.JOIN)
    private List<ArticleTechStack> techStacks = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sector sector;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private List<Topic> topics = new ArrayList<>();

    @Transient
    @Setter
    private double score = 0.0;

    // todo topic과 sector의 포함 관계 확인?
    public Article(
            String title,
            String summary,
            String articleUrl,
            LocalDateTime createdAt,
            Project project,
            Sector sector,
            List<Topic> topics,
            List<TechStack> techStacks
    ) {
        this.title = title;
        this.summary = summary;
        this.articleUrl = articleUrl;
        this.clicks = 0;
        this.createdAt = createdAt;
        this.project = project;
        this.sector = sector;
        if (topics.size() > 3) {
            throw new CustomException(ErrorCode.ARTICLE_INVALID_TOPICS);
        }
        this.topics = topics;
        if (techStacks.size() > 3) {
            throw new CustomException(ErrorCode.ARTICLE_INVALID_TECHSTACK);
        }
        this.techStacks = new ArrayList<>();
        techStacks.forEach(this::addTechStack);
    }

    public void addTechStack(TechStack techStack) {
        ArticleTechStack articleTechStack = new ArticleTechStack(this, techStack);
        this.techStacks.add(articleTechStack);
    }

    public List<TechStack> getTechStacks() {
        return techStacks.stream()
                .map(ArticleTechStack::getTechStack)
                .toList();
    }
}
