package app.baldphone.neo.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

public final class ContactsUtils {
    private static final String TAG = "ContactsUtils";

    private ContactsUtils() {}

    @Nullable
    public static String resolveLookupKey(
            @NonNull ContentResolver resolver,
            @Nullable String cachedLookupUri,
            @Nullable String number,
            @Nullable String name) {

        // 1. Cached URI
        if (!TextUtils.isEmpty(cachedLookupUri)) {
            try {
                Uri cached = Uri.parse(cachedLookupUri);
                Uri fresh = ContactsContract.Contacts.lookupContact(resolver, cached);
                String lookupKey = getLookupKeyFromUri(resolver, fresh);
                if (lookupKey != null) return lookupKey;
            } catch (Exception e) {
                Log.d(TAG, "Failed to resolve cached URI", e);
            }
        }

        // 2. Phone number
        if (!TextUtils.isEmpty(number)) {
            String normalized = PhoneNumberUtils.normalizeNumber(number);
            if (!TextUtils.isEmpty(normalized)) {
                Uri filterUri =
                        Uri.withAppendedPath(
                                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                                Uri.encode(normalized));
                String lookupKey = getLookupKeyFromUri(resolver, filterUri);
                if (lookupKey != null) return lookupKey;
            }
        }

        // 3. Name
        if (!TextUtils.isEmpty(name)) {
            Uri filterUri =
                    Uri.withAppendedPath(
                            ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(name));
            String lookupKey = getLookupKeyFromUri(resolver, filterUri);
            if (lookupKey != null) return lookupKey;
        }

        Log.w(TAG, "No lookup key found for the given details.");
        return null;
    }

    @Nullable
    private static String getLookupKeyFromUri(
            @NonNull ContentResolver resolver, @Nullable Uri contactUri) {
        if (contactUri == null) {
            return null;
        }
        try (Cursor c =
                resolver.query(
                        contactUri,
                        new String[] {ContactsContract.Contacts.LOOKUP_KEY},
                        null,
                        null,
                        null)) {
            if (c != null && c.moveToNext()) {
                return c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY));
            }
        }
        return null;
    }

    @WorkerThread
    public static int getRawContactId(int contactId, @NonNull ContentResolver resolver) {
        try (final Cursor c =
                resolver.query(
                        ContactsContract.RawContacts.CONTENT_URI,
                        new String[] {ContactsContract.RawContacts._ID},
                        ContactsContract.RawContacts.CONTACT_ID + " = ?",
                        new String[] {String.valueOf(contactId)},
                        null)) {
            if (c != null && c.moveToNext())
                return c.getInt(c.getColumnIndexOrThrow(ContactsContract.RawContacts._ID));
        }
        return -1;
    }
}
