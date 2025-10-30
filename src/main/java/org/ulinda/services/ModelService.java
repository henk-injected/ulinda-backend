package org.ulinda.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ulinda.dto.*;
import org.ulinda.entities.Field;
import org.ulinda.entities.Model;
import org.ulinda.entities.ModelLink;
import org.ulinda.enums.FieldType;
import org.ulinda.enums.ModelPermission;
import org.ulinda.enums.QueryType;
import org.ulinda.enums.SearchFieldType;
import org.ulinda.exceptions.ErrorCode;
import org.ulinda.exceptions.FrontendException;
import org.ulinda.repositories.FieldRepository;
import org.ulinda.repositories.ModelLinkRepository;
import org.ulinda.repositories.ModelRepository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.ulinda.dto.CursorInfo;
import org.ulinda.utils.CursorUtils;

@Service
@Slf4j
public class ModelService {

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private FieldRepository fieldRepository;

    @Autowired
    private ModelLinkRepository modelLinkRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserService userService;

    @Transactional
    public void createModel(CreateModelRequest request, UUID ownerId) {
        Model model = new Model();
        model.setDescription(request.getDescription());
        model.setName(request.getName());
        model.setOwnerId(ownerId);
        modelRepository.save(model);

        UUID modelId = model.getId();
        if (modelId == null) {
            throw new RuntimeException("Model id is null");
        }

        for (FieldDto fieldDto : request.getFields()) {
            Field field = new Field();
            field.setDescription(fieldDto.getDescription());
            field.setName(fieldDto.getName());
            field.setIsRequired(fieldDto.getIsRequired());
            field.setType(fieldDto.getType());
            field.setModelId(modelId);
            fieldRepository.save(field);
        }

        createFieldTables(request, modelId);

    }

    private void createFieldTables(CreateModelRequest request, UUID modelId) {
        // Validate UUID format as extra safety
        if (modelId == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }

        String tableName = "records_" + sanitizeIdentifier(modelId.toString());

        StringBuilder createTableSql = new StringBuilder();
        createTableSql.append("CREATE TABLE ");
        appendQuotedIdentifier(createTableSql, tableName);
        createTableSql.append(" (");
        createTableSql.append("id UUID PRIMARY KEY DEFAULT gen_random_uuid(), ");
        createTableSql.append("created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, ");
        createTableSql.append("updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP");

        // Get the saved fields from database to ensure we have the UUIDs
        List<Field> savedFields = fieldRepository.findByModelId(modelId);

        for (Field field : savedFields) {
            if (field.getId() == null) {
                throw new IllegalStateException("Field ID cannot be null");
            }

            String fieldName = "field_" + sanitizeIdentifier(field.getId().toString());
            String columnType = mapFieldTypeToPostgresType(field.getType());

            createTableSql.append(", ");
            appendQuotedIdentifier(createTableSql, fieldName);
            createTableSql.append(" ").append(columnType);
        }

        createTableSql.append(")");

        log.debug("Creating table with SQL: {}", createTableSql.toString());
        jdbcTemplate.execute(createTableSql.toString());
    }

    private String sanitizeIdentifier(String identifier) {
        // Remove hyphens and ensure only alphanumeric and underscore
        return identifier.replaceAll("-", "_").replaceAll("[^a-zA-Z0-9_]", "");
    }

    private void appendQuotedIdentifier(StringBuilder sql, String identifier) {
        // PostgreSQL identifier quoting to prevent injection
        sql.append("\"").append(identifier.replace("\"", "\"\"")).append("\"");
    }

    private String mapFieldTypeToPostgresType(FieldType fieldType) {
        // This is safe as it's a controlled enum mapping
        return switch (fieldType) {
            case EMAIL, SINGLE_LINE_TEXT, MULTI_LINE_TEXT -> "TEXT";
            case DECIMAL -> "DECIMAL";
            case LONG -> "BIGINT";
            case BOOLEAN -> "BOOLEAN";
            case DATE -> "DATE";
            case DATETIME -> "TIMESTAMP";
        };
    }

    @Transactional(readOnly = true)
    public GetModelsResponse getModels(UUID userId) {
        GetModelsResponse response = new GetModelsResponse();
        for(Model model: modelRepository.findAll()) {

            if (!userHasGivenPermissionOnModel(userId, model.getId(), ModelPermission.VIEW_RECORDS)) {
                continue;
            }

            ModelDto modelDto = new ModelDto();
            modelDto.setId(model.getId());
            modelDto.setDescription(model.getDescription());
            modelDto.setName(model.getName());
            response.getModels().add(modelDto);

            //Get Field Info
            List<Field> fieldOptional = fieldRepository.findByModelId(model.getId());
            for (Field field: fieldOptional) {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setId(field.getId());
                fieldDto.setDescription(field.getDescription());
                fieldDto.setName(field.getName());
                fieldDto.setType(field.getType());
                fieldDto.setIsRequired(field.getIsRequired());
                modelDto.getFields().add(fieldDto);
            }
        }
        return response;
    }

    private boolean userHasGivenPermissionOnModel(UUID userId, UUID modelId, ModelPermission checkPermission) {
        Model model = modelRepository.findById(modelId).orElseThrow(() -> new RuntimeException("Model with id " + modelId + " does not exist"));
        boolean hasPermission = false;
        GetUserResponse user = userService.getUser(userId);
        GetUserModelPermissionsResponse userPermissions = userService.getUserModelPermissions(userId);
        if (user.isAdminUser()) {
            hasPermission = true;
        } else {
            for (UserModelPermissionDto permission : userPermissions.getUserModelPermissions()) {
                if (permission.getModelId().equals(model.getId()) && permission.getPermission() == checkPermission) {
                    hasPermission = true;
                }
            }
        }
        return hasPermission;
    }

    @Transactional(readOnly = true)
    public GetModelResponse getModel(UUID modelId, UUID userId) {
        GetModelResponse response = new GetModelResponse();
        Model model = modelRepository.findById(modelId).orElseThrow(() -> new RuntimeException("Model not found: " + modelId));

        boolean hasPermission = userHasGivenPermissionOnModel(userId, model.getId(), ModelPermission.VIEW_RECORDS);
        if (!hasPermission) {
            log.error("User does not have permission to view records for model: " + model.getId());
            throw new FrontendException("VIEW permission required", ErrorCode.PERMISSION_DENIED, true);
        }

        ModelDto modelDto = new ModelDto();
        modelDto.setId(model.getId());
        modelDto.setDescription(model.getDescription());
        modelDto.setName(model.getName());
        response.setModel(modelDto);

        //Get Field Info
        List<Field> fieldOptional = fieldRepository.findByModelId(model.getId());
        for (Field field: fieldOptional) {
            FieldDto fieldDto = new FieldDto();
            fieldDto.setId(field.getId());
            fieldDto.setDescription(field.getDescription());
            fieldDto.setName(field.getName());
            fieldDto.setType(field.getType());
            fieldDto.setIsRequired(field.getIsRequired());
            modelDto.getFields().add(fieldDto);
        }

        List<ModelLink> modelLinks = modelLinkRepository.findByEitherModelId(modelId);
        for (ModelLink modelLink: modelLinks) {
            if (modelId.equals(modelLink.getModel1Id())) {
                Model targetModel = modelRepository.findById(modelLink.getModel2Id()).orElseThrow(() -> new RuntimeException("Model not found: " + modelLink.getModel2Id()));
                ModelLinkTarget modelLinkTarget = new ModelLinkTarget();
                modelLinkTarget.setTargetModelId(modelLink.getModel2Id());
                modelLinkTarget.setCan_have_unlimited_targets(modelLink.isModel1CanHaveUnlimitedModel2s());
                modelLinkTarget.setCan_have_targets_count(modelLink.getModel1CanHaveSoManyModel2sCount());
                modelLinkTarget.setModelLinkId(modelLink.getId());
                modelLinkTarget.setTargetModelName(targetModel.getName());
                if (userHasGivenPermissionOnModel(userId, targetModel.getId(), ModelPermission.VIEW_RECORDS)) {
                    response.getModelLinkTargets().add(modelLinkTarget);
                }
            } else if (modelId.equals(modelLink.getModel2Id())) {
                Model targetModel = modelRepository.findById(modelLink.getModel1Id()).orElseThrow(() -> new RuntimeException("Model not found: " + modelLink.getModel2Id()));
                ModelLinkTarget modelLinkTarget = new ModelLinkTarget();
                modelLinkTarget.setTargetModelId(modelLink.getModel1Id());
                modelLinkTarget.setCan_have_unlimited_targets(modelLink.isModel2CanHaveUnlimitedModel1s());
                modelLinkTarget.setCan_have_targets_count(modelLink.getModel2CanHaveSoManyModel1sCount());
                modelLinkTarget.setModelLinkId(modelLink.getId());
                if (userHasGivenPermissionOnModel(userId, targetModel.getId(), ModelPermission.VIEW_RECORDS)) {
                    modelLinkTarget.setTargetModelName(targetModel.getName());
                }

                response.getModelLinkTargets().add(modelLinkTarget);
            } else {
                throw new IllegalStateException("Target Model ID cannot be null");
            }
        }
        return response;
    }

    @Transactional
    public UUID createRecord(UUID userId , UUID modelId, Map<UUID, Object> fieldValues) {
        // Validate model exists
        if (!modelRepository.existsById(modelId)) {
            throw new IllegalArgumentException("Model not found: " + modelId);
        }

        // Perform permissions check
        if (!userHasGivenPermissionOnModel(userId, modelId, ModelPermission.ADD_RECORDS)) {
            log.error("User with ID: " + userId + " does not have permission to add records for model: " + modelId);
            throw new FrontendException("ADD RECORDS permission required", ErrorCode.PERMISSION_DENIED, true);
        }

        // Get fields for validation and column mapping
        List<Field> fields = fieldRepository.findByModelId(modelId);
        if (fields.isEmpty()) {
            throw new IllegalStateException("No fields found for model: " + modelId);
        }

        String tableName = "records_" + sanitizeIdentifier(modelId.toString());

        // Build dynamic INSERT statement
        StringBuilder insertSql = new StringBuilder();
        insertSql.append("INSERT INTO ");
        appendQuotedIdentifier(insertSql, tableName);
        insertSql.append(" (");

        // Build column names and values
        StringBuilder columnNames = new StringBuilder();
        StringBuilder valuePlaceholders = new StringBuilder();
        List<Object> values = new ArrayList<>();

        boolean first = true;
        for (Field field : fields) {
            UUID fieldId = field.getId();
            if (fieldValues.containsKey(fieldId)) {
                Object value = fieldValues.get(fieldId);

                // Validate and convert value based on field type
                Object convertedValue = validateAndConvertValue(value, field.getType(), field.getName());

                if (!first) {
                    columnNames.append(", ");
                    valuePlaceholders.append(", ");
                }

                String columnName = "field_" + sanitizeIdentifier(fieldId.toString());
                appendQuotedIdentifier(columnNames, columnName);

                valuePlaceholders.append("?");

                values.add(convertedValue);
                first = false;
            }
        }

        if (values.isEmpty()) {
            throw new IllegalArgumentException("No valid field values provided");
        }

        insertSql.append(columnNames);
        insertSql.append(") VALUES (");
        insertSql.append(valuePlaceholders);
        insertSql.append(") RETURNING id");

        log.debug("Executing insert with SQL: {}", insertSql.toString());
        log.debug("Values: {}", values);

        // Execute insert and get the generated ID
        UUID recordId = jdbcTemplate.queryForObject(insertSql.toString(), UUID.class, values.toArray());

        log.debug("Created record with ID: {} in table: {}", recordId, tableName);
        return recordId;
    }

    @Transactional
    public RecordDto updateRecord(UUID userId, UUID modelId, UUID recordId, Map<UUID, Object> fieldValues) {

        // Perform permissions check
        if (!userHasGivenPermissionOnModel(userId, modelId, ModelPermission.EDIT_RECORDS)) {
            log.error("User with ID: " + userId + " does not have permission to edit records for model: " + modelId);
            throw new FrontendException("EDIT permission required", ErrorCode.PERMISSION_DENIED, true);
        }

        //Check if UUID exist
        modelRepository.findById(modelId).orElseThrow(() -> new RuntimeException("Model not found: " + modelId));

        // Validate input
        if (recordId == null) {
            throw new IllegalArgumentException("Record ID cannot be null");
        }
        if (fieldValues == null || fieldValues.isEmpty()) {
            throw new IllegalArgumentException("Field values cannot be null or empty");
        }

        // Get fields for validation
        List<Field> fields = fieldRepository.findByModelId(modelId);
        if (fields.isEmpty()) {
            throw new IllegalStateException("No fields found for model: " + modelId);
        }

        String recordTableName = "records_" + sanitizeIdentifier(modelId.toString());

        // Build dynamic UPDATE statement
        StringBuilder updateSql = new StringBuilder();
        updateSql.append("UPDATE ");
        appendQuotedIdentifier(updateSql, recordTableName);
        updateSql.append(" SET updated_at = CURRENT_TIMESTAMP");

        List<Object> parameters = new ArrayList<>();

        // Build SET clauses for each field
        for (Field field : fields) {
            UUID fieldId = field.getId();
            if (fieldValues.containsKey(fieldId)) {
                Object value = fieldValues.get(fieldId);

                // Validate and convert value based on field type
                Object convertedValue = validateAndConvertValue(value, field.getType(), field.getName());

                String columnName = "field_" + sanitizeIdentifier(fieldId.toString());
                updateSql.append(", ");
                appendQuotedIdentifier(updateSql, columnName);
                updateSql.append(" = ?");

                parameters.add(convertedValue);
            }
        }

        // Add WHERE clause
        updateSql.append(" WHERE id = ?");
        parameters.add(recordId);

        if (parameters.size() == 1) { // Only the recordId parameter
            throw new IllegalArgumentException("No valid field values provided for update");
        }

        log.debug("Executing update with SQL: {}", updateSql.toString());
        log.debug("Parameters: {}", parameters);

        // Execute update
        int rowsAffected = jdbcTemplate.update(updateSql.toString(), parameters.toArray());

        if (rowsAffected == 0) {
            throw new IllegalArgumentException("Record not found or no changes made: " + recordId);
        }

        log.debug("Updated record with ID: {} in table: {}, rows affected: {}", recordId, recordTableName, rowsAffected);

        // Return the updated record
        return getRecord(userId, modelId, recordId);
    }

    private Object validateAndConvertValue(Object value, FieldType fieldType, String fieldName) {
        if (value == null) {
            return null;
        }

        try {
            return switch (fieldType) {
                case EMAIL, SINGLE_LINE_TEXT, MULTI_LINE_TEXT -> {
                    if (value instanceof String) {
                        // Save as null for empty string
                        if (((String) value).isBlank()) {
                            yield null;
                        }
                        yield value;
                    }
                    yield value.toString();
                }
                case DECIMAL -> {
                    if (value instanceof Number) {
                        yield value;
                    }
                    if (value instanceof String) {
                        yield new java.math.BigDecimal((String) value);
                    }
                    throw new IllegalArgumentException("Invalid decimal value");
                }
                case LONG -> {
                    if (value instanceof Number) {
                        yield ((Number) value).longValue();
                    }
                    if (value instanceof String) {
                        yield Long.valueOf((String) value);
                    }
                    throw new IllegalArgumentException("Invalid long value");
                }
                case BOOLEAN -> {
                    if (value instanceof Boolean) {
                        yield value;
                    }
                    if (value instanceof String) {
                        yield Boolean.parseBoolean((String) value);
                    }
                    throw new IllegalArgumentException("Invalid boolean value");
                }
                case DATE -> {
                    if (value instanceof String) {
                        yield java.sql.Date.valueOf((String) value);
                    }
                    if (value instanceof java.sql.Date) {
                        yield value;
                    }
                    throw new IllegalArgumentException("Invalid date value. Use YYYY-MM-DD format");
                }
                case DATETIME -> {
                    if (value instanceof String) {
                        String dateTimeStr = (String) value;
                        try {
                            // Try to parse as ISO format (2025-09-17T11:00:00.000Z)
                            if (dateTimeStr.contains("T") && dateTimeStr.endsWith("Z")) {
                                // Parse ISO format and convert to Timestamp
                                Instant instant = Instant.parse(dateTimeStr);
                                yield Timestamp.from(instant);
                            }
                        } catch (Exception e) {
                            throw new FrontendException("Invalid datetime format. Expected ISO format (2025-09-17T11:00:00.000Z)", true);
                        }
                    }
                    throw new FrontendException("Invalid value for field '" + fieldName + "' of type DATETIME: Expected ISO format (2025-09-17T11:00:00.000Z)", true);
                }
            };
        } catch (Exception e) {
            log.error("Invalid value for field [{}]. Field class type: [{}]. Value: [{}]. Exception message: [{}]", fieldName, value.getClass().getName(), value,  e.getMessage());
            throw new IllegalArgumentException("Invalid value for field '" + fieldName + "' of type " + fieldType + ": " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public GetRecordsResponse getRecords(UUID userId, GetRecordsRequest request, UUID sourceModelId) {

        if (!userHasGivenPermissionOnModel(userId, sourceModelId, ModelPermission.VIEW_RECORDS)) {
            log.error("User with ID [" + userId + "] does not have VIEW permissions on model with ID [" + sourceModelId + "]");
            throw new FrontendException("VIEW permission required", ErrorCode.PERMISSION_DENIED, true);
        }

        if (request.getQueryType() == null) {
            throw new FrontendException("Query Type not supplied", true);
        }

        // Validate limit
        int limit = request.getLimit();
        if (limit <= 0 || limit > 1000) {
            throw new FrontendException("Limit must be between 1 and 1000", true);
        }

        boolean isPreviousPage = request.isPrevious();

        String sortOrder = request.getSortOrder();
        String sortField = request.getSortField();
        String cursor = request.getCursor();
        List<SearchParameter> searchParameters = request.getSearchParameters();

        // Validate model exists
        if (!modelRepository.existsById(sourceModelId)) {
            throw new FrontendException("Model not found: " + sourceModelId, true);
        }

        // Validate UUID's
        ModelLink modelLink;
        //Model targetModel;
        int linkedSourceRecordNumber = 0;
        int linkedTargetRecordNumber = 0;
        List<Field> fields;
        String tableName = null;
        String modelLinkTablename = null;
        UUID targetModelId = null;
        if (request.getQueryType() == QueryType.LINKED_RECORDS || request.getQueryType() == QueryType.RECORDS_NOT_LINKED) {
            UUID modelLinkId = request.getModelLinkId();
            UUID sourceRecordId = request.getSourceRecordId();

            modelLink = modelLinkRepository.findById(modelLinkId).orElseThrow(() -> new IllegalArgumentException("Invalid model link id"));

            modelLinkTablename = "model_links_" + sanitizeIdentifier(modelLinkId.toString());

            if (modelLink.getModel1Id().equals(sourceModelId)) {
                //Validate target model id
                modelRepository.findById(modelLink.getModel2Id()).orElseThrow(() -> new IllegalArgumentException("Invalid model id"));
                targetModelId = modelLink.getModel2Id();
                linkedSourceRecordNumber = 1;
                linkedTargetRecordNumber = 2;
            } else if (modelLink.getModel2Id().equals(sourceModelId)) {
                linkedSourceRecordNumber = 2;
                linkedTargetRecordNumber = 1;
                //Validate target model Id
                modelRepository.findById(modelLink.getModel1Id()).orElseThrow(() -> new IllegalArgumentException("Invalid model id"));
                targetModelId = modelLink.getModel1Id();
            } else {
                throw new IllegalArgumentException("Invalid model link id");
            }

            //Validate source record
            String sql = "SELECT EXISTS(SELECT 1 FROM records_" + sanitizeIdentifier(sourceModelId.toString()) + " WHERE id = ?)";
            log.debug(sql);
            Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, sourceRecordId);
            if (!Boolean.TRUE.equals(exists)) {
                throw new IllegalArgumentException("Invalid source record id");
            }
            // Get fields for metadata and validation
            fields = fieldRepository.findByModelId(targetModelId);
            if (fields.isEmpty()) {
                throw new IllegalStateException("No fields found for model: " + targetModelId);
            }
            tableName = "records_" + sanitizeIdentifier(targetModelId.toString());
        } else {
            // Get fields for metadata and validation
            fields = fieldRepository.findByModelId(sourceModelId);
            if (fields.isEmpty()) {
                throw new IllegalStateException("No fields found for model: " + sourceModelId);
            }
            tableName = "records_" + sanitizeIdentifier(sourceModelId.toString());
        }

        String actualRecordCountSql = null;
        if (request.getQueryType() == QueryType.ALL_RECORDS) {
            actualRecordCountSql = "SELECT COUNT(*) FROM " + " records_" + sanitizeIdentifier(sourceModelId.toString());
        }

        if (request.getQueryType() == QueryType.RECORDS_NOT_LINKED || request.getQueryType() == QueryType.LINKED_RECORDS) {
            actualRecordCountSql = "SELECT COUNT(*) FROM " + " records_" + sanitizeIdentifier(targetModelId.toString());
        }

        if (actualRecordCountSql == null) {
            throw new RuntimeException("actualRecordCountSql is null");
        }

        Long actualRecordCount = jdbcTemplate.queryForObject(actualRecordCountSql, Long.class);

        // Validate and normalize sort parameters
        String validatedSortField = validateSortField(sortField, fields);
        String validatedSortOrder = validateSortOrder(sortOrder);

        // Decode cursor
        CursorInfo cursorInfo = CursorUtils.decodeCursor(cursor);
        
        // Build table name


        // Build the SELECT query
        StringBuilder querySql = new StringBuilder();
        querySql.append("SELECT r.id, r.created_at, r.updated_at");

        if (request.getQueryType() == QueryType.LINKED_RECORDS) {
            querySql.append(", ml.id as link_id");
        }

        // Add field columns to SELECT
        for (Field field : fields) {
            String columnName = "field_" + sanitizeIdentifier(field.getId().toString());
            querySql.append(", r.");
            appendQuotedIdentifier(querySql, columnName);
        }

        querySql.append(" FROM ");
        appendQuotedIdentifier(querySql, tableName);
        querySql.append(" r ");

        List<Object> parameters = new ArrayList<>();

        //Now join the linked records table
        if (request.getQueryType() == QueryType.LINKED_RECORDS) {
            querySql.append(" JOIN " + modelLinkTablename + " ml ");
            querySql.append(" ON ml.record" + linkedTargetRecordNumber + "_id = r.id AND ml.record" + linkedSourceRecordNumber + "_id = ? ");
            parameters.add(request.getSourceRecordId());
            log.debug(querySql.toString());
        }

        if (request.getQueryType() == QueryType.RECORDS_NOT_LINKED) {
            querySql.append(" LEFT JOIN " + modelLinkTablename + " ml ON ml.record" + linkedTargetRecordNumber + "_id = r.id AND ml.record" + linkedSourceRecordNumber + "_id = ? ");
            parameters.add(request.getSourceRecordId());
        }

        // Build WHERE clause
        String sortColumn = CursorUtils.getDatabaseColumnName(validatedSortField, fields);


        querySql.append(" WHERE r.").append(sortColumn).append(" IS NOT NULL ");

        if (request.getQueryType() == QueryType.RECORDS_NOT_LINKED) {
            querySql.append(" AND ml.record" + linkedSourceRecordNumber + "_id IS NULL ");
        }

        // Process search criteria
        List<String> searchConditions = new ArrayList<>();
        
        if (searchParameters != null && !searchParameters.isEmpty()) {

            for (SearchParameter searchParameter : searchParameters) {
                SearchFieldType searchFieldType;
                String columnName;

                if (searchParameter.getSearchFieldIdentifier() == null) {
                    throw new IllegalArgumentException("Invalid search field identifier. Is null.");
                }

                Field field = null;

                if (searchParameter.getSearchFieldIdentifier() == SearchFieldIdentifier.CUSTOM_FIELD) {
                    if (searchParameter.getFieldID() == null) {
                        throw new IllegalArgumentException("Invalid field ID. Is null.");
                    }
                    field = fieldRepository.findById(searchParameter.getFieldID())
                            .orElseThrow(() -> new IllegalArgumentException("Field not found: " + searchParameter.getFieldID()));
                }

                columnName = switch(searchParameter.getSearchFieldIdentifier()) {
                    case null -> throw new IllegalArgumentException("Invalid search field identifier. Is null.");
                    case CREATED_AT -> "r.created_at";
                    case UPDATED_AT -> "r.updated_at";
                    case ID -> "r.id";
                    case CUSTOM_FIELD -> {
                        yield "r.field_" + sanitizeIdentifier(field.getId().toString());
                    }
                };
                searchFieldType = switch(searchParameter.getSearchFieldIdentifier()) {
                    case null -> throw new IllegalArgumentException("Invalid search field identifier. Is null.");
                    case CREATED_AT, UPDATED_AT -> SearchFieldType.DATETIME;
                    case ID -> SearchFieldType.UUID;
                    case CUSTOM_FIELD -> SearchFieldType.fromFieldType(field.getType());

                };

                if (columnName == null) {
                    throw new IllegalArgumentException("Column name is null");
                }

                if (searchFieldType == null) {
                    throw new IllegalArgumentException("Search field type is null");
                }

                //<editor-fold desc="Validate Search">
                switch (searchFieldType) {
                    case null -> throw new IllegalArgumentException("Search field type is null");
                    case DATETIME -> {
                        switch (searchParameter.getSearchType()) {
                            case DATE_TIME_AFTER, DATE_TIME_BEFORE, DATE_TIME_BETWEEN -> log.debug("Valid");
                            default -> throw new IllegalArgumentException("Invalid search parameter: " + searchParameter.getSearchType());
                        }
                    }
                    case UUID, TEXT -> {
                        switch (searchParameter.getSearchType()) {
                            case TEXT_CONTAINS, TEXT_NOT_CONTAINS, TEXT_NOT_EQUALS, TEXT_ENDS_WITH, TEXT_STARTS_WITH, TEXT_EQUALS -> log.debug("Valid");
                            default -> throw new IllegalArgumentException("Invalid search parameter: " + searchParameter.getSearchType());
                        }
                    }
                    case BOOLEAN -> {
                        switch (searchParameter.getSearchType()) {
                            case BOOLEAN_TRUE, BOOLEAN_FALSE ->  log.debug("Valid");
                            default -> throw new IllegalArgumentException("Invalid search parameter: " + searchParameter.getSearchType());
                        }
                    }
                    case DECIMAL -> {
                        switch (searchParameter.getSearchType()) {
                            case DECIMAL_EQUALS, DECIMAL_GREATER_THAN, DECIMAL_LESS_THAN -> log.debug("Valid");
                            default -> throw new IllegalArgumentException("Invalid search parameter: " + searchParameter.getSearchType());
                        }
                    }
                    case LONG ->  {
                        switch (searchParameter.getSearchType()) {
                            case LONG_EQUALS, LONG_GREATER_THAN, LONG_LESS_THAN -> log.debug("Valid");
                            default -> throw new IllegalArgumentException("Invalid search parameter: " + searchParameter.getSearchType());
                        }
                    }
                    case DATE -> {
                        switch (searchParameter.getSearchType()) {
                            case DATE_AFTER, DATE_BEFORE, DATE_BETWEEN, DATE_ON -> log.debug("Valid");
                            default -> throw new IllegalArgumentException("Invalid search parameter: " + searchParameter.getSearchType());
                        }
                    }
                    default -> throw new IllegalArgumentException("Invalid search parameter: " + searchParameter.getSearchType());
                }
                //</editor-fold>

                String condition = switch (searchFieldType) {
                    case null -> throw new IllegalArgumentException("Invalid search field identifier. Is null.");
                    case TEXT -> buildTextSearchCondition(columnName, searchParameter, parameters);
                    case DATE -> buildDateSearchCondition(columnName, searchParameter, parameters);
                    case DATETIME -> buildDateTimeSearchCondition(columnName, searchParameter, parameters);
                    case BOOLEAN -> buildBooleanSearchCondition(columnName, searchParameter, parameters);
                    case DECIMAL -> buildDecimalSearchCondition(columnName, searchParameter, parameters);
                    case LONG -> buildLongSearchCondition(columnName, searchParameter, parameters);
                    case UUID ->  buildUuidSearchCondition(columnName, searchParameter, parameters);
                    default -> throw new IllegalArgumentException("Invalid search parameter: " + searchParameter.getSearchType());
                };

                if (condition != null) {
                    searchConditions.add(condition);
                }
            }
        }

        if (!searchConditions.isEmpty()) {
            querySql.append(" AND ");
            querySql.append(String.join(" AND ", searchConditions));
        }

        // Optional: Get rough count estimate (can be expensive on large tables)
        Long totalRecords;
        try {
            String countSql = convertToCountQuery(querySql.toString());
            totalRecords = jdbcTemplate.queryForObject(countSql, parameters.toArray(), Long.class);
        } catch (Exception e) {
            log.warn("Could not get count estimate for table {}: {}", tableName, e.getMessage());
            totalRecords = -1L; // Indicate count unavailable
        }

        String effectiveSortOrder = validatedSortOrder;
        
        // For previous page navigation, we need to reverse the sort order and comparison
        if (isPreviousPage) {
            effectiveSortOrder = "DESC".equalsIgnoreCase(validatedSortOrder) ? "ASC" : "DESC";
        }
        
        if (cursorInfo != null) {

            Object sortValue = CursorUtils.convertCursorValueForComparison(
                cursorInfo.getSortFieldValue(), validatedSortField, fields);
            UUID cursorRecordId = cursorInfo.getRecordId();

            querySql.append(" AND ");

            if ("DESC".equalsIgnoreCase(effectiveSortOrder)) {
                // For DESC: (sort_field < cursor_value) OR (sort_field = cursor_value AND id < cursor_id)
                querySql.append("(r.").append(sortColumn).append(" < ?");
                querySql.append(" OR (r.").append(sortColumn).append(" = ? AND r.id < ?))");
                parameters.add(sortValue);
                parameters.add(sortValue);
                parameters.add(cursorRecordId);
            } else {
                // For ASC: (sort_field > cursor_value) OR (sort_field = cursor_value AND id > cursor_id)
                querySql.append("(r.").append(sortColumn).append(" > ?");
                querySql.append(" OR (r.").append(sortColumn).append(" = ? AND r.id > ?))");
                parameters.add(sortValue);
                parameters.add(sortValue);
                parameters.add(cursorRecordId);
            }

        }

        // Add ORDER BY clause with tie-breaker
        querySql.append(" ORDER BY r.").append(sortColumn).append(" ").append(effectiveSortOrder);
        querySql.append(", r.id ").append(effectiveSortOrder); // Always add ID as tie-breaker

        // Add LIMIT (get one extra record to check if there are more pages)
        querySql.append(" LIMIT ?");
        parameters.add(limit + 1);

        log.debug("Executing pagination query: {}", querySql.toString());
        log.debug("Parameters: {}", parameters);

        // Execute query
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(querySql.toString(), parameters.toArray());
        } catch (Exception e) {
            log.error("Error executing pagination query for model {}: {}", sourceModelId, e.getMessage());
            throw new RuntimeException("Failed to retrieve records: " + e.getMessage());
        }

        // Check if there are more records (hasNext)
        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit); // Remove the extra record
        }

        // Convert database rows to DTOs
        List<RecordDto> recordDtos = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            RecordDto recordDto = new RecordDto();
            recordDto.setId((UUID) row.get("id"));

            // Handle timestamp conversion safely
            Object createdAtObj = row.get("created_at");
            if (createdAtObj instanceof java.sql.Timestamp) {
                recordDto.setCreatedAt(((java.sql.Timestamp) createdAtObj).toInstant());
            }

            Object updatedAtObj = row.get("updated_at");
            if (updatedAtObj instanceof java.sql.Timestamp) {
                recordDto.setUpdatedAt(((java.sql.Timestamp) updatedAtObj).toInstant());
            }

            UUID linkId = (UUID) row.get("link_id");
            recordDto.setLinkId(linkId);

            // Extract field values
            Map<UUID, Object> fieldValues = new HashMap<>();
            for (Field field : fields) {
                String columnName = "field_" + sanitizeIdentifier(field.getId().toString());
                Object value = row.get(columnName);
                if (value != null) {
                    Object convertedValue = convertFromDatabase(value, field.getType());

                    // Truncate multi-line text fields to 30 characters for search results
                    if (field.getType() == FieldType.MULTI_LINE_TEXT && convertedValue instanceof String) {
                        String textValue = (String) convertedValue;
                        // Replace all whitespace (including newlines, tabs, multiple spaces) with single space
                        textValue = textValue.replaceAll("\\s+", " ").trim();
                        if (textValue.length() > 30) {
                            convertedValue = textValue.substring(0, 30) + "...";
                        } else {
                            convertedValue = textValue;
                        }
                    }

                    fieldValues.put(field.getId(), convertedValue);
                } else {
                    fieldValues.put(field.getId(), null);
                }
            }
            recordDto.setFieldValues(fieldValues);
            recordDtos.add(recordDto);
        }
        
        // If this was a previous page request, we need to reverse the results
        // because we fetched them in reverse order
        if (isPreviousPage) {
            Collections.reverse(recordDtos);
        }

        // Build field DTOs for frontend metadata
        List<FieldDto> fieldDtos = new ArrayList<>();
        for (Field field : fields) {
            FieldDto fieldDto = new FieldDto();
            fieldDto.setId(field.getId());
            fieldDto.setName(field.getName());
            fieldDto.setDescription(field.getDescription());
            fieldDto.setType(field.getType());
            fieldDto.setIsRequired(field.getIsRequired());
            fieldDtos.add(fieldDto);
        }

        // Build pagination info with proper cursor generation
        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setLimit(limit);
        paginationInfo.setSortField(sortField); // Return original field name/ID for frontend
        paginationInfo.setSortOrder(validatedSortOrder);
        paginationInfo.setActualRecordCount(actualRecordCount);
        
        if (isPreviousPage) {
            // For previous page, hasNext means there are more records in the forward direction
            // and hasPrevious means there are more records in the backward direction
            paginationInfo.setHasNext(cursor != null && !cursor.trim().isEmpty());
            paginationInfo.setHasPrevious(hasNext);
        } else {
            // For normal (next) page navigation
            paginationInfo.setHasNext(hasNext);
            paginationInfo.setHasPrevious(cursor != null && !cursor.trim().isEmpty());
        }

        // Generate cursors using the new system
        if (!recordDtos.isEmpty()) {
            if (!isPreviousPage) {
                // Normal navigation - next cursor from last record, prev cursor from first record
                if (hasNext) {
                    RecordDto lastRecord = recordDtos.get(recordDtos.size() - 1);
                    CursorInfo nextCursorInfo = CursorUtils.createCursorFromRecord(lastRecord, validatedSortField, validatedSortOrder, fields);
                    String nextCursor = CursorUtils.encodeCursor(nextCursorInfo);
                    paginationInfo.setNextCursor(nextCursor);
                }
                
                RecordDto firstRecord = recordDtos.get(0);
                CursorInfo prevCursorInfo = CursorUtils.createCursorFromRecord(firstRecord, validatedSortField, validatedSortOrder, fields);
                String prevCursor = CursorUtils.encodeCursor(prevCursorInfo);
                paginationInfo.setPreviousCursor(prevCursor);
            } else {
                // Previous page navigation - next cursor from last record, prev cursor from first record
                RecordDto lastRecord = recordDtos.get(recordDtos.size() - 1);
                CursorInfo nextCursorInfo = CursorUtils.createCursorFromRecord(lastRecord, validatedSortField, validatedSortOrder, fields);
                String nextCursor = CursorUtils.encodeCursor(nextCursorInfo);
                paginationInfo.setNextCursor(nextCursor);
                
                if (hasNext) {
                    RecordDto firstRecord = recordDtos.get(0);
                    CursorInfo prevCursorInfo = CursorUtils.createCursorFromRecord(firstRecord, validatedSortField, validatedSortOrder, fields);
                    String prevCursor = CursorUtils.encodeCursor(prevCursorInfo);
                    paginationInfo.setPreviousCursor(prevCursor);
                }
            }
        }

        paginationInfo.setTotalEstimate(totalRecords != null ? totalRecords : 0L);

        // Build and return response
        GetRecordsResponse response = new GetRecordsResponse();
        response.setRecords(recordDtos);
        response.setFields(fieldDtos);
        response.setPagination(paginationInfo);

        return response;
    }

    @Transactional(readOnly = true)
    public RecordDto getRecord(UUID userId, UUID modelId, UUID recordId) {

        // Perform permissions check
        if (!userHasGivenPermissionOnModel(userId, modelId, ModelPermission.VIEW_RECORDS)) {
            log.error("User with id " + userId + " does not have permission to view records on model with id " + modelId);
            throw new FrontendException("VIEW permission required", ErrorCode.PERMISSION_DENIED, true);
        }

        // Validate input
        if (recordId == null) {
            throw new IllegalArgumentException("Record ID cannot be null");
        }

        //Check if model exists
        modelRepository.findById(modelId).orElseThrow(() -> new RuntimeException("Model not found for model ID: " + modelId));

        String recordTableName = "records_" + sanitizeIdentifier(modelId.toString());
        // Get fields for metadata
        List<Field> fields = fieldRepository.findByModelId(modelId);
        if (fields.isEmpty()) {
            throw new IllegalStateException("No fields found for model: " + modelId);
        }

        // Build the SELECT query for single record
        StringBuilder querySql = new StringBuilder();
        querySql.append("SELECT id, created_at, updated_at");

        // Add field columns to SELECT
        for (Field field : fields) {
            String columnName = "field_" + sanitizeIdentifier(field.getId().toString());
            querySql.append(", ");
            appendQuotedIdentifier(querySql, columnName);
        }

        querySql.append(" FROM ");
        appendQuotedIdentifier(querySql, recordTableName);
        querySql.append(" WHERE id = ?");

        log.debug("Executing single record query: {}", querySql.toString());

        // Execute query
        Map<String, Object> row;
        try {
            row = jdbcTemplate.queryForMap(querySql.toString(), recordId);
        } catch (Exception e) {
            log.error("Error executing single record query for record {}: {}", recordId, e.getMessage());
            throw new RuntimeException("Failed to retrieve record: " + e.getMessage());
        }

        // Convert database row to DTO
        RecordDto recordDto = new RecordDto();
        recordDto.setId((UUID) row.get("id"));

        // Handle timestamp conversion safely
        Object createdAtObj = row.get("created_at");
        if (createdAtObj instanceof java.sql.Timestamp) {
            recordDto.setCreatedAt(((java.sql.Timestamp) createdAtObj).toInstant());
        }

        Object updatedAtObj = row.get("updated_at");
        if (updatedAtObj instanceof java.sql.Timestamp) {
            recordDto.setUpdatedAt(((java.sql.Timestamp) updatedAtObj).toInstant());
        }

        // Extract field values
        Map<UUID, Object> fieldValues = new HashMap<>();
        for (Field field : fields) {
            String columnName = "field_" + sanitizeIdentifier(field.getId().toString());
            Object value = row.get(columnName);
            if (value != null) {
                Object convertedValue = convertFromDatabase(value, field.getType());
                fieldValues.put(field.getId(), convertedValue);
            }
        }
        recordDto.setFieldValues(fieldValues);

        //Find linked records

        recordDto.setLinkedRecordCounts(getLinkedRecordCounts(modelId, recordId));

        return recordDto;
    }

    @Transactional
    public void deleteRecord(UUID userId, UUID modelId, UUID recordId, boolean overrideLinkedModelsError) {

        // perform permissions check
        if (!userHasGivenPermissionOnModel(userId, modelId, ModelPermission.DELETE_RECORDS)) {
            log.error("User with id " + userId + " does not have permission to delete records for model " + modelId);
            throw new FrontendException("DELETE permission required", ErrorCode.PERMISSION_DENIED, true);
        }

        // Validate input
        if (recordId == null) {
            throw new IllegalArgumentException("Record ID cannot be null");
        }

        //Check if model exists
        modelRepository.findById(modelId).orElseThrow(() -> new RuntimeException("Model not found for model ID: " + modelId));

        String recordTableName = "records_" + sanitizeIdentifier(modelId.toString());
        //Check if record exists
        String sqlRecordExists = "SELECT EXISTS(SELECT 1 FROM " + recordTableName + " WHERE id = ?)";
        Boolean targetExists = jdbcTemplate.queryForObject(sqlRecordExists, Boolean.class, recordId);

        if (!Boolean.TRUE.equals(targetExists)) {
            throw new FrontendException("Record does not exist: " + recordId, true);
        }

        if (!overrideLinkedModelsError) {
            List<LinkedRecordCount> linkedRecordCounts = getLinkedRecordCounts(modelId, recordId);
            List<LinkedRecordCount> moreThanZeroLinkedRecordCounts = new ArrayList<>();
            for (LinkedRecordCount linkedRecordCount : linkedRecordCounts) {
                if (linkedRecordCount.getRecordCount() > 0) {
                    moreThanZeroLinkedRecordCounts.add(linkedRecordCount);
                }
            }

            if (!moreThanZeroLinkedRecordCounts.isEmpty()) {
                String modelsLinked = "";
                for (LinkedRecordCount linkedRecordCount : moreThanZeroLinkedRecordCounts) {
                    modelsLinked += linkedRecordCount.getTargetModelName() + "; ";
                    if (modelsLinked.length() > 100) {
                        modelsLinked = modelsLinked.substring(0, 100);
                        modelsLinked = modelsLinked + " ....And potentially more models";
                        break;
                    }
                }
                throw new FrontendException("Record has linked record(s) to :  " + modelsLinked, ErrorCode.RECORD_LINKED_TO_MODELS, true);
            }
        }

        String sqlDelete = "DELETE FROM " + recordTableName + " WHERE id = ?";
        jdbcTemplate.update(sqlDelete, recordId);
    }

    public String convertToCountQuery(String originalSql) {
        // Convert SELECT clause to COUNT(*)
        return originalSql.replaceAll("(?i)SELECT\\s+.*?\\s+FROM", "SELECT COUNT(*) FROM");
    }

    private String validateSortField(String sortField, List<Field> fields) {
        // Default to 'created_at' if no sort field specified
        if (sortField == null || sortField.trim().isEmpty()) {
            throw new FrontendException("Invalid sort field: " + sortField, true);
        }

        // Allow sorting by system fields
        if ("id".equals(sortField) || "created_at".equals(sortField) || "updated_at".equals(sortField)) {
            return sortField;
        }

        // Check if it's a valid field UUID for custom fields
        try {
            UUID fieldId = UUID.fromString(sortField);
            for (Field field : fields) {
                if (field.getId().equals(fieldId)) {
                    return sortField; // Return the UUID as string, not the column name
                }
            }
        } catch (IllegalArgumentException e) {
            // Not a valid UUID, fall through to default
        }

        log.warn("Invalid sort field: {}, defaulting to 'created_at'", sortField);
        return "created_at";
    }

    private String validateSortOrder(String sortOrder) {
        if ("desc".equalsIgnoreCase(sortOrder)) {
            return "DESC";
        }
        return "ASC";
    }

    private String buildTextSearchCondition(String columnName, SearchParameter searchParameter, List<Object> parameters) {

        String value = searchParameter.getTextSearchValue();
        
        switch (searchParameter.getSearchType()) {
            case TEXT_CONTAINS:
                parameters.add("%" + value + "%");
                return columnName + " ILIKE ?";
            case TEXT_EQUALS:
                parameters.add(value);
                return columnName + " = ?";
            case TEXT_STARTS_WITH:
                parameters.add(value + "%");
                return columnName + " ILIKE ?";
            case TEXT_ENDS_WITH:
                parameters.add("%" + value);
                return columnName + " ILIKE ?";
            case TEXT_NOT_CONTAINS:
                parameters.add("%" + value + "%");
                return columnName + " NOT ILIKE ?";
            case TEXT_NOT_EQUALS:
                parameters.add(value);
                return columnName + " != ?";
            default:
                return null;
        }
    }

    private String buildUuidSearchCondition(String columnName, SearchParameter searchParameter, List<Object> parameters) {

        String searchValue = searchParameter.getTextSearchValue();

        switch (searchParameter.getSearchType()) {
            case TEXT_CONTAINS:
                parameters.add("%" + searchParameter.getTextSearchValue() + "%");
                return "CAST(" + columnName + " AS TEXT) ILIKE ?";
            case TEXT_EQUALS:
                // Try to parse as UUID first, fall back to string comparison
                try {
                    UUID.fromString(searchValue);
                    parameters.add(searchValue);
                    return columnName + " = CAST(? AS UUID)";
                } catch (IllegalArgumentException e) {
                    // If not a valid UUID, convert to string for comparison
                    parameters.add(searchValue);
                    return "CAST(" + columnName + " AS TEXT) = ?";
                }
            case TEXT_STARTS_WITH:
                parameters.add(searchValue + "%");
                return "CAST(" + columnName + " AS TEXT) ILIKE ?";
            case TEXT_ENDS_WITH:
                parameters.add("%" + searchValue);
                return "CAST(" + columnName + " AS TEXT) ILIKE ?";
            case TEXT_NOT_CONTAINS:
                parameters.add("%" + searchValue + "%");
                return "CAST(" + columnName + " AS TEXT) NOT ILIKE ?";
            case TEXT_NOT_EQUALS:
                try {
                    UUID.fromString(searchValue);
                    parameters.add(searchValue);
                    return columnName + " != CAST(? AS UUID)";
                } catch (IllegalArgumentException e) {
                    parameters.add(searchValue);
                    return "CAST(" + columnName + " AS TEXT) != ?";
                }
            default:
                throw new RuntimeException("Invalid search parameter: " + searchParameter.getSearchType());
        }
    }

    private String buildDecimalSearchCondition(String columnName, SearchParameter searchParameter, List<Object> parameters) {

        if (searchParameter.getDoubleSearchValue() == null) {
            throw new RuntimeException("Invalid search parameter: " + searchParameter.getSearchType());
        }

        BigDecimal searchValue = searchParameter.getDoubleSearchValue();

        switch (searchParameter.getSearchType()) {
            case DECIMAL_EQUALS:
                parameters.add(searchValue);
                return columnName + " = ?";
            case DECIMAL_GREATER_THAN:
                parameters.add(searchValue);
                return columnName + " > ?";
            case DECIMAL_LESS_THAN:
                parameters.add(searchValue);
                return columnName + " < ?";
            default:
                return null;
        }
    }

    private String buildLongSearchCondition(String columnName, SearchParameter searchParameter, List<Object> parameters) {

        if (searchParameter.getLongSearchValue() == null) {
            throw new RuntimeException("Long value is null");
        }
        long searchValue = searchParameter.getLongSearchValue();

        switch (searchParameter.getSearchType()) {
            case LONG_EQUALS:
                parameters.add(searchValue);
                return columnName + " = ?";
            case LONG_GREATER_THAN:
                parameters.add(searchValue);
                return columnName + " > ?";
            case LONG_LESS_THAN:
                parameters.add(searchValue);
                return columnName + " < ?";
            default:
                return null;
        }
    }

    private String buildBooleanSearchCondition(String columnName, SearchParameter searchParameter, List<Object> parameters) {

        switch (searchParameter.getSearchType()) {
            case BOOLEAN_FALSE -> parameters.add(false);
            case BOOLEAN_TRUE -> parameters.add(true);
            default -> throw new IllegalArgumentException("Invalid search parameter");
        }

        return columnName + " = ?";

    }

    private String buildDateTimeSearchCondition(String columnName, SearchParameter searchParameter, List<Object> parameters) {

        SearchType searchType = searchParameter.getSearchType();


        switch (searchType) {
            case null: throw new IllegalArgumentException("Invalid search parameter");
            case DATE_TIME_BEFORE:
                if (searchParameter.getDateTimeBefore() == null) {
                    throw new IllegalArgumentException("Invalid search parameter");
                }
                parameters.add(searchParameter.getDateTimeBefore());
                return columnName + " < ?";
            case DATE_TIME_AFTER:
                if (searchParameter.getDateTimeAfter() == null) {
                    throw new IllegalArgumentException("Invalid search parameter");
                }
                parameters.add(searchParameter.getDateTimeAfter());
                return columnName + " > ?";
            case DATE_TIME_BETWEEN:
                if (searchParameter.getDateTimeStart() == null || searchParameter.getDateTimeEnd() == null) {
                    throw new IllegalArgumentException("Invalid search parameter");
                }
                parameters.add(searchParameter.getDateTimeStart());
                parameters.add(searchParameter.getDateTimeEnd());
                return columnName + " >= ? AND " + columnName + " <= ?";
            default:
                throw new IllegalArgumentException("Invalid search type");
        }
    }

    private String buildDateSearchCondition(String columnName , SearchParameter searchParameter,  List<Object> parameters) {
        try {
            SearchType searchType = searchParameter.getSearchType();

            switch (searchType) {
                case DATE_ON:
                        parameters.add(searchParameter.getDateOn().toString());
                        return "DATE(" + columnName + ") = DATE(?)";
                case DATE_BEFORE:
                    parameters.add(searchParameter.getDateBefore().toString());
                    return "DATE(" + columnName + ") < DATE(?)";
                case DATE_AFTER:
                    parameters.add(searchParameter.getDateAfter().toString());
                    return "DATE(" + columnName + ") > DATE(?)";
                case DATE_BETWEEN:
                            parameters.add(searchParameter.getDateBefore().toString());
                            parameters.add(searchParameter.getDateAfter().toString());
                            return "DATE(" + columnName + ") >= DATE(?) AND DATE(" + columnName + ") <= DATE(?)";
                default:
                    throw new IllegalArgumentException("Invalid search type");
            }
        } catch (Exception e) {
            log.warn("Failed to parse date search condition: {}", e.getMessage());
        }
        return null;
    }

    private String calculateNextMinute(String dateTimeValue) {
        // Input: "2025-09-07T09:12" -> Output: "2025-09-07T09:13:00"
        // Input: "2025-09-07T23:59" -> Output: "2025-09-08T00:00:00"
        // Parse the input datetime and add 1 minute
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(dateTimeValue + ":00");
        java.time.LocalDateTime nextMinute = dateTime.plusMinutes(1);
        return nextMinute.toString();
    }

    private Object convertFromDatabase(Object value, FieldType fieldType) {
        if (value == null) {
            return null;
        }

        return switch (fieldType) {
            case EMAIL, SINGLE_LINE_TEXT, MULTI_LINE_TEXT -> value.toString();
            case DECIMAL -> {
                if (value instanceof Number) {
                    yield value;
                }
                yield new java.math.BigDecimal(value.toString());
            }
            case LONG -> {
                if (value instanceof Number) {
                    yield ((Number) value).longValue();
                }
                yield Long.valueOf(value.toString());
            }
            case BOOLEAN -> {
                if (value instanceof Boolean) {
                    yield value;
                }
                yield Boolean.parseBoolean(value.toString());
            }
            case DATE -> {
                if (value instanceof java.sql.Date) {
                    yield value.toString(); // Return as ISO date string
                }
                yield value.toString();
            }
            case DATETIME -> {
                if (value instanceof java.sql.Timestamp) {
                    yield ((java.sql.Timestamp) value).toInstant(); // Return as Instant for consistency
                }
                yield value;
            }
        };
    }

    @Transactional
    public void linkModels(LinkModelsRequest linkModelsRequest) {

        UUID model1Id = linkModelsRequest.getModel1Id();
        UUID model2Id = linkModelsRequest.getModel2Id();

        //Check if models exists:
        modelRepository.findById(model1Id).orElseThrow(() -> new IllegalArgumentException("Model with id " + model1Id + " does not exist"));
        modelRepository.findById(model2Id).orElseThrow(() -> new IllegalArgumentException("Model with id " + model2Id + " does not exist"));

        ModelLink link = new ModelLink();
        link.setModel1Id(linkModelsRequest.getModel1Id());
        link.setModel2Id(linkModelsRequest.getModel2Id());
        link.setModel1CanHaveSoManyModel2sCount(linkModelsRequest.getModel1_can_have_so_many_model2s_count());
        link.setModel2CanHaveSoManyModel1sCount(linkModelsRequest.getModel2_can_have_so_many_model1s_count());
        link.setModel1CanHaveUnlimitedModel2s(linkModelsRequest.isModel1_can_have_unlimited_model2s());
        link.setModel2CanHaveUnlimitedModel1s(linkModelsRequest.isModel2_can_have_unlimited_model1s());
        modelLinkRepository.save(link);

        UUID linkId = link.getId();
        if (linkId == null) {
            throw new IllegalArgumentException("Link ID is null");
        }

        // Create "model_links_" table
        String tableName = "model_links_" + sanitizeIdentifier(linkId.toString());
        String sql = "CREATE TABLE " + tableName + " (id UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(), record1_id UUID NOT NULL, record2_id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP);";
        jdbcTemplate.execute(sql);

        log.debug("Model Link Table Created");

        // Create Unique Index
        String sqlIndex = "CREATE UNIQUE INDEX idx_model_links_" + sanitizeIdentifier(linkId.toString()) + " on model_links_" + sanitizeIdentifier(linkId.toString()) + " (record1_id, record2_id);";
        jdbcTemplate.execute(sqlIndex);
        log.debug("Model Link Unique Index Created");

        // Create BTREE index on record2_id and record1_id (already have an index for record1_id and record2_id (Unique Index))
        String index2Sql = "CREATE INDEX idx_" + tableName + "_records_2";
        index2Sql += " ON " + tableName;
        index2Sql += " USING BTREE (record2_id, record1_id);";

        jdbcTemplate.execute(index2Sql);

        log.debug("Model Link Indexes Created");

        //Create foreign keys with cascade delete
        String records1TableName = "records_" + sanitizeIdentifier(model1Id.toString());
        String records2TableName = "records_" + sanitizeIdentifier(model2Id.toString());

        String foreignKey1 = "ALTER TABLE " + tableName + " ADD CONSTRAINT fk_record_1 FOREIGN KEY (record1_id) REFERENCES " + records1TableName + "(id) ON DELETE CASCADE";
        jdbcTemplate.execute(foreignKey1);

        String foreignKey2 = "ALTER TABLE " + tableName + " ADD CONSTRAINT fk_record_2 FOREIGN KEY (record2_id) REFERENCES " + records2TableName + "(id) ON DELETE CASCADE";
        jdbcTemplate.execute(foreignKey2);

        log.debug("Model links foreign keys created with cascade delete");

    }

    @Transactional
    public void updatelinkModels(UpdateLinkedModelsRequest updateLinkedModelsRequest) {
        ModelLink link = modelLinkRepository.findById(updateLinkedModelsRequest.getModelLinkId()).orElseThrow(() -> new RuntimeException("Link with id " + updateLinkedModelsRequest.getModelLinkId() + " does not exist"));
        link.setModel1CanHaveSoManyModel2sCount(updateLinkedModelsRequest.getModel1_can_have_so_many_model2s_count());
        link.setModel2CanHaveSoManyModel1sCount(updateLinkedModelsRequest.getModel2_can_have_so_many_model1s_count());
        link.setModel1CanHaveUnlimitedModel2s(updateLinkedModelsRequest.isModel1_can_have_unlimited_model2s());
        link.setModel2CanHaveUnlimitedModel1s(updateLinkedModelsRequest.isModel2_can_have_unlimited_model1s());
        modelLinkRepository.save(link);
    }

    @Transactional
    public void deleteModelLink(DeleteModelLinkRequest updateLinkedModelsRequest) {
        //Validate Model Link ID
        ModelLink link = modelLinkRepository.findById(updateLinkedModelsRequest.getModelLinkId()).orElseThrow(() -> new RuntimeException("Link with id " + updateLinkedModelsRequest.getModelLinkId() + " does not exist"));
        modelLinkRepository.deleteById(link.getId());

        //Now delete the references table
        String sql = "DROP TABLE model_links_" + sanitizeIdentifier(link.getId().toString());
        jdbcTemplate.update(sql);
    }


    @Transactional
    public void deleteField(UUID fieldId) {
        // First check if the field exists
        Field field = fieldRepository.findById(fieldId).orElseThrow(() -> new IllegalArgumentException("Invalid field id"));
        Model model = modelRepository.findById(field.getModelId()).orElseThrow(() -> new IllegalArgumentException("Invalid field id"));
        UUID modelId = model.getId();
        String sql = "ALTER TABLE " + "records_" + sanitizeIdentifier(modelId.toString()) + " DROP COLUMN " + "field_" + sanitizeIdentifier(fieldId.toString());
        jdbcTemplate.execute(sql);
        fieldRepository.deleteById(fieldId);
    }

    @Transactional
    public void addField(UUID modelId, FieldDto fieldDto) {
        Model model = modelRepository.findById(modelId).orElseThrow(() -> new IllegalArgumentException("Invalid field id"));
        Field field = new Field();
        field.setDescription(fieldDto.getDescription());
        field.setName(fieldDto.getName());
        field.setIsRequired(fieldDto.getIsRequired());
        field.setType(fieldDto.getType());
        field.setModelId(modelId);
        fieldRepository.save(field);

        UUID fieldId = field.getId();
        if (fieldId == null) {
            throw new IllegalArgumentException("Field id is null");
        }

        String columnType = mapFieldTypeToPostgresType(fieldDto.getType());
        String sql = "ALTER TABLE records_" + sanitizeIdentifier(modelId.toString()) + " ADD COLUMN field_" + sanitizeIdentifier(fieldId.toString()) + " " + columnType;
        jdbcTemplate.execute(sql);
    }

    @Transactional
    public FieldDto duplicateField(UUID originalFieldId, DuplicateFieldRequest request) {
        // 1. Get original field
        Field originalField = fieldRepository.findById(originalFieldId)
                .orElseThrow(() -> new FrontendException("Field not found", ErrorCode.GENERAL_ERROR, true));

        UUID modelId = originalField.getModelId();

        // 2. Verify model exists
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new FrontendException("Model not found", ErrorCode.GENERAL_ERROR, true));

        // 3. Determine new field type (use provided type or default to original)
        FieldType originalType = originalField.getType();
        FieldType newType = request.getNewFieldType() != null ? request.getNewFieldType() : originalType;

        // 4. Validate type conversion is allowed
        validateFieldTypeConversion(originalType, newType);

        // 5. Check if a field with the new name already exists in this model
        List<Field> existingFields = fieldRepository.findByModelId(modelId);
        boolean nameExists = existingFields.stream()
                .anyMatch(f -> f.getName().equalsIgnoreCase(request.getNewFieldName()));

        if (nameExists) {
            throw new FrontendException("A field with the name '" + request.getNewFieldName() + "' already exists in this model", ErrorCode.GENERAL_ERROR, true);
        }

        // 6. Create new field with same properties but new name and potentially new type
        Field newField = new Field();
        newField.setModelId(modelId);
        newField.setName(request.getNewFieldName());
        newField.setType(newType);
        newField.setDescription(originalField.getDescription());
        newField.setIsRequired(false); // Always false initially for safety (user can enable later)
        fieldRepository.save(newField);

        UUID newFieldId = newField.getId();
        if (newFieldId == null) {
            throw new IllegalStateException("New field ID is null after save");
        }

        // 7. Add new column to records table
        String columnType = mapFieldTypeToPostgresType(newField.getType());
        String tableName = "records_" + sanitizeIdentifier(modelId.toString());
        String newColumnName = "field_" + sanitizeIdentifier(newFieldId.toString());

        String addColumnSql = "ALTER TABLE " + tableName + " ADD COLUMN " + newColumnName + " " + columnType;
        jdbcTemplate.execute(addColumnSql);

        log.info("Created duplicate field '{}' (ID: {}, type: {}) from original field '{}' (ID: {}, type: {})",
                newField.getName(), newFieldId, newType, originalField.getName(), originalFieldId, originalType);

        // 8. Copy data if requested (with type conversion if needed)
        if (request.isCopyData()) {
            try {
                copyFieldDataWithConversion(tableName, originalFieldId, originalType, newFieldId, newType);
            } catch (Exception e) {
                log.error("Failed to copy data from '{}' to '{}': {}", originalField.getName(), newField.getName(), e.getMessage());
                throw new FrontendException(
                        "Failed to copy and convert data. Some values may not be compatible with the new type (" + newType + "). " +
                                "Error: " + e.getMessage(),
                        ErrorCode.GENERAL_ERROR,
                        true
                );
            }
        }

        // 9. Convert to DTO and return
        FieldDto fieldDto = new FieldDto();
        fieldDto.setId(newField.getId());
        fieldDto.setName(newField.getName());
        fieldDto.setType(newField.getType());
        fieldDto.setDescription(newField.getDescription());
        fieldDto.setIsRequired(newField.getIsRequired());

        return fieldDto;
    }

    /**
     * Validates that a field type conversion is allowed
     */
    private void validateFieldTypeConversion(FieldType fromType, FieldType toType) {
        // Same type is always allowed
        if (fromType == toType) {
            return;
        }

        // Any type can convert to TEXT (SINGLE_LINE_TEXT or MULTI_LINE_TEXT)
        if (toType == FieldType.SINGLE_LINE_TEXT || toType == FieldType.MULTI_LINE_TEXT) {
            return;
        }

        // Define other allowed conversions
        boolean isAllowed = false;

        switch (fromType) {
            case SINGLE_LINE_TEXT:
                // SINGLE_LINE_TEXT can convert to: LONG, DECIMAL
                isAllowed = toType == FieldType.LONG || toType == FieldType.DECIMAL;
                break;

            case LONG:
                // LONG can convert to: DECIMAL
                isAllowed = toType == FieldType.DECIMAL;
                break;

            default:
                isAllowed = false;
        }

        if (!isAllowed) {
            throw new FrontendException(
                    "Cannot convert field type from " + fromType + " to " + toType + ". " +
                            "Allowed conversions: Any type -> (SINGLE_LINE_TEXT, MULTI_LINE_TEXT), " +
                            "SINGLE_LINE_TEXT -> (LONG, DECIMAL), LONG -> DECIMAL",
                    ErrorCode.GENERAL_ERROR,
                    true
            );
        }
    }

    /**
     * Copies data from original field to new field with type conversion
     */
    private void copyFieldDataWithConversion(String tableName, UUID originalFieldId, FieldType fromType, UUID newFieldId, FieldType toType) {
        String originalColumnName = "field_" + sanitizeIdentifier(originalFieldId.toString());
        String newColumnName = "field_" + sanitizeIdentifier(newFieldId.toString());

        String copyDataSql;

        // Determine if we need explicit type casting
        if (fromType == toType) {
            // Same type, simple copy
            copyDataSql = "UPDATE " + tableName + " SET " + newColumnName + " = " + originalColumnName;
        } else if (fromType == FieldType.SINGLE_LINE_TEXT && toType == FieldType.MULTI_LINE_TEXT) {
            // TEXT to TEXT, no casting needed
            copyDataSql = "UPDATE " + tableName + " SET " + newColumnName + " = " + originalColumnName;
        } else if (toType == FieldType.SINGLE_LINE_TEXT || toType == FieldType.MULTI_LINE_TEXT) {
            // Any type to TEXT - use CAST to convert to TEXT
            // PostgreSQL can convert BIGINT, NUMERIC, BOOLEAN, DATE, TIMESTAMP to TEXT
            copyDataSql = "UPDATE " + tableName + " SET " + newColumnName + " = CAST(" + originalColumnName + " AS TEXT)";
        } else if (fromType == FieldType.LONG && toType == FieldType.DECIMAL) {
            // BIGINT to NUMERIC, implicit cast works
            copyDataSql = "UPDATE " + tableName + " SET " + newColumnName + " = " + originalColumnName;
        } else if (fromType == FieldType.SINGLE_LINE_TEXT && toType == FieldType.LONG) {
            // TEXT to BIGINT - need explicit cast, will fail on non-numeric strings
            copyDataSql = "UPDATE " + tableName +
                    " SET " + newColumnName + " = CAST(" + originalColumnName + " AS BIGINT) " +
                    "WHERE " + originalColumnName + " IS NOT NULL " +
                    "AND " + originalColumnName + " ~ '^-?[0-9]+$'"; // Only copy valid integers
        } else if (fromType == FieldType.SINGLE_LINE_TEXT && toType == FieldType.DECIMAL) {
            // TEXT to NUMERIC - need explicit cast, will fail on non-numeric strings
            copyDataSql = "UPDATE " + tableName +
                    " SET " + newColumnName + " = CAST(" + originalColumnName + " AS NUMERIC) " +
                    "WHERE " + originalColumnName + " IS NOT NULL " +
                    "AND " + originalColumnName + " ~ '^-?[0-9]+(\\.[0-9]+)?$'"; // Only copy valid numbers
        } else {
            throw new IllegalStateException("Unhandled type conversion: " + fromType + " -> " + toType);
        }

        int rowsUpdated = jdbcTemplate.update(copyDataSql);
        log.info("Copied data from '{}' ({}) to '{}' ({}) - {} rows updated",
                originalColumnName, fromType, newColumnName, toType, rowsUpdated);
    }

    @Transactional
    public void updateModel(UUID modelId, UpdateModelRequest updateModelRequest) {
        Model model = modelRepository.findById(modelId).orElseThrow(() -> new IllegalArgumentException("Invalid model id"));
        model.setName(updateModelRequest.getModelName());
        model.setDescription(updateModelRequest.getModelDescription());
        modelRepository.save(model);
    }

    @Transactional
    public void updateField(UUID fieldId, UpdateFieldRequest updateFieldRequest) {
        Field field = fieldRepository.findById(fieldId).orElseThrow(() -> new IllegalArgumentException("Invalid field id"));
        field.setName(updateFieldRequest.getName());
        field.setIsRequired(updateFieldRequest.isRequired());
        field.setDescription(updateFieldRequest.getDescription());
        fieldRepository.save(field);
    }

    @Transactional(readOnly = true)
    public GetModelLinksResponse getModelLinks() {
        GetModelLinksResponse response = new GetModelLinksResponse();
        List<ModelLink> links = new ArrayList<>();
        modelLinkRepository.findAll().forEach(link -> links.add(link));
        List<ModelLinkDto> modelLinkDtos = new ArrayList<>();
        for (ModelLink link : links) {
            ModelLinkDto modelLinkDto = new ModelLinkDto();
            modelLinkDto.setModelLinkId(link.getId());
            modelLinkDto.setModel1Id(link.getModel1Id());
            modelLinkDto.setModel2Id(link.getModel2Id());
            modelLinkDto.setModel1_can_have_so_many_model2s_count(link.getModel1CanHaveSoManyModel2sCount());
            modelLinkDto.setModel2_can_have_so_many_model1s_count(link.getModel2CanHaveSoManyModel1sCount());
            modelLinkDto.setModel1_can_have_unlimited_model2s(link.isModel1CanHaveUnlimitedModel2s());
            modelLinkDto.setModel2_can_have_unlimited_model1s(link.isModel2CanHaveUnlimitedModel1s());
            // Get individual model Info
            Model model = modelRepository.findById(modelLinkDto.getModel1Id()).orElseThrow(() -> new IllegalArgumentException("Invalid model id"));
            modelLinkDto.setModel1Name(model.getName());
            model = modelRepository.findById(modelLinkDto.getModel2Id()).orElseThrow(() -> new IllegalArgumentException("Invalid model id"));
            modelLinkDto.setModel2Name(model.getName());
            modelLinkDtos.add(modelLinkDto);
        }
        response.setModelLinks(modelLinkDtos);
        return response;
    }

    @Transactional
    public void linkRecords(UUID userId, LinkRecordsRequest request) {


        // Perform permissions checks
        if (!userHasGivenPermissionOnModel(userId, request.getSourceModelId(), ModelPermission.VIEW_RECORDS)) {
            log.error("User does not have permission to link records: Permissions Needed: VIEW records on model with ID: [" + request.getSourceModelId() + "]");
            throw new FrontendException("VIEW permission required", ErrorCode.PERMISSION_DENIED, true);
        }

        if (!userHasGivenPermissionOnModel(userId, request.getSourceModelId(), ModelPermission.EDIT_RECORDS)) {
            log.error("User does not have permission to link records: Permissions Needed: EDIT records on model with ID: [" + request.getSourceModelId() + "]");
            throw new FrontendException("EDIT permission required", ErrorCode.PERMISSION_DENIED, true);
        }

        UUID modelLinkId = request.getModelLinkId();

        //Check if source model ID exists
        Model sourceModel = modelRepository.findById(request.getSourceModelId()).orElseThrow(() -> new IllegalArgumentException("Invalid model id"));
        ModelLink modelLink = modelLinkRepository.findById(modelLinkId).orElseThrow(() -> new IllegalArgumentException("Invalid model link id"));

        UUID targetModelId;
        int sourceRecord;
        if (modelLink.getModel1Id().equals(request.getSourceModelId())) {
            sourceRecord = 1;
            targetModelId = modelLink.getModel2Id();
        } else if (modelLink.getModel2Id().equals(request.getSourceModelId())) {
            sourceRecord = 2;
            targetModelId = modelLink.getModel1Id();
        }  else {
            throw new IllegalArgumentException("Invalid model Id");
        }

        // Perform permissions checks
        if (!userHasGivenPermissionOnModel(userId, targetModelId, ModelPermission.VIEW_RECORDS)) {
            log.error("User does not have permission to link records: Permissions Needed: VIEW records on model with ID: [" +targetModelId + "]");
            throw new FrontendException("VIEW permission required", ErrorCode.PERMISSION_DENIED, true);
        }

        if (!userHasGivenPermissionOnModel(userId, targetModelId, ModelPermission.EDIT_RECORDS)) {
            log.error("User does not have permission to link records: Permissions Needed: EDIT records on model with ID: [" +targetModelId + "]");
            throw new FrontendException("EDIT permission required", ErrorCode.PERMISSION_DENIED, true);
        }

        //Check if target model exists
        Model targetModel = modelRepository.findById(targetModelId).orElseThrow(() -> new IllegalArgumentException("Target Model not found"));

        //CHeck if source record exists
        String sourceTable = "records_" + sanitizeIdentifier(sourceModel.getId().toString());
        UUID sourceRecordId = request.getSourceRecordId();
        String sqlSourceExist = "SELECT EXISTS(SELECT 1 FROM " + sourceTable + " WHERE id = ?)";
        Boolean sourceExists = jdbcTemplate.queryForObject(sqlSourceExist, Boolean.class, sourceRecordId);
        if (!Boolean.TRUE.equals(sourceExists)) {
            throw new IllegalArgumentException("Source record does not exist");
        }

        //CHeck if target record exists
        String targetTable = "records_" + sanitizeIdentifier(sourceModel.getId().toString());
        UUID targetRecordId = request.getSourceRecordId();
        String sqlTargetExist = "SELECT EXISTS(SELECT 1 FROM " + targetTable + " WHERE id = ?)";
        Boolean targetExists = jdbcTemplate.queryForObject(sqlTargetExist, Boolean.class, targetRecordId);
        if (!Boolean.TRUE.equals(targetExists)) {
            throw new IllegalArgumentException("Target record does not exist");
        }

        String sql;
        String tableName = "model_links_" + sanitizeIdentifier(modelLinkId.toString());
        UUID record1 = request.getTargetRecordId();
        UUID record2 = request.getSourceRecordId();
        switch (sourceRecord) {
            case 1:
                record1 = request.getSourceRecordId();
                record2 = request.getTargetRecordId();
                break;
            case 2:
                record1 = request.getTargetRecordId();
                record2 = request.getSourceRecordId();
                break;
            default:
                throw new IllegalArgumentException("");
        }

        //Check if records are already linked
        String sqlExist = "SELECT EXISTS(SELECT 1 FROM " + tableName + " WHERE record1_id = ? AND record2_id = ?)";
        Boolean exists = jdbcTemplate.queryForObject(sqlExist, Boolean.class, record1, record2);
        if (Boolean.TRUE.equals(exists)) {
            throw new FrontendException("Records are already linked", true);
        }

        // Check if maximum link count is not exceeded
        // Check if model1 record can have more model2 links
        if (!modelLink.isModel1CanHaveUnlimitedModel2s()) {
            Long maxAllowed = modelLink.getModel1CanHaveSoManyModel2sCount();
            if (maxAllowed != null) {
                // Lock all matching link rows and get their IDs
                String lockSql = "SELECT id FROM " + tableName + " WHERE record1_id = ? FOR UPDATE";
                List<UUID> lockedLinkIds = jdbcTemplate.queryForList(lockSql, UUID.class, record1);
                long currentCount = lockedLinkIds.size();

                if (currentCount >= maxAllowed) {
                    throw new FrontendException("Maximum link count exceeded. This record can have at most " + maxAllowed + " linked records.", ErrorCode.SOURCE_RECORD_MAX_LINK_COUNT_EXCEEDED, true);
                }
            }
        }

        // Check if model2 record can have more model1 links
        if (!modelLink.isModel2CanHaveUnlimitedModel1s()) {
            Long maxAllowed = modelLink.getModel2CanHaveSoManyModel1sCount();
            if (maxAllowed != null) {
                // Lock all matching link rows and get their IDs
                String lockSql = "SELECT id FROM " + tableName + " WHERE record2_id = ? FOR UPDATE";
                List<UUID> lockedLinkIds = jdbcTemplate.queryForList(lockSql, UUID.class, record2);
                long currentCount = lockedLinkIds.size();

                if (currentCount >= maxAllowed) {
                    throw new FrontendException("Maximum link count exceeded. This record can have at most " + maxAllowed + " linked records.", ErrorCode.TARGET_RECORD_MAX_LINK_COUNT_EXCEEDED, true);
                }
            }
        }

        sql = "INSERT INTO " + tableName + " (record1_id, record2_id) VALUES " +
                "('" + record1 + "','" + record2 + "')";

        jdbcTemplate.execute(sql);
    }

    @Transactional(readOnly = true)
    public List<LinkedRecordCount> getLinkedRecordCounts(UUID sourceModelId, UUID sourceRecordId) {
        List<ModelLink> modelLinks = modelLinkRepository.findByEitherModelId(sourceModelId);
        List <LinkedRecordCount> linkedRecordCounts = new ArrayList<>();
        UUID targetModelId;
        int record;
        for (ModelLink modelLink : modelLinks) {
            if (modelLink.getModel1Id().equals(sourceModelId)) {
                targetModelId = modelLink.getModel2Id();
                record = 1;
            } else if (modelLink.getModel2Id().equals(sourceModelId)) {
                targetModelId = modelLink.getModel1Id();
                record = 2;
            } else {
                throw new IllegalArgumentException("Invalid model id");
            }
            Model targetModel = modelRepository.findById(targetModelId).orElseThrow(() -> new IllegalArgumentException("Invalid model id"));
            String sql = "SELECT count(*) FROM model_links_" + sanitizeIdentifier(modelLink.getId().toString()) +
                    " WHERE record" + record + "_id = ?";

            long count = jdbcTemplate.queryForObject(sql, Long.class, sourceRecordId);

            LinkedRecordCount linkedRecordCount = new LinkedRecordCount();
            linkedRecordCount.setTargetModelName(targetModel.getName());
            linkedRecordCount.setTargetModelId(targetModelId);
            linkedRecordCount.setRecordCount(count);
            linkedRecordCounts.add(linkedRecordCount);
        }
        return linkedRecordCounts;
    }

    @Transactional
    public void deleteModel(UUID modelId, boolean force) {
        //Validate model id
        modelRepository.findById(modelId).orElseThrow(() -> new IllegalArgumentException("Model not found"));



        List<LinkedRecordCount> linkedRecordCounts = getLinkedRecordCounts(modelId);
        for (LinkedRecordCount linkedRecordCount : linkedRecordCounts) {
            // Check if records are linked to model, unless force is true
            if (linkedRecordCount.getRecordCount() > 0) {
                if (!force) {
                    throw new FrontendException("Records are already linked", ErrorCode.MODEL_HAS_LINKED_RECORDS, true);
                }
            }
        }

        //DROP each model_link_ table associated with model
        for (LinkedRecordCount linkedRecordCount : linkedRecordCounts) {
            String sql = "DROP TABLE model_links_" + sanitizeIdentifier(linkedRecordCount.getLinkId().toString());
            jdbcTemplate.execute(sql);
        }
        //now remove the model link entries
        for (LinkedRecordCount linkedRecordCount : linkedRecordCounts) {
            String sql = "DELETE FROM model_links WHERE id = ?";
            jdbcTemplate.update(sql, linkedRecordCount.getLinkId());
        }
        //Now delete the records table
        {
            String sql = "DROP TABLE records_" + sanitizeIdentifier(modelId.toString());
            jdbcTemplate.execute(sql);
        }

        //Delete the fields from fields table
        {
            String sql = "DELETE FROM fields WHERE model_id = ?";
            jdbcTemplate.update(sql, modelId);
        }

        //Remove the model from models table
        String sql = "DELETE FROM models WHERE id = ?";
        jdbcTemplate.update(sql, modelId);

    }

    @Transactional(readOnly = true)
    public List<LinkedRecordCount> getLinkedRecordCounts(UUID sourceModelId) {
        List<ModelLink> modelLinks = modelLinkRepository.findByEitherModelId(sourceModelId);
        List <LinkedRecordCount> linkedRecordCounts = new ArrayList<>();
        UUID targetModelId;
        for (ModelLink modelLink : modelLinks) {
            if (modelLink.getModel1Id().equals(sourceModelId)) {
                targetModelId = modelLink.getModel2Id();
            } else if (modelLink.getModel2Id().equals(sourceModelId)) {
                targetModelId = modelLink.getModel1Id();
            } else {
                throw new IllegalArgumentException("Invalid model id");
            }
            Model targetModel = modelRepository.findById(targetModelId).orElseThrow(() -> new IllegalArgumentException("Invalid model id"));
            String sql = "SELECT count(*) FROM model_links_" + sanitizeIdentifier(modelLink.getId().toString());

            long count = jdbcTemplate.queryForObject(sql, Long.class);

            LinkedRecordCount linkedRecordCount = new LinkedRecordCount();
            linkedRecordCount.setLinkId(modelLink.getId());
            linkedRecordCount.setTargetModelName(targetModel.getName());
            linkedRecordCount.setTargetModelId(targetModelId);
            linkedRecordCount.setRecordCount(count);
            linkedRecordCounts.add(linkedRecordCount);
        }
        return linkedRecordCounts;
    }

    @Transactional
    public void deleteRecordLink(UUID userId, UUID modelLinkId, UUID linkId) {
        //Check UUID's
        ModelLink modelLink = modelLinkRepository.findById(modelLinkId).orElseThrow(() -> new RuntimeException("modelLink not found"));

        // Perform permissions checks
        if (!userHasGivenPermissionOnModel(userId, modelLink.getModel1Id(), ModelPermission.VIEW_RECORDS)) {
            log.error("Cannot unlink records: User with ID: [" + userId + "] does not have VIEW permission on model with ID: [" + modelLink.getModel1Id() + "]");
            throw new FrontendException("VIEW permission required", ErrorCode.PERMISSION_DENIED, true);
        }

        if (!userHasGivenPermissionOnModel(userId, modelLink.getModel1Id(), ModelPermission.EDIT_RECORDS)) {
            log.error("Cannot unlink records: User with ID: [" + userId + "] does not have EDIT permission on model with ID: [" + modelLink.getModel1Id() + "]");
            throw new FrontendException("EDIT permission required", ErrorCode.PERMISSION_DENIED, true);
        }

        if (!userHasGivenPermissionOnModel(userId, modelLink.getModel2Id(), ModelPermission.VIEW_RECORDS)) {
            log.error("Cannot unlink records: User with ID: [" + userId + "] does not have VIEW permission on model with ID: [" + modelLink.getModel2Id() + "]");
            throw new FrontendException("VIEW permission required", ErrorCode.PERMISSION_DENIED, true);
        }

        if (!userHasGivenPermissionOnModel(userId, modelLink.getModel2Id(), ModelPermission.EDIT_RECORDS)) {
            log.error("Cannot unlink records: User with ID: [" + userId + "] does not have EDIT permission on model with ID: [" + modelLink.getModel2Id() + "]");
            throw new FrontendException("EDIT permission required", ErrorCode.PERMISSION_DENIED, true);
        }

        String sql = "SELECT EXISTS(SELECT 1 FROM model_links_" + sanitizeIdentifier(modelLinkId.toString()) + " WHERE id = ?)";
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, linkId);
        if (!Boolean.TRUE.equals(exists)) {
            throw new IllegalArgumentException("Link not found");
        }
        //Delete link
        String sqlDelete = "DELETE FROM model_links_" + sanitizeIdentifier(modelLinkId.toString()) + " WHERE id = ?";
        jdbcTemplate.update(sqlDelete, linkId);
    }
}
