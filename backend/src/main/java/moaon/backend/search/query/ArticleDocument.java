package moaon.backend.search.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;
import moaon.backend.article.domain.Article;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.techStack.domain.TechStack;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Document(indexName = "articles_idx", aliases = {@Alias(value = "articles", isWriteIndex = true)})
@Setting(settingPath = "/elasticsearch/article-settings.json")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@ToString
@Getter
public class ArticleDocument {

    @Id
    @Field(type = FieldType.Keyword)
    private Long id;

    @MultiField(
            mainField = @Field(type = FieldType.Text,
                    analyzer = "article_nori_analyzer",
                    searchAnalyzer = "article_nori_search_analyzer"),
            otherFields = {
                    @InnerField(suffix = "edge_ngram", type = FieldType.Text,
                            analyzer = "edge_ngram_analyzer",
                            searchAnalyzer = "lowercase_analyzer")
            }
    )
    private String title;

    @MultiField(
            mainField = @Field(type = FieldType.Text,
                    analyzer = "article_nori_analyzer",
                    searchAnalyzer = "article_nori_search_analyzer"),
            otherFields = {
                    @InnerField(suffix = "edge_ngram", type = FieldType.Text,
                            analyzer = "edge_ngram_analyzer",
                            searchAnalyzer = "lowercase_analyzer")
            }
    )
    private String summary;

    @MultiField(
            mainField = @Field(type = FieldType.Text,
                    analyzer = "article_nori_analyzer",
                    searchAnalyzer = "article_nori_search_analyzer"),
            otherFields = {
                    @InnerField(suffix = "edge_ngram", type = FieldType.Text,
                            analyzer = "edge_ngram_analyzer",
                            searchAnalyzer = "lowercase_analyzer")
            }
    )
    private String content;

    @Field(type = FieldType.Keyword)
    private Sector sector;

    @Field(type = FieldType.Keyword)
    private Set<Topic> topics;

    @Field(type = FieldType.Keyword)
    private Set<String> techStacks;

    @Field(type = FieldType.Integer)
    private int clicks;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_fraction)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Keyword)
    private Long projectId;

    @Field(type = FieldType.Keyword)
    private String projectTitle;

    @Field(type = FieldType.Keyword)
    private String url;

    public ArticleDocument(Article article, String content) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.summary = article.getSummary();
        this.content = content;
        this.sector = article.getSector();
        this.topics = new HashSet<>(article.getTopics());
        this.techStacks = setTechStacks(article.getTechStacks());
        this.clicks = article.getClicks();
        this.createdAt = article.getCreatedAt().truncatedTo(ChronoUnit.MILLIS);
        this.projectId = article.getProject().getId();
        this.projectTitle = article.getProject().getTitle();
        this.url = article.getArticleUrl();
    }

    private Set<String> setTechStacks(List<TechStack> techStacks) {
        if (CollectionUtils.isEmpty(techStacks)) {
            return new HashSet<>();
        }
        return techStacks.stream().map(TechStack::getName).collect(toSet());
    }

    public JsonNode convertToJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JavaTimeModule module = new JavaTimeModule();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(
                    DateFormat.date_hour_minute_second_fraction.getPattern());
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
            mapper.registerModule(module);

            String json = mapper.writeValueAsString(this);
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Json 변환 오류");
        }
    }
}
