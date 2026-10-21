package com.utn.vinoteca.features.grapeVariety.service;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.features.grapeVariety.dto.GrapeVarietyRequestDTO;
import com.utn.vinoteca.features.grapeVariety.dto.GrapeVarietyResponseDTO;
import com.utn.vinoteca.features.grapeVariety.entity.GrapeVarietyEntity;
import com.utn.vinoteca.features.grapeVariety.mapper.GrapeVarietyMapper;
import com.utn.vinoteca.features.grapeVariety.repository.GrapeVarietyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GrapeVarietyService implements IGrapeVarietyService {

    private final GrapeVarietyRepository grapeVarietyRepository;
    private final GrapeVarietyMapper grapeVarietyMapper;

    @Override
    @Transactional
    public GrapeVarietyResponseDTO create(GrapeVarietyRequestDTO dto) {
        if (grapeVarietyRepository.existsByName(dto.getName())) {
            throw new BusinessRuleException("Grape variety with name '" + dto.getName() + "' already exists.");
        }

        GrapeVarietyEntity entity = grapeVarietyMapper.toEntity(dto);
        entity.setActive(true);

        return grapeVarietyMapper.toResponse(grapeVarietyRepository.save(entity));
    }

    @Override
    public List<GrapeVarietyResponseDTO> getAll() {
        return grapeVarietyRepository.findAllByActiveTrue().stream()
                .map(grapeVarietyMapper::toResponse)
                .toList();
    }

    @Override
    public GrapeVarietyResponseDTO getByExternalId(UUID externalId) {
        return grapeVarietyRepository.findByExternalIdAndActiveTrue(externalId)
                .map(grapeVarietyMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Grape variety not found with ID: ", externalId));
    }

    @Override
    @Transactional
    public GrapeVarietyResponseDTO update(UUID externalId, GrapeVarietyRequestDTO dto) {
        GrapeVarietyEntity entity = grapeVarietyRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Grape variety not found with ID: ", externalId));

        if (!entity.getName().equalsIgnoreCase(dto.getName()) && grapeVarietyRepository.existsByName(dto.getName())) {
            throw new BusinessRuleException("Grape variety with name '" + dto.getName() + "' already exists.");
        }

        grapeVarietyMapper.updateEntity(entity, dto);

        return grapeVarietyMapper.toResponse(grapeVarietyRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID externalId) {
        GrapeVarietyEntity entity = grapeVarietyRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Grape variety not found with ID: ", externalId));

        entity.setActive(false);
        grapeVarietyRepository.save(entity);
    }

    @Override
    @Transactional
    public void activate(UUID externalId) {
        GrapeVarietyEntity entity = grapeVarietyRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Grape variety not found with ID: ", externalId));

        if (entity.getActive()) {
            throw new BusinessRuleException("Grape variety with ID: " + externalId + " is already active.");
        }

        entity.setActive(true);
        grapeVarietyRepository.save(entity);
    }

    @Override
    public List<GrapeVarietyResponseDTO> getInactive() {
        return grapeVarietyRepository.findAllByActiveFalse().stream()
                .map(grapeVarietyMapper::toResponse)
                .toList();
    }
}
