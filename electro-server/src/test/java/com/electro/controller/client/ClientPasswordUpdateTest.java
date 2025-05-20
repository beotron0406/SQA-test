package com.electro.controller.client;

import com.electro.dto.client.ClientPasswordSettingUserRequest;
import com.electro.entity.authentication.User;
import com.electro.repository.authentication.UserRepository;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class cho tính năng cập nhật mật khẩu của khách hàng
 * Sử dụng Mockito để giả lập các dependencies
 */
@ExtendWith(MockitoExtension.class)
public class ClientPasswordUpdateTest {

    // Mock các đối tượng cần thiết cho controller
    @Mock
    private UserRepository userRepository; // Repository truy vấn dữ liệu người dùng

    @Mock
    private PasswordEncoder passwordEncoder; // Encoder mã hóa mật khẩu

    @Mock
    private Authentication authentication; // Object xác thực người dùng

    // Đối tượng controller cần test, được inject các mock ở trên
    @InjectMocks
    private ClientUserController clientUserController;

    // Dữ liệu test
    private User testUser; // Đối tượng người dùng mẫu
    private static final String TEST_USERNAME = "beotron01"; // Username mẫu
    private static final String ENCODED_OLD_PASSWORD = "encodedOldPassword"; // Mật khẩu cũ đã mã hóa
    private static final String ENCODED_NEW_PASSWORD = "encodedNewPassword"; // Mật khẩu mới đã mã hóa

    /**
     * Thiết lập dữ liệu trước mỗi test case
     */
    @BeforeEach
    void setUp() {
        // Giả lập kết quả trả về của authentication
        when(authentication.getName()).thenReturn(TEST_USERNAME);

        // Khởi tạo người dùng mẫu với mật khẩu đã mã hóa
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(TEST_USERNAME);
        testUser.setEmail("test@example.com");
        testUser.setPhone("1234567890");
        testUser.setFullname("Test User");
        testUser.setPassword(ENCODED_OLD_PASSWORD);
    }

    /**
     * Test case 1: Kiểm tra đổi mật khẩu với giá trị hợp lệ
     */
    @Test
    @DisplayName("1. Đổi mật khẩu với giá trị hợp lệ")
    void updatePasswordSetting_WithValidValue() throws Exception {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("oldPassword"); // Mật khẩu cũ chưa mã hóa
        request.setNewPassword("validNewPassword123@"); // Mật khẩu mới hợp lệ

        // Cấu hình các mock để trả về kết quả mong muốn
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", ENCODED_OLD_PASSWORD)).thenReturn(true); // Xác thực mật khẩu cũ
                                                                                             // thành công
        when(passwordEncoder.encode("validNewPassword123@")).thenReturn(ENCODED_NEW_PASSWORD); // Mã hóa mật khẩu mới

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ObjectNode> response = clientUserController.updatePasswordSetting(authentication, request);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertEquals(ENCODED_NEW_PASSWORD, testUser.getPassword()); // Kiểm tra mật khẩu đã được cập nhật

        // Xác minh các phương thức mock đã được gọi đúng
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches("oldPassword", ENCODED_OLD_PASSWORD);
        verify(passwordEncoder).encode("validNewPassword123@");
        verify(userRepository).save(testUser);
    }

    /**
     * Test case 2: Kiểm tra đổi mật khẩu với mật khẩu mới giống mật khẩu cũ
     */
    @Test
    @DisplayName("2. Đổi mật khẩu với mật khẩu mới giống mật khẩu cũ")
    void updatePasswordSetting_NewPasswordSameAsOldPassword() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("oldPassword"); // Mật khẩu cũ
        request.setNewPassword("oldPassword"); // Mật khẩu mới giống mật khẩu cũ

        // Cấu hình các mock để trả về kết quả mong muốn
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", ENCODED_OLD_PASSWORD)).thenReturn(true); // Xác thực mật khẩu cũ
                                                                                             // thành công

        // Giả lập ném ngoại lệ khi mật khẩu mới giống mật khẩu cũ
        doAnswer(invocation -> {
            throw new Exception("Mật khẩu mới không được giống mật khẩu cũ");
        }).when(passwordEncoder).encode("oldPassword");

        // Act & Assert - Thực hiện và kiểm tra ngoại lệ
        Exception exception = assertThrows(Exception.class,
                () -> clientUserController.updatePasswordSetting(authentication, request));
        assertEquals("Mật khẩu mới không được giống mật khẩu cũ", exception.getMessage());

        // Xác minh các phương thức mock đã được gọi đúng
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches("oldPassword", ENCODED_OLD_PASSWORD);
        verify(passwordEncoder).encode("oldPassword");
        verify(userRepository, never()).save(any()); // Không lưu khi có lỗi
    }

    /**
     * Test case 3: Kiểm tra đổi mật khẩu với mật khẩu mới chứa ký tự đặc biệt (hợp
     * lệ)
     */
    @Test
    @DisplayName("3. Đổi mật khẩu với mật khẩu mới chứa ký tự đặc biệt")
    void updatePasswordSetting_WithSpecialCharacter() throws Exception {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("oldPassword"); // Mật khẩu cũ
        request.setNewPassword("newPassword!@#$%"); // Mật khẩu mới chứa ký tự đặc biệt

        // Cấu hình các mock để trả về kết quả mong muốn
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", ENCODED_OLD_PASSWORD)).thenReturn(true); // Xác thực mật khẩu cũ
                                                                                             // thành công
        when(passwordEncoder.encode("newPassword!@#$%")).thenReturn(ENCODED_NEW_PASSWORD); // Mã hóa mật khẩu mới

        // Act - Thực hiện phương thức cần test
        ResponseEntity<ObjectNode> response = clientUserController.updatePasswordSetting(authentication, request);

        // Assert - Kiểm tra kết quả
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
        assertNotNull(response.getBody()); // Kiểm tra response body không null
        assertEquals(ENCODED_NEW_PASSWORD, testUser.getPassword()); // Kiểm tra mật khẩu đã được cập nhật

        // Xác minh các phương thức mock đã được gọi đúng
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches("oldPassword", ENCODED_OLD_PASSWORD);
        verify(passwordEncoder).encode("newPassword!@#$%");
        verify(userRepository).save(testUser);
    }

    /**
     * Test case 4: Kiểm tra đổi mật khẩu với mật khẩu mới quá ngắn (1 ký tự)
     */
    @Test
    @DisplayName("4. Đổi mật khẩu với mật khẩu mới chỉ có 1 ký tự")
    void updatePasswordSetting_WithOneCharacter() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("oldPassword"); // Mật khẩu cũ
        request.setNewPassword("a"); // Mật khẩu mới quá ngắn (1 ký tự)

        // Cấu hình các mock để trả về kết quả mong muốn
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", ENCODED_OLD_PASSWORD)).thenReturn(true); // Xác thực mật khẩu cũ
                                                                                             // thành công

        // Giả lập ném ngoại lệ khi mật khẩu mới quá ngắn
        doAnswer(invocation -> {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự");
        }).when(passwordEncoder).encode("a");

        // Act & Assert - Thực hiện và kiểm tra ngoại lệ
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> clientUserController.updatePasswordSetting(authentication, request));
        assertEquals("Mật khẩu phải có ít nhất 8 ký tự", exception.getMessage());

        // Xác minh các phương thức mock đã được gọi đúng
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches("oldPassword", ENCODED_OLD_PASSWORD);
        verify(passwordEncoder).encode("a");
        verify(userRepository, never()).save(any()); // Không lưu khi có lỗi
    }

    /**
     * Test case 5: Kiểm tra đổi mật khẩu với mật khẩu cũ không chính xác
     */
    @Test
    @DisplayName("5. Đổi mật khẩu với mật khẩu cũ không chính xác")
    void updatePasswordSetting_WithIncorrectOldPassword() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("wrongOldPassword"); // Mật khẩu cũ không chính xác
        request.setNewPassword("newPassword123"); // Mật khẩu mới hợp lệ

        // Cấu hình các mock để trả về kết quả mong muốn
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOldPassword", ENCODED_OLD_PASSWORD)).thenReturn(false); // Xác thực mật khẩu
                                                                                                   // cũ thất bại

        // Act & Assert - Thực hiện và kiểm tra ngoại lệ
        Exception exception = assertThrows(Exception.class,
                () -> clientUserController.updatePasswordSetting(authentication, request));
        assertEquals("Wrong old password", exception.getMessage());

        // Xác minh các phương thức mock đã được gọi đúng
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches("wrongOldPassword", ENCODED_OLD_PASSWORD);
        verify(passwordEncoder, never()).encode(anyString()); // Không mã hóa mật khẩu mới
        verify(userRepository, never()).save(any()); // Không lưu khi có lỗi
    }

    /**
     * Test case 6: Kiểm tra đổi mật khẩu với mật khẩu mới không đủ phức tạp
     */
    @Test
    @DisplayName("6. Đổi mật khẩu với mật khẩu mới thiếu độ phức tạp")
    void updatePasswordSetting_WithWeakPassword() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("oldPassword"); // Mật khẩu cũ
        request.setNewPassword("simple"); // Mật khẩu mới quá đơn giản

        // Cấu hình các mock để trả về kết quả mong muốn
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", ENCODED_OLD_PASSWORD)).thenReturn(true); // Xác thực mật khẩu cũ
                                                                                             // thành công

        // Giả lập ném ngoại lệ khi mật khẩu mới không đủ phức tạp
        doAnswer(invocation -> {
            throw new IllegalArgumentException("Mật khẩu phải bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt");
        }).when(passwordEncoder).encode("simple");

        // Act & Assert - Thực hiện và kiểm tra ngoại lệ
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> clientUserController.updatePasswordSetting(authentication, request));
        assertEquals("Mật khẩu phải bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt", exception.getMessage());

        // Xác minh các phương thức mock đã được gọi đúng
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches("oldPassword", ENCODED_OLD_PASSWORD);
        verify(passwordEncoder).encode("simple");
        verify(userRepository, never()).save(any()); // Không lưu khi có lỗi
    }

    /**
     * Test case 7: Kiểm tra khi người dùng không tồn tại
     */
    @Test
    @DisplayName("7. Kiểm tra khi không có người dùng")
    void updatePasswordSetting_WhenUserNotFound() {
        // Arrange - Chuẩn bị dữ liệu đầu vào
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("oldPassword"); // Mật khẩu cũ
        request.setNewPassword("newPassword123"); // Mật khẩu mới hợp lệ

        // Cấu hình mock để trả về Optional.empty() -> không tìm thấy người dùng
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert - Thực hiện và kiểm tra ngoại lệ
        assertThrows(UsernameNotFoundException.class,
                () -> clientUserController.updatePasswordSetting(authentication, request));

        // Xác minh các phương thức không được gọi khi không tìm thấy người dùng
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder, never()).matches(anyString(), anyString()); // Không kiểm tra mật khẩu
        verify(passwordEncoder, never()).encode(anyString()); // Không mã hóa mật khẩu
        verify(userRepository, never()).save(any()); // Không lưu người dùng
    }
}