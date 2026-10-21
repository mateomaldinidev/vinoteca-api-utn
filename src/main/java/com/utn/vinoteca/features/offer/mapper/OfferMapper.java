package com.utn.vinoteca.features.offer.mapper;

import com.utn.vinoteca.features.offer.dto.OfferRequestDTO;
import com.utn.vinoteca.features.offer.dto.OfferResponseDTO;
import com.utn.vinoteca.features.offer.entity.OfferEntity;
import com.utn.vinoteca.features.offerProduct.dto.OfferProductResponseDTO;
import com.utn.vinoteca.features.offerProduct.mapper.OfferProductMapper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OfferMapper {
    private final ModelMapper modelMapper;
    private final OfferProductMapper offerProductMapper;

    public OfferMapper(ModelMapper modelMapper, OfferProductMapper offerProductMapper) {
        this.modelMapper = modelMapper;
        this.offerProductMapper = offerProductMapper;
    }

    public OfferResponseDTO toResponse(OfferEntity offer) {
        List<OfferProductResponseDTO> offerProductsDTO = offer.getOfferProducts() != null
                ? offer.getOfferProducts().stream()
                        .map(offerProductMapper::toResponse)
                        .toList()
                : List.of();

        return new OfferResponseDTO(
                offer.getExternalId(),
                offer.getName(),
                offer.getDiscountPercentage(),
                offer.getStartDate(),
                offer.getEndDate(),
                offer.getActive(),
                offerProductsDTO
        );
    }

    public OfferEntity toEntity(OfferRequestDTO dto) {
        return modelMapper.map(dto, OfferEntity.class);
    }

    public void updateEntity(OfferEntity entity, OfferRequestDTO dto) {
        entity.setName(dto.getName());
        entity.setDiscountPercentage(dto.getDiscountPercentage());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
    }
}
