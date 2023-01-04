import java.io.IOException;
import java.util.List;

public interface ServerFetcher {

    List<ServerObj> getListOfServers() throws IOException;

}
