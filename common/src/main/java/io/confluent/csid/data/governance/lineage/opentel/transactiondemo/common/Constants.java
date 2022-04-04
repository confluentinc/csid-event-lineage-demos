package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common;

public class Constants {
    public static class Topics {

        public static final String ACCOUNT_IN = "account-input";
        public static final String ACCOUNT_KTABLE = "account_ktable";
        public static final String ACCOUNT_OUTPUT_TOPIC = "account-output";


        public static final String INPUT_TOPIC = "transaction-request-topic";
        public static final String BALANCE_VERIFICATION_TOPIC = "balance-verification-topic";
        public static final String ENTITY_ENRICHMENTS_TOPIC = "entity-enrichments-topic";
        public static final String FRAUD_DETECTION_TOPIC = "fraud-detection-topic";
        public static final String TRANSACTION_PROCESSING_TOPIC = "transaction-processing-topic";

        public static final String TRANSACTION_OUTPUT_TOPIC = "transaction-output-topic";
        public static final String CARD_DETAILS_TOPIC = "card-details-topic";


        public static final String PAGEVIEWSTATS_OUTPUT_TOPIC = "pageview-stats-output";
        public static final String PLAINSTRING_OUTPUT_TOPIC = "plain-string-output";
        public static final String TRANSACTION_IN = "transaction-input" ;
        public static final String BALANCE_UPDATES_TOPIC = "balance-updates";

        public static final String SPAN_TOPIC = "spans";
        public static final String GROUPED_SPAN_TOPIC = "grouped-spans";
    }
    public static final String USERID_1 = "UserId1";
    public static final String USERID_2 = "UserId2";
    public static final String USERID_3 = "UserId3";


    public static final String PAGEID_1 = "PageId1";
    public static final String PAGEID_2 = "PageId2";
    public static final String PAGEID_3 = "PageId3";

    public static final String REGION_1 = "REGION1";
    public static final String REGION_2 = "REGION2";

    public static final String BOOTSTRAP_KAFKA_SERVER = "kafka:29092";

}
