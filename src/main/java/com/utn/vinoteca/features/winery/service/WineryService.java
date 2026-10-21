package com.utn.vinoteca.features.winery.service;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.features.winery.dto.WineryRequestDTO;
import com.utn.vinoteca.features.winery.dto.WineryResponseDTO;
import com.utn.vinoteca.features.winery.entity.WineryEntity;
import com.utn.vinoteca.features.winery.mapper.WineryMapper;
import com.utn.vinoteca.features.winery.repository.WineryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WineryService implements IWineryService {
    private final WineryRepository wineryRepository;
    private final WineryMapper wineryMapper;

    @Override
    @Transactional
    public WineryResponseDTO create(WineryRequestDTO dto) {
        if (wineryRepository.existsByNameAndActiveTrue(dto.getName())) {
            throw new BusinessRuleException("Winery with name '" + dto.getName() + "' already exists.");
        }
        WineryEntity entity = wineryMapper.toEntity(dto);
        if (entity.getActive() == null) {
            entity.setActive(true);
        }
        WineryEntity saved = wineryRepository.save(entity);
        return wineryMapper.toResponse(saved);
    }

    @Override
    public List<WineryResponseDTO> getAll() {
        return wineryRepository.findAllByActiveTrue().stream()
                .map(wineryMapper::toResponse)
                .toList();
    }

    @Override
    public WineryResponseDTO getByExternalId(UUID externalId) {
        return wineryRepository.findByExternalIdAndActiveTrue(externalId)
                .map(wineryMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Winery not found with ID: ", externalId));
    }

    @Override
    @Transactional
    public WineryResponseDTO update(UUID externalId, WineryRequestDTO dto) {
        WineryEntity entity = wineryRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Winery not found with ID: ", externalId));

        // Validar si el nuevo nombre ya existe en otra bodega activa
        if (wineryRepository.existsByNameAndActiveTrue(dto.getName()) &&
                !entity.getName().equalsIgnoreCase(dto.getName())) { // Solo si el nombre cambió y ya existe
            throw new BusinessRuleException("Winery with name '" + dto.getName() + "' already exists.");
        }

        wineryMapper.updateEntity(entity, dto);
        WineryEntity updated = wineryRepository.save(entity);
        return wineryMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(UUID externalId) {
        WineryEntity entity = wineryRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Winery not found with ID: ", externalId));
        entity.setActive(false);
        wineryRepository.save(entity);
    }

    @Override
    @Transactional
    public void activate(UUID externalId) {
        WineryEntity entity = wineryRepository.findByExternalId(externalId) // Busca activa o inactiva
                .orElseThrow(() -> new ResourceNotFoundException("Winery not found with ID: ", externalId));

        if (entity.getActive()) {
            throw new BusinessRuleException("Winery with ID: " + externalId + " is already active.");
        }
        entity.setActive(true);
        wineryRepository.save(entity);
    }

    @Override
    public List<WineryResponseDTO> getInactive() {
        return wineryRepository.findAllByActiveFalse().stream()
                .map(wineryMapper::toResponse)
                .toList();
    }
}