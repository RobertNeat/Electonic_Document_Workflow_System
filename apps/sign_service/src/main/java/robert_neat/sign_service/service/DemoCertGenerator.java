package robert_neat.sign_service.service;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Generowanie materialu kryptograficznego demo-PKI (PAdES BASELINE-T, mock TSA RFC 3161, bez
 * sieciowego OCSP/CRL). Bouncy Castle (bcpkix-jdk18on) jako prowider kryptograficzny - zob.
 * main_assumptions.md sekcja "sign_service". Dwa poziomy CA (RSA 2048): root (self-signed,
 * generateCaCertificate) i issuing (podpisany przez root, generateIssuingCaCertificate) - TSA i
 * certyfikaty leaf sa podpisywane przez issuing, nie przez root (root pozostaje dedykowany
 * wylacznie do wydania issuing-CA, zgodnie z praktyka realnych PKI).
 */
@Component
public class DemoCertGenerator {

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final int RSA_KEY_SIZE = 2048;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public record KeyCertPair(KeyPair keyPair, X509Certificate certificate) {
    }

    public KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(RSA_KEY_SIZE);
        return generator.generateKeyPair();
    }

    /** Self-signed certyfikat root-CA, dlugi okres waznosci - trust anchor calego lancucha (root -> issuing -> leaf/TSA). */
    public X509Certificate generateCaCertificate(KeyPair caKeyPair, String subjectDn) throws GeneralSecurityException, OperatorCreationException, CertIOException {
        X500Name name = new X500Name(subjectDn);
        Date notBefore = toDate(LocalDate.of(2020, 1, 1));
        Date notAfter = toDate(LocalDate.of(2045, 1, 1));
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name, BigInteger.ONE, notBefore, notAfter, name, caKeyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(caKeyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    /**
     * Certyfikat issuing-CA podpisany przez root-CA (nie self-signed) - subordinate CA, ktory w
     * praktyce wydaje certyfikaty TSA/leaf, root zostaje offline/dedykowany wylacznie do wydania
     * issuing-CA. BasicConstraints(0) (pathLenConstraint=0) - issuing-CA moze wydawac wylacznie
     * certyfikaty koncowe (non-CA), nie dalsze sub-CA. Round-trip subject-u roota przez
     * JcaX509CertificateHolder jak w generateLeafCertificate (patrz tamtejszy Javadoc dot.
     * byte-identycznosci issuer/subject wymaganej przez PKCS12KeyStore.setKeyEntry).
     */
    public X509Certificate generateIssuingCaCertificate(
            KeyPair rootKeyPair, X509Certificate rootCertificate, KeyPair issuingKeyPair,
            String serialNumberHex, String subjectDn, LocalDate notBefore, LocalDate notAfter)
            throws GeneralSecurityException, OperatorCreationException, CertIOException {
        X500Name issuerName;
        try {
            issuerName = new org.bouncycastle.cert.jcajce.JcaX509CertificateHolder(rootCertificate).getSubject();
        } catch (java.security.cert.CertificateEncodingException e) {
            throw new CertificateException("Nie udalo sie odczytac subject-u certyfikatu root-CA", e);
        }
        X500Name subjectName = new X500Name(subjectDn);
        BigInteger serialNumber = new BigInteger(serialNumberHex, 16);
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuerName, serialNumber, toDate(notBefore), toDate(notAfter), subjectName, issuingKeyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(rootKeyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    /**
     * Certyfikat leaf podpisany przez issuing-CA, z danymi (numer seryjny/subject/waznosc) wziętymi
     * z istniejącego wiersza `certyfikat` w bazie - notBefore/notAfter odwzorowują realnie
     * data_wydania/data_wygasniecia, nawet gdy to oznacza certyfikat kryptograficznie wygasły
     * (cert-2) - to zamierzone (zob. main_assumptions zadania).
     */
    public X509Certificate generateLeafCertificate(
            KeyPair caKeyPair, X509Certificate caCertificate, KeyPair leafKeyPair,
            String serialNumberHex, String subjectDn, LocalDate notBefore, LocalDate notAfter)
            throws GeneralSecurityException, OperatorCreationException, CertIOException {
        // Issuer musi byc BYTE-IDENTYCZNY z subject-em zakodowanym w certyfikacie CA, inaczej
        // JDK PKCS12KeyStore.setKeyEntry odrzuca lancuch certyfikatow jako niewalidny mimo ze
        // X500Name.equals() (porownanie semantyczne RDN) zwraca true - round-trip przez
        // X509Certificate.getSubjectX500Principal().getName() zmienia kolejnosc RDN w DER.
        // JcaX509CertificateHolder czyta oryginalne TBSCertificate.subject bez przekodowania.
        X500Name issuerName;
        try {
            issuerName = new org.bouncycastle.cert.jcajce.JcaX509CertificateHolder(caCertificate).getSubject();
        } catch (java.security.cert.CertificateEncodingException e) {
            throw new CertificateException("Nie udalo sie odczytac subject-u certyfikatu CA", e);
        }
        X500Name subjectName = new X500Name(subjectDn);
        BigInteger serialNumber = new BigInteger(serialNumberHex, 16);
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuerName, serialNumber, toDate(notBefore), toDate(notAfter), subjectName, leafKeyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation));
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(caKeyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    /**
     * Certyfikat mock-TSA podpisany przez issuing-CA, z rozszerzeniem ExtendedKeyUsage ograniczonym
     * WYLACZNIE do id-kp-timeStamping (OID 1.3.6.1.5.5.7.3.8, krytyczne) - RFC 3161 §2.3 wymaga
     * tego od certyfikatu podmiotu znakujacego czas; BouncyCastle TimeStampTokenGenerator odrzuca
     * certyfikat bez tego ograniczenia.
     */
    public X509Certificate generateTsaCertificate(
            KeyPair caKeyPair, X509Certificate caCertificate, KeyPair tsaKeyPair,
            String serialNumberHex, String subjectDn, LocalDate notBefore, LocalDate notAfter)
            throws GeneralSecurityException, OperatorCreationException, CertIOException {
        X500Name issuerName;
        try {
            issuerName = new org.bouncycastle.cert.jcajce.JcaX509CertificateHolder(caCertificate).getSubject();
        } catch (java.security.cert.CertificateEncodingException e) {
            throw new CertificateException("Nie udalo sie odczytac subject-u certyfikatu CA", e);
        }
        X500Name subjectName = new X500Name(subjectDn);
        BigInteger serialNumber = new BigInteger(serialNumberHex, 16);
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuerName, serialNumber, toDate(notBefore), toDate(notAfter), subjectName, tsaKeyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        builder.addExtension(Extension.extendedKeyUsage, true,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(caKeyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    /** PKCS12 keystore z pojedynczym wpisem klucz+certyfikat (+ cert CA w lancuchu, jesli podany). */
    public byte[] buildPkcs12(String alias, PrivateKey privateKey, char[] password,
                               X509Certificate certificate, X509Certificate... chainRest)
            throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            X509Certificate[] chain = new X509Certificate[1 + chainRest.length];
            chain[0] = certificate;
            System.arraycopy(chainRest, 0, chain, 1, chainRest.length);
            keyStore.setKeyEntry(alias, privateKey, password, chain);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            keyStore.store(out, password);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new CertificateException("Nie udalo sie zbudowac keystore PKCS12", e);
        }
    }

    private static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}
