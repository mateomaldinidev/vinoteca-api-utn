package com.utn.vinoteca.features.supplier.service;

import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.features.supplier.dto.SupplierRequestDTO;
import com.utn.vinoteca.features.supplier.dto.SupplierResponseDTO;
import com.utn.vinoteca.features.supplier.entity.SupplierEntity;
import com.utn.vinoteca.features.supplier.mapper.SupplierMapper;
import com.utn.vinoteca.features.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierService implements ISupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    @Override
    @Transactional
    public SupplierResponseDTO create(SupplierRequestDTO dto) {
        if (supplierRepository.existsByNameAndActiveTrue(dto.getName())) {
            throw new BusinessRuleException("Supplier with name '" + dto.getName() + "' already exists.");
        }
        SupplierEntity entity = supplierMapper.toEntity(dto);
        // SupplierEntity.active es 'boolean' primitivo, no puede ser null.
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

        SupplierEntity saved = supplierRepository.save(entity);
        return supplierMapper.toResponse(saved);
    }

    @Override
    public List<SupplierResponseDTO> getAll() {
        return supplierRepository.findAllByActiveTrue().stream()
                .map(supplierMapper::toResponse)
                .toList();
    }

    @Override
    public SupplierResponseDTO getByExternalId(UUID externalId) {
        return supplierRepository.findByExternalIdAndActiveTrue(externalId)
                .map(supplierMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: ",externalId));
    }

    @Override
    @Transactional
    public SupplierResponseDTO update(UUID externalId, SupplierRequestDTO dto) {
        SupplierEntity entity = supplierRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: ", externalId));

        if (supplierRepository.existsByNameAndActiveTrue(dto.getName()) &&
            !entity.getName().equalsIgnoreCase(dto.getName())) {
            throw new BusinessRuleException("Supplier with name '" + dto.getName() + "' already exists.");
        }

        supplierMapper.updateEntity(entity, dto);
        SupplierEntity updated = supplierRepository.save(entity);
        return supplierMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(UUID externalId) {
        SupplierEntity entity = supplierRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: ", externalId));
        entity.setActive(false);
        supplierRepository.save(entity);
    }

    @Override
    @Transactional
    public void activate(UUID externalId) {
        SupplierEntity entity = supplierRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: ", externalId));

        if (entity.isActive()) {
            throw new BusinessRuleException("Supplier with ID: " + externalId + " is already active.");
        }
        entity.setActive(true);
        supplierRepository.save(entity);
    }

    @Override
    public List<SupplierResponseDTO> getInactive() {
        return supplierRepository.findAllByActiveFalse().stream()
                .map(supplierMapper::toResponse)
                .toList();
    }
}