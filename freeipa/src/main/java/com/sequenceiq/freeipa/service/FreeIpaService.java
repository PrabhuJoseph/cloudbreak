package com.sequenceiq.freeipa.service;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.model.freeipa.FreeIpaRequest;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.converter.FreeIpaRequestToFreeIpaConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.FreeIpaRepository;

@Service
public class FreeIpaService {

    @Inject
    private FreeIpaRepository repository;

    @Inject
    private FreeIpaRequestToFreeIpaConverter freeIpaConverter;

    public FreeIpa save(FreeIpa freeIpa) {
        return repository.save(freeIpa);
    }

    public FreeIpa findByStack(Stack stack) {
        return repository.getByStack(stack).orElseThrow(() -> new NotFoundException(String.format("FreeIpa not found for Stack [%s]", stack.getId())));
    }

    public FreeIpa findByStackId(Long stackId) {
        return repository.getByStackId(stackId).orElseThrow(() -> new NotFoundException(String.format("FreeIpa not found for Stack [%s]", stackId)));
    }

    public FreeIpa create(Stack stack, FreeIpaRequest request) {
        FreeIpa freeIpa = freeIpaConverter.convert(request);
        freeIpa.setStack(stack);
        return save(freeIpa);
    }
}
