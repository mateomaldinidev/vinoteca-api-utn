package com.utn.corralon.features.product.dto;

import com.utn.corralon.features.productImage.dto.ProductImageResponseDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.SecondaryRow;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponseDTO{

    private UUID externalId;
    private String name;
    private String description;
    private Boolean active;

    private UUID supplierId;
    private String supplierName;

    private UUID categoryId;
    private String categoryName;

    private UUID brandId;
    private String brandName;

    private String principalImagePath;
    private List<ProductImageResponseDTO> images;

}


