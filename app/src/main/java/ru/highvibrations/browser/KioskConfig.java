package ru.highvibrations.browser;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Настройки из res/values/config.xml в готовом к употреблению виде.
 * Ничего не решает - только читает и нормализует.
 */
public final class KioskConfig {

    private final String startUrl;
    private final String startHost;
    private final boolean allowSubdomains;
    private final boolean lockToSite;
    private final List<String> extraHosts;

    private KioskConfig(String startUrl, String startHost, boolean allowSubdomains,
                        boolean lockToSite, List<String> extraHosts) {
        this.startUrl = startUrl;
        this.startHost = startHost;
        this.allowSubdomains = allowSubdomains;
        this.lockToSite = lockToSite;
        this.extraHosts = extraHosts;
    }

    public static KioskConfig from(Context context) {
        Resources res = context.getResources();
        String url = res.getString(R.string.start_url).trim();
        String host = hostOf(url);
        List<String> extras = new ArrayList<>();
        String[] configured = res.getStringArray(R.array.extra_allowed_hosts);
        if (configured != null) {
            for (String entry : configured) {
                String normalized = normalizeHost(entry);
                if (!normalized.isEmpty()) {
                    extras.add(normalized);
                }
            }
        }
        // без валидного адреса режим киоска не включаем, иначе заблокируем всё
        boolean lock = res.getBoolean(R.bool.lock_to_site) && !host.isEmpty();
        return new KioskConfig(url, host, res.getBoolean(R.bool.allow_subdomains),
                lock, Collections.unmodifiableList(extras));
    }

    public String getStartUrl() {
        return startUrl;
    }

    public String getStartHost() {
        return startHost;
    }

    public boolean isAllowSubdomains() {
        return allowSubdomains;
    }

    public boolean isLockToSite() {
        return lockToSite;
    }

    public List<String> getExtraHosts() {
        return extraHosts;
    }

    private static String hostOf(String url) {
        try {
            return normalizeHost(Uri.parse(url).getHost());
        } catch (Exception exception) {
            return "";
        }
    }

    private static String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        return host.trim().toLowerCase(Locale.ROOT);
    }
}
