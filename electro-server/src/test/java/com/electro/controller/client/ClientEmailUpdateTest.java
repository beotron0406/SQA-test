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

@ExtendWith(MockitoExtension.class)
public class ClientEmailUpdateTest {

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
    @DisplayName("1. Cập nhật email với giá trị hợp lệ")
    void updateEmailSetting_WithValidValue() {
        // Arrange
        ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
        request.setEmail("new-email@example.com");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername(TEST_USERNAME);
        updatedUser.setEmail("new-email@example.com"); // Email đã cập nhật
        updatedUser.setPhone("1234567890");
        updatedUser.setFullname("Test User");

        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId(1L);
        updatedResponse.setUsername(TEST_USERNAME);
        updatedResponse.setEmail("new-email@example.com"); // Email đã cập nhật
        updatedResponse.setPhone("1234567890");
        updatedResponse.setFullname("Test User");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(userMapper.partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class)))
                .thenReturn(updatedUser);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.entityToResponse(any(User.class))).thenReturn(updatedResponse);

        // Act
        ResponseEntity<UserResponse> response = clientUserController.updateEmailSetting(authentication, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("new-email@example.com", response.getBody().getEmail());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userMapper).partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("2. Cập nhật email với giá trị không hợp lệ")
    void updateEmailSetting_WithWrongValue() {
        // Arrange
        ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
        request.setEmail("invalid-email"); // Email không hợp lệ

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(userMapper.partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class)))
                .thenThrow(new IllegalArgumentException("Email không đúng định dạng"));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> clientUserController.updateEmailSetting(authentication, request));
        assertEquals("Email không đúng định dạng", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Cập nhật email không nhập giá trị")
    void updateEmailSetting_WithNothing() {
        // Arrange
        ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
        // Không thiết lập giá trị cho email, mặc định sẽ là null

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(userMapper.partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class)))
                .thenThrow(new IllegalArgumentException("Email không được để trống"));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> clientUserController.updateEmailSetting(authentication, request));
        assertEquals("Email không được để trống", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("4. Cập nhật email với ký tự đặc biệt không hợp lệ")
    void updateEmailSetting_WithSpecialCharacter() {
        // Arrange
        ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
        request.setEmail("test@email@example.com"); // Email với nhiều ký tự @ không hợp lệ

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(userMapper.partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class)))
                .thenThrow(new IllegalArgumentException("Email không đúng định dạng"));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> clientUserController.updateEmailSetting(authentication, request));
        assertEquals("Email không đúng định dạng", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. Cập nhật email chỉ chứa khoảng trắng")
    void updateEmailSetting_WithSpace() {
        // Arrange
        ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
        request.setEmail("   ");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(userMapper.partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class)))
                .thenThrow(new IllegalArgumentException("Email không được để trống"));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> clientUserController.updateEmailSetting(authentication, request));
        assertEquals("Email không được để trống", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. Cập nhật email đã tồn tại trong hệ thống")
    void updateEmailSetting_WithExistingEmail() {
        // Arrange
        ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
        request.setEmail("existing@example.com");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(userMapper.partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class)))
                .thenThrow(new IllegalArgumentException("Email đã được sử dụng bởi tài khoản khác"));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> clientUserController.updateEmailSetting(authentication, request));
        assertEquals("Email đã được sử dụng bởi tài khoản khác", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Kiểm tra khi không có người dùng")
    void updateEmailSetting_WhenUserNotFound() {
        // Arrange
        ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
        request.setEmail("new-email@example.com");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> clientUserController.updateEmailSetting(authentication, request));
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userMapper, never()).partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class));
        verify(userRepository, never()).save(any());
    }
}