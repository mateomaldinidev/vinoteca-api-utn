package com.utn.vinoteca.features.offer.service;

import com.utn.vinoteca.features.offer.dto.OfferRequestDTO;
import com.utn.vinoteca.features.offer.dto.OfferResponseDTO;
import com.utn.vinoteca.features.offerProduct.dto.OfferProductRequestDTO;

import java.util.List;
import java.util.UUID;

public interface IOfferService {
    OfferResponseDTO create(OfferRequestDTO dto);
    OfferResponseDTO addProductToOffer(UUID offerExternalId, OfferProductRequestDTO dto);
    OfferResponseDTO removeProductFromOffer(UUID offerExternalId, UUID wineVariantExternalId);
    OfferResponseDTO getByExternalId(UUID externalId);
    List<OfferResponseDTO> getAllActive();
    List<OfferResponseDTO> getAllInactive();
    void activate(UUID externalId);
    void deactivate(UUID externalId);
}
