package com.example.rtc.http;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class HttpsTrustManager {
    private final static Logger LOGGER = Logger.getLogger("HttpsTrustManager");
    private static X509TrustManager trustManager;
    private static SSLSocketFactory sslSocketFactory;

    public synchronized static X509TrustManager getTrustManager() {
        if (trustManager != null) {
            return trustManager;
        }
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            KeyStore ks = KeyStore.getInstance("AndroidCAStore");
            ks.load(null, null);
            tmf.init(ks);
            trustManager = (X509TrustManager) tmf.getTrustManagers()[0];
        } catch (KeyStoreException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (CertificateException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return trustManager;
    }

    public synchronized static SSLSocketFactory createSSLSocketFactory() {
        if (sslSocketFactory != null) {
            return sslSocketFactory;
        }
        try {
            SSLContext sslc;
            try {
                sslc = SSLContext.getInstance("TLSv1.2");
            } catch (NoSuchAlgorithmException var43) {
                sslc = SSLContext.getInstance("TLS");
            }
            sslc.init(null, new TrustManager[]{getTrustManager()}, new SecureRandom());
            sslSocketFactory = sslc.getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (KeyManagementException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return sslSocketFactory;
    }
}