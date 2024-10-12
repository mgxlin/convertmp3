package com.mgxlin.convertmp3.api;

import com.mgxlin.convertmp3.api.ro.AudioFile;
import com.mgxlin.convertmp3.service.ConvertService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AudioApiImpl implements AudioApi {

    @Resource
    private ConvertService convertService;

    @Override
    public String downloadAndConvertAudio(String url) {
        return convertService.downloadAndConvertAudio(url);
    }

    @Override
    public String downloadSortAndMergeAudios(List<AudioFile> audioFiles) {
        return convertService.downloadSortAndMergeAudios(audioFiles);
    }
}
