package com.example.agentswarm.llm;

// Every agent talks to this interface, never to a specific vendor SDK.
// Swap GeminiProvider for GroqProvider/OtherProvider without touching agent code.
public interface LLMProvider {

    /**
     * Send a prompt and get back raw text. Callers that need structured data
     * should instruct the model (via the prompt) to return JSON only, then
     * parse the result themselves - this keeps the provider layer dumb and swappable.
     */
    String generate(String systemPrompt, String userPrompt);

    String name();
}
