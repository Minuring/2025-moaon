package moaon.backend.article.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import moaon.backend.global.domain.BaseTimeEntity;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.project.domain.Project;
import moaon.backend.techStack.domain.ArticleTechStack;
import moaon.backend.techStack.domain.TechStack;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(exclude = {"project", "techStacks", "topics"})
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

    @OneToOne(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ArticleContentSeparated contentSeparated;

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
    @BatchSize(size = 500)
    private List<ArticleTechStack> techStacks = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sector sector;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @BatchSize(size = 500)
    private List<Topic> topics = new ArrayList<>();

    @Transient
    @Setter
    private double score = 0.0;

    // todo topic과 sector의 포함 관계 확인?
    public Article(
            String title,
            String summary,
            String content,
            String articleUrl,
            LocalDateTime createdAt,
            Project project,
            Sector sector,
            List<Topic> topics,
            List<TechStack> techStacks
    ) {
        this.title = title;
        this.summary = summary;
        this.contentSeparated = new ArticleContentSeparated(this, content);
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

    public String getContent() {
        if (contentSeparated == null) {
            return "";
        }
        return contentSeparated.getContent();
    }
}
