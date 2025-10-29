package org.ulinda.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.ulinda.dto.ErrorDetailDto;
import org.ulinda.dto.GetErrorsResponse;
import org.ulinda.entities.ErrorLog;
import org.ulinda.exceptions.ErrorResponse;
import org.ulinda.repositories.ErrorRepository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ErrorService {

    @Autowired
    private ErrorRepository errorRepository;

    /**
     * Save error asynchronously - doesn't block the response
     */
    @Async
    public void saveErrorAsync(Exception e, ErrorResponse er) {
        try {
            ErrorLog error = new ErrorLog();
            error.setMessage(e.getMessage());
            error.setErrorCode(er.getErrorCode());
            error.setStackTrace(getStackTraceAsString(e));
            error.setErrorIdentifier(er.getErrorIdentifier());
            errorRepository.save(error);
            log.debug("Error log saved asynchronously with ID: {}", error.getId());

        } catch (Exception ex) {
            // Never let database save errors break the exception handling
            log.error("Failed to save error log asynchronously: {}", ex.getMessage(), ex);
        }
    }

    private String getStackTraceAsString(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public Page<ErrorLog> getErrors(Pageable pageable) {
        Page<ErrorLog> errors = errorRepository.findAll(pageable);
        return errors;
    }

    public ErrorDetailDto getErrorDetail(UUID errorIdentfier) {
        ErrorLog error = errorRepository.findByErrorIdentifier(errorIdentfier);
        if (error == null) {
            throw new RuntimeException("No error found with ID: " + errorIdentfier);
        }
        ErrorDetailDto errorDetail = new ErrorDetailDto();
        errorDetail.setErrorIdentifier(error.getErrorIdentifier());
        errorDetail.setTimestamp(error.getTimestamp());
        errorDetail.setMessage(error.getMessage());
        errorDetail.setStackTrace(error.getStackTrace());
        return errorDetail;
    }
}