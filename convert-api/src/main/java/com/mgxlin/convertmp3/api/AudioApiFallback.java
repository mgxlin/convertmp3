package com.mgxlin.convertmp3.api;

import com.mgxlin.convertmp3.api.ro.AudioFile;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AudioApiFallback implements FallbackFactory<AudioApi> {


    @Override
    public AudioApi create(Throwable cause) {
        return new AudioApi() {
            @Override
            public String downloadAndConvertAudio(String url) {
                return "请求超时";
            }

            @Override
            public String downloadSortAndMergeAudios(List<AudioFile> audioFiles) {
                return "请求超时";
            }
        };
    }
}
