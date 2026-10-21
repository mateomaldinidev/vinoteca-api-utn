package com.utn.vinoteca.features.wine.service;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.features.grapeVariety.entity.GrapeVarietyEntity;
import com.utn.vinoteca.features.grapeVariety.repository.GrapeVarietyRepository;
import com.utn.vinoteca.features.winery.entity.WineryEntity;
import com.utn.vinoteca.features.winery.repository.WineryRepository;
import com.utn.vinoteca.features.wineType.entity.WineTypeEntity;
import com.utn.vinoteca.features.wineType.repository.WineTypeRepository;
import com.utn.vinoteca.features.wine.dto.WineDeleteResponseDTO;
import com.utn.vinoteca.features.wine.dto.WineRequestDTO;
import com.utn.vinoteca.features.wine.dto.WineResponseDTO;
import com.utn.vinoteca.features.wine.entity.WineEntity;
import com.utn.vinoteca.features.wine.mapper.WineMapper;
import com.utn.vinoteca.features.wine.repository.WineRepository;
import com.utn.vinoteca.features.wine.specification.WineSpecification;
import com.utn.vinoteca.features.wineVariant.dto.DisabledVariantDTO;
import com.utn.vinoteca.features.wineVariant.repository.WineVariantRepository;
import com.utn.vinoteca.features.distributor.entity.DistributorEntity;
import com.utn.vinoteca.features.distributor.repository.DistributorRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@AllArgsConstructor
public class WineService implements IWineService {
    private final WineRepository wineRepository;
    private final DistributorRepository distributorRepository;
    private final WineTypeRepository wineTypeRepository;
    private final WineryRepository wineryRepository;
    private final WineVariantRepository wineVariantRepository;
    private final GrapeVarietyRepository grapeVarietyRepository;

    private final WineMapper wineMapper;

    //CREATE
    @Transactional
    @Override
    public WineResponseDTO create(WineRequestDTO wineRequestDTO) {
        DistributorEntity distributor = findActiveDistributor(wineRequestDTO.getDistributorId());
        WineTypeEntity wineType = findActiveWineType(wineRequestDTO.getWineTypeId());
        WineryEntity winery = findActiveWinery(wineRequestDTO.getWineryId());

        validateVintage(wineRequestDTO.getVintage());
        assertNotDuplicated(wineRequestDTO, winery, null);

        Set<GrapeVarietyEntity> grapeVarieties = resolveGrapeVarieties(wineRequestDTO.getGrapeVarietyIds());

        WineEntity wine = wineMapper.toEntity(wineRequestDTO, distributor, wineType, winery, grapeVarieties);
        wine.setActive(true);

        wineRepository.save(wine);

        return wineMapper.toResponse(wine);
    }

    //UPDATE
    @Transactional
    @Override
    public WineResponseDTO update(UUID externalId, WineRequestDTO wineRequestDTO) {
        WineEntity wine = wineRepository
                .findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Wine not found with ID: ", externalId));

        DistributorEntity distributor = findActiveDistributor(wineRequestDTO.getDistributorId());
        WineTypeEntity wineType = findActiveWineType(wineRequestDTO.getWineTypeId());
        WineryEntity winery = findActiveWinery(wineRequestDTO.getWineryId());

        validateVintage(wineRequestDTO.getVintage());
        assertNotDuplicated(wineRequestDTO, winery, wine.getExternalId());

        Set<GrapeVarietyEntity> grapeVarieties = resolveGrapeVarieties(wineRequestDTO.getGrapeVarietyIds());

        wineMapper.updateEntity(wine, wineRequestDTO, distributor, wineType, winery, grapeVarieties);

        wineRepository.save(wine);

        return wineMapper.toResponse(wine);
    }

    //DELETE logico = active = false
    @Transactional
    @Override
    public WineDeleteResponseDTO delete(UUID externalId) {
        WineEntity wine = wineRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Wine not found. ID: ", externalId));

        List<DisabledVariantDTO> disabledVariants = wine.getWineVariants()
                        .stream()
                        .map(variant -> {

                            variant.setActive(false);
                            wineVariantRepository.save(variant);

                            DisabledVariantDTO dto = new DisabledVariantDTO();

                            dto.setExternalId(variant.getExternalId());
                            dto.setAttribute(variant.getAttribute());

                            return dto;
                        })
                        .toList();

        wine.setActive(false);
        wineRepository.save(wine);

        WineDeleteResponseDTO responseDTO = new WineDeleteResponseDTO();

        responseDTO.setWineExternalId(wine.getExternalId());
        responseDTO.setWineName(wine.getName());
        responseDTO.setActive(false);
        responseDTO.setDisabledVariants(disabledVariants);

        return responseDTO;
    }

    //ACTIVATE despues del delete
    @Transactional
    @Override
    public void activate(UUID externalId) {
        WineEntity wine = wineRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Wine not found. ID: ", externalId));

        if (!wine.isActive()) {
            throw new BusinessRuleException("Wine is already active.");
        }

        if (!wine.getDistributor().isActive()) {
            throw new BusinessRuleException("Cannot activate wine with inactive distributor.");
        }

        if (!wine.getWineType().getActive()) {
            throw new BusinessRuleException("Cannot activate wine with inactive wineType.");
        }

        if (!wine.getWinery().getActive()) {
            throw new BusinessRuleException("Cannot activate wine with inactive winery.");
        }

        wine.setActive(true);
        wineRepository.save(wine);
    }

    //SEARCH WITH FILTERS
    @Override
    public List<WineResponseDTO> search(String name,
                                        UUID distributorId,
                                        UUID wineTypeId,
                                        UUID wineryId,
                                        UUID grapeVarietyId,
                                        Integer vintage,
                                        String region) {
        Specification<WineEntity> specification = Specification
                .where(WineSpecification.hasName(name))
                .and(WineSpecification.isActive(true))
                .and(WineSpecification.hasDistributor(distributorId))
                .and(WineSpecification.hasWineType(wineTypeId))
                .and(WineSpecification.hasWinery(wineryId))
                .and(WineSpecification.hasGrapeVariety(grapeVarietyId))
                .and(WineSpecification.hasVintage(vintage))
                .and(WineSpecification.hasRegion(region));

        return wineRepository.findAll(specification)
                .stream()
                .map(wineMapper::toResponse)
                .toList();
    }

    //LIST ALL INACTIVES
    @Override
    public List<WineResponseDTO> getInactive(String name,
                                             UUID distributorId,
                                             UUID wineTypeId,
                                             UUID wineryId,
                                             UUID grapeVarietyId,
                                             Integer vintage,
                                             String region) {

        Specification<WineEntity> specification = Specification
                .where(WineSpecification.hasName(name))
                .and(WineSpecification.isActive(false))
                .and(WineSpecification.hasDistributor(distributorId))
                .and(WineSpecification.hasWineType(wineTypeId))
                .and(WineSpecification.hasWinery(wineryId))
                .and(WineSpecification.hasGrapeVariety(grapeVarietyId))
                .and(WineSpecification.hasVintage(vintage))
                .and(WineSpecification.hasRegion(region));

        return wineRepository
                .findAll(specification)
                .stream()
                .map(wineMapper::toResponse)
                .toList();
    }

    //GET BY ID
    @Override
    public WineResponseDTO getById(UUID externalId) {
        WineEntity wine = wineRepository
                .findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Wine not found. ID: ", externalId));
        return wineMapper.toResponse(wine);
    }

    private DistributorEntity findActiveDistributor(UUID distributorId) {
        DistributorEntity distributor = distributorRepository.findByExternalId(distributorId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with ID: ", distributorId));

        if (!distributor.isActive()) {
            throw new BusinessRuleException("Cannot assign inactive distributor");
        }

        return distributor;
    }

    private WineTypeEntity findActiveWineType(UUID wineTypeId) {
        WineTypeEntity wineType = wineTypeRepository.findByExternalId(wineTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Wine type not found with ID: ", wineTypeId));

        if (!wineType.getActive()) {
            throw new BusinessRuleException("Cannot assign an inactive wine type");
        }

        return wineType;
    }

    private WineryEntity findActiveWinery(UUID wineryId) {
        WineryEntity winery = wineryRepository.findByExternalId(wineryId)
                .orElseThrow(() -> new ResourceNotFoundException("Winery not found. ID: ", wineryId));

        if (!winery.getActive()) {
            throw new BusinessRuleException("Cannot assign inactive winery");
        }

        return winery;
    }

    private void validateVintage(Integer vintage) {
        if (vintage == null) {
            return;
        }

        int maxVintage = Year.now().getValue() + 1;

        if (vintage < 1900 || vintage > maxVintage) {
            throw new BusinessRuleException("Vintage must be between 1900 and " + maxVintage);
        }
    }

    private void assertNotDuplicated(WineRequestDTO dto, WineryEntity winery, UUID currentExternalId) {
        Optional<WineEntity> duplicated = dto.getVintage() == null
                ? wineRepository.findByNameAndWinery(dto.getName(), winery)
                : wineRepository.findByNameAndWineryAndVintage(dto.getName(), winery, dto.getVintage());

        boolean belongsToAnotherWine = duplicated.isPresent()
                && (currentExternalId == null || !duplicated.get().getExternalId().equals(currentExternalId));

        if (belongsToAnotherWine) {
            throw new BusinessRuleException("Wine already exists for this winery and vintage.");
        }
    }

    private Set<GrapeVarietyEntity> resolveGrapeVarieties(Set<UUID> grapeVarietyIds) {
        if (grapeVarietyIds == null || grapeVarietyIds.isEmpty()) {
            throw new BusinessRuleException("At least one grape variety is required.");
        }

        Set<GrapeVarietyEntity> grapeVarieties = new HashSet<>();

        for (UUID grapeVarietyId : grapeVarietyIds) {
            GrapeVarietyEntity grapeVariety = grapeVarietyRepository.findByExternalIdAndActiveTrue(grapeVarietyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Grape variety not found with ID: ", grapeVarietyId));

            grapeVarieties.add(grapeVariety);
        }

        return grapeVarieties;
    }
}
