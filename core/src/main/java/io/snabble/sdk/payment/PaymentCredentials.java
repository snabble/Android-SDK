package io.snabble.sdk.payment;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.io.InputStream;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import io.snabble.sdk.Environment;
import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.R;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.payment.externalbilling.EncryptedBillingCredentials;
import io.snabble.sdk.payment.payone.sepa.PayoneSepaData;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.Utils;

/**
 * Class for storing encrypting payment credentials
 */
public class PaymentCredentials {
    /**
     * Enum describing the type of the payment credentials
     */
    public enum Type {
        SEPA(null, false, Collections.singletonList(PaymentMethod.DE_DIRECT_DEBIT)),
        // legacy credit card type, not used anymore.
        @Deprecated
        CREDIT_CARD(null, true, Arrays.asList(PaymentMethod.VISA, PaymentMethod.MASTERCARD, PaymentMethod.AMEX)),
        CREDIT_CARD_PSD2(null, true, Arrays.asList(PaymentMethod.VISA, PaymentMethod.MASTERCARD, PaymentMethod.AMEX)),
        PAYDIREKT(null, false, Collections.singletonList(PaymentMethod.PAYDIREKT)),
        TEGUT_EMPLOYEE_CARD("tegutEmployeeID", false, Collections.singletonList(PaymentMethod.TEGUT_EMPLOYEE_CARD)),
        LEINWEBER_CUSTOMER_ID("leinweberCustomerID", false, Collections.singletonList(PaymentMethod.LEINWEBER_CUSTOMER_ID)),
        DATATRANS("datatransAlias", true, Arrays.asList(PaymentMethod.TWINT, PaymentMethod.POST_FINANCE_CARD)),
        DATATRANS_CREDITCARD("datatransCreditCardAlias", true, Arrays.asList(PaymentMethod.VISA, PaymentMethod.MASTERCARD, PaymentMethod.AMEX)),
        PAYONE_CREDITCARD(null, true, Arrays.asList(PaymentMethod.VISA, PaymentMethod.MASTERCARD, PaymentMethod.AMEX)),
        PAYONE_SEPA("payoneSepaData", true, Collections.singletonList(PaymentMethod.PAYONE_SEPA)),
        EXTERNAL_BILLING("contactPersonCredentials", true, Collections.singletonList(PaymentMethod.EXTERNAL_BILLING)),
        ;

        private final String originType;
        private final boolean requiresProject;
        private final List<PaymentMethod> paymentMethods;

        Type(String originType, boolean requiresProject, List<PaymentMethod> paymentMethods) {
            this.originType = originType;
            this.paymentMethods = paymentMethods;
            this.requiresProject = requiresProject;
        }

        /**
         * Get the backend descriptor of the payment credential,if it needs
         * to be transferred on checkout.
         */
        public String getOriginType() {
            return originType;
        }

        /**
         * Get a list of all payment methods that are supported by this type
         */
        public List<PaymentMethod> getPaymentMethods() {
            return paymentMethods;
        }

        /**
         * Returns true if this those payment credentials are specific
         * to a project and cant be shared between multiple projects
         */
        public boolean isProjectDependantType() {
            return requiresProject;
        }
    }

    /**
     * Enum describing the brand of the payment credentials
     */
    public enum Brand {
        UNKNOWN,
        VISA,
        MASTERCARD,
        AMEX,
        TWINT,
        POST_FINANCE_CARD;

        /**
         * Create a Brand from a given {@link PaymentMethod}
         */
        public static Brand fromPaymentMethod(PaymentMethod paymentMethod) {
            switch (paymentMethod) {
                case AMEX:
                    return AMEX;
                case VISA:
                    return VISA;
                case MASTERCARD:
                    return MASTERCARD;
                case TWINT:
                    return TWINT;
                case POST_FINANCE_CARD:
                    return POST_FINANCE_CARD;
            }

            return null;
        }
    }

    private static class SepaData {
        private String name;
        private String iban;
    }

    private static class CreditCardData {
        private String hostedDataID;
        private String schemeTransactionID;
        private String projectID;
        private String hostedDataStoreID;
        private String cardType;
    }

    private static class PaydirektData {
        private String clientID;
        private String customerAuthorizationURI;
        private PaydirektAuthorizationData authorizationData;
    }

    private static class DatatransData {
        private String alias;
        private String expiryMonth;
        private String expiryYear;
    }

    private static class PayoneData {
        PayoneData(String pseudoCardPAN, String name, String userID) {
            this.pseudoCardPAN = pseudoCardPAN;
            this.name = name;
            this.userID = userID;
        }

        private final String pseudoCardPAN;
        private final String name;
        private final String userID;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static class PaydirektAuthorizationData {
        public String id;
        public String name;
        public String ipAddress;
        public String fingerprint;
        public String redirectUrlAfterSuccess;
        public String redirectUrlAfterCancellation;
        public String redirectUrlAfterFailure;
    }

    private static class TegutEmployeeCard {
        private String cardNumber;
    }

    private static class LeinweberCustomerId {
        private String cardNumber;
    }

    private String obfuscatedId;
    // comes from previously saved data on deserialization - was used in encrypt() from old code
    private boolean isKeyStoreEncrypted;
    @Deprecated
    private String encryptedData; // old key-store based encrypted data
    private String rsaEncryptedData; // rsa encrypted data
    private String signature;
    private long validTo;
    private Type type;
    private Brand brand;
    private String appId;
    private String id;
    private String projectId;
    private Map<String, String> additionalData;

    private PaymentCredentials() {

    }

    /**
     * Encrypts and stores SEPA payment credentials.
     */
    public static PaymentCredentials fromSEPA(String name, String iban) {
        if (name == null || name.length() == 0) throw new IllegalArgumentException("Invalid Name");
        if (!IBAN.validate(iban)) throw new IllegalArgumentException("Invalid IBAN");

        final SepaData data = new SepaData();
        data.name = name;
        data.iban = iban;
        final String json = GsonHolder.get().toJson(data, SepaData.class);
        return createForSepa(Type.SEPA, iban, json);
    }

    /**
     * Encrypts and stores ExternalBilling payment credentials.
     */
    public static PaymentCredentials fromExternalBilling(@NonNull final EncryptedBillingCredentials externalBilling, @NonNull final String projectId){
        final String json = GsonHolder.get().toJson(externalBilling, EncryptedBillingCredentials.class);
        final PaymentCredentials pc = new PaymentCredentials();
        pc.generateId();
        pc.type = Type.EXTERNAL_BILLING;
        pc.obfuscatedId = pc.obfuscate(externalBilling.getUsername());

        final List<X509Certificate> certificates = Snabble.getInstance().getPaymentCertificates();
        if (certificates == null || certificates.size() == 0) {
            return null;
        }

        final X509Certificate certificate = certificates.get(0);
        pc.rsaEncryptedData = pc.rsaEncrypt(certificate, json.getBytes());
        pc.signature = pc.sha256Signature(certificate);
        pc.brand = Brand.UNKNOWN;
        pc.appId = Snabble.getInstance().getConfig().appId;
        pc.projectId = projectId;
        pc.additionalData = new HashMap<>();

        if (pc.rsaEncryptedData == null) {
            return null;
        }

        return pc;
    }

    /**
     * Encrypts and stores SEPA payment credentials.
     */
    public static PaymentCredentials fromPayoneSepa(@NonNull final PayoneSepaData sepaData) {
        final String json = GsonHolder.get().toJson(sepaData, PayoneSepaData.class);
        return createForSepa(Type.PAYONE_SEPA, sepaData.getIban(), json);
    }

    private static PaymentCredentials createForSepa(
            @NonNull final Type type,
            @NonNull final String iban,
            @NonNull final String jsonData
    ) {
        final PaymentCredentials pc = new PaymentCredentials();
        pc.generateId();
        pc.type = type;

        final List<X509Certificate> certificates = Snabble.getInstance().getPaymentCertificates();
        if (certificates == null || certificates.size() == 0) {
            return null;
        }

        pc.obfuscatedId = pc.obfuscate(iban);

        final X509Certificate certificate = certificates.get(0);
        pc.rsaEncryptedData = pc.rsaEncrypt(certificate, jsonData.getBytes());
        pc.signature = pc.sha256Signature(certificate);
        pc.brand = Brand.UNKNOWN;
        pc.appId = Snabble.getInstance().getConfig().appId;

        if (pc.rsaEncryptedData == null) {
            return null;
        }

        return pc;
    }

    private static void throwIfEmpty(@NonNull String value, @Nullable String name) {
        if (value.length() == 0) {
            throw new IllegalArgumentException(name != null ? name : "Parameter" + " must not be empty");
        }
    }


    /**
     * Encrypts and stores a Telecash / First Data credit card.
     */
    public static PaymentCredentials fromCreditCardData(String name,
                                                        Brand brand,
                                                        String projectId,
                                                        String obfuscatedId,
                                                        String expirationMonth,
                                                        String expirationYear,
                                                        String hostedDataId,
                                                        String schemeTransactionId,
                                                        String storeId) {
        PaymentCredentials pc = new PaymentCredentials();
        pc.generateId();
        pc.type = Type.CREDIT_CARD_PSD2;

        List<X509Certificate> certificates = Snabble.getInstance().getPaymentCertificates();
        if (certificates.size() == 0) {
            return null;
        }

        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Invalid Name");
        }

        pc.obfuscatedId = obfuscatedId;

        CreditCardData creditCardData = new CreditCardData();
        creditCardData.hostedDataID = hostedDataId;
        creditCardData.projectID = projectId;
        creditCardData.schemeTransactionID = schemeTransactionId;
        creditCardData.hostedDataStoreID = storeId;

        switch (brand) {
            case VISA:
                creditCardData.cardType = "creditCardVisa";
                break;
            case MASTERCARD:
                creditCardData.cardType = "creditCardMastercard";
                break;
            case AMEX:
                creditCardData.cardType = "creditCardAmericanExpress";
                break;
            default:
                return null;
        }

        String json = GsonHolder.get().toJson(creditCardData, CreditCardData.class);

        X509Certificate certificate = certificates.get(0);
        pc.rsaEncryptedData = pc.rsaEncrypt(certificate, json.getBytes());
        pc.signature = pc.sha256Signature(certificate);
        pc.brand = brand;
        pc.appId = Snabble.getInstance().getConfig().appId;
        pc.projectId = projectId;
        pc.validTo = parseValidTo("MM/yyyy", expirationMonth, expirationYear);

        if (pc.rsaEncryptedData == null) {
            return null;
        }

        return pc;
    }

    @Deprecated
    private static long parseValidTo(String format, String expirationMonth, String expirationYear) {
        if (expirationMonth == null || expirationMonth.equals("")
                || expirationYear == null || expirationYear.equals("")) {
            return 0;
        }

        return parseValidTo(format, expirationMonth + "/" + expirationYear);
    }

    private static long parseValidTo(String format, String expirationDate) {
        if (expirationDate == null || expirationDate.trim().equals("")) {
            return 0;
        }

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format, Locale.getDefault());

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(simpleDateFormat.parse(expirationDate));
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));

            return calendar.getTimeInMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Encrypts and stores a paydirekt authorization token.
     */
    public static PaymentCredentials fromPaydirekt(PaydirektAuthorizationData authorizationData, String customerAuthorizationURI) {
        if (customerAuthorizationURI == null) {
            return null;
        }

        PaymentCredentials pc = new PaymentCredentials();
        pc.generateId();
        pc.type = Type.PAYDIREKT;

        List<X509Certificate> certificates = Snabble.getInstance().getPaymentCertificates();
        if (certificates.size() == 0) {
            return null;
        }

        pc.obfuscatedId = "paydirekt";

        PaydirektData paydirektData = new PaydirektData();
        paydirektData.clientID = Snabble.getInstance().getClientId();
        paydirektData.customerAuthorizationURI = customerAuthorizationURI;

        String json = GsonHolder.get().toJson(paydirektData, PaydirektData.class);

        X509Certificate certificate = certificates.get(0);
        pc.rsaEncryptedData = pc.rsaEncrypt(certificate, json.getBytes());
        pc.signature = pc.sha256Signature(certificate);
        pc.appId = Snabble.getInstance().getConfig().appId;

        pc.additionalData = new HashMap<>();
        pc.additionalData.put("deviceID", authorizationData.id);
        pc.additionalData.put("deviceName", authorizationData.name);
        pc.additionalData.put("deviceFingerprint", authorizationData.fingerprint);
        pc.additionalData.put("deviceIPAddress", authorizationData.ipAddress);

        if (pc.rsaEncryptedData == null) {
            return null;
        }

        return pc;
    }

    /**
     * Encrypts and stores a datatrans authorization token.
     */
    public static PaymentCredentials fromDatatrans(String token, Brand brand, String obfuscatedId,
                                                   String expirationMonth, String expirationYear,
                                                   String projectId) {
        if (token == null) {
            return null;
        }

        PaymentCredentials pc = new PaymentCredentials();
        pc.generateId();
        if (brand == Brand.TWINT || brand == Brand.POST_FINANCE_CARD) {
            pc.type = Type.DATATRANS;
        } else {
            pc.type = Type.DATATRANS_CREDITCARD;
        }
        pc.projectId = projectId;

        List<X509Certificate> certificates = Snabble.getInstance().getPaymentCertificates();
        if (certificates.size() == 0) {
            return null;
        }

        pc.obfuscatedId = obfuscatedId;

        DatatransData datatransData = new DatatransData();
        datatransData.alias = token;

        if (pc.type == Type.DATATRANS_CREDITCARD) {
            datatransData.expiryMonth = expirationMonth;
            datatransData.expiryYear = expirationYear;
        }

        String json = GsonHolder.get().toJson(datatransData, DatatransData.class);

        X509Certificate certificate = certificates.get(0);
        pc.rsaEncryptedData = pc.rsaEncrypt(certificate, json.getBytes());
        pc.signature = pc.sha256Signature(certificate);
        pc.appId = Snabble.getInstance().getConfig().appId;
        pc.brand = brand;
        pc.obfuscatedId = obfuscatedId;
        pc.validTo = parseValidTo("MM/yy", expirationMonth, expirationYear);

        if (pc.rsaEncryptedData == null) {
            return null;
        }

        return pc;
    }

    /**
     * Encrypts and stores a payone pseudo card pan.
     */
    public static PaymentCredentials fromPayone(String pseudocardpan,
                                                String truncatedcardpan,
                                                PaymentCredentials.Brand brand,
                                                String cardexpiredate,
                                                String lastname,
                                                String userId,
                                                String projectId) {
        if (pseudocardpan == null) {
            return null;
        }

        PaymentCredentials pc = new PaymentCredentials();
        pc.generateId();
        if (brand == Brand.MASTERCARD || brand == Brand.AMEX || brand == Brand.VISA) {
            pc.type = Type.PAYONE_CREDITCARD;
        } else {
            return null;
        }
        pc.projectId = projectId;

        List<X509Certificate> certificates = Snabble.getInstance().getPaymentCertificates();
        if (certificates.size() == 0) {
            return null;
        }

        PayoneData payoneData = new PayoneData(pseudocardpan, lastname, userId);

        String json = GsonHolder.get().toJson(payoneData, PayoneData.class);

        X509Certificate certificate = certificates.get(0);
        pc.rsaEncryptedData = pc.rsaEncrypt(certificate, json.getBytes());
        pc.signature = pc.sha256Signature(certificate);
        pc.appId = Snabble.getInstance().getConfig().appId;
        pc.brand = brand;
        pc.obfuscatedId = truncatedcardpan;
        pc.validTo = parseValidTo("yyMM", cardexpiredate);

        if (pc.rsaEncryptedData == null) {
            return null;
        }

        return pc;
    }

    /**
     * Encrypts and stores a tegut employee card.
     */
    public static PaymentCredentials fromTegutEmployeeCard(String obfuscatedId, String cardNumber, String projectId) {
        if (cardNumber == null || cardNumber.length() != 19
                || (!cardNumber.startsWith("9280001621")
                && !cardNumber.startsWith("9280001620"))) {
            return null;
        }

        PaymentCredentials pc = new PaymentCredentials();
        pc.generateId();
        pc.type = Type.TEGUT_EMPLOYEE_CARD;

        List<X509Certificate> certificates = Snabble.getInstance().getPaymentCertificates();
        if (certificates.size() == 0) {
            return null;
        }

        pc.obfuscatedId = obfuscatedId;

        X509Certificate certificate = certificates.get(0);

        TegutEmployeeCard data = new TegutEmployeeCard();
        data.cardNumber = cardNumber;
        String json = GsonHolder.get().toJson(data, TegutEmployeeCard.class);

        pc.rsaEncryptedData = pc.rsaEncrypt(certificate, json.getBytes());
        pc.signature = pc.sha256Signature(certificate);
        pc.brand = Brand.UNKNOWN;
        pc.appId = Snabble.getInstance().getConfig().appId;
        pc.projectId = projectId;

        if (pc.rsaEncryptedData == null) {
            return null;
        }

        return pc;
    }

    /**
     * Encrypts and stores a leinweber customer id.
     */
    public static PaymentCredentials fromLeinweberCustomerId(String obfuscatedId, String cardNumber, String projectId) {
        if (cardNumber == null || cardNumber.length() != 6) {
            return null;
        }

        PaymentCredentials pc = new PaymentCredentials();
        pc.generateId();
        pc.type = Type.LEINWEBER_CUSTOMER_ID;

        List<X509Certificate> certificates = Snabble.getInstance().getPaymentCertificates();
        if (certificates.size() == 0) {
            return null;
        }

        pc.obfuscatedId = obfuscatedId;

        X509Certificate certificate = certificates.get(0);

        LeinweberCustomerId data = new LeinweberCustomerId();
        data.cardNumber = cardNumber;
        String json = GsonHolder.get().toJson(data, LeinweberCustomerId.class);

        pc.rsaEncryptedData = pc.rsaEncrypt(certificate, json.getBytes());
        pc.signature = pc.sha256Signature(certificate);
        pc.brand = Brand.UNKNOWN;
        pc.appId = Snabble.getInstance().getConfig().appId;
        pc.projectId = projectId;

        if (pc.rsaEncryptedData == null) {
            return null;
        }

        return pc;
    }

    /**
     * Returns the type of the payment credentials
     */
    public Type getType() {
        if (type == null) { // backwards compatibility
            return Type.SEPA;
        }

        return type;
    }

    /**
     * Returns the brand of the payment credentials
     */
    public Brand getBrand() {
        if (brand == null) { // backwards compatibility
            return Brand.UNKNOWN;
        }

        return brand;
    }

    /**
     * Returns the associated project or null if no project is needed
     */
    @Nullable
    public String getProjectId() {
        return projectId;
    }

    /**
     * Returns the associated app id
     */
    public String getAppId() {
        return appId;
    }

    private String obfuscate(String s) {
        int numCharsStart = 4;
        int numCharsEnd = 2;

        StringBuilder sb = new StringBuilder(s.length());
        sb.append(s.substring(0, numCharsStart));
        for (int i = numCharsStart; i < s.length() - numCharsEnd; i++) {
            sb.append('\u2022'); // Bullet
        }
        sb.append(s.substring(s.length() - numCharsEnd));

        for (int i = 4; i < sb.length(); i += 4) {
            sb.insert(i, ' ');
            i++;
        }

        return sb.toString();
    }

    /**
     * Asynchronous encryption using the certificate the backend provided for us
     **/
    private String rsaEncrypt(X509Certificate certificate, byte[] data) {
        try {
            if (validateCertificate(certificate)) {
                Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
                AlgorithmParameterSpec params = new OAEPParameterSpec("SHA-256", "MGF1",
                        new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT);
                cipher.init(Cipher.ENCRYPT_MODE, certificate.getPublicKey(), params);
                byte[] encrypted = cipher.doFinal(data);
                return Base64.encodeToString(encrypted, Base64.NO_WRAP);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new AssertionError(e.getMessage());
        }
    }

    /**
     * Synchronous decryption using the user credentials
     **/
    private String decryptUsingKeyStore() {
        PaymentCredentialsStore store = Snabble.getInstance().getPaymentCredentialsStore();

        if (store.keyStoreCipher == null) {
            if (isKeyStoreEncrypted) {
                Logger.e("Accessing encrypted data without providing KeyStoreCipher");
            }

            return encryptedData;
        }

        if (isKeyStoreEncrypted) {
            byte[] decrypted = store.keyStoreCipher.decrypt(Base64.decode(encryptedData, Base64.NO_WRAP));
            if (decrypted != null) {
                return new String(decrypted);
            } else {
                Logger.e("Could not decrypt using KeyStoreCipher");
                return null;
            }
        }

        return encryptedData;
    }

    /**
     * @return Returns true if the current app id matches the app id when this payment
     * method was created.
     */
    public boolean isAvailableInCurrentApp() {
        return Snabble.getInstance().getConfig().appId.equals(appId);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean canBypassKeyStore() {
        return rsaEncryptedData != null;
    }

    private boolean validateCertificate(X509Certificate certificate) {
        try {
            Snabble snabble = Snabble.getInstance();
            int caResId;

            Environment environment = snabble.getEnvironment();
            if (environment != null) {
                switch (environment) {
                    case PRODUCTION:
                        caResId = R.raw.ca_prod;
                        break;
                    case STAGING:
                        caResId = R.raw.ca_staging;
                        break;
                    case TESTING:
                        caResId = R.raw.ca_testing;
                        break;
                    default:
                        return false;
                }
            } else {
                return false;
            }

            InputStream is = Snabble.getInstance().getApplication().getResources().openRawResource(caResId);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate trustedCA = (X509Certificate) certificateFactory.generateCertificate(is);

            List<Certificate> certificates = new ArrayList<>();
            certificates.add(certificate);

            CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX");
            CertPath cp = certificateFactory.generateCertPath(certificates);
            TrustAnchor trustAnchor = new TrustAnchor(trustedCA, null);
            HashSet<TrustAnchor> set = new HashSet<>();
            set.add(trustAnchor);
            PKIXParameters pkixParameters = new PKIXParameters(set);
            pkixParameters.setRevocationEnabled(false);

            try {
                certPathValidator.validate(cp, pkixParameters);
                return true;
            } catch (CertPathValidatorException e) {
                Logger.d("Could not verify certificate: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private String sha256Signature(X509Certificate certificate) {
        return Utils.sha256Hex(Utils.hexString(certificate.getSignature()));
    }

    /**
     * Validates the current payment credentials.
     * If this method returns false the payment credentials are not usable anymore.
     * <p>
     * E.g. on timed out credit cards or expired certificates.
     */
    public boolean validate() {
        if (type == Type.CREDIT_CARD_PSD2) {
            Date date = new Date(validTo);
            if (date.getTime() < System.currentTimeMillis()) {
                Logger.errorEvent("removing payment credentials: expired");
                return false;
            }
        }

        if (type == Type.CREDIT_CARD) {
            Logger.errorEvent("removing payment credentials: old credit card type");
            return false;
        }

        List<X509Certificate> certificates = Snabble.getInstance().getPaymentCertificates();
        for (X509Certificate cert : certificates) {
            if (sha256Signature(cert).equals(signature)) {
                return true;
            }
        }

        Logger.errorEvent("removing payment credentials: different gateway certificate signature");
        return false;
    }

    boolean checkAppId() {
        if (appId == null) {
            appId = Snabble.getInstance().getConfig().appId;
            return true;
        }

        return false;
    }

    void generateId() {
        id = UUID.randomUUID().toString();
    }

    /**
     * Returns the id of the payment credentials
     */
    public String getId() {
        return id;
    }

    /**
     * Returns how long the credentials are valid as a UNIX timestamp.
     */
    public long getValidTo() {
        return validTo;
    }

    /**
     * Returns the obfuscated and user displayable credentials string
     */
    public String getObfuscatedId() {
        return obfuscatedId;
    }

    /**
     * Returns the encrypted payment data that can only be decrypted by the snabble payment gateway.
     */
    public String getEncryptedData() {
        if (rsaEncryptedData != null) {
            return rsaEncryptedData;
        }

        Logger.logEvent("Accessing not migrated keystore credentials");
        return decryptUsingKeyStore();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean migrateFromKeyStore() {
        // if data is available and we have not migrated before, start migration
        if (rsaEncryptedData == null && encryptedData != null) {
            rsaEncryptedData = decryptUsingKeyStore();

            if (rsaEncryptedData != null) {
                Logger.logEvent("Successfully migrated payment credentials");
                return true;
            } else {
                Logger.logEvent("Payment credential migration was unsuccessful");
                return false;
            }
        }

        if (rsaEncryptedData == null) {
            Logger.errorEvent("Payment credentials are contain no data - removing");
            return false;
        }

        // if already migrated do nothing
        return true;
    }

    /**
     * Additional payment credentials specific key value pairs
     */
    public Map<String, String> getAdditionalData() {
        return additionalData;
    }

    /**
     * Get the associated payment method for the payment credentials
     */
    public PaymentMethod getPaymentMethod() {
        if (getType() == PaymentCredentials.Type.SEPA) {
            return PaymentMethod.DE_DIRECT_DEBIT;
        } else if (getType() == Type.PAYONE_SEPA) {
            return PaymentMethod.PAYONE_SEPA;
        } else if (type == Type.TEGUT_EMPLOYEE_CARD) {
            return PaymentMethod.TEGUT_EMPLOYEE_CARD;
        } else if (type == Type.LEINWEBER_CUSTOMER_ID) {
            return PaymentMethod.LEINWEBER_CUSTOMER_ID;
        } else if (type == Type.PAYDIREKT) {
            return PaymentMethod.PAYDIREKT;
        } else if (type == Type.EXTERNAL_BILLING) {
            return PaymentMethod.EXTERNAL_BILLING;
        } else if (type == Type.CREDIT_CARD_PSD2
                || type == Type.DATATRANS
                || type == Type.DATATRANS_CREDITCARD) {
            switch (getBrand()) {
                case VISA:
                    return PaymentMethod.VISA;
                case AMEX:
                    return PaymentMethod.AMEX;
                case MASTERCARD:
                    return PaymentMethod.MASTERCARD;
                case POST_FINANCE_CARD:
                    return PaymentMethod.POST_FINANCE_CARD;
                case TWINT:
                    return PaymentMethod.TWINT;
            }
        } else if (type == Type.PAYONE_CREDITCARD) {
            switch (getBrand()) {
                case VISA:
                    return PaymentMethod.VISA;
                case AMEX:
                    return PaymentMethod.AMEX;
                case MASTERCARD:
                    return PaymentMethod.MASTERCARD;
            }
        }

        return null;
    }
}
