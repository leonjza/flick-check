package com.flick.flickcheck;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import javax.net.ssl.X509TrustManager;

// Many thanks to Nikolay Elenkov for feedback.
// Shamelessly based upon Moxie's example code (AOSP/Google did not offer code)
// http://www.thoughtcrime.org/blog/authenticity-is-broken-in-ssl-but-your-app-ha/
public final class PubKeyManager implements X509TrustManager {

    // DER encoded public key
    // [root@fII ssl]# openssl rsa -in nginx.key -pubout -outform DER | xxd -p
    private static String PUB_KEY =
            "30820122300d06092a864886f70d01010105000382010f003082010a0282" +
            "010100b7051e2040155a8e78903e325a8680bd680f0c9cbd164225422a6f" +
            "ace762db4da9c7fa11687cc10fc1a20ea1e31260525145d5b18e2692e6e6" +
            "1e0b00d14e78fc62d031cafef90d9dc9599527beae644d1ce0af5b4ec21d" +
            "405544a1c4a69fc39704e5897791c407f5e77c8bc195be7bcdb6fb30da1f" +
            "2485d8853c9ce40ebc834e5d7c5c81f052ad03a57921aa940d6b928a0cee" +
            "39979398e84d9cbf57565109f42f9634db46211f65b89fb9c7375e5a9810" +
            "c0a89d10b7b6d9301eab716102e35ffe09ae29f764bc2527534e68381306" +
            "fb7a984c208baa00090b65f4c44d0ace781cd9779130b9e4ea1a54c8bc3c" +
            "1e9fa31855ebf57f72815775bba604ed6d41290203010001";

    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {

        assert (chain != null);
        if (chain == null) {
            throw new IllegalArgumentException(
                    "checkServerTrusted: X509Certificate array is null");
        }

        assert (chain.length > 0);
        if (!(chain.length > 0)) {
            throw new IllegalArgumentException(
                    "checkServerTrusted: X509Certificate is empty");
        }

        // Ripped from OWASP Poc:
        //  https://www.owasp.org/index.php/Certificate_and_Public_Key_Pinning#Android
        //
        // Trashed a few lines here as the cert is self-signed too. We only
        // really care about the PIN.


        // Hack ahead: BigInteger and toString(). We know a DER encoded Public
        // Key starts with 0x30 (ASN.1 SEQUENCE and CONSTRUCTED), so there is
        // no leading 0x00 to drop.
        RSAPublicKey pubkey = (RSAPublicKey) chain[0].getPublicKey();
        String encoded = new BigInteger(1 /* positive */, pubkey.getEncoded())
                .toString(16);

        // Pin it!
        final boolean expected = PUB_KEY.equalsIgnoreCase(encoded);
        assert(expected);
        if (!expected) {
            throw new CertificateException(
                    "checkServerTrusted: Expected public key: " + PUB_KEY
                            + ", got public key:" + encoded);
        }
    }

    public void checkClientTrusted(X509Certificate[] xcs, String string) {
        // throw new
        // UnsupportedOperationException("checkClientTrusted: Not supported yet.");
    }

    public X509Certificate[] getAcceptedIssuers() {
        // throw new
        // UnsupportedOperationException("getAcceptedIssuers: Not supported yet.");
        return null;
    }
}
