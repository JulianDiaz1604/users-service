package com.pragma.users.domain.spi;

import java.util.UUID;

/**
 * Driven port: queries the restaurant service to retrieve the owner of a given restaurant.
 * Throws {@link com.pragma.users.domain.exception.RestaurantNotFoundException} when the
 * restaurant does not exist.
 */
public interface IRestaurantOwnershipPort {

    /**
     * Returns the ownerId for the given restaurant.
     *
     * @param restaurantId the UUID of the restaurant to look up
     * @return the UUID of the owner
     * @throws com.pragma.users.domain.exception.RestaurantNotFoundException if the restaurant
     *         does not exist in the restaurant service
     */
    UUID getOwnerId(UUID restaurantId);
}
