package moaon.backend.fixture;

import moaon.backend.article.domain.Article;
import moaon.backend.article.domain.ArticleContent;
import moaon.backend.article.repository.ArticleContentRepository;
import moaon.backend.article.repository.ArticleRepository;
import moaon.backend.member.Member;
import moaon.backend.member.MemberRepository;
import moaon.backend.project.domain.Category;
import moaon.backend.project.domain.Project;
import moaon.backend.project.repository.CategoryRepository;
import moaon.backend.project.repository.ProjectRepository;
import moaon.backend.techStack.TechStackRepository;
import moaon.backend.techStack.domain.TechStack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

@TestComponent
@Transactional
public class RepositoryHelper {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TechStackRepository techStackRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleContentRepository articleContentRepository;

    public Project save(Project project) {
        memberRepository.save(project.getAuthor());
        techStackRepository.saveAll(project.getTechStacks());
        categoryRepository.saveAll(project.getCategories());

        return projectRepository.save(project);
    }

    public Project saveAnyProject() {
        return save(Fixtures.projectBuilder().build());
    }

    public Article save(Article article) {
        String content = Fixtures.stringWithSequence("아티클 본문");
        return save(article, content);
    }

    public Article saveArticle(Consumer<Fixtures.ArticleBuilder> builderConsumer) {
        Fixtures.ArticleBuilder builder = Fixtures.articleBuilder();
        builderConsumer.accept(builder);
        return save(builder.build());
    }

    public Article save(Article article, String content) {
        Article saved = articleRepository.save(article);
        articleContentRepository.save(new ArticleContent(saved, content));
        techStackRepository.saveAll(article.getTechStacks());
        save(article.getProject());

        return saved;
    }

    public Article saveAnyArticle() {
        return save(Fixtures.articleBuilder().build());
    }

    public Member save(Member member) {
        return memberRepository.save(member);
    }

    public Member saveAnyMember() {
        return save(Fixtures.anyMember());
    }

    public Article getArticleById(long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("테스트 실패"));
    }

    public TechStack save(TechStack techStack) {
        return techStackRepository.save(techStack);
    }

    public TechStack saveAnyTechStack() {
        return save(Fixtures.anyTechStack());
    }

    public Category save(Category category) {
        return categoryRepository.save(category);
    }
}
