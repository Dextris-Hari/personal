package com.navate.security;

import com.navate.enums.Role;
import com.navate.model.UserEntity;
import com.navate.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner init(UserRepository repo, BCryptPasswordEncoder encoder) {
        return args -> {
            if (repo.findByUsername("admin").isEmpty()) {
                var u = new UserEntity();
                u.setUsername("admin");
                u.setPassword(encoder.encode("adminpass"));
                u.setRole(Set.of(Role.STADIUM_ADMIN));
                repo.save(u);
            }
            if (repo.findByUsername("staff").isEmpty()) {
                var u = new UserEntity();
                u.setUsername("staff");
                u.setPassword(encoder.encode("staffpass"));
                u.setRole(Set.of(Role.STADIUM_STAFF));
                repo.save(u);
            }
            if (repo.findByUsername("zamboni").isEmpty()) {
                var u = new UserEntity();
                u.setUsername("zamboni");
                u.setPassword(encoder.encode("zambonipass"));
                u.setRole(Set.of(Role.ZAMBONI_OPERATOR));
                repo.save(u);
            }
            if (repo.findByUsername("front").isEmpty()) {
                var u = new UserEntity();
                u.setUsername("front");
                u.setPassword(encoder.encode("frontpass"));
                u.setRole(Set.of(Role.FRONT_DESK));
                repo.save(u);
            }
        };
    }
}