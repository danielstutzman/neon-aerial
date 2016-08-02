import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class ReadBSQ {
  public static void main(String[] argv) throws java.io.IOException {
    if (argv.length < 1) {
      System.err.println("First arg should be path to BSQ .dat file");
      System.exit(1);
    }
    File path = new File(argv[0]);

    int inputWidth = 0;
    int inputHeight = 0;
    File hdrFile = new File(path.getAbsolutePath().replace(".dat", ".hdr"));
    BufferedReader stream =
      new BufferedReader(new InputStreamReader(new FileInputStream(hdrFile)));
    while (true) {
      String line = stream.readLine();
      if (line == null) {
        break;
      }

      if (line.startsWith("samples")) {
        inputWidth = Integer.parseInt(line.split(" = ")[1]);
      } else if (line.startsWith("lines")) {
        inputHeight = Integer.parseInt(line.split(" = ")[1]);
      }
    }

    double[] input = new double[inputWidth * inputHeight];
    FileInputStream is = new FileInputStream(path);
    byte bytes[] = new byte[1024];
    int read;
    int i = 0;
    while((i < inputWidth * inputHeight - bytes.length) &&
        (read = is.read(bytes)) != -1){
      DoubleBuffer doubles =
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer();
      doubles.get(input, i, read / 8);
      i += read / 8;
    }

    byte[] aByteArray = new byte[inputWidth * inputHeight * 4];
    DataBuffer buffer = new DataBufferByte(aByteArray, aByteArray.length);
    for (i = 0; i < inputWidth * inputHeight; i++) {
      double level = input[i];
      byte out;
      if (level <= 0.0) {
        out = (byte)0xff;
      } else if (level >= 1.0) {
        out = 0x00;
      } else {
        out = (byte)((1.0 - level) * 255);
      }

      aByteArray[i * 4 + 0] = out; // red
      aByteArray[i * 4 + 1] = out; // green
      aByteArray[i * 4 + 2] = out; // blue
      aByteArray[i * 4 + 3] = (level > 0.0) ? (byte)255 : 0; // alpha
    }

    //3 bytes per pixel: red, green, blue
    WritableRaster raster = Raster.createInterleavedRaster(
      buffer, inputWidth, inputHeight, 4 * inputWidth, 4, new int[] {0, 1, 2, 3},
      (Point)null);
    ColorModel cm = new ComponentColorModel(
      ColorModel.getRGBdefault().getColorSpace(),
      true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
    BufferedImage image = new BufferedImage(cm, raster, true, null);

    BufferedImage out = new BufferedImage(
      (inputWidth + 9) / 10, (inputHeight + 9) / 10, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = out.createGraphics();
    AffineTransform transform = AffineTransform.getScaleInstance(0.1, 0.1);
    g.drawRenderedImage(image, transform);
    ImageIO.write(out, "png", new File(path.getName() + ".png"));
  }
}
