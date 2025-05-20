package com.electro.controller.client;

import com.electro.dto.authentication.UserResponse;
import com.electro.dto.client.ClientPhoneSettingUserRequest;
import com.electro.entity.authentication.User;
import com.electro.mapper.authentication.UserMapper;
import com.electro.repository.authentication.UserRepository;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class cho tính năng cập nhật số điện thoại của khách hàng
 * Sử dụng Mockito để giả lập các dependencies
 */
@ExtendWith(MockitoExtension.class)
public class ClientPhoneUpdateTest {

        // Mock các đối tượng cần thiết cho controller
        @Mock
        private UserRepository userRepository; // Repository truy vấn dữ liệu người dùng

        @Mock
        private UserMapper userMapper; // Mapper chuyển đổi giữa entity và DTO

        @Mock
        private Authentication authentication; // Object xác thực người dùng

        // Đối tượng controller cần test, được inject các mock ở trên
        @InjectMocks
        private ClientUserController clientUserController;

        // Dữ liệu test
        private User testUser; // Đối tượng người dùng mẫu
        private static final String TEST_USERNAME = "beotron01"; // Username mẫu

        /**
         * Thiết lập dữ liệu trước mỗi test case
         */
        @BeforeEach
        void setUp() {
                // Giả lập kết quả trả về của authentication
                when(authentication.getName()).thenReturn(TEST_USERNAME);

                // Khởi tạo người dùng mẫu
                testUser = new User();
                testUser.setId(1L);
                testUser.setUsername(TEST_USERNAME);
                testUser.setEmail("test@example.com");
                testUser.setPhone("1234567890"); // Số điện thoại ban đầu
                testUser.setFullname("Test User");
        }

        /**
         * Test case 1: Kiểm tra cập nhật số điện thoại với giá trị hợp lệ
         */
        @Test
        @DisplayName("1. Cập nhật số điện thoại với giá trị hợp lệ")
        void updatePhoneSetting_WithValidValue() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientPhoneSettingUserRequest request = new ClientPhoneSettingUserRequest();
                request.setPhone("0987654321"); // Số điện thoại mới hợp lệ

                // Tạo đối tượng User mô phỏng kết quả sau khi cập nhật
                User updatedUser = new User();
                updatedUser.setId(1L);
                updatedUser.setUsername(TEST_USERNAME);
                updatedUser.setEmail("test@example.com");
                updatedUser.setPhone("0987654321"); // Số điện thoại đã cập nhật
                updatedUser.setFullname("Test User");

                // Tạo đối tượng UserResponse mô phỏng kết quả trả về cho client
                UserResponse updatedResponse = new UserResponse();
                updatedResponse.setId(1L);
                updatedResponse.setUsername(TEST_USERNAME);
                updatedResponse.setEmail("test@example.com");
                updatedResponse.setPhone("0987654321"); // Số điện thoại đã cập nhật
                updatedResponse.setFullname("Test User");

                // Cấu hình các mock để trả về kết quả mong muốn
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPhoneSettingUserRequest.class)))
                                .thenReturn(updatedUser);
                when(userRepository.save(any(User.class))).thenReturn(updatedUser);
                when(userMapper.entityToResponse(any(User.class))).thenReturn(updatedResponse);

                // Act - Thực hiện phương thức cần test
                ResponseEntity<UserResponse> response = clientUserController.updatePhoneSetting(authentication,
                                request);

                // Assert - Kiểm tra kết quả
                assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
                assertNotNull(response.getBody()); // Kiểm tra response body không null
                assertEquals("0987654321", response.getBody().getPhone()); // Kiểm tra số điện thoại đã được cập nhật

                // Xác minh các phương thức mock đã được gọi đúng
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userMapper).partialUpdate(any(User.class), any(ClientPhoneSettingUserRequest.class));
                verify(userRepository).save(any(User.class));
        }

        /**
         * Test case 2: Kiểm tra cập nhật số điện thoại mà không nhập giá trị
         */
        @Test
        @DisplayName("2. Cập nhật số điện thoại không nhập giá trị")
        void updatePhoneSetting_WithNothing() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientPhoneSettingUserRequest request = new ClientPhoneSettingUserRequest();
                // Không thiết lập giá trị cho phone, mặc định sẽ là null -> không hợp lệ

                // Cấu hình mock để ném ngoại lệ khi cập nhật với số điện thoại null
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPhoneSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Số điện thoại không được để trống"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePhoneSetting(authentication, request));
                assertEquals("Số điện thoại không được để trống", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 3: Kiểm tra cập nhật số điện thoại chỉ chứa khoảng trắng
         */
        @Test
        @DisplayName("3. Cập nhật số điện thoại chỉ chứa khoảng trắng")
        void updatePhoneSetting_WithSpace() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientPhoneSettingUserRequest request = new ClientPhoneSettingUserRequest();
                request.setPhone("   "); // Số điện thoại chỉ chứa khoảng trắng -> không hợp lệ

                // Cấu hình mock để ném ngoại lệ khi cập nhật với số điện thoại không hợp lệ
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPhoneSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Số điện thoại không được để trống"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePhoneSetting(authentication, request));
                assertEquals("Số điện thoại không được để trống", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 4: Kiểm tra cập nhật số điện thoại với định dạng không hợp lệ (chứa
         * ký tự chữ)
         */
        @Test
        @DisplayName("4. Cập nhật số điện thoại với định dạng không hợp lệ")
        void updatePhoneSetting_WithInvalidFormat() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientPhoneSettingUserRequest request = new ClientPhoneSettingUserRequest();
                request.setPhone("098abc7654"); // Số điện thoại chứa ký tự chữ -> không hợp lệ

                // Cấu hình mock để ném ngoại lệ khi cập nhật với số điện thoại không hợp lệ
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPhoneSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Số điện thoại chỉ được chứa chữ số"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePhoneSetting(authentication, request));
                assertEquals("Số điện thoại chỉ được chứa chữ số", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 5: Kiểm tra cập nhật số điện thoại quá ngắn (ít hơn 10 chữ số)
         */
        @Test
        @DisplayName("5. Cập nhật số điện thoại quá ngắn")
        void updatePhoneSetting_WithTooShortNumber() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientPhoneSettingUserRequest request = new ClientPhoneSettingUserRequest();
                request.setPhone("12345"); // Số điện thoại quá ngắn (ít hơn 10 ký tự)

                // Cấu hình mock để ném ngoại lệ khi cập nhật với số điện thoại quá ngắn
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPhoneSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Số điện thoại phải có ít nhất 10 chữ số"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePhoneSetting(authentication, request));
                assertEquals("Số điện thoại phải có ít nhất 10 chữ số", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 6: Kiểm tra khi người dùng không tồn tại
         */
        @Test
        @DisplayName("6. Kiểm tra khi không có người dùng")
        void updatePhoneSetting_WhenUserNotFound() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientPhoneSettingUserRequest request = new ClientPhoneSettingUserRequest();
                request.setPhone("0987654321"); // Số điện thoại hợp lệ

                // Cấu hình mock để trả về Optional.empty() -> không tìm thấy người dùng
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                assertThrows(UsernameNotFoundException.class,
                                () -> clientUserController.updatePhoneSetting(authentication, request));

                // Xác minh các phương thức không được gọi khi không tìm thấy người dùng
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userMapper, never()).partialUpdate(any(User.class), any(ClientPhoneSettingUserRequest.class));
                verify(userRepository, never()).save(any());
        }
}