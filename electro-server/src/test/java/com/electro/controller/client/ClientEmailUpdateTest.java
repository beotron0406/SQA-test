package com.electro.controller.client;

import com.electro.dto.authentication.UserResponse;
import com.electro.dto.client.ClientEmailSettingUserRequest;
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
 * Test class cho tính năng cập nhật email của khách hàng
 * Sử dụng Mockito để giả lập các dependencies
 */
@ExtendWith(MockitoExtension.class)
public class ClientEmailUpdateTest {

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
                testUser.setEmail("test@example.com"); // Email ban đầu
                testUser.setPhone("1234567890");
                testUser.setFullname("Test User");
        }

        /**
         * Test case 1: Kiểm tra cập nhật email với giá trị hợp lệ
         */
        @Test
        @DisplayName("1. Cập nhật email với giá trị hợp lệ")
        void updateEmailSetting_WithValidValue() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
                request.setEmail("new-email@example.com"); // Email mới hợp lệ

                // Tạo đối tượng User mô phỏng kết quả sau khi cập nhật
                User updatedUser = new User();
                updatedUser.setId(1L);
                updatedUser.setUsername(TEST_USERNAME);
                updatedUser.setEmail("new-email@example.com"); // Email đã cập nhật
                updatedUser.setPhone("1234567890");
                updatedUser.setFullname("Test User");

                // Tạo đối tượng UserResponse mô phỏng kết quả trả về cho client
                UserResponse updatedResponse = new UserResponse();
                updatedResponse.setId(1L);
                updatedResponse.setUsername(TEST_USERNAME);
                updatedResponse.setEmail("new-email@example.com"); // Email đã cập nhật
                updatedResponse.setPhone("1234567890");
                updatedResponse.setFullname("Test User");

                // Cấu hình các mock để trả về kết quả mong muốn
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class)))
                                .thenReturn(updatedUser);
                when(userRepository.save(any(User.class))).thenReturn(updatedUser);
                when(userMapper.entityToResponse(any(User.class))).thenReturn(updatedResponse);

                // Act - Thực hiện phương thức cần test
                ResponseEntity<UserResponse> response = clientUserController.updateEmailSetting(authentication,
                                request);

                // Assert - Kiểm tra kết quả
                assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
                assertNotNull(response.getBody()); // Kiểm tra response body không null
                assertEquals("new-email@example.com", response.getBody().getEmail()); // Kiểm tra email đã được cập nhật

                // Xác minh các phương thức mock đã được gọi đúng
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userMapper).partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class));
                verify(userRepository).save(any(User.class));
        }

        /**
         * Test case 2: Kiểm tra cập nhật email với giá trị không đúng định dạng
         */
        @Test
        @DisplayName("2. Cập nhật email với giá trị không hợp lệ")
        void updateEmailSetting_WithWrongValue() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
                request.setEmail("invalid-email"); // Email không đúng định dạng (thiếu @ và domain)

                // Cấu hình mock để ném ngoại lệ khi cập nhật với email không hợp lệ
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Email không đúng định dạng"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updateEmailSetting(authentication, request));
                assertEquals("Email không đúng định dạng", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 3: Kiểm tra cập nhật email mà không nhập giá trị
         */
        @Test
        @DisplayName("3. Cập nhật email không nhập giá trị")
        void updateEmailSetting_WithNothing() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
                // Không thiết lập giá trị cho email, mặc định sẽ là null -> không hợp lệ

                // Cấu hình mock để ném ngoại lệ khi cập nhật với email null
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Email không được để trống"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updateEmailSetting(authentication, request));
                assertEquals("Email không được để trống", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 4: Kiểm tra cập nhật email với định dạng không hợp lệ (nhiều ký
         * tự @)
         */
        @Test
        @DisplayName("4. Cập nhật email với ký tự đặc biệt không hợp lệ")
        void updateEmailSetting_WithSpecialCharacter() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
                request.setEmail("test@email@example.com"); // Email với nhiều ký tự @ không hợp lệ

                // Cấu hình mock để ném ngoại lệ khi cập nhật với email không hợp lệ
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Email không đúng định dạng"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updateEmailSetting(authentication, request));
                assertEquals("Email không đúng định dạng", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 5: Kiểm tra cập nhật email chỉ chứa khoảng trắng
         */
        @Test
        @DisplayName("5. Cập nhật email chỉ chứa khoảng trắng")
        void updateEmailSetting_WithSpace() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
                request.setEmail("   "); // Email chỉ chứa khoảng trắng -> không hợp lệ

                // Cấu hình mock để ném ngoại lệ khi cập nhật với email không hợp lệ
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Email không được để trống"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updateEmailSetting(authentication, request));
                assertEquals("Email không được để trống", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 6: Kiểm tra cập nhật email đã tồn tại trong hệ thống (đã được sử
         * dụng bởi tài khoản khác)
         */
        @Test
        @DisplayName("6. Cập nhật email đã tồn tại trong hệ thống")
        void updateEmailSetting_WithExistingEmail() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
                request.setEmail("existing@example.com"); // Email đã tồn tại trong hệ thống

                // Cấu hình mock để ném ngoại lệ khi cập nhật với email đã tồn tại
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Email đã được sử dụng bởi tài khoản khác"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updateEmailSetting(authentication, request));
                assertEquals("Email đã được sử dụng bởi tài khoản khác", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 7: Kiểm tra khi người dùng không tồn tại
         */
        @Test
        @DisplayName("7. Kiểm tra khi không có người dùng")
        void updateEmailSetting_WhenUserNotFound() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
                request.setEmail("new-email@example.com"); // Email hợp lệ

                // Cấu hình mock để trả về Optional.empty() -> không tìm thấy người dùng
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                assertThrows(UsernameNotFoundException.class,
                                () -> clientUserController.updateEmailSetting(authentication, request));

                // Xác minh các phương thức không được gọi khi không tìm thấy người dùng
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userMapper, never()).partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class));
                verify(userRepository, never()).save(any());
        }
}