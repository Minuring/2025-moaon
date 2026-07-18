package moaon.backend.article.draft.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.global.domain.BaseTimeEntity;
import moaon.backend.member.domain.Member;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "article_draft")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(of = "id", callSuper = false)
public class ArticleDraft extends BaseTimeEntity {

    private static final int MAX_SUMMARY_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 500)
    private String crawledTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String crawledContent;

    @Column(length = 255)
    private String analyzedSummary;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Sector analyzedSector;

    @ElementCollection
    @CollectionTable(name = "article_draft_topic", joinColumns = @JoinColumn(name = "article_draft_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Enumerated(EnumType.STRING)
    @Column(name = "topic", length = 50)
    private List<Topic> analyzedTopics = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "article_draft_tech_stack", joinColumns = @JoinColumn(name = "article_draft_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Column(name = "tech_stack_name", length = 255)
    private List<String> analyzedTechStacks = new ArrayList<>();

    public ArticleDraft(Member member, String url, String crawledTitle, String crawledContent) {
        this.member = member;
        this.url = url;
        this.crawledTitle = crawledTitle;
        this.crawledContent = crawledContent;
    }

    public void applyAnalysis(String summary, Sector sector, List<Topic> topics, List<String> techStacks) {
        this.analyzedSummary = truncateSummary(summary);
        this.analyzedSector = sector;
        this.analyzedTopics = topics;
        this.analyzedTechStacks = techStacks;
    }

    public boolean isOwnedBy(Member requester) {
        return this.member.equals(requester);
    }

    private static String truncateSummary(String summary) {
        if (summary == null) {
            return "";
        }
        String trimmed = summary.strip();
        if (trimmed.length() <= MAX_SUMMARY_LENGTH) {
            return trimmed;
        }

        int lastSentenceEnd = trimmed.lastIndexOf(".", MAX_SUMMARY_LENGTH);
        if (lastSentenceEnd > 0) {
            return trimmed.substring(0, lastSentenceEnd + 1);
        }
        return trimmed.substring(0, MAX_SUMMARY_LENGTH);
    }
}
