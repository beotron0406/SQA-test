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

@ExtendWith(MockitoExtension.class)
public class ClientProfileUpdateTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private UserMapper userMapper;

        @Mock
        private Authentication authentication;

        @InjectMocks
        private ClientUserController clientUserController;

        private User testUser;
        private Address testAddress;
        private static final String TEST_USERNAME = "beotron01";

        @BeforeEach
        void setUp() {
                // Mock authentication
                when(authentication.getName()).thenReturn(TEST_USERNAME);

                // Setup test address
                testAddress = new Address();
                testAddress.setLine("gn");
                testAddress.setId(1L);

                // Setup test user
                testUser = new User();
                testUser.setId(1L);
                testUser.setUsername(TEST_USERNAME);
                testUser.setEmail("test@example.com");
                testUser.setPhone("1234567890");
                testUser.setFullname("Test User");
                testUser.setGender("M");
                testUser.setAddress(testAddress);
        }

        @Test
        @DisplayName("1. Cập nhật profile với thông tin hợp lệ")
        void updateProfile_WithValidInformation() {
                // Arrange
                AddressRequest addressRequest = new AddressRequest();
                addressRequest.setLine("gn");
                addressRequest.setProvinceId(60L);
                addressRequest.setDistrictId(665L);
                addressRequest.setWardId(10235L);

                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("Nguyễn Văn A");
                request.setGender("M");
                request.setAddress(addressRequest);

                User updatedUser = new User();
                updatedUser.setId(1L);
                updatedUser.setUsername(TEST_USERNAME);
                updatedUser.setEmail("test@example.com");
                updatedUser.setPhone("1234567890");
                updatedUser.setFullname("Nguyễn Văn A");
                updatedUser.setGender("M");
                updatedUser.setAddress(testAddress);

                UserResponse updatedResponse = new UserResponse();
                updatedResponse.setId(1L);
                updatedResponse.setUsername(TEST_USERNAME);
                updatedResponse.setEmail("test@example.com");
                updatedResponse.setPhone("1234567890");
                updatedResponse.setFullname("Nguyễn Văn A");
                updatedResponse.setGender("M");

                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenReturn(updatedUser);
                when(userRepository.save(any(User.class))).thenReturn(updatedUser);
                when(userMapper.entityToResponse(any(User.class))).thenReturn(updatedResponse);

                // Act
                ResponseEntity<UserResponse> response = clientUserController.updatePersonalSetting(authentication,
                                request);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("Nguyễn Văn A", response.getBody().getFullname());
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userMapper).partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class));
                verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("2. Cập nhật profile với tên chỉ có 1 ký tự")
        void updateProfile_WithNameInOneChar() {
                // Arrange
                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("A");
                request.setGender("M");

                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Họ tên phải có ít nhất 2 ký tự"));

                // Act & Assert
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));
                assertEquals("Họ tên phải có ít nhất 2 ký tự", exception.getMessage());
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("3. Cập nhật profile với tên quá dài")
        void updateProfile_WithLengthName() {
                // Arrange
                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("A".repeat(256)); // Tạo chuỗi 256 ký tự "A"
                request.setGender("M");

                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Họ tên không được vượt quá 255 ký tự"));

                // Act & Assert
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));
                assertEquals("Họ tên không được vượt quá 255 ký tự", exception.getMessage());
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("4. Cập nhật profile với tên chỉ chứa khoảng trắng")
        void updateProfile_WithNameOnlySpace() {
                // Arrange
                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("   ");
                request.setGender("M");

                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Họ tên không được để trống"));

                // Act & Assert
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));
                assertEquals("Họ tên không được để trống", exception.getMessage());
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("5. Cập nhật profile không có thông tin địa chỉ")
        void updateProfile_NoAddressInput() {
                // Arrange
                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("Nguyễn Văn A");
                request.setGender("M");
                // Không truyền address

                User updatedUser = new User();
                updatedUser.setId(1L);
                updatedUser.setUsername(TEST_USERNAME);
                updatedUser.setEmail("test@example.com");
                updatedUser.setPhone("1234567890");
                updatedUser.setFullname("Nguyễn Văn A");
                updatedUser.setGender("M");
                updatedUser.setAddress(testAddress); // Giữ nguyên địa chỉ cũ

                UserResponse updatedResponse = new UserResponse();
                updatedResponse.setId(1L);
                updatedResponse.setUsername(TEST_USERNAME);
                updatedResponse.setEmail("test@example.com");
                updatedResponse.setPhone("1234567890");
                updatedResponse.setFullname("Nguyễn Văn A");
                updatedResponse.setGender("M");

                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenReturn(updatedUser);
                when(userRepository.save(any(User.class))).thenReturn(updatedUser);
                when(userMapper.entityToResponse(any(User.class))).thenReturn(updatedResponse);

                // Act
                ResponseEntity<UserResponse> response = clientUserController.updatePersonalSetting(authentication,
                                request);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("Nguyễn Văn A", response.getBody().getFullname());
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userMapper).partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class));
                verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("6. Cập nhật profile với địa chỉ chỉ chứa khoảng trắng")
        void updateProfile_AddressWithSpace() {
                // Arrange
                AddressRequest addressRequest = new AddressRequest();
                addressRequest.setLine("   ");
                addressRequest.setProvinceId(60L);
                addressRequest.setDistrictId(665L);
                addressRequest.setWardId(10235L);

                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("Nguyễn Văn A");
                request.setGender("M");
                request.setAddress(addressRequest);

                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Địa chỉ chi tiết không được để trống"));

                // Act & Assert
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));
                assertEquals("Địa chỉ chi tiết không được để trống", exception.getMessage());
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("7. Cập nhật profile với province không tồn tại")
        void updateProfile_SelectAddressWrong_Province() {
                // Arrange
                AddressRequest addressRequest = new AddressRequest();
                addressRequest.setLine("gn");
                addressRequest.setProvinceId(999L);
                addressRequest.setDistrictId(665L);
                addressRequest.setWardId(10235L);

                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("Nguyễn Văn A");
                request.setGender("M");
                request.setAddress(addressRequest);

                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Tỉnh/thành phố không tồn tại"));

                // Act & Assert
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));
                assertEquals("Tỉnh/thành phố không tồn tại", exception.getMessage());
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("8. Cập nhật profile với district không thuộc province đã chọn")
        void updateProfile_SelectAddressWrong_District() {
                // Arrange
                AddressRequest addressRequest = new AddressRequest();
                addressRequest.setLine("gn");
                addressRequest.setProvinceId(60L);
                addressRequest.setDistrictId(999L); // District không thuộc province đã chọn
                addressRequest.setWardId(10235L);

                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("Nguyễn Văn A");
                request.setGender("M");
                request.setAddress(addressRequest);

                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException(
                                                "Quận/huyện không thuộc tỉnh/thành phố đã chọn"));

                // Act & Assert
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));
                assertEquals("Quận/huyện không thuộc tỉnh/thành phố đã chọn", exception.getMessage());
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("9. Cập nhật profile với ward không thuộc district đã chọn")
        void updateProfile_SelectAddressWrong_Ward() {
                // Arrange
                AddressRequest addressRequest = new AddressRequest();
                addressRequest.setLine("gn");
                addressRequest.setProvinceId(60L);
                addressRequest.setDistrictId(665L);
                addressRequest.setWardId(999L); // Ward không thuộc district đã chọn

                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("Nguyễn Văn A");
                request.setGender("M");
                request.setAddress(addressRequest);

                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
                when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                                .thenThrow(new IllegalArgumentException("Phường/xã không thuộc quận/huyện đã chọn"));

                // Act & Assert
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));
                assertEquals("Phường/xã không thuộc quận/huyện đã chọn", exception.getMessage());
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("10. Kiểm tra khi không có người dùng")
        void updateProfile_WhenUserNotFound() {
                // Arrange
                ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
                request.setUsername(TEST_USERNAME);
                request.setFullname("Nguyễn Văn A");
                request.setGender("M");

                when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

                // Act & Assert
                assertThrows(UsernameNotFoundException.class,
                                () -> clientUserController.updatePersonalSetting(authentication, request));
                verify(userRepository).findByUsername(TEST_USERNAME);
                verify(userMapper, never()).partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class));
                verify(userRepository, never()).save(any());
        }
}