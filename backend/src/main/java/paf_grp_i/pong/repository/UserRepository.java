package paf_grp_i.pong.repository;

import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;

import paf_grp_i.pong.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    // default: JPQL
    // @Query("SELECT u FROM User u WHERE u.email = ?1")
    // alternately: native SQL
    // @Query(value = "SELECT * FROM user u WHERE u.email = ?1", nativeQuery = true)
    // alternately: without @Query, rely on naming conventions
    public User findByEmail(String email);

    boolean existsByEmail(String email);
}
