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
public class ModelController {

    @Autowired
    private ModelService modelService;

    @Autowired
    private AuthenticationHelper authenticationHelper;

    @GetMapping("/models")
    public ResponseEntity<GetModelsResponse> getModels(Authentication authentication) {
        UUID userId = authenticationHelper.getUserId(authentication);
        GetModelsResponse response = modelService.getModels(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/models/{modelId}")
    public ResponseEntity<GetModelResponse> getModel(@PathVariable("modelId") UUID modelId, Authentication authentication) {
        UUID userId = authenticationHelper.getUserId(authentication);
        GetModelResponse response = modelService.getModel(modelId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/models/{modelId}/records")
    public ResponseEntity<UUID> createRecord(@PathVariable UUID modelId,
                                             @RequestBody @Valid CreateRecordRequest request,
                                             Authentication authentication) {
        UUID userId = authenticationHelper.getUserId(authentication);
        UUID recordId = modelService.createRecord(userId, modelId, request.getFieldValues());
        return ResponseEntity.ok(recordId);
    }

    @PutMapping("/records/{modelId}/{recordId}")
    public ResponseEntity<RecordDto> updateRecord(@PathVariable UUID recordId,
                                                  @PathVariable UUID modelId,
                                                  @RequestBody @Valid UpdateRecordRequest request,
                                                  Authentication authentication) {
        UUID userId = authenticationHelper.getUserId(authentication);
        RecordDto updatedRecord = modelService.updateRecord(userId, modelId, recordId, request.getFieldValues());
        return ResponseEntity.ok(updatedRecord);
    }

    @GetMapping("/records/{modelId}/{recordId}")
    public ResponseEntity<RecordDto> getRecord(@PathVariable UUID recordId,
                                               @PathVariable UUID modelId,
                                               Authentication authentication) {
        UUID userId = authenticationHelper.getUserId(authentication);
        RecordDto record = modelService.getRecord(userId, modelId, recordId);
        return ResponseEntity.ok(record);
    }

    @DeleteMapping("/records/{modelId}/{recordId}")
    public void deleteRecord(@PathVariable UUID recordId,
                             @PathVariable UUID modelId,
                             @RequestParam(defaultValue = "false") boolean overrideLinkedModelsError,
                             Authentication authentication) {
        UUID userId = authenticationHelper.getUserId(authentication);
        modelService.deleteRecord(userId, modelId, recordId, overrideLinkedModelsError);
    }

    @PostMapping("/models/{modelId}/records/search")
    public ResponseEntity<GetRecordsResponse> getRecords(
            @PathVariable UUID modelId,
            @Valid @RequestBody GetRecordsRequest request,
            Authentication authentication) {
        UUID userId = authenticationHelper.getUserId(authentication);
        GetRecordsResponse response = modelService.getRecords(userId, request, modelId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/model/linked-records/{modelLinkId}/{linkId}")
    public void deleteLink(@PathVariable UUID modelLinkId, @PathVariable UUID linkId, Authentication authentication) {
        UUID userId = authenticationHelper.getUserId(authentication);
        modelService.deleteRecordLink(userId, modelLinkId, linkId);
    }

    @PostMapping("/records/link-records")
    public void linkRecords(@Valid @RequestBody LinkRecordsRequest request, Authentication authentication) {
        UUID userId = authenticationHelper.getUserId(authentication);
        modelService.linkRecords(userId, request);
    }
}
