public class BannedWebsite {

    private String siteUrl;

    public BannedWebsite(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    @Override
    public String toString() {
        return "BannedWebsite{" +
                "siteUrl='" + siteUrl + '\'' +
                '}';
    }
}
