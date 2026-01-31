package paf_grp_i.pong.repository;

import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;

import paf_grp_i.pong.model.User;

/**
 * Repository interface for {@link User} entities.
 * Provides database access methods for user-related operations.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address.
     * Uses Spring Data JPA naming conventions to generate the query automatically.
     *
     * @param email the email address to search for
     * @return the user with the specified email, or null if not found
     */
    // default: JPQL
    // @Query("SELECT u FROM User u WHERE u.email = ?1")
    // alternately: native SQL
    // @Query(value = "SELECT * FROM user u WHERE u.email = ?1", nativeQuery = true)
    // alternately: without @Query, rely on naming conventions
    public User findByEmail(String email);

    /**
     * Checks whether a user with the specified email exists.
     *
     * @param email the email address to check
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(String email);
}
