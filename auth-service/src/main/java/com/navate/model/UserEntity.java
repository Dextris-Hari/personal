package com.navate.model;

import com.navate.enums.Role;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

@Entity
@Table(name = "tbl_users")
@Data
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "username", unique = true)
    private String username;
    @Column(name = "password", unique = true)
    private String password; // bcrypt hashed
    @Column(name = "mobile_number", unique = true)
    private String mobileNumber; // <-- added field
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tbl_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> role;

}
