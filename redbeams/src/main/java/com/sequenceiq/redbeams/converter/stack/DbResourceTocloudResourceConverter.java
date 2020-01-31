package com.sequenceiq.redbeams.converter.stack;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.redbeams.domain.stack.DBResource;

@Component
public class DbResourceTocloudResourceConverter  extends AbstractConversionServiceAwareConverter<DBResource, CloudResource> {
    @Override
    public CloudResource convert(DBResource source) {
        return new Builder()
                .type(source.getResourceType())
                .name(source.getResourceName())
                .reference(source.getResourceReference())
                .status(source.getResourceStatus())
                .build();
    }
}
