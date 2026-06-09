package com.mathewshereni.open_aid_transparency.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetches the official Sustainable Development Goals from the UN SDG API. The
 * response is a flat JSON array of {code, title, description, uri} objects.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UnSdgClient {

    private final RestClient unSdgRestClient;

    public List<UnSdgGoal> fetchGoals() {
        JsonNode root = unSdgRestClient.get()
                .uri("/v1/sdg/Goal/List")
                .retrieve()
                .body(JsonNode.class);

        if (root == null || !root.isArray()) {
            throw new IllegalStateException("Unexpected response shape from the UN SDG API");
        }

        List<UnSdgGoal> goals = new ArrayList<>();
        for (JsonNode node : root) {
            String code = textOrNull(node.get("code"));
            if (code == null) {
                continue;
            }
            try {
                goals.add(new UnSdgGoal(
                        Integer.parseInt(code.trim()),
                        textOrNull(node.get("title")),
                        textOrNull(node.get("description"))));
            } catch (NumberFormatException notAGoalNumber) {
                // skip anything whose code isn't a plain 1..17
            }
        }

        log.info("UN SDG API returned {} goals", goals.size());
        return goals;
    }

    private String textOrNull(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.asText();
    }
}
