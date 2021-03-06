package com.sequenceiq.environment.api.v1.proxy.endpoint;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.proxy.ProxyConfigDescription;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponses;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/proxies")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/proxies", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface ProxyEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.LIST, produces = MediaType.APPLICATION_JSON, notes = ProxyConfigDescription.PROXY_CONFIG_NOTES,
            nickname = "listProxyConfigsV1")
    ProxyResponses list();

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.GET_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = ProxyConfigDescription.PROXY_CONFIG_NOTES, nickname = "getProxyConfigByNameV1")
    ProxyResponse getByName(@PathParam("name") String name);

    @GET
    @Path("account/{accountId}/name/{name}/crn")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.GET_CRN_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = ProxyConfigDescription.PROXY_CONFIG_NOTES, nickname = "getProxyConfigCrnByNameV1")
    String getCrnByAccountIdAndName(@AccountId @PathParam("accountId") String accountId, @PathParam("name") String name);

    @GET
    @Path("environment/crn/{environmentCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.GET_BY_ENVIRONMENT_CRN, produces = MediaType.APPLICATION_JSON,
            notes = ProxyConfigDescription.PROXY_CONFIG_NOTES, nickname = "getProxyConfigByEnvironmentCrnV1", httpMethod = "GET")
    ProxyResponse getByEnvironmentCrn(@PathParam("environmentCrn") String environmentCrn);

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.GET_BY_CRN, produces = MediaType.APPLICATION_JSON,
            notes = ProxyConfigDescription.PROXY_CONFIG_NOTES, nickname = "getProxyConfigByCrnV1")
    ProxyResponse getByResourceCrn(@PathParam("crn") String crn);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.CREATE, produces = MediaType.APPLICATION_JSON,
            notes = ProxyConfigDescription.PROXY_CONFIG_NOTES, nickname = "createProxyConfigV1")
    ProxyResponse post(@Valid ProxyRequest request);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.DELETE_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = ProxyConfigDescription.PROXY_CONFIG_NOTES, nickname = "deleteProxyConfigByNameV1")
    ProxyResponse deleteByName(@PathParam("name") String name);

    @DELETE
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.DELETE_BY_CRN, produces = MediaType.APPLICATION_JSON,
            notes = ProxyConfigDescription.PROXY_CONFIG_NOTES, nickname = "deleteProxyConfigByCrnV1")
    ProxyResponse deleteByCrn(@PathParam("crn") String crn);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.DELETE_MULTIPLE_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = ProxyConfigDescription.PROXY_CONFIG_NOTES, nickname = "deleteProxyConfigsV1")
    ProxyResponses deleteMultiple(Set<String> names);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.GET_REQUEST_BY_NAME,
            produces = MediaType.APPLICATION_JSON, notes = ProxyConfigDescription.PROXY_CONFIG_NOTES,
            nickname = "getProxyRequestFromNameV1")
    ProxyRequest getRequest(@PathParam("name") String name);
}
