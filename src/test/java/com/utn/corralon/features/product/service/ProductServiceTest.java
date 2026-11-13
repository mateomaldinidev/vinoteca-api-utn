package com.utn.corralon.features.product.service;

import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.features.brand.entity.BrandEntity;
import com.utn.corralon.features.brand.repository.BrandRepository;
import com.utn.corralon.features.category.entity.CategoryEntity;
import com.utn.corralon.features.category.repository.CategoryRepository;
import com.utn.corralon.features.product.entity.ProductEntity;
import com.utn.corralon.features.product.mapper.ProductMapper;
import com.utn.corralon.features.product.repository.ProductRepository;
import com.utn.corralon.features.productVariant.repository.ProductVariantRepository;
import com.utn.corralon.features.supplier.entity.SupplierEntity;
import com.utn.corralon.features.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private SupplierEntity supplier;
    private CategoryEntity category;
    private BrandEntity brand;

    @BeforeEach
    void setUp() {
        supplier = SupplierEntity.builder().active(true).build();
        category = CategoryEntity.builder().active(true).build();
        brand = BrandEntity.builder().active(true).build();
    }

    @Test
    void activate_shouldActivateInactiveProductWhenRelatedEntitiesAreActive() {
        UUID externalId = UUID.randomUUID();
        ProductEntity product = product(externalId, false);

        when(productRepository.findByExternalId(externalId)).thenReturn(Optional.of(product));

        productService.activate(externalId);

        assertThat(product.isActive()).isTrue();
        verify(productRepository).save(product);
    }

    @Test
    void activate_shouldRejectProductThatIsAlreadyActive() {
        UUID externalId = UUID.randomUUID();
        ProductEntity product = product(externalId, true);

        when(productRepository.findByExternalId(externalId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.activate(externalId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Product is already active.");

        verify(productRepository, never()).save(any(ProductEntity.class));
    }

    @Test
    void activate_shouldRejectInactiveProductWhenSupplierIsInactive() {
        UUID externalId = UUID.randomUUID();
        ProductEntity product = product(externalId, false);
        supplier.setActive(false);

        when(productRepository.findByExternalId(externalId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.activate(externalId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cannot activate product with inactive supplier.");

        assertThat(product.isActive()).isFalse();
        verify(productRepository, never()).save(any(ProductEntity.class));
    }

    private ProductEntity product(UUID externalId, boolean active) {
        return ProductEntity.builder()
                .externalId(externalId)
                .name("Producto de prueba")
                .active(active)
                .supplier(supplier)
                .category(category)
                .brand(brand)
                .build();
    }
}
