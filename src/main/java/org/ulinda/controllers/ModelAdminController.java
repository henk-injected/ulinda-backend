package org.ulinda.controllers;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.ulinda.dto.*;
import org.ulinda.security.AuthenticationHelper;
import org.ulinda.services.ModelService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class ModelAdminController {

    @Autowired
    private ModelService modelService;

    @Autowired
    private AuthenticationHelper authenticationHelper;

    @PostMapping("/create-model")
    public ResponseEntity<String> createModel(@RequestBody @Valid CreateModelRequest request, Authentication authentication) {
        modelService.createModel(request, authenticationHelper.getUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body("Model created successfully");
    }

    @PostMapping("/models/link-models")
    public void linkModels(@Valid @RequestBody LinkModelsRequest linkModelsRequest) {
        modelService.linkModels(linkModelsRequest);
    }

    @PostMapping("/models/linked-models")
    public void updateLinkedModels(@Valid @RequestBody UpdateLinkedModelsRequest updateLinkedModelsRequest) {
        modelService.updatelinkModels(updateLinkedModelsRequest);
    }

    @DeleteMapping("/models/link-models")
    public void deleteModelLink(@Valid @RequestBody DeleteModelLinkRequest deleteModelLinkRequest) {
        modelService.deleteModelLink(deleteModelLinkRequest);
    }

    @PostMapping("/fields/{modelId}")
    public void createNewField(@PathVariable UUID modelId, @RequestBody @Valid FieldDto fieldDto) {
        modelService.addField(modelId, fieldDto);
    }

    @PostMapping("/fields/{fieldId}/duplicate")
    public ResponseEntity<FieldDto> duplicateField(
            @PathVariable UUID fieldId,
            @RequestBody @Valid DuplicateFieldRequest request) {
        FieldDto newField = modelService.duplicateField(fieldId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(newField);
    }

    @DeleteMapping("/fields/{fieldId}")
    public void deleteField(@PathVariable UUID fieldId) {
        modelService.deleteField(fieldId);
    }

    @PutMapping("/models/{modelId}")
    public void updateModel(@PathVariable UUID modelId, @Valid @RequestBody UpdateModelRequest updateModelRequest) {
        modelService.updateModel(modelId, updateModelRequest);
    }

    @PutMapping("/fields/{fieldId}")
    public void updateField(@PathVariable UUID fieldId, @Valid @RequestBody UpdateFieldRequest updateModelRequest) {
        modelService.updateField(fieldId, updateModelRequest);
    }

    @DeleteMapping("/models/{modelId}")
    public void deleteModel(@PathVariable UUID modelId, @RequestParam(defaultValue = "false") boolean force) {
        modelService.deleteModel(modelId, force);
    }

    @GetMapping("/models/link-models")
    public ResponseEntity<GetModelLinksResponse> getLinkedModels() {
        return ResponseEntity.ok(modelService.getModelLinks());
    }
}
