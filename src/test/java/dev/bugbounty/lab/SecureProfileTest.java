package dev.bugbounty.lab;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("secure")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:secure;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class SecureProfileTest extends ProfileBehaviorTest {}
