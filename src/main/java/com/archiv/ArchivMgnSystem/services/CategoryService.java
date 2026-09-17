package com.archiv.ArchivMgnSystem.services;

import com.archiv.ArchivMgnSystem.dtos.CategoryDTO;
import com.archiv.ArchivMgnSystem.dtos.Response;

public interface CategoryService {

    Response createCategory(CategoryDTO categoryDTO);

    Response getAllCategories();

    Response getCategoryById(Long id);

    Response updateCategory(Long id, CategoryDTO categoryDTO);

    Response deleteCategory(Long id);
}
