package paf_grp_i.pong.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "app_user") // Renamed to avoid SQL reserved keyword "user"
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 45)
    private String email;

    @Column(nullable = false, length = 64)
    private String password;

    @Column(name = "first_name", nullable = true, length = 20)
    private String firstName;

    @Column(name = "last_name", nullable = true, length = 20)
    private String lastName;
}
