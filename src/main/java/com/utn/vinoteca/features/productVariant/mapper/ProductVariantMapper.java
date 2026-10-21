package com.utn.vinoteca.features.productVariant.mapper;

import com.utn.vinoteca.features.product.entity.ProductEntity;
import com.utn.vinoteca.features.productVariant.dto.ProductVariantRequestDTO;
import com.utn.vinoteca.features.productVariant.dto.ProductVariantResponseDTO;
import com.utn.vinoteca.features.productVariant.entity.ProductVariantEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ProductVariantMapper {
    private final ModelMapper modelMapper;

    public ProductVariantMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public ProductVariantResponseDTO toResponse(ProductVariantEntity variant) {
        ProductVariantResponseDTO dto =
                modelMapper.map(
                        variant,
                        ProductVariantResponseDTO.class);
        dto.setProductId(variant.getProduct().getExternalId());
        dto.setProductName(variant.getProduct().getName());
        return dto;
    }

    public ProductVariantEntity toEntity(ProductVariantRequestDTO dto,
                                         ProductEntity product) {
        ProductVariantEntity variant =
                modelMapper.map(
                        dto,
                        ProductVariantEntity.class);

        variant.setProduct(product);
        return variant;
    }

    public void updateEntity(ProductVariantEntity variant,
                        ProductVariantRequestDTO dto,
                        ProductEntity product) {
        modelMapper.map(dto, variant);
        variant.setProduct(product);

    }
}
