package student.inti.goodneighbour.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.TextView;

import student.inti.goodneighbour.R;

public class UIUtils {
    private static Dialog loadingDialog;

    public static void showLoading(Context context, String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            updateLoadingMessage(message);
            return;
        }

        loadingDialog = new Dialog(context);
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        loadingDialog.setCancelable(false);

        TextView loadingText = loadingDialog.findViewById(R.id.loadingText);
        loadingText.setText(message);

        loadingDialog.show();
    }

    public static void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private static void updateLoadingMessage(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            TextView loadingText = loadingDialog.findViewById(R.id.loadingText);
            if (loadingText != null) {
                loadingText.setText(message);
            }
        }
    }
}