package com.utn.corralon.features.productVariant.service;

import com.utn.corralon.features.product.entity.ProductEntity;
import com.utn.corralon.features.product.repository.ProductRepository;
import com.utn.corralon.features.productVariant.dto.ProductVariantRequestDTO;
import com.utn.corralon.features.productVariant.dto.ProductVariantResponseDTO;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import com.utn.corralon.features.productVariant.mapper.ProductVariantMapper;
import com.utn.corralon.features.productVariant.repository.ProductVariantRepository;
import com.utn.corralon.features.stockMovement.service.IStockService;
import com.utn.corralon.features.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductVariantServiceTest {

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IStockService stockService;

    @Mock
    private ProductVariantMapper productVariantMapper;

    @InjectMocks
    private ProductVariantService productVariantService;

    @Test
    void update_shouldNotModifyStockDirectly() {
        UUID variantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        ProductEntity product = ProductEntity.builder()
                .externalId(productId)
                .active(true)
                .build();

        ProductVariantEntity variant = ProductVariantEntity.builder()
                .externalId(variantId)
                .stock(10)
                .active(true)
                .product(product)
                .build();

        ProductVariantRequestDTO request = ProductVariantRequestDTO.builder()
                .attribute("750ml")
                .price(BigDecimal.TEN)
                .stock(999)
                .productId(productId)
                .build();

        when(productVariantRepository.findByExternalIdAndActiveTrue(variantId))
                .thenReturn(Optional.of(variant));
        when(productRepository.findByExternalId(productId))
                .thenReturn(Optional.of(product));
        when(productVariantRepository.findByProductAndAttribute(product, "750ml"))
                .thenReturn(Optional.of(variant));

        // Simula el mapeo historico, que pisaba el stock con el valor del DTO.
        doAnswer(invocation -> {
            ProductVariantEntity target = invocation.getArgument(0);
            ProductVariantRequestDTO dto = invocation.getArgument(1);
            target.setStock(dto.getStock());
            return null;
        }).when(productVariantMapper).updateEntity(any(), any(), any());

        when(productVariantMapper.toResponse(variant))
                .thenReturn(ProductVariantResponseDTO.builder().stock(10).build());

        ProductVariantResponseDTO response = productVariantService.update(variantId, request);

        assertThat(variant.getStock()).isEqualTo(10);
        assertThat(response.getStock()).isEqualTo(10);
        verify(productVariantRepository).save(variant);
    }
}
