package com.electro.controller.client;

import com.electro.dto.CollectionWrapper;
import com.electro.dto.client.ClientCategoryResponse;
import com.electro.entity.product.Category;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.client.ClientCategoryMapper;
import com.electro.repository.product.CategoryRepository;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ClientCategoryControllerTests {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ClientCategoryMapper clientCategoryMapper;

    @InjectMocks
    private ClientCategoryController clientCategoryController;

    private Category parentCategory1;
    private Category parentCategory2;
    private Category childCategory1;
    private Category childCategory2;
    private Category childCategory3;
    private Category grandchildCategory;
    private List<Category> parentCategories;
    private List<ClientCategoryResponse> clientCategoryResponses;
    private ClientCategoryResponse mockResponse1;
    private ClientCategoryResponse mockResponse2;

    @BeforeEach
    void setUp() {
        // Setup parent categories
        parentCategory1 = mock(Category.class);
        when(parentCategory1.getId()).thenReturn(1L);
        when(parentCategory1.getName()).thenReturn("Electronics");
        when(parentCategory1.getSlug()).thenReturn("electronics");

        parentCategory2 = mock(Category.class);
        when(parentCategory2.getId()).thenReturn(2L);
        when(parentCategory2.getName()).thenReturn("Computers");
        when(parentCategory2.getSlug()).thenReturn("computers");

        // Setup child categories
        childCategory1 = mock(Category.class);
        when(childCategory1.getId()).thenReturn(3L);
        when(childCategory1.getName()).thenReturn("Smartphones");
        when(childCategory1.getSlug()).thenReturn("smartphones");
        when(childCategory1.getParentCategory()).thenReturn(parentCategory1);

        childCategory2 = mock(Category.class);
        when(childCategory2.getId()).thenReturn(4L);
        when(childCategory2.getName()).thenReturn("Laptops");
        when(childCategory2.getSlug()).thenReturn("laptops");
        when(childCategory2.getParentCategory()).thenReturn(parentCategory2);

        childCategory3 = mock(Category.class);
        when(childCategory3.getId()).thenReturn(5L);
        when(childCategory3.getName()).thenReturn("Tablets");
        when(childCategory3.getSlug()).thenReturn("tablets");
        when(childCategory3.getParentCategory()).thenReturn(parentCategory1);

        // Setup grandchild category
        grandchildCategory = mock(Category.class);
        when(grandchildCategory.getId()).thenReturn(6L);
        when(grandchildCategory.getName()).thenReturn("Earphones");
        when(grandchildCategory.getSlug()).thenReturn("earphones");
        when(grandchildCategory.getParentCategory()).thenReturn(childCategory1);

        // Setup lists
        parentCategories = Arrays.asList(parentCategory1, parentCategory2);

        // Tạo các mock response objects (nhưng không cần stub getter/setter)
        mockResponse1 = mock(ClientCategoryResponse.class);
        mockResponse2 = mock(ClientCategoryResponse.class);
        clientCategoryResponses = Arrays.asList(mockResponse1, mockResponse2);
    }

    // TC-CAT-01: Lấy tất cả danh mục cha với danh mục con
    @Test
    @DisplayName("Should return all parent categories with child categories")
    void getAllCategories_ShouldReturnAllParentCategoriesWithChildren() {
        // Arrange
        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(parentCategories);
        when(clientCategoryMapper.entityToResponse(eq(parentCategories), anyInt())).thenReturn(clientCategoryResponses);

        // Act
        ResponseEntity<CollectionWrapper<ClientCategoryResponse>> response = clientCategoryController
                .getAllCategories();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getContent());
        assertEquals(2, response.getBody().getContent().size());

        verify(categoryRepository).findByParentCategoryIsNull();
        verify(clientCategoryMapper).entityToResponse(eq(parentCategories), eq(3)); // Default depth is 3
    }

    // TC-CAT-02: Danh sách rỗng khi không có danh mục nào
    @Test
    @DisplayName("Should return empty list when no categories exist")
    void getAllCategories_ShouldReturnEmptyList_WhenNoCategoriesExist() {
        // Arrange
        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(Collections.emptyList());
        when(clientCategoryMapper.entityToResponse(eq(Collections.emptyList()), anyInt()))
                .thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<CollectionWrapper<ClientCategoryResponse>> response = clientCategoryController
                .getAllCategories();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getContent().isEmpty());

        verify(categoryRepository).findByParentCategoryIsNull();
        verify(clientCategoryMapper).entityToResponse(eq(Collections.emptyList()), eq(3));
    }

    // TC-CAT-03: Lấy danh mục theo slug hợp lệ
    @Test
    @DisplayName("Should return category details when valid slug provided")
    void getCategory_ShouldReturnCategoryDetails_WhenValidSlugProvided() {
        // Arrange
        String slug = "smartphones";
        ClientCategoryResponse mockCategoryResponse = mock(ClientCategoryResponse.class);

        when(categoryRepository.findBySlug(eq(slug))).thenReturn(Optional.of(childCategory1));
        when(clientCategoryMapper.entityToResponse(eq(childCategory1), anyBoolean())).thenReturn(mockCategoryResponse);

        // Act
        ResponseEntity<ClientCategoryResponse> response = clientCategoryController.getCategory(slug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertSame(mockCategoryResponse, response.getBody());

        verify(categoryRepository).findBySlug(slug);
        verify(clientCategoryMapper).entityToResponse(eq(childCategory1), eq(false));
    }

    // TC-CAT-04: Xử lý khi slug không tồn tại
    @Test
    @DisplayName("Should throw ResourceNotFoundException when invalid slug provided")
    void getCategory_ShouldThrowResourceNotFoundException_WhenInvalidSlugProvided() {
        // Arrange
        String slug = "non-existent-category";

        when(categoryRepository.findBySlug(eq(slug))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> clientCategoryController.getCategory(slug));

        verify(categoryRepository).findBySlug(slug);
        verify(clientCategoryMapper, never()).entityToResponse(any(Category.class), anyBoolean());
    }

    // TC-CAT-05: Lấy danh mục cha cùng danh mục con
    @Test
    @DisplayName("Should return parent category with its children")
    void getCategory_ShouldReturnParentCategoryWithChildren() {
        // Arrange
        String slug = "electronics";
        ClientCategoryResponse mockResponse = mock(ClientCategoryResponse.class);

        when(categoryRepository.findBySlug(eq(slug))).thenReturn(Optional.of(parentCategory1));
        when(clientCategoryMapper.entityToResponse(eq(parentCategory1), eq(false))).thenReturn(mockResponse);

        // Act
        ResponseEntity<ClientCategoryResponse> response = clientCategoryController.getCategory(slug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertSame(mockResponse, response.getBody());

        verify(categoryRepository).findBySlug(slug);
        verify(clientCategoryMapper).entityToResponse(eq(parentCategory1), eq(false));
    }

    // TC-CAT-06: Lấy danh mục lá không có con
    @Test
    @DisplayName("Should return leaf category without children")
    void getCategory_ShouldReturnLeafCategoryWithoutChildren() {
        // Arrange
        String slug = "earphones";
        ClientCategoryResponse mockResponse = mock(ClientCategoryResponse.class);

        when(categoryRepository.findBySlug(eq(slug))).thenReturn(Optional.of(grandchildCategory));
        when(clientCategoryMapper.entityToResponse(eq(grandchildCategory), eq(false))).thenReturn(mockResponse);

        // Act
        ResponseEntity<ClientCategoryResponse> response = clientCategoryController.getCategory(slug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertSame(mockResponse, response.getBody());

        verify(categoryRepository).findBySlug(slug);
        verify(clientCategoryMapper).entityToResponse(eq(grandchildCategory), eq(false));
    }

    // TC-CAT-07: Kiểm tra cấu trúc phân cấp đa tầng
    @Test
    @DisplayName("Should return categories with proper nesting structure")
    void getAllCategories_ShouldReturnCategoriesWithProperNestingStructure() {
        // Arrange
        List<ClientCategoryResponse> nestedResponses = new ArrayList<>();
        ClientCategoryResponse nestedMockResponse = mock(ClientCategoryResponse.class);
        nestedResponses.add(nestedMockResponse);

        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(Collections.singletonList(parentCategory1));
        when(clientCategoryMapper.entityToResponse(anyList(), eq(3))).thenReturn(nestedResponses);

        // Act
        ResponseEntity<CollectionWrapper<ClientCategoryResponse>> response = clientCategoryController
                .getAllCategories();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getContent());
        assertEquals(1, response.getBody().getContent().size());
        assertSame(nestedMockResponse, response.getBody().getContent().get(0));

        verify(categoryRepository).findByParentCategoryIsNull();
        verify(clientCategoryMapper).entityToResponse(anyList(), eq(3));
    }

    // TC-CAT-08: Kiểm tra giới hạn độ sâu của danh mục
    @Test
    @DisplayName("Should return categories with specified depth limit")
    void getAllCategories_ShouldReturnCategoriesWithSpecifiedDepthLimit() {
        // Arrange
        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(parentCategories);
        when(clientCategoryMapper.entityToResponse(eq(parentCategories), eq(3))).thenReturn(clientCategoryResponses);

        // Act
        ResponseEntity<CollectionWrapper<ClientCategoryResponse>> response = clientCategoryController
                .getAllCategories();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(categoryRepository).findByParentCategoryIsNull();
        // Verify depth parameter is 3 (default value in controller)
        verify(clientCategoryMapper).entityToResponse(eq(parentCategories), eq(3));
    }

    // TC-CAT-09: Kiểm tra danh mục có nhiều con
    @Test
    @DisplayName("Should return category with all its children")
    void getCategory_ShouldReturnCategoryWithAllChildren() {
        // Arrange
        String slug = "computers";
        ClientCategoryResponse mockResponse = mock(ClientCategoryResponse.class);

        when(categoryRepository.findBySlug(eq(slug))).thenReturn(Optional.of(parentCategory2));
        when(clientCategoryMapper.entityToResponse(eq(parentCategory2), eq(false))).thenReturn(mockResponse);

        // Act
        ResponseEntity<ClientCategoryResponse> response = clientCategoryController.getCategory(slug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertSame(mockResponse, response.getBody());

        verify(categoryRepository).findBySlug(slug);
        verify(clientCategoryMapper).entityToResponse(eq(parentCategory2), eq(false));
    }

    // TC-CAT-10: Kiểm tra xử lý slug có ký tự đặc biệt
    @Test
    @DisplayName("Should handle category slug with special characters")
    void getCategory_ShouldHandleCategorySlugWithSpecialCharacters() {
        // Arrange
        String slug = "gaming-&-entertainment";

        Category specialCategory = mock(Category.class);
        when(specialCategory.getId()).thenReturn(9L);
        when(specialCategory.getName()).thenReturn("Gaming & Entertainment");
        when(specialCategory.getSlug()).thenReturn("gaming-&-entertainment");

        ClientCategoryResponse mockResponse = mock(ClientCategoryResponse.class);

        when(categoryRepository.findBySlug(eq(slug))).thenReturn(Optional.of(specialCategory));
        when(clientCategoryMapper.entityToResponse(eq(specialCategory), eq(false))).thenReturn(mockResponse);

        // Act
        ResponseEntity<ClientCategoryResponse> response = clientCategoryController.getCategory(slug);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertSame(mockResponse, response.getBody());

        verify(categoryRepository).findBySlug(slug);
        verify(clientCategoryMapper).entityToResponse(eq(specialCategory), eq(false));
    }
}