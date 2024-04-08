package de.swiftbird.elasticandroid;

import android.annotation.SuppressLint;

import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Provides utility methods for creating instances of {@link OkHttpClient} with specific
 * configurations, including optional SSL certificate pinning and custom headers.
 */
public  class NetworkBuilder {

    /**
     * Creates and configures an {@link OkHttpClient} instance with optional SSL certificate pinning and
     * a custom User-Agent header. This method allows for configuring the client to either trust all certificates,
     * trust certificates specified via a full certificate string or use the default system trust manager.
     *
     * @param checkCA        Indicates whether the CA (Certificate Authority) should be checked. If {@code true},
     *                       the method uses the provided {@code sslCertFull} string to pin the certificate
     *                       or the system trust manager if the string is null or empty. If {@code false},
     *                       the client trusts all certificates.
     * @param sslCertFull    The full SSL certificate string for pinning. This is used only if {@code checkCA} is true
     *                       and the string is not null or empty.
     * @param timeoutSeconds The timeout in seconds for both read and connect operations.
     * @return An {@link OkHttpClient} instance configured according to the specified parameters.
     * @throws RuntimeException If SSL certificate pinning is requested but fails due to any reason,
     *                          this exception is thrown, encapsulating the original exception.
     */
    public static OkHttpClient getOkHttpClient(boolean checkCA, @Nullable String sslCertFull, @Nullable int timeoutSeconds) {
        //AppLog.d("NetworkBuilder", "Creating OkHttpClient with checkCA: " + checkCA + " and sslCertFull: " + sslCertFull);

        Interceptor authInterceptor = chain -> {
            Request originalRequest = chain.request();
            Request newRequest = originalRequest.newBuilder()
                    .header("User-Agent", "elastic agent " + BuildConfig.AGENT_VERSION)
                    .build();
            return chain.proceed(newRequest);
        };

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(authInterceptor);

        if (checkCA && sslCertFull != null && !sslCertFull.isEmpty()) {
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");

                // Create a KeyStore containing our trusted certificate
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null); // Initialize the keyStore

                try (InputStream caInput = new ByteArrayInputStream(sslCertFull.getBytes())) {
                    Certificate ca = cf.generateCertificate(caInput);
                    keyStore.setCertificateEntry("ca", ca);
                }

                // Create a TrustManager that trusts the certificate in our KeyStore
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);

                // Create an SSLContext that uses our TrustManager
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);

                X509TrustManager trustManager = (X509TrustManager) tmf.getTrustManagers()[0];
                builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);

            } catch (Exception e) {
                AppLog.e("NetworkBuilder", "Failed to set SSL certificate", e);
                throw new RuntimeException("Failed to set SSL certificate", e);
            }

        } else {
            // Fallback to trusting all certificates if sslCertFull is null or checkCA is false
            try {
                @SuppressLint("CustomX509TrustManager")
                final TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                            }

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[]{};
                            }
                        }
                };

                    final SSLContext sslContext = SSLContext.getInstance("SSL");
                    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                    builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
                    builder.hostnameVerifier((hostname, session) -> true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        return builder.readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS).build();
    }
}
