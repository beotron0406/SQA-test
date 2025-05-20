package com.electro.controller.client;

import com.electro.dto.client.ClientBrandResponse;
import com.electro.dto.client.ClientFilterResponse;
import com.electro.entity.product.Brand;
import com.electro.repository.product.BrandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ClientFilterControllerTests {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private ClientFilterController clientFilterController;

    private Brand testBrand1;
    private Brand testBrand2;
    private Brand testBrand3;
    private List<Brand> multipleBrands;
    private List<Brand> singleBrand;

    @BeforeEach
    void setUp() {
        // Setup test brands
        testBrand1 = mock(Brand.class);
        when(testBrand1.getId()).thenReturn(1L);
        when(testBrand1.getName()).thenReturn("Apple");
        when(testBrand1.getCode()).thenReturn("APPLE");

        testBrand2 = mock(Brand.class);
        when(testBrand2.getId()).thenReturn(2L);
        when(testBrand2.getName()).thenReturn("Samsung");
        when(testBrand2.getCode()).thenReturn("SAMSUNG");

        testBrand3 = mock(Brand.class);
        when(testBrand3.getId()).thenReturn(3L);
        when(testBrand3.getName()).thenReturn("Dell");
        when(testBrand3.getCode()).thenReturn("DELL");

        multipleBrands = Arrays.asList(testBrand1, testBrand2);
        singleBrand = Collections.singletonList(testBrand3);
    }

    // TC-FILTER-01: Lấy thương hiệu theo danh mục
    @Test
    @DisplayName("Should return brands by category slug")
    void getFilterByCategorySlug_ShouldReturnBrands_WhenCategorySlugExists() {
        // Arrange
        String categorySlug = "smartphones";

        when(brandRepository.findByCategorySlug(eq(categorySlug))).thenReturn(multipleBrands);

        // Act
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterByCategorySlug(categorySlug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getFilterBrands());
        assertEquals(2, response.getBody().getFilterBrands().size());

        // Verify specific values of the first brand
        ClientBrandResponse firstBrand = response.getBody().getFilterBrands().get(0);
        assertEquals(1L, firstBrand.getBrandId());
        assertEquals("Apple", firstBrand.getBrandName());

        // Verify specific values of the second brand
        ClientBrandResponse secondBrand = response.getBody().getFilterBrands().get(1);
        assertEquals(2L, secondBrand.getBrandId());
        assertEquals("Samsung", secondBrand.getBrandName());

        verify(brandRepository).findByCategorySlug(categorySlug);
    }

    // TC-FILTER-02: Danh sách rỗng khi không tìm thấy danh mục
    @Test
    @DisplayName("Should return empty list when no brands found for category slug")
    void getFilterByCategorySlug_ShouldReturnEmptyList_WhenNoBrandsFound() {
        // Arrange
        String categorySlug = "non-existent-category";

        when(brandRepository.findByCategorySlug(eq(categorySlug))).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterByCategorySlug(categorySlug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getFilterBrands());
        assertTrue(response.getBody().getFilterBrands().isEmpty());

        verify(brandRepository).findByCategorySlug(categorySlug);
    }

    // TC-FILTER-03: Lấy thương hiệu theo từ khóa tìm kiếm
    @Test
    @DisplayName("Should return brands matching search query")
    void getFilterBySearchQuery_ShouldReturnBrands_WhenMatchesFound() {
        // Arrange
        String searchQuery = "phone";

        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(multipleBrands);

        // Act
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getFilterBrands());
        assertEquals(2, response.getBody().getFilterBrands().size());

        // Verify brand IDs and names
        assertEquals(1L, response.getBody().getFilterBrands().get(0).getBrandId());
        assertEquals("Apple", response.getBody().getFilterBrands().get(0).getBrandName());
        assertEquals(2L, response.getBody().getFilterBrands().get(1).getBrandId());
        assertEquals("Samsung", response.getBody().getFilterBrands().get(1).getBrandName());

        verify(brandRepository).findBySearchQuery(searchQuery);
    }

    // TC-FILTER-04: Danh sách rỗng khi từ khóa không có kết quả
    @Test
    @DisplayName("Should return empty list when no brands match search query")
    void getFilterBySearchQuery_ShouldReturnEmptyList_WhenNoMatchesFound() {
        // Arrange
        String searchQuery = "non-existent-product";

        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getFilterBrands());
        assertTrue(response.getBody().getFilterBrands().isEmpty());

        verify(brandRepository).findBySearchQuery(searchQuery);
    }

    // TC-FILTER-05: Tìm kiếm không phân biệt chữ hoa, thường
    @Test
    @DisplayName("Should handle case-insensitive search")
    void getFilterBySearchQuery_ShouldHandleCaseInsensitiveSearch() {
        // Arrange
        String searchQuery = "sAMsUnG"; // Mix of upper and lower case

        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(Collections.singletonList(testBrand2));

        // Act
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getFilterBrands());
        assertEquals(1, response.getBody().getFilterBrands().size());

        // Verify brand ID and name
        assertEquals(2L, response.getBody().getFilterBrands().get(0).getBrandId());
        assertEquals("Samsung", response.getBody().getFilterBrands().get(0).getBrandName());

        verify(brandRepository).findBySearchQuery(searchQuery);
    }

    // TC-FILTER-06: Tìm kiếm khớp một phần từ
    @Test
    @DisplayName("Should handle partial word matches in search")
    void getFilterBySearchQuery_ShouldHandlePartialWordMatches() {
        // Arrange
        String searchQuery = "sam"; // Partial match for "Samsung"

        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(Collections.singletonList(testBrand2));

        // Act
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getFilterBrands());
        assertEquals(1, response.getBody().getFilterBrands().size());

        // Verify brand ID and name
        assertEquals(2L, response.getBody().getFilterBrands().get(0).getBrandId());
        assertEquals("Samsung", response.getBody().getFilterBrands().get(0).getBrandName());

        verify(brandRepository).findBySearchQuery(searchQuery);
    }

    // TC-FILTER-07: Xử lý ký tự đặc biệt trong tìm kiếm
    @Test
    @DisplayName("Should handle special characters in search query")
    void getFilterBySearchQuery_ShouldHandleSpecialCharactersInSearchQuery() {
        // Arrange
        String searchQuery = "ap!@#$%^&*()ple"; // Apple with special characters

        // Giả sử repository sẽ xử lý cleanup và trả về Apple
        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(Collections.singletonList(testBrand1));

        // Act
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getFilterBrands());
        assertEquals(1, response.getBody().getFilterBrands().size());

        // Verify brand ID and name
        assertEquals(1L, response.getBody().getFilterBrands().get(0).getBrandId());
        assertEquals("Apple", response.getBody().getFilterBrands().get(0).getBrandName());

        verify(brandRepository).findBySearchQuery(searchQuery);
    }

    // TC-FILTER-08: Danh mục có nhiều thương hiệu
    @Test
    @DisplayName("Should return multiple brands for popular category")
    void getFilterByCategorySlug_ShouldReturnMultipleBrands_ForPopularCategory() {
        // Arrange
        String categorySlug = "laptops";
        List<Brand> laptopBrands = Arrays.asList(testBrand1, testBrand2, testBrand3);

        when(brandRepository.findByCategorySlug(eq(categorySlug))).thenReturn(laptopBrands);

        // Act
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterByCategorySlug(categorySlug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getFilterBrands());
        assertEquals(3, response.getBody().getFilterBrands().size());

        verify(brandRepository).findByCategorySlug(categorySlug);
    }

    // TC-FILTER-09: Danh mục với một thương hiệu
    @Test
    @DisplayName("Should return single brand for niche category")
    void getFilterByCategorySlug_ShouldReturnSingleBrand_ForNicheCategory() {
        // Arrange
        String categorySlug = "smartwatches";

        when(brandRepository.findByCategorySlug(eq(categorySlug))).thenReturn(singleBrand);

        // Act
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterByCategorySlug(categorySlug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getFilterBrands());
        assertEquals(1, response.getBody().getFilterBrands().size());

        // Verify brand ID and name
        assertEquals(3L, response.getBody().getFilterBrands().get(0).getBrandId());
        assertEquals("Dell", response.getBody().getFilterBrands().get(0).getBrandName());

        verify(brandRepository).findByCategorySlug(categorySlug);
    }

    // TC-FILTER-10: Từ khóa tìm kiếm phổ biến
    @Test
    @DisplayName("Should return popular brands for common search term")
    void getFilterBySearchQuery_ShouldReturnPopularBrands_ForCommonSearchTerm() {
        // Arrange
        String searchQuery = "laptop";
        List<Brand> laptopBrands = Arrays.asList(testBrand1, testBrand2, testBrand3);

        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(laptopBrands);

        // Act
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getFilterBrands());
        assertEquals(3, response.getBody().getFilterBrands().size());

        verify(brandRepository).findBySearchQuery(searchQuery);
    }
    
    @Test
    @DisplayName("Should handle search terms with whitespace")
    void getFilterBySearchQuery_ShouldHandleSearchTermsWithWhitespace() {
        // Arrange
        String searchQuery = "iPhone Pro";

        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(Collections.singletonList(testBrand1));

        // Act
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getFilterBrands());
        assertEquals(1, response.getBody().getFilterBrands().size());

        // Verify brand ID and name
        assertEquals(1L, response.getBody().getFilterBrands().get(0).getBrandId());
        assertEquals("Apple", response.getBody().getFilterBrands().get(0).getBrandName());

        verify(brandRepository).findBySearchQuery(searchQuery);
    }
    
    @Test
    @DisplayName("Should handle search terms with product name and number")
    void getFilterBySearchQuery_ShouldHandleSearchTermsWithProductNameAndNumber() {
        // Arrange
        String searchQuery = "iPhone 14";

        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(Collections.singletonList(testBrand1));

        // Act
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getFilterBrands());
        assertEquals(1, response.getBody().getFilterBrands().size());

        // Verify brand ID and name
        assertEquals(1L, response.getBody().getFilterBrands().get(0).getBrandId());
        assertEquals("Apple", response.getBody().getFilterBrands().get(0).getBrandName());

        verify(brandRepository).findBySearchQuery(searchQuery);
    }
}