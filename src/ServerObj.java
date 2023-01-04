public class ServerObj {

    private String serverName;

    public ServerObj(String serverName) {
        this.serverName = serverName;
    }

    public String getServerName() {
        return serverName;
    }

    @Override
    public String toString() {
        return "ServerObj{" +
                "serverName='" + serverName + '\'' +
                '}';
    }

}
