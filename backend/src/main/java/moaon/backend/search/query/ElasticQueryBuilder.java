package moaon.backend.search.query;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import jakarta.annotation.Nullable;
import moaon.backend.article.domain.ArticleCursor;
import moaon.backend.article.domain.ArticleSortType;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.global.domain.SearchKeyword;
import moaon.backend.techStack.domain.TechStack;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

class ElasticQueryBuilder {

    private static final HighlightQuery highlightQuery = new HighlightQuery(
            new Highlight(HighlightParameters.builder()
                    .withPreTags("<mark>")
                    .withPostTags("</mark>")
                    .withFragmentSize(255)
                    .withNumberOfFragments(1)
                    .build(),
                    List.of(
                            new HighlightField("title"),
                            new HighlightField("summary")
                    )
            ), ArticleDocument.class);

    private final List<Query> musts = new ArrayList<>();
    private final List<Query> filters = new ArrayList<>();
    private Pageable pageable;
    private Sort sort;
    private List<Object> searchAfter;
    private long timeoutMillis = 100L;

    public ElasticQueryBuilder withIds(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            filters.add(createIdsQuery(ids));
        }
        return this;
    }

    public ElasticQueryBuilder withTextSearch(SearchKeyword searchKeyword) {
        if (searchKeyword != null) {
            musts.add(createTextMatchQuery(searchKeyword));
        }
        return this;
    }

    public ElasticQueryBuilder withSector(Sector sector) {
        if (sector != null) {
            filters.add(createSectorQuery(sector));
        }
        return this;
    }

    public ElasticQueryBuilder withTopicsAndMatch(List<Topic> topics) {
        if (topics != null && !topics.isEmpty()) {
            filters.add(createTopicsAndQuery(topics));
        }
        return this;
    }

    public ElasticQueryBuilder withTopicsOrMatch(List<Topic> topics) {
        if (topics != null && !topics.isEmpty()) {
            filters.add(createTopicsOrQuery(topics));
        }
        return this;
    }

    public ElasticQueryBuilder withTechStacksAndMatch(List<TechStack> techStacks) {
        if (techStacks != null && !techStacks.isEmpty()) {
            filters.add(createTechStacksAndQuery(techStacks));
        }
        return this;
    }

    public ElasticQueryBuilder withPagination(int limit, @Nullable ArticleCursor cursor, ArticleSortType sortType) {
        if (cursor == null) {
            limit = Math.max(limit, 1);
            this.pageable = PageRequest.of(0, limit);
            return this;
        }
        this.pageable = PageRequest.ofSize(limit);
        if (ArticleSortType.CREATED_AT == sortType) {
            this.searchAfter = List.of(cursor.getSortValueAsLong(), cursor.getLastId());
            return this;
        }

        this.searchAfter = List.of(cursor.getSortValue(), cursor.getLastId());
        return this;
    }

    public ElasticQueryBuilder withSort(ArticleSortType sortType) {
        this.sort = createSort(sortType);
        return this;
    }

    public ElasticQueryBuilder withQueryCondition(ArticleQueryCondition condition) {
        return this.withTextSearch(condition.search())
                .withSector(condition.sector())
                .withTechStacksAndMatch(condition.techStacks())
                .withTopicsAndMatch(condition.topics())
                .withSort(condition.sortType())
                .withPagination(condition.limit(), condition.cursor(), condition.sortType());
    }

    public ElasticQueryBuilder withTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    public NativeQuery build() {
        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(combineBoolQuery())
                .withTrackTotalHits(true)
                .withTrackScores(true)  // always track for logging
                .withPageable(pageable)
                .withHighlightQuery(highlightQuery)
                .withSourceFilter(new FetchSourceFilter(null, null, new String[]{"content"}))
                .withTimeout(Duration.ofMillis(timeoutMillis));

        if (sort != null) {
            builder.withSort(sort);
        }

        if (searchAfter != null && !searchAfter.isEmpty()) {
            builder.withSearchAfter(searchAfter);
        }

        return builder.build();
    }

    private Query createIdsQuery(List<Long> ids) {
        List<FieldValue> values = ids.stream().map(FieldValue::of).toList();
        return TermsQuery.of(t -> t
                .field("id")
                .terms(f -> f.value(values))
        )._toQuery();
    }

    private Query createTextMatchQuery(SearchKeyword searchKeyword) {
        List<Query> mustQueries = new ArrayList<>();

        // 1) 마지막 이전 토큰들은 모두 exact 필수
        for (String token : searchKeyword.allTokensBeforeLastToken()) {
            mustQueries.add(exactTokenQuery(token));
        }

        // 2) 마지막 토큰은 exact OR prefix 중 하나는 반드시 매칭
        //    그리고 exact가 prefix보다 더 높은 점수를 갖도록 설계
        mustQueries.add(lastTokenRequiredQuery(searchKeyword.lastToken()));

        return QueryBuilders.bool()
                .must(mustQueries)
                .build()
                ._toQuery();
    }

    private Query lastTokenRequiredQuery(String token) {
        return QueryBuilders.bool()
                .should(
                        // exact가 기본 축
                        exactTokenQuery(token),

                        // prefix는 약한 보조 신호
                        prefixTokenQuery(token)
                )
                .minimumShouldMatch("1")
                .build()
                ._toQuery();
    }

    private Query exactTokenQuery(String token) {
        return Query.of(q -> {
            String title = "title^1.5";
            String summary = "summary^1.25";
            String content = "content^0.3";
            return q.multiMatch(
                    MultiMatchQuery.of(m -> m.query(token).fields(title, summary, content).operator(Operator.And).type(TextQueryType.MostFields).tieBreaker(0.3))
            );
        });
    }

    private Query prefixTokenQuery(String token) {
        return Query.of(q -> q.disMax(
                DisMaxQuery.of(d -> d
                        .tieBreaker(0.5)
                        .queries(
                                Query.of(m -> m.match(mm -> mm
                                        .field("title.edge_ngram")
                                        .query(token)
                                        .boost(1.0f)
                                )),
                                Query.of(m -> m.match(mm -> mm
                                        .field("summary.edge_ngram")
                                        .query(token)
                                        .boost(0.5f)
                                )),
                                Query.of(m -> m.match(mm -> mm
                                        .field("content.edge_ngram")
                                        .query(token)
                                        .boost(0.1f)
                                ))
                        )
                )
        ));
    }

    private Query createSectorQuery(Sector sector) {
        return TermQuery.of(t -> t
                .field("sector")
                .value(sector.name())
        )._toQuery();
    }

    private Query createTopicsAndQuery(List<Topic> topics) {
        List<Query> topicQueries = topics.stream()
                .map(this::createSingleTopicQuery)
                .toList();
        return BoolQuery.of(b -> b.filter(topicQueries))._toQuery();
    }

    private Query createTopicsOrQuery(List<Topic> topics) {
        List<Query> topicQueries = topics.stream()
                .map(this::createSingleTopicQuery)
                .toList();
        return BoolQuery.of(b -> b.should(topicQueries))._toQuery();
    }

    private Query createTechStacksAndQuery(List<TechStack> techStacks) {
        List<Query> topicQueries = techStacks.stream()
                .map(this::createSingleTechStackQuery)
                .toList();
        return BoolQuery.of(b -> b.filter(topicQueries))._toQuery();
    }

    private Query createSingleTopicQuery(Topic topic) {
        return TermQuery.of(t -> t
                .field("topics")
                .value(topic.name())
        )._toQuery();
    }

    private Query createSingleTechStackQuery(TechStack techStack) {
        return TermQuery.of(t -> t
                .field("techStacks")
                .value(techStack.getName())
        )._toQuery();
    }

    private Query combineBoolQuery() {
        return BoolQuery.of(b -> b
                .must(musts)
                .filter(filters)
        )._toQuery();
    }

    private Sort createSort(ArticleSortType sortType) {
        return switch (sortType) {
            case RELEVANCE -> Sort.by(Order.desc("_score"), Order.asc("id"));
            case CLICKS -> Sort.by(Order.desc("clicks"), Order.asc("id"));
            case CREATED_AT -> Sort.by(Order.desc("createdAt"), Order.asc("id"));
        };
    }
}
