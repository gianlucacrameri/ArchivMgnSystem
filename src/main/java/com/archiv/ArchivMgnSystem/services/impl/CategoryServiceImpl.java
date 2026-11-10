package com.archiv.ArchivMgnSystem.services.impl;


import com.archiv.ArchivMgnSystem.dtos.CategoryDTO;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.exceptions.NotFoundException;
import com.archiv.ArchivMgnSystem.models.Category;
import com.archiv.ArchivMgnSystem.repositories.CategoryRepository;
import com.archiv.ArchivMgnSystem.repositories.ReservationRepository;
import com.archiv.ArchivMgnSystem.repositories.TransactionRepository;
import com.archiv.ArchivMgnSystem.services.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.internal.bytebuddy.description.method.MethodDescription;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final ReservationRepository reservationRepository;
    private final ModelMapper modelMapper;

    @Override
    public Response createCategory(CategoryDTO categoryDTO) {
        Category categoryToSave = modelMapper.map(categoryDTO, Category.class);
        Category savedCategory = categoryRepository.save(categoryToSave);
        savedCategory.setItems(null);

        return Response.builder()
                .status(200)
                .message("Category saved successfully")
                .category(modelMapper.map(savedCategory, CategoryDTO.class))
                .build();
    }

    @Override
    public Response getAllCategories() {
        List<Category> categories = categoryRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        categories.forEach(category -> category.setItems(null));
        List<CategoryDTO> categoryDTOList = modelMapper.map(categories, new TypeToken<List<CategoryDTO>>(){}.getType());

        return Response.builder()
                .status(200)
                .message("success")
                .categories(categoryDTOList)
                .build();
    }

    @Override
    public Response getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        CategoryDTO categoryDTO = modelMapper.map(category, CategoryDTO.class);

        return Response.builder()
                .status(200)
                .message("success")
                .category(categoryDTO)
                .build();
    }

    @Override
    public Response updateCategory(Long id, CategoryDTO categoryDTO) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        existingCategory.setName(categoryDTO.getName());
        categoryRepository.save(existingCategory);

        return Response.builder()
                .status(200)
                .message("Category was successfully updated")
                .build();
    }

    @Override
    @Transactional
    public Response deleteCategory(Long id) {
        categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        // Die Kategorie löscht ihr Material mit (cascade). Dessen Anträge und
        // Reservierungen müssen vorher weg, sonst blockiert die Datenbank.
        reservationRepository.deleteByItemCategoryId(id);
        transactionRepository.deleteByItemCategoryId(id);
        categoryRepository.deleteById(id);

        return Response.builder()
                .status(200)
                .message("Category was successfully deleted")
                .build();
    }
}
