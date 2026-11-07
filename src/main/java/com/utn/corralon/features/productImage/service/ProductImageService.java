package com.utn.corralon.features.productImage.service;

import com.utn.corralon.exception.BadRequestException;
import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.exception.FileStorageException;
import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.features.product.entity.ProductEntity;
import com.utn.corralon.features.product.repository.ProductRepository;
import com.utn.corralon.features.productImage.dto.ProductImageResponseDTO;
import com.utn.corralon.features.productImage.entity.ProductImageEntity;
import com.utn.corralon.features.productImage.mapper.ProductImageMapper;
import com.utn.corralon.features.productImage.repository.ProductImageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImageService implements IProductImageService {

    public static final int MAX_IMAGES_PER_PRODUCT = 5;
    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final String PRODUCTS_FOLDER = "products";

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg",
            "jpeg",
            "png",
            "webp"
    );

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductImageMapper productImageMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public List<ProductImageResponseDTO> getByProduct(UUID productExternalId) {
        ensureProductExists(productExternalId);

        return productImageRepository
                .findByProductExternalIdOrderByPrincipalDescIdAsc(productExternalId)
                .stream()
                .map(productImageMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<ProductImageResponseDTO> upload(UUID productExternalId, List<MultipartFile> files) {
        ProductEntity product = productRepository.findByExternalId(productExternalId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found. ID: ", productExternalId));

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least one image file is required.");
        }

        long currentCount = productImageRepository.countByProductExternalId(productExternalId);
        if (currentCount + files.size() > MAX_IMAGES_PER_PRODUCT) {
            throw new BadRequestException("A product can have at most " + MAX_IMAGES_PER_PRODUCT + " images.");
        }

        Path productsDirectory = productsDirectory();
        List<Path> storedFiles = new ArrayList<>();
        List<ProductImageEntity> newImages = new ArrayList<>();
        boolean hasPrincipal = productImageRepository
                .findFirstByProductExternalIdAndPrincipalTrue(productExternalId)
                .isPresent();

        try {
            Files.createDirectories(productsDirectory);

            for (MultipartFile file : files) {
                validateFile(file);

                String extension = detectImageExtension(file);
                String fileName = createUniqueFileName(productsDirectory, extension);
                Path target = productsDirectory.resolve(fileName).normalize();

                if (!target.startsWith(productsDirectory)) {
                    throw new BadRequestException("Invalid image file name.");
                }

                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
                }
                storedFiles.add(target);

                newImages.add(ProductImageEntity.builder()
                        .imagePath(PRODUCTS_FOLDER + "/" + fileName)
                        .principal(!hasPrincipal && newImages.isEmpty())
                        .product(product)
                        .build());
            }

            List<ProductImageEntity> savedImages = productImageRepository.saveAll(newImages);

            return savedImages.stream()
                    .map(productImageMapper::toResponse)
                    .toList();
        } catch (BadRequestException | BusinessRuleException ex) {
            deleteFilesQuietly(storedFiles);
            throw ex;
        } catch (IOException ex) {
            deleteFilesQuietly(storedFiles);
            throw new FileStorageException("Could not store the image files.", ex);
        } catch (RuntimeException ex) {
            deleteFilesQuietly(storedFiles);
            throw ex;
        }
    }

    @Override
    @Transactional
    public void delete(UUID productExternalId, UUID imageExternalId) {
        ensureProductExists(productExternalId);

        ProductImageEntity image = productImageRepository
                .findByExternalIdAndProductExternalId(imageExternalId, productExternalId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found. ID: ", imageExternalId));

        long imageCount = productImageRepository.countByProductExternalId(productExternalId);
        if (imageCount <= 1) {
            throw new BusinessRuleException("A product must have at least one image.");
        }

        boolean wasPrincipal = image.isPrincipal();
        productImageRepository.delete(image);

        if (wasPrincipal) {
            productImageRepository.findFirstByProductExternalIdOrderByIdAsc(productExternalId)
                    .filter(candidate -> !candidate.getExternalId().equals(imageExternalId))
                    .ifPresent(candidate -> {
                        candidate.setPrincipal(true);
                        productImageRepository.save(candidate);
                    });
        }

        deleteStoredFile(image.getImagePath());
    }

    @Override
    @Transactional
    public ProductImageResponseDTO setPrincipal(UUID productExternalId, UUID imageExternalId) {
        ensureProductExists(productExternalId);

        ProductImageEntity selected = productImageRepository
                .findByExternalIdAndProductExternalId(imageExternalId, productExternalId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found. ID: ", imageExternalId));

        if (!selected.isPrincipal()) {
            productImageRepository
                    .findByProductExternalIdOrderByPrincipalDescIdAsc(productExternalId)
                    .forEach(image -> {
                        if (image.isPrincipal()) {
                            image.setPrincipal(false);
                            productImageRepository.save(image);
                        }
                    });

            selected.setPrincipal(true);
            productImageRepository.save(selected);
        }

        return productImageMapper.toResponse(selected);
    }

    private void ensureProductExists(UUID productExternalId) {
        if (!productRepository.existsByExternalId(productExternalId)) {
            throw new ResourceNotFoundException("Product not found. ID: ", productExternalId);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("Each image must be smaller than 5 MB.");
        }

        String contentType = file.getContentType();
        if (contentType != null
                && !contentType.isBlank()
                && !"application/octet-stream".equalsIgnoreCase(contentType)
                && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Only JPG, JPEG, PNG and WEBP images are allowed.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !hasAllowedExtension(originalName)) {
            throw new BadRequestException("Only JPG, JPEG, PNG and WEBP images are allowed.");
        }
    }

    private boolean hasAllowedExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return false;
        }

        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    private String detectImageExtension(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(12);

            if (isJpeg(header)) {
                return "jpg";
            }

            if (isPng(header)) {
                return "png";
            }

            if (isWebp(header)) {
                return "webp";
            }
        } catch (IOException ex) {
            throw new FileStorageException("Could not read the image file.", ex);
        }

        throw new BadRequestException("The file content is not a valid JPG, JPEG, PNG or WEBP image.");
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        return header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 'P'
                && header[2] == 'N'
                && header[3] == 'G'
                && (header[4] & 0xFF) == 0x0D
                && (header[5] & 0xFF) == 0x0A
                && (header[6] & 0xFF) == 0x1A
                && (header[7] & 0xFF) == 0x0A;
    }

    private boolean isWebp(byte[] header) {
        return header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P';
    }

    private String createUniqueFileName(Path productsDirectory, String extension) {
        String fileName;

        do {
            fileName = UUID.randomUUID() + "." + extension;
        } while (Files.exists(productsDirectory.resolve(fileName)));

        return fileName;
    }

    private Path uploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private Path productsDirectory() {
        return uploadRoot().resolve(PRODUCTS_FOLDER).normalize();
    }

    private void deleteStoredFile(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }

        Path root = uploadRoot();
        Path file = root.resolve(imagePath).normalize();

        if (!file.startsWith(root)) {
            log.warn("Skipping deletion of suspicious image path: {}", imagePath);
            return;
        }

        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            log.warn("Could not delete image file {}", file, ex);
        }
    }

    private void deleteFilesQuietly(List<Path> files) {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ex) {
                log.warn("Could not delete image file {}", file, ex);
            }
        }
    }
}
