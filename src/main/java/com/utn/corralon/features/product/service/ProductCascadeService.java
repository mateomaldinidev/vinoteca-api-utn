package com.utn.corralon.features.product.service;

import com.utn.corralon.features.product.entity.ProductEntity;
import com.utn.corralon.features.product.repository.ProductRepository;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Centraliza la desactivacion en cascada de productos y variantes cuando se
 * elimina logicamente una categoria, marca o proveedor.
 */
@Service
@RequiredArgsConstructor
public class ProductCascadeService {

    private final ProductRepository productRepository;

    public long countByCategory(UUID categoryExternalId) {
        return productRepository.countByCategory_ExternalId(categoryExternalId);
    }

    public long countByBrand(UUID brandExternalId) {
        return productRepository.countByBrand_ExternalId(brandExternalId);
    }

    public long countBySupplier(UUID supplierExternalId) {
        return productRepository.countBySupplier_ExternalId(supplierExternalId);
    }

    @Transactional
    public void deactivateByCategory(UUID categoryExternalId) {
        deactivate(productRepository.findByCategory_ExternalId(categoryExternalId));
    }

    @Transactional
    public void deactivateByBrand(UUID brandExternalId) {
        deactivate(productRepository.findByBrand_ExternalId(brandExternalId));
    }

    @Transactional
    public void deactivateBySupplier(UUID supplierExternalId) {
        deactivate(productRepository.findBySupplier_ExternalId(supplierExternalId));
    }

    private void deactivate(List<ProductEntity> products) {
        for (ProductEntity product : products) {
            product.setActive(false);

            List<ProductVariantEntity> variants = product.getProductVariants();
            if (variants != null) {
                variants.forEach(variant -> variant.setActive(false));
            }
        }
    }
}
