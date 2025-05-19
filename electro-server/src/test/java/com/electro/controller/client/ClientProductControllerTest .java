package com.electro.controller.client;

import com.electro.dto.ListResponse;
import com.electro.dto.client.ClientListedProductResponse;
import com.electro.dto.client.ClientProductResponse;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientProductControllerTest {

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

        private Product product1;
        private Product product2;
        private List<Product> productList;
        private Page<Product> productPage;
        private ClientListedProductResponse listedProductResponse1;
        private ClientListedProductResponse listedProductResponse2;
        private ClientProductResponse productResponse;
        private List<SimpleProductInventory> productInventories;

        @BeforeEach
        void setUp() {
                // Create test products
                product1 = new Product();
                product1.setId(1L);
                product1.setName("Test Product 1");
                product1.setSlug("test-product-1");
                product1.setCode("TP001");
                product1.setStatus(1);

                product2 = new Product();
                product2.setId(2L);
                product2.setName("Test Product 2");
                product2.setSlug("test-product-2");
                product2.setCode("TP002");
                product2.setStatus(1);

                productList = Arrays.asList(product1, product2);
                productPage = new PageImpl<>(productList);

                // Create test product responses
                listedProductResponse1 = new ClientListedProductResponse();
                listedProductResponse1.setId(1L);
                listedProductResponse1.setName("Test Product 1");
                listedProductResponse1.setSlug("test-product-1");
                listedProductResponse1.setCode("TP001");
                listedProductResponse1.setStatus(1);
                listedProductResponse1.setPrice(1000000.0);
                listedProductResponse1.setInventory(10);
                listedProductResponse1.setCanBeSold(8);

                listedProductResponse2 = new ClientListedProductResponse();
                listedProductResponse2.setId(2L);
                listedProductResponse2.setName("Test Product 2");
                listedProductResponse2.setSlug("test-product-2");
                listedProductResponse2.setCode("TP002");
                listedProductResponse2.setStatus(1);
                listedProductResponse2.setPrice(1500000.0);
                listedProductResponse2.setInventory(5);
                listedProductResponse2.setCanBeSold(3);

                // Create product response
                productResponse = new ClientProductResponse();
                productResponse.setId(1L);
                productResponse.setName("Test Product 1");
                productResponse.setSlug("test-product-1");
                productResponse.setCode("TP001");
                productResponse.setStatus(1);
                productResponse.setPrice(1000000.0);
                productResponse.setInventory(10);
                productResponse.setCanBeSold(8);
                productResponse.setAverageRatingScore(4);
                productResponse.setCountReviews(10);

                // Create inventory data
                productInventories = new ArrayList<>();
                SimpleProductInventory inventory = new SimpleProductInventory() {
                        @Override
                        public Long getProductId() {
                                return 1L;
                        }

                        @Override
                        public Integer getInventory() {
                                return 10;
                        }

                        @Override
                        public Integer getWaitingForDelivery() {
                                return 2;
                        }

                        @Override
                        public Integer getCanBeSold() {
                                return 8;
                        }

                        @Override
                        public Integer getAreComing() {
                                return 5;
                        }
                };
                productInventories.add(inventory);
        }

        @Test
        @DisplayName("Should return all products with pagination")
        void getAllProducts_ShouldReturnAllProductsWithPagination() {
                // Arrange
                when(productRepository.findByParams(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(),
                                any(Pageable.class)))
                                .thenReturn(productPage);

                when(projectionRepository.findSimpleProductInventories(anyList()))
                                .thenReturn(productInventories);

                when(clientProductMapper.entityToListedResponse(eq(product1), any()))
                                .thenReturn(listedProductResponse1);
                when(clientProductMapper.entityToListedResponse(eq(product2), any()))
                                .thenReturn(listedProductResponse2);

                // Act
                ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController
                                .getAllProducts(
                                                1, 10, null, null, null, false, false);

                // Assert
                assertNotNull(response);
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals(2, response.getBody().getItems().size());
                assertEquals("Test Product 1", response.getBody().getItems().get(0).getName());
                assertEquals("Test Product 2", response.getBody().getItems().get(1).getName());

                // Verify
                verify(productRepository).findByParams(isNull(), isNull(), isNull(), eq(false), eq(false),
                                any(Pageable.class));
                verify(projectionRepository).findSimpleProductInventories(anyList());
                verify(clientProductMapper, times(2)).entityToListedResponse(any(Product.class), anyList());
        }

        @Test
        @DisplayName("Should filter products based on criteria")
        void getAllProducts_ShouldFilterProductsBasedOnCriteria() {
                // Arrange
                String filter = "category.id==1";
                String sort = "name,asc";
                String search = "test";
                boolean saleable = true;

                when(productRepository.findByParams(eq(filter), eq(sort), eq(search), eq(saleable), eq(false),
                                any(Pageable.class)))
                                .thenReturn(productPage);

                when(projectionRepository.findSimpleProductInventories(anyList()))
                                .thenReturn(productInventories);

                when(clientProductMapper.entityToListedResponse(any(Product.class), anyList()))
                                .thenReturn(listedProductResponse1)
                                .thenReturn(listedProductResponse2);

                // Act
                ResponseEntity<ListResponse<ClientListedProductResponse>> response = clientProductController
                                .getAllProducts(
                                                1, 10, filter, sort, search, saleable, false);

                // Assert
                assertNotNull(response);
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                // Verify
                verify(productRepository).findByParams(eq(filter), eq(sort), eq(search), eq(saleable), eq(false),
                                any(Pageable.class));
        }

        @Test
        @DisplayName("Should return product by slug")
        void getProduct_ShouldReturnProductBySlug() {
                // Arrange
                String slug = "test-product-1";

                when(productRepository.findBySlug(slug)).thenReturn(Optional.of(product1));
                when(projectionRepository.findSimpleProductInventories(eq(List.of(1L)))).thenReturn(productInventories);
                when(reviewRepository.findAverageRatingScoreByProductId(1L)).thenReturn(4);
                when(reviewRepository.countByProductId(1L)).thenReturn(10);

                // For related products
                when(productRepository.findByParams(anyString(), eq("random"), isNull(), eq(false), eq(false),
                                any(PageRequest.class)))
                                .thenReturn(new PageImpl<>(List.of(product2)));
                when(projectionRepository.findSimpleProductInventories(eq(List.of(2L)))).thenReturn(new ArrayList<>());
                when(clientProductMapper.entityToListedResponse(eq(product2), anyList()))
                                .thenReturn(listedProductResponse2);

                when(clientProductMapper.entityToResponse(
                                eq(product1), anyList(), eq(4), eq(10), anyList())).thenReturn(productResponse);

                // Act
                ResponseEntity<ClientProductResponse> response = clientProductController.getProduct(slug);

                // Assert
                assertNotNull(response);
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("Test Product 1", response.getBody().getName());
                assertEquals(slug, response.getBody().getSlug());

                // Verify
                verify(productRepository).findBySlug(slug);
                verify(projectionRepository).findSimpleProductInventories(eq(List.of(1L)));
                verify(reviewRepository).findAverageRatingScoreByProductId(1L);
                verify(reviewRepository).countByProductId(1L);
                verify(clientProductMapper).entityToResponse(any(), anyList(), anyInt(), anyInt(), anyList());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when product slug not found")
        void getProduct_ShouldThrowResourceNotFoundException_WhenProductSlugNotFound() {
                // Arrange
                String slug = "non-existent-product";
                when(productRepository.findBySlug(slug)).thenReturn(Optional.empty());

                // Act & Assert
                assertThrows(ResourceNotFoundException.class, () -> clientProductController.getProduct(slug));

                // Verify
                verify(productRepository).findBySlug(slug);
                verifyNoInteractions(projectionRepository, clientProductMapper, reviewRepository);
        }
}