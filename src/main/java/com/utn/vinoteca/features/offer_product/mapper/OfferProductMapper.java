package com.utn.vinoteca.features.offer_product.mapper;

import com.utn.vinoteca.features.offer.entity.OfferEntity;
import com.utn.vinoteca.features.offer_product.dto.OfferProductRequestDTO;
import com.utn.vinoteca.features.offer_product.dto.OfferProductResponseDTO;
import com.utn.vinoteca.features.offer_product.entity.OfferProductEntity;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class OfferProductMapper {
    private final ModelMapper modelMapper;

    public OfferProductMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public OfferProductResponseDTO toResponse(OfferProductEntity entity) {
        return new OfferProductResponseDTO(
                entity.getOffer().getExternalId(),
                entity.getWineVariant().getExternalId(),
                entity.getDiscountedPrice()
        );
    }

    public OfferProductEntity toEntity(OfferProductRequestDTO dto, OfferEntity offer, WineVariantEntity variant) {
        OfferProductEntity entity = modelMapper.map(dto, OfferProductEntity.class);
        entity.setOffer(offer);
        entity.setWineVariant(variant);
        return entity;
    }
}
