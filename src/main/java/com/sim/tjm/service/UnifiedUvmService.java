package com.sim.tjm.service;

import com.sim.tjm.model.*;
import com.sim.tjm.repo.AppConfigurationRepository;
import com.sim.tjm.repo.EntityStoreRepository;
import com.sim.tjm.repo.TransactionAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UnifiedUvmService {
    private static final Logger logger = LoggerFactory.getLogger(UnifiedUvmService.class);

    @Autowired
    private AppConfigurationRepository appRepository;

    @Autowired
    private TransactionAuditRepository auditRepo;

    @Autowired
    private EntityStoreRepository entityStoreRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${tml.controller.host:http://localhost}")
    private String tmlHost;

    @Value("${tml.controller.port:8087}")
    private String tmlPort;

    @Value("${tml.controller.read-endpoint:/spriced/platform/getData}")
    private String tmlReadEndpoint;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<Map<String, Object>> getAvailableEntities() {
        List<Map<String, Object>> entityList = new ArrayList<>();

        try {
            List<EntityStore> activeEntities = entityStoreRepo.findByIsEnabledTrueAndIsActiveTrue();

            for (EntityStore store : activeEntities) {
                Map<String, Object> uiEntity = new LinkedHashMap<>();
                uiEntity.put("entityName", store.getEntityName());

                List<String> attrNames = new ArrayList<>();
                String payloadStr = store.getPayload();

                if (payloadStr != null && !payloadStr.isBlank()) {
                    JsonNode payload = objectMapper.readTree(payloadStr);
                    if (payload.isArray() && !payload.isEmpty()) {
                        JsonNode firstElement = payload.get(0);
                        JsonNode attributesList = firstElement.get("attributesList");

                        if (attributesList != null && attributesList.isArray()) {
                            for (JsonNode attr : attributesList) {
                                if (attr.has("name")) {
                                    attrNames.add(attr.get("name").asText());
                                }
                            }
                        }
                    }
                }

                uiEntity.put("attributes", attrNames);
                entityList.add(uiEntity);
            }
        } catch (Exception e) {
            logger.error("[DEAM_DB_ERROR] Failed to parse entity payloads from entity_store table", e);
        }

        return entityList;
    }

    public Map<String, Object> executeTjmSequence(String appId, ApiEndpoint api, Map<String, Object> payload, String sourceTrigger) {
        String transactionId = UUID.randomUUID().toString();
        TjmSequence tjm = api.getTjmSequence();

        String actualSource = (sourceTrigger != null) ? sourceTrigger : (tjm != null && tjm.getSourceTrigger() != null ? tjm.getSourceTrigger() : "CONSOLE");
        logAndSaveAudit(transactionId, appId, api.getApiUrl(), actualSource, "TJM_START", "Initialized");

        Map<String, Object> activeContextVariables = new LinkedHashMap<>();
        if (tjm != null && tjm.getInputParameters() != null && !tjm.getInputParameters().isEmpty()) {
            for (String key : tjm.getInputParameters().split(",")) {
                String k = key.trim();
                if (payload != null && payload.containsKey(k)) {
                    activeContextVariables.put(k, payload.get(k));
                }
            }
        }

        Map<String, Object> executionState = new LinkedHashMap<>();
        executionState.put("API_INPUTS", new LinkedHashMap<>(activeContextVariables));

        if (tjm != null && tjm.getSteps() != null) {
            processStepRecursively(tjm.getSteps(), 0, executionState, activeContextVariables, api, transactionId, appId, actualSource);
        }

        Map<String, Object> formattedOutput = new LinkedHashMap<>();
        String outputEntity = (tjm != null) ? tjm.getFinalOutputEntity() : null;
        String outputCol = (tjm != null) ? tjm.getFinalOutputColumn() : null;

        if (outputEntity != null && !outputEntity.isEmpty() && outputCol != null) {
            List<Map<String, Object>> finalFilters = buildExplicitFilters(tjm.getFinalResponseMappings(), activeContextVariables);
            Object finalData = executeTmlControllerFetch(outputEntity, new String[]{outputCol}, finalFilters);
            formattedOutput.put(outputCol, finalData);
        } else {
            formattedOutput = executionState;
        }

        logAndSaveAudit(transactionId, appId, api.getApiUrl(), actualSource, "TJM_COMPLETE", outputCol);
        return formattedOutput;
    }

    private void processStepRecursively(
            List<TjmStep> steps, int currentIndex, Map<String, Object> currentState,
            Map<String, Object> activeContextVariables, ApiEndpoint api, String txId, String appId, String source) {

        if (steps == null || currentIndex >= steps.size()) return;

        TjmStep currentStep = steps.get(currentIndex);
        logAndSaveAudit(txId, appId, api.getApiUrl(), source, currentStep.getStepType(), currentStep.getTargetEntity());

        String[] colArray = (currentStep.getTargetColumns() != null) ? currentStep.getTargetColumns().split(",") : new String[]{"*"};
        List<Map<String, Object>> explicitFilters = buildExplicitFilters(currentStep.getStepMappings(), activeContextVariables);

        if ("FABRIC_FETCH".equals(currentStep.getStepType())) {
            Object fetchedData = executeTmlControllerFetch(currentStep.getTargetEntity(), colArray, explicitFilters);
            currentState.put(currentStep.getTargetEntity(), fetchedData);

            // --- Aggressive Data Unwrapping ---
            Map<String, Object> firstRow = null;

            if (fetchedData instanceof List && !((List<?>) fetchedData).isEmpty()) {
                Object item = ((List<?>) fetchedData).get(0);
                if (item instanceof Map) firstRow = (Map<String, Object>) item;
            } else if (fetchedData instanceof Map) {
                Map<String, Object> mapResponse = (Map<String, Object>) fetchedData;

                // Check if it's wrapped in a standard enterprise 'data' or 'content' array
                if (mapResponse.containsKey("data") && mapResponse.get("data") instanceof List) {
                    List<?> list = (List<?>) mapResponse.get("data");
                    if (!list.isEmpty() && list.get(0) instanceof Map) {
                        firstRow = (Map<String, Object>) list.get(0);
                    }
                } else if (mapResponse.containsKey("content") && mapResponse.get("content") instanceof List) {
                    List<?> list = (List<?>) mapResponse.get("content");
                    if (!list.isEmpty() && list.get(0) instanceof Map) {
                        firstRow = (Map<String, Object>) list.get(0);
                    }
                } else if (mapResponse.containsKey("error")) {
                    logger.error("[TJM_ENGINE] TML Controller returned an error: {}", mapResponse.get("error"));
                } else {
                    // Fallback: The TML controller returned a direct Object. Treat the Map itself as the row.
                    firstRow = mapResponse;
                }
            }

            // Map the extracted row back into the active context pool for subsequent steps
            if (firstRow != null && !firstRow.isEmpty()) {
                for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
                    activeContextVariables.put(currentStep.getTargetEntity() + "." + entry.getKey(), entry.getValue());
                }
                logger.info("[TJM_ENGINE] Mapped intermediate variables to context: {}", firstRow.keySet());
            } else {
                logger.warn("[TJM_ENGINE] Step 1 returned no parseable rows. Raw response: {}", fetchedData);
            }
        }

        processStepRecursively(steps, currentIndex + 1, currentState, activeContextVariables, api, txId, appId, source);
    }

    private List<Map<String, Object>> buildExplicitFilters(List<Map<String, String>> mappings, Map<String, Object> activeContextVariables) {
        List<Map<String, Object>> filters = new ArrayList<>();
        if (mappings != null) {
            for (Map<String, String> mapping : mappings) {
                Object runtimeValue = activeContextVariables.get(mapping.get("sourceInput"));
                if (runtimeValue != null) {
                    Map<String, Object> filter = new LinkedHashMap<>();
                    filter.put("attributeName", mapping.get("targetCol"));
                    filter.put("operator", mapping.get("operator"));
                    filter.put("attributeValue", runtimeValue);
                    filters.add(filter);
                }
            }
        }

        // Apply logical operator rule: "AND" for all except the last condition
        for (int i = 0; i < filters.size(); i++) {
            if (i == filters.size() - 1) {
                filters.get(i).put("logicalOperator", "");
            } else {
                filters.get(i).put("logicalOperator", "AND");
            }
        }

        return filters;
    }

    private Object executeTmlControllerFetch(String entityName, String[] columns, List<Map<String, Object>> explicitFilters) {
        String tmlUrl = String.format("%s:%s%s", tmlHost, tmlPort, tmlReadEndpoint);
        Map<String, Object> tmlRequestParams = new LinkedHashMap<>();
        tmlRequestParams.put("entityName", entityName);
        tmlRequestParams.put("attributesList", Arrays.asList(columns));
        tmlRequestParams.put("filterList", explicitFilters);

        try {
            logger.info("[TML_CALL] Request to {}: {}", tmlUrl, tmlRequestParams);
            ResponseEntity<Object> response = restTemplate.postForEntity(tmlUrl, tmlRequestParams, Object.class);
            return (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) ? response.getBody() : Map.of("error", "Non-200 status");
        } catch (Exception e) {
            logger.error("[TML_ERROR] Failed to fetch data from TML Controller", e);
            return Map.of("error", "TML connection failed: " + e.getMessage());
        }
    }

    private void logAndSaveAudit(String txId, String appId, String apiRef, String source, String action, String target) {
        TransactionAudit audit = new TransactionAudit();
        audit.setTransactionId(txId);
        audit.setAppId(appId);
        audit.setApiRef(apiRef);
        audit.setSourceTrigger(source);
        audit.setActionType(action);
        audit.setTarget(target);
        auditRepo.save(audit);
    }
}