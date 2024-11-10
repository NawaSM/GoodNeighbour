package student.inti.goodneighbour;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

public class GoodNeighbourApplication extends Application {
    private static final String DB_URL = "https://goodneighbour-b8fad-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        // Initialize Firebase Realtime Database
        FirebaseDatabase.getInstance(FirebaseApp.getInstance());
        // Enable offline persistence (optional)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}