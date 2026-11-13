package com.utn.corralon.features.product.mapper;

import com.utn.corralon.config.ModelMapperConfig;
import com.utn.corralon.features.brand.entity.BrandEntity;
import com.utn.corralon.features.category.entity.CategoryEntity;
import com.utn.corralon.features.product.dto.ProductResponseDTO;
import com.utn.corralon.features.product.entity.ProductEntity;
import com.utn.corralon.features.productImage.mapper.ProductImageMapper;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import com.utn.corralon.features.supplier.entity.SupplierEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private ProductMapper productMapper;

    @BeforeEach
    void setUp() {
        productMapper = new ProductMapper(
                new ModelMapperConfig().modelMapper(),
                new ProductImageMapper()
        );
    }

    @Test
    void toResponse_shouldReturnCheapestActiveVariantWithStock() {
        ProductEntity product = product(
                variant("750 ml", "1500.00", 3, true),
                variant("1 L", "1200.00", 1, true),
                variant("2 L", "900.00", 0, true),
                variant("3 L", "800.00", 2, false)
        );

        ProductResponseDTO response = productMapper.toResponse(product);

        assertThat(response.getMinPrice()).isEqualByComparingTo("1200.00");
    }

    @Test
    void toResponse_shouldReturnNullWhenThereAreNoAvailableVariants() {
        ProductEntity product = product(
                variant("750 ml", "1500.00", 0, true),
                variant("1 L", "1200.00", 2, false)
        );

        ProductResponseDTO response = productMapper.toResponse(product);

        assertThat(response.getMinPrice()).isNull();
    }

    @Test
    void toResponse_shouldIgnoreVariantsWithInvalidPrice() {
        ProductEntity product = product(
                variant("750 ml", "0.00", 3, true),
                variant("1 L", "2500.00", 1, true)
        );

        ProductResponseDTO response = productMapper.toResponse(product);

        assertThat(response.getMinPrice()).isEqualByComparingTo("2500.00");
    }

    private ProductEntity product(ProductVariantEntity... variants) {
        return ProductEntity.builder()
                .externalId(UUID.randomUUID())
                .name("Vino de prueba")
                .active(true)
                .supplier(SupplierEntity.builder()
                        .externalId(UUID.randomUUID())
                        .name("Proveedor")
                        .active(true)
                        .build())
                .category(CategoryEntity.builder()
                        .externalId(UUID.randomUUID())
                        .name("Categoría")
                        .active(true)
                        .build())
                .brand(BrandEntity.builder()
                        .externalId(UUID.randomUUID())
                        .name("Marca")
                        .active(true)
                        .build())
                .productVariants(List.of(variants))
                .productImages(List.of())
                .build();
    }

    private ProductVariantEntity variant(String attribute, String price, int stock, boolean active) {
        return ProductVariantEntity.builder()
                .externalId(UUID.randomUUID())
                .attribute(attribute)
                .price(new BigDecimal(price))
                .stock(stock)
                .active(active)
                .build();
    }
}
