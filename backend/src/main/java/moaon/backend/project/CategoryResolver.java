package moaon.backend.project;

import lombok.RequiredArgsConstructor;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.project.domain.Category;
import moaon.backend.project.repository.CategoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoryResolver {

    private final CategoryRepository categoryRepository;

    public List<Category> resolve(List<String> rawNames) {
        if (rawNames.size() == 1 && rawNames.getFirst().equals("all")) {
            return List.of();
        }
        return rawNames.stream()
                .map(name -> categoryRepository.findByName(name)
                        .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND)))
                .toList();
    }
}
