package moaon.backend.techStack;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.techStack.domain.TechStack;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TechStackResolver {

    private final TechStackRepository techStackRepository;

    public List<TechStack> resolve(List<String> rawNames) {
        return rawNames.stream()
                .map(TechStack::normalize)
                .map(name -> techStackRepository.findByName(name)
                        .orElseThrow(() -> new CustomException(ErrorCode.TECHSTACK_NOT_FOUND)))
                .toList();
    }
}
