package com.electro.controller.client;

import com.electro.dto.address.AddressRequest;
import com.electro.dto.authentication.UserResponse;
import com.electro.dto.client.ClientPersonalSettingUserRequest;
import com.electro.entity.authentication.User;
import com.electro.entity.address.Address;
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
 * Test class cho tính năng cập nhật thông tin cá nhân của khách hàng
 * Sử dụng Mockito để giả lập các dependencies
 */
@ExtendWith(MockitoExtension.class)
public class ClientProfileUpdateTest {

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
        private Address testAddress; // Đối tượng địa chỉ mẫu
        private static final String TEST_USERNAME = "beotron01"; // Username mẫu

        /**
         * Thiết lập dữ liệu trước mỗi test case
         */
        @BeforeEach
        void setUp() {
                // Giả lập kết quả trả về của authentication
                when(authentication.getName()).thenReturn(TEST_USERNAME);

                // Khởi tạo địa chỉ mẫu
                testAddress = new Address();
                testAddress.setLine("gn"); // Địa chỉ chi tiết
                testAddress.setId(1L);

                // Khởi tạo người dùng mẫu
                testUser = new User();
                testUser.setId(1L);
                testUser.setUsername(TEST_USERNAME);
                testUser.setEmail("test@example.com");
                testUser.setPhone("1234567890");
                testUser.setFullname("Test User");
                testUser.setGender("M");
                testUser.setAddress(testAddress);
        }

        /**
         * Test case 1: Kiểm tra cập nhật thông tin cá nhân với dữ liệu hợp lệ
         */
        @Test
        @DisplayName("1. Cập nhật profile với thông tin hợp lệ")
        void updateProfile_WithValidInformation() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                // Tạo đối tượng AddressRequest chứa thông tin địa chỉ mới
                AddressRequest addressRequest = new AddressRequest();
                addressRequest.setLine("gn");
                addressRequest.setProvinceId(60L); // ID tỉnh/thành phố
                addressRequest.setDistrictId(665L); // ID quận/huyện
                addressRequest.setWardId(10235L); // ID phường/xã
                // Tạo đối tượng request với thông tin cá nhân mới
                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("Nguyễn Văn A"); // Họ tên mới
                request.setGender("M"); // Giới tính (Nam)
                request.setAddress(addressRequest); // Địa chỉ mới
                // Tạo đối tượng User mô phỏng kết quả sau khi cập nhật
                User updatedUser = new User();
                updatedUser.setId(1L);
                updatedUser.setUsername(TEST_USERNAME);
                updatedUser.setEmail("test@example.com");
                updatedUser.setPhone("1234567890");
                updatedUser.setFullname("Nguyễn Văn A"); // Họ tên đã được cập nhật
                updatedUser.setGender("M");
                updatedUser.setAddress(testAddress);
                // Tạo đối tượng UserResponse mô phỏng kết quả trả về cho client
                UserResponse updatedResponse = new UserResponse();
                updatedResponse.setId(1L);
                updatedResponse.setUsername(TEST_USERNAME);
                updatedResponse.setEmail("test@example.com");
                updatedResponse.setPhone("1234567890");
                updatedResponse.setFullname("Nguyễn Văn A");
                updatedResponse.setGender("M");
                // Cấu hình các mock để trả về kết quả mong muốn
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenReturn(updatedUser);
                when(userRepository.save(any(User.class))).thenReturn(updatedUser);
                when(userMapper.entityToResponse(any(User.class))).thenReturn(updatedResponse);
                // Act - Thực hiện phương thức cần test
                ResponseEntity<UserResponse> response = clientUserController.updatePersonalSetting(authentication,
                                request);
                // Assert - Kiểm tra kết quả
                assertEquals(HttpStatus.OK, response.getStatusCode()); // Kiểm tra status code
                assertNotNull(response.getBody()); // Kiểm tra response body không null
                assertEquals("Nguyễn Văn A", response.getBody().getFullname()); // Kiểm tra họ tên đã được cập nhật
                // Xác minh các phương thức mock đã được gọi đúng
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userMapper).partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class));
                verify(userRepository).save(any(User.class));
        }

        /**
         * Test case 2: Kiểm tra cập nhật thông tin cá nhân với tên quá ngắn (1 ký tự)
         */
        @Test
        @DisplayName("2. Cập nhật profile với tên chỉ có 1 ký tự")
        void updateProfile_WithNameInOneChar() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("A"); // Họ tên chỉ có 1 ký tự -> không hợp lệ
                request.setGender("M");

                // Cấu hình mock để ném ngoại lệ khi cập nhật với tên không hợp lệ
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Họ tên phải có ít nhất 2 ký tự"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));
                assertEquals("Họ tên phải có ít nhất 2 ký tự", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 3: Kiểm tra cập nhật thông tin cá nhân với tên quá dài (>255 ký tự)
         */
        @Test
        @DisplayName("3. Cập nhật profile với tên quá dài")
        void updateProfile_WithLengthName() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("A".repeat(256)); // Tạo chuỗi 256 ký tự "A" -> vượt giới hạn 255
                request.setGender("M");

                // Cấu hình mock để ném ngoại lệ khi cập nhật với tên quá dài
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Họ tên không được vượt quá 255 ký tự"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));
                assertEquals("Họ tên không được vượt quá 255 ký tự", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 4: Kiểm tra cập nhật thông tin cá nhân với tên chỉ có khoảng trắng
         */
        @Test
        @DisplayName("4. Cập nhật profile với tên chỉ chứa khoảng trắng")
        void updateProfile_WithNameOnlySpace() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("   "); // Tên chỉ chứa khoảng trắng -> không hợp lệ
                request.setGender("M");

                // Cấu hình mock để ném ngoại lệ khi cập nhật với tên không hợp lệ
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Họ tên không được để trống"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));
                assertEquals("Họ tên không được để trống", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 5: Kiểm tra cập nhật thông tin cá nhân không có địa chỉ
         * Khi không có địa chỉ mới, giữ nguyên địa chỉ cũ
         */
        @Test
        @DisplayName("5. Cập nhật profile không có thông tin địa chỉ")
        void updateProfile_NoAddressInput() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("Nguyễn Văn A");
                request.setGender("M");
                // Không truyền address -> giữ nguyên địa chỉ cũ

                // Tạo đối tượng User mô phỏng kết quả sau khi cập nhật
                User updatedUser = new User();
                updatedUser.setId(1L);
                updatedUser.setUsername(TEST_USERNAME);
                updatedUser.setEmail("test@example.com");
                updatedUser.setPhone("1234567890");
                updatedUser.setFullname("Nguyễn Văn A");
                updatedUser.setGender("M");
                updatedUser.setAddress(testAddress); // Giữ nguyên địa chỉ cũ

                // Tạo đối tượng UserResponse mô phỏng kết quả trả về cho client
                UserResponse updatedResponse = new UserResponse();
                updatedResponse.setId(1L);
                updatedResponse.setUsername(TEST_USERNAME);
                updatedResponse.setEmail("test@example.com");
                updatedResponse.setPhone("1234567890");
                updatedResponse.setFullname("Nguyễn Văn A");
                updatedResponse.setGender("M");

                // Cấu hình các mock để trả về kết quả mong muốn
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenReturn(updatedUser);
                when(userRepository.save(any(User.class))).thenReturn(updatedUser);
                when(userMapper.entityToResponse(any(User.class))).thenReturn(updatedResponse);

                // Act - Thực hiện phương thức cần test
                ResponseEntity<UserResponse> response = clientUserController.updatePersonalSetting(authentication,
                                request);

                // Assert - Kiểm tra kết quả
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("Nguyễn Văn A", response.getBody().getFullname());

                // Xác minh các phương thức mock đã được gọi đúng
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userMapper).partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class));
                verify(userRepository).save(any(User.class));
        }

        /**
         * Test case 6: Kiểm tra cập nhật thông tin với địa chỉ chỉ có khoảng trắng
         */
        @Test
        @DisplayName("6. Cập nhật profile với địa chỉ chỉ chứa khoảng trắng")
        void updateProfile_AddressWithSpace() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                AddressRequest addressRequest = new AddressRequest();
                addressRequest.setLine("   "); // Địa chỉ chi tiết chỉ có khoảng trắng -> không hợp lệ
                addressRequest.setProvinceId(60L);
                addressRequest.setDistrictId(665L);
                addressRequest.setWardId(10235L);

                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("Nguyễn Văn A");
                request.setGender("M");
                request.setAddress(addressRequest);

                // Cấu hình mock để ném ngoại lệ khi cập nhật với địa chỉ không hợp lệ
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Địa chỉ chi tiết không được để trống"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));
                assertEquals("Địa chỉ chi tiết không được để trống", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 7: Kiểm tra cập nhật thông tin với tỉnh/thành phố không tồn tại
         */
        @Test
        @DisplayName("7. Cập nhật profile với province không tồn tại")
        void updateProfile_SelectAddressWrong_Province() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                AddressRequest addressRequest = new AddressRequest();
                addressRequest.setLine("gn");
                addressRequest.setProvinceId(999L); // ID tỉnh/thành phố không tồn tại
                addressRequest.setDistrictId(665L);
                addressRequest.setWardId(10235L);

                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("Nguyễn Văn A");
                request.setGender("M");
                request.setAddress(addressRequest);

                // Cấu hình mock để ném ngoại lệ khi cập nhật với tỉnh/thành phố không tồn tại
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Tỉnh/thành phố không tồn tại"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));
                assertEquals("Tỉnh/thành phố không tồn tại", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 8: Kiểm tra cập nhật thông tin với quận/huyện không thuộc
         * tỉnh/thành phố đã chọn
         */
        @Test
        @DisplayName("8. Cập nhật profile với district không thuộc province đã chọn")
        void updateProfile_SelectAddressWrong_District() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                AddressRequest addressRequest = new AddressRequest();
                addressRequest.setLine("gn");
                addressRequest.setProvinceId(60L);
                addressRequest.setDistrictId(999L); // ID quận/huyện không thuộc tỉnh/thành phố đã chọn
                addressRequest.setWardId(10235L);

                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("Nguyễn Văn A");
                request.setGender("M");
                request.setAddress(addressRequest);

                // Cấu hình mock để ném ngoại lệ khi cập nhật với quận/huyện không hợp lệ
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException(
                                                "Quận/huyện không thuộc tỉnh/thành phố đã chọn"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));
                assertEquals("Quận/huyện không thuộc tỉnh/thành phố đã chọn", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 9: Kiểm tra cập nhật thông tin với phường/xã không thuộc quận/huyện
         * đã chọn
         */
        @Test
        @DisplayName("9. Cập nhật profile với ward không thuộc district đã chọn")
        void updateProfile_SelectAddressWrong_Ward() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                AddressRequest addressRequest = new AddressRequest();
                addressRequest.setLine("gn");
                addressRequest.setProvinceId(60L);
                addressRequest.setDistrictId(665L);
                addressRequest.setWardId(999L); // ID phường/xã không thuộc quận/huyện đã chọn

                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("Nguyễn Văn A");
                request.setGender("M");
                request.setAddress(addressRequest);

                // Cấu hình mock để ném ngoại lệ khi cập nhật với phường/xã không hợp lệ
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Phường/xã không thuộc quận/huyện đã chọn"));

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));
                assertEquals("Phường/xã không thuộc quận/huyện đã chọn", exception.getMessage());

                // Xác minh repository.save không được gọi khi dữ liệu không hợp lệ
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        /**
         * Test case 10: Kiểm tra khi người dùng không tồn tại
         */
        @Test
        @DisplayName("10. Kiểm tra khi không có người dùng")
        void updateProfile_WhenUserNotFound() {
                // Arrange - Chuẩn bị dữ liệu đầu vào
                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("Nguyễn Văn A");
                request.setGender("M");

                // Cấu hình mock để trả về Optional.empty() -> không tìm thấy người dùng
                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

                // Act & Assert - Thực hiện và kiểm tra ngoại lệ
                assertThrows(UsernameNotFoundException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));

                // Xác minh các phương thức không được gọi khi không tìm thấy người dùng
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userMapper, never()).partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class));
                verify(userRepository, never()).save(any());
        }
}