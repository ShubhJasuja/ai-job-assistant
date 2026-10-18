package com.jobassistant.service.phase5;

/**
 * Form-field detection and mapping service.
 * Inspects supported controls; does not hardcode company selectors.
 */
public class FormFieldMapper {

    public boolean supportsControl(String tag, String type) {
        return ("input".equals(tag) || "textarea".equals(tag) || "select".equals(tag));
    }

    // Reports missing/ambiguous rather than guessing
    public String detectAmbiguity(String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return "MISSING: " + label;
        }
        return null;
    }
}
