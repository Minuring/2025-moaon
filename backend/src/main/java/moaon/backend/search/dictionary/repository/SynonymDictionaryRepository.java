package moaon.backend.search.dictionary.repository;

import moaon.backend.search.dictionary.domain.SynonymDictionaryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SynonymDictionaryRepository extends JpaRepository<SynonymDictionaryEntry, Long> {
}
