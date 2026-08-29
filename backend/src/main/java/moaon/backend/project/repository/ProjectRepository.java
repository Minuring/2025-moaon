package moaon.backend.project.repository;

import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.member.Member;
import moaon.backend.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long>, CustomizedProjectRepository {
    @Override
    default Project getById(Long id) {
        return findById(id).orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
    }

    List<Project> findByAuthor(Member author);
}
