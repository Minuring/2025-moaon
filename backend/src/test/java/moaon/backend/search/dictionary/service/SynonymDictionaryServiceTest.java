package moaon.backend.search.dictionary.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import moaon.backend.search.dictionary.domain.SynonymDictionaryEntry;
import moaon.backend.search.dictionary.dto.BulkConflict;
import moaon.backend.search.dictionary.dto.SynonymEntry;
import moaon.backend.search.dictionary.repository.SynonymDictionaryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SynonymDictionaryServiceTest {

    private final SynonymDictionaryRepository repository = mock(SynonymDictionaryRepository.class);
    private final SynonymDictionaryService service = new SynonymDictionaryService(repository, mock(ElasticsearchClient.class));

    @DisplayName("한 줄에 하나씩 쉼표로 구분한 동의어 묶음을 한 번에 추가한다.")
    @Test
    void addAll() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<SynonymEntry> result = service.addAll("""
                아키텍처, 아키텍쳐 ,architecture

                도커, docker
                """);

        assertThat(result).extracting(SynonymEntry::getRaw)
                .containsExactly("아키텍처, 아키텍쳐, architecture", "도커, docker");
    }

    @DisplayName("기존 항목과 term이 겹치는 줄이 있으면 아무것도 추가하지 않는다.")
    @Test
    void addAllConflictWithExisting() {
        when(repository.findAll()).thenReturn(List.of(new SynonymDictionaryEntry(List.of("도커", "Docker"))));

        assertThatThrownBy(() -> service.addAll("""
                아키텍처, 아키텍쳐
                docker, 도커 스웜
                """))
                .isInstanceOfSatisfying(BulkConflictException.class, e -> assertThat(e.getConflicts())
                        .containsExactly(new BulkConflict("docker, 도커 스웜", "docker", "도커, Docker")));
        verify(repository, never()).saveAll(any());
    }

    @DisplayName("입력의 다른 줄과 term이 겹치는 줄이 있으면 아무것도 추가하지 않는다.")
    @Test
    void addAllConflictInInput() {
        when(repository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.addAll("""
                테스트, test
                test, 테스팅
                """))
                .isInstanceOfSatisfying(BulkConflictException.class, e -> assertThat(e.getConflicts())
                        .containsExactly(new BulkConflict("test, 테스팅", "test", "테스트, test")));
        verify(repository, never()).saveAll(any());
    }
}
