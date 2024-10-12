package com.mgxlin.convertmp3.api.ro;

public class AudioFile {
    public String url;
    public Long sort;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getSort() {
        return sort;
    }

    public void setSort(Long sort) {
        this.sort = sort;
    }

    public AudioFile() {
    }

    public AudioFile(String url, Long sort) {
        this.url = url;
        this.sort = sort;
    }
}
