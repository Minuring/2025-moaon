package moaon.backend.project.repository;

import java.util.List;
import moaon.backend.member.domain.Member;
import moaon.backend.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long>, CustomizedProjectRepository {

    List<Project> findByAuthor(Member author);
}
