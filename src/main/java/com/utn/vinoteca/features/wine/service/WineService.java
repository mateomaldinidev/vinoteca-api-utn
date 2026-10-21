package com.utn.vinoteca.features.wine.service;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
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

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class WineService implements IWineService{
    private final WineRepository wineRepository;
    private final DistributorRepository distributorRepository;
    private final WineTypeRepository wineTypeRepository;
    private final WineryRepository wineryRepository;
    private final WineVariantRepository wineVariantRepository;

    private final WineMapper wineMapper;

    //CREATE
    @Transactional
    @Override
    public WineResponseDTO create(WineRequestDTO wineRequestDTO) {
        DistributorEntity distributor = distributorRepository.findByExternalId(wineRequestDTO.getDistributorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with ID: ", wineRequestDTO.getDistributorId()));
        if(!distributor.isActive()){
            throw new BusinessRuleException("Cannot assign inactive distributor");
        }

        WineTypeEntity wineType = wineTypeRepository.findByExternalId(wineRequestDTO.getWineTypeId())
                        .orElseThrow(() -> new ResourceNotFoundException("WineType not found with ID: ", wineRequestDTO.getWineTypeId()));
        if(!wineType.getActive()){
            throw new BusinessRuleException("Cannot assign inactive wineType");
        }
        WineryEntity winery =
                wineryRepository.findByExternalId(wineRequestDTO.getWineryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Winery not found. ID: ", wineRequestDTO.getWineryId()));

        if(!winery.getActive()){
            throw new BusinessRuleException("Cannot assign inactive winery");
        }


        if(wineRepository.existsByNameAndWinery(wineRequestDTO.getName(), winery)){
            throw new BusinessRuleException("Wine already exists for this winery.");
        }

        WineEntity wine = wineMapper.toEntity(wineRequestDTO, distributor, wineType, winery);
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

        DistributorEntity distributor = distributorRepository.findByExternalId(wineRequestDTO.getDistributorId())
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with ID: ", wineRequestDTO.getDistributorId()));

        if (!distributor.isActive()) {
            throw new BusinessRuleException("Cannot assign inactive distributor");
        }

        WineTypeEntity wineType = wineTypeRepository.findByExternalId(wineRequestDTO.getWineTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("WineType not found with ID: ", wineRequestDTO.getWineTypeId()));

        if (!wineType.getActive()) {
            throw new BusinessRuleException("Cannot assign inactive wineType");
        }

        WineryEntity winery = wineryRepository.findByExternalId(wineRequestDTO.getWineryId())
                .orElseThrow(() -> new ResourceNotFoundException("Winery not found. ID: ", wineRequestDTO.getWineryId()));

        if (!winery.getActive()) {
            throw new BusinessRuleException("Cannot assign inactive winery");
        }

        WineEntity duplicated = wineRepository.findByNameAndWinery(wineRequestDTO.getName(), winery)
                .orElse(null);

        if (duplicated != null && !duplicated.getExternalId().equals(wine.getExternalId()))
        {
            throw new BusinessRuleException("Wine already exists for this winery.");
        }

        wineMapper.updateEntity(wine, wineRequestDTO, distributor, wineType, winery);

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
    public List<WineResponseDTO> search(String name, UUID distributorId, UUID wineTypeId, UUID wineryId){
        Specification<WineEntity> specification = Specification
                .where(WineSpecification.hasName(name))
                .and(WineSpecification.isActive(true))
                .and(WineSpecification.hasDistributor(distributorId))
                .and(WineSpecification.hasWineType(wineTypeId))
                .and(WineSpecification.hasWinery(wineryId));

        return wineRepository.findAll(specification)
                .stream()
                .map(wineMapper::toResponse)
                .toList();
    }

    //LIST ALL INACTIVES
    @Override
    public List<WineResponseDTO> getInactive(String name, UUID distributorId, UUID wineTypeId, UUID wineryId) {

        Specification<WineEntity> specification = Specification
                .where(WineSpecification.hasName(name))
                .and(WineSpecification.isActive(false))
                .and(WineSpecification.hasDistributor(distributorId))
                .and(WineSpecification.hasWineType(wineTypeId))
                .and(WineSpecification.hasWinery(wineryId));

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

}