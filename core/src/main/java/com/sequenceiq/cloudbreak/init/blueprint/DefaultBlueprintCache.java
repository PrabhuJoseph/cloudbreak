package com.sequenceiq.cloudbreak.init.blueprint;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.converter.BlueprintRequestToBlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintInputParameters;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintUtils;

@Service
public class DefaultBlueprintCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBlueprintCache.class);

    @Value("#{'${cb.blueprint.defaults:}'.split(';')}")
    private List<String> blueprintArray;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private BlueprintRequestToBlueprintConverter converter;

    private Map<String, Blueprint> defaultBlueprints = new HashMap<>();

    @PostConstruct
    public void loadBlueprintsFromFile() {
        for (String blueprintStrings : blueprintArray) {
            try {
                String[] split = blueprintStrings.split("=");
                if (blueprintUtils.isBlueprintNamePreConfigured(blueprintStrings, split)) {
                    LOGGER.info("Load default blueprint '{}'.", blueprintStrings);
                    BlueprintRequest blueprintJson = new BlueprintRequest();
                    blueprintJson.setName(split[0].trim());
                    JsonNode jsonNode = blueprintUtils.convertStringToJsonNode(blueprintUtils.readDefaultBlueprintFromFile(split));
                    blueprintJson.setAmbariBlueprint(jsonNode.get("blueprint").toString());
                    Blueprint bp = converter.convert(blueprintJson);
                    JsonNode inputs = jsonNode.get("inputs");
                    JsonNode description = jsonNode.get("description");
                    bp.setDescription(description == null ? split[0] : description.asText(split[0]));
                    BlueprintInputParameters inputParameters = new BlueprintInputParameters(blueprintUtils.prepareInputs(inputs));
                    bp.setInputParameters(new Json(inputParameters));
                    defaultBlueprints.put(bp.getName(), bp);
                }
            } catch (IOException e) {
                LOGGER.info("Can not read default blueprint from file: ", e);
            }
        }
    }

    public Map<String, Blueprint> defaultBlueprints() {
        Map<String, Blueprint> result = new HashMap<>();
        defaultBlueprints.entrySet().stream().forEach(e -> result.put(e.getKey(), SerializationUtils.clone(e.getValue())));
        return result;
    }

    public List<String> blueprintArray() {
        return blueprintArray;
    }
}
