package dev.bugbounty.lab;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("vulnerable")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:vulnerable;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class VulnerableProfileTest extends ProfileBehaviorTest {}
