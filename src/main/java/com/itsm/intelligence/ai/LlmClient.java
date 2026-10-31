package com.itsm.intelligence.ai;

/**
 * Adapter target: the only view the rest of the application has of a large-language-model
 * provider. Swapping Gemini for another provider means writing one new implementation.
 */
public interface LlmClient {

    /** True when the provider is configured and enabled. */
    boolean isAvailable();

    String providerName();

    /**
     * Sends a prompt and returns the model's reply, which the prompt asks to be JSON.
     *
     * @throws com.itsm.exception.ExternalServiceException on network or provider failure
     */
    String generateJson(String prompt);
}
