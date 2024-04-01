package de.swiftbird.elasticandroid;

import static org.mockito.Mockito.*;

import android.content.Context;
import android.os.Build;
import android.widget.TextView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.lang.reflect.Field;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.LOLLIPOP})
public class EnrollmentRepositoryTest {

    @Mock
    private Context mockContext;

    @Mock
    private TextView mockStatusTextView;

    @Mock
    private TextView mockErrorTextView;

    @Mock
    private FleetApi mockFleetApi;
    @Mock
    private AppEnrollRequest mockRequest;
    @Mock
    private StatusCallback mockCallback;

    @Mock
    private Call<FleetStatusResponse> mockStatusCall;
    @Mock
    private Call<FleetEnrollResponse> mockEnrollCall;

    private FleetEnrollRepository enrollmentRepository;

    // Initialize mocks before each test
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // Given
        String serverUrl = "https://fleet.example.com/";
        String token = "token123";
        boolean checkCert = true;

        this.enrollmentRepository = new FleetEnrollRepository(
                mockContext,
                serverUrl,
                token,
                checkCert,
                mockStatusTextView,
                mockErrorTextView
        );

        // Inject mockFleetApi using reflection or adjust FleetEnrollRepository to allow setting this dependency more directly
        Field fleetApiField = FleetEnrollRepository.class.getDeclaredField("fleetApi");
        fleetApiField.setAccessible(true);
        fleetApiField.set(enrollmentRepository, mockFleetApi);
    }

    // Test constructor behavior with valid inputs
    @Test
    public void constructor_initializesWithValidParameters() throws NoSuchFieldException, IllegalAccessException {
        // Given
        String token = "token123";
        boolean checkCert = true;

        // Then
        // Use reflection to access private fields for verification
        Field fleetApiField = FleetEnrollRepository.class.getDeclaredField("fleetApi");
        fleetApiField.setAccessible(true);
        Object fleetApiValue = fleetApiField.get(enrollmentRepository);

        Field tokenField = FleetEnrollRepository.class.getDeclaredField("token");
        tokenField.setAccessible(true);
        String tokenValue = (String) tokenField.get(enrollmentRepository);

        Field verifyCertField = FleetEnrollRepository.class.getDeclaredField("verifyCert");
        verifyCertField.setAccessible(true);
        boolean verifyCertValue = (boolean) verifyCertField.get(enrollmentRepository);

        // Verify that the FleetApi was initialized (non-null)
        Assert.assertNotNull("FleetApi should be initialized", fleetApiValue);

        // Verify token and verifyCert values
        Assert.assertEquals("Token should match the provided value", token, tokenValue);
        Assert.assertEquals("verifyCert should match the provided value", checkCert, verifyCertValue);

    }

    @Test
    public void enrollAgent_success() {
        when(mockRequest.getServerUrl()).thenReturn("https://fleet.example.com/");
        when(mockFleetApi.getFleetStatus()).thenReturn(mockStatusCall);
        when(mockFleetApi.enrollAgent(anyString(), any(FleetEnrollRequest.class))).thenReturn(mockEnrollCall);

        doAnswer(invocation -> {
            Callback<FleetStatusResponse> callback = invocation.getArgument(0);
            FleetStatusResponse mockResponse = new FleetStatusResponse("HEALTHY");
            callback.onResponse(null, Response.success(mockResponse));
            return null; // Suitable for void methods
        }).when(mockStatusCall).enqueue(any(Callback.class));

        doAnswer(invocation -> {
            Callback<FleetEnrollResponse> callback = invocation.getArgument(0);
            FleetEnrollResponse.Item item = new FleetEnrollResponse.Item("123", "1", true, "2024-03-20T19:52:33Z", "eb0088c0", "eb0088c0-e635-11ee-8207-1b9b3acd8589", "online", null, "PERMANENT");
            callback.onResponse(null, Response.success(new FleetEnrollResponse("created", item)));
            return null;
        }).when(mockEnrollCall).enqueue(any());

        enrollmentRepository.enrollAgent(mockRequest, mockCallback);

        // Verify that no error message was set
        verify(mockErrorTextView, never()).setText(anyString());

        // Create an InOrder verifier for mockStatusTextView
        InOrder inOrder = inOrder(mockStatusTextView);

        // Verify the sequence of setText calls
        inOrder.verify(mockStatusTextView).setText("Starting enrollment process...");
        inOrder.verify(mockStatusTextView).setText("Requesting Fleet Server details...");
        inOrder.verify(mockStatusTextView).setText("Fleet server is healthy. Sending enrollment request to fleet server (this may take a while)...");
        inOrder.verify(mockStatusTextView).setText("Enrollment successful. Agent ID: " + "eb0088c0");
    }

    @Test
    public void enrollAgent_fleetStatusCheckFails() {
        when(mockFleetApi.getFleetStatus()).thenReturn(mockStatusCall);

        doAnswer(invocation -> {
            Callback<FleetStatusResponse> callback = invocation.getArgument(0);
            callback.onFailure(null, new Throwable("Network error"));
            return null;
        }).when(mockStatusCall).enqueue(any(Callback.class));

        enrollmentRepository.enrollAgent(mockRequest, mockCallback);

        verify(mockErrorTextView).setText("Could not communicate with Fleet Server - Error: Network error");
    }

    @Test
    public void enrollAgent_fleetServerReportsUnhealthyStatus() {
        when(mockFleetApi.getFleetStatus()).thenReturn(mockStatusCall);

        doAnswer(invocation -> {
            Callback<FleetStatusResponse> callback = invocation.getArgument(0);
            FleetStatusResponse mockResponse = new FleetStatusResponse("UNHEALTHY");
            callback.onResponse(null, Response.success(mockResponse));
            return null;
        }).when(mockStatusCall).enqueue(any(Callback.class));

        enrollmentRepository.enrollAgent(mockRequest, mockCallback);

        verify(mockErrorTextView).setText("Fleet server is not healthy - Status: UNHEALTHY");
    }

    @Test
    public void enrollAgent_enrollmentRequestFails() {
        // Mock the fleet status check to be successful
        simulateSuccessfulFleetStatusCheck();

        when(mockFleetApi.enrollAgent(anyString(), any(FleetEnrollRequest.class))).thenReturn(mockEnrollCall);

        doAnswer(invocation -> {
            Callback<FleetEnrollResponse> callback = invocation.getArgument(0);
            callback.onFailure(null, new IOException("Failed to connect"));
            return null;
        }).when(mockEnrollCall).enqueue(any(Callback.class));

        enrollmentRepository.enrollAgent(mockRequest, mockCallback);

        verify(mockErrorTextView).setText("Enrollment failed with error: Failed to connect");
    }

    @Test
    public void enrollAgent_errorHandlingInEnrollmentRequest() {
        // Mock the fleet status check to be successful
        simulateSuccessfulFleetStatusCheck();

        when(mockFleetApi.enrollAgent(anyString(), any(FleetEnrollRequest.class))).thenReturn(mockEnrollCall);

        doAnswer(invocation -> {
            Callback<FleetEnrollResponse> callback = invocation.getArgument(0);
            // Simulate an HTTP 400 error
            callback.onResponse(null, Response.error(400, ResponseBody.create(MediaType.parse("application/json"), "Bad Request")));
            return null;
        }).when(mockEnrollCall).enqueue(any(Callback.class));

        enrollmentRepository.enrollAgent(mockRequest, mockCallback);

        verify(mockErrorTextView).setText("Enrollment failed with code: 400 (Error message could not be read)");
    }

    private void simulateSuccessfulFleetStatusCheck() {
        when(mockFleetApi.getFleetStatus()).thenReturn(mockStatusCall);

        doAnswer(invocation -> {
            Callback<FleetStatusResponse> callback = invocation.getArgument(0);
            FleetStatusResponse mockResponse = new FleetStatusResponse("HEALTHY");
            callback.onResponse(null, Response.success(mockResponse));
            return null;
        }).when(mockStatusCall).enqueue(any(Callback.class));
    }



}


