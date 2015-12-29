package com.card_database.mtg.carddatabasemtg;

import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Методы для скачивания файлов.
 */
final class DownloadUtils {

    /**
     * Выполняет сетевой запрос для скачивания файла, и сохраняет ответ в указанный файл.
     *
     * @param downloadUrl   URL - откуда скачивать (http:// или https://)
     * @param destFile      файл, в который сохранять.
     * @param progressCallback  опциональный callback для уведомления о прогрессе скачивания
     *                          файлы. Его метод onProgressChanged вызывается синхронно
     *                          в текущем потоке.
     *
     * @throws IOException  В случае ошибки выполнения сетевого запроса или записи файла.
     */
    static void downloadFile(String downloadUrl,
                             File destFile,
                             @Nullable ProgressCallback progressCallback) throws IOException {
        Log.d(TAG, "Start downloading url: " + downloadUrl);
        Log.d(TAG, "Saving to file: " + destFile);

        // Выполняем запрос по указанному урлу. Поскольку мы используем только http:// или https://
        // урлы для скачивания, мы привести результат к HttpURLConnection. В случае урла с другой
        // схемой, будет ошибка.
        HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
        InputStream in = null;
        OutputStream out = null;
        int responseCode = 0;

        try {

            // Проверяем HTTP код ответа. Ожидаем только ответ 200 (ОК).
            // Остальные коды считаем ошибкой.
            responseCode = conn.getResponseCode();
            Log.d(TAG, "Received HTTP response code: " + responseCode);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new FileNotFoundException("Unexpected HTTP response: " + responseCode
                        + ", " + conn.getResponseMessage());
            }

            // Узнаем размер файла, который мы собираемся скачать
            // (приходит в ответе в HTTP заголовке Content-Length)
            int contentLength = conn.getContentLength();
            Log.d(TAG, "Content Length: " + contentLength);

            // Создаем временный буффер для I/O операций размером 128кб
            byte [] buffer = new byte[1024 * 128];

            // Размер полученной порции в байтах
            int receivedBytes;
            // Сколько байт всего получили (и записали).
            int receivedLength = 0;
            // прогресс скачивания от 0 до 100
            int progress = 0;

            // Начинаем читать ответ
            in = conn.getInputStream();
            // И открываем файл для записи
            out = new FileOutputStream(destFile);

            // В цикле читаем данные порциями в буффер, и из буффера пишем в файл.
            // Заканчиваем по признаку конца файла -- in.read(buffer) возвращает -1
            while ((receivedBytes = in.read(buffer)) >= 0) {
                out.write(buffer, 0, receivedBytes);
                receivedLength += receivedBytes;

                if (contentLength > 0) {
                    int newProgress = 100 * receivedLength / contentLength;
                    if (newProgress > progress && progressCallback != null) {
                        Log.d(TAG, "Downloaded " + newProgress + "% of " + contentLength + " bytes");
                        progressCallback.onProgressChanged(newProgress);
                    }
                    progress = newProgress;
                }
            }

            if (receivedLength != contentLength) {
                Log.w(TAG, "Received " + receivedLength + " bytes, but expected " + contentLength);
            } else {
                Log.d(TAG, "Received " + receivedLength + " bytes");
            }

        } finally {
            // Закрываем все потоки и соедиениние
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close HTTP input stream: " + e, e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close file: " + e, e);
                }
            }
            conn.disconnect();
        }
    }

    static void downloadFile(String downloadUrl, File destFile) throws IOException {
        downloadFile(downloadUrl, destFile, null /*progressCallback*/);
    }

    private static final String TAG = "fgh_download";


    private DownloadUtils() {}
}