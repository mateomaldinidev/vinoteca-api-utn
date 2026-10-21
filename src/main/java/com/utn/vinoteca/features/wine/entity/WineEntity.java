package com.utn.vinoteca.features.wine.entity;

import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.features.distributor.entity.DistributorEntity;
import com.utn.vinoteca.features.wineType.entity.WineTypeEntity;
import com.utn.vinoteca.features.winery.entity.WineryEntity;
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

    @Column(name = "active", nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "wine", fetch = FetchType.LAZY)
    private List<WineVariantEntity> wineVariants;

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
