package com.utn.vinoteca.features.product.specification;

import com.utn.vinoteca.features.product.entity.ProductEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class ProductSpecification {

    public static Specification<ProductEntity> hasName(String name) {
        return (root, query, criteriaBuilder) -> {
            if(name == null || name.isBlank()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + name.toLowerCase() + "%"
            );
        };
    }

    public static Specification<ProductEntity> isActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if(active == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("active"),
                    active
            );
        };
    }

    public static Specification<ProductEntity> hasCategory(UUID categoryId) {
        return (root, query, criteriaBuilder) -> {
            if (categoryId == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("category").get("externalId"),categoryId
            );
        };
    }

    public static Specification<ProductEntity> hasSupplier(UUID supplierId) {
        return (root, query, criteriaBuilder) -> {
            if (supplierId == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("supplier").get("externalId"),supplierId
            );
        };
    }

    public static Specification<ProductEntity> hasBrand(UUID brandId) {
        return (root, query, criteriaBuilder) -> {
            if (brandId == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("brand").get("externalId"),brandId
            );
        };
    }
}
