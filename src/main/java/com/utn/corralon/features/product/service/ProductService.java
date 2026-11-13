package com.utn.corralon.features.product.service;

import com.utn.corralon.exception.BusinessRuleException;
import com.utn.corralon.exception.ResourceNotFoundException;
import com.utn.corralon.features.brand.entity.BrandEntity;
import com.utn.corralon.features.brand.repository.BrandRepository;
import com.utn.corralon.features.category.entity.CategoryEntity;
import com.utn.corralon.features.category.repository.CategoryRepository;
import com.utn.corralon.features.product.dto.ProductDeleteResponseDTO;
import com.utn.corralon.features.product.dto.ProductRequestDTO;
import com.utn.corralon.features.product.dto.ProductResponseDTO;
import com.utn.corralon.features.product.entity.ProductEntity;
import com.utn.corralon.features.product.mapper.ProductMapper;
import com.utn.corralon.features.product.repository.ProductRepository;
import com.utn.corralon.features.product.specification.ProductSpecification;
import com.utn.corralon.features.productVariant.dto.DisabledVariantDTO;
import com.utn.corralon.features.productVariant.repository.ProductVariantRepository;
import com.utn.corralon.features.supplier.entity.SupplierEntity;
import com.utn.corralon.features.supplier.repository.SupplierRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ProductService implements IProductService{
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductVariantRepository productVariantRepository;

    private final ProductMapper productMapper;

    //CREATE
    @Transactional
    @Override
    public ProductResponseDTO create(ProductRequestDTO productRequestDTO) {
        SupplierEntity supplier = supplierRepository.findByExternalId(productRequestDTO.getSupplierId())
                        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: ", productRequestDTO.getSupplierId()));
        if(!supplier.isActive()){
            throw new BusinessRuleException("Cannot assign inactive supplier");
        }

        CategoryEntity category = categoryRepository.findByExternalId(productRequestDTO.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: ", productRequestDTO.getCategoryId()));
        if(!category.getActive()){
            throw new BusinessRuleException("Cannot assign inactive category");
        }
        BrandEntity brand =
                brandRepository.findByExternalId(productRequestDTO.getBrandId())
                        .orElseThrow(() -> new ResourceNotFoundException("Brand not found. ID: ", productRequestDTO.getBrandId()));

        if(!brand.getActive()){
            throw new BusinessRuleException("Cannot assign inactive brand");
        }


        if(productRepository.existsByNameAndBrand(productRequestDTO.getName(), brand)){
            throw new BusinessRuleException("Product already exists for this brand.");
        }

        ProductEntity product = productMapper.toEntity(productRequestDTO, supplier, category, brand);
        product.setActive(true);

        productRepository.save(product);

        return productMapper.toResponse(product);
    }

    //UPDATE
    @Transactional
    @Override
    public ProductResponseDTO update(UUID externalId, ProductRequestDTO productRequestDTO) {
        ProductEntity product = productRepository
                .findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: ", externalId));

        SupplierEntity supplier = supplierRepository.findByExternalId(productRequestDTO.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: ", productRequestDTO.getSupplierId()));

        if (!supplier.isActive()) {
            throw new BusinessRuleException("Cannot assign inactive supplier");
        }

        CategoryEntity category = categoryRepository.findByExternalId(productRequestDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: ", productRequestDTO.getCategoryId()));

        if (!category.getActive()) {
            throw new BusinessRuleException("Cannot assign inactive category");
        }

        BrandEntity brand = brandRepository.findByExternalId(productRequestDTO.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found. ID: ", productRequestDTO.getBrandId()));

        if (!brand.getActive()) {
            throw new BusinessRuleException("Cannot assign inactive brand");
        }

        ProductEntity duplicated = productRepository.findByNameAndBrand(productRequestDTO.getName(), brand)
                .orElse(null);

        if (duplicated != null && !duplicated.getExternalId().equals(product.getExternalId()))
        {
            throw new BusinessRuleException("Product already exists for this brand.");
        }

        productMapper.updateEntity(product, productRequestDTO, supplier, category, brand);

        productRepository.save(product);

        return productMapper.toResponse(product);
    }

    //DELETE logico = active = false
    @Transactional
    @Override
    public ProductDeleteResponseDTO delete(UUID externalId) {
        ProductEntity product = productRepository.findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found. ID: ", externalId));

        List<DisabledVariantDTO> disabledVariants = product.getProductVariants()
                        .stream()
                        .map(variant -> {

                            variant.setActive(false);
                            productVariantRepository.save(variant);

                            DisabledVariantDTO dto = new DisabledVariantDTO();

                            dto.setExternalId(variant.getExternalId());
                            dto.setAttribute(variant.getAttribute());

                            return dto;
                        })
                        .toList();

        product.setActive(false);
        productRepository.save(product);

        ProductDeleteResponseDTO responseDTO = new ProductDeleteResponseDTO();

        responseDTO.setProductExternalId(product.getExternalId());
        responseDTO.setProductName(product.getName());
        responseDTO.setActive(false);
        responseDTO.setDisabledVariants(disabledVariants);

        return responseDTO;
    }

    //ACTIVATE despues del delete
    @Transactional
    @Override
    public void activate(UUID externalId) {
        ProductEntity product = productRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found. ID: ", externalId));

        if (product.isActive()) {
            throw new BusinessRuleException("Product is already active.");
        }

        if (!product.getSupplier().isActive()) {
            throw new BusinessRuleException("Cannot activate product with inactive supplier.");
        }

        if (!product.getCategory().getActive()) {
            throw new BusinessRuleException("Cannot activate product with inactive category.");
        }

        if (!product.getBrand().getActive()) {
            throw new BusinessRuleException("Cannot activate product with inactive brand.");
        }

        product.setActive(true);
        productRepository.save(product);
    }

    //SEARCH WITH FILTERS
    @Transactional
    @Override
    public List<ProductResponseDTO> search(String name, UUID supplierId, UUID categoryId, UUID brandId){
        Specification<ProductEntity> specification = Specification
                .where(ProductSpecification.hasName(name))
                .and(ProductSpecification.isActive(true))
                .and(ProductSpecification.hasSupplier(supplierId))
                .and(ProductSpecification.hasCategory(categoryId))
                .and(ProductSpecification.hasBrand(brandId));

        return productRepository.findAll(specification)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    //LIST ALL INACTIVES
    @Transactional
    @Override
    public List<ProductResponseDTO> getInactive(String name, UUID supplierId, UUID categoryId, UUID brandId) {

        Specification<ProductEntity> specification = Specification
                .where(ProductSpecification.hasName(name))
                .and(ProductSpecification.isActive(false))
                .and(ProductSpecification.hasSupplier(supplierId))
                .and(ProductSpecification.hasCategory(categoryId))
                .and(ProductSpecification.hasBrand(brandId));

        return productRepository
                .findAll(specification)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    //GET BY ID
    @Transactional
    @Override
    public ProductResponseDTO getById(UUID externalId) {
        ProductEntity product = productRepository
                .findByExternalIdAndActiveTrue(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found. ID: ", externalId));
        return productMapper.toResponse(product);
    }

    //GET BY ID (INACTIVE, SOLO GESTION)
    @Transactional
    @Override
    public ProductResponseDTO getInactiveByExternalId(UUID externalId) {
        ProductEntity product = productRepository
                .findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found. ID: ", externalId));

        if (product.isActive()) {
            throw new BusinessRuleException("Product is active. Use the active product endpoint.");
        }

        return productMapper.toResponse(product);
    }

}