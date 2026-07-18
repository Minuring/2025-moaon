package moaon.backend.techStack.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import moaon.backend.techStack.domain.TechStack;
import moaon.backend.techStack.repository.TechStackRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TechStackResolver {

    private final TechStackRepository techStackRepository;

    public List<TechStack> resolve(List<String> rawNames) {
        return rawNames.stream()
                .map(TechStack::normalize)
                .map(techStackRepository::findByName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}
