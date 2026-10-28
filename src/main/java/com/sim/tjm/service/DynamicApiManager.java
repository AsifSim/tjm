package com.sim.tjm.service;

import com.sim.tjm.model.ApiEndpoint;
import com.sim.tjm.model.AppConfiguration;
import com.sim.tjm.repo.AppConfigurationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DynamicApiManager {
    private static final Logger logger = LoggerFactory.getLogger(DynamicApiManager.class);

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private AppConfigurationRepository configRepository;

    @Autowired
    private UnifiedUvmService unifiedUvmService;

    private final Map<String, RouteContext> activeRoutes = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeExistingApis() {
        List<AppConfiguration> configs = configRepository.findAll();
        for (AppConfiguration config : configs) {
            if (config.getApis() != null) {
                config.getApis().forEach((apiRef, api) -> {
                    registerEndpoint(api.getApiUrl(), api.getMethod(), config.getAppId(), apiRef);
                });
            }
        }
        logger.info("[DYNAMIC_API] Loaded {} active dynamic routes from database.", activeRoutes.size());
    }

    public void registerEndpoint(String urlPath, String httpMethod, String appId, String apiRef) {
        String cleanPath = urlPath.startsWith("/") ? urlPath : "/" + urlPath;

        try {
            RequestMappingInfo mappingInfo = RequestMappingInfo
                    .paths(cleanPath)
                    .methods(RequestMethod.valueOf(httpMethod.toUpperCase()))
                    .options(new RequestMappingInfo.BuilderConfiguration())
                    .build();

            Method targetMethod = DynamicApiManager.class.getMethod("handleDynamicRequest", HttpServletRequest.class, Map.class);

            handlerMapping.unregisterMapping(mappingInfo);
            handlerMapping.registerMapping(mappingInfo, this, targetMethod);

            activeRoutes.put(cleanPath, new RouteContext(appId, apiRef));
            logger.info("[DYNAMIC_API] Successfully activated endpoint: {} [{}]", httpMethod, cleanPath);

        } catch (NoSuchMethodException e) {
            logger.error("Failed to register dynamic endpoint", e);
        }
    }

    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleDynamicRequest(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> payload) {

        String path = request.getRequestURI();
        RouteContext context = activeRoutes.get(path);

        if (context == null) {
            return ResponseEntity.notFound().build();
        }

        AppConfiguration config = configRepository.findById(context.appId).orElseThrow();
        ApiEndpoint targetApi = config.getApis().get(context.apiRef);

        String mode = (targetApi.getTjmSequence() != null && targetApi.getTjmSequence().getSourceTrigger() != null)
                ? targetApi.getTjmSequence().getSourceTrigger() : "CONSOLE";

        Map<String, Object> tjmResult = unifiedUvmService.executeTjmSequence(context.appId, targetApi, payload, mode);

        Map<String, Object> protocolResponse = new LinkedHashMap<>();
        protocolResponse.put("mode", mode);
        protocolResponse.put("status", "SUCCESS");
        protocolResponse.put("data", tjmResult);
        protocolResponse.put("trace_id", UUID.randomUUID().toString());
        protocolResponse.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(protocolResponse);
    }

    private record RouteContext(String appId, String apiRef) {}

    public boolean isDynamicRoute(String path) {
        return activeRoutes.containsKey(path);
    }
}