package com.sequenceiq.freeipa.converter.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.usersync.UserSyncStatusResponse;
import com.sequenceiq.freeipa.converter.authentication.StackAuthenticationToStackAuthenticationResponseConverter;
import com.sequenceiq.freeipa.converter.freeipa.FreeIpaToFreeIpaServerResponseConverter;
import com.sequenceiq.freeipa.converter.image.ImageToImageSettingsResponseConverter;
import com.sequenceiq.freeipa.converter.instance.InstanceGroupToInstanceGroupResponseConverter;
import com.sequenceiq.freeipa.converter.network.NetworkToNetworkResponseConverter;
import com.sequenceiq.freeipa.converter.telemetry.TelemetryConverter;
import com.sequenceiq.freeipa.converter.usersync.UserSyncStatusToUserSyncStatusResponseConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Image;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackAuthentication;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.entity.UserSyncStatus;

@ExtendWith(MockitoExtension.class)
class StackToDescribeFreeIpaResponseConverterTest {

    private static final String ENV_CRN = "envCrn";

    private static final String NAME = "freeIpa";

    private static final String RESOURCE_CRN = "crn1";

    private static final String CHILD_ENVIRONMENT_CRN = "crn:child";

    private static final String CLOUD_PLATFORM = "AWS";

    private static final StackAuthenticationResponse STACK_AUTHENTICATION_RESPONSE = new StackAuthenticationResponse();

    private static final ImageSettingsResponse IMAGE_SETTINGS_RESPONSE = new ImageSettingsResponse();

    private static final InstanceGroupResponse INSTANCE_GROUP_RESPONSE = new InstanceGroupResponse();

    private static final List<InstanceGroupResponse> INSTANCE_GROUP_RESPONSES = List.of(INSTANCE_GROUP_RESPONSE);

    private static final UserSyncStatusResponse USERSYNC_STATUS_RESPONSE = new UserSyncStatusResponse();

    private static final Status STATUS = Status.AVAILABLE;

    private static final String STATUS_REASON = "Because reasons";

    private static final String APP_VERSION = "appVersion";

    private static final String STATUS_STRING = "Status string";

    private static final String DOMAIN = "example.com";

    private static final String FREEIPA_HOST = "freeipa.example.com";

    private static final int GATEWAY_PORT = 8080;

    private static final String SERVER_IP = "1.1.1.1";

    @InjectMocks
    private StackToDescribeFreeIpaResponseConverter underTest;

    @Mock
    private StackAuthenticationToStackAuthenticationResponseConverter authenticationResponseConverter;

    @Mock
    private ImageToImageSettingsResponseConverter imageSettingsResponseConverter;

    @Mock
    private FreeIpaToFreeIpaServerResponseConverter freeIpaServerResponseConverter;

    @Mock
    private NetworkToNetworkResponseConverter networkResponseConverter;

    @Mock
    private InstanceGroupToInstanceGroupResponseConverter instanceGroupConverter;

    @Mock
    private TelemetryConverter telemetryConverter;

    @Mock
    private UserSyncStatusToUserSyncStatusResponseConverter userSyncStatusConverter;

    @BeforeAll
    static void initInstanceGroupResponse() {
        InstanceMetaDataResponse instanceMetaDataResponse = new InstanceMetaDataResponse();
        instanceMetaDataResponse.setPrivateIp(SERVER_IP);
        INSTANCE_GROUP_RESPONSE.setMetaData(Set.of(instanceMetaDataResponse));
    }

    @Test
    void convert() {
        FreeIpaServerResponse freeIpaServerResponse = new FreeIpaServerResponse();
        Stack stack = createStack();
        Image image = new Image();
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        when(authenticationResponseConverter.convert(stack.getStackAuthentication())).thenReturn(STACK_AUTHENTICATION_RESPONSE);
        when(imageSettingsResponseConverter.convert(image)).thenReturn(IMAGE_SETTINGS_RESPONSE);
        when(freeIpaServerResponseConverter.convert(freeIpa)).thenReturn(freeIpaServerResponse);
        when(instanceGroupConverter.convert(stack.getInstanceGroups())).thenReturn(INSTANCE_GROUP_RESPONSES);
        when(userSyncStatusConverter.convert(userSyncStatus)).thenReturn(USERSYNC_STATUS_RESPONSE);

        DescribeFreeIpaResponse result = underTest.convert(stack, image, freeIpa, userSyncStatus);

        assertThat(result)
                .returns(NAME, DescribeFreeIpaResponse::getName)
                .returns(ENV_CRN, DescribeFreeIpaResponse::getEnvironmentCrn)
                .returns(RESOURCE_CRN, DescribeFreeIpaResponse::getCrn)
                .returns(CLOUD_PLATFORM, DescribeFreeIpaResponse::getCloudPlatform)
                .returns(STACK_AUTHENTICATION_RESPONSE, DescribeFreeIpaResponse::getAuthentication)
                .returns(IMAGE_SETTINGS_RESPONSE, DescribeFreeIpaResponse::getImage)
                .returns(freeIpaServerResponse, DescribeFreeIpaResponse::getFreeIpa)
                // TODO placement
                .returns(INSTANCE_GROUP_RESPONSES, DescribeFreeIpaResponse::getInstanceGroups)
                .returns(STATUS, DescribeFreeIpaResponse::getStatus)
                .returns(STATUS_REASON, DescribeFreeIpaResponse::getStatusReason)
                .returns(STATUS_STRING, DescribeFreeIpaResponse::getStatusString)
                // TODO decorateFreeIpaServerResponseWithIps
                .returns(APP_VERSION, DescribeFreeIpaResponse::getAppVersion)
                // TODO decorateWithCloudStorgeAndTelemetry
                .returns(USERSYNC_STATUS_RESPONSE, DescribeFreeIpaResponse::getUserSyncStatus);

        assertThat(freeIpaServerResponse)
                .returns(Set.of(SERVER_IP), FreeIpaServerResponse::getServerIp)
                .returns(FREEIPA_HOST, FreeIpaServerResponse::getFreeIpaHost)
                .returns(GATEWAY_PORT, FreeIpaServerResponse::getFreeIpaPort);
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENV_CRN);
        stack.setName(NAME);
        stack.setResourceCrn(RESOURCE_CRN);
        stack.setStackStatus(createStackStatus());
        stack.setCloudPlatform(CLOUD_PLATFORM);
        stack.setStackAuthentication(new StackAuthentication());
        stack.setAppVersion(APP_VERSION);
        stack.setGatewayport(GATEWAY_PORT);
        return stack;
    }

    private StackStatus createStackStatus() {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(STATUS);
        stackStatus.setStatusString(STATUS_STRING);
        stackStatus.setStatusReason(STATUS_REASON);
        return stackStatus;
    }
}