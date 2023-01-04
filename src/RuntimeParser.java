import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * Class used for parsing the output generated from PVPN
 */
public class RuntimeParser {

    private BufferedReader stdInput;
    private Process process;

    public RuntimeParser(Process process) {
        this.process = process;
        this.stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    public String parseOutput() throws IOException {
        String s = "";
        StringBuilder builder = new StringBuilder();

        while ((s = stdInput.readLine()) != null) {
            builder.append(s);
            builder.append("\n");
        }
        System.out.println(builder.toString());
        return builder.toString();
    }


}
