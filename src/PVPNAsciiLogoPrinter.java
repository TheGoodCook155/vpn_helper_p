import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Prints the PVPN logo as ASCII
 */
public class PVPNAsciiLogoPrinter {

    private static String density = "Ñ@#W$9876543210?!abc;:+=-,._ ";
    private static List<Character> list = new ArrayList<>();

    public PVPNAsciiLogoPrinter() {

    }

    static {

        for (int i = 0; i < density.length(); i++){
            Character character = density.charAt(i);
            list.add(character);
        }
    }

    public void printLogo(){

        StringBuilder sb = new StringBuilder();

        BufferedImage image = null;
        try {
            image = ImageIO.read(new File("protonGray30.png"));
        } catch (IOException e) {

        }

        for (int x = 0; x < image.getHeight(); x++) {
            for (int y = 0; y < image.getWidth(); y++) {
                Color old = new Color(image.getRGB(y,x));

                int redOld = old.getRed();
                int greenOld = old.getBlue();
                int blueOld = old.getBlue();

                double mid = (redOld + greenOld + blueOld) / 3;

                int index = 0;

                index = (int) Math.floor(mid / 9);

                sb.append(list.get(index));
                sb.append(" ");

                if (y == image.getWidth() - 1) {
                    sb.append("\n");
                }
            }
        }
        System.out.println(sb.toString());
    }
}
