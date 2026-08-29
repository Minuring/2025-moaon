package moaon.backend.article.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import moaon.backend.article.domain.Article;
import moaon.backend.article.domain.ArticleCursor;
import moaon.backend.article.domain.ArticleSortType;
import moaon.backend.article.dto.ArticleDto;
import moaon.backend.article.dto.ArticleQueryCondition;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ArticleRepository {

    private final EntityManager em;
    private final JPAQueryFactory jpaQueryFactory;
    private ArticleRetrievalQuery articleRetrievalQuery;

    @PostConstruct
    public void init() {
        articleRetrievalQuery = new ArticleRetrievalQuery(jpaQueryFactory);
    }

    @Transactional(readOnly = true)
    public ArticleSearchResult search(ArticleQueryCondition condition) {
        return search(condition, null);
    }

    @Transactional(readOnly = true)
    public ArticleSearchResult search(ArticleQueryCondition condition, Long scopeProjectId) {
        List<Article> fetched = articleRetrievalQuery.search(condition, scopeProjectId);

        boolean hasNext = fetched.size() > condition.limit();
        List<Article> articles = hasNext ? fetched.subList(0, condition.limit()) : fetched;

        return new ArticleSearchResult(
                articles.stream().map(ArticleDto::from).toList(),
                articleRetrievalQuery.count(condition, scopeProjectId),
                hasNext,
                hasNext ? buildCursor(articles.getLast(), condition.sortType()) : null
        );
    }

    @Transactional
    public Article save(Article a) {
        em.persist(a);
        return a;
    }

    @Transactional(readOnly = true)
    public Optional<Article> findById(Long id) {
        return Optional.ofNullable(em.find(Article.class, id));
    }

    @Transactional
    public int increaseClickCount(Long id) {
        return em.createQuery("update Article a set a.clicks = a.clicks + 1 where a.id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

    private ArticleCursor buildCursor(Article lastArticle, ArticleSortType sortType) {
        Long lastId = lastArticle.getId();
        if (sortType == ArticleSortType.CLICKS) {
            return new ArticleCursor(lastArticle.getClicks(), lastId);
        }
        if (sortType == ArticleSortType.RELEVANCE) {
            return new ArticleCursor(lastArticle.getScore(), lastId);
        }
        return new ArticleCursor(lastArticle.getCreatedAt(), lastId);
    }
}
