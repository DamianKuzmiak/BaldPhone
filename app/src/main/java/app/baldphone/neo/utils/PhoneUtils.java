package app.baldphone.neo.utils;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.Locale;
import java.util.Objects;

public class PhoneUtils {
    private static final String TAG = "PhoneUtils";

    // Lazily-initialized instance for efficiency.
    private static final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    /**
     * Attempts to determine the user's current region (country).
     *
     * <p>This method tries to fetch the country code in the following order of priority:
     *
     * <ol>
     *   <li>Network Country ISO from TelephonyManager.
     *   <li>SIM Country ISO from TelephonyManager.
     *   <li>The country from the device's primary locale configuration.
     * </ol>
     *
     * This is useful for correctly formatting phone numbers according to the local conventions.
     *
     * @param context The application context to access system services and resources.
     * @return A two-letter uppercase country code (ISO 3166-1 alpha-2), e.g., "US", "GB".
     */
    @NonNull
    public static String getDeviceRegion(@NonNull Context context) {
        TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        try {
            String networkCountryCode = tm.getNetworkCountryIso();
            if (networkCountryCode != null && !networkCountryCode.isEmpty()) {
                return networkCountryCode.toUpperCase(Locale.US);
            }

            String simCountryIso = tm.getSimCountryIso();
            if (simCountryIso != null && !simCountryIso.isEmpty()) {
                return simCountryIso.toUpperCase(Locale.US);
            }
        } catch (Exception e) {
            Log.e("SignalUtils", "Error getting region from TelephonyManager.", e);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources()
                    .getConfiguration()
                    .getLocales()
                    .get(0)
                    .getCountry()
                    .toUpperCase(Locale.US);
        } else {
            //noinspection deprecation
            return context.getResources()
                    .getConfiguration()
                    .locale
                    .getCountry()
                    .toUpperCase(Locale.US);
        }
    }

    @Nullable
    public static String formatToE164(@NonNull String number, @NonNull String region) {
        try {
            Phonenumber.PhoneNumber phoneProto = phoneNumberUtil.parse(number, region);
            if (phoneNumberUtil.isValidNumber(phoneProto)) {
                return phoneNumberUtil.format(phoneProto, PhoneNumberUtil.PhoneNumberFormat.E164);
            }
        } catch (NumberParseException e) {
            Log.w(TAG, "Could not parse number '" + number + "' for region '" + region + "'", e);
        }
        return null;
    }
}
