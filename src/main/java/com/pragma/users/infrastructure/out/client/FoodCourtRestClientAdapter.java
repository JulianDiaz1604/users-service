package com.pragma.users.infrastructure.out.client;

import com.pragma.users.domain.exception.EmployeeAlreadyExistsException;
import com.pragma.users.domain.exception.RestaurantNotFoundException;
import com.pragma.users.domain.spi.IEmployeeRestaurantLinkPort;
import com.pragma.users.domain.spi.IRestaurantOwnershipPort;
import com.pragma.users.infrastructure.out.client.dto.EmployeeLinkRequestDto;
import com.pragma.users.infrastructure.out.client.dto.OwnershipResponseDto;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Infrastructure adapter implementing both food-court SPI ports over a shared {@link RestClient}.
 * <ul>
 *   <li>{@code GET /api/v1/restaurants/{restaurantId}/owner} — ownership lookup</li>
 *   <li>{@code POST /api/v1/restaurants/{restaurantId}/employees} — link registration</li>
 * </ul>
 * The owner JWT is forwarded to food-court via {@link AuthorizationForwardingInterceptor},
 * which must be registered on the {@link RestClient} at construction time.
 */
public class FoodCourtRestClientAdapter implements IRestaurantOwnershipPort, IEmployeeRestaurantLinkPort {

    private static final String OWNER_PATH = "/api/v1/restaurants/{restaurantId}/owner";
    private static final String EMPLOYEES_PATH = "/api/v1/restaurants/{restaurantId}/employees";

    private final RestClient restClient;

    public FoodCourtRestClientAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Calls {@code GET /api/v1/restaurants/{restaurantId}/owner} and returns the ownerId.
     *
     * @throws RestaurantNotFoundException if food-court responds with 404
     */
    @Override
    public UUID getOwnerId(UUID restaurantId) {
        OwnershipResponseDto response = restClient.get()
                .uri(OWNER_PATH, restaurantId)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        (req, res) -> { throw new RestaurantNotFoundException(); })
                .body(OwnershipResponseDto.class);

        if (response == null) {
            throw new RestaurantNotFoundException();
        }
        return response.ownerId();
    }

    /**
     * Calls {@code POST /api/v1/restaurants/{restaurantId}/employees} with the given employeeId.
     *
     * @throws EmployeeAlreadyExistsException if food-court responds with 409
     * @throws RuntimeException               for any other non-2xx response (caller must compensate)
     */
    @Override
    public void registerEmployee(UUID restaurantId, UUID employeeId) {
        restClient.post()
                .uri(EMPLOYEES_PATH, restaurantId)
                .body(new EmployeeLinkRequestDto(employeeId))
                .retrieve()
                .onStatus(status -> status.value() == 409,
                        (req, res) -> { throw new EmployeeAlreadyExistsException(); })
                .toBodilessEntity();
    }
}
