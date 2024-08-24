package com.rtm516.mcxboxbroadcast.core.webrtc;

import com.rtm516.mcxboxbroadcast.core.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.tls.AlertDescription;
import org.bouncycastle.tls.Certificate;
import org.bouncycastle.tls.CertificateRequest;
import org.bouncycastle.tls.DefaultTlsClient;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.SignatureAndHashAlgorithm;
import org.bouncycastle.tls.TlsAuthentication;
import org.bouncycastle.tls.TlsCredentials;
import org.bouncycastle.tls.TlsFatalAlert;
import org.bouncycastle.tls.TlsServerCertificate;
import org.bouncycastle.tls.crypto.TlsCertificate;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.jcajce.JcaDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCertificate;
import org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCrypto;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class DtlsClient extends DefaultTlsClient {
    private final JcaTlsCrypto crypto;
    private final String finalFingerprint;
    private final Logger logger;

    private final KeyPair keyPair;
    private final X509Certificate cert;
    private final Certificate bcCert;

    public DtlsClient(JcaTlsCrypto crypto, String serverFingerprint, Logger logger) throws NoSuchAlgorithmException, CertificateException, OperatorCreationException {
        super(crypto);

        this.crypto = crypto;
        this.finalFingerprint = serverFingerprint;
        this.logger = logger.prefixed("DtlsClient");

        // Generate the RSA key pair
        var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        this.keyPair = keyPairGenerator.generateKeyPair();

        // Create the certificate builder
        X500Name subject = new X500Name("CN=Test Certificate");
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(System.currentTimeMillis()),
            new Date(System.currentTimeMillis() - 10000L),
            new Date(System.currentTimeMillis() + 31536000000L),
            subject,
            keyPair.getPublic()
        );

        // Create the ContentSigner for signing the certificate
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA")
            .setProvider("BC")
            .build(keyPair.getPrivate());

        // Generate the certificate
        this.cert = new JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(certBuilder.build(contentSigner));

        // Create the BC Certificate object
        this.bcCert = new Certificate(new TlsCertificate[]{new JcaTlsCertificate(crypto, cert)});
    }

    @Override
    public TlsAuthentication getAuthentication() throws IOException {
        return new TlsAuthentication() {
            @Override
            public void notifyServerCertificate(TlsServerCertificate serverCertificate) throws IOException {
                if (serverCertificate == null || serverCertificate.getCertificate() == null || serverCertificate.getCertificate().isEmpty()) {
                    logger.error("Invalid certificate: " + serverCertificate);
                    throw new TlsFatalAlert(AlertDescription.bad_certificate);
                }
                var cert = serverCertificate.getCertificate().getCertificateAt(0).getEncoded();
                var fp = fingerprintFor(cert);

                if (!fp.equals(finalFingerprint)) {
                    logger.error("Fingerprint does not match! Expected " + finalFingerprint + " got " + fp);
                    throw new TlsFatalAlert(AlertDescription.bad_certificate);
                }
            }

            @Override
            public TlsCredentials getClientCredentials(CertificateRequest certificateRequest) {
                return new JcaDefaultTlsCredentialedSigner(new TlsCryptoParameters(context), crypto, keyPair.getPrivate(), bcCert, SignatureAndHashAlgorithm.rsa_pss_rsae_sha256);
            }
        };
    }

    @Override
    protected ProtocolVersion[] getSupportedVersions() {
        return new ProtocolVersion[]{ProtocolVersion.DTLSv12};
    }

    private String fingerprintFor(byte[] input) {
        var digest = new SHA256Digest();
        digest.update(input, 0, input.length);
        var result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);

        var hexBytes = Hex.encode(result);
        String hex = new String(hexBytes, StandardCharsets.US_ASCII).toUpperCase();

        var fp = new StringBuilder();
        int i = 0;
        fp.append(hex, i, i + 2);
        while ((i += 2) < hex.length())
        {
            fp.append(':');
            fp.append(hex, i, i + 2);
        }
        return fp.toString();
    }

    public String getClientFingerprint() throws CertificateEncodingException {
        return fingerprintFor(cert.getEncoded());
    }
};
