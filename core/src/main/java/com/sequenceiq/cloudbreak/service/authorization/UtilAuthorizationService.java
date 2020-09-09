package com.sequenceiq.cloudbreak.service.authorization;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.google.api.client.util.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.UmsRightProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.base.RightV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.CheckResourceRightsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.CheckRightV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CheckResourceRightV4SingleResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CheckResourceRightsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CheckRightV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CheckRightV4SingleResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

@Service
public class UtilAuthorizationService {

    @Inject
    private UmsRightProvider umsRightProvider;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public CheckRightV4Response getRightResult(CheckRightV4Request rightReq) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<AuthorizationProto.RightCheck> rightChecks = rightReq.getRights().stream()
                .map(rightV4 -> createRightCheckObject(umsRightProvider.getRight(rightV4.getAction()), null))
                .collect(Collectors.toList());
        List<Boolean> results = callHasRightsBasedOnEntitlement(userCrn, rightChecks);
        return new CheckRightV4Response(rightReq.getRights().stream()
                .map(rightV4 -> new CheckRightV4SingleResponse(rightV4,
                        rightV4.isLegacyRight() ? Boolean.TRUE : results.get(rightReq.getRights().indexOf(rightV4))))
                .collect(Collectors.toList()));
    }

    public CheckResourceRightsV4Response getResourceRightsResult(CheckResourceRightsV4Request checkResourceRightsV4Request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<AuthorizationProto.RightCheck> rightChecks = Lists.newArrayList();
        checkResourceRightsV4Request.getResourceRights().stream()
                .forEach(resourceRightsV4 -> resourceRightsV4.getRights().stream()
                    .forEach(rightV4 ->  rightChecks.add(createRightCheckObject(rightV4.getAction().getRight(), resourceRightsV4.getResourceCrn()))));
        List<Boolean> results = callHasRightsBasedOnEntitlement(userCrn, rightChecks);
        return generateResponse(rightChecks, results);
    }

    private List<Boolean> callHasRightsBasedOnEntitlement(String userCrn, List<AuthorizationProto.RightCheck> rightChecks) {
        if (grpcUmsClient.isAuthorizationEntitlementRegistered(userCrn, ThreadBasedUserCrnProvider.getAccountId())) {
            return grpcUmsClient.hasRights(userCrn, userCrn, rightChecks, MDCUtils.getRequestId());
        }
        return rightChecks.stream().map(rightCheck -> Boolean.TRUE).collect(Collectors.toList());
    }

    private CheckResourceRightsV4Response generateResponse(List<AuthorizationProto.RightCheck> rightChecks, List<Boolean> results) {
        CheckResourceRightsV4Response response = new CheckResourceRightsV4Response(Lists.newArrayList());
        rightChecks.stream().forEach(rightCheck -> {
            if (!getResourceRightSingleResponse(response, rightCheck).isPresent()) {
                response.getResponses().add(new CheckResourceRightV4SingleResponse(rightCheck.getResource(), Lists.newArrayList()));
            }
            RightV4 rightV4ByAction = RightV4.getByAction(AuthorizationResourceAction.getByRight(rightCheck.getRight()));
            Boolean result = rightV4ByAction.isLegacyRight() ? Boolean.TRUE : results.get(rightChecks.indexOf(rightCheck));
            CheckRightV4SingleResponse singleResponse = new CheckRightV4SingleResponse(
                    rightV4ByAction,
                    result);
            getResourceRightSingleResponse(response, rightCheck).get().getRights().add(singleResponse);
        });
        return response;
    }

    private Optional<CheckResourceRightV4SingleResponse> getResourceRightSingleResponse(CheckResourceRightsV4Response response,
            AuthorizationProto.RightCheck rightCheck) {
        return response.getResponses().stream()
                .filter(singleResponseStream -> StringUtils.equals(singleResponseStream.getResourceCrn(), rightCheck.getResource()))
                .findFirst();
    }

    private AuthorizationProto.RightCheck createRightCheckObject(String right, String resource) {
        AuthorizationProto.RightCheck.Builder builder = AuthorizationProto.RightCheck.newBuilder()
                .setRight(right);
        if (resource != null) {
            builder.setResource(resource);
        }
        return builder.build();
    }
}
