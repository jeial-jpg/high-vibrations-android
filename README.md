# На высоких вибрациях

Официальное Android-приложение сайта **[high-vibrations.ru](https://high-vibrations.ru)**.
Открывается сразу сайтом — во весь экран, без адресной строки и вкладок.

[**⬇ Скачать APK (v1.0, 712 КБ)**](../../releases/latest) · Android 7.0+

## Возможности

- **Сайт как приложение** — запуск сразу на `high-vibrations.ru`, без браузерных панелей.
- **Только сайт** — работают все поддомены и пути `high-vibrations.ru`, переходы на чужие домены блокируются.
- **Пароли** — «Сохранить пароль?», обновление при смене, автозаполнение форм.
  Хранение только на устройстве: AES-256-GCM + Android Keystore.
- **Сессия** — свернул и вернулся: страница там же, без перезагрузки. Выгрузило из кеша — открывается главная.
- **Уведомления** — системное разрешение (Android 13+), свой канал, уведомления сайта показываются как системные.
- **Камера и микрофон** — доступ только для сайта и только после согласия; кнопки переключения камеры и микрофона поверх страницы, пока идёт съёмка.
- **Медиа** — видео и аудио на весь экран, загрузка файлов в формы.
- **Без рекламы, аналитики и трекеров.** Всё работает локально.

## Установка (APK)

1. Скачайте `high-vibrations-1.0.apk` со страницы [Releases](../../releases).
2. Разрешите установку из этого источника (Настройки → Безопасность → Установка неизвестных приложений).
3. Откройте файл и нажмите «Установить».

Требования: **Android 7.0+** (minSdk 24). Размер ≈ 712 КБ. Один APK на все устройства.

## Настройка сборки

Единственный файл настроек — `app/src/main/res/values/config.xml`:

| Параметр | Смысл |
|---|---|
| `start_url` | адрес, который открывается при запуске |
| `allow_subdomains` | разрешить поддомены |
| `lock_to_site` | `true` — уйти на другой сайт нельзя |
| `extra_allowed_hosts` | дополнительные домены (оплата, OAuth) |

Сборка (Windows, BAT):

```
BUILD-HIGH-VIBRATIONS.bat            :: debug APK
BUILD-HIGH-VIBRATIONS.bat release    :: release APK (подпись из keystore/)
BUILD-HIGH-VIBRATIONS.bat install    :: собрать и установить
```

Требуется только JDK 17 + Android SDK (без Android Studio).

## Структура проекта

```
app/src/main/java/ru/highvibrations/browser/
  MainActivity.java               точка входа, UI, жизненный цикл
  KioskConfig.java                настройки из config.xml
  SitePolicy.java                 правила доступа к доменам
  KioskWebViewClient.java         сторож навигации
  KioskChromeClient.java          видео, файлы, камера/микрофон
  MediaPermissionController.java  разрешения камеры и микрофона
  MediaOverlay.java               кнопки переключения камеры и микрофона
  MediaScripts.java / MediaBridge.java   JS-хук состояния камеры
  PasswordVault.java              шифрованное хранилище паролей
  PasswordDialog.java / CredentialPrompter.java   окна сохранения паролей
  LoginScripts.java / LoginBridge.java   перехват и автозаполнение форм
  NotificationPermission.java / WebNotifications.java / NotificationScripts.java
  PermissionGate.java             правило повторного запроса (24 ч → не более 2 раз)
  FullscreenController.java       полноэкранный режим
  WebViewSettings.java            настройки WebView
```

## Лицензия

Код приложения — см. [LICENSE](LICENSE). Бренд и контент сайта принадлежат владельцу `high-vibrations.ru`.
