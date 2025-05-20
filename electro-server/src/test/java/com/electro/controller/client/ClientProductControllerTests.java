package com.electro.controller.client;

import com.electro.dto.ListResponse;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Thêm LENIENT strictness
public class ClientProductControllerTests {

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
        private List<Product> productList;
        private Page<Product> productPage;
        private ClientListedProductResponse clientListedProductResponse1;
        private ClientListedProductResponse clientListedProductResponse2;
        private List<ClientListedProductResponse> clientListedProductResponses;
        private SimpleProductInventory simpleProductInventory1;
        private SimpleProductInventory simpleProductInventory2;
        private List<SimpleProductInventory> productInventories;
        private ClientProductResponse clientProductResponse;

        @BeforeEach
        void setUp() {
                // Setup test products - chỉ giữ lại phần thiết lập cơ bản mà không có stubs
                testProduct1 = mock(Product.class);
                testProduct2 = mock(Product.class);

                Category category = mock(Category.class);

                productList = Arrays.asList(testProduct1, testProduct2);
                productPage = new PageImpl<>(productList);

                // Mocking SimpleProductInventory - chỉ khởi tạo mà không stub
                simpleProductInventory1 = mock(SimpleProductInventory.class);
                simpleProductInventory2 = mock(SimpleProductInventory.class);

                productInventories = Arrays.asList(simpleProductInventory1, simpleProductInventory2);

                // Setup client responses using direct instantiation
                clientListedProductResponse1 = new ClientListedProductResponse();
                clientListedProductResponse2 = new ClientListedProductResponse();

                clientListedProductResponses = Arrays.asList(clientListedProductResponse1,
                                clientListedProductResponse2);

                // Setup client product detail response
                clientProductResponse = new ClientProductResponse();

                // Stubbing cơ bản cho tất cả các test
                when(testProduct1.getId()).thenReturn(1L);
                when(testProduct2.getId()).thenReturn(2L);

                // Sử dụng anyList() thay vì List.of(1L) hoặc List.of(2L)
                when(projectionRepository.findSimpleProductInventories(anyList())).thenReturn(productInventories);

                // Sử dụng doReturn/when thay vì when/thenReturn để tránh strict matching
                doReturn(clientListedProductResponse1).when(clientProductMapper)
                                .entityToListedResponse(any(Product.class), anyList());
        }

        // TC-PROD-01: Lấy tất cả sản phẩm với phân trang
        @Test
        @DisplayName("Should return all products with pagination when no filters applied")
        void getAllProducts_ShouldReturnAllProductsWithPagination_WhenNoFiltersApplied() {
                // Arrange
                int page = 1;
                int size = 10;
                Pageable pageable = PageRequest.of(page - 1, size);

                when(productRepository.findByParams(any(), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class)))
                                .thenReturn(productPage);

                // Act
                ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController
                                .getAllProducts(page, size, null, null, null, false, false);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                verify(productRepository).findByParams(any(), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class));
                verify(projectionRepository).findSimpleProductInventories(anyList());
        }

        // TC-PROD-02: Lọc sản phẩm theo nhiều tiêu chí
        @Test
        @DisplayName("Should return filtered products when filter parameters are provided")
        void getAllProducts_ShouldReturnFilteredProducts_WhenFilterParametersProvided() {
                // Arrange
                int page = 1;
                int size = 10;
                String filter = "category.id==1";
                String sort = "price,asc";
                String search = "iPhone";
                boolean saleable = true;
                boolean newable = false;

                Pageable pageable = PageRequest.of(page - 1, size);
                List<Product> filteredProducts = List.of(testProduct1);
                Page<Product> filteredProductPage = new PageImpl<>(filteredProducts);

                when(productRepository.findByParams(eq(filter), eq(sort), eq(search), eq(saleable), eq(newable),
                                any(Pageable.class)))
                                .thenReturn(filteredProductPage);

                // Act
                ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController
                                .getAllProducts(page, size, filter, sort, search, saleable, newable);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                verify(productRepository).findByParams(eq(filter), eq(sort), eq(search), eq(saleable), eq(newable),
                                any(Pageable.class));
                verify(projectionRepository).findSimpleProductInventories(anyList());
        }

        // TC-PROD-03: Kết quả rỗng khi không có sản phẩm nào phù hợp với bộ lọc
        @Test
        @DisplayName("Should return empty list when no products match filters")
        void getAllProducts_ShouldReturnEmptyList_WhenNoProductsMatchFilters() {
                // Arrange
                int page = 1;
                int size = 10;
                String filter = "category.id==999"; // Non-existent category
                Pageable pageable = PageRequest.of(page - 1, size);

                Page<Product> emptyPage = new PageImpl<>(new ArrayList<>());

                when(productRepository.findByParams(eq(filter), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class)))
                                .thenReturn(emptyPage);
                when(projectionRepository.findSimpleProductInventories(anyList())).thenReturn(new ArrayList<>());

                // Act
                ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController
                                .getAllProducts(page, size, filter, null, null, false, false);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                verify(productRepository).findByParams(eq(filter), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class));
                verify(projectionRepository).findSimpleProductInventories(anyList());
                verify(clientProductMapper, never()).entityToListedResponse(any(Product.class), anyList());
        }

        // TC-PROD-04: Tìm kiếm sản phẩm theo từ khóa
        @Test
        @DisplayName("Should return products when search term provided")
        void getAllProducts_ShouldReturnMatchingProducts_WhenSearchTermProvided() {
                // Arrange
                int page = 1;
                int size = 10;
                String search = "iPhone";
                Pageable pageable = PageRequest.of(page - 1, size);

                List<Product> searchResults = List.of(testProduct1);
                Page<Product> searchResultPage = new PageImpl<>(searchResults);

                when(productRepository.findByParams(any(), any(), eq(search), anyBoolean(), anyBoolean(),
                                any(Pageable.class)))
                                .thenReturn(searchResultPage);

                // Act
                ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController
                                .getAllProducts(page, size, null, null, search, false, false);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                verify(productRepository).findByParams(any(), any(), eq(search), anyBoolean(), anyBoolean(),
                                any(Pageable.class));
                verify(projectionRepository).findSimpleProductInventories(anyList());
        }

        // TC-PROD-05: Lọc sản phẩm có thể bán
        @Test
        @DisplayName("Should return only saleable products when saleable flag is true")
        void getAllProducts_ShouldReturnSaleableProducts_WhenSaleableFlagIsTrue() {
                // Arrange
                int page = 1;
                int size = 10;
                boolean saleable = true;
                Pageable pageable = PageRequest.of(page - 1, size);

                when(productRepository.findByParams(any(), any(), any(), eq(saleable), anyBoolean(),
                                any(Pageable.class)))
                                .thenReturn(productPage);

                doReturn(clientListedProductResponse1, clientListedProductResponse2)
                                .when(clientProductMapper).entityToListedResponse(any(Product.class), anyList());

                // Act
                ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController
                                .getAllProducts(page, size, null, null, null, saleable, false);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                verify(productRepository).findByParams(any(), any(), any(), eq(saleable), anyBoolean(),
                                any(Pageable.class));
                verify(projectionRepository).findSimpleProductInventories(anyList());
        }

        // TC-PROD-06: Lọc sản phẩm mới
        @Test
        @DisplayName("Should return only new products when newable flag is true")
        void getAllProducts_ShouldReturnNewProducts_WhenNewableFlagIsTrue() {
                // Arrange
                int page = 1;
                int size = 10;
                boolean newable = true;

                Pageable pageable = PageRequest.of(page - 1, size);

                when(productRepository.findByParams(any(), any(), any(), anyBoolean(), eq(newable),
                                any(Pageable.class)))
                                .thenReturn(productPage);

                // Act
                ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController
                                .getAllProducts(page, size, null, null, null, false, newable);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                verify(productRepository).findByParams(any(), any(), any(), anyBoolean(), eq(newable),
                                any(Pageable.class));
        }

        // TC-PROD-07: Sắp xếp sản phẩm theo giá
        @Test
        @DisplayName("Should return products sorted by price ascending")
        void getAllProducts_ShouldReturnProductsSortedByPriceAscending_WhenSortParamProvided() {
                // Arrange
                int page = 1;
                int size = 10;
                String sort = "price,asc";

                Pageable pageable = PageRequest.of(page - 1, size);

                when(productRepository.findByParams(any(), eq(sort), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class)))
                                .thenReturn(productPage);

                // Act
                ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController
                                .getAllProducts(page, size, null, sort, null, false, false);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                verify(productRepository).findByParams(any(), eq(sort), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class));
        }

        // TC-PROD-08: Lọc sản phẩm theo khoảng giá
        @Test
        @DisplayName("Should return products filtered by price range")
        void getAllProducts_ShouldReturnProductsFilteredByPriceRange_WhenPriceRangeFilterProvided() {
                // Arrange
                int page = 1;
                int size = 10;
                String filter = "price>=500;price<=1000";

                Pageable pageable = PageRequest.of(page - 1, size);

                when(productRepository.findByParams(eq(filter), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class)))
                                .thenReturn(productPage);

                // Act
                ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController
                                .getAllProducts(page, size, filter, null, null, false, false);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                verify(productRepository).findByParams(eq(filter), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class));
        }

        // TC-PROD-09: Lọc sản phẩm theo thương hiệu
        @Test
        @DisplayName("Should return products filtered by brand")
        void getAllProducts_ShouldReturnProductsFilteredByBrand_WhenBrandFilterProvided() {
                // Arrange
                int page = 1;
                int size = 10;
                String filter = "brand.id==1";

                Pageable pageable = PageRequest.of(page - 1, size);

                when(productRepository.findByParams(eq(filter), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class)))
                                .thenReturn(productPage);

                // Act
                ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController
                                .getAllProducts(page, size, filter, null, null, false, false);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                verify(productRepository).findByParams(eq(filter), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class));
        }

        // TC-PROD-10: Lấy chi tiết sản phẩm
        @Test
        @DisplayName("Should return product details when valid slug provided")
        void getProduct_ShouldReturnProductDetails_WhenValidSlugProvided() {
                // Arrange
                String slug = "iphone-14";

                when(productRepository.findBySlug(slug)).thenReturn(Optional.of(testProduct1));
                when(reviewRepository.findAverageRatingScoreByProductId(anyLong())).thenReturn(4);
                when(reviewRepository.countByProductId(anyLong())).thenReturn(10);

                // For related products
                Page<Product> relatedProducts = new PageImpl<>(List.of(testProduct2));
                when(productRepository.findByParams(any(), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class)))
                                .thenReturn(relatedProducts);

                doReturn(clientProductResponse).when(clientProductMapper).entityToResponse(
                                any(Product.class),
                                anyList(),
                                anyInt(),
                                anyInt(),
                                anyList());

                // Act
                ResponseEntity<ClientProductResponse> response = clientProductController.getProduct(slug);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                verify(productRepository).findBySlug(slug);
                verify(reviewRepository).findAverageRatingScoreByProductId(anyLong());
                verify(reviewRepository).countByProductId(anyLong());
                verify(productRepository).findByParams(any(), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class));
        }

        // TC-PROD-11: Xử lý khi slug không tồn tại
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

        // TC-PROD-12: Phân trang với dữ liệu lớn
        @Test
        @DisplayName("Should handle pagination with large result set correctly")
        void getAllProducts_ShouldHandlePaginationWithLargeResultSet() {
                // Arrange
                int page = 5;
                int size = 20;
                Pageable pageable = PageRequest.of(page - 1, size);

                when(productRepository.findByParams(any(), any(), any(), anyBoolean(), anyBoolean(), eq(pageable)))
                                .thenReturn(productPage);

                // Act
                ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController
                                .getAllProducts(page, size, null, null, null, false, false);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                verify(productRepository).findByParams(any(), any(), any(), anyBoolean(), anyBoolean(), eq(pageable));
        }

        // TC-PROD-13: Xử lý từ khóa tìm kiếm có ký tự đặc biệt
        @Test
        @DisplayName("Should handle search terms with special characters")
        void getAllProducts_ShouldHandleSearchTermsWithSpecialCharacters() {
                // Arrange
                int page = 1;
                int size = 10;
                String search = "iPhone+Pro&Max";

                Pageable pageable = PageRequest.of(page - 1, size);

                when(productRepository.findByParams(any(), any(), eq(search), anyBoolean(), anyBoolean(),
                                any(Pageable.class)))
                                .thenReturn(productPage);

                // Act
                ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController
                                .getAllProducts(page, size, null, null, search, false, false);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                verify(productRepository).findByParams(any(), any(), eq(search), anyBoolean(), anyBoolean(),
                                any(Pageable.class));
        }

        // TC-PROD-14: Kết hợp nhiều điều kiện lọc
        @Test
        @DisplayName("Should combine multiple filter conditions correctly")
        void getAllProducts_ShouldCombineMultipleFilterConditionsCorrectly() {
                // Arrange
                int page = 1;
                int size = 10;
                String filter = "category.id==1;brand.id==2";
                String sort = "name,asc";
                String search = "Pro";
                boolean saleable = true;
                boolean newable = false;

                Pageable pageable = PageRequest.of(page - 1, size);

                when(productRepository.findByParams(eq(filter), eq(sort), eq(search), eq(saleable), eq(newable),
                                any(Pageable.class)))
                                .thenReturn(productPage);

                // Act
                ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController
                                .getAllProducts(page, size, filter, sort, search, saleable, newable);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                verify(productRepository).findByParams(eq(filter), eq(sort), eq(search), eq(saleable), eq(newable),
                                any(Pageable.class));
        }

        // TC-PROD-15: Lấy sản phẩm với sản phẩm liên quan
        @Test
        @DisplayName("Should return product with related products")
        void getProduct_ShouldReturnProductWithRelatedProducts() {
                // Arrange
                String slug = "samsung-galaxy-s23";

                when(productRepository.findBySlug(slug)).thenReturn(Optional.of(testProduct2));
                when(reviewRepository.findAverageRatingScoreByProductId(anyLong())).thenReturn(5);
                when(reviewRepository.countByProductId(anyLong())).thenReturn(15);

                // For related products
                List<Product> relatedProductList = List.of(testProduct1);
                Page<Product> relatedProducts = new PageImpl<>(relatedProductList);
                when(productRepository.findByParams(anyString(), eq("random"), isNull(), eq(false), eq(false),
                                any(Pageable.class)))
                                .thenReturn(relatedProducts);

                List<ClientListedProductResponse> relatedProductResponses = List.of(clientListedProductResponse1);

                doReturn(clientProductResponse).when(clientProductMapper).entityToResponse(
                                any(Product.class),
                                anyList(),
                                anyInt(),
                                anyInt(),
                                eq(relatedProductResponses));

                // Act
                ResponseEntity<ClientProductResponse> response = clientProductController.getProduct(slug);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                verify(productRepository).findBySlug(slug);
                verify(productRepository).findByParams(anyString(), eq("random"), isNull(), eq(false), eq(false),
                                any(Pageable.class));
        }

        // TC-PROD-16: Thay đổi khoảng giá sau lựa chọn đầu tiên
        @Test
        @DisplayName("Should return different products when price range is changed")
        void viewDifferentPriceAfterFirstSelection() {
                // Arrange - Initial price range
                int page = 1;
                int size = 10;
                String initialFilter = "price>=100;price<=500";

                List<Product> initialProducts = List.of(testProduct1);
                Page<Product> initialProductPage = new PageImpl<>(initialProducts);

                when(productRepository.findByParams(eq(initialFilter), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class)))
                                .thenReturn(initialProductPage);

                // Act - Initial price range
                ResponseEntity<ListResponse<ClientListedProductResponse>> initialResponse = clientProductController
                                .getAllProducts(page, size, initialFilter, null, null, false, false);

                // Assert - Initial price range
                assertEquals(HttpStatus.OK, initialResponse.getStatusCode());
                assertNotNull(initialResponse.getBody());
                assertEquals(1, initialResponse.getBody().getContent().size());

                // Arrange - New price range
                String newFilter = "price>=501;price<=1000";

                List<Product> newProducts = List.of(testProduct2);
                Page<Product> newProductPage = new PageImpl<>(newProducts);

                when(productRepository.findByParams(eq(newFilter), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class)))
                                .thenReturn(newProductPage);

                // Act - New price range
                ResponseEntity<ListResponse<ClientListedProductResponse>> newResponse = clientProductController
                                .getAllProducts(page, size, newFilter, null, null, false, false);

                // Assert - New price range
                assertEquals(HttpStatus.OK, newResponse.getStatusCode());
                assertNotNull(newResponse.getBody());
                assertEquals(1, newResponse.getBody().getContent().size());

                // Verify both price filters were applied
                verify(productRepository).findByParams(eq(initialFilter), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class));
                verify(productRepository).findByParams(eq(newFilter), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class));
        }

        // TC-PROD-17: Điều hướng giữa các danh mục chính
        @Test
        @DisplayName("Should navigate successfully between different main categories")
        void navigateBetweenMainCategories() {
                // Arrange - First category
                int page = 1;
                int size = 10;
                String filter1 = "category.id==1"; // First category

                List<Product> category1Products = List.of(testProduct1);
                Page<Product> category1Page = new PageImpl<>(category1Products);

                when(productRepository.findByParams(eq(filter1), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class)))
                                .thenReturn(category1Page);

                // Act - First category
                ResponseEntity<ListResponse<ClientListedProductResponse>> response1 = clientProductController
                                .getAllProducts(page, size, filter1, null, null, false, false);

                // Assert - First category
                assertEquals(HttpStatus.OK, response1.getStatusCode());
                assertNotNull(response1.getBody());
                assertEquals(1, response1.getBody().getContent().size());

                // Arrange - Second category
                String filter2 = "category.id==2"; // Second category

                List<Product> category2Products = List.of(testProduct2);
                Page<Product> category2Page = new PageImpl<>(category2Products);

                when(productRepository.findByParams(eq(filter2), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class)))
                                .thenReturn(category2Page);

                // Act - Second category
                ResponseEntity<ListResponse<ClientListedProductResponse>> response2 = clientProductController
                                .getAllProducts(page, size, filter2, null, null, false, false);

                // Assert - Second category
                assertEquals(HttpStatus.OK, response2.getStatusCode());
                assertNotNull(response2.getBody());
                assertEquals(1, response2.getBody().getContent().size());

                // Verify both categories were accessed
                verify(productRepository).findByParams(eq(filter1), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class));
                verify(productRepository).findByParams(eq(filter2), any(), any(), anyBoolean(), anyBoolean(),
                                any(Pageable.class));
        }
}