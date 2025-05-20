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

@ExtendWith(MockitoExtension.class)
public class ClientPasswordUpdateTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ClientUserController clientUserController;

    private User testUser;
    private static final String TEST_USERNAME = "beotron01";
    private static final String ENCODED_OLD_PASSWORD = "encodedOldPassword";
    private static final String ENCODED_NEW_PASSWORD = "encodedNewPassword";

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
        testUser.setPassword(ENCODED_OLD_PASSWORD);
    }

    @Test
    @DisplayName("1. Đổi mật khẩu với giá trị hợp lệ")
    void updatePasswordSetting_WithValidValue() throws Exception {
        // Arrange
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("validNewPassword123@");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", ENCODED_OLD_PASSWORD)).thenReturn(true);
        when(passwordEncoder.encode("validNewPassword123@")).thenReturn(ENCODED_NEW_PASSWORD);

        // Act
        ResponseEntity<ObjectNode> response = clientUserController.updatePasswordSetting(authentication, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ENCODED_NEW_PASSWORD, testUser.getPassword());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches("oldPassword", ENCODED_OLD_PASSWORD);
        verify(passwordEncoder).encode("validNewPassword123@");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("2. Đổi mật khẩu với mật khẩu mới giống mật khẩu cũ")
    void updatePasswordSetting_NewPasswordSameAsOldPassword() {
        // Arrange
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("oldPassword");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", ENCODED_OLD_PASSWORD)).thenReturn(true);

        // Simulate throwing exception for same password
        doAnswer(invocation -> {
            throw new Exception("Mật khẩu mới không được giống mật khẩu cũ");
        }).when(passwordEncoder).encode("oldPassword");

        // Act & Assert
        Exception exception = assertThrows(Exception.class,
                () -> clientUserController.updatePasswordSetting(authentication, request));
        assertEquals("Mật khẩu mới không được giống mật khẩu cũ", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches("oldPassword", ENCODED_OLD_PASSWORD);
        verify(passwordEncoder).encode("oldPassword");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Đổi mật khẩu với mật khẩu mới chứa ký tự đặc biệt")
    void updatePasswordSetting_WithSpecialCharacter() throws Exception {
        // Arrange
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword!@#$%");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", ENCODED_OLD_PASSWORD)).thenReturn(true);
        when(passwordEncoder.encode("newPassword!@#$%")).thenReturn(ENCODED_NEW_PASSWORD);

        // Act
        ResponseEntity<ObjectNode> response = clientUserController.updatePasswordSetting(authentication, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ENCODED_NEW_PASSWORD, testUser.getPassword());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches("oldPassword", ENCODED_OLD_PASSWORD);
        verify(passwordEncoder).encode("newPassword!@#$%");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("4. Đổi mật khẩu với mật khẩu mới chỉ có 1 ký tự")
    void updatePasswordSetting_WithOneCharacter() {
        // Arrange
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("a");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", ENCODED_OLD_PASSWORD)).thenReturn(true);

        // Simulate throwing exception for short password
        doAnswer(invocation -> {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự");
        }).when(passwordEncoder).encode("a");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> clientUserController.updatePasswordSetting(authentication, request));
        assertEquals("Mật khẩu phải có ít nhất 8 ký tự", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches("oldPassword", ENCODED_OLD_PASSWORD);
        verify(passwordEncoder).encode("a");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. Đổi mật khẩu với mật khẩu cũ không chính xác")
    void updatePasswordSetting_WithIncorrectOldPassword() {
        // Arrange
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("wrongOldPassword");
        request.setNewPassword("newPassword123");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOldPassword", ENCODED_OLD_PASSWORD)).thenReturn(false);

        // Act & Assert
        Exception exception = assertThrows(Exception.class,
                () -> clientUserController.updatePasswordSetting(authentication, request));
        assertEquals("Wrong old password", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches("wrongOldPassword", ENCODED_OLD_PASSWORD);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. Đổi mật khẩu với mật khẩu mới thiếu độ phức tạp")
    void updatePasswordSetting_WithWeakPassword() {
        // Arrange
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("simple"); // Thiếu độ phức tạp

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", ENCODED_OLD_PASSWORD)).thenReturn(true);

        // Simulate throwing exception for weak password
        doAnswer(invocation -> {
            throw new IllegalArgumentException("Mật khẩu phải bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt");
        }).when(passwordEncoder).encode("simple");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> clientUserController.updatePasswordSetting(authentication, request));
        assertEquals("Mật khẩu phải bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches("oldPassword", ENCODED_OLD_PASSWORD);
        verify(passwordEncoder).encode("simple");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Kiểm tra khi không có người dùng")
    void updatePasswordSetting_WhenUserNotFound() {
        // Arrange
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword123");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> clientUserController.updatePasswordSetting(authentication, request));
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }
}