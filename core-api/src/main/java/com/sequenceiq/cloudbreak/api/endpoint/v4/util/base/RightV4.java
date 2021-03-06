package com.sequenceiq.cloudbreak.api.endpoint.v4.util.base;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.exception.NotFoundException;

public enum RightV4 {
    ENV_CREATE(AuthorizationResourceAction.CREATE_ENVIRONMENT),
    ENV_START(AuthorizationResourceAction.START_ENVIRONMENT),
    ENV_STOP(AuthorizationResourceAction.STOP_ENVIRONMENT),
    ENV_DELETE(AuthorizationResourceAction.DELETE_ENVIRONMENT),
    DH_CREATE(AuthorizationResourceAction.CREATE_DATAHUB),
    DH_START(AuthorizationResourceAction.START_DATAHUB),
    DH_STOP(AuthorizationResourceAction.STOP_DATAHUB),
    DH_DELETE(AuthorizationResourceAction.DELETE_DATAHUB),
    // legacy
    DISTROX_READ(AuthorizationResourceAction.DATAHUB_READ),
    DISTROX_WRITE(AuthorizationResourceAction.DATAHUB_WRITE),
    SDX_READ(AuthorizationResourceAction.DATALAKE_READ),
    SDX_WRITE(AuthorizationResourceAction.DATALAKE_WRITE),
    ENVIRONMENT_READ(AuthorizationResourceAction.ENVIRONMENT_READ),
    ENVIRONMENT_WRITE(AuthorizationResourceAction.ENVIRONMENT_WRITE);

    private static final Map<AuthorizationResourceAction, RightV4> BY_ACTION = Stream.of(RightV4.values())
            .collect(Collectors.toUnmodifiableMap(RightV4::getAction, Function.identity()));

    private AuthorizationResourceAction action;

    RightV4(AuthorizationResourceAction action) {
        this.action = action;
    }

    public AuthorizationResourceAction getAction() {
        return action;
    }

    public static RightV4 getByAction(AuthorizationResourceAction authorizationResourceAction) {
        RightV4 result = BY_ACTION.get(authorizationResourceAction);
        if (result == null) {
            throw new NotFoundException(String.format("Action %s not found in this enum", authorizationResourceAction));
        }
        return result;
    }
}
