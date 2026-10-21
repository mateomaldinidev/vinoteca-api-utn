package com.utn.vinoteca.features.distributor.service;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.features.distributor.dto.DistributorRequestDTO;
import com.utn.vinoteca.features.distributor.dto.DistributorResponseDTO;
import com.utn.vinoteca.features.distributor.entity.DistributorEntity;
import com.utn.vinoteca.features.distributor.mapper.DistributorMapper;
import com.utn.vinoteca.features.distributor.repository.DistributorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DistributorService implements IDistributorService {

    private final DistributorRepository distributorRepository;
    private final DistributorMapper distributorMapper;

    @Override
    @Transactional
    public DistributorResponseDTO create(DistributorRequestDTO dto) {
        if (distributorRepository.existsByNameAndActiveTrue(dto.getName())) {
            throw new BusinessRuleException("Distributor with name '" + dto.getName() + "' already exists.");
        }
        DistributorEntity entity = distributorMapper.toEntity(dto);
        // DistributorEntity.active es 'boolean' primitivo, no puede ser null.
        // Si el DTO no lo especifica, el builder lo inicializará a 'false'.
        // Aquí lo forzamos a 'true' si no se especificó en el DTO o si se quiere que por defecto sea activo.
        // La validación @NotNull en el DTO asegura que siempre venga un valor.
        if (entity.isActive() == false && dto.getActive() == null) { // Si el builder lo puso en false y el DTO no lo especificó
             entity.setActive(true); // Asumimos que por defecto se crea activo
        } else if (dto.getActive() != null) {
            entity.setActive(dto.getActive());
        } else {
            entity.setActive(true);
        }

        DistributorEntity saved = distributorRepository.save(entity);
        return distributorMapper.toResponse(saved);
    }

    @Override
    public List<DistributorResponseDTO> getAll() {
        return distributorRepository.findAllByActiveTrue().stream()
                .map(distributorMapper::toResponse)
                .toList();
    }

    @Override
    public DistributorResponseDTO getByExternalId(UUID externalId) {
        return distributorRepository.findByExternalIdAndActiveTrue(externalId)
                .map(distributorMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with ID: ",externalId));
    }

    @Override
    @Transactional
    public DistributorResponseDTO update(UUID externalId, DistributorRequestDTO dto) {
        DistributorEntity entity = distributorRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with ID: ", externalId));

        if (distributorRepository.existsByNameAndActiveTrue(dto.getName()) &&
            !entity.getName().equalsIgnoreCase(dto.getName())) {
            throw new BusinessRuleException("Distributor with name '" + dto.getName() + "' already exists.");
        }

        distributorMapper.updateEntity(entity, dto);
        DistributorEntity updated = distributorRepository.save(entity);
        return distributorMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(UUID externalId) {
        DistributorEntity entity = distributorRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with ID: ", externalId));
        entity.setActive(false);
        distributorRepository.save(entity);
    }

    @Override
    @Transactional
    public void activate(UUID externalId) {
        DistributorEntity entity = distributorRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with ID: ", externalId));

        if (entity.isActive()) {
            throw new BusinessRuleException("Distributor with ID: " + externalId + " is already active.");
        }
        entity.setActive(true);
        distributorRepository.save(entity);
    }

    @Override
    public List<DistributorResponseDTO> getInactive() {
        return distributorRepository.findAllByActiveFalse().stream()
                .map(distributorMapper::toResponse)
                .toList();
    }
}