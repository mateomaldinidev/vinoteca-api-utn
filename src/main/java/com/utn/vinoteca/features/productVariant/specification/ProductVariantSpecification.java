package com.utn.vinoteca.features.productVariant.specification;

import com.utn.vinoteca.features.productVariant.entity.ProductVariantEntity;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductVariantSpecification {

    public static Specification<ProductVariantEntity> hasAttribute(String attribute) {
        return (root, query, criteriaBuilder) -> {
            if (attribute == null || attribute.isBlank()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("attribute")),
                    "%" + attribute.toLowerCase() + "%"
            );
        };
    }

    public static Specification<ProductVariantEntity> isActive(Boolean active) {
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

    public static Specification<ProductVariantEntity> hasMinPrice(BigDecimal minPrice)
    {
        return (root, query, criteriaBuilder) -> {
            if(minPrice == null) {
                return null;
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                    root.get("price"),
                    minPrice
            );
        };
    }

    public static Specification<ProductVariantEntity> hasMaxPrice(BigDecimal maxPrice)
    {
        return (root, query, criteriaBuilder) -> {
            if(maxPrice == null) {
                return null;
            }
            return criteriaBuilder.lessThanOrEqualTo(
                    root.get("price"),
                    maxPrice
            );
        };
    }

    public static Specification<ProductVariantEntity> hasMinStock(Integer minStock)
    {
        return (root, query, criteriaBuilder) -> {
            if(minStock == null) {
                return null;
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                    root.get("stock"),
                    minStock
            );
        };
    }

    public static Specification<ProductVariantEntity> hasProduct(UUID productId) {
        return (root, query, criteriaBuilder) -> {
            if (productId == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("product").get("externalId"),
                    productId
            );
        };
    }

    public static Specification<ProductVariantEntity> hasCategory(UUID categoryId) {
        return (root, query, criteriaBuilder) -> {
            if(categoryId == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("product").get("category").get("externalId"),
                    categoryId
            );
        };
    }

    public static Specification<ProductVariantEntity> hasBrand(UUID brandId) {
        return (root, query, criteriaBuilder) -> {
            if(brandId == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("product").get("brand").get("externalId"),
                    brandId
            );
        };
    }

    public static Specification<ProductVariantEntity> hasProductName(String productName) {
        return (root, query, criteriaBuilder) -> {
            if (productName == null || productName.isBlank()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("product").get("name")),
                    "%" + productName.toLowerCase() + "%"
            );
        };
    }
}
