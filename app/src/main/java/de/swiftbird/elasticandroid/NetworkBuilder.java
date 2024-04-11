package de.swiftbird.elasticandroid;

import android.annotation.SuppressLint;
import androidx.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Provides utility methods for creating instances of {@link OkHttpClient} with specific
 * configurations, including optional SSL certificate pinning and custom headers.
 */
public class NetworkBuilder {
    private static Retrofit retrofit_es = null;
    private static Retrofit retrofit_fleet = null;

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
    public static OkHttpClient getOkHttpClient(boolean checkCA, @Nullable String sslCertFull, int timeoutSeconds) {
        AppLog.d("NetworkBuilder", "Creating OkHttpClient with checkCA: " + checkCA + " and sslCertFull: " + sslCertFull);

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
            // Trust the provided certificate
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

        } else if (!checkCA) {
            // Trust all certificates
            try {
                @SuppressLint("CustomX509TrustManager")
                final TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @SuppressLint("TrustAllX509TrustManager") // Intended behavior if "checkCA" is false
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                            }

                            @SuppressLint("TrustAllX509TrustManager") // Intended behavior if "checkCA" is false
                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
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
            } else {
                // Use the default system trust manager if checkCA is true and sslCertFull is null or empty
                builder.hostnameVerifier((hostname, session) -> true);
            }

        return builder.readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS).build();
    }

    /**
     * Creates a {@link Retrofit} instance to be used to connect to Elasticsearch with the specified base
     * URL and optional SSL certificate pinning.
     * This method uses the {@link NetworkBuilder#getOkHttpClient(boolean, String, int)} method to create
     * an {@link OkHttpClient} instance with the specified parameters.
     *
     * @param baseUrl     The base URL for the Retrofit instance.
     * @param checkCA     Indicates whether the CA (Certificate Authority) should be checked. If {@code true},
     *                    the method uses the provided {@code sslCertFull} string to pin the certificate
     *                    or the system trust manager if the string is null or empty. If {@code false},
     *                    the client trusts all certificates.
     * @param sslCertFull The full SSL certificate string for pinning. This is used only if {@code checkCA} is true
     *                    and the string is not null or empty.
     * @return A {@link Retrofit} instance configured with the specified base URL and SSL certificate pinning.
     */
    public static Retrofit getClientElasticsearch(String baseUrl, boolean checkCA, @Nullable String sslCertFull, int timeoutSeconds) {
        if (retrofit_es == null) {
            if(baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalArgumentException("Base URL cannot be null or empty");
            }

            retrofit_es = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(NetworkBuilder.getOkHttpClient(checkCA, sslCertFull, timeoutSeconds))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit_es;
    }

    /**
     * Creates a {@link Retrofit} instance to be used to connect to the Fleet Server with the specified base
     * URL and optional SSL certificate pinning.
     * This method uses the {@link NetworkBuilder#getOkHttpClient(boolean, String, int)} method to create
     * an {@link OkHttpClient} instance with the specified parameters.
     *
     * @param baseUrl     The base URL for the Retrofit instance.
     * @param checkCA     Indicates whether the CA (Certificate Authority) should be checked. If {@code true},
     *                    the method uses the provided {@code sslCertFull} string to pin the certificate
     *                    or the system trust manager if the string is null or empty. If {@code false},
     *                    the client trusts all certificates.
     * @param sslCertFull The full SSL certificate string for pinning. This is used only if {@code checkCA} is true
     *                    and the string is not null or empty.
     * @return A {@link Retrofit} instance configured with the specified base URL and SSL certificate pinning.
     */
    public static Retrofit getClientFleet(String baseUrl, boolean checkCA, @Nullable String sslCertFull, int timeoutSeconds) {
        if (retrofit_fleet == null) {
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalArgumentException("Base URL cannot be null or empty");
            }

            retrofit_fleet = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(NetworkBuilder.getOkHttpClient(checkCA, sslCertFull, timeoutSeconds))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit_fleet;
    }

    /**
     * Resets the Retrofit clients to null. This method is used to clear the Retrofit clients
     * when a new configuration is needed, such as when the SSL certificate changes.
     */
    public static void resetClients() {
        retrofit_es = null;
        retrofit_fleet = null;
    }
}
