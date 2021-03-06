package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager;

import java.util.List;
import java.util.StringJoiner;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClouderaManagerProductV4Response implements JsonEntity {

    @NotNull
    @ApiModelProperty(ModelDescriptions.ClouderaManagerProductDescription.NAME)
    private String name;

    @NotNull
    @ApiModelProperty(ModelDescriptions.ClouderaManagerProductDescription.DISPLAY_NAME)
    private String displayName;

    @ApiModelProperty(ModelDescriptions.ClouderaManagerProductDescription.VERSION)
    private String version;

    @ApiModelProperty(ModelDescriptions.ClouderaManagerProductDescription.PARCEL)
    private String parcel;

    @ApiModelProperty(ModelDescriptions.ClouderaManagerProductDescription.CSD)
    private List<String> csd;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getParcel() {
        return parcel;
    }

    public void setParcel(String parcel) {
        this.parcel = parcel;
    }

    public List<String> getCsd() {
        return csd;
    }

    public void setCsd(List<String> csd) {
        this.csd = csd;
    }

    public ClouderaManagerProductV4Response withName(String name) {
        this.name = name;
        return this;
    }

    public ClouderaManagerProductV4Response withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public ClouderaManagerProductV4Response withVersion(String version) {
        this.version = version;
        return this;
    }

    public ClouderaManagerProductV4Response withParcel(String parcel) {
        this.parcel = parcel;
        return this;
    }

    public ClouderaManagerProductV4Response withCsd(List<String> csd) {
        this.csd = csd;
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClouderaManagerProductV4Response.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("version='" + version + "'")
                .add("parcel='" + parcel + "'")
                .add("csd=" + csd)
                .toString();
    }
}
