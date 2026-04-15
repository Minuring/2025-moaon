package moaon.backend.search.admin.repository;

import java.util.List;
import moaon.backend.search.admin.domain.SearchCaseResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchCaseResultRepository extends JpaRepository<SearchCaseResult, Long> {
    List<SearchCaseResult> findByCaseIdOrderByRankAsc(String caseId);
}
