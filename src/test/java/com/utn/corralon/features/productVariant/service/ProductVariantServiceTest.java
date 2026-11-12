package com.utn.corralon.features.productVariant.service;

import com.utn.corralon.features.product.entity.ProductEntity;
import com.utn.corralon.features.product.repository.ProductRepository;
import com.utn.corralon.features.productVariant.dto.ProductVariantRequestDTO;
import com.utn.corralon.features.productVariant.dto.ProductVariantResponseDTO;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import com.utn.corralon.features.productVariant.mapper.ProductVariantMapper;
import com.utn.corralon.features.productVariant.repository.ProductVariantRepository;
import com.utn.corralon.features.stockMovement.service.IStockService;
import com.utn.corralon.features.user.entity.UserEntity;
import com.utn.corralon.features.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    void create_withZeroStock_shouldNotRegisterMovement() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        ProductEntity product = ProductEntity.builder()
                .externalId(productId)
                .active(true)
                .build();

        ProductVariantRequestDTO request = ProductVariantRequestDTO.builder()
                .productId(productId)
                .attribute("750ml")
                .price(BigDecimal.valueOf(1000))
                .stock(0)
                .build();

        ProductVariantEntity variant = ProductVariantEntity.builder()
                .externalId(variantId)
                .attribute("750ml")
                .price(BigDecimal.valueOf(1000))
                .stock(0)
                .active(true)
                .product(product)
                .build();

        when(productRepository.findByExternalId(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.existsByProductAndAttribute(product, "750ml")).thenReturn(false);
        when(productVariantMapper.toEntity(request, product)).thenReturn(variant);
        when(productVariantRepository.save(any(ProductVariantEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productVariantMapper.toResponse(variant)).thenReturn(
                ProductVariantResponseDTO.builder()
                        .externalId(variantId)
                        .stock(0)
                        .build()
        );

        ProductVariantResponseDTO response = productVariantService.create(request, "admin@corralon.com");

        assertThat(response).isNotNull();
        assertThat(response.getStock()).isEqualTo(0);
        assertThat(variant.getStock()).isEqualTo(0);
        verify(productVariantRepository).save(variant);
        verifyNoInteractions(stockService);
    }

    @Test
    void create_withPositiveStock_shouldRegisterEntryMovementWithAuthenticatedUser() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        String username = "admin@corralon.com";
        Long userId = 42L;

        ProductEntity product = ProductEntity.builder()
                .externalId(productId)
                .active(true)
                .build();

        ProductVariantRequestDTO request = ProductVariantRequestDTO.builder()
                .productId(productId)
                .attribute("750ml")
                .price(BigDecimal.valueOf(1000))
                .stock(15)
                .build();

        ProductVariantEntity initialVariant = ProductVariantEntity.builder()
                .externalId(variantId)
                .attribute("750ml")
                .price(BigDecimal.valueOf(1000))
                .stock(15) // simulando mapeo inicial
                .active(true)
                .product(product)
                .build();

        ProductVariantEntity variantAfterEntry = ProductVariantEntity.builder()
                .externalId(variantId)
                .attribute("750ml")
                .price(BigDecimal.valueOf(1000))
                .stock(15)
                .active(true)
                .product(product)
                .build();

        when(productRepository.findByExternalId(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.existsByProductAndAttribute(product, "750ml")).thenReturn(false);
        when(productVariantMapper.toEntity(request, product)).thenReturn(initialVariant);
        when(productVariantRepository.save(any(ProductVariantEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmail(username)).thenReturn(Optional.of(UserEntity.builder().id(userId).build()));
        when(stockService.registerEntry(eq(variantId), eq(15), eq("Stock inicial"), eq(userId)))
                .thenReturn(variantAfterEntry);
        when(productVariantMapper.toResponse(variantAfterEntry)).thenReturn(
                ProductVariantResponseDTO.builder()
                        .externalId(variantId)
                        .stock(15)
                        .build()
        );

        ProductVariantResponseDTO response = productVariantService.create(request, username);

        assertThat(response).isNotNull();
        assertThat(response.getStock()).isEqualTo(15);
        verify(productVariantRepository).save(initialVariant);
        verify(stockService).registerEntry(eq(variantId), eq(15), eq("Stock inicial"), eq(userId));
    }

    @Test
    void create_whenStockMovementFails_shouldPropagateExceptionForRollback() throws NoSuchMethodException {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        String username = "admin@corralon.com";
        Long userId = 42L;

        ProductEntity product = ProductEntity.builder()
                .externalId(productId)
                .active(true)
                .build();

        ProductVariantRequestDTO request = ProductVariantRequestDTO.builder()
                .productId(productId)
                .attribute("750ml")
                .price(BigDecimal.valueOf(1000))
                .stock(10)
                .build();

        ProductVariantEntity initialVariant = ProductVariantEntity.builder()
                .externalId(variantId)
                .attribute("750ml")
                .price(BigDecimal.valueOf(1000))
                .stock(10)
                .active(true)
                .product(product)
                .build();

        when(productRepository.findByExternalId(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.existsByProductAndAttribute(product, "750ml")).thenReturn(false);
        when(productVariantMapper.toEntity(request, product)).thenReturn(initialVariant);
        when(productVariantRepository.save(any(ProductVariantEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmail(username)).thenReturn(Optional.of(UserEntity.builder().id(userId).build()));
        when(stockService.registerEntry(eq(variantId), eq(10), eq("Stock inicial"), eq(userId)))
                .thenThrow(new RuntimeException("Simulated stock service failure"));

        assertThatThrownBy(() -> productVariantService.create(request, username))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated stock service failure");

        // Verifica que el metodo create este anotado con @Transactional para garantizar atomicidad y rollback
        Method createWithUsername = ProductVariantService.class.getMethod("create", ProductVariantRequestDTO.class, String.class);
        Method createWithoutUsername = ProductVariantService.class.getMethod("create", ProductVariantRequestDTO.class);

        assertThat(createWithUsername.isAnnotationPresent(Transactional.class) ||
                createWithUsername.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)).isTrue();
        assertThat(createWithoutUsername.isAnnotationPresent(Transactional.class) ||
                createWithoutUsername.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)).isTrue();
    }
}
