package moaon.backend.search.dictionary;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import moaon.backend.search.dictionary.dto.AnalyzeRequest;
import moaon.backend.search.dictionary.dto.AnalyzeResponse;
import moaon.backend.search.dictionary.service.AnalyzePreviewService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/analyze")
@RequiredArgsConstructor
public class AnalyzeApiController {

    private final AnalyzePreviewService analyzePreviewService;

    @PostMapping
    public AnalyzeResponse analyze(@RequestBody @Valid AnalyzeRequest request) {
        return analyzePreviewService.analyze(request.getText());
    }
}
