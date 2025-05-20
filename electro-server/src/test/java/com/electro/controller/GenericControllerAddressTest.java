package com.electro.controller;

import com.electro.dto.ListResponse;
import com.electro.dto.address.AddressRequest;
import com.electro.dto.address.AddressResponse;
import com.electro.service.CrudService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenericController<AddressRequest,AddressResponse> – Address endpoints")
class GenericControllerAddressTest {

    @Mock
    CrudService<Long, AddressRequest, AddressResponse> addressService;

    @InjectMocks
    GenericController<AddressRequest, AddressResponse> controller;

    ObjectMapper mapper;
    JsonNode payload;
    AddressRequest req;
    AddressResponse r1, r2;

    @BeforeEach
    void setUp() throws Exception {
        // wire the generic controller
        controller.setCrudService(addressService);
        controller.setRequestType(AddressRequest.class);

        mapper = new ObjectMapper();
        payload = mapper.readTree("""
            {
              "line":"123 Main St",
              "provinceId":1,
              "districtId":2,
              "wardId":3
            }
        """);

        req = new AddressRequest();
        req.setLine("123 Main St");
        req.setProvinceId(1L);
        req.setDistrictId(2L);
        req.setWardId(3L);

        r1 = new AddressResponse();
        r1.setId(10L);
        r1.setLine("123 Main St");

        r2 = new AddressResponse();
        r2.setId(20L);
        r2.setLine("456 Side Rd");
    }

    @Nested
    @DisplayName("GET /api/addresses")
    class GetAll {
        @Test
        @DisplayName("200 – returns full ListResponse")
        void success() {
            ListResponse<AddressResponse> listResp = ListResponse.of(List.of(r1, r2), null);
            given(addressService.findAll(0, 5, null, null, null, false))
                .willReturn(listResp);

            ResponseEntity<ListResponse<AddressResponse>> resp =
                controller.getAllResources(0, 5, null, null, null, false);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertSame(listResp, resp.getBody());
            then(addressService).should()
                .findAll(0, 5, null, null, null, false);
        }
    }

    @Nested
    @DisplayName("GET /api/addresses/{id}")
    class GetOne {
        @Test
        @DisplayName("200 – returns the AddressResponse")
        void found() {
            given(addressService.findById(10L)).willReturn(r1);

            ResponseEntity<AddressResponse> resp =
                controller.getResource(10L);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(10L, resp.getBody().getId());
            then(addressService).should().findById(10L);
        }

        @Test
        @DisplayName("500 – propagates when not found")
        void notFound() {
            given(addressService.findById(99L))
                .willThrow(new RuntimeException("Not found"));

            assertThrows(RuntimeException.class,
                () -> controller.getResource(99L));
            then(addressService).should().findById(99L);
        }
    }

    @Nested
    @DisplayName("POST /api/addresses")
    class Create {
        @Test
        @DisplayName("201 – returns created AddressResponse")
        void create() {
            given(addressService.save(eq(payload), eq(AddressRequest.class)))
                .willReturn(r1);

            ResponseEntity<AddressResponse> resp =
                controller.createResource(payload);

            assertEquals(HttpStatus.CREATED, resp.getStatusCode());
            assertEquals(10L, resp.getBody().getId());
            then(addressService).should()
                .save(payload, AddressRequest.class);
        }
    }

    @Nested
    @DisplayName("PUT /api/addresses/{id}")
    class Update {
        @Test
        @DisplayName("200 – returns updated AddressResponse")
        void update() {
            given(addressService.save(eq(10L), eq(payload), eq(AddressRequest.class)))
                .willReturn(r2);

            ResponseEntity<AddressResponse> resp =
                controller.updateResource(10L, payload);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(20L, resp.getBody().getId());
            then(addressService).should()
                .save(10L, payload, AddressRequest.class);
        }
    }

    @Nested
    @DisplayName("DELETE /api/addresses/{id}")
    class Delete {
        @Test
        @DisplayName("204 – no content on success")
        void deleteSuccess() {
            willDoNothing().given(addressService).delete(10L);

            ResponseEntity<Void> resp =
                controller.deleteResource(10L);

            assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
            then(addressService).should().delete(10L);
        }

        @Test
        @DisplayName("500 – propagates when not found")
        void deleteNotFound() {
            willThrow(new RuntimeException("Not found"))
                .given(addressService).delete(99L);

            assertThrows(RuntimeException.class,
                () -> controller.deleteResource(99L));
            then(addressService).should().delete(99L);
        }
    }
}
