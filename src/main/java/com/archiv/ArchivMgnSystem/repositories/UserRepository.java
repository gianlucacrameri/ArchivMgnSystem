package com.archiv.ArchivMgnSystem.repositories;

import com.archiv.ArchivMgnSystem.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
