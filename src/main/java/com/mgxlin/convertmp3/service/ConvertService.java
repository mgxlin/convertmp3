package com.mgxlin.convertmp3.service;

import com.mgxlin.convertmp3.api.ro.AudioFile;
import com.mgxlin.convertmp3.file.FileToMultipartFile;
import com.wancheli.module.infra.api.file.FileApi;
import com.wancheli.module.infra.api.file.dto.FileUploadRespDTO;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

@Service
public class ConvertService {

    private final CloseableHttpClient httpClient;

    @Resource
    private FileApi fileApi;

    @Autowired
    public ConvertService() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);
        connectionManager.setDefaultMaxPerRoute(20);

        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    private static final String DOWNLOAD_DIR = "G:\\1011\\download\\";
    private static final String OUTPUT_DIR = "G:\\1011\\output\\";

    public String httpClientDownloadFile(String urlString,String uuid) {
        String fileName = uuid + "-" +urlString.substring(urlString.lastIndexOf('/') + 1);
        String filePath = DOWNLOAD_DIR + fileName;

        HttpGet httpGet = new HttpGet(urlString);

        try (CloseableHttpResponse response = httpClient.execute(httpGet);
             FileOutputStream fos = new FileOutputStream(filePath)) {

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try (InputStream inputStream = entity.getContent();
                     ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);
                     FileChannel fileChannel = fos.getChannel()) {

                    fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error downloading file: " + urlString, e);
        }

        return filePath;
    }

    public static String getFileExtension(String fullName) {
        String fileName = new File(fullName).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    private String downloadFile(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        String fileName = urlString.substring(urlString.lastIndexOf('/') + 1);
        String filePath = DOWNLOAD_DIR + fileName;

        try (InputStream in = url.openStream();
             ReadableByteChannel rbc = Channels.newChannel(in);
             FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return filePath;
    }

    public String downloadAndMergeAudios(List<String> urls) {
        try {
            // 创建必要的目录
            new File(DOWNLOAD_DIR).mkdirs();
            new File(OUTPUT_DIR).mkdirs();
            String uuid = UUID.randomUUID().toString().replace("-","");;
            // 并行下载所有文件
            List<CompletableFuture<String>> downloadFutures = urls.stream()
                    .map(url -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return httpClientDownloadFile(url,uuid);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }))
                    .collect(Collectors.toList());

            // 等待所有下载完成
            CompletableFuture.allOf(downloadFutures.toArray(new CompletableFuture[0])).join();

            // 获取下载的文件路径
            List<String> filePaths = downloadFutures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            // 转换所有非 MP3 文件为 MP3
            List<String> mp3Files = filePaths.stream()
                    .map(this::ensureMP3)
                    .collect(Collectors.toList());

            // 合并所有 MP3 文件
            String mergedFilePath = mergeMP3Files(mp3Files);

            // 清理临时文件
            cleanupFiles(mp3Files);

            return "Files merged successfully: " + mergedFilePath;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public String async(List<AudioFile> audioFiles) {
        return downloadMergeAndConvertAudios(audioFiles);
    }

    public String downloadMergeAndConvertAudios(List<AudioFile> audioFiles) {
        try {
            // Create necessary directories
            new File(DOWNLOAD_DIR).mkdirs();
            new File(OUTPUT_DIR).mkdirs();

            // 异步批量下载
            long startDownload = System.currentTimeMillis();
            String uuid = UUID.randomUUID().toString().replace("-","");
            List<CompletableFuture<AbstractMap.SimpleEntry<Long, String>>> downloadFutures = audioFiles.stream()
                    .map(audioFile -> CompletableFuture.supplyAsync(() ->
                            new AbstractMap.SimpleEntry<>(audioFile.sort, httpClientDownloadFile(audioFile.url,uuid))))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(downloadFutures.toArray(new CompletableFuture[0])).join();

            List<String> sortedFilePaths = downloadFutures.stream()
                    .map(CompletableFuture::join)
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());

            long endDownload = System.currentTimeMillis();
            System.out.println(Thread.currentThread().getName()+"Download time: " + (endDownload - startDownload) + "ms");

            // 合并文件
            long startMerge = System.currentTimeMillis();

            String mergedFilePath = mergeAudioFiles(sortedFilePaths,uuid);

            long endMerge = System.currentTimeMillis();
            System.out.println(Thread.currentThread().getName()+"Merge time: " + (endMerge - startMerge) + "ms");

            // 转换为MP3

            long startConvert = System.currentTimeMillis();
            String finalFilePath = ensureMP3(mergedFilePath);
            long endConvert = System.currentTimeMillis();

            System.out.println(Thread.currentThread().getName()+"Convert time: " + (endConvert - startConvert) + "ms");

            // 删除临时文件
            cleanupFiles(sortedFilePaths);
            if (!mergedFilePath.equals(finalFilePath)) {
                new File(mergedFilePath).delete();
            }

            // 上传文件
            uploadFile(finalFilePath);
            return "Files converted successfully: " + finalFilePath;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public void uploadFile(String filePath) {
        try {
            File file = new File(filePath);

            FileToMultipartFile multipartFile = new FileToMultipartFile(file,
                    file.getName(),
                    "application/octet-stream");

            FileUploadRespDTO uploadRespDTO = fileApi.common(multipartFile, 1, 1).getData();

            // 处理上传响应
            System.out.println("File uploaded: " + file.getName() + ", Response: " + uploadRespDTO);
        } catch (Exception e) {
            System.err.println("Error uploading file: " + filePath);
            e.printStackTrace();
        }
    }

    public String downloadSortAndMergeAudios(List<AudioFile> audioFiles) {
        try {
            // 创建必要的目录
            new File(DOWNLOAD_DIR).mkdirs();
            new File(OUTPUT_DIR).mkdirs();

            long start = System.currentTimeMillis();
            String uuid = UUID.randomUUID().toString().replace("-","");;
            // 并行下载所有文件
            List<CompletableFuture<AbstractMap.SimpleEntry<Long, String>>> downloadFutures = audioFiles.stream()
                    .map(audioFile -> CompletableFuture.supplyAsync(() ->
                            new AbstractMap.SimpleEntry<>(audioFile.sort, httpClientDownloadFile(audioFile.url,uuid))))
                    .collect(Collectors.toList());

            // 等待所有下载完成
            CompletableFuture.allOf(downloadFutures.toArray(new CompletableFuture[0])).join();

            // 获取下载的文件路径，并按 sort 值正序排序
            List<String> sortedFilePaths = downloadFutures.stream()
                    .map(CompletableFuture::join)
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());

            // 并行转换所有非 MP3 文件为 MP3
            List<CompletableFuture<String>> mp3Futures = sortedFilePaths.stream()
                    .map(filePath -> CompletableFuture.supplyAsync(() -> ensureMP3(filePath)))
                    .collect(Collectors.toList());

            // 等待所有转换完成
            CompletableFuture.allOf(mp3Futures.toArray(new CompletableFuture[0])).join();

            // 获取转换后的 MP3 文件路径
            List<String> mp3Files = mp3Futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            // 合并所有 MP3 文件
            String mergedFilePath = mergeMP3Files(mp3Files);
            // 清理临时文件
            cleanupFiles(mp3Files);

            return "Files downloaded, sorted, converted, and merged successfully: " + mergedFilePath;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String ensureMP3(String filePath) {
        if (isMP3(filePath)) {
            return filePath;
        }
        return convertToMp3(filePath);
    }

    private boolean isMP3(String filePath) {
        return filePath.toLowerCase().endsWith(".mp3");
    }

    private String convertToMp3(String inputFilePath) {
        try {
            String outputFilePath = inputFilePath.substring(0, inputFilePath.lastIndexOf('.')) + ".mp3";
            String command = String.format("ffmpeg -y -i %s -acodec libmp3lame %s", inputFilePath, outputFilePath);
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            new File(inputFilePath).delete();
            return outputFilePath;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert file to MP3: " + inputFilePath, e);
        }
    }

    public String downloadAndConvertAudio(String url) {
        try {
            String uuid = UUID.randomUUID().toString().replace("-","");;
            // 下载文件
            String downloadedFilePath = httpClientDownloadFile(url,uuid);

            // 检查是否为 MP3 文件
            if (isMP3(downloadedFilePath)) {
                return "File downloaded successfully (already in MP3 format): " + downloadedFilePath;
            }

            // 转换为 MP3
            String mp3FilePath = convertToMp3(downloadedFilePath);

            return "File downloaded and converted successfully: " + mp3FilePath;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public String mergeAudioFiles(List<String> audioFiles,String uuid) throws IOException, InterruptedException {
        // Determine the output format based on the first file
        String outputExtension = getFileExtension(audioFiles.get(0));
        String outputFile = OUTPUT_DIR + "merged_" + uuid + "." + outputExtension;

        // Create a temporary file list
        String fileList = createFileList(audioFiles,uuid);

        // Construct the FFmpeg command
        String command = String.format("ffmpeg -y -f concat -safe 0 -i %s -c copy %s", fileList, outputFile);

        // Execute the FFmpeg command
        Process process = Runtime.getRuntime().exec(command);
        try {
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                // 处理超时情况
                System.out.println("操作未完成"+uuid);
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            // 处理中断异常
            e.printStackTrace();
        } finally {
            new File(fileList).delete();
        }
        
        return outputFile;
    }

    private String mergeMP3Files(List<String> mp3Files) throws IOException, InterruptedException {
        String outputFile = OUTPUT_DIR + "merged_" + UUID.randomUUID() + ".mp3";
        String fileList = createFileList(mp3Files,UUID.randomUUID().toString().replace("-",""));
        String command = String.format("ffmpeg -f concat -safe 0 -i %s -c copy %s", fileList, outputFile);
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
        new File(fileList).delete();
        return outputFile;
    }

    private String createFileList(List<String> files, String uuid) throws IOException {
        String fileListPath = DOWNLOAD_DIR + uuid + ".txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileListPath))) {
            for (String file : files) {
                writer.println("file '" + new File(file).getAbsolutePath() + "'");
            }
        }
        return fileListPath;
    }

    private void cleanupFiles(List<String> files) {
        for (String file : files) {
            new File(file).delete();
        }
    }
}
