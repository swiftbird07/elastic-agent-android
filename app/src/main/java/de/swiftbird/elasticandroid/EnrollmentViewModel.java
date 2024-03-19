package de.swiftbird.elasticandroid;

import android.app.Application;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class EnrollmentViewModel extends AndroidViewModel {
    private final EnrollmentRepository repository;

    public EnrollmentViewModel(Application application, @NonNull EnrollmentRequest request, TextView tError) {
        super(application);
        this.repository = new EnrollmentRepository(application.getApplicationContext(), request.getServerUrl(), request.getToken(), request.getCheckCert(),  tError);
    }

    public LiveData<EnrollmentResponse> enrollAgent(EnrollmentRequest request) {
        return repository.enrollAgent(request);
    }
}