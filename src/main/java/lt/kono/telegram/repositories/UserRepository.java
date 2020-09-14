package lt.kono.telegram.repositories;

import lt.kono.telegram.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Users repository
 */
public interface UserRepository extends JpaRepository<User, Long> {
}
