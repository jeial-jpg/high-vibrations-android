package ru.highvibrations.browser;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.security.KeyStore;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Локальное хранилище логинов и паролей.
 *
 * Значения шифруются ключом Android Keystore (AES/GCM) и лежат в приватных
 * SharedPreferences приложения. Наружу ничего не передаётся, бэкап в облако
 * не требуется - всё остаётся на устройстве.
 */
public final class PasswordVault {

    /** Пара логин/пароль для одного хоста. */
    public static final class Credentials {
        public final String username;
        public final String password;

        Credentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    private static final String TAG = "PasswordVault";
    private static final String PREFS = "saved_logins";
    private static final String KEY_ALIAS = "hv_login_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SharedPreferences prefs;

    public PasswordVault(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Сохранённая пара для хоста или null. */
    public Credentials get(String host) {
        if (host == null || host.isEmpty()) {
            return null;
        }
        String stored = prefs.getString(prefKey(host), null);
        if (stored == null) {
            return null;
        }
        try {
            String plain = decrypt(stored);
            int split = plain.indexOf('\n');
            if (split < 0) {
                return new Credentials(plain, "");
            }
            return new Credentials(plain.substring(0, split), plain.substring(split + 1));
        } catch (Exception exception) {
            Log.w(TAG, "cannot decrypt credentials for " + host);
            return null;
        }
    }

    /** Сохранить или перезаписать пару для хоста. */
    public boolean save(String host, String username, String password) {
        if (host == null || host.isEmpty() || password == null || password.isEmpty()) {
            return false;
        }
        try {
            String safeUser = username == null ? "" : username.replace("\n", " ");
            String blob = encrypt(safeUser + "\n" + password);
            return prefs.edit().putString(prefKey(host), blob).commit();
        } catch (Exception exception) {
            Log.e(TAG, "cannot save credentials for " + host, exception);
            return false;
        }
    }

    public void clear(String host) {
        if (host != null && !host.isEmpty()) {
            prefs.edit().remove(prefKey(host)).apply();
        }
    }

    private String prefKey(String host) {
        return "cred_" + host.toLowerCase(Locale.ROOT);
    }

    private String encrypt(String plain) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey());
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(plain.getBytes("UTF-8"));
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    private String decrypt(String stored) throws Exception {
        byte[] combined = Base64.decode(stored, Base64.NO_WRAP);
        if (combined.length <= IV_BYTES) {
            throw new IllegalArgumentException("bad blob");
        }
        byte[] iv = new byte[IV_BYTES];
        System.arraycopy(combined, 0, iv, 0, IV_BYTES);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_BITS, iv));
        return new String(cipher.doFinal(combined, IV_BYTES, combined.length - IV_BYTES), "UTF-8");
    }

    private SecretKey secretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());
            generator.generateKey();
        }
        return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
    }
}
