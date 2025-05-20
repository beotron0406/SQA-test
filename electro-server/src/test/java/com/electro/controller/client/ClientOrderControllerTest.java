package com.electro.controller.client;

import com.electro.constant.AppConstants;
import com.electro.dto.ListResponse;
import com.electro.dto.client.ClientConfirmedOrderResponse;
import com.electro.dto.client.ClientOrderDetailResponse;
import com.electro.dto.client.ClientSimpleOrderRequest;
import com.electro.dto.client.ClientSimpleOrderResponse;
import com.electro.entity.general.Notification;
import com.electro.entity.general.NotificationType;
import com.electro.entity.order.Order;
import com.electro.entity.authentication.User;
import com.electro.entity.cashbook.PaymentMethodType;
import com.electro.exception.ResourceNotFoundException;
import com.electro.mapper.client.ClientOrderMapper;
import com.electro.mapper.general.NotificationMapper;
import com.electro.repository.general.NotificationRepository;
import com.electro.repository.order.OrderRepository;
import com.electro.service.general.NotificationService;
import com.electro.service.order.OrderService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InvalidOrderRequestException extends RuntimeException {
    
    public InvalidOrderRequestException(String message) {
        super(message);
    }
    
    public InvalidOrderRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}

class EmptyCartException extends RuntimeException {
    public EmptyCartException(String message) {
        super(message);
    }
}

@ExtendWith(MockitoExtension.class)
public class ClientOrderControllerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ClientOrderMapper clientOrderMapper;

    @Mock
    private OrderService orderService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ClientOrderController clientOrderController;

    private User mockUser;
    private Order mockOrder, testOrder1, testOrder2;
    private ClientSimpleOrderRequest mockOrderRequest;
    private ClientSimpleOrderResponse mockOrderResponse, orderResponse1, orderResponse2;
    private ClientOrderDetailResponse mockOrderDetailResponse, detailResponse;
    private List<Order> orderList;
    private ClientConfirmedOrderResponse mockConfirmedOrderResponse, confirmedOrderResponse;
    private Notification mockNotification;

    @BeforeEach
    void setUp() {
        // Set up mock user
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");

        // Set up mock order
        mockOrder = new Order();
        mockOrder.setCode("ORD12345");
        mockOrder.setUser(mockUser);
        mockOrder.setPaypalOrderId("PAY12345");

        // Set up mock order 2
        testOrder1 = new Order();
        testOrder1.setId(1L);
        testOrder1.setCode("ORDER-001");
        testOrder1.setStatus(1); // Processing
        testOrder1.setTotalPay(new BigDecimal("100.00"));
        testOrder1.setUser(mockUser);

        testOrder2 = new Order();
        testOrder2.setId(2L);
        testOrder2.setCode("ORDER-002");
        testOrder2.setStatus(2); // Shipped
        testOrder2.setTotalPay(new BigDecimal("150.00"));
        testOrder2.setUser(mockUser);

        orderList = Arrays.asList(testOrder1, testOrder2);

        // Set up mock order request
        mockOrderRequest = new ClientSimpleOrderRequest();
        // populate with test data

        // Set up mock order responses
        mockOrderResponse = new ClientSimpleOrderResponse();
        mockOrderResponse.setOrderCode("ORD12345");

        mockOrderDetailResponse = new ClientOrderDetailResponse();
        mockOrderDetailResponse.setOrderCode("ORD12345");

        mockConfirmedOrderResponse = new ClientConfirmedOrderResponse();
        mockConfirmedOrderResponse.setOrderCode("ORD12345");

        // Set up DTOs
        orderResponse1 = new ClientSimpleOrderResponse();
        orderResponse1.setOrderCode("ORDER-001");
        orderResponse1.setOrderStatus(1);

        orderResponse2 = new ClientSimpleOrderResponse();
        orderResponse2.setOrderCode("ORDER-002");
        orderResponse2.setOrderStatus(2);

        detailResponse = new ClientOrderDetailResponse();
        detailResponse.setOrderCode("ORDER-001");
        detailResponse.setOrderStatus(1);

        confirmedOrderResponse = new ClientConfirmedOrderResponse();
        confirmedOrderResponse.setOrderCode("ORDER-003");

        // Set up mock notification
        mockNotification = new Notification();
        mockNotification.setId(1L);
        mockNotification.setUser(mockUser);
        mockNotification.setType(NotificationType.CHECKOUT_PAYPAL_CANCEL);
        mockNotification.setMessage(String.format("Bạn đã hủy thanh toán PayPal cho đơn hàng %s.", mockOrder.getCode()));
        mockNotification.setAnchor("/order/detail/" + mockOrder.getCode());
        mockNotification.setStatus(1); // 1 - Chưa đọc (Unread)
    }

    // Method: getAllOrders()
    @Test
    @DisplayName("GAO-01: View order history")
    void testGetAllOrders() {
        // Given
        String username = "testuser";
        when(authentication.getName()).thenReturn(username);

        List<Order> orders = new ArrayList<>();
        orders.add(mockOrder);
        Page<Order> orderPage = new PageImpl<>(orders);
        
        when(orderRepository.findAllByUsername(eq(username), anyString(), nullable(String.class), any(PageRequest.class)))
                .thenReturn(orderPage);
        when(clientOrderMapper.entityToResponse(any(Order.class))).thenReturn(mockOrderResponse);

        // When
        ResponseEntity<ListResponse<ClientSimpleOrderResponse>> response = clientOrderController.getAllOrders(
                authentication, 1, 10, "id,desc", null);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getSize());
        verify(orderRepository).findAllByUsername(eq(username), anyString(), nullable(String.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("GAO-02: View order history with existing orders")
    void testGetAllOrdersWithExistingOrders() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        
        Page<Order> orderPage = new PageImpl<>(orderList);
        when(orderRepository.findAllByUsername(eq("testuser"), anyString(), isNull(), any(PageRequest.class)))
                .thenReturn(orderPage);
        
        when(clientOrderMapper.entityToResponse(testOrder1)).thenReturn(orderResponse1);
        when(clientOrderMapper.entityToResponse(testOrder2)).thenReturn(orderResponse2);

        // Act
        ResponseEntity<ListResponse<ClientSimpleOrderResponse>> response = 
                clientOrderController.getAllOrders(authentication, 1, 10, "createdAt,desc", null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getContent().size());
        assertEquals("ORDER-001", response.getBody().getContent().get(0).getOrderCode());
        assertEquals("ORDER-002", response.getBody().getContent().get(1).getOrderCode());
        
        verify(orderRepository).findAllByUsername(eq("testuser"), eq("createdAt,desc"), isNull(), any(PageRequest.class));
    }

    // Method: getOrder()
    @Test
    @DisplayName("GO-01: Get order details")
    void testGetOrder() {
        // Given
        String orderCode = "ORD12345";
        when(orderRepository.findByCode(orderCode)).thenReturn(Optional.of(mockOrder));
        when(clientOrderMapper.entityToDetailResponse(mockOrder)).thenReturn(mockOrderDetailResponse);

        // When
        ResponseEntity<ClientOrderDetailResponse> response = clientOrderController.getOrder(orderCode);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockOrderDetailResponse, response.getBody());
        verify(orderRepository).findByCode(orderCode);
    }

    @Test
    @DisplayName("GO-03: Get order details - Order not found")
    void testGetOrderNotFound() {
        // Given
        String orderCode = "INVALID_CODE";
        when(orderRepository.findByCode(orderCode)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> clientOrderController.getOrder(orderCode)
        );
        
        // Verify the exception message contains relevant information
        assertTrue(exception.getMessage().contains(orderCode));
        
        verify(orderRepository).findByCode(orderCode);
    }
    
    // Method: createClientOrder()
    @Test
    @DisplayName("CCO-01: Place order with preplaced information")
    void testCreateClientOrder() {
        // Given
        when(orderService.createClientOrder(any(ClientSimpleOrderRequest.class)))
                .thenReturn(mockConfirmedOrderResponse);

        // When
        ResponseEntity<ClientConfirmedOrderResponse> response = clientOrderController.createClientOrder(mockOrderRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockConfirmedOrderResponse, response.getBody());
        verify(orderService, times(1)).createClientOrder(mockOrderRequest);
    }

    @Test
    @DisplayName("CCO-02: Place order with missing information")
    void testPlaceOrderWithMissingInformation() {
        // Arrange
        ClientSimpleOrderRequest incompleteRequest = new ClientSimpleOrderRequest();
        // Leave the paymentMethodType null, which is the only field in the actual class
        
        // Mock the validation behavior in the service
        when(orderService.createClientOrder(any(ClientSimpleOrderRequest.class)))
                .thenThrow(new InvalidOrderRequestException("Missing required information: paymentMethodType"));
        
        // Act & Assert
        InvalidOrderRequestException exception = assertThrows(
                InvalidOrderRequestException.class,
                () -> clientOrderController.createClientOrder(incompleteRequest)
        );
        
        // Verify exception message contains details about missing information
        assertTrue(exception.getMessage().contains("Missing required information"), 
                "Exception message should indicate that required information is missing");
        assertTrue(exception.getMessage().contains("paymentMethodType"), 
                "Exception message should specify missing paymentMethodType information");
        
        // Verify service method was called with the incomplete request
        verify(orderService).createClientOrder(incompleteRequest);
    }

    @Test
    @DisplayName("CCO-03: Place order with empty cart")
    void testPlaceOrderWithEmptyCart() {
        // Arrange
        ClientSimpleOrderRequest orderRequest = new ClientSimpleOrderRequest();
        orderRequest.setPaymentMethodType(PaymentMethodType.CASH); // Set the payment method type
        
        // Mock behavior - service throws EmptyCartException when cart is empty
        when(orderService.createClientOrder(any(ClientSimpleOrderRequest.class)))
                .thenThrow(new EmptyCartException("Cannot place order with empty cart"));
        
        // Act & Assert
        EmptyCartException exception = assertThrows(
                EmptyCartException.class,
                () -> clientOrderController.createClientOrder(orderRequest)
        );
        
        // Verify exception message
        assertEquals("Cannot place order with empty cart", exception.getMessage());
        
        // Verify service method was called with the request
        verify(orderService).createClientOrder(orderRequest);
    }

    // Method: cancelOrder()
    @Test
    @DisplayName("CO-01: Cancel order")
    void testCancelOrder() {
        // Given
        String orderCode = "ORD12345";
        doNothing().when(orderService).cancelOrder(orderCode);

        // When
        ResponseEntity<ObjectNode> response = clientOrderController.cancelOrder(orderCode);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(orderService).cancelOrder(orderCode);
    }

    @Test
    @DisplayName("CO-02: Cancel order - Order not found")
    void testCancelOrderNotFound() {
        // Arrange
        String nonExistentOrderCode = "NON_EXISTENT_CODE";
        
        // Mock behavior - service throws ResourceNotFoundException when order isn't found
        doThrow(new ResourceNotFoundException("Order", "orderCode", nonExistentOrderCode))
                .when(orderService).cancelOrder(nonExistentOrderCode);
        
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> clientOrderController.cancelOrder(nonExistentOrderCode)
        );
        
         // Verify exception message contains details about the resource not found
        assertEquals("Order not found with orderCode: 'NON_EXISTENT_CODE'", exception.getMessage());
        
        // Verify service method was called with the provided order code
        verify(orderService).cancelOrder(nonExistentOrderCode);
    }

    // Method: paymentSuccessAndCaptureTransaction()
    @Test
    @DisplayName("PSCT-01: When complete payment, place order")
    void testPaymentSuccessAndCaptureTransaction() {
        // Given
        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).setParameter("token", "PAY12345");
        ((MockHttpServletRequest) request).setParameter("PayerID", "PAYER12345");
        
        doNothing().when(orderService).captureTransactionPaypal(anyString(), anyString());

        // When
        RedirectView redirectView = clientOrderController.paymentSuccessAndCaptureTransaction(request);

        // Then
        assertNotNull(redirectView);
        assertEquals(AppConstants.FRONTEND_HOST + "/payment/success", redirectView.getUrl());
        verify(orderService, times(1)).captureTransactionPaypal("PAY12345", "PAYER12345");
    }

    @Test
    @DisplayName("PSCT-02: PayPal payment capture with null parameters")
    void testPaymentSuccessWithNullParameters() {
        // Arrange - simulating null parameters in the request
        HttpServletRequest request = new MockHttpServletRequest();
        
        // Act
        RedirectView redirectView = clientOrderController.paymentSuccessAndCaptureTransaction(request);
        
        // Assert
        // Verify that the order service was called with null parameters
        verify(orderService).captureTransactionPaypal(null, null);
        
        // Verify that the redirect URL still points to the success page
        assertEquals(AppConstants.FRONTEND_HOST + "/payment/success", redirectView.getUrl());
    }

    // Method: paymentCancel()
    @Test
    @DisplayName("PC-01: Cancel payment when placing order - Verify order is not saved")
    void testPaymentCancelOrderRemainsIncomplete() {
        // Given
        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).setParameter("token", "PAY12345");
        
        // Create a mock order with initial state
        Order originalOrder = new Order();
        originalOrder.setCode("ORD12345");
        originalOrder.setUser(mockUser);
        originalOrder.setPaypalOrderId("PAY12345");
        originalOrder.setPaymentMethodType(PaymentMethodType.PAYPAL);
        originalOrder.setPaymentStatus(1);  // 1 - Chưa thanh toán (Not paid)
        originalOrder.setStatus(1);  // Pending status
        originalOrder.setPaypalOrderStatus("CREATED");
        
        when(orderRepository.findByPaypalOrderId("PAY12345")).thenReturn(Optional.of(originalOrder));
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);
        doNothing().when(notificationService).pushNotification(anyString(), any());
        
        // When
        clientOrderController.paymentCancel(request);
        
        // Then
        // Verify the order's status remains unchanged
        // The controller doesn't update the order in the database, so we verify that save is not called
        verify(orderRepository, never()).save(any(Order.class));
        
        // Verify the original order object is not modified
        assertEquals(1, originalOrder.getPaymentStatus());  // Still unpaid
        assertEquals(1, originalOrder.getStatus());  // Still in pending status
        assertEquals("CREATED", originalOrder.getPaypalOrderStatus());  // PayPal status unchanged
        
        // Also verify no other service methods are called that might update the order
        verify(orderService, never()).captureTransactionPaypal(anyString(), anyString());
    }

    @Test
    @DisplayName("PC-02: Cancel payment when placing order - Order not found")
    void testPaymentCancelOrderNotFound() {
        // Given
        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).setParameter("token", "INVALID_PAY_ID");
        
        when(orderRepository.findByPaypalOrderId("INVALID_PAY_ID")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            clientOrderController.paymentCancel(request);
        });
        
        verify(orderRepository).findByPaypalOrderId("INVALID_PAY_ID");
        verify(notificationRepository, never()).save(any(Notification.class));
    }
}