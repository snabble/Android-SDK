package io.snabble.sdk.payment;

import android.util.Base64;

import androidx.annotation.Nullable;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.Project;
import io.snabble.sdk.R;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.Utils;

public class PaymentCredentials {
    public enum Type {
        SEPA(null, false, Collections.singletonList(PaymentMethod.DE_DIRECT_DEBIT)),
        // legacy credit card type, not used anymore.
        CREDIT_CARD(null, true, Arrays.asList(PaymentMethod.VISA, PaymentMethod.MASTERCARD, PaymentMethod.AMEX)),
        CREDIT_CARD_PSD2(null, true, Arrays.asList(PaymentMethod.VISA, PaymentMethod.MASTERCARD, PaymentMethod.AMEX)),
        PAYDIREKT(null, false, Collections.singletonList(PaymentMethod.PAYDIREKT)),
        TEGUT_EMPLOYEE_CARD("tegutEmployeeID", false, Collections.singletonList(PaymentMethod.TEGUT_EMPLOYEE_CARD)),
        DATATRANS("datatransAlias", true, Arrays.asList(PaymentMethod.TWINT, PaymentMethod.POST_FINANCE_CARD)),
        DATATRANS_CREDITCARD("datatransCreditCardAlias", true, Arrays.asList(PaymentMethod.VISA, PaymentMethod.MASTERCARD, PaymentMethod.AMEX));

        private String originType;
        private boolean requiresProject;
        private List<PaymentMethod> paymentMethods;

        Type(String originType, boolean requiresProject, List<PaymentMethod> paymentMethods) {
            this.originType = originType;
            this.paymentMethods = paymentMethods;
            this.requiresProject = requiresProject;
        }

        public String getOriginType() {
            return originType;
        }

        public List<PaymentMethod> getPaymentMethods() {
            return paymentMethods;
        }

        public boolean isProjectDependantType() {
            return requiresProject;
        }
    }

    public enum Brand {
        UNKNOWN,
        VISA,
        MASTERCARD,
        AMEX,
        TWINT,
        POST_FINANCE_CARD;

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
    }

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

    private String obfuscatedId;
    private boolean isKeyStoreEncrypted;
    private boolean canBypassKeyStore;
    private String encryptedData;
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

    public static PaymentCredentials fromSEPA(String name, String iban) {
        PaymentCredentials pc = new PaymentCredentials();
        pc.generateId();
        pc.type = Type.SEPA;

        List<X509Certificate> certificates = Snabble.getInstance().getPaymentSigningCertificates();
        if (certificates.size() == 0) {
            return null;
        }

        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Invalid Name");
        }

        if (!IBAN.validate(iban)) {
            throw new IllegalArgumentException("Invalid IBAN");
        }

        pc.obfuscatedId = pc.obfuscate(iban);

        SepaData data = new SepaData();
        data.name = name;
        data.iban = iban;
        String json = GsonHolder.get().toJson(data, SepaData.class);

        X509Certificate certificate = certificates.get(0);
        pc.encryptedData = pc.rsaEncrypt(certificate, json.getBytes());
        pc.encrypt();
        pc.signature = pc.sha256Signature(certificate);
        pc.brand = Brand.UNKNOWN;
        pc.appId = Snabble.getInstance().getConfig().appId;

        if (pc.encryptedData == null) {
            return null;
        }

        return pc;
    }

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

        List<X509Certificate> certificates = Snabble.getInstance().getPaymentSigningCertificates();
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
        pc.encryptedData = pc.rsaEncrypt(certificate, json.getBytes());
        pc.encrypt();
        pc.signature = pc.sha256Signature(certificate);
        pc.brand = brand;
        pc.appId = Snabble.getInstance().getConfig().appId;
        pc.projectId = projectId;
        pc.validTo = parseValidTo("MM/yyyy", expirationMonth, expirationYear);

        if (pc.encryptedData == null) {
            return null;
        }

        return pc;
    }

    private static long parseValidTo(String format, String expirationMonth, String expirationYear) {
        if (expirationMonth == null || expirationMonth.equals("")
         || expirationYear == null || expirationYear.equals("")) {
            return 0;
        }

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
            Date date = simpleDateFormat.parse(expirationMonth + "/" + expirationYear);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));

            return calendar.getTimeInMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    public static PaymentCredentials fromPaydirekt(PaydirektAuthorizationData authorizationData, String customerAuthorizationURI) {
        if (customerAuthorizationURI == null) {
            return null;
        }

        PaymentCredentials pc = new PaymentCredentials();
        pc.generateId();
        pc.type = Type.PAYDIREKT;

        List<X509Certificate> certificates = Snabble.getInstance().getPaymentSigningCertificates();
        if (certificates.size() == 0) {
            return null;
        }

        pc.obfuscatedId = "paydirekt";

        PaydirektData paydirektData = new PaydirektData();
        paydirektData.clientID = Snabble.getInstance().getClientId();
        paydirektData.customerAuthorizationURI = customerAuthorizationURI;

        String json = GsonHolder.get().toJson(paydirektData, PaydirektData.class);

        X509Certificate certificate = certificates.get(0);
        pc.encryptedData = pc.rsaEncrypt(certificate, json.getBytes());
        pc.encrypt();
        pc.signature = pc.sha256Signature(certificate);
        pc.appId = Snabble.getInstance().getConfig().appId;

        pc.additionalData = new HashMap<>();
        pc.additionalData.put("deviceID", authorizationData.id);
        pc.additionalData.put("deviceName", authorizationData.name);
        pc.additionalData.put("deviceFingerprint", authorizationData.fingerprint);
        pc.additionalData.put("deviceIPAddress", authorizationData.ipAddress);

        if (pc.encryptedData == null) {
            return null;
        }

        return pc;
    }

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

        List<X509Certificate> certificates = Snabble.getInstance().getPaymentSigningCertificates();
        if (certificates.size() == 0) {
            return null;
        }

        pc.obfuscatedId = obfuscatedId;

        DatatransData datatransData = new DatatransData();
        datatransData.alias = token;

        String json = GsonHolder.get().toJson(datatransData, DatatransData.class);

        X509Certificate certificate = certificates.get(0);
        pc.encryptedData = pc.rsaEncrypt(certificate, json.getBytes());
        pc.encrypt();
        pc.signature = pc.sha256Signature(certificate);
        pc.appId = Snabble.getInstance().getConfig().appId;
        pc.brand = brand;
        pc.obfuscatedId = obfuscatedId;
        pc.validTo = parseValidTo("MM/yy", expirationMonth, expirationYear);

        if (pc.encryptedData == null) {
            return null;
        }

        return pc;
    }

    public static PaymentCredentials fromTegutEmployeeCard(String obfuscatedId, String cardNumber, String projectId) {
        if (cardNumber == null || cardNumber.length() != 19
                || (!cardNumber.startsWith("9280001621")
                && !cardNumber.startsWith("9280001625")
                && !cardNumber.startsWith("9280001620"))) {
            return null;
        }

        PaymentCredentials pc = new PaymentCredentials();
        pc.generateId();
        pc.type = Type.TEGUT_EMPLOYEE_CARD;

        List<X509Certificate> certificates = Snabble.getInstance().getPaymentSigningCertificates();
        if (certificates.size() == 0) {
            return null;
        }

        pc.obfuscatedId = obfuscatedId;

        X509Certificate certificate = certificates.get(0);

        TegutEmployeeCard data = new TegutEmployeeCard();
        data.cardNumber = cardNumber;
        String json = GsonHolder.get().toJson(data, TegutEmployeeCard.class);

        pc.encryptedData = pc.rsaEncrypt(certificate, json.getBytes());
        pc.signature = pc.sha256Signature(certificate);
        pc.brand = Brand.UNKNOWN;
        pc.appId = Snabble.getInstance().getConfig().appId;
        pc.projectId = projectId;
        pc.canBypassKeyStore = true;

        if (pc.encryptedData == null) {
            return null;
        }

        return pc;
    }

    public Type getType() {
        if (type == null) { // backwards compatibility
            return Type.SEPA;
        }

        return type;
    }

    public Brand getBrand() {
        if (brand == null) { // backwards compatibility
            return Brand.UNKNOWN;
        }

        return brand;
    }

    @Nullable
    public String getProjectId() {
        return projectId;
    }

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

        for (int i=4; i<sb.length(); i+=4) {
            sb.insert(i, ' ');
            i++;
        }

        return sb.toString();
    }

    /** Asynchronous encryption using the certificate the backend provided for us **/
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

    /** Synchronous encryption using the user credentials **/
    private void encrypt() {
        PaymentCredentialsStore store = Snabble.getInstance().getPaymentCredentialsStore();

        if (store.keyStoreCipher == null) {
            return;
        }

        byte[] keyStoreEncrypted = store.keyStoreCipher.encrypt(encryptedData.getBytes());

        if (keyStoreEncrypted != null) {
            encryptedData = Base64.encodeToString(keyStoreEncrypted, Base64.NO_WRAP);
            isKeyStoreEncrypted = true;
        } else {
            Logger.e("Could not encrypt payment credentials: KeyStore unavailable");
        }
    }

    /** Synchronous decryption using the user credentials **/
    private String decrypt() {
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

    public boolean canBypassKeyStore() {
        return canBypassKeyStore;
    }

    private boolean validateCertificate(X509Certificate certificate) {
        try {
            Snabble snabble = Snabble.getInstance();
            int caResId;

            switch (snabble.getEnvironment()) {
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

    public boolean validate() {
        if (type == Type.CREDIT_CARD_PSD2) {
            Date date = new Date(validTo);
            if (date.getTime() < System.currentTimeMillis()) {
                return false;
            }
        }

        if (type == Type.CREDIT_CARD) {
            return false;
        }

        List<X509Certificate> certificates = Snabble.getInstance().getPaymentSigningCertificates();
        for (X509Certificate cert : certificates) {
            if (sha256Signature(cert).equals(signature)) {
                return true;
            }
        }

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

    public String getId() {
        return id;
    }

    public long getValidTo() {
        return validTo;
    }

    public String getObfuscatedId() {
        return obfuscatedId;
    }

    public String getEncryptedData() {
        return decrypt();
    }

    public Map<String, String> getAdditionalData() {
        return additionalData;
    }

    public PaymentMethod getPaymentMethod() {
        if (getType() == PaymentCredentials.Type.SEPA) {
            return PaymentMethod.DE_DIRECT_DEBIT;
        } else if (type == Type.TEGUT_EMPLOYEE_CARD) {
            return PaymentMethod.TEGUT_EMPLOYEE_CARD;
        } else if (type == Type.PAYDIREKT) {
            return PaymentMethod.PAYDIREKT;
        } else if (type == Type.CREDIT_CARD_PSD2
                || type == Type.DATATRANS
                || type == Type.DATATRANS_CREDITCARD) {
            switch (getBrand()) {
                case VISA: return PaymentMethod.VISA;
                case AMEX: return PaymentMethod.AMEX;
                case MASTERCARD: return PaymentMethod.MASTERCARD;
                case POST_FINANCE_CARD: return PaymentMethod.POST_FINANCE_CARD;
                case TWINT: return PaymentMethod.TWINT;
            }
        }

        return null;
    }
}
