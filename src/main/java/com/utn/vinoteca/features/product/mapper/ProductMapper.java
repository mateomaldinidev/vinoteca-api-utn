package com.utn.vinoteca.features.product.mapper;

import com.utn.vinoteca.features.brand.entity.BrandEntity;
import com.utn.vinoteca.features.category.entity.CategoryEntity;
import com.utn.vinoteca.features.product.dto.ProductRequestDTO;
import com.utn.vinoteca.features.product.dto.ProductResponseDTO;
import com.utn.vinoteca.features.product.entity.ProductEntity;
import com.utn.vinoteca.features.supplier.entity.SupplierEntity;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@AllArgsConstructor

@Component
public class ProductMapper {
    private final ModelMapper modelMapper;


    public ProductResponseDTO toResponse(ProductEntity product) {
        ProductResponseDTO response = modelMapper.map(product, ProductResponseDTO.class);

        response.setSupplierId(product.getSupplier().getExternalId());
        response.setSupplierName(product.getSupplier().getName());

        response.setCategoryId(product.getCategory().getExternalId());
        response.setCategoryName(product.getCategory().getName());

        response.setBrandId(product.getBrand().getExternalId());
        response.setBrandName(product.getBrand().getName());

        return response;
    }

    public ProductEntity toEntity(ProductRequestDTO dto,
                                  SupplierEntity supplier,
                                  CategoryEntity category,
                                  BrandEntity brand) {
        ProductEntity product = modelMapper.map(dto, ProductEntity.class);

        product.setSupplier(supplier);
        product.setCategory(category);
        product.setBrand(brand);

        return product;
    }

    public void updateEntity(ProductEntity product,
                             ProductRequestDTO dto,
                             SupplierEntity supplier,
                             CategoryEntity category,
                             BrandEntity brand) {
        modelMapper.map(dto, product);

        product.setSupplier(supplier);
        product.setCategory(category);
        product.setBrand(brand);
    }


}