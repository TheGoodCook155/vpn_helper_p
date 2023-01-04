import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Instance of SiteParser object
 */
public class SitesParser implements SitesFetcher{

    private static SitesParser instance;

    private SitesParser(){

    }

    public static SitesParser getSitesParserObjInstance(){

        if (instance == null){
            instance = new SitesParser();
        }
        return instance;
    }


    /**
     * Returns List<BannedWebsite> objects read from blocked_websites.csv
     * @return List<BannedWebsite></>
     */
    @Override
    public List<BannedWebsite> getAllWebsites(){

        List<BannedWebsite> returnedSites = new ArrayList<>();

        String websites = "blocked_websites.csv";

        try (BufferedReader reader = new BufferedReader(new FileReader(websites))){
            String line;

            while ((line = reader.readLine()) != null){

                BannedWebsite bannedWebsite = new BannedWebsite(line.substring(0,line.length()-1));
                returnedSites.add(bannedWebsite);
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        return returnedSites;
    }

}
