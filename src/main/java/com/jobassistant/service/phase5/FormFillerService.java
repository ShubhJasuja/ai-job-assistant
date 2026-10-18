package com.jobassistant.service.phase5;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic form-filling service using mapped fields.
 */
public class FormFillerService {

    private final FormFieldMapper mapper = new FormFieldMapper();

    public Map<String, String> fillSupported(Map<String, String> profile, Map<String, String> answers) {
        Map<String, String> result = new HashMap<>(answers);
        for (String key : profile.keySet()) {
            if (!answers.containsKey(key) || answers.get(key) == null || answers.get(key).trim().isEmpty()) {
                result.put(key, "UNRESOLVED"); // report missing, do not invent
            }
        }
        return result;
    }

    public boolean isVerified(String label, String value) {
        return mapper.detectAmbiguity(label, value) == null;
    }
}
