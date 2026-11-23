package dev.bugbounty.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bugbounty.lab.controller.AuthController;
import dev.bugbounty.lab.config.LabMode;
import dev.bugbounty.lab.service.ContentPolicy;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {"spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:lab;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "lab.seed=true", "lab.jwt-secret=test-only-secure-secret-at-least-32-characters",
        "lab.webhook-secret=test-only-webhook-secret-at-least-32-chars"})
@AutoConfigureMockMvc
@Transactional
abstract class ProfileBehaviorTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired LabMode mode;
    @Autowired ContentPolicy content;

    private Cookie login(String username, String password) throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new AuthController.LoginRequest(username, password))))
                .andExpect(status().isOk()).andReturn();
        return result.getResponse().getCookie("LAB_TOKEN");
    }

    private MockHttpServletRequestBuilder adminWrite(String path, Cookie admin) throws Exception {
        var request = post(path).cookie(admin);
        if (!mode.vulnerable()) {
            var tokenResponse = mvc.perform(get("/api/auth/csrf").cookie(admin))
                    .andExpect(status().isOk()).andReturn().getResponse();
            request.cookie(tokenResponse.getCookie("XSRF-TOKEN"));
            request.header("X-XSRF-TOKEN", json.readTree(tokenResponse.getContentAsString()).get("token").asText());
        }
        return request;
    }

    @Test
    void debugUsersAreExposedOnlyByVulnerableConfiguration() throws Exception {
        var response = mvc.perform(get("/api/lab/debug/users"))
                .andExpect(mode.vulnerable() ? status().isOk() : status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        if (mode.vulnerable()) assertThat(response).contains("alice@example.test");
    }

    @Test
    void newLabAdministrationEndpointsRejectResearcher() throws Exception {
        Cookie alice = login("alice", "alice-demo-123");
        mvc.perform(get("/api/admin/lab/components").cookie(alice)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/lab/security-events").cookie(alice)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/lab/integration-events").cookie(alice)).andExpect(status().isForbidden());
    }

    @Test
    void untrustedComponentIsApprovedOnlyInVulnerableMode() throws Exception {
        Cookie admin = login("admin", "admin-demo-123");
        String untrusted = "{\"name\":\"demo-parser\",\"version\":\"0.1.0\","
                + "\"source\":\"https://untrusted.example.test/parser\",\"sha256\":\"" + "0".repeat(64)
                + "\",\"content\":\"untrusted local component text\"}";
        mvc.perform(adminWrite("/api/admin/lab/components", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(untrusted))
                .andExpect(mode.vulnerable() ? status().isCreated() : status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM approved_component", Integer.class))
                .isEqualTo(mode.vulnerable() ? 1 : 0);
    }

    @Test
    void trustedComponentCanBeApprovedInBothModes() throws Exception {
        Cookie admin = login("admin", "admin-demo-123");
        String fixture = mvc.perform(get("/api/admin/lab/component-fixture").cookie(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(adminWrite("/api/admin/lab/components", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture))
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM approved_component", Integer.class)).isEqualTo(1);
    }

    @Test
    void changedComponentBytesCannotReuseTrustedDigestInSecureMode() throws Exception {
        Cookie admin = login("admin", "admin-demo-123");
        String fixture = mvc.perform(get("/api/admin/lab/component-fixture").cookie(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var tampered = json.readTree(fixture);
        ((com.fasterxml.jackson.databind.node.ObjectNode) tampered).put("content", "changed local component");
        mvc.perform(adminWrite("/api/admin/lab/components", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(tampered)))
                .andExpect(mode.vulnerable() ? status().isCreated() : status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM approved_component", Integer.class))
                .isEqualTo(mode.vulnerable() ? 1 : 0);
    }

    @Test
    void newReportCannotBeClosedWithoutTriageOnlyInSecureMode() throws Exception {
        long reportId = jdbc.queryForObject("SELECT id FROM report WHERE title = 'Bob report'", Long.class);
        Cookie admin = login("admin", "admin-demo-123");
        mvc.perform(adminWrite("/api/admin/lab/reports/" + reportId + "/close", admin))
                .andExpect(mode.vulnerable() ? status().isOk() : status().isConflict());
        assertThat(jdbc.queryForObject("SELECT status FROM report WHERE id = ?", String.class, reportId))
                .isEqualTo(mode.vulnerable() ? "CLOSED" : "NEW");
    }

    @Test
    void normalTriageThenCloseWorksInBothModes() throws Exception {
        long reportId = jdbc.queryForObject("SELECT id FROM report WHERE title = 'Bob report'", Long.class);
        Cookie admin = login("admin", "admin-demo-123");
        mvc.perform(adminWrite("/api/admin/lab/reports/" + reportId + "/triage", admin))
                .andExpect(status().isOk());
        mvc.perform(adminWrite("/api/admin/lab/reports/" + reportId + "/close", admin))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM report WHERE id = ?", String.class, reportId))
                .isEqualTo("CLOSED");
    }

    @Test
    void unsignedWebhookIsAcceptedOnlyInVulnerableMode() throws Exception {
        long reportId = jdbc.queryForObject("SELECT id FROM report WHERE title = 'Bob report'", Long.class);
        String payload = "{\"reportId\":" + reportId + ",\"decision\":\"TRIAGED\"}";
        mvc.perform(post("/api/lab/webhook/triage").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(mode.vulnerable() ? status().isAccepted() : status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM integration_event", Integer.class))
                .isEqualTo(mode.vulnerable() ? 1 : 0);
        assertThat(jdbc.queryForObject("SELECT status FROM report WHERE id = ?", String.class, reportId))
                .isEqualTo(mode.vulnerable() ? "TRIAGED" : "NEW");
    }

    @Test
    void signedWebhookWorksInBothModes() throws Exception {
        long reportId = jdbc.queryForObject("SELECT id FROM report WHERE title = 'Bob report'", Long.class);
        String payload = "{\"reportId\":" + reportId + ",\"decision\":\"TRIAGED\"}";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-only-webhook-secret-at-least-32-chars"
                .getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        mvc.perform(post("/api/lab/webhook/triage").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Lab-Signature", signature).content(payload))
                .andExpect(status().isAccepted());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM integration_event", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM report WHERE id = ?", String.class, reportId))
                .isEqualTo("TRIAGED");
    }

    @Test
    void adminPromotionCreatesAuditAndAlertOnlyInSecureMode() throws Exception {
        long bobId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'bob'", Long.class);
        Cookie admin = login("admin", "admin-demo-123");
        mvc.perform(adminWrite("/api/admin/users/" + bobId + "/role", admin).param("role", "ADMIN"))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM security_event", Integer.class))
                .isEqualTo(mode.vulnerable() ? 0 : 2);
        String events = mvc.perform(get("/api/admin/lab/security-events").cookie(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        if (mode.vulnerable()) assertThat(events).isEqualTo("[]");
        else assertThat(events).contains("ROLE_CHANGED", "ROLE_PROMOTION_ALERT");
    }

    @Test
    void reviewOutageFailsClosedOnlyInSecureMode() throws Exception {
        long reportId = jdbc.queryForObject("SELECT id FROM report WHERE title = 'Bob report'", Long.class);
        Cookie admin = login("admin", "admin-demo-123");
        mvc.perform(adminWrite("/api/admin/lab/reports/" + reportId + "/triage", admin))
                .andExpect(status().isOk());
        mvc.perform(adminWrite("/api/admin/lab/reports/" + reportId + "/close", admin)
                        .param("simulateReviewFailure", "true"))
                .andExpect(mode.vulnerable() ? status().isOk() : status().isServiceUnavailable());
        assertThat(jdbc.queryForObject("SELECT status FROM report WHERE id = ?", String.class, reportId))
                .isEqualTo(mode.vulnerable() ? "CLOSED" : "TRIAGED");
    }

    @Test
    void sqlInjectionChangesSearchResultOnlyInVulnerableMode() throws Exception {
        var admin = login("admin", "admin-demo-123");
        var ordinary = mvc.perform(get("/api/reports/search").param("q", "no-such-title-2468")
                .cookie(admin)).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(ordinary.getResponse().getContentAsString()).size()).isZero();

        var result = mvc.perform(get("/api/reports/search").param("q", "' OR 1=1 --")
                .cookie(admin)).andExpect(status().isOk()).andReturn();
        JsonNode rows = json.readTree(result.getResponse().getContentAsString());
        assertThat(rows.size()).isEqualTo(mode.vulnerable() ? 2 : 0);
    }

    @Test
    void reportAndCommentsRespectOwnershipOnlyInSecureMode() throws Exception {
        long bobReport = jdbc.queryForObject("SELECT id FROM report WHERE title = 'Bob report'", Long.class);
        var alice = login("alice", "alice-demo-123");
        mvc.perform(get("/api/reports/{id}", bobReport).cookie(alice))
                .andExpect(mode.vulnerable() ? status().isOk() : status().isNotFound());
        mvc.perform(get("/api/reports/{id}/comments", bobReport).cookie(alice))
                .andExpect(mode.vulnerable() ? status().isOk() : status().isNotFound());
    }

    @Test
    void storedMarkupIsPreservedOrCleanedOnWrite() {
        String input = "hello <img src=x onerror=alert(1)>";
        String output = content.onWrite(input);
        assertThat(output.contains("onerror")).isEqualTo(mode.vulnerable());
    }

    @Test
    void roleChangeWithoutCsrfTokenIsProfileDependent() throws Exception {
        long aliceId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'alice'", Long.class);
        mvc.perform(post("/api/admin/users/{id}/role", aliceId).param("role", "TRIAGER")
                        .cookie(login("admin", "admin-demo-123")))
                .andExpect(mode.vulnerable() ? status().isNoContent() : status().isForbidden());
    }

    @Test
    void validCsrfTokenKeepsAdminActionWorking() throws Exception {
        long aliceId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'alice'", Long.class);
        Cookie admin = login("admin", "admin-demo-123");
        var tokenResponse = mvc.perform(get("/api/auth/csrf").cookie(admin))
                .andExpect(status().isOk()).andReturn().getResponse();
        var request = post("/api/admin/users/{id}/role", aliceId).param("role", "TRIAGER").cookie(admin);
        if (!mode.vulnerable()) {
            request.cookie(tokenResponse.getCookie("XSRF-TOKEN"));
            request.header("X-XSRF-TOKEN", json.readTree(tokenResponse.getContentAsString()).get("token").asText());
        }
        mvc.perform(request).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT role FROM app_user WHERE id = ?", String.class, aliceId))
                .isEqualTo("TRIAGER");
    }

    @Test
    void commentWriteStoresOrRemovesExecutableMarkup() throws Exception {
        long aliceReport = jdbc.queryForObject("SELECT id FROM report WHERE title = 'Alice report'", Long.class);
        Cookie alice = login("alice", "alice-demo-123");
        var tokenResponse = mvc.perform(get("/api/auth/csrf").cookie(alice))
                .andExpect(status().isOk()).andReturn().getResponse();
        var request = post("/api/reports/{id}/comments", aliceReport).cookie(alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"<img src=x onerror=alert(1)>\"}");
        if (!mode.vulnerable()) {
            request.cookie(tokenResponse.getCookie("XSRF-TOKEN"));
            request.header("X-XSRF-TOKEN", json.readTree(tokenResponse.getContentAsString()).get("token").asText());
        }
        mvc.perform(request).andExpect(status().isCreated());
        String saved = jdbc.queryForObject("SELECT body FROM report_comment WHERE report_id = ? ORDER BY id DESC LIMIT 1",
                String.class, aliceReport);
        assertThat(saved.contains("onerror")).isEqualTo(mode.vulnerable());
    }

    @Test
    void repeatedFailuresAreLimitedOnlyInSecureMode() throws Exception {
        for (int attempt = 1; attempt <= 6; attempt++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"rate-limit-probe\",\"password\":\"bad\"}"))
                    .andExpect(!mode.vulnerable() && attempt == 6
                            ? status().isTooManyRequests() : status().isUnauthorized());
        }
    }

    @Test
    void authenticationErrorsAndHashesDiffer() throws Exception {
        String unknown = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nobody\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String wrong = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(unknown.equals(wrong)).isEqualTo(!mode.vulnerable());
        String hash = jdbc.queryForObject("SELECT password_hash FROM app_user WHERE username = 'alice'", String.class);
        assertThat(Pattern.matches("[0-9a-f]{32}", hash)).isEqualTo(mode.vulnerable());
        var token = login("alice", "alice-demo-123").getValue();
        var chunks = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(chunks[1]));
        assertThat(payload.contains("\"exp\"")).isEqualTo(!mode.vulnerable());
    }

    @Test
    void predictableLabKeyOnlyForgesVulnerableAdminSession() throws Exception {
        long adminId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'admin'", Long.class);
        var predictableKey = Keys.hmacShaKeyFor("local-lab-predictable-secret-0000"
                .getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder().subject(Long.toString(adminId))
                .signWith(predictableKey, Jwts.SIG.HS256).compact();
        mvc.perform(get("/api/admin/users").cookie(new Cookie("LAB_TOKEN", forged)))
                .andExpect(mode.vulnerable() ? status().isOk() : status().isUnauthorized());
    }

    @Test
    void userResponsesNeverExposePasswordHashes() throws Exception {
        Cookie admin = login("admin", "admin-demo-123");
        String hash = jdbc.queryForObject("SELECT password_hash FROM app_user WHERE username = 'admin'", String.class);
        String users = mvc.perform(get("/api/admin/users").cookie(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String me = mvc.perform(get("/api/auth/me").cookie(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(users).doesNotContain(hash, "passwordHash");
        assertThat(me).doesNotContain(hash, "passwordHash");
    }

    @Test
    void contentSecurityPolicyIsPresentOnlyInSecureMode() throws Exception {
        var response = mvc.perform(get("/api/meta")).andExpect(status().isOk()).andReturn().getResponse();
        String policy = response.getHeader("Content-Security-Policy");
        if (mode.vulnerable()) {
            assertThat(policy).isNull();
        } else {
            assertThat(policy).contains("script-src 'self'").doesNotContain("unsafe-inline");
        }
    }

    @Test
    void validOwnerAccessStillWorks() throws Exception {
        long aliceReport = jdbc.queryForObject("SELECT id FROM report WHERE title = 'Alice report'", Long.class);
        mvc.perform(get("/api/reports/{id}", aliceReport).cookie(login("alice", "alice-demo-123")))
                .andExpect(status().isOk());
    }

    @Test
    void ordinarySearchRespectsResearcherScope() throws Exception {
        var result = mvc.perform(get("/api/reports/search").cookie(login("alice", "alice-demo-123")))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString()).size())
                .isEqualTo(mode.vulnerable() ? 2 : 1);
    }

    @Test
    void researcherCanSubmitAReport() throws Exception {
        Cookie alice = login("alice", "alice-demo-123");
        var tokenResponse = mvc.perform(get("/api/auth/csrf").cookie(alice))
                .andExpect(status().isOk()).andReturn().getResponse();
        var request = post("/api/reports").cookie(alice).contentType(MediaType.APPLICATION_JSON)
                .content("{\"programId\":1,\"title\":\"New example\",\"description\":\"Test body\",\"severity\":\"LOW\"}");
        if (!mode.vulnerable()) {
            request.cookie(tokenResponse.getCookie("XSRF-TOKEN"));
            request.header("X-XSRF-TOKEN", json.readTree(tokenResponse.getContentAsString()).get("token").asText());
        }
        mvc.perform(request).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM report WHERE title = 'New example'", Integer.class))
                .isEqualTo(1);
    }
}
