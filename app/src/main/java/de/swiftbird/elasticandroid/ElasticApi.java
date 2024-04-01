package de.swiftbird.elasticandroid;

import java.util.ArrayList;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ElasticApi {
        @PUT("/{index}/_doc/{id}")
        Call<ElasticResponse> put(
                @Header("Authorization") String accessApiKeyHeader,
                @Path("index") String index,
                @Body ElasticDocument elasticDocument);

        // Batch
        @PUT("/{index}/_doc/_bulk")
        Call<ElasticResponse> putBulk(
                @Header("Authorization") String accessApiKeyHeader,
                @Path("index") String index,
                @Body ArrayList<ElasticDocument> elasticDocument);



}
