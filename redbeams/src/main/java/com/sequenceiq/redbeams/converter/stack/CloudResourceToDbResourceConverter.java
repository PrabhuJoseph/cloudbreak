package com.sequenceiq.redbeams.converter.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.redbeams.domain.stack.DBResource;

@Component
public class CloudResourceToDbResourceConverter extends AbstractConversionServiceAwareConverter<CloudResource, DBResource> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourceToDbResourceConverter.class);

    @Override
    public DBResource convert(CloudResource source) {
        DBResource dbResource = new DBResource();
        dbResource.setResourceName(source.getName());
        dbResource.setResourceStatus(source.getStatus());
        dbResource.setResourceType(source.getType());
        dbResource.setResourceReference(source.getReference());
        return dbResource;
    }

}
