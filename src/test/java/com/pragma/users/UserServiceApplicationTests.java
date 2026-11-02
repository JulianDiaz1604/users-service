package com.pragma.users;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: this test passes if the Spring application context
        // starts without errors (bean wiring, configuration, datasource).
    }

}
