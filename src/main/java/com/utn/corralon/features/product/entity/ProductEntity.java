package com.utn.corralon.features.product.entity;

import com.utn.corralon.features.productImage.entity.ProductImageEntity;
import com.utn.corralon.features.productVariant.entity.ProductVariantEntity;
import com.utn.corralon.features.supplier.entity.SupplierEntity;
import com.utn.corralon.features.category.entity.CategoryEntity;
import com.utn.corralon.features.brand.entity.BrandEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "externalId", nullable = false, unique = true, updatable = false)
    @UuidGenerator
    private UUID externalId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductVariantEntity> productVariants;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImageEntity> productImages;

    @ManyToOne()
    @JoinColumn(name = "supplierId", nullable = false)
    private SupplierEntity supplier;

    @ManyToOne()
    @JoinColumn(name = "categoryId", nullable = false)
    private CategoryEntity category;

    @ManyToOne()
    @JoinColumn(name = "brandId", nullable = false)
    private BrandEntity brand;
}
