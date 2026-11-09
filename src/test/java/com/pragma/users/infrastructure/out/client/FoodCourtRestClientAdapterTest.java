package com.pragma.users.infrastructure.out.client;

import com.pragma.users.domain.exception.EmployeeAlreadyExistsException;
import com.pragma.users.domain.exception.RestaurantNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.springframework.http.HttpStatus;

class FoodCourtRestClientAdapterTest {

    private static final String BASE_URL = "http://food-court";
    private static final String AUTH_HEADER_VALUE = "Bearer test-owner-token";

    private MockRestServiceServer server;
    private FoodCourtRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        builder.requestInterceptor(new AuthorizationForwardingInterceptor());
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new FoodCourtRestClientAdapter(builder.build());

        // Set up inbound request with Authorization header in RequestContextHolder
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        server.verify();
    }

    @Test
    @DisplayName("EAC-S06: GET owner returns 404 → throws RestaurantNotFoundException")
    void whenGetOwnerReturns404ThenThrowsRestaurantNotFoundException() {
        UUID restaurantId = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/v1/restaurants/" + restaurantId + "/owner"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(RestaurantNotFoundException.class, () -> adapter.getOwnerId(restaurantId));
    }

    @Test
    @DisplayName("EAC-S01: GET owner returns 200 → returns ownerId UUID")
    void whenGetOwnerReturns200ThenReturnsOwnerId() {
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String responseJson = String.format(
                "{\"restaurantId\":\"%s\",\"ownerId\":\"%s\"}", restaurantId, ownerId);

        server.expect(requestTo(BASE_URL + "/api/v1/restaurants/" + restaurantId + "/owner"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        UUID result = adapter.getOwnerId(restaurantId);

        assertEquals(ownerId, result);
    }

    @Test
    @DisplayName("Authorization header is forwarded to food-court on GET owner request")
    void authorizationHeaderIsForwardedOnGetOwnerRequest() {
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String responseJson = String.format(
                "{\"restaurantId\":\"%s\",\"ownerId\":\"%s\"}", restaurantId, ownerId);

        server.expect(requestTo(BASE_URL + "/api/v1/restaurants/" + restaurantId + "/owner"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        adapter.getOwnerId(restaurantId);
        // server.verify() in @AfterEach confirms the expectation was met
    }

    @Test
    @DisplayName("EAC-S08 (link): POST employees returns 409 → throws EmployeeAlreadyExistsException")
    void whenPostEmployeeReturns409ThenThrowsEmployeeAlreadyExistsException() {
        UUID restaurantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/api/v1/restaurants/" + restaurantId + "/employees"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThrows(EmployeeAlreadyExistsException.class,
                () -> adapter.registerEmployee(restaurantId, employeeId));
    }

    @Test
    @DisplayName("Authorization header is forwarded to food-court on POST employees request")
    void authorizationHeaderIsForwardedOnRegisterEmployeeRequest() {
        UUID restaurantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/api/v1/restaurants/" + restaurantId + "/employees"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
                .andRespond(withStatus(HttpStatus.CREATED));

        adapter.registerEmployee(restaurantId, employeeId);
    }
}
