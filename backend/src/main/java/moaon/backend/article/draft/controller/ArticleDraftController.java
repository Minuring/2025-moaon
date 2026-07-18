package moaon.backend.article.draft.controller;

import lombok.RequiredArgsConstructor;
import moaon.backend.article.draft.domain.ArticleDraft;
import moaon.backend.article.draft.dto.ArticleDraftCreateResponse;
import moaon.backend.article.draft.service.ArticleDraftService;
import moaon.backend.member.domain.Member;
import moaon.backend.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
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
}
