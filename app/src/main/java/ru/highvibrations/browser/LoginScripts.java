package ru.highvibrations.browser;

/**
 * JavaScript-скрипты: наблюдатель за формой входа и автозаполнение.
 * Хранятся отдельно, чтобы не засорять логику Activity.
 */
public final class LoginScripts {

    private LoginScripts() {
    }

    /**
     * Ставит на страницу наблюдатель: когда пользователь отправляет форму
     * с паролем, логин и пароль уходят в {@link LoginBridge}.
     * Читать сохранённые пароли из JavaScript нельзя - только запись.
     */
    public static String captureWatcher() {
        return "(function(){"
                + "if(window.__hvWatch){return;}window.__hvWatch=true;"
                + "console.log('hv watch ready');"
                + "var last='';"
                + "function userField(pw){"
                + "var scope=pw.form||document;"
                + "var nodes=scope.querySelectorAll('input[type=text],input[type=email],"
                + "input[type=tel],input:not([type])');"
                + "var fallback=null;"
                + "for(var i=0;i<nodes.length;i++){var el=nodes[i];"
                + "if(el.type==='password'){continue;}"
                + "if(fallback===null){fallback=el;}"
                + "if(el.value){return el;}}"
                + "return fallback;}"
                + "function report(){"
                + "var pws=document.querySelectorAll('input[type=password]');"
                + "if(!pws.length){return;}"
                + "var pw=pws[0];"
                + "if(!pw.value){return;}"
                + "var uf=userField(pw);"
                + "var user=uf?uf.value:'';"
                + "var sig=location.hostname+'|'+user+'|'+pw.value;"
                + "if(sig===last){return;}"
                + "last=sig;"
                + "try{HVLogin.onCredentials(location.hostname,user,pw.value);}catch(e){}"
                + "}"
                + "document.addEventListener('submit',function(){setTimeout(report,0);},true);"
                + "document.addEventListener('click',function(e){"
                + "var t=e.target;if(!t){return;}"
                + "var tag=(t.tagName||'').toLowerCase();"
                + "if(t.type==='submit'||tag==='button'){setTimeout(report,400);}},true);"
                + "document.addEventListener('keydown',function(e){"
                + "if(e.key==='Enter'){setTimeout(report,400);}},true);"
                + "})();";
    }

    /**
     * Заполняет форму входа сохранёнными данными (значения экранируются).
     * Повторяет попытки, пока форма не появится (SPA рисуют её позже).
     */
    public static String autofill(String username, String password) {
        return "(function(){"
                + "var U=" + quote(username) + ",P=" + quote(password) + ";"
                + "var tries=0;"
                + "function put(el,v){"
                + "if(!el||el.value){return;}"
                + "el.value=v;"
                + "el.dispatchEvent(new Event('input',{bubbles:true}));"
                + "el.dispatchEvent(new Event('change',{bubbles:true}));}"
                + "function fill(){"
                + "tries++;"
                + "var pw=document.querySelector('input[type=password]');"
                + "if(!pw){if(tries<40){setTimeout(fill,500);}return;}"
                + "var scope=pw.form||document;"
                + "var nodes=scope.querySelectorAll('input[type=text],input[type=email],"
                + "input[type=tel],input:not([type])');"
                + "for(var i=0;i<nodes.length;i++){"
                + "if(nodes[i].type!=='password'){put(nodes[i],U);break;}}"
                + "put(pw,P);}"
                + "fill();"
                + "})();";
    }

    /** Экранирование значения для безопасной вставки в JS-строку. */
    private static String quote(String value) {
        if (value == null) {
            return "''";
        }
        StringBuilder builder = new StringBuilder("'");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\'':
                    builder.append("\\'");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '<':
                    builder.append("\\x3c");
                    break;
                case '>':
                    builder.append("\\x3e");
                    break;
                case '&':
                    builder.append("\\x26");
                    break;
                default:
                    builder.append(c);
            }
        }
        return builder.append("'").toString();
    }
}
