package moaon.backend.article;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import moaon.backend.article.dto.*;
import moaon.backend.global.CountCooldownCookieManager;
import moaon.backend.member.Member;
import moaon.backend.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/articles")
public class ArticleController {

    private static final String CLICK_COOKIE_NAME_PREFIX = "clicked_articles_";
    private static final String CLICK_COOKIE_PATH = "/articles";

    private final CountCooldownCookieManager cookieManager;
    private final ArticleDtoConverter articleDtoConverter;
    private final ArticleService articleService;
    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<ArticleListResponse> search(@ModelAttribute @Validated ArticleSearchRequest request) {
        ArticleQueryCondition queryCondition = articleDtoConverter.convert(request);
        return ResponseEntity.ok(articleService.getPagedArticles(queryCondition));
    }

    @PostMapping
    public ResponseEntity<Void> saveArticles(
            @CookieValue(value = "token", required = false) String token,
            @RequestBody @Valid List<ArticleCreateRequest> requests
    ) {
        Member member = memberService.getUserByToken(token);
        List<ArticleCreateParams> params = requests.stream().map(articleDtoConverter::convert).toList();
        articleService.save(params, member);
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
