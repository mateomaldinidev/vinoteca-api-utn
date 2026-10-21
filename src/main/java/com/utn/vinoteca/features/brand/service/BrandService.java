package com.utn.vinoteca.features.brand.service;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.features.brand.dto.BrandRequestDTO;
import com.utn.vinoteca.features.brand.dto.BrandResponseDTO;
import com.utn.vinoteca.features.brand.entity.BrandEntity;
import com.utn.vinoteca.features.brand.mapper.BrandMapper;
import com.utn.vinoteca.features.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandService implements IBrandService {
    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Override
    @Transactional
    public BrandResponseDTO create(BrandRequestDTO dto) {
        if (brandRepository.existsByNameAndActiveTrue(dto.getName())) {
            throw new BusinessRuleException("Brand with name '" + dto.getName() + "' already exists.");
        }
        BrandEntity entity = brandMapper.toEntity(dto);
        if (entity.getActive() == null) {
            entity.setActive(true);
        }
        BrandEntity saved = brandRepository.save(entity);
        return brandMapper.toResponse(saved);
    }

    @Override
    public List<BrandResponseDTO> getAll() {
        return brandRepository.findAllByActiveTrue().stream()
                .map(brandMapper::toResponse)
                .toList();
    }

    @Override
    public BrandResponseDTO getByExternalId(UUID externalId) {
        return brandRepository.findByExternalIdAndActiveTrue(externalId)
                .map(brandMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with ID: ", externalId));
    }

    @Override
    @Transactional
    public BrandResponseDTO update(UUID externalId, BrandRequestDTO dto) {
        BrandEntity entity = brandRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with ID: ", externalId));

        // Validar si el nuevo nombre ya existe en otra marca activa
        if (brandRepository.existsByNameAndActiveTrue(dto.getName()) &&
                !entity.getName().equalsIgnoreCase(dto.getName())) { // Solo si el nombre cambió y ya existe
            throw new BusinessRuleException("Brand with name '" + dto.getName() + "' already exists.");
        }

        brandMapper.updateEntity(entity, dto);
        BrandEntity updated = brandRepository.save(entity);
        return brandMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(UUID externalId) {
        BrandEntity entity = brandRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with ID: ", externalId));
        entity.setActive(false);
        brandRepository.save(entity);
    }

    @Override
    @Transactional
    public void activate(UUID externalId) {
        BrandEntity entity = brandRepository.findByExternalId(externalId) // Busca activa o inactiva
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with ID: ", externalId));

        if (entity.getActive()) {
            throw new BusinessRuleException("Brand with ID: " + externalId + " is already active.");
        }
        entity.setActive(true);
        brandRepository.save(entity);
    }

    @Override
    public List<BrandResponseDTO> getInactive() {
        return brandRepository.findAllByActiveFalse().stream()
                .map(brandMapper::toResponse)
                .toList();
    }
}