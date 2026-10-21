package com.utn.vinoteca.features.wineVariant.specification;

import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public class WineVariantSpecification {

    public static Specification<WineVariantEntity> hasAttribute(String attribute) {
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

    public static Specification<WineVariantEntity> isActive(Boolean active) {
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

    public static Specification<WineVariantEntity> hasMinPrice(BigDecimal minPrice)
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

    public static Specification<WineVariantEntity> hasMaxPrice(BigDecimal maxPrice)
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

    public static Specification<WineVariantEntity> hasMinStock(Integer minStock)
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

    public static Specification<WineVariantEntity> hasWine(UUID wineId) {
        return (root, query, criteriaBuilder) -> {
            if (wineId == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("wine").get("externalId"),
                    wineId
            );
        };
    }

    public static Specification<WineVariantEntity> hasWineType(UUID wineTypeId) {
        return (root, query, criteriaBuilder) -> {
            if(wineTypeId == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("wine").get("wineType").get("externalId"),
                    wineTypeId
            );
        };
    }

    public static Specification<WineVariantEntity> hasWinery(UUID wineryId) {
        return (root, query, criteriaBuilder) -> {
            if(wineryId == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("wine").get("winery").get("externalId"),
                    wineryId
            );
        };
    }

    public static Specification<WineVariantEntity> hasWineName(String wineName) {
        return (root, query, criteriaBuilder) -> {
            if (wineName == null || wineName.isBlank()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("wine").get("name")),
                    "%" + wineName.toLowerCase() + "%"
            );
        };
    }
}
