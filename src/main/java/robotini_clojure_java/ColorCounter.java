package robotini_clojure_java;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class ColorCounter {
    public static CountResult count(BufferedImage image, int threshold) {
        int red = 0;
        int green = 0;
        int blue = 0;

        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        for (int i=0; i<data.length; i+= 3) {
            int r = data[i] & 0xff;
            int g = data[i + 1] & 0xff;
            int b = data[i + 2] & 0xff;

            if (r + g + b < threshold) {
                // below threshold
            } else if (r < b && g < b) {
                blue++;
            } else if (r < g && b < g) {
                green++;
            } else if (b < r && g < r) {
                red++;
            }
        }

        return new CountResult(red, green, blue);
    }

    public static class CountResult {
        public CountResult(int red, int green, int blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public int red;
        public int green;
        public int blue;
    }
}
