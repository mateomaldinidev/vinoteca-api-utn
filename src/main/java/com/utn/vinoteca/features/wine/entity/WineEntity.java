package com.utn.vinoteca.features.wine.entity;

import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.features.distributor.entity.DistributorEntity;
import com.utn.vinoteca.features.grapeVariety.entity.GrapeVarietyEntity;
import com.utn.vinoteca.features.wineType.entity.WineTypeEntity;
import com.utn.vinoteca.features.winery.entity.WineryEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "wines")
public class WineEntity {

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

    @Column(name = "vintage")
    private Integer vintage;

    @Column(name = "region", length = 120)
    private String region;

    @Column(name = "alcohol", precision = 4, scale = 1)
    private BigDecimal alcohol;

    @Column(name = "tasting_notes", length = 1000)
    private String tastingNotes;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Builder.Default
    @OneToMany(mappedBy = "wine", fetch = FetchType.LAZY)
    private List<WineVariantEntity> wineVariants = new ArrayList<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "wine_grape_varieties",
            joinColumns = @JoinColumn(name = "wine_id"),
            inverseJoinColumns = @JoinColumn(name = "grape_variety_id")
    )
    private Set<GrapeVarietyEntity> grapeVarieties = new HashSet<>();

    @ManyToOne()
    @JoinColumn(name = "distributorId", nullable = false)
    private DistributorEntity distributor;

    @ManyToOne()
    @JoinColumn(name = "wineTypeId", nullable = false)
    private WineTypeEntity wineType;

    @ManyToOne()
    @JoinColumn(name = "wineryId", nullable = false)
    private WineryEntity winery;
}
