package moaon.backend.techStack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import moaon.backend.techStack.domain.TechStack;
import moaon.backend.techStack.repository.TechStackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TechStackResolverTest {

    @Mock
    private TechStackRepository techStackRepository;

    @DisplayName("resolve: 정규화 후 존재하는 이름만 TechStack으로 변환하고 없는 이름은 버린다")
    @Test
    void resolve_filtersUnknownNames() {
        TechStackResolver resolver = new TechStackResolver(techStackRepository);
        TechStack java = new TechStack("java");
        when(techStackRepository.findByName("java")).thenReturn(Optional.of(java));
        when(techStackRepository.findByName("unknownTech")).thenReturn(Optional.empty());

        List<TechStack> result = resolver.resolve(List.of("Java", "unknown tech"));

        assertThat(result).containsExactly(java);
    }
}
