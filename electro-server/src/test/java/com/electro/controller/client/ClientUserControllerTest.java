package com.electro.controller.client;

import com.electro.dto.authentication.UserResponse;
import com.electro.dto.client.ClientEmailSettingUserRequest;
import com.electro.dto.client.ClientPasswordSettingUserRequest;
import com.electro.dto.client.ClientPersonalSettingUserRequest;
import com.electro.dto.client.ClientPhoneSettingUserRequest;
import com.electro.entity.authentication.User;
import com.electro.mapper.authentication.UserMapper;
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
public class ClientUserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ClientUserController clientUserController;

    private User testUser;
    private UserResponse testUserResponse;
    private static final String TEST_USERNAME = "testuser";

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
        testUser.setPassword("encodedPassword");

        // Setup test user response
        testUserResponse = new UserResponse();
        testUserResponse.setId(1L);
        testUserResponse.setUsername(TEST_USERNAME);
        testUserResponse.setEmail("test@example.com");
        testUserResponse.setPhone("1234567890");
        testUserResponse.setFullname("Test User");
    }

    @Test
    @DisplayName("Should return user info when authenticated")
    void getUserInfo_ShouldReturnUserInfo_WhenAuthenticated() {
        // Arrange
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(userMapper.entityToResponse(any(User.class))).thenReturn(testUserResponse);

        // Act
        ResponseEntity<UserResponse> response = clientUserController.getUserInfo(authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testUserResponse, response.getBody());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userMapper).entityToResponse(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void getUserInfo_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> clientUserController.getUserInfo(authentication));
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userMapper, never()).entityToResponse(any(User.class));
    }

    @Test
    @DisplayName("Should update personal settings when valid request")
    void updatePersonalSetting_ShouldUpdateUser_WhenValidRequest() {
        // Arrange
        ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
        request.setFullname("Updated Test User");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername(TEST_USERNAME);
        updatedUser.setEmail("test@example.com");
        updatedUser.setPhone("1234567890");
        updatedUser.setFullname("Updated Test User");

        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId(1L);
        updatedResponse.setUsername(TEST_USERNAME);
        updatedResponse.setEmail("test@example.com");
        updatedResponse.setPhone("1234567890");
        updatedResponse.setFullname("Updated Test User");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(userMapper.partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class)))
                .thenReturn(updatedUser);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.entityToResponse(any(User.class))).thenReturn(updatedResponse);

        // Act
        ResponseEntity<UserResponse> response = clientUserController.updatePersonalSetting(authentication, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedResponse, response.getBody());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userMapper).partialUpdate(any(User.class), any(ClientPersonalSettingUserRequest.class));
        verify(userRepository).save(any(User.class));
        verify(userMapper).entityToResponse(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found for personal update")
    void updatePersonalSetting_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        ClientPersonalSettingUserRequest request = new ClientPersonalSettingUserRequest();
        request.setFullname("Updated Test User");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> clientUserController.updatePersonalSetting(authentication, request));
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userMapper, never()).partialUpdate(any(), any(ClientPersonalSettingUserRequest.class));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update phone settings when valid request")
    void updatePhoneSetting_ShouldUpdateUser_WhenValidRequest() {
        // Arrange
        ClientPhoneSettingUserRequest request = new ClientPhoneSettingUserRequest();
        request.setPhone("9876543210");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername(TEST_USERNAME);
        updatedUser.setEmail("test@example.com");
        updatedUser.setPhone("9876543210");
        updatedUser.setFullname("Test User");

        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId(1L);
        updatedResponse.setUsername(TEST_USERNAME);
        updatedResponse.setEmail("test@example.com");
        updatedResponse.setPhone("9876543210");
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
        assertEquals(updatedResponse, response.getBody());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userMapper).partialUpdate(any(User.class), any(ClientPhoneSettingUserRequest.class));
        verify(userRepository).save(any(User.class));
        verify(userMapper).entityToResponse(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found for phone update")
    void updatePhoneSetting_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        ClientPhoneSettingUserRequest request = new ClientPhoneSettingUserRequest();
        request.setPhone("9876543210");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> clientUserController.updatePhoneSetting(authentication, request));
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userMapper, never()).partialUpdate(any(), any(ClientPhoneSettingUserRequest.class));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update email settings when valid request")
    void updateEmailSetting_ShouldUpdateUser_WhenValidRequest() {
        // Arrange
        ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
        request.setEmail("updated@example.com");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername(TEST_USERNAME);
        updatedUser.setEmail("updated@example.com");
        updatedUser.setPhone("1234567890");
        updatedUser.setFullname("Test User");

        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId(1L);
        updatedResponse.setUsername(TEST_USERNAME);
        updatedResponse.setEmail("updated@example.com");
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
        assertEquals(updatedResponse, response.getBody());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userMapper).partialUpdate(any(User.class), any(ClientEmailSettingUserRequest.class));
        verify(userRepository).save(any(User.class));
        verify(userMapper).entityToResponse(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found for email update")
    void updateEmailSetting_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        ClientEmailSettingUserRequest request = new ClientEmailSettingUserRequest();
        request.setEmail("updated@example.com");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> clientUserController.updateEmailSetting(authentication, request));
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userMapper, never()).partialUpdate(any(), any(ClientEmailSettingUserRequest.class));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update password when old password is correct")
    void updatePasswordSetting_ShouldUpdatePassword_WhenOldPasswordIsCorrect() throws Exception {
        // Arrange
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        // Act
        ResponseEntity<ObjectNode> response = clientUserController.updatePasswordSetting(authentication, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches("oldPassword", "encodedPassword");
        verify(passwordEncoder).encode("newPassword");

        // Verify the user's password was updated and saved
        verify(userRepository).save(testUser);
        assertEquals("encodedNewPassword", testUser.getPassword());
    }

    @Test
    @DisplayName("Should throw exception when old password is incorrect")
    void updatePasswordSetting_ShouldThrowException_WhenOldPasswordIsIncorrect() {
        // Arrange
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("wrongPassword");
        request.setNewPassword("newPassword");

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // Act & Assert
        Exception exception = assertThrows(Exception.class,
                () -> clientUserController.updatePasswordSetting(authentication, request));
        assertEquals("Wrong old password", exception.getMessage());

        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches("wrongPassword", "encodedPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user not found for password update")
    void updatePasswordSetting_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        ClientPasswordSettingUserRequest request = new ClientPasswordSettingUserRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword");

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