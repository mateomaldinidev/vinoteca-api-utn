package com.utn.vinoteca.features.wineVariant.service;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.features.wine.entity.WineEntity;
import com.utn.vinoteca.features.wine.repository.WineRepository;
import com.utn.vinoteca.features.wineVariant.dto.WineVariantRequestDTO;
import com.utn.vinoteca.features.wineVariant.dto.WineVariantResponseDTO;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.features.wineVariant.mapper.WineVariantMapper;
import com.utn.vinoteca.features.wineVariant.repository.WineVariantRepository;
import com.utn.vinoteca.features.wineVariant.specification.WineVariantSpecification;
import com.utn.vinoteca.features.stockMovement.dto.StockMovementRequestDTO;
import com.utn.vinoteca.features.stock.service.StockService;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class WineVariantService implements IWineVariantService {

    private final WineVariantRepository wineVariantRepository;
    private final WineRepository wineRepository;
    private final StockService stockService;

    private final WineVariantMapper wineVariantMapper;

    //CREATE
    @Transactional
    @Override
    public WineVariantResponseDTO create(WineVariantRequestDTO wineVariantRequestDTO) {
        WineEntity wine = wineRepository.findByExternalId(wineVariantRequestDTO.getWineId())
                .orElseThrow(() -> new ResourceNotFoundException("Wine not found. ID: ", wineVariantRequestDTO.getWineId()));

        if (!wine.isActive()) {
            throw new BusinessRuleException("Cannot create variant for inactive wine");
        }
        if (wineVariantRepository.existsByWineAndAttribute(wine, wineVariantRequestDTO.getAttribute())) {
            throw new BusinessRuleException("Wine variant already exists");
        }

        validateWholesaleConfig(wineVariantRequestDTO);

        WineVariantEntity variant = wineVariantMapper.toEntity(wineVariantRequestDTO, wine);
        wineVariantRepository.save(variant);
        return wineVariantMapper.toResponse(variant);
    }

    //UPDATE
    @Transactional
    @Override
    public WineVariantResponseDTO update(UUID externalId, WineVariantRequestDTO wineVariantRequestDTO) {
        WineVariantEntity variant = getActiveVariant(externalId);

        WineEntity wine = wineRepository
                .findByExternalId(wineVariantRequestDTO.getWineId())
                .orElseThrow(() -> new ResourceNotFoundException("Wine not found. ID: ", wineVariantRequestDTO.getWineId()));
        if (!wine.isActive()) {
            throw new BusinessRuleException("Cannot assign inactive wine");
        }
        WineVariantEntity duplicated = wineVariantRepository
                .findByWineAndAttribute(wine, wineVariantRequestDTO.getAttribute())
                .orElse(null);

        if (duplicated != null && !duplicated.getExternalId().equals(variant.getExternalId())) {
            throw new BusinessRuleException("Wine variant already exists");
        }

        validateWholesaleConfig(wineVariantRequestDTO);

        wineVariantMapper.updateEntity(variant, wineVariantRequestDTO, wine);

        wineVariantRepository.save(variant);
        return wineVariantMapper.toResponse(variant);
    }

    //DELETE (ACTIVE = FALSE)
    @Transactional
    @Override
    public void delete(UUID externalId) {
        WineVariantEntity variant = getActiveVariant(externalId);

        variant.setActive(false);
        wineVariantRepository.save(variant);
    }

    //activacion para cuando esta active = false
    @Override
    @Transactional
    public void activate(UUID externalId) {
        WineVariantEntity variant = wineVariantRepository.findByExternalId(externalId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Wine variant not found with ID: ", externalId));

        if (variant.getActive()) {
            throw new BusinessRuleException("Wine variant is already active");
        }

        if (!variant.getWine().isActive()) {
            throw new BusinessRuleException("Cannot activate variant of inactive wine");
        }
        variant.setActive(true);
        wineVariantRepository.save(variant);
    }

    //GET BY ID
    @Override
    public WineVariantResponseDTO getById(UUID externalId) {
        WineVariantEntity variant = getActiveVariant(externalId);

        return wineVariantMapper.toResponse(variant);
    }

    //SEARCH WITH FILTERS (ACTIVES)
    @Override
    public List<WineVariantResponseDTO> search(String attribute, BigDecimal minPrice, BigDecimal maxPrice, Integer minStock, UUID wineId, UUID wineTypeId, UUID wineryId, String wineName) {
        Specification<WineVariantEntity> specification =
                Specification
                        .where(WineVariantSpecification.hasAttribute(attribute)
                                .and(WineVariantSpecification.isActive(true))
                                .and(WineVariantSpecification.hasMinPrice(minPrice))
                                .and(WineVariantSpecification.hasMaxPrice(maxPrice))
                                .and(WineVariantSpecification.hasMinStock(minStock))
                                .and(WineVariantSpecification.hasWine(wineId))
                                .and(WineVariantSpecification.hasWineType(wineTypeId))
                                .and(WineVariantSpecification.hasWinery(wineryId))
                                .and(WineVariantSpecification.hasWineName(wineName)));

        return wineVariantRepository
                .findAll(specification)
                .stream()
                .map(wineVariantMapper::toResponse)
                .toList();
    }

    //GET ALL INACTIVES
    @Override
    public List<WineVariantResponseDTO> getInactive(String attribute, BigDecimal minPrice, BigDecimal maxPrice, Integer minStock, UUID wineId, UUID wineTypeId, UUID wineryId, String wineName) {

        Specification<WineVariantEntity> specification =
                Specification
                        .where(WineVariantSpecification.hasAttribute(attribute))
                        .and(WineVariantSpecification.isActive(false))
                        .and(WineVariantSpecification.hasMinPrice(minPrice))
                        .and(WineVariantSpecification.hasMaxPrice(maxPrice))
                        .and(WineVariantSpecification.hasMinStock(minStock))
                        .and(WineVariantSpecification.hasWine(wineId))
                        .and(WineVariantSpecification.hasWineType(wineTypeId))
                        .and(WineVariantSpecification.hasWinery(wineryId))
                        .and(WineVariantSpecification.hasWineName(wineName));

        return wineVariantRepository
                .findAll(specification)
                .stream()
                .map(wineVariantMapper::toResponse)
                .toList();
    }

    //solo para entrada de mercaderia
    @Transactional
    @Override
    public WineVariantResponseDTO registerEntry(StockMovementRequestDTO dto) {
        WineVariantEntity variant = getActiveVariant(dto.getVariantId());

        stockService.registerEntry(variant, dto.getQuantity(), dto.getReason());

        return wineVariantMapper.toResponse(variant);
    }

    //Adjust positive or negative - es para correcciones de stock por si hay un error humano
    @Transactional
    @Override
    public WineVariantResponseDTO adjustStock(StockMovementRequestDTO dto) {
        WineVariantEntity variant = getActiveVariant(dto.getVariantId());

        stockService.adjust(variant, dto.getQuantity(), dto.getReason());

        return wineVariantMapper.toResponse(variant);
    }

    //consultar stock
    @Override
    public Integer getAvailableStock(UUID variantId) {
        WineVariantEntity variant = getActiveVariant(variantId);
        return variant.getStock();
    }


    //metodo interno para vuscar el formato
    private WineVariantEntity getActiveVariant(UUID variantId) {
        return wineVariantRepository.findByExternalIdAndActiveTrue(variantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Wine variant not found with ID:", variantId)
                );
    }

    private void validateWholesaleConfig(WineVariantRequestDTO dto) {
        boolean hasPrice = dto.getWholesalePrice() != null;
        boolean hasMinQty = dto.getWholesaleMinQty() != null;

        if (hasPrice && !hasMinQty) {
            throw new BusinessRuleException("wholesaleMinQty is required when wholesalePrice is set");
        }
        if (!hasPrice && hasMinQty) {
            throw new BusinessRuleException("wholesalePrice is required when wholesaleMinQty is set");
        }
        if (hasPrice && hasMinQty) {
            if (dto.getWholesalePrice().compareTo(dto.getPrice()) >= 0) {
                throw new BusinessRuleException("wholesalePrice must be less than regular price");
            }
        }
    }
}
