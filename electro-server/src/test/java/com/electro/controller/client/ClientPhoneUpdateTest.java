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

@ExtendWith(MockitoExtension.class)
public class ClientPhoneUpdateTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ClientUserController clientUserController;

    private User testUser;
    private static final String TEST_USERNAME = "beotron01";

    @BeforeEach
    void setUp() {
        // Mock authentication
        when(authentication.getName()).thenReturn(TEST_USERNAME);

        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(TEST_USERNAME);
        testUser.setEmail("test@example.com");
        testUser.setPhone("1234567890");
        testUser.setFullname("Test User");
    }

    @Test
    @DisplayName("1. Cập nhật số điện thoại với giá trị hợp lệ")
    void updatePhoneSetting_WithValidValue() {
        // Arrange
        ClientPhoneSettingUserRequest request = new ClientPhoneSettingUserRequest();
        request.setPhone("0987654321");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername(TEST_USERNAME);
        updatedUser.setEmail("test@example.com");
        updatedUser.setPhone("0987654321"); // Số điện thoại đã cập nhật
        updatedUser.setFullname("Test User");

        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId(1L);
        updatedResponse.setUsername(TEST_USERNAME);
        updatedResponse.setEmail("test@example.com");
        updatedResponse.setPhone("0987654321"); // Số điện thoại đã cập nhật
        updatedResponse.setFullname("Test User");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(userMapper.partialUpdate(any(User.class), any(ClientPhoneSettingUserRequest.class)))
                .thenReturn(updatedUser);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.entityToResponse(any(User.class))).thenReturn(updatedResponse);

        // Act
        ResponseEntity<UserResponse> response = clientUserController.updatePhoneSetting(authentication, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("0987654321", response.getBody().getPhone());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userMapper).partialUpdate(any(User.class), any(ClientPhoneSettingUserRequest.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("2. Cập nhật số điện thoại không nhập giá trị")
    void updatePhoneSetting_WithNothing() {
        // Arrange
        ClientPhoneSettingUserRequest request = new ClientPhoneSettingUserRequest();
        // Không thiết lập giá trị cho phone, mặc định sẽ là null

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(userMapper.partialUpdate(any(User.class), any(ClientPhoneSettingUserRequest.class)))
                .thenThrow(new IllegalArgumentException("Số điện thoại không được để trống"));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> clientUserController.updatePhoneSetting(authentication, request));
        assertEquals("Số điện thoại không được để trống", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Cập nhật số điện thoại chỉ chứa khoảng trắng")
    void updatePhoneSetting_WithSpace() {
        // Arrange
        ClientPhoneSettingUserRequest request = new ClientPhoneSettingUserRequest();
        request.setPhone("   ");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(userMapper.partialUpdate(any(User.class), any(ClientPhoneSettingUserRequest.class)))
                .thenThrow(new IllegalArgumentException("Số điện thoại không được để trống"));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> clientUserController.updatePhoneSetting(authentication, request));
        assertEquals("Số điện thoại không được để trống", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("4. Cập nhật số điện thoại với định dạng không hợp lệ")
    void updatePhoneSetting_WithInvalidFormat() {
        // Arrange
        ClientPhoneSettingUserRequest request = new ClientPhoneSettingUserRequest();
        request.setPhone("098abc7654");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(userMapper.partialUpdate(any(User.class), any(ClientPhoneSettingUserRequest.class)))
                .thenThrow(new IllegalArgumentException("Số điện thoại chỉ được chứa chữ số"));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> clientUserController.updatePhoneSetting(authentication, request));
        assertEquals("Số điện thoại chỉ được chứa chữ số", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. Cập nhật số điện thoại quá ngắn")
    void updatePhoneSetting_WithTooShortNumber() {
        // Arrange
        ClientPhoneSettingUserRequest request = new ClientPhoneSettingUserRequest();
        request.setPhone("12345"); // Quá ngắn

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(userMapper.partialUpdate(any(User.class), any(ClientPhoneSettingUserRequest.class)))
                .thenThrow(new IllegalArgumentException("Số điện thoại phải có ít nhất 10 chữ số"));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> clientUserController.updatePhoneSetting(authentication, request));
        assertEquals("Số điện thoại phải có ít nhất 10 chữ số", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. Kiểm tra khi không có người dùng")
    void updatePhoneSetting_WhenUserNotFound() {
        // Arrange
        ClientPhoneSettingUserRequest request = new ClientPhoneSettingUserRequest();
        request.setPhone("0987654321");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> clientUserController.updatePhoneSetting(authentication, request));
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userMapper, never()).partialUpdate(any(User.class), any(ClientPhoneSettingUserRequest.class));
        verify(userRepository, never()).save(any());
    }
}