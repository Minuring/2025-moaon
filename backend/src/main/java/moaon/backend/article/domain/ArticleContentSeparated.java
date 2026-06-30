package moaon.backend.article.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import lombok.Getter;
@Entity
public class ArticleContentSeparated {

    @Id
    private Long id = null;

    @MapsId
    @OneToOne
    @JoinColumn(name = "id")
    private Article article;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Getter
    private String content;

    public ArticleContentSeparated(final Article article, final String content) {
        this.article = article;
        this.content = content == null ? "" : content;
    }

    protected ArticleContentSeparated() {
    }
}
