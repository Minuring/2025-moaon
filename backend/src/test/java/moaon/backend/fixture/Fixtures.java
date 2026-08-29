package moaon.backend.fixture;

import moaon.backend.article.domain.Article;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.member.Member;
import moaon.backend.project.domain.Category;
import moaon.backend.project.domain.Images;
import moaon.backend.project.domain.Project;
import moaon.backend.techStack.domain.TechStack;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class Fixtures {

    private static final AtomicLong SEQUENCE = new AtomicLong(0L);

    public static ProjectBuilder projectBuilder() {
        return new ProjectBuilder();
    }

    public static ArticleBuilder articleBuilder() {
        return new ArticleBuilder();
    }

    public static Project anyProject() {
        return new ProjectBuilder()
                .id(SEQUENCE.getAndIncrement())
                .title(stringWithSequence("프로젝트"))
                .summary("프로젝트 요약문입니다.".repeat(2))
                .description("충분히 긴 설명입니다.".repeat(15))
                .githubUrl("https://github.com/moaon")
                .productionUrl("https://moaon.site")
                .images("image.png")
                .author(anyMember())
                .techStacks(anyTechStack(), anyTechStack())
                .categories(anyProjectCategory())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Article anyArticle(Project project) {
        Sector sector = randomSector();
        return new ArticleBuilder()
                .id(SEQUENCE.getAndIncrement())
                .title(stringWithSequence("아티클"))
                .summary("프로젝트 요약문입니다.".repeat(2))
                .articleUrl("https://tempdev.tistory.com/19")
                .clicks(new Random().nextInt())
                .techStacks(anyTechStack(), anyTechStack())
                .sector(sector)
                .topics(sector.getTopics().getFirst())
                .createdAt(LocalDateTime.now())
                .project(project)
                .build();
    }

    public static Article anyArticle() {
        return anyArticle(anyProject());
    }

    public static Member anyMember() {
        return new Member(
                stringWithSequence("testSocialId"),
                stringWithSequence("testEmail@gmail.com"),
                stringWithSequence("testMember")
        );
    }

    public static TechStack anyTechStack() {
        return new TechStack(stringWithSequence("testTechStack"));
    }

    public static Category anyProjectCategory() {
        return new Category(stringWithSequence("testProjectCategory"));
    }

    public static Sector randomSector() {
        return Sector.values()[new Random().nextInt(Sector.values().length)];
    }

    public static String stringWithSequence(String name) {
        return name + SEQUENCE.incrementAndGet();
    }

    public static class ArticleBuilder {

        private Long id;
        private String title;
        private String summary;
        private String articleUrl;
        private LocalDateTime createdAt;
        private Project project;
        private Sector sector;
        private List<Topic> topics;
        private List<TechStack> techStacks;
        private int clicks;
        private double score;

        public ArticleBuilder() {
            this.title = stringWithSequence("테스트 아티클 제목");
            this.summary = stringWithSequence("테스트 아티클 요약");
            this.articleUrl = "https://test-product.com";
            this.createdAt = LocalDateTime.now();
            this.project = new ProjectBuilder().build();
            this.sector = Fixtures.randomSector();
            this.topics = new ArrayList<>(List.of(Topic.ETC));
            this.techStacks = new ArrayList<>(List.of(Fixtures.anyTechStack()));
            this.clicks = 0;
            this.score = 0.0;
        }

        public ArticleBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ArticleBuilder title(String title) {
            this.title = title;
            return this;
        }

        public ArticleBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public ArticleBuilder articleUrl(String articleUrl) {
            this.articleUrl = articleUrl;
            return this;
        }

        public ArticleBuilder techStacks(List<TechStack> techStacks) {
            this.techStacks = techStacks;
            return this;
        }

        public ArticleBuilder techStacks(TechStack... techStacks) {
            this.techStacks = Arrays.asList(techStacks);
            return this;
        }

        public ArticleBuilder sector(Sector sector) {
            this.sector = sector;
            return this;
        }

        public ArticleBuilder topics(Topic... topics) {
            this.topics = Arrays.asList(topics);
            return this;
        }

        public ArticleBuilder project(Project project) {
            this.project = project;
            return this;
        }

        public ArticleBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ArticleBuilder clicks(int clicks) {
            this.clicks = clicks;
            return this;
        }

        public ArticleBuilder score(double score) {
            this.score = score;
            return this;
        }

        public Article build() {
            Article article = Article.builder()
                    .id(this.id)
                    .title(this.title)
                    .summary(this.summary)
                    .articleUrl(this.articleUrl)
                    .clicks(this.clicks)
                    .createdAt(this.createdAt)
                    .project(this.project)
                    .sector(this.sector)
                    .topics(this.topics)
                    .techStacks(new ArrayList<>())
                    .score(this.score)
                    .build();

            for (TechStack techStack : techStacks) {
                article.addTechStack(techStack);
            }

            return article;
        }
    }

    public static class ProjectBuilder {

        private Long id;
        private String title;
        private String summary;
        private String description;
        private String githubUrl;
        private String productionUrl;
        private Images images;
        private Member author;
        private List<TechStack> techStacks;
        private List<Category> categories;
        private LocalDateTime createdAt;
        private int views = 0;
        private List<Member> lovedMembers = new ArrayList<>();
        private List<Article> articles = new ArrayList<>();

        public ProjectBuilder() {
            this.title = stringWithSequence("테스트 프로젝트 제목");
            this.summary = stringWithSequence("테스트 프로젝트 요약");
            this.description = stringWithSequence("테스트 프로젝트 상세 설명");
            this.githubUrl = "https://github.com/test-repo";
            this.productionUrl = "https://test-product.com";
            this.images = new Images(List.of("https://test.com/image1.png", "https://test.com/image2.png"));
            this.author = Fixtures.anyMember();
            this.techStacks = new ArrayList<>(List.of(Fixtures.anyTechStack()));
            this.categories = new ArrayList<>(List.of(Fixtures.anyProjectCategory()));
            this.createdAt = LocalDateTime.now();
            this.lovedMembers = new ArrayList<>();
            this.articles = new ArrayList<>();
        }

        public ProjectBuilder title(String title) {
            this.title = title;
            return this;
        }

        public ProjectBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public ProjectBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ProjectBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ProjectBuilder githubUrl(String githubUrl) {
            this.githubUrl = githubUrl;
            return this;
        }

        public ProjectBuilder productionUrl(String productionUrl) {
            this.productionUrl = productionUrl;
            return this;
        }

        public ProjectBuilder images(String... imageUrls) {
            this.images = new Images(Arrays.asList(imageUrls));
            return this;
        }

        public ProjectBuilder author(Member author) {
            this.author = author;
            return this;
        }

        public ProjectBuilder techStacks(TechStack... techStacks) {
            this.techStacks = new ArrayList<>(Arrays.asList(techStacks));
            return this;
        }

        public ProjectBuilder categories(Category... categories) {
            this.categories = new ArrayList<>(Arrays.asList(categories));
            return this;
        }

        public ProjectBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ProjectBuilder views(int views) {
            this.views = views;
            return this;
        }

        public Project build() {
            Project project = Project.builder()
                    .id(this.id)
                    .title(this.title)
                    .productionUrl(this.productionUrl)
                    .views(this.views)
                    .author(this.author)
                    .lovedMembers(this.lovedMembers)
                    .createdAt(this.createdAt)
                    .summary(this.summary)
                    .categories(new ArrayList<>())
                    .description(this.description)
                    .githubUrl(this.githubUrl)
                    .images(this.images)
                    .techStacks(new ArrayList<>())
                    .articles(this.articles)
                    .build();

            for (Category category : categories) {
                project.addCategory(category);
            }
            for (TechStack techStack : techStacks) {
                project.addTechStack(techStack);
            }

            for (int i = 0; i < views; i++) {
                project.addViewCount();
            }
            return project;
        }
    }
}
