package com.electro.controller.client;

import com.electro.dto.client.ClientCartRequest;
import com.electro.dto.client.ClientCartResponse;
import com.electro.dto.client.ClientCartVariantKeyRequest;
import com.electro.dto.client.ClientCartVariantRequest;
import com.electro.dto.client.UpdateQuantityType;
import com.electro.entity.authentication.User;
import com.electro.entity.cart.Cart;
import com.electro.entity.cart.CartVariant;
import com.electro.entity.cart.CartVariantKey;
import com.electro.entity.inventory.DocketVariant;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.client.ClientCartMapper;
import com.electro.repository.cart.CartRepository;
import com.electro.repository.cart.CartVariantRepository;
import com.electro.repository.inventory.DocketVariantRepository;
import com.electro.utils.InventoryUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientCartControllerTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartVariantRepository cartVariantRepository;

    @Mock
    private ClientCartMapper clientCartMapper;

    @Mock
    private DocketVariantRepository docketVariantRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ObjectNode objectNode;

    @InjectMocks
    private ClientCartController clientCartController;

    private Cart testCart;
    private ClientCartRequest testCartRequest;
    private ClientCartResponse testCartResponse;
    private Long cartId = 1L;
    private Long userId = 1L;
    private Long variantId = 100L;
    private User testUser;
    private List<DocketVariant> docketVariants;
    private Map<String, Integer> inventoryMap;

    @BeforeEach
    public void setup() {
        // Setup test user
        testUser = new User();
        testUser.setId(userId);
        
        // Setup test cart
        testCart = new Cart();
        testCart.setId(cartId);
        testCart.setUser(testUser);
        testCart.setStatus(1); // Normal status
        testCart.setCartVariants(new HashSet<>());

        // Setup inventory map
        inventoryMap = new HashMap<>();
        inventoryMap.put("canBeSold", 10);
        
        // Setup docket variants for inventory
        docketVariants = new ArrayList<>();

        // Setup test cart request
        testCartRequest = new ClientCartRequest();
        testCartRequest.setCartId(cartId);
        testCartRequest.setUserId(userId);
        testCartRequest.setStatus(1);
        testCartRequest.setCartItems(new HashSet<>());
        testCartRequest.setUpdateQuantityType(UpdateQuantityType.OVERRIDE);

        // Setup test cart response
        testCartResponse = new ClientCartResponse();
        testCartResponse.setCartId(cartId);
        testCartResponse.setCartItems(new HashSet<>());
    }

    // Method getCar()
    @Test
    @DisplayName("GC-01: Get cart for existing user")
    public void shouldGetCartForExistingUser() {
        // Arrange
        when(cartRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testCart));
        when(clientCartMapper.entityToResponse(testCart)).thenReturn(testCartResponse);
        
        // Act
        ResponseEntity<ObjectNode> response = clientCartController.getCart(authentication);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartRepository).findByUsername(testUser.getUsername());
        verify(clientCartMapper).entityToResponse(testCart);
    }

    @Test
    @DisplayName("GC-02: Get cart for user without existing cart")
    public void shouldReturnEmptyObjectForUserWithoutCart() {
        // Arrange
        when(cartRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.empty());
        
        // Act
        ResponseEntity<ObjectNode> response = clientCartController.getCart(authentication);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartRepository).findByUsername(testUser.getUsername());
        verify(clientCartMapper, never()).entityToResponse(any(Cart.class));
    }

    // Method: saveCart()
    @Test
    @DisplayName("SC-01: Add product to empty cart")
    public void testAddProductToEmptyCart() {
        // Setup
        testCartRequest.setCartId(null); // New cart (empty)
        
        ClientCartVariantRequest variantRequest = new ClientCartVariantRequest();
        variantRequest.setVariantId(variantId);
        variantRequest.setQuantity(1);
        testCartRequest.getCartItems().add(variantRequest);
        
        Cart newCart = new Cart();
        newCart.setUser(testUser);
        newCart.setStatus(1);
        CartVariant cartVariant = new CartVariant();
        cartVariant.setCartVariantKey(new CartVariantKey(cartId, variantId));
        cartVariant.setQuantity(1);
        newCart.getCartVariants().add(cartVariant);
        
        when(clientCartMapper.requestToEntity(testCartRequest)).thenReturn(newCart);
        when(docketVariantRepository.findByVariantId(variantId)).thenReturn(docketVariants);
        
        // Use try-with-resources for static mocking
        try (MockedStatic<InventoryUtils> mockedInventoryUtils = Mockito.mockStatic(InventoryUtils.class)) {
            mockedInventoryUtils.when(() -> InventoryUtils.calculateInventoryIndices(anyList()))
                .thenReturn(inventoryMap);
            
            // Execute
            ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(testCartRequest);
            
            // Verify
            assertEquals(HttpStatus.OK, response.getStatusCode());
            // assertNotNull(response.getBody());
            verify(cartRepository).save(newCart);
            verify(clientCartMapper).requestToEntity(testCartRequest);
            verify(clientCartMapper, never()).partialUpdate(any(), any());
        }
    }

    @Test
    @DisplayName("SC-02: Add same product with same options to cart")
    public void testAddSameProductWithSameOptionsToCart() {
        // Setup
        ClientCartVariantRequest variantRequest = new ClientCartVariantRequest();
        variantRequest.setVariantId(variantId);
        variantRequest.setQuantity(2);
        testCartRequest.getCartItems().add(variantRequest);
        
        CartVariant cartVariant = new CartVariant();
        cartVariant.setCartVariantKey(new CartVariantKey(cartId, variantId));
        cartVariant.setQuantity(2);
        testCart.getCartVariants().add(cartVariant);
        
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
        when(clientCartMapper.partialUpdate(any(Cart.class), any(ClientCartRequest.class))).thenReturn(testCart);
        when(docketVariantRepository.findByVariantId(variantId)).thenReturn(docketVariants);
        
        try (MockedStatic<InventoryUtils> mockedInventoryUtils = Mockito.mockStatic(InventoryUtils.class)) {
            mockedInventoryUtils.when(() -> InventoryUtils.calculateInventoryIndices(anyList()))
                .thenReturn(inventoryMap);
            
            // Execute
            ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(testCartRequest);
            
            // Verify
            assertEquals(HttpStatus.OK, response.getStatusCode());
            // assertNotNull(response.getBody());
            verify(cartRepository).findById(cartId);
            verify(clientCartMapper).partialUpdate(testCart, testCartRequest);
            verify(cartRepository).save(testCart);
        }
    }

    @Test
    @DisplayName("SC-03: Add same product with different options to cart")
    public void testAddSameProductWithDifferentOptionsToCart() {
        // Setup - different variant of same product (different variantId)
        Long variantId2 = 101L;
        
        ClientCartVariantRequest variantRequest1 = new ClientCartVariantRequest();
        variantRequest1.setVariantId(variantId);
        variantRequest1.setQuantity(1);
        
        ClientCartVariantRequest variantRequest2 = new ClientCartVariantRequest();
        variantRequest2.setVariantId(variantId2);
        variantRequest2.setQuantity(1);
        
        testCartRequest.getCartItems().add(variantRequest1);
        testCartRequest.getCartItems().add(variantRequest2);
        
        CartVariant cartVariant1 = new CartVariant();
        cartVariant1.setCartVariantKey(new CartVariantKey(cartId, variantId));
        cartVariant1.setQuantity(1);
        
        CartVariant cartVariant2 = new CartVariant();
        cartVariant2.setCartVariantKey(new CartVariantKey(cartId, variantId2));
        cartVariant2.setQuantity(1);
        
        testCart.getCartVariants().add(cartVariant1);
        testCart.getCartVariants().add(cartVariant2);
        
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
        when(clientCartMapper.partialUpdate(any(Cart.class), any(ClientCartRequest.class))).thenReturn(testCart);
        when(docketVariantRepository.findByVariantId(anyLong())).thenReturn(docketVariants);
        
        try (MockedStatic<InventoryUtils> mockedInventoryUtils = Mockito.mockStatic(InventoryUtils.class)) {
            mockedInventoryUtils.when(() -> InventoryUtils.calculateInventoryIndices(anyList()))
                .thenReturn(inventoryMap);
            
            // Execute
            ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(testCartRequest);
            
            // Verify
            assertEquals(HttpStatus.OK, response.getStatusCode());
            // assertNotNull(response.getBody());
            verify(cartRepository).findById(cartId);
            verify(clientCartMapper).partialUpdate(testCart, testCartRequest);
            verify(cartRepository).save(testCart);
        }
    }

    @Test
    @DisplayName("sC-04: Add product with quantity equal to available stock")
    public void testAddProductWithQuantityEqualToStock() {
        // Setup
        ClientCartVariantRequest variantRequest = new ClientCartVariantRequest();
        variantRequest.setVariantId(variantId);
        variantRequest.setQuantity(10); // Equal to stock (10)
        testCartRequest.getCartItems().add(variantRequest);
        
        CartVariant cartVariant = new CartVariant();
        cartVariant.setCartVariantKey(new CartVariantKey(cartId, variantId));
        cartVariant.setQuantity(10);
        testCart.getCartVariants().add(cartVariant);
        
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
        when(clientCartMapper.partialUpdate(any(Cart.class), any(ClientCartRequest.class))).thenReturn(testCart);
        when(docketVariantRepository.findByVariantId(variantId)).thenReturn(docketVariants);
        
        try (MockedStatic<InventoryUtils> mockedInventoryUtils = Mockito.mockStatic(InventoryUtils.class)) {
            mockedInventoryUtils.when(() -> InventoryUtils.calculateInventoryIndices(anyList()))
                .thenReturn(inventoryMap);
            
            // Execute
            ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(testCartRequest);
            
            // Verify
            assertEquals(HttpStatus.OK, response.getStatusCode());
            // assertNotNull(response.getBody());
            verify(cartRepository).save(testCart);
        }
    }

    @Test
    @DisplayName("SC-05: Add product with quantity exceeding stock")
    public void testAddProductWithQuantityExceedingStock() {
        // Setup
        ClientCartVariantRequest variantRequest = new ClientCartVariantRequest();
        variantRequest.setVariantId(variantId);
        variantRequest.setQuantity(15); // Exceeds stock (10)
        testCartRequest.getCartItems().add(variantRequest);
        
        CartVariant cartVariant = new CartVariant();
        cartVariant.setCartVariantKey(new CartVariantKey(cartId, variantId));
        cartVariant.setQuantity(15);
        testCart.getCartVariants().add(cartVariant);
        
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
        when(clientCartMapper.partialUpdate(any(Cart.class), any(ClientCartRequest.class))).thenReturn(testCart);
        when(docketVariantRepository.findByVariantId(variantId)).thenReturn(docketVariants);
        
        // Use try-with-resources for static mocking
        try (MockedStatic<InventoryUtils> mockedInventoryUtils = Mockito.mockStatic(InventoryUtils.class)) {
            mockedInventoryUtils.when(() -> InventoryUtils.calculateInventoryIndices(anyList()))
                .thenReturn(inventoryMap);
            
            // Execute & Verify
            Exception exception = assertThrows(RuntimeException.class, () -> {
                clientCartController.saveCart(testCartRequest);
            });
            
            assertEquals("Variant quantity cannot greater than variant inventory", exception.getMessage());
        }
    }

    @Test
    @DisplayName("SC-06: Add product with quantity below minimum")
    public void testAddProductWithQuantityBelowMinimum() {
        // Setup - assuming minimum is 1
        ClientCartVariantRequest variantRequest = new ClientCartVariantRequest();
        variantRequest.setVariantId(variantId);
        variantRequest.setQuantity(0); // Below minimum
        testCartRequest.getCartItems().add(variantRequest);
        
        CartVariant cartVariant = new CartVariant();
        cartVariant.setCartVariantKey(new CartVariantKey(cartId, variantId));
        cartVariant.setQuantity(0);
        testCart.getCartVariants().add(cartVariant);
        
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
        when(clientCartMapper.partialUpdate(any(Cart.class), any(ClientCartRequest.class))).thenReturn(testCart);
        when(docketVariantRepository.findByVariantId(variantId)).thenReturn(docketVariants);
        
        try (MockedStatic<InventoryUtils> mockedInventoryUtils = Mockito.mockStatic(InventoryUtils.class)) {
            mockedInventoryUtils.when(() -> InventoryUtils.calculateInventoryIndices(anyList()))
                .thenReturn(inventoryMap);
            
            // Execute
            ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(testCartRequest);
            
            // Verify - Controller doesn't validate minimum quantity, so it should succeed
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            // assertNotNull(response.getBody());
            verify(cartRepository).save(testCart);
        }
    }

    @Test
    @DisplayName("SC-07: Add product multiple times exceeding stock")
    public void testAddProductMultipleTimesExceedingStock() {
        // Setup first request with 6 quantity
        ClientCartVariantRequest variantRequest = new ClientCartVariantRequest();
        variantRequest.setVariantId(variantId);
        variantRequest.setQuantity(6); // First add
        testCartRequest.getCartItems().add(variantRequest);
        
        CartVariant cartVariant = new CartVariant();
        cartVariant.setCartVariantKey(new CartVariantKey(cartId, variantId));
        cartVariant.setQuantity(6);
        testCart.getCartVariants().add(cartVariant);
        
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
        when(clientCartMapper.partialUpdate(any(Cart.class), any(ClientCartRequest.class))).thenReturn(testCart);
        when(docketVariantRepository.findByVariantId(variantId)).thenReturn(docketVariants);
        
        try (MockedStatic<InventoryUtils> mockedInventoryUtils = Mockito.mockStatic(InventoryUtils.class)) {
            mockedInventoryUtils.when(() -> InventoryUtils.calculateInventoryIndices(anyList()))
                .thenReturn(inventoryMap);
            
            // Execute first request
            ResponseEntity<ClientCartResponse> response1 = clientCartController.saveCart(testCartRequest);
            assertEquals(HttpStatus.OK, response1.getStatusCode());
            
            // Setup second request that would exceed stock
            testCartRequest.getCartItems().clear();
            ClientCartVariantRequest variantRequest2 = new ClientCartVariantRequest();
            variantRequest2.setVariantId(variantId);
            variantRequest2.setQuantity(11); // 6 + 5 (exceeds stock of 10)
            testCartRequest.getCartItems().add(variantRequest2);
            
            testCart.getCartVariants().clear();
            CartVariant cartVariant2 = new CartVariant();
            cartVariant2.setCartVariantKey(new CartVariantKey(cartId, variantId));
            cartVariant2.setQuantity(11);
            testCart.getCartVariants().add(cartVariant2);
            
            // Execute & Verify
            Exception exception = assertThrows(RuntimeException.class, () -> {
                clientCartController.saveCart(testCartRequest);
            });
            
            assertEquals("Variant quantity cannot greater than variant inventory", exception.getMessage());
        }
    }

    @Test
    @DisplayName("SC-08: Add product to cart when not logged in")
    public void testAddProductToCartWhenNotLoggedIn() {
        // Setup - null username to simulate not logged in
        testCartRequest.setUserId(null);
        testCartRequest.setCartId(null);
        
        ClientCartVariantRequest variantRequest = new ClientCartVariantRequest();
        variantRequest.setVariantId(variantId);
        variantRequest.setQuantity(1);
        testCartRequest.getCartItems().add(variantRequest);
        
        Cart newCart = new Cart();
        newCart.setUser(null); // No username
        CartVariant cartVariant = new CartVariant();
        cartVariant.setCartVariantKey(new CartVariantKey(cartId, variantId));
        cartVariant.setQuantity(1);
        newCart.getCartVariants().add(cartVariant);
        
        when(clientCartMapper.requestToEntity(testCartRequest)).thenReturn(newCart);
        when(docketVariantRepository.findByVariantId(variantId)).thenReturn(docketVariants);
        
        try (MockedStatic<InventoryUtils> mockedInventoryUtils = Mockito.mockStatic(InventoryUtils.class)) {
            mockedInventoryUtils.when(() -> InventoryUtils.calculateInventoryIndices(anyList()))
                .thenReturn(inventoryMap);
            
            // Execute
            ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(testCartRequest);
            
            // Verify - Controller should validate login status, so it should fail
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            // assertNotNull(response.getBody());
            verify(cartRepository).save(newCart);
        }
    }

    @Test
    @DisplayName("SC-09: Create new cart when cartId is null") // check lai
    public void shouldCreateNewCartWhenCartIdIsNull() {
        // Arrange
        testCartRequest.setCartId(null); // Simulate new cart
        
        ClientCartVariantRequest variantRequest = new ClientCartVariantRequest();
        variantRequest.setVariantId(variantId);
        variantRequest.setQuantity(1);
        testCartRequest.getCartItems().add(variantRequest);
        
        // Mock new cart creation
        when(clientCartMapper.requestToEntity(testCartRequest)).thenReturn(testCart);
        
        // Mock docket variant data
        List<DocketVariant> mockDocketVariants = Collections.emptyList(); // Empty list for simplicity
        // when(docketVariantRepository.findByVariantId(variantId)).thenReturn(mockDocketVariants);
        
        // Use MockedStatic to mock the static method
        try (MockedStatic<InventoryUtils> mockedInventoryUtils = mockStatic(InventoryUtils.class)) {
            // Set up the inventory check
            Map<String, Integer> inventoryMap = new HashMap<>();
            inventoryMap.put("canBeSold", 10); // Enough inventory
            mockedInventoryUtils.when(() -> InventoryUtils.calculateInventoryIndices(mockDocketVariants))
                .thenReturn(inventoryMap);
            
            when(cartRepository.save(testCart)).thenReturn(testCart);
            when(clientCartMapper.entityToResponse(testCart)).thenReturn(testCartResponse);
            
            // Act
            ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(testCartRequest);
            
            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(clientCartMapper).requestToEntity(testCartRequest);
            verify(cartRepository).save(testCart);
            verify(clientCartMapper).entityToResponse(testCart);
        }
    }

    @Test
    @DisplayName("SC-10: Cart not found when updating with invalid cartId")
    public void shouldThrowExceptionWhenCartNotFound() {
        // Arrange
        Long invalidCartId = 999L;
        testCartRequest.setCartId(invalidCartId);
        
        when(cartRepository.findById(invalidCartId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(
            ResourceNotFoundException.class,
            () -> clientCartController.saveCart(testCartRequest),
            "Expected saveCart to throw ResourceNotFoundException when cart not found"
        );
        
        verify(cartRepository).findById(invalidCartId);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("SC-11: Increase item quantity through cart")
    public void shouldIncreaseItemQuantity() {
        // Arrange
        int initialQuantity = 2;
        int newQuantity = 3;
        
        // Create a cart variant with initial quantity
        CartVariant cartVariant = createCartVariant(initialQuantity);
        testCart.getCartVariants().add(cartVariant);
        
        // Create the request with increased quantity
        ClientCartVariantRequest variantRequest = new ClientCartVariantRequest();
        variantRequest.setVariantId(variantId);
        variantRequest.setQuantity(newQuantity);
        testCartRequest.getCartItems().add(variantRequest);
        
        // Mock docket variant data
        List<DocketVariant> mockDocketVariants = Collections.emptyList(); // Empty list for simplicity
        when(docketVariantRepository.findByVariantId(variantId)).thenReturn(mockDocketVariants);
        
        // Use MockedStatic to mock the static method
        try (MockedStatic<InventoryUtils> mockedInventoryUtils = mockStatic(InventoryUtils.class)) {
            // Set up the inventory check
            Map<String, Integer> inventoryMap = new HashMap<>();
            inventoryMap.put("canBeSold", 10); // Enough inventory
            mockedInventoryUtils.when(() -> InventoryUtils.calculateInventoryIndices(mockDocketVariants))
                .thenReturn(inventoryMap);
            
            // Set up repository and mapper behavior
            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
            when(clientCartMapper.partialUpdate(any(Cart.class), any(ClientCartRequest.class))).thenAnswer(invocation -> {
                Cart cart = invocation.getArgument(0);
                // ClientCartRequest request = invocation.getArgument(1);
                
                // Simulate updating the cart with new quantity
                Optional<CartVariant> existingVariant = cart.getCartVariants().stream()
                        .filter(v -> v.getCartVariantKey().getVariantId().equals(variantId))
                        .findFirst();
                
                if (existingVariant.isPresent()) {
                    existingVariant.get().setQuantity(newQuantity);
                }
                
                return cart;
            });
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
            when(clientCartMapper.entityToResponse(testCart)).thenReturn(testCartResponse);
            
            // Act
            ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(testCartRequest);
            
            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(cartRepository).findById(cartId);
            verify(cartRepository).save(testCart);
            verify(clientCartMapper).entityToResponse(testCart);
        }
    }

    @Test
    @DisplayName("SC-12: Increase item quantity to maximum stock limit")
    public void shouldIncreaseItemQuantityToMaxStockLimit() {
        // Arrange
        int initialQuantity = 5;
        int maxInventory = 10;
        int newQuantity = maxInventory; // Increasing to max allowed
        
        // Create a cart variant with initial quantity
        CartVariant cartVariant = createCartVariant(initialQuantity);
        testCart.getCartVariants().add(cartVariant);
        
        // Create the request with increased quantity
        ClientCartVariantRequest variantRequest = new ClientCartVariantRequest();
        variantRequest.setVariantId(variantId);
        variantRequest.setQuantity(newQuantity);
        testCartRequest.getCartItems().add(variantRequest);
        
        // Mock docket variant data
        List<DocketVariant> mockDocketVariants = Collections.emptyList(); // Empty list for simplicity
        when(docketVariantRepository.findByVariantId(variantId)).thenReturn(mockDocketVariants);
        
        // Use MockedStatic to mock the static method
        try (MockedStatic<InventoryUtils> mockedInventoryUtils = mockStatic(InventoryUtils.class)) {
            // Set up the inventory check
            Map<String, Integer> inventoryMap = new HashMap<>();
            inventoryMap.put("canBeSold", maxInventory); // Exactly enough inventory
            mockedInventoryUtils.when(() -> InventoryUtils.calculateInventoryIndices(mockDocketVariants))
                .thenReturn(inventoryMap);
            
            // Set up repository and mapper behavior
            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
            when(clientCartMapper.partialUpdate(any(Cart.class), any(ClientCartRequest.class))).thenAnswer(invocation -> {
                Cart cart = invocation.getArgument(0);
                // ClientCartRequest request = invocation.getArgument(1);
                
                // Simulate updating the cart with new quantity
                Optional<CartVariant> existingVariant = cart.getCartVariants().stream()
                        .filter(v -> v.getCartVariantKey().getVariantId().equals(variantId))
                        .findFirst();
                
                if (existingVariant.isPresent()) {
                    existingVariant.get().setQuantity(newQuantity);
                }
                
                return cart;
            });
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
            when(clientCartMapper.entityToResponse(testCart)).thenReturn(testCartResponse);
            
            // Act
            ResponseEntity<ClientCartResponse> response = clientCartController.saveCart(testCartRequest);
            
            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(cartRepository).findById(cartId);
            verify(cartRepository).save(testCart);
            verify(clientCartMapper).entityToResponse(testCart);
        }
    }

    @Test
    @DisplayName("SC-13: Attempt to increase item quantity beyond stock limit")
    public void shouldThrowExceptionWhenIncreasingBeyondStockLimit() {
        // Arrange
        int initialQuantity = 5;
        int maxInventory = 10;
        int exceedingQuantity = maxInventory + 1; // Exceeding max inventory
        
        // Create a cart variant with initial quantity
        CartVariant cartVariant = createCartVariant(initialQuantity);
        testCart.getCartVariants().add(cartVariant);
        
        // Create the request with increased quantity
        ClientCartVariantRequest variantRequest = new ClientCartVariantRequest();
        variantRequest.setVariantId(variantId);
        variantRequest.setQuantity(exceedingQuantity);
        testCartRequest.getCartItems().add(variantRequest);
        
        // Mock docket variant data
        List<DocketVariant> mockDocketVariants = Collections.emptyList(); // Empty list for simplicity
        when(docketVariantRepository.findByVariantId(variantId)).thenReturn(mockDocketVariants);
        
        // Use MockedStatic to mock the static method
        try (MockedStatic<InventoryUtils> mockedInventoryUtils = mockStatic(InventoryUtils.class)) {
            // Set up the inventory check
            Map<String, Integer> inventoryMap = new HashMap<>();
            inventoryMap.put("canBeSold", maxInventory); // Not enough inventory
            mockedInventoryUtils.when(() -> InventoryUtils.calculateInventoryIndices(mockDocketVariants))
                .thenReturn(inventoryMap);
            
            // Set up repository and mapper behavior
            when(cartRepository.findById(cartId)).thenReturn(Optional.of(testCart));
            when(clientCartMapper.partialUpdate(any(Cart.class), any(ClientCartRequest.class))).thenAnswer(invocation -> {
                Cart cart = invocation.getArgument(0);
                // ClientCartRequest request = invocation.getArgument(1);
                
                // Simulate updating the cart with new quantity
                Optional<CartVariant> existingVariant = cart.getCartVariants().stream()
                        .filter(v -> v.getCartVariantKey().getVariantId().equals(variantId))
                        .findFirst();
                
                if (existingVariant.isPresent()) {
                    existingVariant.get().setQuantity(exceedingQuantity);
                }
                
                return cart;
            });
            
            // Act & Assert
            RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> clientCartController.saveCart(testCartRequest),
                "Expected saveCart to throw RuntimeException when quantity exceeds inventory"
            );
            
            assertEquals("Variant quantity cannot greater than variant inventory", exception.getMessage());
            verify(cartRepository).findById(cartId);
            verify(cartRepository, never()).save(any(Cart.class));
        }
    }

    // Method: deleteCartItems()
    @Test
    @DisplayName("DC-01: Remove item from cart")
    public void shouldRemoveItemFromCart() {
        // Arrange
        List<ClientCartVariantKeyRequest> deleteRequests = new ArrayList<>();
        ClientCartVariantKeyRequest keyRequest = new ClientCartVariantKeyRequest();
        keyRequest.setCartId(cartId);
        keyRequest.setVariantId(variantId);
        deleteRequests.add(keyRequest);
        
        // Expected CartVariantKey to be deleted
        List<CartVariantKey> expectedKeys = deleteRequests.stream()
                .map(req -> new CartVariantKey(req.getCartId(), req.getVariantId()))
                .collect(Collectors.toList());
        
        // Act
        ResponseEntity<Void> response = clientCartController.deleteCartItems(deleteRequests);
        
        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(cartVariantRepository).deleteAllById(expectedKeys);
    }

    // Helper methods
    private CartVariant createCartVariant(int quantity) {
        CartVariantKey key = new CartVariantKey(cartId, variantId);
        CartVariant variant = new CartVariant();
        variant.setCartVariantKey(key);
        variant.setQuantity(quantity);
        return variant;
    }
}