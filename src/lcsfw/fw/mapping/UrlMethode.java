package lcsfw.fw.mapping;

import lcsfw.fw.http.HttpMethode;

public class UrlMethode {
    String url;
    HttpMethode methode;

    public UrlMethode(String url, HttpMethode methode) {
        this.url = url;
        this.methode = methode;
    }

    @Override
    public boolean equals(Object obj) {
        return this.url.equals(obj);
    }
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HttpMethode getMethode() {
        return methode;
    }

    public void setMethode(HttpMethode methode) {
        this.methode = methode;
    }

}
