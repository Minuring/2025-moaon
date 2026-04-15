package moaon.backend.search.admin.repository;

import moaon.backend.search.admin.domain.SearchCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SearchCaseRepository extends JpaRepository<SearchCase, String>,
        JpaSpecificationExecutor<SearchCase> {
}
