package moaon.backend.search.admin.service;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.search.admin.domain.ReviewStatus;
import moaon.backend.search.admin.domain.SearchCase;
import moaon.backend.search.admin.domain.SearchCaseResult;
import moaon.backend.search.admin.dto.ReviewRequest;
import moaon.backend.search.admin.dto.SearchCaseDetail;
import moaon.backend.search.admin.dto.SearchCaseSummary;
import moaon.backend.search.admin.repository.SearchCaseRepository;
import moaon.backend.search.admin.repository.SearchCaseResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchCaseAdminService {

    private final SearchCaseRepository searchCaseRepository;
    private final SearchCaseResultRepository searchCaseResultRepository;

    @Transactional(readOnly = true)
    public Page<SearchCaseSummary> list(int page, int size, ReviewStatus statusFilter) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(
            Sort.Order.desc("badCaseScore"),
            Sort.Order.desc("searchedAt")
        ));

        Page<SearchCase> cases;
        if (statusFilter != null) {
            cases = searchCaseRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("status"), statusFilter),
                pageable
            );
        } else {
            cases = searchCaseRepository.findAll(pageable);
        }
        return cases.map(SearchCaseSummary::from);
    }

    @Transactional(readOnly = true)
    public SearchCaseDetail detail(String id) {
        SearchCase sc = searchCaseRepository.findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.SEARCH_CASE_NOT_FOUND));
        List<SearchCaseResult> results = searchCaseResultRepository.findByCaseIdOrderByRankAsc(id);
        return SearchCaseDetail.from(sc, results);
    }

    @Transactional
    public void review(String id, ReviewRequest request) {
        SearchCase sc = searchCaseRepository.findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.SEARCH_CASE_NOT_FOUND));
        sc.review(request.status(), request.memo());
    }
}
