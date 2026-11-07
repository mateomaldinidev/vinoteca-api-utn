package com.utn.corralon.features.productImage.service;

import com.utn.corralon.features.productImage.dto.ProductImageResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface IProductImageService {

    List<ProductImageResponseDTO> getByProduct(UUID productExternalId);

    List<ProductImageResponseDTO> upload(UUID productExternalId, List<MultipartFile> files);

    void delete(UUID productExternalId, UUID imageExternalId);

    ProductImageResponseDTO setPrincipal(UUID productExternalId, UUID imageExternalId);
}
