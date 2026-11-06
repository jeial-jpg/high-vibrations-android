package ru.highvibrations.browser;

import android.net.Uri;

import java.util.List;
import java.util.Locale;

/**
 * Единственное место, где решается "можно ли загрузить этот URL".
 * Используется WebViewClient-ом для навигации и Activity для проверок.
 */
public final class SitePolicy {

    private final String allowedHost;
    private final boolean allowSubdomains;
    private final boolean lockToSite;
    private final List<String> extraHosts;

    public SitePolicy(KioskConfig config) {
        this.allowedHost = config.getStartHost();
        this.allowSubdomains = config.isAllowSubdomains();
        this.lockToSite = config.isLockToSite();
        this.extraHosts = config.getExtraHosts();
    }

    /**
     * Разрешена ли загрузка URL.
     *
     * Пропускаем: свой хост и его пути, поддомены (если включено),
     * дополнительные хосты из конфига, внутренние схемы страницы
     * и локальные файлы приложения. Остальное (чужой домен, mailto:, tel:,
     * intent:, market:) - запрещено, чтобы нельзя было уйти из приложения.
     */
    public boolean isAllowed(String url) {
        if (!lockToSite) {
            return true;
        }
        if (url == null || url.isEmpty()) {
            return true;
        }
        Uri uri;
        try {
            uri = Uri.parse(url);
        } catch (Exception exception) {
            return false;
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }
        switch (scheme.toLowerCase(Locale.ROOT)) {
            case "http":
            case "https":
                return isAllowedHost(uri.getHost());
            case "about":
            case "data":
            case "blob":
            case "javascript":
                return true;
            case "file":
                return url.contains("android_asset") || url.contains("/data/user/")
                        || url.contains("/data/data/");
            default:
                return false;
        }
    }

    /** Свой хост, его поддомены и хосты из extra_allowed_hosts. */
    public boolean isAllowedHost(String host) {
        if (!lockToSite) {
            return true;
        }
        if (host == null || host.isEmpty()) {
            return false;
        }
        String candidate = host.toLowerCase(Locale.ROOT);
        if (matches(candidate, allowedHost)) {
            return true;
        }
        for (String extra : extraHosts) {
            if (matches(candidate, extra)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String candidate, String allowed) {
        if (allowed.isEmpty()) {
            return false;
        }
        if (candidate.equals(allowed)) {
            return true;
        }
        return allowSubdomains && candidate.endsWith("." + allowed);
    }
}
