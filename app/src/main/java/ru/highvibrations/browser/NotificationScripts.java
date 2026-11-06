package ru.highvibrations.browser;

/**
 * Подмена window.Notification на странице: уведомление сайта уходит
 * в системные через {@link WebNotificationBridge}.
 * Разрешение сообщаем сайту то же, что дал пользователь в Android.
 */
public final class NotificationScripts {

    private NotificationScripts() {
    }

    public static String polyfill(boolean granted) {
        return "(function(){"
                + "if(window.__hvNotify){return;}window.__hvNotify=true;"
                + "var allowed=" + (granted ? "true" : "false") + ";"
                + "var Original=window.Notification;"
                + "function HvNotification(title,options){"
                + "options=options||{};"
                + "try{HVNotify.show(String(title==null?'':title),"
                + "String(options.body==null?'':options.body));}catch(e){}"
                + "}"
                + "HvNotification.permission=allowed?'granted':'denied';"
                + "HvNotification.requestPermission=function(callback){"
                + "var result=allowed?'granted':'denied';"
                + "if(typeof callback==='function'){setTimeout(function(){callback(result);},0);}"
                + "try{return Promise.resolve(result);}catch(e){return result;}"
                + "};"
                + "if(Original&&Original.prototype){HvNotification.prototype=Original.prototype;}"
                + "try{window.Notification=HvNotification;"
                + "console.log('hv notify ready, granted='+allowed);}catch(e){}"
                + "})();";
    }
}
