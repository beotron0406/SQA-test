package com.electro.controller.client;

import com.electro.dto.client.ClientListedProductResponse;
import com.electro.dto.client.ClientProductResponse;
import com.electro.entity.product.Category;
import com.electro.entity.product.Product;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.client.ClientProductMapper;
import com.electro.projection.inventory.SimpleProductInventory;
import com.electro.repository.ProjectionRepository;
import com.electro.repository.product.ProductRepository;
import com.electro.repository.review.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ClientProductDetailControllerTests {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProjectionRepository projectionRepository;

    @Mock
    private ClientProductMapper clientProductMapper;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ClientProductController clientProductController;

    private Product testProduct1;
    private Product testProduct2;
    private List<SimpleProductInventory> productInventories;
    private SimpleProductInventory simpleProductInventory;
    private ClientProductResponse clientProductResponse;
    private List<ClientListedProductResponse> relatedProductResponses;

    @BeforeEach
    void setUp() {
        // Setup test products
        testProduct1 = mock(Product.class);
        when(testProduct1.getId()).thenReturn(1L);
        when(testProduct1.getName()).thenReturn("iPhone 14");
        when(testProduct1.getSlug()).thenReturn("iphone-14");

        Category category = mock(Category.class);
        when(category.getId()).thenReturn(1L);
        when(category.getName()).thenReturn("Smartphones");
        when(testProduct1.getCategory()).thenReturn(category);

        // Setup product 2 (for related products)
        testProduct2 = mock(Product.class);
        when(testProduct2.getId()).thenReturn(2L);
        when(testProduct2.getName()).thenReturn("Samsung Galaxy S23");
        when(testProduct2.getSlug()).thenReturn("samsung-galaxy-s23");
        when(testProduct2.getCategory()).thenReturn(category);

        // Setup inventory
        simpleProductInventory = mock(SimpleProductInventory.class);
        when(simpleProductInventory.getProductId()).thenReturn(1L);
        when(simpleProductInventory.getInventory()).thenReturn(50);
        when(simpleProductInventory.getCanBeSold()).thenReturn(45);

        productInventories = Collections.singletonList(simpleProductInventory);

        // Setup client product response
        clientProductResponse = mock(ClientProductResponse.class);

        // Setup related products response
        ClientListedProductResponse relatedProductResponse = mock(ClientListedProductResponse.class);
        relatedProductResponses = Collections.singletonList(relatedProductResponse);
    }

    // TC-PROD-DETAIL-01: Lấy chi tiết sản phẩm theo slug hợp lệ
    @Test
    @DisplayName("Should return product details when valid slug provided")
    void getProduct_ShouldReturnProductDetails_WhenValidSlugProvided() {
        // Arrange
        String slug = "iphone-14";

        when(productRepository.findBySlug(slug)).thenReturn(Optional.of(testProduct1));
        when(projectionRepository.findSimpleProductInventories(eq(List.of(1L)))).thenReturn(productInventories);
        when(projectionRepository.findSimpleProductInventories(argThat(list -> !list.equals(List.of(1L)))))
                .thenReturn(Collections.emptyList());
        when(reviewRepository.findAverageRatingScoreByProductId(1L)).thenReturn(4);
        when(reviewRepository.countByProductId(1L)).thenReturn(10);

        // For related products
        Page<Product> relatedProducts = new PageImpl<>(Collections.singletonList(testProduct2));
        when(productRepository.findByParams(anyString(), eq("random"), isNull(), eq(false), eq(false),
                any(Pageable.class)))
                .thenReturn(relatedProducts);

        when(clientProductMapper.entityToResponse(any(Product.class), anyList(), anyInt(), anyInt(), anyList()))
                .thenReturn(clientProductResponse);

        // Act
        ResponseEntity<ClientProductResponse> response = clientProductController.getProduct(slug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertSame(clientProductResponse, response.getBody());

        verify(productRepository).findBySlug(slug);
        // Verify được gọi đúng 2 lần với các tham số khác nhau
        verify(projectionRepository).findSimpleProductInventories(eq(List.of(1L)));
        verify(projectionRepository).findSimpleProductInventories(argThat(list -> !list.equals(List.of(1L))));
        verify(reviewRepository).findAverageRatingScoreByProductId(1L);
        verify(reviewRepository).countByProductId(1L);
        verify(clientProductMapper).entityToResponse(
                eq(testProduct1),
                anyList(),
                anyInt(),
                anyInt(),
                anyList());
    }

    // TC-PROD-DETAIL-02: Xử lý khi slug không tồn tại
    @Test
    @DisplayName("Should throw ResourceNotFoundException when invalid slug provided")
    void getProduct_ShouldThrowResourceNotFoundException_WhenInvalidSlugProvided() {
        // Arrange
        String slug = "non-existent-product";

        when(productRepository.findBySlug(slug)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> clientProductController.getProduct(slug));

        verify(productRepository).findBySlug(slug);
        verify(projectionRepository, never()).findSimpleProductInventories(anyList());
        verify(reviewRepository, never()).findAverageRatingScoreByProductId(anyLong());
        verify(reviewRepository, never()).countByProductId(anyLong());
        verify(clientProductMapper, never()).entityToResponse(any(), anyList(), anyInt(), anyInt(), anyList());
    }

    // TC-PROD-DETAIL-03: Kiểm tra thông tin đánh giá sản phẩm
    @Test
    @DisplayName("Should return product with correct rating information")
    void getProduct_ShouldReturnProductWithCorrectRatingInformation() {
        // Arrange
        String slug = "iphone-14";
        int averageRating = 4;
        int reviewCount = 10;

        when(productRepository.findBySlug(slug)).thenReturn(Optional.of(testProduct1));
        when(projectionRepository.findSimpleProductInventories(eq(List.of(1L)))).thenReturn(productInventories);
        when(projectionRepository.findSimpleProductInventories(argThat(list -> !list.equals(List.of(1L)))))
                .thenReturn(Collections.emptyList());
        when(reviewRepository.findAverageRatingScoreByProductId(1L)).thenReturn(averageRating);
        when(reviewRepository.countByProductId(1L)).thenReturn(reviewCount);

        // For related products
        Page<Product> relatedProducts = new PageImpl<>(Collections.singletonList(testProduct2));
        when(productRepository.findByParams(anyString(), eq("random"), isNull(), eq(false), eq(false),
                any(Pageable.class)))
                .thenReturn(relatedProducts);

        when(clientProductMapper.entityToResponse(any(Product.class), anyList(), anyInt(), anyInt(), anyList()))
                .thenReturn(clientProductResponse);

        // Act
        ResponseEntity<ClientProductResponse> response = clientProductController.getProduct(slug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify rating information is passed to the mapper
        verify(projectionRepository).findSimpleProductInventories(eq(List.of(1L)));
        verify(projectionRepository).findSimpleProductInventories(argThat(list -> !list.equals(List.of(1L))));
        verify(clientProductMapper).entityToResponse(
                any(Product.class),
                anyList(),
                eq(averageRating),
                eq(reviewCount),
                anyList());
    }

    // TC-PROD-DETAIL-04: Kiểm tra sản phẩm liên quan
    @Test
    @DisplayName("Should return product with related products")
    void getProduct_ShouldReturnProductWithRelatedProducts() {
        // Arrange
        String slug = "samsung-galaxy-s23";

        when(productRepository.findBySlug(slug)).thenReturn(Optional.of(testProduct2));
        when(projectionRepository.findSimpleProductInventories(eq(List.of(2L)))).thenReturn(productInventories);
        when(projectionRepository.findSimpleProductInventories(argThat(list -> !list.equals(List.of(2L)))))
                .thenReturn(Collections.emptyList());
        when(reviewRepository.findAverageRatingScoreByProductId(anyLong())).thenReturn(4);
        when(reviewRepository.countByProductId(anyLong())).thenReturn(10);

        // Setup related products
        List<Product> relatedProductList = Collections.singletonList(testProduct1);
        Page<Product> relatedProducts = new PageImpl<>(relatedProductList);

        when(productRepository.findByParams(anyString(), eq("random"), isNull(), eq(false), eq(false),
                any(Pageable.class)))
                .thenReturn(relatedProducts);

        when(clientProductMapper.entityToListedResponse(any(Product.class), anyList()))
                .thenReturn(mock(ClientListedProductResponse.class));

        when(clientProductMapper.entityToResponse(any(Product.class), anyList(), anyInt(), anyInt(), anyList()))
                .thenReturn(clientProductResponse);

        // Act
        ResponseEntity<ClientProductResponse> response = clientProductController.getProduct(slug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify related products are retrieved and mapped
        verify(projectionRepository).findSimpleProductInventories(eq(List.of(2L)));
        verify(projectionRepository).findSimpleProductInventories(argThat(list -> !list.equals(List.of(2L))));
        verify(productRepository).findByParams(anyString(), eq("random"), isNull(), eq(false), eq(false),
                any(Pageable.class));
        verify(clientProductMapper).entityToListedResponse(any(Product.class), anyList());
    }

    // TC-PROD-DETAIL-05: Kiểm tra thông tin tồn kho
    @Test
    @DisplayName("Should return product with inventory information")
    void getProduct_ShouldReturnProductWithInventoryInformation() {
        // Arrange
        String slug = "iphone-14";

        when(productRepository.findBySlug(slug)).thenReturn(Optional.of(testProduct1));
        when(projectionRepository.findSimpleProductInventories(eq(List.of(1L)))).thenReturn(productInventories);
        when(reviewRepository.findAverageRatingScoreByProductId(anyLong())).thenReturn(4);
        when(reviewRepository.countByProductId(anyLong())).thenReturn(10);

        // For related products
        Page<Product> emptyRelatedProducts = new PageImpl<>(Collections.emptyList());
        when(productRepository.findByParams(anyString(), eq("random"), isNull(), eq(false), eq(false),
                any(Pageable.class)))
                .thenReturn(emptyRelatedProducts);

        when(projectionRepository.findSimpleProductInventories(argThat(list -> !list.equals(List.of(1L)))))
                .thenReturn(Collections.emptyList());

        when(clientProductMapper.entityToResponse(any(Product.class), anyList(), anyInt(), anyInt(), anyList()))
                .thenReturn(clientProductResponse);

        // Act
        ResponseEntity<ClientProductResponse> response = clientProductController.getProduct(slug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify inventory information is retrieved and passed to the mapper
        verify(projectionRepository).findSimpleProductInventories(eq(List.of(1L)));
        verify(projectionRepository).findSimpleProductInventories(argThat(list -> !list.equals(List.of(1L))));
        verify(clientProductMapper).entityToResponse(
                any(Product.class),
                eq(productInventories),
                anyInt(),
                anyInt(),
                anyList());
    }

    // TC-PROD-DETAIL-06: Xử lý slug có ký tự đặc biệt
    @Test
    @DisplayName("Should handle product slug with special characters")
    void getProduct_ShouldHandleProductSlugWithSpecialCharacters() {
        // Arrange
        String specialSlug = "gaming-&-accessories";

        Product specialProduct = mock(Product.class);
        when(specialProduct.getId()).thenReturn(3L);
        when(specialProduct.getName()).thenReturn("Gaming & Accessories");
        when(specialProduct.getSlug()).thenReturn(specialSlug);

        when(productRepository.findBySlug(specialSlug)).thenReturn(Optional.of(specialProduct));
        when(projectionRepository.findSimpleProductInventories(eq(List.of(3L)))).thenReturn(productInventories);
        when(projectionRepository.findSimpleProductInventories(argThat(list -> !list.equals(List.of(3L)))))
                .thenReturn(Collections.emptyList());
        when(reviewRepository.findAverageRatingScoreByProductId(anyLong())).thenReturn(4);
        when(reviewRepository.countByProductId(anyLong())).thenReturn(10);

        // For related products
        Page<Product> relatedProducts = new PageImpl<>(Collections.emptyList());
        when(productRepository.findByParams(anyString(), eq("random"), isNull(), eq(false), eq(false),
                any(Pageable.class)))
                .thenReturn(relatedProducts);

        when(clientProductMapper.entityToResponse(any(Product.class), anyList(), anyInt(), anyInt(), anyList()))
                .thenReturn(clientProductResponse);

        // Act
        ResponseEntity<ClientProductResponse> response = clientProductController.getProduct(specialSlug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(productRepository).findBySlug(specialSlug);
        verify(projectionRepository).findSimpleProductInventories(eq(List.of(3L)));
        verify(projectionRepository).findSimpleProductInventories(argThat(list -> !list.equals(List.of(3L))));
    }

    // TC-PROD-DETAIL-07: Sản phẩm không có sản phẩm liên quan
    @Test
    @DisplayName("Should handle product with no related products")
    void getProduct_ShouldHandleProductWithNoRelatedProducts() {
        // Arrange
        String slug = "unique-product";

        Product uniqueProduct = mock(Product.class);
        when(uniqueProduct.getId()).thenReturn(4L);
        when(uniqueProduct.getName()).thenReturn("Unique Product");
        when(uniqueProduct.getSlug()).thenReturn(slug);

        when(productRepository.findBySlug(slug)).thenReturn(Optional.of(uniqueProduct));
        when(projectionRepository.findSimpleProductInventories(eq(List.of(4L)))).thenReturn(productInventories);
        when(projectionRepository.findSimpleProductInventories(argThat(list -> !list.equals(List.of(4L)))))
                .thenReturn(Collections.emptyList());
        when(reviewRepository.findAverageRatingScoreByProductId(anyLong())).thenReturn(5);
        when(reviewRepository.countByProductId(anyLong())).thenReturn(2);

        // Empty related products
        Page<Product> emptyRelatedProducts = new PageImpl<>(Collections.emptyList());
        when(productRepository.findByParams(anyString(), eq("random"), isNull(), eq(false), eq(false),
                any(Pageable.class)))
                .thenReturn(emptyRelatedProducts);

        when(clientProductMapper.entityToResponse(any(Product.class), anyList(), anyInt(), anyInt(),
                eq(Collections.emptyList())))
                .thenReturn(clientProductResponse);

        // Act
        ResponseEntity<ClientProductResponse> response = clientProductController.getProduct(slug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify empty related products list is passed to the mapper
        verify(projectionRepository).findSimpleProductInventories(eq(List.of(4L)));
        verify(projectionRepository).findSimpleProductInventories(argThat(list -> !list.equals(List.of(4L))));
        verify(clientProductMapper).entityToResponse(
                any(Product.class),
                anyList(),
                anyInt(),
                anyInt(),
                eq(Collections.emptyList()));
    }

    // TC-PROD-DETAIL-08: Sản phẩm có nhiều ảnh
    @Test
    @DisplayName("Should return product with multiple images")
    void getProduct_ShouldReturnProductWithMultipleImages() {
        // Arrange
        String slug = "macbook-pro";

        Product productWithImages = mock(Product.class);
        when(productWithImages.getId()).thenReturn(5L);
        when(productWithImages.getName()).thenReturn("MacBook Pro");
        when(productWithImages.getSlug()).thenReturn(slug);

        // Set up multiple images for the product using mocks
        // Since we don't have direct access to the images in our test, we'll verify
        // through the mapper

        when(productRepository.findBySlug(slug)).thenReturn(Optional.of(productWithImages));
        when(projectionRepository.findSimpleProductInventories(eq(List.of(5L)))).thenReturn(productInventories);
        when(projectionRepository.findSimpleProductInventories(argThat(list -> !list.equals(List.of(5L)))))
                .thenReturn(Collections.emptyList());
        when(reviewRepository.findAverageRatingScoreByProductId(anyLong())).thenReturn(5);
        when(reviewRepository.countByProductId(anyLong())).thenReturn(20);

        // For related products
        Page<Product> relatedProducts = new PageImpl<>(Collections.emptyList());
        when(productRepository.findByParams(anyString(), eq("random"), isNull(), eq(false), eq(false),
                any(Pageable.class)))
                .thenReturn(relatedProducts);

        when(clientProductMapper.entityToResponse(eq(productWithImages), anyList(), anyInt(), anyInt(), anyList()))
                .thenReturn(clientProductResponse);

        // Act
        ResponseEntity<ClientProductResponse> response = clientProductController.getProduct(slug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(productRepository).findBySlug(slug);
        verify(projectionRepository).findSimpleProductInventories(eq(List.of(5L)));
        verify(projectionRepository).findSimpleProductInventories(argThat(list -> !list.equals(List.of(5L))));
        verify(clientProductMapper).entityToResponse(
                eq(productWithImages),
                anyList(),
                anyInt(),
                anyInt(),
                anyList());
    }
}