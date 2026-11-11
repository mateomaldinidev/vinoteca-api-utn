package com.utn.corralon.features.productVariant.service;

import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.features.product.entity.ProductEntity;
import com.utn.corralon.features.product.repository.ProductRepository;
import com.utn.corralon.features.productVariant.dto.ProductVariantRequestDTO;
import com.utn.corralon.features.productVariant.dto.ProductVariantResponseDTO;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import com.utn.corralon.features.productVariant.mapper.ProductVariantMapper;
import com.utn.corralon.features.productVariant.repository.ProductVariantRepository;
import com.utn.corralon.features.productVariant.specification.ProductVariantSpecification;
import com.utn.corralon.features.stockMovement.dto.StockMovementRequestDTO;
import com.utn.corralon.features.stockMovement.service.IStockService;
import com.utn.corralon.features.user.entity.UserEntity;
import com.utn.corralon.features.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ProductVariantService implements IProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final IStockService stockService;

    private final ProductVariantMapper productVariantMapper;

    //CREATE
    @Transactional
    @Override
    public ProductVariantResponseDTO create(ProductVariantRequestDTO productVariantRequestDTO) {
        ProductEntity product = productRepository.findByExternalId(productVariantRequestDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found. ID: ", productVariantRequestDTO.getProductId()));

        if (!product.isActive()) {
            throw new BusinessRuleException("Cannot create variant for inactive product");
        }
        if (productVariantRepository.existsByProductAndAttribute(product, productVariantRequestDTO.getAttribute())) {
            throw new BusinessRuleException("Product variant already exists");
        }

        validateWholesaleConfig(productVariantRequestDTO);

        ProductVariantEntity variant = productVariantMapper.toEntity(productVariantRequestDTO, product);
        productVariantRepository.save(variant);
        return productVariantMapper.toResponse(variant);
    }

    //UPDATE
    @Transactional
    @Override
    public ProductVariantResponseDTO update(UUID externalId, ProductVariantRequestDTO productVariantRequestDTO) {
        ProductVariantEntity variant = getActiveVariant(externalId);

        ProductEntity product = productRepository
                .findByExternalId(productVariantRequestDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found. ID: ", productVariantRequestDTO.getProductId()));
        if (!product.isActive()) {
            throw new BusinessRuleException("Cannot assign inactive product");
        }
        ProductVariantEntity duplicated = productVariantRepository
                .findByProductAndAttribute(product, productVariantRequestDTO.getAttribute())
                .orElse(null);

        if (duplicated != null && !duplicated.getExternalId().equals(variant.getExternalId())) {
            throw new BusinessRuleException("Product variant already exists");
        }

        validateWholesaleConfig(productVariantRequestDTO);

        // El stock solo se modifica a traves de StockService (entradas, ventas,
        // devoluciones y ajustes). Una edicion de variante no debe pisarlo.
        Integer stockBeforeUpdate = variant.getStock();
        productVariantMapper.updateEntity(variant, productVariantRequestDTO, product);
        variant.setStock(stockBeforeUpdate);

        productVariantRepository.save(variant);
        return productVariantMapper.toResponse(variant);
    }

    //DELETE (ACTIVE = FALSE)
    @Transactional
    @Override
    public void delete(UUID externalId) {
        ProductVariantEntity variant = getActiveVariant(externalId);

        variant.setActive(false);
        productVariantRepository.save(variant);
    }

    //activacion para cuando esta active = false
    @Override
    @Transactional
    public void activate(UUID externalId) {
        ProductVariantEntity variant = productVariantRepository.findByExternalId(externalId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product variant not found with ID: ", externalId));

        if (variant.getActive()) {
            throw new BusinessRuleException("Product variant is already active");
        }

        if (!variant.getProduct().isActive()) {
            throw new BusinessRuleException("Cannot activate variant of inactive product");
        }
        variant.setActive(true);
        productVariantRepository.save(variant);
    }

    //GET BY ID
    @Override
    public ProductVariantResponseDTO getById(UUID externalId) {
        ProductVariantEntity variant = getActiveVariant(externalId);

        return productVariantMapper.toResponse(variant);
    }

    //SEARCH WITH FILTERS (ACTIVES)
    @Override
    public List<ProductVariantResponseDTO> search(String attribute, BigDecimal minPrice, BigDecimal maxPrice, Integer minStock, UUID productId, UUID categoryId, UUID brandId, String productName) {
        Specification<ProductVariantEntity> specification =
                Specification
                        .where(ProductVariantSpecification.hasAttribute(attribute)
                                .and(ProductVariantSpecification.isActive(true))
                                .and(ProductVariantSpecification.hasMinPrice(minPrice))
                                .and(ProductVariantSpecification.hasMaxPrice(maxPrice))
                                .and(ProductVariantSpecification.hasMinStock(minStock))
                                .and(ProductVariantSpecification.hasProduct(productId))
                                .and(ProductVariantSpecification.hasCategory(categoryId))
                                .and(ProductVariantSpecification.hasBrand(brandId))
                                .and(ProductVariantSpecification.hasProductName(productName)));

        return productVariantRepository
                .findAll(specification)
                .stream()
                .map(productVariantMapper::toResponse)
                .toList();
    }

    //GET ALL INACTIVES
    @Override
    public List<ProductVariantResponseDTO> getInactive(String attribute, BigDecimal minPrice, BigDecimal maxPrice, Integer minStock, UUID productId, UUID categoryId, UUID brandId, String productName) {

        Specification<ProductVariantEntity> specification =
                Specification
                        .where(ProductVariantSpecification.hasAttribute(attribute))
                        .and(ProductVariantSpecification.isActive(false))
                        .and(ProductVariantSpecification.hasMinPrice(minPrice))
                        .and(ProductVariantSpecification.hasMaxPrice(maxPrice))
                        .and(ProductVariantSpecification.hasMinStock(minStock))
                        .and(ProductVariantSpecification.hasProduct(productId))
                        .and(ProductVariantSpecification.hasCategory(categoryId))
                        .and(ProductVariantSpecification.hasBrand(brandId))
                        .and(ProductVariantSpecification.hasProductName(productName));

        return productVariantRepository
                .findAll(specification)
                .stream()
                .map(productVariantMapper::toResponse)
                .toList();
    }

    //solo para entrada de mercaderia
    @Transactional
    @Override
    public ProductVariantResponseDTO registerEntry(StockMovementRequestDTO dto, String username) {
        ProductVariantEntity variant = stockService.registerEntry(
                dto.getVariantId(),
                dto.getQuantity(),
                dto.getReason(),
                resolveUserId(username)
        );

        return productVariantMapper.toResponse(variant);
    }

    //Adjust positive or negative - es para correcciones de stock por si hay un error humano
    @Transactional
    @Override
    public ProductVariantResponseDTO adjustStock(StockMovementRequestDTO dto, String username) {
        ProductVariantEntity variant = stockService.registerAdjustment(
                dto.getVariantId(),
                dto.getQuantity(),
                dto.getReason(),
                resolveUserId(username)
        );

        return productVariantMapper.toResponse(variant);
    }

    //consultar stock
    @Override
    public Integer getAvailableStock(UUID variantId) {
        ProductVariantEntity variant = getActiveVariant(variantId);
        return variant.getStock();
    }


    //metodo interno para vuscar la variante
    private ProductVariantEntity getActiveVariant(UUID variantId) {
        return productVariantRepository.findByExternalIdAndActiveTrue(variantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product variant not found with ID:", variantId)
                );
    }

    private Long resolveUserId(String username) {
        if (username == null) {
            return null;
        }
        return userRepository.findByEmail(username)
                .map(UserEntity::getId)
                .orElse(null);
    }

    private void validateWholesaleConfig(ProductVariantRequestDTO dto) {
        boolean hasPrice = dto.getWholesalePrice() != null;
        boolean hasMinQty = dto.getWholesaleMinQty() != null;

        if (hasPrice && !hasMinQty) {
            throw new BusinessRuleException("wholesaleMinQty is required when wholesalePrice is set");
        }
        if (!hasPrice && hasMinQty) {
            throw new BusinessRuleException("wholesalePrice is required when wholesaleMinQty is set");
        }
        if (hasPrice && hasMinQty) {
            if (dto.getWholesalePrice().compareTo(dto.getPrice()) >= 0) {
                throw new BusinessRuleException("wholesalePrice must be less than regular price");
            }
        }
    }
}
