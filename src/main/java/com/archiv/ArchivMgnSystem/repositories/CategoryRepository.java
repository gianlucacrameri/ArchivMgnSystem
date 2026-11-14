package com.archiv.ArchivMgnSystem.repositories;

import com.archiv.ArchivMgnSystem.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findFirstByNameIgnoreCase(String name);


}
