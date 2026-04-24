package com.fqt.fqtmod.translation;

import com.fqt.fqtmod.FTBQuestTranslator;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TTSManager {
    private static final TTSManager INSTANCE = new TTSManager();
    private volatile Thread playbackThread;
    private volatile boolean isLoadingFlag = false;
    private volatile boolean isPlayingFlag = false;
    private volatile boolean hasErrorFlag = false;
    private volatile String lastError = null;
    private volatile long errorTimestamp = 0;
    private volatile SourceDataLine currentLine = null;

    private static final Set<String> SUPPORTED_LANGS = new HashSet<>(Arrays.asList(
            "af", "sq", "am", "ar", "hy", "as", "ay", "az", "bm", "eu", "be", "bn", "bho", "bs", "bg", "ca", "ceb", "zh", "zh-TW", "co", "hr", "cs", "da", "dv", "nl", "en", "eo", "et", "ee", "tl", "fi", "fr", "fy", "gl", "ka", "de", "el", "gn", "gu", "ht", "ha", "haw", "hi", "hmn", "hu", "is", "ig", "ilo", "id", "ga", "it", "ja", "jv", "kn", "kk", "km", "rw", "ko", "kri", "ku", "ky", "lo", "la", "lv", "ln", "lt", "lg", "lb", "mk", "mg", "ms", "ml", "mt", "mi", "mr", "mn", "my", "ne", "no", "ny", "or", "om", "ps", "fa", "pl", "pt", "pa", "qu", "ro", "ru", "sm", "sa", "gd", "nso", "sr", "st", "sn", "sd", "si", "sk", "sl", "so", "es", "su", "sw", "sv", "tg", "ta", "tt", "te", "th", "ti", "ts", "tr", "tk", "ak", "uk", "ur", "ug", "uz", "vi", "cy", "xh", "yi", "yo", "zu"
    ));

    public static TTSManager getInstance() { return INSTANCE; }
    public boolean isLoading() { return isLoadingFlag; }
    public boolean isPlaying() { return isPlayingFlag; }

    public boolean hasError() {
        if (hasErrorFlag && System.currentTimeMillis() - errorTimestamp > 5000) { hasErrorFlag = false; lastError = null; }
        return hasErrorFlag;
    }

    public String getLastError() { return lastError; }

    public boolean supportsLanguage(String langCode) {
        if (langCode == null) return false;
        String base = LanguageMapper.getApiLanguageCode(langCode).toLowerCase();
        if (SUPPORTED_LANGS.contains(base)) return true;
        if (base.contains("-")) return SUPPORTED_LANGS.contains(base.split("-")[0]);
        return false;
    }

    public void stop() {
        isLoadingFlag = false; isPlayingFlag = false;
        if (currentLine != null) {
            try { currentLine.stop(); currentLine.flush(); currentLine.close(); } catch (Exception ignored) {}
            currentLine = null;
        }
        if (playbackThread != null && playbackThread.isAlive()) { playbackThread.interrupt(); playbackThread = null; }
    }

    private void setError(String error) {
        hasErrorFlag = true; lastError = error; errorTimestamp = System.currentTimeMillis(); stop();
    }

    public void play(String text, String langCode) {
        stop(); hasErrorFlag = false; lastError = null;
        if (text == null || text.trim().isEmpty()) return;
        String apiLang = LanguageMapper.getApiLanguageCode(langCode);
        if (!supportsLanguage(langCode)) { setError("TTS not supported for: " + apiLang); return; }
        isLoadingFlag = true;
        playbackThread = new Thread(() -> {
            SourceDataLine line = null;
            try {
                String cleanText = TextFormatUtils.stripAllFormatting(text);
                
                // Split text into chunks of max 180 chars to bypass Google TTS API limit and avoid cutoffs
                java.util.List<String> chunks = new java.util.ArrayList<>();
                String[] sentences = cleanText.split("(?<=[.!?])\\s+");
                
                for (String sentence : sentences) {
                    if (sentence.length() <= 180) {
                        chunks.add(sentence);
                    } else {
                        // Split by spaces if possible
                        String[] words = sentence.split("\\s+");
                        StringBuilder currentChunk = new StringBuilder();
                        for (String word : words) {
                            if (word.length() > 180) {
                                // Word itself is too long (e.g. no spaces), split by chars
                                if (currentChunk.length() > 0) {
                                    chunks.add(currentChunk.toString());
                                    currentChunk = new StringBuilder();
                                }
                                for (int i = 0; i < word.length(); i += 180) {
                                    chunks.add(word.substring(i, Math.min(i + 180, word.length())));
                                }
                            } else if (currentChunk.length() + word.length() + 1 <= 180) {
                                if (currentChunk.length() > 0) currentChunk.append(" ");
                                currentChunk.append(word);
                            } else {
                                chunks.add(currentChunk.toString());
                                currentChunk = new StringBuilder(word);
                            }
                        }
                        if (currentChunk.length() > 0) chunks.add(currentChunk.toString());
                    }
                }

                ByteArrayOutputStream pcmOut = new ByteArrayOutputStream();
                AudioFormat audioFormat = null;
                
                for (int cIdx = 0; cIdx < chunks.size(); cIdx++) {
                    String chunk = chunks.get(cIdx);
                    if (Thread.currentThread().isInterrupted()) return;
                    String encoded = URLEncoder.encode(chunk, StandardCharsets.UTF_8);
                    String urlStr = "https://translate.googleapis.com/translate_tts?ie=UTF-8&tl=" + apiLang 
                            + "&client=gtx&total=" + chunks.size() + "&idx=" + cIdx 
                            + "&textlen=" + chunk.length() + "&q=" + encoded;

                    java.net.URL url = new URI(urlStr).toURL();
                    byte[] mp3Bytes = null;
                    int retryCount = 0;
                    
                    while (retryCount < 3) {
                        if (Thread.currentThread().isInterrupted()) return;
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                        conn.setRequestProperty("Referer", "https://translate.google.com/");
                        conn.setConnectTimeout(10000);
                        conn.setReadTimeout(15000);

                        int responseCode = conn.getResponseCode();
                        if (responseCode == 200) {
                            try (BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = bis.read(buffer)) != -1) {
                                    baos.write(buffer, 0, bytesRead);
                                }
                                mp3Bytes = baos.toByteArray();
                            }
                            break;
                        } else if (responseCode == 429) {
                            retryCount++;
                            try { Thread.sleep(1000 * retryCount); } catch (InterruptedException e) { return; }
                            continue;
                        } else {
                            break;
                        }
                    }

                    if (mp3Bytes == null || mp3Bytes.length < 100) continue;

                    try {
                        javazoom.jl.decoder.Decoder decoder = new javazoom.jl.decoder.Decoder();
                        javazoom.jl.decoder.Bitstream bitstream = new javazoom.jl.decoder.Bitstream(new ByteArrayInputStream(mp3Bytes));

                        boolean isFirstFrame = (audioFormat == null);

                        while (true) {
                            if (Thread.currentThread().isInterrupted()) return;
                            javazoom.jl.decoder.Header header = bitstream.readFrame();
                            if (header == null) break;
                            javazoom.jl.decoder.SampleBuffer sample = (javazoom.jl.decoder.SampleBuffer) decoder.decodeFrame(header, bitstream);
                            
                            if (isFirstFrame) {
                                int sampleRate = sample.getSampleFrequency();
                                int channels = sample.getChannelCount();
                                
                                double speedFactor = 1.0;
                                try {
                                    speedFactor = com.fqt.fqtmod.FabricConfig.getTtsRate() * com.fqt.fqtmod.FabricConfig.getTtsPitch();
                                } catch (Exception ignored) {}
                                if (speedFactor <= 0) speedFactor = 1.0;
                                
                                int newSampleRate = (int) (sampleRate * speedFactor);

                                audioFormat = new AudioFormat(
                                        AudioFormat.Encoding.PCM_SIGNED,
                                        newSampleRate, 16, channels,
                                        channels * 2, sampleRate, false
                                );
                                isFirstFrame = false;
                            }
                            
                            short[] buf = sample.getBuffer();
                            int bufLen = sample.getBufferLength();
                            for (int i = 0; i < bufLen; i++) {
                                pcmOut.write(buf[i] & 0xFF);
                                pcmOut.write((buf[i] >> 8) & 0xFF);
                            }
                            bitstream.closeFrame();
                        }
                        bitstream.close();
                    } catch (Exception e) {
                        // Ignore chunk decode errors and continue
                    }
                    try { Thread.sleep(800); } catch (InterruptedException e) { return; }
                }

                if (audioFormat == null || pcmOut.size() == 0) {
                    setError("TTS returned empty audio");
                    return;
                }
                
                byte[] pcmBytes = pcmOut.toByteArray();
                if (Thread.currentThread().isInterrupted()) return;
                
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                line = (SourceDataLine) AudioSystem.getLine(info); 
                line.open(audioFormat); 
                line.start();
                currentLine = line;
                
                isLoadingFlag = false; 
                isPlayingFlag = true;
                
                int chunkSize = 4096;
                for (int offset = 0; offset < pcmBytes.length; offset += chunkSize) {
                    if (Thread.currentThread().isInterrupted()) break;
                    line.write(pcmBytes, offset, Math.min(chunkSize, pcmBytes.length - offset));
                }
                
                if (!Thread.currentThread().isInterrupted()) {
                    line.drain();
                } else {
                    line.flush();
                }
                isPlayingFlag = false;
            } catch (Throwable e) { 
                setError("TTS error: " + e.getMessage()); 
                FTBQuestTranslator.LOGGER.error("TTS playback failed", e);
            } finally {
                isLoadingFlag = false; isPlayingFlag = false;
                if (line != null) { try { line.stop(); line.close(); } catch (Exception ignored) {} }
            }
        }, "TTS-Playback");
        playbackThread.setDaemon(true); playbackThread.start();
    }
}
