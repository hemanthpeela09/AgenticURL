package com.example.orchestrator.llm;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LangChain-style prompt templates.
 * Supports variable interpolation and template composition.
 */
public class PromptTemplate {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{(\\w+)\\}");

    private final String template;

    public PromptTemplate(String template) {
        this.template = template;
    }

    /**
     * Format the template with provided variables.
     * Variables in the template should be in the format {variableName}.
     *
     * @param variables Map of variable names to values
     * @return Formatted prompt string
     */
    public String format(Map<String, Object> variables) {
        String result = template;
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = variables.get(varName);
            if (value != null) {
                result = result.replace("{" + varName + "}", value.toString());
            }
        }

        return result;
    }

    /**
     * Create a new template by composing this template with another.
     *
     * @param other Template to append
     * @return New composed template
     */
    public PromptTemplate compose(PromptTemplate other) {
        return new PromptTemplate(this.template + "\n\n" + other.template);
    }

    /**
     * Get the raw template string.
     */
    public String getTemplate() {
        return template;
    }

    // ---- Pre-built templates for SDLC agents ----

    public static final PromptTemplate ANALYST = new PromptTemplate("""
            You are a requirements analyst. Analyze the following requirement and:
            1. Identify the main intent
            2. List functional requirements
            3. List non-functional requirements
            4. Identify any ambiguities that need clarification

            Requirement: {requirement}

            Provide your analysis in a structured format.
            """);

    public static final PromptTemplate ARCHITECT = new PromptTemplate("""
            You are a software architect. Based on the following requirements specification:

            {requirement_spec}

            Design a system architecture that includes:
            1. Component breakdown
            2. API endpoints
            3. Data flows
            4. Technology choices with justification

            If this is a brownfield change to an existing system, also provide:
            - Impact analysis
            - Breaking changes
            - Migration strategy
            """);

    public static final PromptTemplate IMPLEMENTER = new PromptTemplate("""
            You are a senior software engineer. Based on the design specification:

            {design_spec}

            Create an implementation plan that includes:
            1. Module structure
            2. Class hierarchy
            3. Key interfaces
            4. Implementation notes

            Focus on clean, testable, maintainable code.
            """);

    public static final PromptTemplate TEST_AUTHOR = new PromptTemplate("""
            You are a QA engineer. Based on the design specification:

            {design_spec}

            Create a comprehensive test plan including:
            1. Unit test cases
            2. Integration test cases
            3. Edge cases and boundary conditions
            4. Expected code coverage targets
            """);

    public static final PromptTemplate DOC_WRITER = new PromptTemplate("""
            You are a technical writer. Document the following system:

            Design: {design_spec}
            Implementation: {code_manifest}
            Tests: {test_plan}

            Create documentation covering:
            1. Overview
            2. API reference
            3. Setup guide
            4. Testing guide
            5. Known limitations
            """);

    public static final PromptTemplate RELEASE_MANAGER = new PromptTemplate("""
            You are a release manager. Assess release readiness based on:

            Tests: {test_report}
            Documentation: {docs}

            Provide:
            1. Release readiness assessment
            2. Risk analysis
            3. Deployment checklist
            4. Rollback plan
            """);
}