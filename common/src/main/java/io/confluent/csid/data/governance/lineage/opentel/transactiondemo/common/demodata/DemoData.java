package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.demodata;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Address;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Card;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.ContactDetails;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Location;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Merchant;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Payee;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Transaction;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionStatus;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionStatus.Status;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

public class DemoData {

    @Getter
    private static final Map<String, ScenarioData> scenarioData = initDemoData();


    public static Transaction getInitialTransaction(ScenarioData scenarioData) {
        return buildTransaction(
            scenarioData.getTransactionId(),
            scenarioData.getCard().getCardNumber(),
            scenarioData.getCard().getCvv(),
            scenarioData.getCard().getExpiry(),
            scenarioData.getPayee().getFirstName(),
            scenarioData.getPayee().getLastName(),
            scenarioData.getMerchant().getId(),
            scenarioData.getTransactionLocation().getLatitude(),
            scenarioData.getTransactionLocation().getLongitude(),
            scenarioData.getAmount()
        );
    }

    private static Map<String, ScenarioData> initDemoData() {
        Map<String, ScenarioData> data = new HashMap<>();
        Address payeeAddress = new Address("12 Somerset Drive", "", "Phoenix", "Arizona", "85006", "USA");
        ContactDetails contactDetails = new ContactDetails("932142344", "Loraine.Sr142@hotmail.com", payeeAddress);

        Address merchantAddress = new Address("25 Somerset Ave", "", "Phoenix", "Arizona", "85006", "USA");
        Location merchantLocation = new Location(33.551248, -112.087979);
        Merchant merchant = new Merchant("Cafe Nero PH12", merchantAddress, merchantLocation, "Commercial Bank Of Arizona", 312312133);

        Location transactionLocation = new Location(33.551248, -112.087979);
        String transactionId = "bc85fd08-4a6a-4e3d-bbbf-dc6599de4c1c";
        data.put(transactionId, new ScenarioData(
            payeeAddress,
            contactDetails,
            new Payee("Loraine", "Summer", payeeAddress, "Female", contactDetails),
            new Card(5404365260583743L, 491, "01/01/2022", 2000, "Bank of America", 382912341, List.of("USA")),
            merchantAddress,
            merchantLocation,
            merchant,
            transactionLocation,
            8.95,
            transactionId
        ));

        payeeAddress = new Address("Unit 5", "125 Brandenburgische Straffe", "Berlin", "Berlin", "10369", "Germany");
        contactDetails = new ContactDetails("543534123", "ShultzG83@gmail.com", payeeAddress);

        merchantAddress = new Address("49 Konstanzer St", "", "Berlin", "Berlin", "10707", "Germany");

        merchantLocation = new Location(52.493721, 13.310281);
        merchant = new Merchant("Pizza takeaway", merchantAddress, merchantLocation, "DMK", 651255734);

        transactionLocation = new Location(52.493721, 13.310281);
        transactionId = "00f7816d-2f7c-464e-a85d-56790d65384b";

        data.put(transactionId, new ScenarioData(
            payeeAddress,
            contactDetails,
            new Payee("Gunter", "Shultz", payeeAddress, "Male", contactDetails),
            new Card(4624365230583743L, 732, "01/05/2023", 20, "Postbank", 382912341, List.of("Germany", "Austria")),
            merchantAddress,
            merchantLocation,
            merchant,
            transactionLocation,
            24.85,
            transactionId
        ));

        payeeAddress = new Address("Unit 5", "125 Brandenburgische Straffe", "Berlin", "Berlin", "10369", "Germany");
        contactDetails = new ContactDetails("543534123", "ShultzG83@gmail.com", payeeAddress);

        merchantAddress = new Address("98 Archiepiskopou Makariou Iii Ave", "7102 Aradippou", "Larnaca", "Larnaca", "7102", "Cyprus");

        merchantLocation = new Location(34.684475, 33.028238);
        merchant = new Merchant("Rentacar Larnaca Central", merchantAddress, merchantLocation, "HSBC", 655255734);

        transactionLocation = new Location(34.684475, 33.028238);
        transactionId = "26bce136-0482-45b5-8a70-f35c31168d2e";

        data.put(transactionId, new ScenarioData(
            payeeAddress,
            contactDetails,
            new Payee("Gunter", "Shultz", payeeAddress, "Male", contactDetails),
            new Card(4624365230583743L, 732, "01/05/2023", 1350, "Postbank", 382912341, List.of("Germany", "Austria")),
            merchantAddress,
            merchantLocation,
            merchant,
            transactionLocation,
            259.00,
            transactionId
        ));

        payeeAddress = new Address("Unit 5", "125 Brandenburgische Straffe", "Berlin", "Berlin", "10369", "Germany");
        contactDetails = new ContactDetails("543534123", "ShultzG83@gmail.com", payeeAddress);

        merchantAddress = new Address("49 Konstanzer St", "", "Berlin", "Berlin", "10707", "Germany");

        merchantLocation = new Location(52.493721, 13.310281);
        merchant = new Merchant("Pizza takeaway", merchantAddress, merchantLocation, "DMK", 651255734);

        transactionLocation = new Location(52.493721, 13.310281);
        transactionId = "24f7794a-f26f-45ab-a6bf-4c17e526ad0d";

        data.put(transactionId, new ScenarioData(
            payeeAddress,
            contactDetails,
            new Payee("Gunter", "Shultz", payeeAddress, "Male", contactDetails),
            new Card(4624365230583743L, 732, "01/05/2023", 1350, "Postbank", 382912341, List.of("Germany", "Austria")),
            merchantAddress,
            merchantLocation,
            merchant,
            transactionLocation,
            24.85,
            transactionId
        ));
        return data;
    }


    private static Transaction buildTransaction(String transactionId,
        Long cardNumber,
        Integer cvv,
        String expiry,
        String firstName,
        String lastName,
        String merchantId,
        Double latitude,
        Double longitude,
        Double amount) {
        return Transaction.builder()
            .transactionId(transactionId)
            .card(Card.builder()
                .cardNumber(cardNumber)
                .cvv(cvv)
                .expiry(expiry).build())
            .payee(Payee.builder()
                .firstName(firstName)
                .lastName(lastName).build())
            .merchant(Merchant.builder()
                .id(merchantId)
                .build())
            .location(Location.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build())
            .amount(amount)
            .status(new TransactionStatus(Status.PENDING, null)).build();
    }

}
