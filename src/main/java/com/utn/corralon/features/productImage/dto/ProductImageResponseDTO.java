package com.utn.corralon.features.productImage.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductImageResponseDTO {

    private UUID externalId;
    private String imagePath;
    private String url;
    private boolean principal;
}
