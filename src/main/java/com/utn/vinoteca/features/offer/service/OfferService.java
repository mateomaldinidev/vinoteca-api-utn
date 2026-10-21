package com.utn.vinoteca.features.offer.service;

import com.utn.vinoteca.exception.BadRequestException;
import com.utn.vinoteca.exception.BusinessRuleException;
import com.utn.vinoteca.exception.ResourceNotFoundException;
import com.utn.vinoteca.security.enums.Roles;
import com.utn.vinoteca.security.repository.CredentialsRepository;
import com.utn.vinoteca.security.enums.Roles;
import com.utn.vinoteca.features.notifications.service.IEmailService;
import com.utn.vinoteca.features.offer.dto.OfferRequestDTO;
import com.utn.vinoteca.features.offer.dto.OfferResponseDTO;
import com.utn.vinoteca.features.offer.entity.OfferEntity;
import com.utn.vinoteca.features.offer.mapper.OfferMapper;
import com.utn.vinoteca.features.offer.repository.OfferRepository;
import com.utn.vinoteca.features.offer_product.dto.OfferProductRequestDTO;
import com.utn.vinoteca.features.offer_product.entity.OfferProductEntity;
import com.utn.vinoteca.features.offer_product.mapper.OfferProductMapper;
import com.utn.vinoteca.features.offer_product.repository.OfferProductRepository;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.features.wineVariant.repository.WineVariantRepository;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferService implements IOfferService {

    private final OfferRepository offerRepository;
    private final OfferProductRepository offerProductRepository;
    private final WineVariantRepository wineVariantRepository;
    private final UserRepository userRepository;
    private final CredentialsRepository credentialsRepository;
    private final IEmailService emailService;

    private final OfferMapper offerMapper;
    private final OfferProductMapper offerProductMapper;

    // Crear una nueva oferta con fechas y porcentaje de descuento válidos
    @Override
    @Transactional
    public OfferResponseDTO create(OfferRequestDTO dto) {
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessRuleException("End date must be after start date");
        }
        if (dto.getDiscountPercentage().compareTo(BigDecimal.ZERO) <= 0
                || dto.getDiscountPercentage().compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessRuleException("Discount percentage must be between 0 and 100");
        }

        OfferEntity entity = offerMapper.toEntity(dto);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setActive(true);

        OfferEntity saved = offerRepository.save(entity);
        return offerMapper.toResponse(saved);
    }

    // Agregar un vino a una oferta activa, validando que no esté repetido y que el precio con descuento sea menor al original
    @Override
    @Transactional
    public OfferResponseDTO addProductToOffer(UUID offerExternalId, OfferProductRequestDTO dto) {
        if (dto.getOfferId() != null && !dto.getOfferId().equals(offerExternalId)) {
            throw new BadRequestException("offerId in the body does not match the path");
        }

        OfferEntity offer = offerRepository.findByExternalIdAndActiveTrue(offerExternalId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with ID: ", offerExternalId));

        WineVariantEntity variant = wineVariantRepository.findByExternalIdAndActiveTrue(dto.getWineVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Wine variant not found with ID: ", dto.getWineVariantId()));

        if (offerProductRepository.existsByOfferExternalIdAndWineVariantExternalId(
                offerExternalId, dto.getWineVariantId())) {
            throw new BusinessRuleException("Wine variant is already associated with this offer");
        }

        if (dto.getDiscountedPrice().compareTo(variant.getPrice()) >= 0) {
            throw new BusinessRuleException("Discounted price must be less than the regular price");
        }

        OfferProductEntity offerProduct = offerProductMapper.toEntity(dto, offer, variant);
        offerProductRepository.save(offerProduct);
        offer.getOfferProducts().add(offerProduct);

        return offerMapper.toResponse(offer);
    }

    // Sacar un vino de una oferta activa
    @Override
    @Transactional
    public OfferResponseDTO removeProductFromOffer(UUID offerExternalId, UUID wineVariantExternalId) {
        OfferEntity offer = offerRepository.findByExternalIdAndActiveTrue(offerExternalId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with ID: ", offerExternalId));

        OfferProductEntity offerProduct = offerProductRepository
                .findByOfferExternalIdAndWineVariantExternalId(offerExternalId, wineVariantExternalId)
                .orElseThrow(() -> new BusinessRuleException("Wine variant is not associated with this offer"));

        offer.getOfferProducts().remove(offerProduct);
        offerRepository.save(offer);

        return offerMapper.toResponse(offer);
    }

    // Buscar una oferta por su ID, activa o inactiva
    @Override
    public OfferResponseDTO getByExternalId(UUID externalId) {
        OfferEntity offer = offerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with ID: ", externalId));
        return offerMapper.toResponse(offer);
    }

    // Listar todas las ofertas activas
    @Override
    public List<OfferResponseDTO> getAllActive() {
        return offerRepository.findAllByActiveTrue().stream()
                .map(offerMapper::toResponse)
                .toList();
    }

    // Listar todas las ofertas inactivas
    @Override
    public List<OfferResponseDTO> getAllInactive() {
        return offerRepository.findAllByActiveFalse().stream()
                .map(offerMapper::toResponse)
                .toList();
    }

    // Activar una oferta que esté inactiva y que no haya vencido
    @Override
    @Transactional
    public void activate(UUID externalId) {
        OfferEntity offer = offerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with ID: ", externalId));

        if (offer.getActive()) {
            throw new BusinessRuleException("Offer is already active");
        }

        if (offer.getEndDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot activate an offer whose end date has already passed");
        }

        offer.setActive(true);
        offerRepository.save(offer);

        OfferResponseDTO offerDTO = offerMapper.toResponse(offer);

        List<UserEntity> customers = userRepository.findAllByActiveTrue().stream()
                .filter(u -> credentialsRepository.findByUsername(u.getEmail())
                        .map(CredentialsEntity -> CredentialsEntity.getRoles().stream()
                                .anyMatch(r -> r.getRole() == Roles.ROLE_CUSTOMER))
                        .orElse(false))
                .toList();
        for (UserEntity customer : customers) {
            emailService.sendNewPromotionEmail(customer.getEmail(), customer.getName(), offerDTO);
        }
    }

    // Desactivar una oferta que esté activa
    @Override
    @Transactional
    public void deactivate(UUID externalId) {
        OfferEntity offer = offerRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with ID: ", externalId));

        if (!offer.getActive()) {
            throw new BusinessRuleException("Offer is already inactive");
        }

        offer.setActive(false);
        offerRepository.save(offer);
    }
}
