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

/**
 * Test class cho tính năng quản lý danh mục sản phẩm trong mô-đun khách hàng
 * Kiểm tra các chức năng hiển thị và tìm kiếm danh mục cho khách hàng
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Cho phép mock linh hoạt hơn
public class ClientCategoryControllerTests {

    // Mock các đối tượng cần thiết cho controller
    @Mock
    private CategoryRepository categoryRepository; // Repository truy vấn dữ liệu danh mục

    @Mock
    private ClientCategoryMapper clientCategoryMapper; // Mapper chuyển đổi giữa entity và DTO

    // Đối tượng controller cần test, được inject các mock ở trên
    @InjectMocks
    private ClientCategoryController clientCategoryController;

    // Dữ liệu test - Cấu trúc phân cấp danh mục nhiều tầng
    private Category parentCategory1; // Danh mục cha 1 (Electronics)
    private Category parentCategory2; // Danh mục cha 2 (Computers)
    private Category childCategory1; // Danh mục con 1 (Smartphones) của Electronics
    private Category childCategory2; // Danh mục con 2 (Laptops) của Computers
    private Category childCategory3; // Danh mục con 3 (Tablets) của Electronics
    private Category grandchildCategory; // Danh mục cháu (Earphones) của Smartphones
    private List<Category> parentCategories; // Danh sách tất cả danh mục cha
    private List<ClientCategoryResponse> clientCategoryResponses; // Danh sách response DTO
    private ClientCategoryResponse mockResponse1; // Response giả lập 1
    private ClientCategoryResponse mockResponse2; // Response giả lập 2

    /**
     * Thiết lập dữ liệu trước mỗi test case
     * Tạo cấu trúc phân cấp danh mục 3 tầng:
     * - Danh mục cha (Electronics, Computers)
     * - Danh mục con (Smartphones, Laptops, Tablets)
     * - Danh mục cháu (Earphones)
     */
    @BeforeEach
    void setUp() {
        // Khởi tạo các danh mục cha
        parentCategory1 = mock(Category.class);
        when(parentCategory1.getId()).thenReturn(1L);
        when(parentCategory1.getName()).thenReturn("Electronics");
        when(parentCategory1.getSlug()).thenReturn("electronics");

        parentCategory2 = mock(Category.class);
        when(parentCategory2.getId()).thenReturn(2L);
        when(parentCategory2.getName()).thenReturn("Computers");
        when(parentCategory2.getSlug()).thenReturn("computers");

        // Khởi tạo các danh mục con
        childCategory1 = mock(Category.class);
        when(childCategory1.getId()).thenReturn(3L);
        when(childCategory1.getName()).thenReturn("Smartphones");
        when(childCategory1.getSlug()).thenReturn("smartphones");
        when(childCategory1.getParentCategory()).thenReturn(parentCategory1); // Thuộc Electronics

        childCategory2 = mock(Category.class);
        when(childCategory2.getId()).thenReturn(4L);
        when(childCategory2.getName()).thenReturn("Laptops");
        when(childCategory2.getSlug()).thenReturn("laptops");
        when(childCategory2.getParentCategory()).thenReturn(parentCategory2); // Thuộc Computers

        childCategory3 = mock(Category.class);
        when(childCategory3.getId()).thenReturn(5L);
        when(childCategory3.getName()).thenReturn("Tablets");
        when(childCategory3.getSlug()).thenReturn("tablets");
        when(childCategory3.getParentCategory()).thenReturn(parentCategory1); // Thuộc Electronics

        // Khởi tạo danh mục cháu
        grandchildCategory = mock(Category.class);
        when(grandchildCategory.getId()).thenReturn(6L);
        when(grandchildCategory.getName()).thenReturn("Earphones");
        when(grandchildCategory.getSlug()).thenReturn("earphones");
        when(grandchildCategory.getParentCategory()).thenReturn(childCategory1); // Thuộc Smartphones

        // Tạo danh sách danh mục cha
        parentCategories = Arrays.asList(parentCategory1, parentCategory2);

        // Tạo các response mock (không cần thiết lập chi tiết các getter/setter)
        mockResponse1 = mock(ClientCategoryResponse.class);
        mockResponse2 = mock(ClientCategoryResponse.class);
        clientCategoryResponses = Arrays.asList(mockResponse1, mockResponse2);
    }

    /**
     * Test case 1: Kiểm tra lấy tất cả danh mục cha cùng các danh mục con
     * Khi: Gọi API lấy tất cả danh mục
     * Mong đợi: Trả về danh sách các danh mục cha kèm theo danh mục con
     */
    @Test
    @DisplayName("Should return all parent categories with child categories")
    void getAllCategories_ShouldReturnAllParentCategoriesWithChildren() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        // Cấu hình mock để trả về danh sách danh mục cha
        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(parentCategories);
        // Cấu hình mapper để trả về DTO có cấu trúc phân cấp
        when(clientCategoryMapper.entityToResponse(eq(parentCategories), anyInt())).thenReturn(clientCategoryResponses);

        // Act - Thực hiện phương thức cần test
        ResponseEntity<CollectionWrapper<ClientCategoryResponse>> response = clientCategoryController
                .getAllCategories();

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertNotNull(response.getBody().getContent()); // Kiểm tra danh sách danh mục không null
        assertEquals(2, response.getBody().getContent().size()); // Kiểm tra số lượng danh mục cha

        // Xác minh các phương thức mock đã được gọi đúng
        verify(categoryRepository).findByParentCategoryIsNull();
        verify(clientCategoryMapper).entityToResponse(eq(parentCategories), eq(3)); // Độ sâu mặc định là 3
    }

    /**
     * Test case 2: Kiểm tra khi không có danh mục nào trong hệ thống
     * Khi: Không có danh mục nào trong database
     * Mong đợi: Trả về danh sách rỗng
     */
    @Test
    @DisplayName("Should return empty list when no categories exist")
    void getAllCategories_ShouldReturnEmptyList_WhenNoCategoriesExist() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        // Cấu hình mock để trả về danh sách rỗng
        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(Collections.emptyList());
        when(clientCategoryMapper.entityToResponse(eq(Collections.emptyList()), anyInt()))
                .thenReturn(Collections.emptyList());

        // Act - Thực hiện phương thức cần test
        ResponseEntity<CollectionWrapper<ClientCategoryResponse>> response = clientCategoryController
                .getAllCategories();

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertTrue(response.getBody().getContent().isEmpty()); // Kiểm tra danh sách rỗng

        // Xác minh các phương thức mock đã được gọi đúng
        verify(categoryRepository).findByParentCategoryIsNull();
        verify(clientCategoryMapper).entityToResponse(eq(Collections.emptyList()), eq(3));
    }

    /**
     * Test case 3: Kiểm tra lấy danh mục theo slug hợp lệ
     * Khi: Gọi API với slug tồn tại
     * Mong đợi: Trả về thông tin chi tiết của danh mục
     */
    @Test
    @DisplayName("Should return category details when valid slug provided")
    void getCategory_ShouldReturnCategoryDetails_WhenValidSlugProvided() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String slug = "smartphones";
        ClientCategoryResponse mockCategoryResponse = mock(ClientCategoryResponse.class);

        // Cấu hình mock để trả về danh mục khi tìm theo slug
        when(categoryRepository.findBySlug(eq(slug))).thenReturn(Optional.of(childCategory1));
        when(clientCategoryMapper.entityToResponse(eq(childCategory1), anyBoolean())).thenReturn(mockCategoryResponse);

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientCategoryResponse> response = clientCategoryController.getCategory(slug);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertSame(mockCategoryResponse, response.getBody()); // Kiểm tra đúng đối tượng trả về

        // Xác minh các phương thức mock đã được gọi đúng
        verify(categoryRepository).findBySlug(slug);
        verify(clientCategoryMapper).entityToResponse(eq(childCategory1), eq(false));
    }

    /**
     * Test case 4: Kiểm tra khi slug không tồn tại
     * Khi: Gọi API với slug không tồn tại
     * Mong đợi: Ném ngoại lệ ResourceNotFoundException
     */
    @Test
    @DisplayName("Should throw ResourceNotFoundException when invalid slug provided")
    void getCategory_ShouldThrowResourceNotFoundException_WhenInvalidSlugProvided() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String slug = "non-existent-category"; // Slug không tồn tại

        // Cấu hình mock để trả về Optional rỗng (không tìm thấy danh mục)
        when(categoryRepository.findBySlug(eq(slug))).thenReturn(Optional.empty());

        // Act & Assert - Thực hiện và kiểm tra ngoại lệ
        assertThrows(ResourceNotFoundException.class,
                () -> clientCategoryController.getCategory(slug));

        // Xác minh các phương thức mock đã được gọi đúng
        verify(categoryRepository).findBySlug(slug);
        // Xác minh mapper không được gọi khi không tìm thấy danh mục
        verify(clientCategoryMapper, never()).entityToResponse(any(Category.class), anyBoolean());
    }

    /**
     * Test case 5: Kiểm tra lấy danh mục cha cùng với danh mục con
     * Khi: Gọi API với slug của danh mục cha
     * Mong đợi: Trả về thông tin danh mục cha kèm danh mục con
     */
    @Test
    @DisplayName("Should return parent category with its children")
    void getCategory_ShouldReturnParentCategoryWithChildren() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String slug = "electronics";
        ClientCategoryResponse mockResponse = mock(ClientCategoryResponse.class);

        // Cấu hình mock để trả về danh mục cha
        when(categoryRepository.findBySlug(eq(slug))).thenReturn(Optional.of(parentCategory1));
        when(clientCategoryMapper.entityToResponse(eq(parentCategory1), eq(false))).thenReturn(mockResponse);

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientCategoryResponse> response = clientCategoryController.getCategory(slug);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertSame(mockResponse, response.getBody()); // Kiểm tra đúng đối tượng trả về

        // Xác minh các phương thức mock đã được gọi đúng
        verify(categoryRepository).findBySlug(slug);
        verify(clientCategoryMapper).entityToResponse(eq(parentCategory1), eq(false));
    }

    /**
     * Test case 6: Kiểm tra lấy danh mục lá (không có danh mục con)
     * Khi: Gọi API với slug của danh mục lá
     * Mong đợi: Trả về thông tin danh mục không có danh mục con
     */
    @Test
    @DisplayName("Should return leaf category without children")
    void getCategory_ShouldReturnLeafCategoryWithoutChildren() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String slug = "earphones";
        ClientCategoryResponse mockResponse = mock(ClientCategoryResponse.class);

        // Cấu hình mock để trả về danh mục lá
        when(categoryRepository.findBySlug(eq(slug))).thenReturn(Optional.of(grandchildCategory));
        when(clientCategoryMapper.entityToResponse(eq(grandchildCategory), eq(false))).thenReturn(mockResponse);

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientCategoryResponse> response = clientCategoryController.getCategory(slug);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertSame(mockResponse, response.getBody()); // Kiểm tra đúng đối tượng trả về

        // Xác minh các phương thức mock đã được gọi đúng
        verify(categoryRepository).findBySlug(slug);
        verify(clientCategoryMapper).entityToResponse(eq(grandchildCategory), eq(false));
    }

    /**
     * Test case 7: Kiểm tra cấu trúc phân cấp đa tầng
     * Khi: Lấy tất cả danh mục
     * Mong đợi: Trả về cấu trúc phân cấp đúng (danh mục cha - con - cháu)
     */
    @Test
    @DisplayName("Should return categories with proper nesting structure")
    void getAllCategories_ShouldReturnCategoriesWithProperNestingStructure() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        List<ClientCategoryResponse> nestedResponses = new ArrayList<>();
        ClientCategoryResponse nestedMockResponse = mock(ClientCategoryResponse.class);
        nestedResponses.add(nestedMockResponse);

        // Cấu hình mock để trả về một danh mục cha
        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(Collections.singletonList(parentCategory1));
        // Cấu hình mapper để trả về cấu trúc phân cấp của danh mục
        when(clientCategoryMapper.entityToResponse(anyList(), eq(3))).thenReturn(nestedResponses);

        // Act - Thực hiện phương thức cần test
        ResponseEntity<CollectionWrapper<ClientCategoryResponse>> response = clientCategoryController
                .getAllCategories();

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertNotNull(response.getBody().getContent()); // Kiểm tra danh sách danh mục không null
        assertEquals(1, response.getBody().getContent().size()); // Kiểm tra số lượng danh mục cha
        assertSame(nestedMockResponse, response.getBody().getContent().get(0)); // Kiểm tra đúng đối tượng trả về

        // Xác minh các phương thức mock đã được gọi đúng
        verify(categoryRepository).findByParentCategoryIsNull();
        verify(clientCategoryMapper).entityToResponse(anyList(), eq(3));
    }

    /**
     * Test case 8: Kiểm tra giới hạn độ sâu của danh mục
     * Khi: Lấy tất cả danh mục với độ sâu nhất định
     * Mong đợi: Mapper được gọi với tham số độ sâu đúng
     */
    @Test
    @DisplayName("Should return categories with specified depth limit")
    void getAllCategories_ShouldReturnCategoriesWithSpecifiedDepthLimit() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(parentCategories);
        when(clientCategoryMapper.entityToResponse(eq(parentCategories), eq(3))).thenReturn(clientCategoryResponses);

        // Act - Thực hiện phương thức cần test
        ResponseEntity<CollectionWrapper<ClientCategoryResponse>> response = clientCategoryController
                .getAllCategories();

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null

        // Xác minh các phương thức mock đã được gọi đúng
        verify(categoryRepository).findByParentCategoryIsNull();
        // Xác minh tham số độ sâu là 3 (giá trị mặc định trong controller)
        verify(clientCategoryMapper).entityToResponse(eq(parentCategories), eq(3));
    }

    /**
     * Test case 9: Kiểm tra danh mục có nhiều con
     * Khi: Gọi API với slug của danh mục có nhiều con
     * Mong đợi: Trả về thông tin danh mục kèm tất cả danh mục con
     */
    @Test
    @DisplayName("Should return category with all its children")
    void getCategory_ShouldReturnCategoryWithAllChildren() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String slug = "computers";
        ClientCategoryResponse mockResponse = mock(ClientCategoryResponse.class);

        // Cấu hình mock để trả về danh mục cha có nhiều con
        when(categoryRepository.findBySlug(eq(slug))).thenReturn(Optional.of(parentCategory2));
        when(clientCategoryMapper.entityToResponse(eq(parentCategory2), eq(false))).thenReturn(mockResponse);

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientCategoryResponse> response = clientCategoryController.getCategory(slug);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertSame(mockResponse, response.getBody()); // Kiểm tra đúng đối tượng trả về

        // Xác minh các phương thức mock đã được gọi đúng
        verify(categoryRepository).findBySlug(slug);
        verify(clientCategoryMapper).entityToResponse(eq(parentCategory2), eq(false));
    }

    /**
     * Test case 10: Kiểm tra xử lý slug có ký tự đặc biệt
     * Khi: Gọi API với slug chứa ký tự đặc biệt
     * Mong đợi: Hệ thống vẫn xử lý được và trả về kết quả
     */
    @Test
    @DisplayName("Should handle category slug with special characters")
    void getCategory_ShouldHandleCategorySlugWithSpecialCharacters() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String slug = "gaming-&-entertainment"; // Slug chứa ký tự đặc biệt &

        // Tạo danh mục mẫu với ký tự đặc biệt
        Category specialCategory = mock(Category.class);
        when(specialCategory.getId()).thenReturn(9L);
        when(specialCategory.getName()).thenReturn("Gaming & Entertainment");
        when(specialCategory.getSlug()).thenReturn("gaming-&-entertainment");

        ClientCategoryResponse mockResponse = mock(ClientCategoryResponse.class);

        // Cấu hình mock để trả về danh mục có slug đặc biệt
        when(categoryRepository.findBySlug(eq(slug))).thenReturn(Optional.of(specialCategory));
        when(clientCategoryMapper.entityToResponse(eq(specialCategory), eq(false))).thenReturn(mockResponse);

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientCategoryResponse> response = clientCategoryController.getCategory(slug);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertSame(mockResponse, response.getBody()); // Kiểm tra đúng đối tượng trả về

        // Xác minh các phương thức mock đã được gọi đúng
        verify(categoryRepository).findBySlug(slug);
        verify(clientCategoryMapper).entityToResponse(eq(specialCategory), eq(false));
    }
}