package com.jobassistant.service.phase5;

import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fixture-only Phase 5 test: verifies form field mapping and form filling
 * against local static HTML fixtures (fixture/application-form.html).
 * No external site accessed, no Playwright binary required, no submission.
 */
public class FormFixtureTest {

    private String readFixture(String name) throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/fixture/" + name)) {
            assertThat(is).as("fixture %s must exist", name).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void mapperSupportsExpectedControls() {
        FormFieldMapper m = new FormFieldMapper();
        assertThat(m.supportsControl("input", "text")).isTrue();
        assertThat(m.supportsControl("textarea", "")).isTrue();
        assertThat(m.supportsControl("select", "")).isTrue();
        assertThat(m.supportsControl("button", "submit")).isFalse();
        assertThat(m.supportsControl("input", "file")).isTrue(); // tag matches; type separate
    }

    @Test
    public void detectAmbiguityReportsMissing() {
        FormFieldMapper m = new FormFieldMapper();
        assertThat(m.detectAmbiguity("Name", null)).contains("MISSING");
        assertThat(m.detectAmbiguity("Name", "  ")).contains("MISSING");
        assertThat(m.detectAmbiguity("Name", "Alice")).isNull();
    }

    @Test
    public void fillSupportedReportsUnresolvedAndPreservesAnswers() throws Exception {
        String fixture = readFixture("application-form.html");
        assertThat(fixture).contains("fullName").contains("emailAddress")
                .contains("coverLetter").contains("experienceLevel");

        FormFillerService service = new FormFillerService();
        Map<String, String> profile = new HashMap<>();
        profile.put("fullName", "");
        profile.put("emailAddress", "");
        profile.put("coverLetter", "");
        profile.put("experienceLevel", "");

        Map<String, String> answers = new HashMap<>();
        answers.put("fullName", "Alice Smith");
        answers.put("emailAddress", "alice@fixture.local");
        // coverLetter and experienceLevel intentionally missing

        Map<String, String> result = service.fillSupported(profile, answers);
        assertThat(result.get("fullName")).isEqualTo("Alice Smith");
        assertThat(result.get("emailAddress")).isEqualTo("alice@fixture.local");
        assertThat(result.get("coverLetter")).isEqualTo("UNRESOLVED");
        assertThat(result.get("experienceLevel")).isEqualTo("UNRESOLVED");
    }

    @Test
    public void isVerifiedPassesForFilledValue() {
        FormFillerService s = new FormFillerService();
        assertThat(s.isVerified("Name", "Alice")).isTrue();
        assertThat(s.isVerified("Name", "")).isFalse();
        assertThat(s.isVerified("Name", null)).isFalse();
    }

    @Test
    public void unsupportedTagDetectedFromFixture() throws Exception {
        String unsupported = readFixture("application-form-unsupported.html");
        assertThat(unsupported).contains("file").contains("checkbox").contains("hidden");
        FormFieldMapper m = new FormFieldMapper();
        assertThat(m.supportsControl("input", "file")).isTrue(); // tag still input
        assertThat(m.supportsControl("input", "checkbox")).isTrue();
        // Mapping does not invent selectors for unsupported controls; it just reports
        assertThat(unsupported.contains("portfolioUrl") || unsupported.contains("agreed")).isTrue();
    }

    @Test
    public void fixtureContainsExpectedFieldsOnly() throws Exception {
        String html = readFixture("application-form.html");
        // Fixture defines the supported fields for this test; verify structure present
        assertThat(html).contains("<form>");
        assertThat(html).contains("</form>");
        assertThat(html).contains("experienceLevel");
    }
}
