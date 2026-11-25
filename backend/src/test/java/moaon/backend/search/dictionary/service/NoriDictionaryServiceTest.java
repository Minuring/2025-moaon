package moaon.backend.search.dictionary.service;

import moaon.backend.search.dictionary.domain.NoriDictionaryEntry;
import moaon.backend.search.dictionary.dto.BulkConflict;
import moaon.backend.search.dictionary.dto.NoriEntry;
import moaon.backend.search.dictionary.repository.NoriDictionaryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoriDictionaryServiceTest {

    private final NoriDictionaryRepository repository = mock(NoriDictionaryRepository.class);
    private final NoriDictionaryService service = new NoriDictionaryService(repository);

    @DisplayName("dictionary.txt 형식(표면어 또는 표면어와 분해 토큰)의 여러 줄을 한 번에 추가한다.")
    @Test
    void addAll() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<NoriEntry> result = service.addAll("""
                아키텍쳐

                 세종시   세종 시
                """);

        assertThat(result).extracting(NoriEntry::getSurface, NoriEntry::getSegments)
                .containsExactly(
                        tuple("아키텍쳐", List.of()),
                        tuple("세종시", List.of("세종", "시"))
                );
    }

    @DisplayName("기존 항목과 표면어가 겹치는 줄이 있으면 아무것도 추가하지 않는다.")
    @Test
    void addAllConflictWithExisting() {
        when(repository.findAll()).thenReturn(List.of(new NoriDictionaryEntry("Kibana", List.of())));

        assertThatThrownBy(() -> service.addAll("""
                아키텍쳐
                kibana
                """))
                .isInstanceOfSatisfying(BulkConflictException.class, e -> assertThat(e.getConflicts())
                        .containsExactly(new BulkConflict("kibana", "kibana", "Kibana")));
        verify(repository, never()).saveAll(any());
    }

    @DisplayName("입력의 다른 줄과 표면어가 겹치는 줄이 있으면 아무것도 추가하지 않는다.")
    @Test
    void addAllConflictInInput() {
        when(repository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.addAll("""
                세종시
                세종시 세종 시
                """))
                .isInstanceOfSatisfying(BulkConflictException.class, e -> assertThat(e.getConflicts())
                        .containsExactly(new BulkConflict("세종시 세종 시", "세종시", "세종시")));
        verify(repository, never()).saveAll(any());
    }
}
