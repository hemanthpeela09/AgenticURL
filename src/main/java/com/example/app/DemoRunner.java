package com.example.app;

import com.example.orchestrator.core.ApprovalStrategy;
import com.example.orchestrator.scenario.RunResult;
import com.example.orchestrator.scenario.Scenario;
import com.example.orchestrator.scenario.ScenarioRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.example.orchestrator.llm.LlmFactory;

/**
 * CLI demo: `./gradlew bootRun --args='--scenario=greenfield'` runs a scenario
 * prints the run summary as JSON, and exists. Without --scenario the app starts
 * normally as a web server.
 */
@Component
public class DemoRunner implements ApplicationRunner {
    private final ConfigurableApplicationContext context;

    @Autowired
    public DemoRunner(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("scenario")) {
            return;
        }
        String key = args.getOptionValues("scenario").get(0);
        String approval = args.containsOption("approval")
                ? args.getOptionValues("approval").get(0)
                : "auto";

        ScenarioRunner runner = new ScenarioRunner(LlmFactory.fromEnv(), (String k, java.util.Map<String, Object> d) -> {});
        var strategy = "deny".equalsIgnoreCase(approval)
                ? ApprovalStrategy.denyAll()
                : ApprovalStrategy.autoApprove();
        RunResult result = runner.run(Scenario.fromKey(key), strategy);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.findAndRegisterModules();
        System.out.println("=== Orchestrator run: " + key + " ===");
        System.out.println(mapper.writeValueAsString(result));

        int exit = result.completed() ? 0 : 2;
        System.exit(org.springframework.boot.SpringApplication.exit(context, () -> exit));
    }
}
