package com.electro.controller.authentication;

import com.electro.config.security.JwtUtils;
import com.electro.controller.authentication.AuthController;
import com.electro.dto.authentication.*;
import com.electro.entity.authentication.RefreshToken;
import com.electro.entity.authentication.User;
import com.electro.exception.RefreshTokenException;
import com.electro.exception.VerificationException;
import com.electro.mapper.authentication.UserMapper;
import com.electro.repository.authentication.UserRepository;
import com.electro.service.auth.VerificationService;
import com.electro.service.authetication.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.NestedServletException;
import com.electro.exception.GlobalExceptionHandler;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class, excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebSecurityConfigurer.class)
})
@WithMockUser
public class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private AuthenticationManager authenticationManager;

        @MockBean
        private VerificationService verificationService;

        @MockBean
        private RefreshTokenService refreshTokenService;

        @MockBean
        private JwtUtils jwtUtils;

        @MockBean
        private UserRepository userRepository;

        @MockBean
        private UserMapper userMapper;
        // Test objects
        private LoginRequest loginRequest;
        private UserRequest userRequest;
        private RegistrationRequest registrationRequest;
        private RefreshTokenRequest refreshTokenRequest;
        private ResetPasswordRequest resetPasswordRequest;
        private User user;
        private Authentication authentication;
        private RefreshToken refreshToken;

        @BeforeEach
        public void setup() {
                AuthController authController = new AuthController(
                                authenticationManager, verificationService, refreshTokenService,
                                jwtUtils, userRepository, userMapper);

                // Tạo instance của GlobalExceptionHandler
                GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

                // Cấu hình MockMvc với exception handler
                mockMvc = MockMvcBuilders
                                .standaloneSetup(authController)
                                .setControllerAdvice(exceptionHandler) // Thêm exception handler vào cấu hình
                                .build();
                // Setup login request
                loginRequest = new LoginRequest();
                loginRequest.setUsername("beotron000");
                loginRequest.setPassword("hieubeotron04062003");

                // Setup user request for registration
                userRequest = new UserRequest();
                userRequest.setUsername("beotron000");
                userRequest.setPassword("hieubeotron04062003");
                userRequest.setFullname("ng hieu");
                userRequest.setEmail("sunshine040603@gmail.com");
                userRequest.setPhone("0987654321");
                userRequest.setGender("M");

                // Setup registration confirmation request
                registrationRequest = new RegistrationRequest();
                registrationRequest.setUserId(1L);
                registrationRequest.setToken("1234");

                // Setup refresh token request
                refreshTokenRequest = new RefreshTokenRequest();
                refreshTokenRequest.setRefreshToken("refresh-token-123");

                // Setup reset password request
                resetPasswordRequest = new ResetPasswordRequest();
                resetPasswordRequest.setEmail("sunshine040603@gmail.com");
                resetPasswordRequest.setToken("reset-token-123");
                resetPasswordRequest.setPassword("newPassword123");

                // Setup mock user
                user = new User();
                user.setId(1L);
                user.setUsername("beotron000");
                user.setEmail("sunshine040603@gmail.com");
                user.setFullname("ng hieu");

                // Setup mock authentication
                authentication = mock(Authentication.class);
                when(authentication.getName()).thenReturn("beotron000");

                // Setup mock refresh token
                refreshToken = new RefreshToken();
                refreshToken.setToken("refresh-token-123");
                refreshToken.setUser(user);
                refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));
        }

        @Test
        @DisplayName("Login - Success")
        void testLogin_Success() throws Exception {
                // Arrange
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(authentication);
                when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token-123");
                when(refreshTokenService.createRefreshToken(authentication)).thenReturn(refreshToken);

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Login success!"))
                                .andExpect(jsonPath("$.accessToken").value("jwt-token-123"))
                                .andExpect(jsonPath("$.refreshToken").value("refresh-token-123"));

                verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
                verify(jwtUtils).generateJwtToken(authentication);
                verify(refreshTokenService).createRefreshToken(authentication);
        }

        @Test
        @DisplayName("Login - Invalid Credentials")
        void testLogin_InvalidCredentials() throws Exception {
                // Arrange
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new BadCredentialsException("Bad credentials"));

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isUnauthorized());

                verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
                verify(jwtUtils, never()).generateJwtToken(any());
                verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("Refresh Token - Success")
        void testRefreshToken_Success() throws Exception {
                // Arrange
                when(refreshTokenService.findByToken("refresh-token-123")).thenReturn(Optional.of(refreshToken));
                when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
                when(jwtUtils.generateTokenFromUsername("beotron000")).thenReturn("new-jwt-token-123");

                // Act & Assert
                mockMvc.perform(post("/api/auth/refresh-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Refresh token"))
                                .andExpect(jsonPath("$.accessToken").value("new-jwt-token-123"))
                                .andExpect(jsonPath("$.refreshToken").value("refresh-token-123"));

                verify(refreshTokenService).findByToken("refresh-token-123");
                verify(refreshTokenService).verifyExpiration(refreshToken);
                verify(jwtUtils).generateTokenFromUsername("beotron000");
        }

        @Test
        @DisplayName("Refresh Token - Expired")
        void testRefreshToken_Expired() throws Exception {
                // Arrange
                when(refreshTokenService.findByToken("refresh-token-123")).thenReturn(Optional.of(refreshToken));
                when(refreshTokenService.verifyExpiration(refreshToken))
                                .thenThrow(new RefreshTokenException(
                                                "Refresh token was expired. Please make a new signin request!"));

                // Act & Assert
                mockMvc.perform(post("/api/auth/refresh-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message")
                                                .value("Refresh token was expired. Please make a new signin request!"));

                verify(refreshTokenService).findByToken("refresh-token-123");
                verify(refreshTokenService).verifyExpiration(refreshToken);
        }

        @Test
        @DisplayName("Register User - Success")
        void testRegisterUser_Success() throws Exception {
                // Arrange
                when(verificationService.generateTokenVerify(any(UserRequest.class))).thenReturn(1L);

                // Act & Assert
                mockMvc.perform(post("/api/auth/registration")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(1));

                verify(verificationService).generateTokenVerify(any(UserRequest.class));
        }

        @Test
        @DisplayName("Register User - Username Already Exists")
        void testRegisterUser_UsernameExists() throws Exception {
                // Arrange
                when(verificationService.generateTokenVerify(any(UserRequest.class)))
                                .thenThrow(new VerificationException("Username is existing"));

                // Act & Assert
                mockMvc.perform(post("/api/auth/registration")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Username is existing"));

                verify(verificationService).generateTokenVerify(any(UserRequest.class));
        }

        @Test
        @DisplayName("Register User - Email Already Exists")
        void testRegisterUser_EmailExists() throws Exception {
                // Arrange
                when(verificationService.generateTokenVerify(any(UserRequest.class)))
                                .thenThrow(new VerificationException("Email is existing"));

                // Act & Assert
                mockMvc.perform(post("/api/auth/registration")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Email is existing"));

                verify(verificationService).generateTokenVerify(any(UserRequest.class));
        }

        @Test
        @DisplayName("Resend Registration Token - Success")
        void testResendRegistrationToken_Success() throws Exception {
                // Arrange
                doNothing().when(verificationService).resendRegistrationToken(1L);

                // Act & Assert
                mockMvc.perform(get("/api/auth/registration/1/resend-token"))
                                .andExpect(status().isOk());

                verify(verificationService).resendRegistrationToken(1L);
        }

        @Test
        @DisplayName("Resend Registration Token - Invalid User ID")
        void testResendRegistrationToken_InvalidUserId() throws Exception {
                // Arrange
                doThrow(new VerificationException("User ID is invalid. Please try again!"))
                                .when(verificationService).resendRegistrationToken(999L);

                // Act & Assert
                mockMvc.perform(get("/api/auth/registration/999/resend-token"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("User ID is invalid. Please try again!"));

                verify(verificationService).resendRegistrationToken(999L);
        }

        @Test
        @DisplayName("Confirm Registration - Success")
        void testConfirmRegistration_Success() throws Exception {
                // Arrange
                doNothing().when(verificationService).confirmRegistration(any(RegistrationRequest.class));

                // Act & Assert
                mockMvc.perform(post("/api/auth/registration/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registrationRequest)))
                                .andExpect(status().isOk());

                verify(verificationService).confirmRegistration(any(RegistrationRequest.class));
        }

        @Test
        @DisplayName("Confirm Registration - Invalid Token")
        void testConfirmRegistration_InvalidToken() throws Exception {
                // Arrange
                doThrow(new VerificationException("Invalid token"))
                                .when(verificationService).confirmRegistration(any(RegistrationRequest.class));

                // Act & Assert
                mockMvc.perform(post("/api/auth/registration/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registrationRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Invalid token"));

                verify(verificationService).confirmRegistration(any(RegistrationRequest.class));
        }

        @Test
        @DisplayName("Change Registration Email - Success")
        void testChangeRegistrationEmail_Success() throws Exception {
                // Arrange
                doNothing().when(verificationService).changeRegistrationEmail(anyLong(), anyString());

                // Act & Assert
                mockMvc.perform(put("/api/auth/registration/1/change-email")
                                .param("email", "newemail@example.com"))
                                .andExpect(status().isOk());

                verify(verificationService).changeRegistrationEmail(1L, "newemail@example.com");
        }

        @Test
        @DisplayName("Change Registration Email - Invalid User ID")
        void testChangeRegistrationEmail_InvalidUserId() throws Exception {
                // Arrange
                doThrow(new VerificationException("User does not exist"))
                                .when(verificationService).changeRegistrationEmail(eq(999L), anyString());

                // Act & Assert
                mockMvc.perform(put("/api/auth/registration/999/change-email")
                                .param("email", "newemail@example.com"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("User does not exist"));

                verify(verificationService).changeRegistrationEmail(999L, "newemail@example.com");
        }

        @Test
        @DisplayName("Forgot Password - Success")
        void testForgotPassword_Success() throws Exception {
                // Arrange
                doNothing().when(verificationService).forgetPassword(anyString());

                // Act & Assert
                mockMvc.perform(get("/api/auth/forgot-password")
                                .param("email", "sunshine040603@gmail.com"))
                                .andExpect(status().isOk());

                verify(verificationService).forgetPassword("sunshine040603@gmail.com");
        }

        @Test
        @DisplayName("Forgot Password - Email Not Found")
        void testForgotPassword_EmailNotFound() throws Exception {
                // Arrange
                doThrow(new RuntimeException("Email doesn't exist"))
                                .when(verificationService).forgetPassword("nonexistent@example.com");

                // Act & Assert - kiểm tra ngoại lệ
                Exception exception = assertThrows(NestedServletException.class, () -> {
                        mockMvc.perform(get("/api/auth/forgot-password")
                                        .param("email", "nonexistent@example.com"));
                });

                assertTrue(exception.getCause() instanceof RuntimeException);
                assertEquals("Email doesn't exist", exception.getCause().getMessage());

                verify(verificationService).forgetPassword("nonexistent@example.com");
        }

        @Test
        @DisplayName("Reset Password - Success")
        void testResetPassword_Success() throws Exception {
                // Arrange
                doNothing().when(verificationService).resetPassword(any(ResetPasswordRequest.class));

                // Act & Assert
                mockMvc.perform(put("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                                .andExpect(status().isOk());

                verify(verificationService).resetPassword(any(ResetPasswordRequest.class));
        }

        @Test
        @DisplayName("Reset Password - Invalid Token")
        void testResetPassword_InvalidToken() throws Exception {
                // Arrange
                doThrow(new RuntimeException("Email and/or token are invalid"))
                                .when(verificationService).resetPassword(any(ResetPasswordRequest.class));

                // Act & Assert
                mockMvc.perform(put("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.message").value("Email and/or token are invalid"));

                verify(verificationService).resetPassword(any(ResetPasswordRequest.class));
        }

        @Test
        @DisplayName("Get User Info - Success")
        void testGetAdminUserInfo_Success() throws Exception {
                // Arrange
                UserResponse userResponse = new UserResponse();
                userResponse.setId(1L);
                userResponse.setUsername("beotron000");
                userResponse.setEmail("sunshine040603@gmail.com");
                userResponse.setFullname("ng hieu");

                when(userRepository.findByUsername("beotron000")).thenReturn(Optional.of(user));
                when(userMapper.entityToResponse(user)).thenReturn(userResponse);

                // Act & Assert
                mockMvc.perform(get("/api/auth/info")
                                .principal(authentication))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.username").value("beotron000"))
                                .andExpect(jsonPath("$.email").value("sunshine040603@gmail.com"))
                                .andExpect(jsonPath("$.fullname").value("ng hieu"));

                verify(userRepository).findByUsername("beotron000");
                verify(userMapper).entityToResponse(user);
        }

        @Test
        @DisplayName("Get User Info - User Not Found")
        void testGetAdminUserInfo_UserNotFound() throws Exception {
                // Arrange
                when(userRepository.findByUsername("beotron000")).thenReturn(Optional.empty());
                when(authentication.getName()).thenReturn("beotron000");

                // Act & Assert - kiểm tra ngoại lệ
                Exception exception = assertThrows(NestedServletException.class, () -> {
                        mockMvc.perform(get("/api/auth/info")
                                        .principal(authentication));
                });

                assertTrue(exception.getCause() instanceof UsernameNotFoundException);
                assertEquals("beotron000", exception.getCause().getMessage());

                verify(userRepository).findByUsername("beotron000");
        }
}