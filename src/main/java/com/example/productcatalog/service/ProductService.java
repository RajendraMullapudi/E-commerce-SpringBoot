package com.example.productcatalog.service;

import com.example.productcatalog.dto.ProductRequest;
import com.example.productcatalog.dto.ProductResponse;
import com.example.productcatalog.entity.Product;
import com.example.productcatalog.exception.ResourceNotFoundException;
import com.example.productcatalog.repository.ProductRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public ProductResponse createProduct(ProductRequest productRequest) {
        // Check for unique SKU before creating
        if (productRepository.findBySku(productRequest.getSku()).isPresent()) {
            throw new IllegalArgumentException("Product with SKU '" + productRequest.getSku() + "' already exists.");
        }
        Product product = new Product();
        BeanUtils.copyProperties(productRequest, product);
        Product savedProduct = productRepository.save(product);
        return convertToDto(savedProduct);
    }

    public Page<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir, String searchName) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> products;
        if (searchName != null && !searchName.trim().isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCase(searchName, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }
        return products.map(this::convertToDto);
    }


    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return convertToDto(product);
    }

    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Check for SKU uniqueness if it's being updated to a different existing SKU
        if (!existingProduct.getSku().equals(productRequest.getSku())) {
            if (productRepository.findBySku(productRequest.getSku()).isPresent()) {
                throw new IllegalArgumentException("Product with SKU '" + productRequest.getSku() + "' already exists.");
            }
        }

        BeanUtils.copyProperties(productRequest, existingProduct);
        Product updatedProduct = productRepository.save(existingProduct);
        return convertToDto(updatedProduct);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    // Helper method to convert Entity to DTO
    private ProductResponse convertToDto(Product product) {
        ProductResponse dto = new ProductResponse();
        BeanUtils.copyProperties(product, dto);
        return dto;
    }

    // Helper method to convert DTO to Entity (useful for updates as well)
    private Product convertToEntity(ProductRequest dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        return product;
    }
}