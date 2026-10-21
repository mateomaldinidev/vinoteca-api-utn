package com.utn.vinoteca.features.wineType.service;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.features.wineType.dto.WineTypeRequestDTO;
import com.utn.vinoteca.features.wineType.dto.WineTypeResponseDTO;
import com.utn.vinoteca.features.wineType.entity.WineTypeEntity;
import com.utn.vinoteca.features.wineType.mapper.WineTypeMapper;
import com.utn.vinoteca.features.wineType.repository.WineTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WineTypeService implements IWineTypeService {

    private final WineTypeRepository wineTypeRepository;
    private final WineTypeMapper wineTypeMapper;

    @Override
    @Transactional
    public WineTypeResponseDTO create(WineTypeRequestDTO dto) {
        if (wineTypeRepository.existsByNameAndActiveTrue(dto.getName())) {
            throw new BusinessRuleException("WineType with name '" + dto.getName() + "' already exists.");
        }
        WineTypeEntity entity = wineTypeMapper.toEntity(dto);
        if (entity.getActive() == null) {
            entity.setActive(true);
        }
        WineTypeEntity saved = wineTypeRepository.save(entity);
        return wineTypeMapper.toResponse(saved);
    }

    @Override
    public List<WineTypeResponseDTO> getAll() {
        return wineTypeRepository.findAllByActiveTrue().stream()
                .map(wineTypeMapper::toResponse)
                .toList();
    }

    @Override
    public WineTypeResponseDTO getByExternalId(UUID externalId) {
        return wineTypeRepository.findByExternalIdAndActiveTrue(externalId)
                .map(wineTypeMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Wine type not found with ID: ", externalId));
    }

    @Override
    @Transactional
    public WineTypeResponseDTO update(UUID externalId, WineTypeRequestDTO dto) {
        WineTypeEntity entity = wineTypeRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Wine type not found with ID: ", externalId));

        if (wineTypeRepository.existsByNameAndActiveTrue(dto.getName()) &&
            !entity.getName().equalsIgnoreCase(dto.getName())) {
            throw new BusinessRuleException("WineType with name '" + dto.getName() + "' already exists.");
        }

        wineTypeMapper.updateEntity(entity, dto);
        WineTypeEntity updated = wineTypeRepository.save(entity);
        return wineTypeMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(UUID externalId) {
        WineTypeEntity entity = wineTypeRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Wine type not found with ID: ", externalId));
        entity.setActive(false);
        wineTypeRepository.save(entity);
    }

    @Override
    @Transactional
    public void activate(UUID externalId) {
        WineTypeEntity entity = wineTypeRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Wine type not found with ID: ", externalId));

        if (entity.getActive()) {
            throw new BusinessRuleException("WineType with ID: " + externalId + " is already active.");
        }
        entity.setActive(true);
        wineTypeRepository.save(entity);
    }

    @Override
    public List<WineTypeResponseDTO> getInactive() {
        return wineTypeRepository.findAllByActiveFalse().stream()
                .map(wineTypeMapper::toResponse)
                .toList();
    }
}