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
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tbl_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> role;
    // getters/setters
   /* public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public String getUsername(){return username;}
    public void setUsername(String username){this.username=username;}
    public String getPassword(){return password;}
    public void setPassword(String password){this.password=password;}
    public Set<Role> getRoles(){return roles;}
    public void setRoles(Set<Role> roles){this.roles=roles;}*/
}
