package moaon.backend.search.dictionary.repository;

import moaon.backend.search.dictionary.domain.NoriDictionaryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoriDictionaryRepository extends JpaRepository<NoriDictionaryEntry, Long> {
}
