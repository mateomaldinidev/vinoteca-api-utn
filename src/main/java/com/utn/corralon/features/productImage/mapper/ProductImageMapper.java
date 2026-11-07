package com.utn.corralon.features.productImage.mapper;

import com.utn.corralon.features.productImage.dto.ProductImageResponseDTO;
import com.utn.corralon.features.productImage.entity.ProductImageEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductImageMapper {

    private static final String PUBLIC_PREFIX = "/uploads/";

    public ProductImageResponseDTO toResponse(ProductImageEntity image) {
        String normalizedPath = image.getImagePath() == null
                ? ""
                : image.getImagePath().replace("\\", "/");

        return ProductImageResponseDTO.builder()
                .externalId(image.getExternalId())
                .imagePath(normalizedPath)
                .url(PUBLIC_PREFIX + normalizedPath)
                .principal(image.isPrincipal())
                .build();
    }
}
