package moaon.backend.techStack.service;

import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.techStack.TechStackRepository;
import moaon.backend.techStack.TechStackResolver;
import moaon.backend.techStack.domain.TechStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechStackResolverTest {

    private final TechStackRepository techStackRepository = Mockito.mock(TechStackRepository.class);
    private final TechStackResolver resolver = new TechStackResolver(techStackRepository);

    @DisplayName("resolve: 정규화 후 존재하는 이름을 TechStack으로 변환한다.")
    @Test
    void resolve() {
        TechStack java = new TechStack("java");
        when(techStackRepository.findByName("java")).thenReturn(Optional.of(java));

        List<TechStack> result = resolver.resolve(List.of("Java"));

        assertThat(result).containsExactly(java);
    }

    @DisplayName("resolve: 존재하지 않는 이름에 대해서는 예외를 발생시킨다.")
    @Test
    void resolve_filtersUnknownNames() {
        TechStack java = new TechStack("java");
        when(techStackRepository.findByName("java")).thenReturn(Optional.of(java));
        when(techStackRepository.findByName("unknownTech")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(List.of("Java", "unkownTech")))
                .isInstanceOf(CustomException.class);
    }
}
