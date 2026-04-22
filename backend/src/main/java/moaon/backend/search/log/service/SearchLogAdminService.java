package moaon.backend.search.log.service;

import lombok.RequiredArgsConstructor;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.search.log.domain.SearchLogDocument;
import moaon.backend.search.log.SearchLogRepository;
import moaon.backend.search.log.dto.SearchLogDetail;
import moaon.backend.search.log.dto.SearchLogSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchLogAdminService {

    private final SearchLogRepository searchLogRepository;

    public Page<SearchLogSummary> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("searchedAt")));
        return searchLogRepository.findAll(pageable).map(SearchLogSummary::from);
    }

    public SearchLogDetail detail(String id) {
        SearchLogDocument doc = searchLogRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.SEARCH_LOG_NOT_FOUND));
        return SearchLogDetail.from(doc);
    }
}
