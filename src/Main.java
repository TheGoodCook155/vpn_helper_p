import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class Main {

    private static boolean runProgram = true;
    private static boolean isLinux = false;

    private static LoggerCls logger;

    private static  Runtime runtime;

    private static RuntimeParser runtimeParser;

    private static ServerParser serverParser;

    private static PVPNAsciiLogoPrinter logoPrinter;

    private static SitesParser sitesParser;

    private static AppShutdownListener appShutdownListener;



    public static void main(String[] args) throws Exception {

        logger = LoggerCls.getLoggerInstance();
        runtime = Runtime.getRuntime();
        serverParser = ServerParser.getServerParserObjInstance();
        logoPrinter = new PVPNAsciiLogoPrinter();
        sitesParser = SitesParser.getSitesParserObjInstance();
        appShutdownListener = AppShutdownListener.getAppShutdownListenerInstance();


        logger.logThis().info("OS is: " + System.getProperty("os.name").toLowerCase());

        if (System.getProperty("os.name").toLowerCase().equals("linux")){
            isLinux = true;
        }

        Scanner scanner = new Scanner(System.in);
        String pickedOption = "3";

        System.out.println("\nProtonVPN helper\n");

        logoPrinter.printLogo();

        if(isLinux){

            while (runProgram){

                System.out.println("Please choose an option:\n");
                System.out.println("1. Access blocked website with servers:");
                System.out.println("2. Check if site resolves while connected to a server/s:");
                System.out.println("3. Quit");

                pickedOption = sanitize(scanner.nextLine());


                switch (pickedOption){

                    case "1":
                        runProgram = checkSiteWithServers(serverParser);
                        break;
                    case "2":
                        checkSiteResolves();
                        break;
                    case "3":
                        disconnectVPN(runtimeParser);
                        runProgram = false;
                        break;
                    default:
                        logger.logThis().info("switch block | Invalid entry: " + pickedOption);
                        System.out.println("\nInvalid entry. Please try again. Use digits only (1-3):\n\n");
                }
            }

        }else{
            logger.logThis().info("System is not Linux: Quiting: ");
            System.out.println("\n Unsupported OS. Please run the app on Linux. Quiting!");
        }

    }

    /**
     *
     * @param serverParser receives serverParser instance
     * @return The returned value is always false. The value is returned either from checkAgainstAllServers() or checkOneServerAgainstSitesDb() so when hard killing the app/or when exiting from the calling method, the main menu won't get printed
     * @throws IOException
     */
    private static boolean checkSiteWithServers(ServerParser serverParser) throws IOException {

        logger.logThis().info("checkSiteWithServers() started: ");

        boolean localRun = true;
        Scanner scanner = new Scanner(System.in);
        String server = "";
        String siteToCheck = "";
        String optionPicked = "";
        boolean terminateWhileFalse = true;

        List<ServerObj> allServers = serverParser.getListOfServers();

        System.out.println("\nYou chose (Access blocked website with servers): ");

        while (localRun){

            logger.logThis().info("checkSiteWithServers() | whileLoop: ");

            System.out.println("\nPlease choose an option: \n");

            System.out.println("1. Check against all servers available to you: ");
            System.out.println("2. Check against country servers (enter two digit country letter):");
            System.out.println("3. Check servers against sites (every server -> every site):");
            System.out.println("4. Go back (<-)");

            optionPicked = scanner.nextLine();

            logger.logThis().info("checkSiteWithServers() | whileLoop: | after optionPicked: ");

            switch (optionPicked){

                case "1":
                   localRun = checkAgainstAllServers(allServers);
                    terminateWhileFalse = localRun;
                    break;
                case "2":
                    checkAgainstCountryServers(allServers);
                    break;
                case "3":
                    localRun = checkOneServerAgainstSitesDb(allServers);
                    terminateWhileFalse = localRun;
                    break;
                case "4":
                    System.out.println("Returning.\n");
                    localRun = false;
                    break;
                default:
                    System.out.println("Invalid entry, restarting.\n");
                    localRun = false;
                    break;
            }
        }
            return terminateWhileFalse;

    }

    /**
     * Finds the HTTP response status code. Iterates over a list of servers available to the user. Once connected to a server it iterates over a list
     * of BannedWebsite objects read from blocked_websites.csv and checks the HTTP response status code for each website.
     * In case the app is killed prematurely, you'll get prompted to save the last index from @param allServers
     * In case you have saved the index previously, you'll get prompted to load the last saved session.
     * @param allServers List of the servers available to the logged-in user
     * @return The return value is always false
     */
    private static boolean checkOneServerAgainstSitesDb(List<ServerObj> allServers) {

        logger.logThis().info("checkOneServerAgainstSitesDb() | Started");


        List<BannedWebsite> bannedWebsites = sitesParser.getAllWebsites();

        String folderPath = createFolder().toString();
        logger.logThis().info("checkOneServerAgainstSitesDb() | createFolder() | Folder created: ");
        logger.logThis().info("checkOneServerAgainstSitesDb() | folderPath: " + folderPath);


        BufferedWriter writer = null;


        AppShutdownListener appShutdownListener = AppShutdownListener.getAppShutdownListenerInstance();
        Scanner saveDataScanner = new Scanner(System.in);

        SaveCurrentIndex saveIndexSaveObj = new SaveCurrentIndex();
        Thread mainThread = Thread.currentThread();

        mainThread.setName("Main thread");

        int startingIndex = 0;

        boolean listenerStarted = appShutdownListener.getAppShutdownListenerInstance().isHookIsRunning();
        final boolean[] stopLoop = {false};

        startingIndex = loadSavedIndex();
        logger.logThis().info("checkOneServerAgainstSitesDb() | startingIndex: " + saveIndexSaveObj.getIndex());


        System.out.println("\nStarted, hit ctrl+c to kill the app prematurely");


        logger.logThis().info("checkOneServerAgainstSitesDb() | createFolder() | Folder created: ");
        logger.logThis().info("checkOneServerAgainstSitesDb() | folderPath: " + folderPath);


        synchronized (saveIndexSaveObj){

            for (int i = startingIndex; i < allServers.size(); i++){

                logger.logThis().info("checkOneServerAgainstSitesDb | load index: " + i);
                ServerObj server = allServers.get(i);

                String serverName = server.getServerName();
                logger.logThis().info("checkOneServerAgainstSitesDb() | checking serverName " + serverName);

                String fileName = "server_" + serverName + "_allWebsites.txt";

                Path filePath = Paths.get(folderPath + "/" + fileName);

                if (stopLoop[0] == true){
                    break;
                }

                saveIndexSaveObj.setIndex(i);

                logger.logThis().info("checkOneServerAgainstSitesDb() | saveIndexSaveObj: " + saveIndexSaveObj.getIndex());


                try {
                    logger.logThis().info("checkOneServerAgainstSitesDb() | filePath: " + filePath);
                    writer = new BufferedWriter(new FileWriter(filePath.toFile()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try {
                    boolean isConnected = connectAndCheckIfServerIsConnected(server);
                    if (isConnected) {


                        if (listenerStarted == false){
                            appShutdownListener.setHookIsRunning(true);
                            listenerStarted = true;

                            Thread terminateThread = new Thread() {
                                public void run() {
                                    stopLoop[0] = true;

                                    this.setName("Second Thread");

                                    System.out.println("\n\nShutting down... Save index: y/n?\n");

                                    String command = saveDataScanner.nextLine();
                                    if (command.equals("y")){
                                        saveIndexSaveObj.saveIndex(saveIndexSaveObj);
                                        System.out.println("\nLast saved index: " + saveIndexSaveObj.getIndex());
                                    }else{
                                        System.out.println("Not saved: ");
                                    }
                                    logger.logThis().info("checkOneServerAgainstSitesDb() | Second thread started | Saved index: " + saveIndexSaveObj.getIndex());


                                    logger.logThis().info("checkOneServerAgainstSitesDb() | Second thread started | localRun: "  + " About to kill main thread");

                                    mainThread.stop();
                                }
                            };

                            try {

                                terminateThread.join();

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Runtime.getRuntime().addShutdownHook(terminateThread);
                        }



                        logger.logThis().info("checkOneServerAgainstSitesDb() | isConnected is true");

                        String prefix = "http://";

                        HttpURLConnection connection = null;

                        int responseCode = -1;

                        for (int j = 0; j < bannedWebsites.size(); j++){
                            
                            if (stopLoop[0] == true){
                                break;
                            }

                            BannedWebsite bannedWebsite = bannedWebsites.get(j);
                            String siteURL = bannedWebsite.getSiteUrl();

                            String website = prefix + siteURL;

                            try {

                                URL url = new URL(website);
                                logger.logThis().info("Connecting to: " + url.toString());
                                connection = (HttpURLConnection)url.openConnection();
                                connection.setRequestMethod("GET");
                                connection.setConnectTimeout(10000);
                                connection.setReadTimeout(10000);
                                connection.connect();
                                responseCode = connection.getResponseCode();

                                writer.write("\n==============================\n");
                                System.out.println("\n==============================\n");
                                writer.write("HTTP status code: " + responseCode);
                                writer.write("\n");
                                System.out.println("HTTP status code: " + responseCode);

                                if (responseCode == 200){
                                    writer.write("Able to reach the site: " + website);
                                    writer.write("\n");
                                    System.out.println("Able to reach the site: " + website);
                                }else {
                                    if (responseCode == 403) {
                                        writer.write("Is not able to reach the site (blocked): " + website);
                                        writer.write("\n");
                                        System.out.println("Is not able to reach the site (blocked): " + website);
                                    }

                                    if (responseCode == 301 || responseCode == 302){
                                        String redirectedUrl = connection.getHeaderField("Location");
                                        writer.write("Redirect: (301 || 302). " + website + "\n");
                                        writer.write("Redirected to: " + redirectedUrl);
                                        writer.write("\n");
                                        System.out.println("Redirect: (301 || 302). " + website);
                                        System.out.println("Redirect URL: " + redirectedUrl);
                                    }

                                    if (responseCode == 502){
                                        writer.write("Didn't reach the site. Bad Gateway: " + website);
                                        writer.write("\n");
                                        System.out.println("Didn't reach the site. Bad Gateway: " + website);
                                    }
                                }
                                System.out.println("\n==============================\n");
                                writer.write("\n==============================\n");
                                writer.flush();
                            }catch (MalformedURLException e){
                                e.printStackTrace();
                                writer.write("checkOneServerAgainstSitesDb | MalformedURLException | " + e + "\n");
                            }catch (UnknownHostException e){
                                e.printStackTrace();
                                writer.write("checkOneServerAgainstSitesDb | UnknownHostException | " + e + "\n");
                            }catch (SocketTimeoutException e){
                                e.printStackTrace();
                                writer.write("checkOneServerAgainstSitesDb | UnknownHostException | " + e + "\n");
                            }catch (IOException e){
                                e.printStackTrace();
                                writer.write("checkOneServerAgainstSitesDb | SocketTimeoutException | " + e + "\n");
                            } finally {
                                connection.disconnect();
                                try {
                                    Thread.sleep(1000);
                                }catch (InterruptedException e){
                                    writer.write("checkOneServerAgainstSitesDb() | Interrupted exception: " + e + "\n");
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.logThis().log(Level.SEVERE, "Not connected!");
                    throw new RuntimeException(e);
                }

            }

            }
             return false;
        }


    /**
     *Finds the HTTP response status code. Filters the servers by country by entering a two-letter country code.
     * Once you choose a website (checks against a single website) it will start to iterate over the filtered list of country servers, connect to each server and find the HTTP response status code for the site entered.
     * @param allServers List of servers available to the logged-in user
     * @throws IOException
     */

    private static void checkAgainstCountryServers(List<ServerObj> allServers) throws IOException {

        List<ServerObj> filtered = new ArrayList<>();

        Scanner scanner = new Scanner(System.in);

        String folderPath = createFolder().toString();
        logger.logThis().info("checkAgainstCountryServers() | createFolder() | Folder created: ");
        logger.logThis().info("checkAgainstCountryServers() | folderPath: " + folderPath);

        boolean localRun = true;

        while (localRun) {

            System.out.println("\nEnter the two letter country code: e.g. CH = Switzerland");

            String entry = scanner.nextLine().toUpperCase();


            if (entry.length() > 2 || entry.length() < 2) {
                System.out.println("Invalid entry. Enter two digit code: ");
                logger.logThis().info("checkAgainstCountryServers() | Invalid entry. Entry length > 2");
                return;
            } else {
                logger.logThis().info("checkAgainstCountryServers() | Filtering started: ");


                allServers.stream().filter(el -> el.getServerName().startsWith(entry))
                        .forEach(el -> {
                            filtered.add(el);
                        });

                if (filtered.size() > 0) {
                    logger.logThis().info("checkAgainstCountryServers() | Filtered servers are available: ");

                    String site = "";

                    System.out.println("Please enter the site to be checked: ");

                    site = scanner.next();
                    AtomicBoolean siteIsAvailable = new AtomicBoolean(false);

                    if (checkValidWebUrl(site)) {

                        System.out.println("Started... Enter ctrl+c to kill the app prematurely\n");

                        String fileName = site + ".txt";

                        Path filePath = Paths.get(folderPath + "/" + fileName);

                        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()));

                        String finalSite = site;

                        logger.logThis().info("checkAgainstCountryServers() | Filtered servers size: " + filtered.size());


                        filtered.forEach(server -> {
                            try {
                                boolean isConnected = connectAndCheckIfServerIsConnected(server, finalSite);
                                if (isConnected) {
                                    logger.logThis().info("checkAgainstCountryServers() | siteIsAvailable is:" + siteIsAvailable.get() + " | entering checkReturnCodeAndScrapeHtml(): ");
                                    checkReturnCode(finalSite, writer, server);
                                }
                            } catch (IOException e) {
                                logger.logThis().log(Level.SEVERE, "checkSiteAvailability() failed, Not connected!");
                                throw new RuntimeException(e);
                            }
                        });

                        localRun = false;


                    } else {
                        System.out.println("Invalid site. Please try again");
                        logger.logThis().info("checkAgainstCountryServers() | Invalid site");
                        return;
                    }
                }else{
                    System.out.println("Didn't find servers with the two letter code entered. Please try again!");
                    logger.logThis().info("checkAgainstCountryServers() | No servers found with the given predicate: " + entry);
                }
            }

        }
    }

    /**
     *Finds the HTTP response status code.
     *Once you choose a website (checks against a single website) it will start to iterate over the list of servers available to the user, connect to each server and find the HTTP response status code for the site entered.
     * In case the app is killed prematurely, you'll get prompted to save the last index from @param allServers
     * In case you have saved the index previously, you'll get prompted to load the last saved session.
     * @param allServers allServers List of servers available to the logged-in user
     * @return The return value is always false
     * @throws IOException
     */
    private static boolean checkAgainstAllServers(List<ServerObj> allServers) throws IOException {

        logger.logThis().info("checkAgainstAllServers() started: ");
        String site = "";
        final boolean[] localRun = {true};
        Scanner scanner = new Scanner(System.in);
        int startingIndex = 0;


        Scanner saveDataScanner = new Scanner(System.in);
        SaveCurrentIndex saveCurrentIndexObj = new SaveCurrentIndex();
        Thread mainThread = Thread.currentThread();
        boolean listenerStarted = appShutdownListener.getAppShutdownListenerInstance().isHookIsRunning();
        final boolean[] stopLoop = {false};

        startingIndex = loadSavedIndex();


        String folderPath = createFolder().toString();
        logger.logThis().info("checkAgainstAllServers() | createFolder() | Folder created: ");
        logger.logThis().info("checkAgainstAllServers() | folderPath: " + folderPath);

        while (localRun[0]){

            logger.logThis().info("checkAgainstAllServers() while() started: ");

            System.out.println("Please enter the site to be checked: ");

            site = scanner.next();
            AtomicBoolean siteIsAvailable = new AtomicBoolean(false);

            if (checkValidWebUrl(site)){

                String fileName = site + ".txt";

                Path filePath = Paths.get(folderPath + "/" + fileName);
                logger.logThis().info("checkAgainstAllServers() | filePath: " + filePath);

                BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()));

                String finalSite = site;


                System.out.println("\nStarted... Enter ctrl+c to kill the app prematurely\n");


                synchronized (saveCurrentIndexObj){


                    for (int i = startingIndex; i < allServers.size(); i++){

                        logger.logThis().info("checkAgainstAllServers | load index: " + i);

                        ServerObj server = allServers.get(i);

                        if (stopLoop[0] == true){
                            break;
                        }

                        saveCurrentIndexObj.setIndex(i);

                        if (listenerStarted == false){

                            appShutdownListener.setHookIsRunning(true);
                            listenerStarted = true;

                            Thread terminateThread = new Thread() {
                                public void run() {
                                    mainThread.stop();
                                    stopLoop[0] = true;
                                    this.setName("Second Thread");
                                    logger.logThis().info("checkAgainstAllServers | Second thread started: " + this.getName());

                                    System.out.println("\n\nShutting down... Save index: y/n?\n");

                                    String command = saveDataScanner.nextLine();
                                    if (command.equals("y")){
                                        saveCurrentIndexObj.saveIndex(saveCurrentIndexObj);
                                    }else{
                                        System.out.println("Not saved: ");
                                    }
                                    System.out.println("\nLast saved index: " + saveCurrentIndexObj.getIndex());

                                    logger.logThis().info("checkAgainstAllServers | Second thread started | Saved index: " + saveCurrentIndexObj.getIndex());

                                    localRun[0] = false;

                                    logger.logThis().info("checkAgainstAllServers | Second thread started | localRun: " + localRun[0] + " About to kill main thread");

                                }
                            };
                            try {

                                terminateThread.join();

                            }catch (InterruptedException e){
                                e.printStackTrace();
                                logger.logThis().info("checkAgainstAllServers() | InterruptedException | Error termination main thread");
                            }

                            Runtime.getRuntime().addShutdownHook(terminateThread);
                        }

                        try {
                            boolean isConnected =  connectAndCheckIfServerIsConnected(server, finalSite);
                            if (isConnected){

                                logger.logThis().info("checkAgainstAllServers() | while() | siteIsAvailable is:" + siteIsAvailable.get() + " | entering checkReturnCodeAndScrapeHtml(): ");
                                checkReturnCode(finalSite,writer,server);
                            }
                        } catch (IOException e) {
                            logger.logThis().log(Level.SEVERE,"checkSiteAvailability() failed, Not connected!");
                            throw new RuntimeException(e);
                        }
                    }

                }


                localRun[0] = false;
                //you are still connected at this point
                logger.logThis().info("checkAgainstAllServers() while() ends: | localRun = " + localRun[0]);

            }else{
                System.out.println("Invalid website, please enter website again: ");
                logger.logThis().log(Level.SEVERE,"checkAgainstAllServers() Invalid website entered: ");
                System.out.println("\nPlease enter the website you would like to test e.g. (www.google.com): \n");
            }
        }
        return false;

    }

    /**
     * If save.dat file exists, it will return the saved index.
     * @return It returns the starting index of List<ServerObj> or 0 if the save.dat file does not exists</>
     */
    private static int loadSavedIndex() {

        Scanner scanner = new Scanner(System.in);
        String command = "";

        int startingIndex = 0;
        String localDirLoad = System.getProperty("user.dir");
        String fileNameLoad = "save.dat";
        String loadLocation = localDirLoad + "/" + fileNameLoad;
        Path path = Path.of(loadLocation);

        if (Files.exists(path)){

            System.out.println("Do you want to load the last saved index position: y/n?");
            while (true) {
                command = scanner.nextLine();

                if (command.equals("y") || command.equals("n")){
                    if (command.equals("y")){

                        SaveCurrentIndex savedIndex = SaveCurrentIndex.loadIndex();
                        if (savedIndex != null && savedIndex.getIndex() > 0){
                            startingIndex = savedIndex.getIndex();
                        }

                    }else{
                        startingIndex = 0;
                    }
                    break;
                }else{
                    System.out.println("Invalid command: Enter a valid command: y/n");
                }
            }

        }
        return startingIndex;
    }

    /**
     * Checks if connection is established based on the PVPN dedicated app output
     * @param server ServerObj
     * @param site Website URL
     * @return Returns whether a connection is established
     * @throws IOException
     */
    private static boolean connectAndCheckIfServerIsConnected(ServerObj server, String site) throws IOException {

        String serverName = server.getServerName();

        String startVPNCommand = "protonvpn-cli c " + serverName;

        //connect to the server
        Process process = runtime.exec(startVPNCommand);
        runtimeParser = new RuntimeParser(process);
        String connectOutput = runtimeParser.parseOutput();
        logger.logThis().info("checkIfServerIsConnected() connectOutput: " + connectOutput);

        if (isConnected(connectOutput)){
            return true;
        }else{
            logger.logThis().info("ERROR | Error connecting | checkIfServerIsConnected() | isConnected() = false | connect output: " + connectOutput);
            return false;
        }

    }
    /**
     * Checks if connection is established based on the PVPN dedicated app output
     * @param server ServerObj
     * @return Returns whether a connection is established
     * @throws IOException
     */
    private static boolean connectAndCheckIfServerIsConnected(ServerObj server) throws IOException {

        String serverName = server.getServerName();

        String startVPNCommand = "protonvpn-cli c " + serverName;

        //connect to the server
        Process process = runtime.exec(startVPNCommand);
        runtimeParser = new RuntimeParser(process);
        String connectOutput = runtimeParser.parseOutput();
        logger.logThis().info("checkIfServerIsConnected() connectOutput: " + connectOutput);

        if (isConnected(connectOutput)){
            return true;
        }else{
            logger.logThis().info("ERROR | Error connecting | checkIfServerIsConnected() | isConnected() = false | connect output: " + connectOutput);
            return false;
        }

    }

    /**
     *
     * @param site Website URL
     * @param writer Shared BufferedWriter used to generate the report
     * @param server ServerObj
     * @throws IOException
     */

    private static void checkReturnCode(String site, BufferedWriter writer, ServerObj server) throws IOException {
        logger.logThis().info("checkReturnCode() started: ");
        String siteAndProt = "http://"+site;
        logger.logThis().info("checkReturnCode() siteParam: " + site + " siteAndProt: " + siteAndProt);

        URL url = null;

        int responseCode = -1;

        try {

            url = new URL(siteAndProt);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.connect();

            responseCode = connection.getResponseCode();
            logger.logThis().info("checkReturnCode() | response code is: " + responseCode);

            writer.write("\n==============================\n");
            System.out.println("\n==============================\n");
            writer.write("HTTP status code: " + responseCode);
            writer.write("\n");
            System.out.println("HTTP status code: " + responseCode);

            if (responseCode == 200){
                writer.write("Able to reach the site with server: " + server.getServerName());
                writer.write("\n");
                System.out.println("Able to reach the site with server: " + server.getServerName());
            }else {
                if (responseCode == 403) {
                    writer.write("Is not able to reach the site with server: " + server.getServerName());
                    writer.write("\n");
                    System.out.println("Is not able to reach the site with server: " + server.getServerName());
                }
                if (responseCode == 301 || responseCode == 302){
                    String redirectedUrl = connection.getHeaderField("Location");
                    writer.write("Redirect: (301 || 302). " + siteAndProt + "\n");
                    writer.write("Redirected to: " + redirectedUrl);
                    writer.write("\n");
                    System.out.println("Redirect: (301 || 302). " + siteAndProt);
                    System.out.println("Redirect URL: " + redirectedUrl);
                }

                if (responseCode == 502){
                    writer.write("Didn't reach the site. Bad Gateway: " + siteAndProt);
                    writer.write("\n");
                    System.out.println("Didn't reach the site. Bad Gateway: " + siteAndProt);
                }
            }
            System.out.println("\n==============================\n");
            writer.write("\n==============================\n");
            writer.flush();
        }catch (MalformedURLException e){
            e.printStackTrace();
            logger.logThis().info("ERROR | checkReturnCode() MalformedException : Begin of stacktrace:\n");
            writer.write("checkReturnCode | Malformed Exception | " + e);
        }catch (SocketTimeoutException e){
            e.printStackTrace();
            logger.logThis().info("ERROR | checkReturnCode() SocketTimeoutException : Begin of stacktrace:\n");
            writer.write("checkReturnCode | SocketTimeoutException | " + e + "\n");
        }catch (IOException e){
            e.printStackTrace();
            logger.logThis().info("ERROR | checkReturnCode() IOException  : Begin of stacktrace:\n");
            String stackTrace = String.valueOf(e.getStackTrace());
            logger.logThis().log(Level.SEVERE, stackTrace);
            writer.write("checkReturnCode | IOException Exception | " + e + "\n");
        }


    }

    /**
     * Checks if site resolves when connected to a server. Reference Cloudflare/Google DNS/s
     * @throws Exception
     */
    private static void checkSiteResolves() throws Exception {

        boolean localRun = true;
        Scanner scanner = new Scanner(System.in);
        String server = "";
        String siteToCheck = "";

        logger.logThis().info("checkSiteResolves() started: ");

        System.out.println("\nYou chose (Check if site resolves while connected to a server/s): Pick option: ");

        while (localRun){

            System.out.println("\nPlease enter the server you would like to test e.g. (CH#14) " + "or \"b\" to go back: ");
            server = scanner.nextLine();

            if (server.equals("b")){
                localRun = false;
            }

            if (serverIsValid(server)){

                String connectStr = "protonvpn-cli c " + server;
                Process process = runtime.exec(connectStr);
                runtimeParser = new RuntimeParser(process);
                String connectOutput = runtimeParser.parseOutput();

                if (isConnected(connectOutput)){
                    logger.logThis().info("checkSiteResolves() | isConnected() = true connect output: " + connectOutput);
                }else{
                    logger.logThis().info("ERROR | Error connecting | checkSiteResolves() | isConnected() = false | connect output: " + connectOutput);
                    return;
                }

                boolean validWebsiteUrl = false;
                String websiteURL = "";

                while (validWebsiteUrl == false){

                    System.out.println("\nPlease enter a website to check: e.g. www.google.com: " + "or \"q\" to quit: ");
                    websiteURL = scanner.nextLine();

                    if (websiteURL.equals("q")){
                        System.out.println("Returning\n");
                        return;
                    }


                    if (checkValidWebUrl(websiteURL)){

                        String cloudFlareDNS = "1.1.1.1";
                        String googlesDNS = "8.8.8.8";
                        //custom DNS

                        //Cloudflare
                        String commandCloudFlare = "nslookup " + websiteURL + " " + cloudFlareDNS;
                        Process outputCloudFlare = runtime.exec(commandCloudFlare);
                        runtimeParser = new RuntimeParser(outputCloudFlare);
                        String loggedOutputCloudFlare = runtimeParser.parseOutput();
                        logger.logThis().info("======================================");
                        logger.logThis().info("checkSiteResolves() | checkValidWebUrl() = true | loggedOutput: " + loggedOutputCloudFlare);
                        System.out.println("======================================");


                        //Google
                        String commandGoogle = "nslookup " + websiteURL + " " + googlesDNS;
                        Process outputGoogle = runtime.exec(commandGoogle);
                        runtimeParser = new RuntimeParser(outputGoogle);
                        String loggedOutputGoogle = runtimeParser.parseOutput();
                        logger.logThis().info("======================================");
                        logger.logThis().info("checkSiteResolves() | checkValidWebUrl() = true | loggedOutput: " + loggedOutputGoogle);
                        System.out.println("======================================");

                        //PVPN DNS
                        String command = "nslookup " + websiteURL;
                        Process output = runtime.exec(command);
                        runtimeParser = new RuntimeParser(output);
                        String loggedOutput = runtimeParser.parseOutput();
                        logger.logThis().info("checkSiteResolves() | checkValidWebUrl() = true | loggedOutput: " + loggedOutput);
                        System.out.println("======================================");

                        disconnectVPN(runtimeParser);

                        localRun = false;
                        break;
                    }else{
                        System.out.println("Invalid website, please enter website again: ");
                        logger.logThis().log(Level.SEVERE,"checkSiteResolves() Invalid website entered: ");
                    }
                }
            }else{
                if (server.equals("b")) {
                    System.out.println("Returning\n");
                    disconnectVPN(runtimeParser);
                }else{
                    System.out.println("Invalid server, please enter server again: ");
                    logger.logThis().log(Level.SEVERE,"checkSiteResolves() Invalid server entered: ");
                }
            }
        }
    }

    private static void disconnectVPN(RuntimeParser runtimeParser) throws IOException {
        Process process = null;

        try{
            process = runtime.exec("protonvpn-cli d");
        }catch (RuntimeException | IOException e){
            logger.logThis().info("ERROR | RuntimeException | IOException | disconnectVPN()" + e.getStackTrace());
        }

        runtimeParser = new RuntimeParser(process);
        String disconnectVPNOutput = runtimeParser.parseOutput();
        logger.logThis().info("disconnectVPN() | VPN is: " + disconnectVPNOutput);

    }

    private static boolean isConnected(String connectOutput) {
        //No active Proton VPN connection. protonvpn-cli s -> Disconnected

        if (connectOutput.matches("Disconnected") || connectOutput.matches(
                "Setting up Proton VPN.\n" +
                "\n" +
                "No server could be found with the provided servername.\n" +
                "Either the server is under maintenance or\n" +
                "you don't have access to it with your plan.\n" +
                "If you've recently upgraded your plan, please re-login.\n")){
        logger.logThis().info("INSIDE isConnected() | Not connected, about to return false");
            return  false;
        }
        logger.logThis().info("INSIDE isConnected() | Connected, about to return return true");
        return true;

    }

    private static boolean checkValidWebUrl(String websiteURL) {

        if (websiteURL.matches("[-a-zA-Z0-9@:%._\\+~#=]{1,45}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)")){
            return true;
        }

        return false;
    }

    private static boolean serverIsValid(String server) throws Exception {

        if (server.matches("[U]??[S]??[-]??[A-Z]{2}[#]{1}[1-9]{1}[0-9]?") || server.matches("(US|JP|NL)-FREE#[0-9]{6}") || server.matches("(US-)??[A-Z]{2}#[1-9]{1}[0-9]??-TOR")){
            return true;
        }else {
            logger.logThis().log(Level.SEVERE,"Invalid server. Regex exception");
            return false;
        }
    }

    private static String sanitize(String str) {

        logger.logThis().info("sanitize | str:  " + str);

        if (str.length() > 1){
            return "-1";
        }
        if (str.length() == 1 && str.equals("1") || str.equals("2") || str.equals("3")){
            logger.logThis().info("sanitize | str is valid  " + str);
            return str;
        }else{
            return "-1";
        }
    }

    private static Path createFolder(){
        String localDir = System.getProperty("user.dir");
        Path dirPath = Paths.get(localDir + "/output/");
        File dir = new File(dirPath.toString());
        if (!dir.exists()){
            dir.mkdir();
        }

        return dirPath.toAbsolutePath();

    }


}
