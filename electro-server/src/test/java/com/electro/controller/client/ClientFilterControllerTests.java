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

/**
 * Test class cho tính năng lọc thương hiệu trong mô-đun khách hàng
 * Kiểm tra các chức năng lọc theo danh mục và từ khóa tìm kiếm
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Cho phép mock linh hoạt hơn
public class ClientFilterControllerTests {

    // Mock đối tượng BrandRepository để giả lập truy vấn dữ liệu
    @Mock
    private BrandRepository brandRepository;

    // Đối tượng controller cần test, được inject mock repository
    @InjectMocks
    private ClientFilterController clientFilterController;

    // Dữ liệu test
    private Brand testBrand1;
    private Brand testBrand2;
    private Brand testBrand3;
    private List<Brand> multipleBrands; // Danh sách nhiều thương hiệu
    private List<Brand> singleBrand; // Danh sách một thương hiệu

    /**
     * Thiết lập dữ liệu trước mỗi test case
     */
    @BeforeEach
    void setUp() {
        // Tạo các thương hiệu mẫu bằng mock để kiểm soát hành vi
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

        // Tạo các danh sách thương hiệu cho test
        multipleBrands = Arrays.asList(testBrand1, testBrand2);
        singleBrand = Collections.singletonList(testBrand3);
    }

    /**
     * Test case 1: Kiểm tra lấy thương hiệu theo slug danh mục
     * Khi: Danh mục tồn tại và có thương hiệu
     * Mong đợi: Trả về danh sách thương hiệu hợp lệ
     */
    @Test
    @DisplayName("Should return brands by category slug")
    void getFilterByCategorySlug_ShouldReturnBrands_WhenCategorySlugExists() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String categorySlug = "smartphones";

        // Cấu hình mock để trả về danh sách thương hiệu khi tìm theo danh mục
        when(brandRepository.findByCategorySlug(eq(categorySlug))).thenReturn(multipleBrands);

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterByCategorySlug(categorySlug);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertNotNull(response.getBody().getFilterBrands()); // Kiểm tra danh sách thương hiệu không null
        assertEquals(2, response.getBody().getFilterBrands().size()); // Kiểm tra số lượng thương hiệu

        // Kiểm tra thông tin của thương hiệu đầu tiên
        ClientBrandResponse firstBrand = response.getBody().getFilterBrands().get(0);
        assertEquals(1L, firstBrand.getBrandId());
        assertEquals("Apple", firstBrand.getBrandName());

        // Kiểm tra thông tin của thương hiệu thứ hai
        ClientBrandResponse secondBrand = response.getBody().getFilterBrands().get(1);
        assertEquals(2L, secondBrand.getBrandId());
        assertEquals("Samsung", secondBrand.getBrandName());

        // Xác minh phương thức repository đã được gọi
        verify(brandRepository).findByCategorySlug(categorySlug);
    }

    /**
     * Test case 2: Kiểm tra lấy thương hiệu theo slug danh mục không tồn tại
     * Khi: Không có thương hiệu nào cho danh mục đã cho
     * Mong đợi: Trả về danh sách rỗng
     */
    @Test
    @DisplayName("Should return empty list when no brands found for category slug")
    void getFilterByCategorySlug_ShouldReturnEmptyList_WhenNoBrandsFound() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String categorySlug = "non-existent-category";

        // Cấu hình mock để trả về danh sách rỗng
        when(brandRepository.findByCategorySlug(eq(categorySlug))).thenReturn(Collections.emptyList());

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterByCategorySlug(categorySlug);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertNotNull(response.getBody().getFilterBrands()); // Kiểm tra danh sách thương hiệu không null
        assertTrue(response.getBody().getFilterBrands().isEmpty()); // Kiểm tra danh sách rỗng

        // Xác minh phương thức repository đã được gọi
        verify(brandRepository).findByCategorySlug(categorySlug);
    }

    /**
     * Test case 3: Kiểm tra lấy thương hiệu theo từ khóa tìm kiếm
     * Khi: Có thương hiệu khớp với từ khóa
     * Mong đợi: Trả về danh sách thương hiệu hợp lệ
     */
    @Test
    @DisplayName("Should return brands matching search query")
    void getFilterBySearchQuery_ShouldReturnBrands_WhenMatchesFound() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String searchQuery = "phone";

        // Cấu hình mock để trả về danh sách thương hiệu
        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(multipleBrands);

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertNotNull(response.getBody().getFilterBrands()); // Kiểm tra danh sách thương hiệu không null
        assertEquals(2, response.getBody().getFilterBrands().size()); // Kiểm tra số lượng thương hiệu

        // Kiểm tra thông tin thương hiệu
        assertEquals(1L, response.getBody().getFilterBrands().get(0).getBrandId());
        assertEquals("Apple", response.getBody().getFilterBrands().get(0).getBrandName());
        assertEquals(2L, response.getBody().getFilterBrands().get(1).getBrandId());
        assertEquals("Samsung", response.getBody().getFilterBrands().get(1).getBrandName());

        // Xác minh phương thức repository đã được gọi
        verify(brandRepository).findBySearchQuery(searchQuery);
    }

    /**
     * Test case 4: Kiểm tra lấy thương hiệu theo từ khóa tìm kiếm không có kết quả
     * Khi: Không có thương hiệu nào khớp với từ khóa
     * Mong đợi: Trả về danh sách rỗng
     */
    @Test
    @DisplayName("Should return empty list when no brands match search query")
    void getFilterBySearchQuery_ShouldReturnEmptyList_WhenNoMatchesFound() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String searchQuery = "non-existent-product";

        // Cấu hình mock để trả về danh sách rỗng
        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(Collections.emptyList());

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertNotNull(response.getBody().getFilterBrands()); // Kiểm tra danh sách thương hiệu không null
        assertTrue(response.getBody().getFilterBrands().isEmpty()); // Kiểm tra danh sách rỗng

        // Xác minh phương thức repository đã được gọi
        verify(brandRepository).findBySearchQuery(searchQuery);
    }

    /**
     * Test case 5: Kiểm tra tìm kiếm không phân biệt chữ hoa, thường
     * Khi: Từ khóa tìm kiếm có chữ hoa và chữ thường lẫn lộn
     * Mong đợi: Vẫn trả về kết quả chính xác
     */
    @Test
    @DisplayName("Should handle case-insensitive search")
    void getFilterBySearchQuery_ShouldHandleCaseInsensitiveSearch() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String searchQuery = "sAMsUnG"; // Kết hợp chữ hoa và chữ thường

        // Cấu hình mock để trả về thương hiệu Samsung
        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(Collections.singletonList(testBrand2));

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertNotNull(response.getBody().getFilterBrands()); // Kiểm tra danh sách thương hiệu không null
        assertEquals(1, response.getBody().getFilterBrands().size()); // Kiểm tra số lượng thương hiệu

        // Kiểm tra thông tin thương hiệu
        assertEquals(2L, response.getBody().getFilterBrands().get(0).getBrandId());
        assertEquals("Samsung", response.getBody().getFilterBrands().get(0).getBrandName());

        // Xác minh phương thức repository đã được gọi
        verify(brandRepository).findBySearchQuery(searchQuery);
    }

    /**
     * Test case 6: Kiểm tra tìm kiếm khi chỉ nhập một phần từ
     * Khi: Từ khóa tìm kiếm chỉ là một phần của tên thương hiệu
     * Mong đợi: Vẫn tìm kiếm được kết quả phù hợp
     */
    @Test
    @DisplayName("Should handle partial word matches in search")
    void getFilterBySearchQuery_ShouldHandlePartialWordMatches() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String searchQuery = "sam"; // Chỉ một phần của "Samsung"

        // Cấu hình mock để trả về thương hiệu Samsung
        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(Collections.singletonList(testBrand2));

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertNotNull(response.getBody().getFilterBrands()); // Kiểm tra danh sách thương hiệu không null
        assertEquals(1, response.getBody().getFilterBrands().size()); // Kiểm tra số lượng thương hiệu

        // Kiểm tra thông tin thương hiệu
        assertEquals(2L, response.getBody().getFilterBrands().get(0).getBrandId());
        assertEquals("Samsung", response.getBody().getFilterBrands().get(0).getBrandName());

        // Xác minh phương thức repository đã được gọi
        verify(brandRepository).findBySearchQuery(searchQuery);
    }

    /**
     * Test case 7: Kiểm tra xử lý ký tự đặc biệt trong tìm kiếm
     * Khi: Từ khóa tìm kiếm có chứa các ký tự đặc biệt
     * Mong đợi: Hệ thống vẫn xử lý được và trả về kết quả
     */
    @Test
    @DisplayName("Should handle special characters in search query")
    void getFilterBySearchQuery_ShouldHandleSpecialCharactersInSearchQuery() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String searchQuery = "ap!@#$%^&*()ple"; // "Apple" với ký tự đặc biệt

        // Giả sử repository sẽ xử lý dọn dẹp ký tự đặc biệt và trả về Apple
        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(Collections.singletonList(testBrand1));

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertNotNull(response.getBody().getFilterBrands()); // Kiểm tra danh sách thương hiệu không null
        assertEquals(1, response.getBody().getFilterBrands().size()); // Kiểm tra số lượng thương hiệu

        // Kiểm tra thông tin thương hiệu
        assertEquals(1L, response.getBody().getFilterBrands().get(0).getBrandId());
        assertEquals("Apple", response.getBody().getFilterBrands().get(0).getBrandName());

        // Xác minh phương thức repository đã được gọi
        verify(brandRepository).findBySearchQuery(searchQuery);
    }

    /**
     * Test case 8: Kiểm tra danh mục có nhiều thương hiệu
     * Khi: Danh mục phổ biến có nhiều thương hiệu
     * Mong đợi: Trả về đầy đủ danh sách thương hiệu
     */
    @Test
    @DisplayName("Should return multiple brands for popular category")
    void getFilterByCategorySlug_ShouldReturnMultipleBrands_ForPopularCategory() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String categorySlug = "laptops";
        List<Brand> laptopBrands = Arrays.asList(testBrand1, testBrand2, testBrand3);

        // Cấu hình mock để trả về danh sách nhiều thương hiệu
        when(brandRepository.findByCategorySlug(eq(categorySlug))).thenReturn(laptopBrands);

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterByCategorySlug(categorySlug);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertNotNull(response.getBody().getFilterBrands()); // Kiểm tra danh sách thương hiệu không null
        assertEquals(3, response.getBody().getFilterBrands().size()); // Kiểm tra số lượng thương hiệu

        // Xác minh phương thức repository đã được gọi
        verify(brandRepository).findByCategorySlug(categorySlug);
    }

    /**
     * Test case 9: Kiểm tra danh mục có một thương hiệu
     * Khi: Danh mục chuyên biệt chỉ có một thương hiệu
     * Mong đợi: Trả về chính xác một thương hiệu
     */
    @Test
    @DisplayName("Should return single brand for niche category")
    void getFilterByCategorySlug_ShouldReturnSingleBrand_ForNicheCategory() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String categorySlug = "smartwatches";

        // Cấu hình mock để trả về danh sách một thương hiệu
        when(brandRepository.findByCategorySlug(eq(categorySlug))).thenReturn(singleBrand);

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterByCategorySlug(categorySlug);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertNotNull(response.getBody().getFilterBrands()); // Kiểm tra danh sách thương hiệu không null
        assertEquals(1, response.getBody().getFilterBrands().size()); // Kiểm tra số lượng thương hiệu

        // Kiểm tra thông tin thương hiệu
        assertEquals(3L, response.getBody().getFilterBrands().get(0).getBrandId());
        assertEquals("Dell", response.getBody().getFilterBrands().get(0).getBrandName());

        // Xác minh phương thức repository đã được gọi
        verify(brandRepository).findByCategorySlug(categorySlug);
    }

    /**
     * Test case 10: Kiểm tra từ khóa tìm kiếm phổ biến
     * Khi: Tìm kiếm với từ khóa phổ biến
     * Mong đợi: Trả về danh sách nhiều thương hiệu liên quan
     */
    @Test
    @DisplayName("Should return popular brands for common search term")
    void getFilterBySearchQuery_ShouldReturnPopularBrands_ForCommonSearchTerm() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String searchQuery = "laptop";
        List<Brand> laptopBrands = Arrays.asList(testBrand1, testBrand2, testBrand3);

        // Cấu hình mock để trả về danh sách nhiều thương hiệu
        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(laptopBrands);

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertNotNull(response.getBody().getFilterBrands()); // Kiểm tra danh sách thương hiệu không null
        assertEquals(3, response.getBody().getFilterBrands().size()); // Kiểm tra số lượng thương hiệu

        // Xác minh phương thức repository đã được gọi
        verify(brandRepository).findBySearchQuery(searchQuery);
    }

    /**
     * Test case 11: Kiểm tra xử lý khoảng trắng trong từ khóa tìm kiếm
     * Khi: Từ khóa tìm kiếm có khoảng trắng
     * Mong đợi: Hệ thống vẫn xử lý được và trả về kết quả
     */
    @Test
    @DisplayName("Should handle search terms with whitespace")
    void getFilterBySearchQuery_ShouldHandleSearchTermsWithWhitespace() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String searchQuery = "iPhone Pro";

        // Cấu hình mock để trả về thương hiệu Apple
        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(Collections.singletonList(testBrand1));

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertNotNull(response.getBody().getFilterBrands()); // Kiểm tra danh sách thương hiệu không null
        assertEquals(1, response.getBody().getFilterBrands().size()); // Kiểm tra số lượng thương hiệu

        // Kiểm tra thông tin thương hiệu
        assertEquals(1L, response.getBody().getFilterBrands().get(0).getBrandId());
        assertEquals("Apple", response.getBody().getFilterBrands().get(0).getBrandName());

        // Xác minh phương thức repository đã được gọi
        verify(brandRepository).findBySearchQuery(searchQuery);
    }

    /**
     * Test case 12: Kiểm tra tìm kiếm với tên sản phẩm kèm số
     * Khi: Từ khóa tìm kiếm có số
     * Mong đợi: Hệ thống vẫn xử lý được và trả về kết quả
     */
    @Test
    @DisplayName("Should handle search terms with product name and number")
    void getFilterBySearchQuery_ShouldHandleSearchTermsWithProductNameAndNumber() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        String searchQuery = "iPhone 14";

        // Cấu hình mock để trả về thương hiệu Apple
        when(brandRepository.findBySearchQuery(eq(searchQuery))).thenReturn(Collections.singletonList(testBrand1));

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ClientFilterResponse> response = clientFilterController.getFilterBySearchQuery(searchQuery);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertNotNull(response.getBody().getFilterBrands()); // Kiểm tra danh sách thương hiệu không null
        assertEquals(1, response.getBody().getFilterBrands().size()); // Kiểm tra số lượng thương hiệu

        // Kiểm tra thông tin thương hiệu
        assertEquals(1L, response.getBody().getFilterBrands().get(0).getBrandId());
        assertEquals("Apple", response.getBody().getFilterBrands().get(0).getBrandName());

        // Xác minh phương thức repository đã được gọi
        verify(brandRepository).findBySearchQuery(searchQuery);
    }
}