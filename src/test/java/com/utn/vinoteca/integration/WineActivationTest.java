package com.utn.vinoteca.integration;

import com.utn.vinoteca.features.distributor.entity.DistributorEntity;
import com.utn.vinoteca.features.grapeVariety.entity.GrapeVarietyEntity;
import com.utn.vinoteca.features.grapeVariety.repository.GrapeVarietyRepository;
import com.utn.vinoteca.features.wine.dto.WineRequestDTO;
import com.utn.vinoteca.features.wine.dto.WineResponseDTO;
import com.utn.vinoteca.features.wine.entity.WineEntity;
import com.utn.vinoteca.features.wine.service.IWineService;
import com.utn.vinoteca.features.wineType.entity.WineTypeEntity;
import com.utn.vinoteca.features.winery.entity.WineryEntity;
import com.utn.vinoteca.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WineActivationTest extends AbstractIntegrationTest {

    @Autowired
    private IWineService wineService;

    @Autowired
    private GrapeVarietyRepository grapeVarietyRepository;

    @Test
    void deletedWineCanBeReactivated() {
        WineTypeEntity wineType = wineTypeRepository.save(WineTypeEntity.builder()
                .name("Tinto activation")
                .active(true)
                .build());
        WineryEntity winery = wineryRepository.save(WineryEntity.builder()
                .name("Winery activation")
                .active(true)
                .build());
        DistributorEntity distributor = distributorRepository.save(DistributorEntity.builder()
                .name("Distributor activation")
                .contact("activation@vinoteca.test")
                .active(true)
                .build());
        GrapeVarietyEntity variety = grapeVarietyRepository.save(GrapeVarietyEntity.builder()
                .name("Malbec activation")
                .active(true)
                .build());

        WineResponseDTO created = wineService.create(WineRequestDTO.builder()
                .name("Activation wine")
                .description("Test")
                .vintage(2021)
                .region("Mendoza")
                .alcohol(new BigDecimal("13.5"))
                .tastingNotes("Test")
                .grapeVarietyIds(Set.of(variety.getExternalId()))
                .distributorId(distributor.getExternalId())
                .wineTypeId(wineType.getExternalId())
                .wineryId(winery.getExternalId())
                .build());

        wineService.delete(created.getExternalId());
        wineService.activate(created.getExternalId());

        WineEntity stored = wineRepository.findByExternalId(created.getExternalId()).orElseThrow();

        assertThat(stored.isActive()).isTrue();
    }
}
