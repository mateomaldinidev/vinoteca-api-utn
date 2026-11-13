package com.utn.corralon.features.product.mapper;

import com.utn.corralon.features.brand.entity.BrandEntity;
import com.utn.corralon.features.category.entity.CategoryEntity;
import com.utn.corralon.features.product.dto.ProductRequestDTO;
import com.utn.corralon.features.product.dto.ProductResponseDTO;
import com.utn.corralon.features.product.entity.ProductEntity;
import com.utn.corralon.features.productImage.dto.ProductImageResponseDTO;
import com.utn.corralon.features.productImage.entity.ProductImageEntity;
import com.utn.corralon.features.productImage.mapper.ProductImageMapper;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import com.utn.corralon.features.supplier.entity.SupplierEntity;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor
@Component
public class ProductMapper {

    private final ModelMapper modelMapper;
    private final ProductImageMapper productImageMapper;

    public ProductResponseDTO toResponse(ProductEntity product) {
        ProductResponseDTO response = modelMapper.map(product, ProductResponseDTO.class);

        response.setSupplierId(product.getSupplier().getExternalId());
        response.setSupplierName(product.getSupplier().getName());

        response.setCategoryId(product.getCategory().getExternalId());
        response.setCategoryName(product.getCategory().getName());

        response.setBrandId(product.getBrand().getExternalId());
        response.setBrandName(product.getBrand().getName());

        List<ProductImageResponseDTO> images = (product.getProductImages() == null
                ? List.<ProductImageEntity>of()
                : product.getProductImages())
                .stream()
                .sorted(Comparator.comparing(ProductImageEntity::isPrincipal).reversed()
                        .thenComparing(ProductImageEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(productImageMapper::toResponse)
                .toList();

        response.setImages(images);
        response.setPrincipalImagePath(images.stream()
                .filter(ProductImageResponseDTO::isPrincipal)
                .map(ProductImageResponseDTO::getUrl)
                .findFirst()
                .orElseGet(() -> images.isEmpty() ? null : images.get(0).getUrl()));

        response.setMinPrice(resolveMinPrice(product));

        return response;
    }

    private BigDecimal resolveMinPrice(ProductEntity product) {
        if (product.getProductVariants() == null) {
            return null;
        }

        return product.getProductVariants().stream()
                .filter(variant -> Boolean.TRUE.equals(variant.getActive()))
                .filter(variant -> variant.getStock() != null && variant.getStock() > 0)
                .map(ProductVariantEntity::getPrice)
                .filter(Objects::nonNull)
                .filter(price -> price.compareTo(BigDecimal.ZERO) > 0)
                .min(Comparator.naturalOrder())
                .orElse(null);
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
