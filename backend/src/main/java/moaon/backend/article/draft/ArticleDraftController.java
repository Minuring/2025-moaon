package moaon.backend.article.draft;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import moaon.backend.article.draft.dto.ArticleDraftAnalyzeResponse;
import moaon.backend.article.draft.dto.ArticleDraftCreateResponse;
import moaon.backend.article.draft.service.ArticleDraftService;
import moaon.backend.member.Member;
import moaon.backend.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/articles/drafts")
@RequiredArgsConstructor
public class ArticleDraftController {

    private final ArticleDraftService articleDraftService;
    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<ArticleDraftCreateResponse> create(
            @CookieValue(value = "token", required = false) String token,
            @RequestParam("url") String url
    ) {
        Member member = memberService.getUserByToken(token);
        ArticleDraft draft = articleDraftService.create(url, member);
        return ResponseEntity.status(HttpStatus.CREATED).body(ArticleDraftCreateResponse.from(draft));
    }

    @PostMapping("/{draftId}/analyze")
    public ResponseEntity<ArticleDraftAnalyzeResponse> analyze(
            @CookieValue(value = "token", required = false) String token,
            @PathVariable("draftId") Long draftId
    ) {
        Member member = memberService.getUserByToken(token);
        ArticleDraft draft = articleDraftService.analyze(draftId, member);
        return ResponseEntity.ok(ArticleDraftAnalyzeResponse.from(draft, member));
    }

    @GetMapping("/quota")
    public ResponseEntity<Map<String, Integer>> getQuota(
            @CookieValue(value = "token", required = false) String token
    ) {
        Member member = memberService.getUserByToken(token);
        return ResponseEntity.ok(Map.of("remainingCount", member.getTodayRemainingTokens()));
    }
}
