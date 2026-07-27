package moaon.backend.article;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import moaon.backend.article.dto.ArticleCreateRequest;
import moaon.backend.article.dto.ArticleQueryCondition;
import moaon.backend.article.dto.ArticleListResponse;
import moaon.backend.article.dto.ArticleSearchRequest;
import moaon.backend.global.CountCooldownCookieManager;
import moaon.backend.member.Member;
import moaon.backend.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    private static final String CLICK_COOKIE_NAME_PREFIX = "clicked_articles_";
    private static final String CLICK_COOKIE_PATH = "/articles";

    private final CountCooldownCookieManager cookieManager;
    private final ArticleService articleService;
    private final MemberService memberService;

    public ArticleController(
            CountCooldownCookieManager cookieManager,
            ArticleService articleService,
            MemberService memberService
    ) {
        this.cookieManager = cookieManager;
        this.articleService = articleService;
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<ArticleListResponse> search(@ModelAttribute @Validated ArticleSearchRequest request) {
        ArticleQueryCondition condition = request.toCondition();
        return ResponseEntity.ok(articleService.getPagedArticles(condition));
    }

    @PostMapping
    public ResponseEntity<Void> saveArticles(
            @CookieValue(value = "token", required = false) String token,
            @RequestBody @Valid List<ArticleCreateRequest> requests
    ) {
        Member member = memberService.getUserByToken(token);
        articleService.save(requests, member);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{id}/clicks")
    public ResponseEntity<Void> updateArticleClicks(
            @PathVariable("id") long id,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (cookieManager.isCountIncreasable(CLICK_COOKIE_NAME_PREFIX, id, request)) {
            articleService.increaseClicksCount(id);
            cookieManager.markAsCounted(CLICK_COOKIE_NAME_PREFIX, CLICK_COOKIE_PATH, id, response);
        }
        return ResponseEntity.ok().build();
    }
}
