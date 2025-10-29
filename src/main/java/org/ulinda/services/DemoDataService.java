package org.ulinda.services;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ulinda.dto.CreateModelRequest;
import org.ulinda.dto.FieldDto;
import org.ulinda.dto.GetModelsResponse;
import org.ulinda.dto.ModelDto;
import org.ulinda.dto.LinkModelsRequest;
import org.ulinda.dto.LinkRecordsRequest;
import org.ulinda.dto.GetRecordsRequest;
import org.ulinda.dto.GetRecordsResponse;
import org.ulinda.dto.RecordDto;
import org.ulinda.dto.GetModelResponse;
import org.ulinda.dto.ModelLinkTarget;
import org.ulinda.enums.FieldType;
import org.ulinda.enums.QueryType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Collections;

@Service
@Slf4j
public class DemoDataService {

    @Autowired
    private ModelService modelService;

    @Autowired
    private UserService userService;

    private UUID adminUserId = null;

    // Sample data arrays
    private static final String[] FIRST_NAMES = {
            "John", "Jane", "Michael", "Sarah", "David", "Emily", "Robert", "Lisa", "James", "Mary",
            "William", "Patricia", "Richard", "Jennifer", "Charles", "Linda", "Joseph", "Elizabeth",
            "Thomas", "Barbara", "Christopher", "Susan", "Daniel", "Jessica", "Paul", "Helen",
            "Mark", "Nancy", "Donald", "Betty", "Steven", "Helen", "Andrew", "Sandra", "Kenneth",
            "Donna", "Joshua", "Carol", "Kevin", "Ruth", "Brian", "Sharon", "George", "Michelle",
            "Edward", "Laura", "Ronald", "Sarah", "Timothy", "Kimberly", "Jason", "Deborah",
            "Jeffrey", "Dorothy", "Ryan", "Lisa", "Jacob", "Nancy", "Gary", "Karen", "Nicholas",
            "Betty", "Eric", "Helen", "Jonathan", "Sandra", "Stephen", "Donna", "Larry", "Carol",
            "Justin", "Ruth", "Scott", "Sharon", "Brandon", "Michelle", "Benjamin", "Laura",
            "Samuel", "Sarah", "Gregory", "Kimberly", "Frank", "Deborah", "Raymond", "Dorothy",
            "Alexander", "Lisa", "Patrick", "Nancy", "Jack", "Karen", "Dennis", "Betty",
            "Jerry", "Helen", "Tyler", "Sandra", "Aaron", "Donna", "Jose", "Carol", "Henry", "Ruth"
    };

    private static final String[] LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez",
            "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor",
            "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez",
            "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young", "Allen", "King", "Wright",
            "Scott", "Torres", "Nguyen", "Hill", "Flores", "Green", "Adams", "Nelson", "Baker",
            "Hall", "Rivera", "Campbell", "Mitchell", "Carter", "Roberts", "Gomez", "Phillips",
            "Evans", "Turner", "Diaz", "Parker", "Cruz", "Edwards", "Collins", "Reyes", "Stewart",
            "Morris", "Morales", "Murphy", "Cook", "Rogers", "Gutierrez", "Ortiz", "Morgan",
            "Cooper", "Peterson", "Bailey", "Reed", "Kelly", "Howard", "Ramos", "Kim", "Cox",
            "Ward", "Richardson", "Watson", "Brooks", "Chavez", "Wood", "James", "Bennett",
            "Gray", "Mendoza", "Ruiz", "Hughes", "Price", "Alvarez", "Castillo", "Sanders",
            "Patel", "Myers", "Long", "Ross", "Foster", "Jimenez", "Powell", "Jenkins", "Perry",
            "Russell", "Sullivan", "Bell", "Coleman", "Butler", "Henderson", "Barnes", "Gonzales"
    };

    private static final String[] MOTORBIKE_BRANDS = {
            "Harley-Davidson", "Honda", "Yamaha", "Kawasaki", "Suzuki", "BMW", "Ducati", "KTM",
            "Triumph", "Indian", "Aprilia", "Husqvarna", "Royal Enfield", "Moto Guzzi", "MV Agusta",
            "Benelli", "CFMoto", "Zero", "Norton", "Buell"
    };

    private static final String[] TRANSACTION_TYPES = {
            "Purchase", "Refund", "Transfer", "Payment", "Deposit", "Withdrawal", "Fee", "Interest",
            "Subscription", "Salary", "Bonus", "Expense", "Invoice", "Credit", "Debit", "Adjustment"
    };

    private static final String[] TRANSACTION_STATUSES = {
            "Completed", "Pending", "Failed", "Cancelled", "Processing", "Approved", "Rejected", "On Hold"
    };

    private static final String[] TRANSACTION_DESCRIPTIONS = {
            "Online purchase from vendor", "Refund processed for order", "Bank transfer to account",
            "Monthly subscription payment", "Salary deposit", "ATM withdrawal", "Service fee charge",
            "Interest payment received", "Credit card payment", "Utility bill payment", "Insurance premium",
            "Rent payment", "Grocery store purchase", "Gas station payment", "Restaurant bill",
            "Online streaming service", "Software license purchase", "Equipment rental fee",
            "Consulting services payment", "Travel expense reimbursement", "Conference registration fee",
            "Office supplies purchase", "Marketing campaign cost", "Legal services fee", "Audit expense",
            "Training course payment", "Equipment maintenance", "Security service fee", "Cleaning service",
            "Internet service payment", "Phone bill payment", "Parking fee", "Toll road charge",
            "Taxi fare payment", "Hotel accommodation", "Flight booking", "Car rental fee",
            "Medical expense", "Pharmacy purchase", "Gym membership fee", "Investment deposit",
            "Loan payment", "Credit card annual fee", "Bank overdraft fee", "Currency exchange",
            "International transfer", "Wire transfer fee", "Check processing fee", "Account maintenance"
    };

    private static final Random random = new Random();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void loadDemoData() {
        adminUserId = userService.getAdminUserId();
        createModels();
        log.info("Loading Demo Data...");
        loadEmployeeData();
        loadMotorbikeData();
        loadPayslipData();
        log.info("Linking Demo Data...");
        loadTransactionData();
        linkEmployeesToPayslips();
        linkEmployeesToMotorbikes();
        log.info("Demo Data Loaded!");
    }

    private void createModels() {
        {
            CreateModelRequest modelRequest = new CreateModelRequest();
            modelRequest.setName("Employees");
            modelRequest.setDescription("List of Employees");
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("Name");
                fieldDto.setType(FieldType.SINGLE_LINE_TEXT);
                modelRequest.getFields().add(fieldDto);
            }
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("Surname");
                fieldDto.setType(FieldType.SINGLE_LINE_TEXT);
                modelRequest.getFields().add(fieldDto);
            }
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("ID Number");
                fieldDto.setType(FieldType.SINGLE_LINE_TEXT);
                modelRequest.getFields().add(fieldDto);
            }
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("Age");
                fieldDto.setType(FieldType.SINGLE_LINE_TEXT);
                modelRequest.getFields().add(fieldDto);
            }

            modelService.createModel(modelRequest, null);
        }
        {
            CreateModelRequest modelRequest = new CreateModelRequest();
            modelRequest.setName("Motorbikes");
            modelRequest.setDescription("List of Motorbikes");
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("Brand");
                fieldDto.setType(FieldType.SINGLE_LINE_TEXT);
                modelRequest.getFields().add(fieldDto);
            }
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("Model year");
                fieldDto.setType(FieldType.LONG);
                modelRequest.getFields().add(fieldDto);
            }
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("Engine Number");
                fieldDto.setType(FieldType.SINGLE_LINE_TEXT);
                modelRequest.getFields().add(fieldDto);
            }
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("License Plate Number");
                fieldDto.setType(FieldType.SINGLE_LINE_TEXT);
                modelRequest.getFields().add(fieldDto);
            }
            modelService.createModel(modelRequest, null);
        }
        {
            CreateModelRequest modelRequest = new CreateModelRequest();
            modelRequest.setName("Payslips");
            modelRequest.setDescription("List of Payslips");
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("Payslip Number");
                fieldDto.setType(FieldType.SINGLE_LINE_TEXT);
                modelRequest.getFields().add(fieldDto);
            }
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("Date");
                fieldDto.setType(FieldType.SINGLE_LINE_TEXT);
                modelRequest.getFields().add(fieldDto);
            }
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("Amount");
                fieldDto.setType(FieldType.DECIMAL);
                modelRequest.getFields().add(fieldDto);
            }
            modelService.createModel(modelRequest, null);
        }
        {
            CreateModelRequest modelRequest = new CreateModelRequest();
            modelRequest.setName("Transactions");
            modelRequest.setDescription("Financial Transaction Records");
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("Transaction ID");
                fieldDto.setType(FieldType.SINGLE_LINE_TEXT);
                modelRequest.getFields().add(fieldDto);
            }
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("Amount");
                fieldDto.setType(FieldType.DECIMAL);
                modelRequest.getFields().add(fieldDto);
            }
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("Type");
                fieldDto.setType(FieldType.SINGLE_LINE_TEXT);
                modelRequest.getFields().add(fieldDto);
            }
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("Status");
                fieldDto.setType(FieldType.SINGLE_LINE_TEXT);
                modelRequest.getFields().add(fieldDto);
            }
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("Description");
                fieldDto.setType(FieldType.SINGLE_LINE_TEXT);
                modelRequest.getFields().add(fieldDto);
            }
            {
                FieldDto fieldDto = new FieldDto();
                fieldDto.setName("Date");
                fieldDto.setType(FieldType.SINGLE_LINE_TEXT);
                modelRequest.getFields().add(fieldDto);
            }
            modelService.createModel(modelRequest, null);
        }
    }

    private void loadEmployeeData() {
        GetModelsResponse modelsResponse = modelService.getModels(adminUserId);

        for (ModelDto model : modelsResponse.getModels()) {
            if (model.getName().equals("Employees")) {
                UUID modelId = model.getId();

                // Create field ID map for easy lookup
                Map<String, UUID> fieldMap = new HashMap<>();
                for (FieldDto field : model.getFields()) {
                    fieldMap.put(field.getName(), field.getId());
                }

                // Create 100 employee records
                for (int i = 0; i < 100; i++) {
                    Map<UUID, Object> fieldValues = new HashMap<>();

                    // Generate random employee data
                    String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                    String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                    String idNumber = generateIdNumber();
                    int age = 18 + random.nextInt(47); // Age between 18-64

                    fieldValues.put(fieldMap.get("Name"), firstName);
                    fieldValues.put(fieldMap.get("Surname"), lastName);
                    fieldValues.put(fieldMap.get("ID Number"), idNumber);
                    fieldValues.put(fieldMap.get("Age"), String.valueOf(age));

                    modelService.createRecord(adminUserId, modelId, fieldValues);
                }
                break;
            }
        }
    }

    private void loadMotorbikeData() {
        GetModelsResponse modelsResponse = modelService.getModels(adminUserId);

        for (ModelDto model : modelsResponse.getModels()) {
            if (model.getName().equals("Motorbikes")) {
                UUID modelId = model.getId();

                // Create field ID map for easy lookup
                Map<String, UUID> fieldMap = new HashMap<>();
                for (FieldDto field : model.getFields()) {
                    fieldMap.put(field.getName(), field.getId());
                }

                // Create 100 motorbike records
                for (int i = 0; i < 100; i++) {
                    Map<UUID, Object> fieldValues = new HashMap<>();

                    // Generate random motorbike data
                    String brand = MOTORBIKE_BRANDS[random.nextInt(MOTORBIKE_BRANDS.length)];
                    Long modelYear = (long) (2000 + random.nextInt(25)); // Years 2000-2024
                    String engineNumber = generateEngineNumber();
                    String licensePlate = generateLicensePlate();

                    fieldValues.put(fieldMap.get("Brand"), brand);
                    fieldValues.put(fieldMap.get("Model year"), modelYear);
                    fieldValues.put(fieldMap.get("Engine Number"), engineNumber);
                    fieldValues.put(fieldMap.get("License Plate Number"), licensePlate);

                    modelService.createRecord(adminUserId, modelId, fieldValues);
                }
                break;
            }
        }
    }

    private void loadPayslipData() {
        GetModelsResponse modelsResponse = modelService.getModels(adminUserId);

        for (ModelDto model : modelsResponse.getModels()) {
            if (model.getName().equals("Payslips")) {
                UUID modelId = model.getId();

                // Create field ID map for easy lookup
                Map<String, UUID> fieldMap = new HashMap<>();
                for (FieldDto field : model.getFields()) {
                    fieldMap.put(field.getName(), field.getId());
                }

                // Create 100 payslip records
                for (int i = 0; i < 100; i++) {
                    Map<UUID, Object> fieldValues = new HashMap<>();

                    // Generate random payslip data
                    String payslipNumber = "PS" + String.format("%06d", i + 1);
                    LocalDate payDate = generateRandomDate();
                    BigDecimal amount = generateRandomSalary();

                    fieldValues.put(fieldMap.get("Payslip Number"), payslipNumber);
                    fieldValues.put(fieldMap.get("Date"), payDate.format(DATE_FORMATTER));
                    fieldValues.put(fieldMap.get("Amount"), amount);

                    modelService.createRecord(adminUserId, modelId, fieldValues);
                }
                break;
            }
        }
    }

    private void loadTransactionData() {
        GetModelsResponse modelsResponse = modelService.getModels(adminUserId);

        for (ModelDto model : modelsResponse.getModels()) {
            if (model.getName().equals("Transactions")) {
                UUID modelId = model.getId();

                // Create field ID map for easy lookup
                Map<String, UUID> fieldMap = new HashMap<>();
                for (FieldDto field : model.getFields()) {
                    fieldMap.put(field.getName(), field.getId());
                }

                // Create 100,000 transaction records
                for (int i = 0; i < 10000; i++) {
                    Map<UUID, Object> fieldValues = new HashMap<>();

                    // Generate transaction data
                    String transactionId = "TXN-" + String.format("%08d", i + 1);
                    String type = TRANSACTION_TYPES[random.nextInt(TRANSACTION_TYPES.length)];
                    String status = TRANSACTION_STATUSES[random.nextInt(TRANSACTION_STATUSES.length)];
                    String description = TRANSACTION_DESCRIPTIONS[random.nextInt(TRANSACTION_DESCRIPTIONS.length)];
                    BigDecimal amount = generateTransactionAmount(type);
                    LocalDate transactionDate = generateRandomTransactionDate();

                    fieldValues.put(fieldMap.get("Transaction ID"), transactionId);
                    fieldValues.put(fieldMap.get("Amount"), amount);
                    fieldValues.put(fieldMap.get("Type"), type);
                    fieldValues.put(fieldMap.get("Status"), status);
                    fieldValues.put(fieldMap.get("Description"), description);
                    fieldValues.put(fieldMap.get("Date"), transactionDate.format(DATE_FORMATTER));

                    modelService.createRecord(adminUserId, modelId, fieldValues);
                }
                break;
            }
        }
    }

    // Helper methods to generate realistic fake data

    private String generateIdNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 13; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String generateEngineNumber() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();

        // Add 2-3 letters
        for (int i = 0; i < 2 + random.nextInt(2); i++) {
            sb.append(letters.charAt(random.nextInt(letters.length())));
        }

        // Add 6-8 digits
        for (int i = 0; i < 6 + random.nextInt(3); i++) {
            sb.append(random.nextInt(10));
        }

        return sb.toString();
    }

    private String generateLicensePlate() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();

        // Format: ABC-123-GP (South African style)
        for (int i = 0; i < 3; i++) {
            sb.append(letters.charAt(random.nextInt(letters.length())));
        }
        sb.append("-");

        for (int i = 0; i < 3; i++) {
            sb.append(random.nextInt(10));
        }
        sb.append("-");

        for (int i = 0; i < 2; i++) {
            sb.append(letters.charAt(random.nextInt(letters.length())));
        }

        return sb.toString();
    }

    private LocalDate generateRandomDate() {
        // Generate dates within the last 2 years
        LocalDate startDate = LocalDate.now().minusYears(2);
        long daysBetween = LocalDate.now().toEpochDay() - startDate.toEpochDay();
        long randomDays = random.nextLong(daysBetween);
        return startDate.plusDays(randomDays);
    }

    private BigDecimal generateRandomSalary() {
        // Generate salary between 15,000 and 150,000
        double minSalary = 15000.0;
        double maxSalary = 150000.0;
        double salary = minSalary + (random.nextDouble() * (maxSalary - minSalary));
        return BigDecimal.valueOf(Math.round(salary * 100.0) / 100.0);
    }

    private BigDecimal generateTransactionAmount(String type) {
        // Generate realistic amounts based on transaction type
        double amount = switch (type) {
            case "Salary", "Bonus" -> 25000.0 + (random.nextDouble() * 75000.0); // 25K - 100K
            case "Fee", "Interest" -> 5.0 + (random.nextDouble() * 500.0); // 5 - 505
            case "Withdrawal", "ATM" -> 20.0 + (random.nextDouble() * 1000.0); // 20 - 1020
            case "Purchase", "Payment" -> 10.0 + (random.nextDouble() * 5000.0); // 10 - 5010
            case "Refund" -> 15.0 + (random.nextDouble() * 2000.0); // 15 - 2015
            case "Transfer", "Deposit" -> 100.0 + (random.nextDouble() * 50000.0); // 100 - 50100
            default -> 1.0 + (random.nextDouble() * 1000.0); // 1 - 1001
        };
        return BigDecimal.valueOf(Math.round(amount * 100.0) / 100.0);
    }

    private LocalDate generateRandomTransactionDate() {
        // Generate dates within the last 3 years for more transaction history
        LocalDate startDate = LocalDate.now().minusYears(3);
        long daysBetween = LocalDate.now().toEpochDay() - startDate.toEpochDay();
        long randomDays = random.nextLong(daysBetween);
        return startDate.plusDays(randomDays);
    }

    private void linkEmployeesToPayslips() {
        GetModelsResponse modelsResponse = modelService.getModels(adminUserId);
        UUID employeesModelId = null;
        UUID payslipsModelId = null;

        // Find the model IDs for Employees and Payslips
        for (ModelDto model : modelsResponse.getModels()) {
            if (model.getName().equals("Employees")) {
                employeesModelId = model.getId();
            } else if (model.getName().equals("Payslips")) {
                payslipsModelId = model.getId();
            }
        }

        if (employeesModelId == null || payslipsModelId == null) {
            throw new RuntimeException("Could not find Employees or Payslips model");
        }

        // Create the model link (one-to-one relationship: each employee has one payslip)
        LinkModelsRequest linkModelsRequest = new LinkModelsRequest();
        linkModelsRequest.setModel1Id(employeesModelId);
        linkModelsRequest.setModel2Id(payslipsModelId);
        linkModelsRequest.setModel1_can_have_unlimited_model2s(false);
        linkModelsRequest.setModel2_can_have_unlimited_model1s(false);
        linkModelsRequest.setModel1_can_have_so_many_model2s_count(1L);
        linkModelsRequest.setModel2_can_have_so_many_model1s_count(1L);

        modelService.linkModels(linkModelsRequest);

        // Get the model link ID by calling getModel on one of the models
        GetModelResponse employeesModelResponse = modelService.getModel(employeesModelId, adminUserId);
        UUID modelLinkId = null;

        for (ModelLinkTarget linkTarget : employeesModelResponse.getModelLinkTargets()) {
            if (linkTarget.getTargetModelId().equals(payslipsModelId)) {
                modelLinkId = linkTarget.getModelLinkId();
                break;
            }
        }

        if (modelLinkId == null) {
            throw new RuntimeException("Could not find the created Employee-Payslip model link");
        }

        // Get all employee records
        GetRecordsRequest employeeRequest = new GetRecordsRequest();
        employeeRequest.setQueryType(QueryType.ALL_RECORDS);
        employeeRequest.setLimit(1000);
        employeeRequest.setSortField("created_at");
        GetRecordsResponse employeesResponse = modelService.getRecords(adminUserId, employeeRequest, employeesModelId);

        // Get all payslip records
        GetRecordsRequest payslipRequest = new GetRecordsRequest();
        payslipRequest.setQueryType(QueryType.ALL_RECORDS);
        payslipRequest.setLimit(1000);
        payslipRequest.setSortField("created_at");
        GetRecordsResponse payslipsResponse = modelService.getRecords(adminUserId, payslipRequest, payslipsModelId);

        // Link each employee to exactly one payslip (1:1 relationship)
        List<RecordDto> employees = employeesResponse.getRecords();
        List<RecordDto> payslips = payslipsResponse.getRecords();

        for (int i = 0; i < Math.min(employees.size(), payslips.size()); i++) {
            LinkRecordsRequest linkRecordsRequest = new LinkRecordsRequest();
            linkRecordsRequest.setModelLinkId(modelLinkId);
            linkRecordsRequest.setSourceModelId(employeesModelId);
            linkRecordsRequest.setSourceRecordId(employees.get(i).getId());
            linkRecordsRequest.setTargetRecordId(payslips.get(i).getId());

            modelService.linkRecords(adminUserId, linkRecordsRequest);
        }
    }

    private void linkEmployeesToMotorbikes() {
        GetModelsResponse modelsResponse = modelService.getModels(adminUserId);
        UUID employeesModelId = null;
        UUID motorbikesModelId = null;

        // Find the model IDs for Employees and Motorbikes
        for (ModelDto model : modelsResponse.getModels()) {
            if (model.getName().equals("Employees")) {
                employeesModelId = model.getId();
            } else if (model.getName().equals("Motorbikes")) {
                motorbikesModelId = model.getId();
            }
        }

        if (employeesModelId == null || motorbikesModelId == null) {
            throw new RuntimeException("Could not find Employees or Motorbikes model");
        }

        // Create the model link (one employee can have multiple motorbikes)
        LinkModelsRequest linkModelsRequest = new LinkModelsRequest();
        linkModelsRequest.setModel1Id(employeesModelId);
        linkModelsRequest.setModel2Id(motorbikesModelId);
        linkModelsRequest.setModel1_can_have_unlimited_model2s(true);
        linkModelsRequest.setModel2_can_have_unlimited_model1s(true);
        linkModelsRequest.setModel1_can_have_so_many_model2s_count(null);
        linkModelsRequest.setModel2_can_have_so_many_model1s_count(null);

        modelService.linkModels(linkModelsRequest);

        // Get the model link ID by calling getModel on one of the models
        GetModelResponse employeesModelResponse = modelService.getModel(employeesModelId, adminUserId);
        UUID modelLinkId = null;

        for (ModelLinkTarget linkTarget : employeesModelResponse.getModelLinkTargets()) {
            if (linkTarget.getTargetModelId().equals(motorbikesModelId)) {
                modelLinkId = linkTarget.getModelLinkId();
                break;
            }
        }

        if (modelLinkId == null) {
            throw new RuntimeException("Could not find the created model link");
        }

        // Get all employee records
        GetRecordsRequest employeeRequest = new GetRecordsRequest();
        employeeRequest.setLimit(1000);
        employeeRequest.setQueryType(QueryType.ALL_RECORDS);
        employeeRequest.setSortField("created_at");
        GetRecordsResponse employeesResponse = modelService.getRecords(adminUserId, employeeRequest, employeesModelId);

        // Get all motorbike records
        GetRecordsRequest motorbikeRequest = new GetRecordsRequest();
        motorbikeRequest.setLimit(1000);
        motorbikeRequest.setQueryType(QueryType.ALL_RECORDS);
        motorbikeRequest.setSortField("created_at");
        GetRecordsResponse motorbikesResponse = modelService.getRecords(adminUserId, motorbikeRequest, motorbikesModelId);

        // Link each employee to 50 random motorbikes
        List<RecordDto> employees = employeesResponse.getRecords();
        List<RecordDto> motorbikes = motorbikesResponse.getRecords();

        // Link each employee to 50 randomly selected motorbikes
        int motorbikeId = 0;
        for (RecordDto employee : employees) {

            // Create a copy of the motorbikes list for shuffling
            List<RecordDto> availableMotorbikes = new ArrayList<>(motorbikes);
            //Collections.shuffle(availableMotorbikes, random);

            // Link to the first 5 motorbikes after shuffling
            int bikesToLink = Math.min(5, availableMotorbikes.size());
            for (int j = 0; j < bikesToLink; j++) {
                if (motorbikeId >= 100) {
                    motorbikeId = 0;
                }
                LinkRecordsRequest linkRecordsRequest = new LinkRecordsRequest();
                linkRecordsRequest.setModelLinkId(modelLinkId);
                linkRecordsRequest.setSourceModelId(employeesModelId);
                linkRecordsRequest.setSourceRecordId(employee.getId());
                linkRecordsRequest.setTargetRecordId(availableMotorbikes.get(motorbikeId).getId());
                modelService.linkRecords(adminUserId, linkRecordsRequest);
                motorbikeId ++;
            }

        }
    }
}