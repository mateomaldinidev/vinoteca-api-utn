package com.utn.vinoteca.features.wineVariant.entity;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.StockInsufficientException;
import com.utn.vinoteca.features.wine.entity.WineEntity;
import com.utn.vinoteca.features.stockMovement.entity.StockMovementEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "wine_variants")
public class WineVariantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "externalId", nullable = false, unique = true, updatable = false)
    @UuidGenerator
    private UUID externalId;

    @Column(name = "attribute", nullable = false)
    private String attribute;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "wholesale_price")
    private BigDecimal wholesalePrice;

    @Column(name = "wholesaleMinQty")
    private Integer wholesaleMinQty;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wineId", nullable = false)
    private WineEntity wine;

    @OneToMany(mappedBy = "variant")
    private List<StockMovementEntity> stockMovements;

    public void decrementStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Quantity must be positive");
        }

        if (this.stock < quantity) {
            throw new StockInsufficientException(this.externalId.toString());
        }

        this.stock -= quantity;
    }

    public void restoreStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Quantity must be positive");
        }

        this.stock += quantity;
    }

    public void adjustStock(int delta) {
        int newStock = this.stock + delta;

        if (newStock < 0) {
            throw new BusinessRuleException("Stock cannot be negative");
        }

        this.stock = newStock;
    }
}