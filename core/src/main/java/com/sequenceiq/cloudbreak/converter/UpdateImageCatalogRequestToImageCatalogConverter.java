package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.UpdateImageCatalogV4Request;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@Component
public class UpdateImageCatalogRequestToImageCatalogConverter extends AbstractConversionServiceAwareConverter<UpdateImageCatalogV4Request, ImageCatalog> {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Override
    public ImageCatalog convert(UpdateImageCatalogV4Request source) {
        ImageCatalog original = imageCatalogService.findByResourceCrn(source.getCrn());
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(source.getUrl());
        imageCatalog.setName(source.getName());
        imageCatalog.setId(original.getId());
        imageCatalog.setCrn(source.getCrn());
        imageCatalog.setCreator(original.getCreator());
        return imageCatalog;
    }

}
