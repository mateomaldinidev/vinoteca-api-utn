package com.utn.vinoteca.features.cartItem.mapper;

import com.utn.vinoteca.features.cartItem.dto.CartItemRequestDTO;
import com.utn.vinoteca.features.cartItem.dto.CartItemResponseDTO;
import com.utn.vinoteca.features.cartItem.entity.CartItemEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class CartItemMapper {
    private final ModelMapper modelMapper;

    public CartItemMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public CartItemResponseDTO toResponse(CartItemEntity entity) {
        CartItemResponseDTO dto = modelMapper.map(entity, CartItemResponseDTO.class);
        dto.setWineName(entity.getWineVariant().getWine().getName());
        dto.setVariantAttribute(entity.getWineVariant().getAttribute());
        dto.setUnitPrice(entity.getWineVariant().getPrice());
        return dto;
    }

    public CartItemEntity toEntity(CartItemRequestDTO dto) {
        return modelMapper.map(dto, CartItemEntity.class);
    }

    public void updateEntity(CartItemEntity entity, CartItemRequestDTO dto) {
        entity.setQuantity(dto.getQuantity());
    }
}
