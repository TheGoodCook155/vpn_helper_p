import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Class used for fetching the servers from json.
 */
public class ServerParser implements ServerFetcher{


    private static ServerParser instance;
    private static File file = new File("cached_serverlist.json");
    private static LoggerCls logger = LoggerCls.getLoggerInstance();

    private   ServerParser serverParser;

    private JSONObject jsonObject;

    private ServerParser(){
        jsonObject = new JSONObject(file);
    }

    public static ServerParser getServerParserObjInstance(){
        logger.logThis().info("ServerParser | getJsonObjInstance() get instance called");
        if (instance == null){
            instance = new ServerParser();
        }
        return instance;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public static LoggerCls getLogger() {
        return logger;
    }

    /**
     * Method used for returning List<ServerObj> that were fetched from the json file
     */
    @Override
    public List<ServerObj> getListOfServers() throws IOException {

        logger.logThis().info("ServerParser | getListOfServers() | Attempting to refresh the server cache");

        //refresh the server cache
        copyConfigToCurrentFolder();


         List<ServerObj> res = new ArrayList<>();

         serverParser = ServerParser.getServerParserObjInstance();

        String jsonStr = "";
        StringBuilder stringBuilder = new StringBuilder();

        Scanner scanner = new Scanner(new File("cached_serverlist.json"));

        while (scanner.hasNext()){
            stringBuilder.append(scanner.nextLine());
            stringBuilder.append("\n");
        }

        jsonStr = stringBuilder.toString();

        JSONObject jsonObj = new JSONObject(jsonStr);

        //Servers arr
        JSONArray servers = jsonObj.getJSONArray("LogicalServers");

        for (int i = 0; i < servers.length(); i++){

            JSONObject server = (JSONObject) servers.get(i);
            String serverName = server.getString("Name");
            ServerObj serverObj = new ServerObj(serverName);
            res.add(serverObj);
        }

        return res;
    }

    //copy the cached_serverlist.json file from the host PC
    // ~/.cache/protonvpn/cached_serverlist.json
    public void copyConfigToCurrentFolder() throws IOException {

        String userHome = System.getProperty("user.home");
        String currentWorkingDir = System.getProperty("user.dir");
        Path currentWorkingDirPath = Path.of(currentWorkingDir);

        logger.logThis().info("ServerParser | copyConfigToCurrentFolder() | Current Directory Working Path: " + currentWorkingDir);
        logger.logThis().info("ServerParser | copyConfigToCurrentFolder() |UserHome: " + userHome);

        Path serverConfigFilePath = Paths.get(userHome + "/.cache/protonvpn/cached_serverlist.json");

        logger.logThis().info("ServerParser | copyConfigToCurrentFolder() | serverConfigFilePath:  " + serverConfigFilePath);

        if (Files.exists(serverConfigFilePath)){
            //copy to current working folder
            logger.logThis().info("ServerParser | copyConfigToCurrentFolder() | File exists: " + serverConfigFilePath);

            File sourceFile = serverConfigFilePath.toFile();
            manualCopy(sourceFile,currentWorkingDirPath);
            logger.logThis().info("ServerParser | copyConfigToCurrentFolder() | Copy successful ");
        }

    }

    private void manualCopy(File source, Path destination) {

        File toWriteTo = new File("cached_serverlist.json");

        try (InputStream in = new BufferedInputStream(new FileInputStream(source));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(destination + "/" + toWriteTo))) {

            byte[] buffer = new byte[2048];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
                out.flush();
            }
        } catch (IOException e) {
            logger.logThis().info("ServerParser | manualCopy() | IO exception " + e.getMessage());
            e.printStackTrace();
        }
    }

}
