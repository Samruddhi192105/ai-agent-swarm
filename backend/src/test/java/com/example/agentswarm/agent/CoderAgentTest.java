package com.example.agentswarm.agent;

import com.example.agentswarm.dto.CoderOutput;
import com.example.agentswarm.dto.PlannerSpec;
import com.example.agentswarm.llm.LLMProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoderAgentTest {

    @Test
        void generateRetriesValidationFailuresAndAcceptsMoreThanTwentyFiles() throws Exception {
        String malformed = """
                {"files":[
                  {"path":"pom.xml","content":"<project><modelVersion>4.0.0</modelVersion><dependencies><groupId>org.example</groupId></dependencies></project>"},
                  {"path":"Dockerfile","content":"FROM scratch"}
                ]}
                """;
        String overLimit = responseWithFileCount(41);
        String valid = responseWithFileCount(21);
        LLMProvider llm = mock(LLMProvider.class);
        when(llm.generate(anyString(), anyString())).thenReturn(malformed, overLimit, valid);
        CoderAgent coder = new CoderAgent(llm);
        PlannerSpec spec = new PlannerSpec("sample", List.of(), List.of(), List.of(),
                new PlannerSpec.Database("postgresql", List.of()), List.of());

        CoderOutput output = coder.generate(spec);

                assertEquals(21, output.files().size());
        ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
                verify(llm, org.mockito.Mockito.times(3)).generate(anyString(), prompts.capture());
        assertTrue(prompts.getAllValues().get(1).contains("unexpected <groupId>"));
                assertTrue(prompts.getAllValues().get(2).contains("Maximum allowed: 40"));
                assertTrue(prompts.getAllValues().get(2).contains("PREVIOUS OUTPUT VALIDATION ERROR"));
        }

        private String responseWithFileCount(int count) throws Exception {
                List<CoderOutput.GeneratedFileDto> files = new java.util.ArrayList<>();
                files.add(new CoderOutput.GeneratedFileDto("pom.xml", "<project/>"));
                files.add(new CoderOutput.GeneratedFileDto("Dockerfile", "FROM scratch"));
                for (int index = 2; index < count; index++) {
                        files.add(new CoderOutput.GeneratedFileDto("src/file-" + index + ".txt", "content"));
                }
                return new ObjectMapper().writeValueAsString(new CoderOutput(files));
    }
}