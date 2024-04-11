package de.swiftbird.elasticandroid;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Defines HTTP operations for interacting with an Elasticsearch server using Retrofit.
 * Supports operations such as inserting a single document and performing bulk insertions.
 */
public interface ElasticApi {

        /**
         * Inserts or updates a document in the specified Elasticsearch index.
         *
         * @param accessApiKeyHeader Authorization header containing the API key for Elasticsearch access.
         * @param index The Elasticsearch index where the document will be inserted or updated.
         * @param elasticDocument The document to insert or update, wrapped in an {@link ElasticDocument} object.
         * @return A {@link Call} object for the network request, with {@link ElasticResponse} as the expected response type.
         */
        @PUT("/{index}/_doc")
        Call<ElasticResponse> put(
                @Header("Authorization") String accessApiKeyHeader,
                @Path("index") String index,
                @Body ElasticDocument elasticDocument);


        /**
         * Performs a bulk operation in the specified Elasticsearch index, allowing for multiple documents
         * to be inserted or updated in a single request.
         *
         * @param accessApiKeyHeader Authorization header containing the API key for Elasticsearch access.
         * @param index The Elasticsearch index where the bulk operation will be performed.
         * @param elasticDocument A {@link RequestBody} object containing the bulk operation data.
         * @return A {@link Call} object for the network request, with {@link ElasticResponse} as the expected response type.
         */
        @PUT("/{index}/_bulk")
        Call<ElasticResponse> putBulk(
                @Header("Authorization") String accessApiKeyHeader,
                @Path("index") String index,
                @Body RequestBody elasticDocument);
}
