package com.utn.vinoteca.features.wine.specification;

import com.utn.vinoteca.features.wine.entity.WineEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class WineSpecification {

    public static Specification<WineEntity> hasName(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.isBlank()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + name.toLowerCase() + "%"
            );
        };
    }

    public static Specification<WineEntity> isActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (active == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("active"),
                    active
            );
        };
    }

    public static Specification<WineEntity> hasWineType(UUID wineTypeId) {
        return (root, query, criteriaBuilder) -> {
            if (wineTypeId == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("wineType").get("externalId"), wineTypeId
            );
        };
    }

    public static Specification<WineEntity> hasDistributor(UUID distributorId) {
        return (root, query, criteriaBuilder) -> {
            if (distributorId == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("distributor").get("externalId"), distributorId
            );
        };
    }

    public static Specification<WineEntity> hasWinery(UUID wineryId) {
        return (root, query, criteriaBuilder) -> {
            if (wineryId == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("winery").get("externalId"), wineryId
            );
        };
    }

    public static Specification<WineEntity> hasGrapeVariety(UUID grapeVarietyId) {
        return (root, query, criteriaBuilder) -> {
            if (grapeVarietyId == null) {
                return null;
            }
            if (query != null) {
                query.distinct(true);
            }
            return criteriaBuilder.equal(
                    root.join("grapeVarieties").get("externalId"), grapeVarietyId
            );
        };
    }

    public static Specification<WineEntity> hasVintage(Integer vintage) {
        return (root, query, criteriaBuilder) -> {
            if (vintage == null) {
                return null;
            }
            return criteriaBuilder.equal(
                    root.get("vintage"), vintage
            );
        };
    }

    public static Specification<WineEntity> hasRegion(String region) {
        return (root, query, criteriaBuilder) -> {
            if (region == null || region.isBlank()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("region")),
                    "%" + region.toLowerCase() + "%"
            );
        };
    }
}
