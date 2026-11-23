package dev.bugbounty.lab;

import dev.bugbounty.lab.config.LabMode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class LabModeTest {
    @Test
    void vulnerableModeRequiresLoopbackBinding() {
        var environment = new MockEnvironment().withProperty("server.address", "0.0.0.0");
        environment.setActiveProfiles("vulnerable");
        assertThatThrownBy(() -> new LabMode(environment))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("127.0.0.1");
        environment.setProperty("server.address", "127.0.0.1");
        assertThat(new LabMode(environment).vulnerable()).isTrue();
    }

    @Test
    void profileMustBeExplicitAndUnique() {
        var environment = new MockEnvironment().withProperty("server.address", "127.0.0.1");
        assertThatThrownBy(() -> new LabMode(environment)).isInstanceOf(IllegalStateException.class);
        environment.setActiveProfiles("vulnerable", "secure");
        assertThatThrownBy(() -> new LabMode(environment)).isInstanceOf(IllegalStateException.class);
    }
}
