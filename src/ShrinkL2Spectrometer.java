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
import java.nio.FloatBuffer;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class ShrinkL2Spectrometer {
  // e.g. { 0, -1, -1, 1 } means:
  //  1st layer should become red channel
  //  2nd layer should be ignored
  //  3rd layer should be ignored
  //  4th layer should become green channel
  // Must include 1st layer since it's used for alpha
  private static final int[] LAYER_TO_COLOR = new int[]{ 0, -1, -1, 1 };

  public static void main(String[] argv) throws java.io.IOException {
    // Don't pop up Java dock icon just because we're using AWT classes
    System.setProperty("java.awt.headless", "true");

    if (argv.length < 2) {
      System.err.println("First arg should be directory to output to");
      System.err.println("2nd-nth args should be paths to BSQ .dat files");
      System.exit(1);
    }
    File outputDir = new File(argv[0]);
    for (int i = 1; i < argv.length; i++) {
      File inputPath = new File(argv[i]);
      File hdrFile = new File(inputPath.getAbsolutePath().replace(".dat", ".hdr"));
      File outputPath = new File(outputDir, inputPath.getName() + ".png");
      if (!hdrFile.exists()) {
        System.err.println("Doesn't exist: " + hdrFile);
      } else if (outputPath.exists()) {
        System.err.println("Already exists: " + inputPath);
      } else {
        shrinkL2Spectrometer(inputPath, hdrFile, outputPath);
      }
    }
  }

  private static void shrinkL2Spectrometer(
      File inputPath, File hdrFile, File outputPath) throws java.io.IOException {
    int inputWidth = 0;
    int inputHeight = 0;
    int dataType = 0;
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
      } else if (line.startsWith("data type")) {
        dataType = Integer.parseInt(line.split(" = ")[1]);
      }
    }

    byte[] aByteArray;
    if (dataType == 4) { // 4 = 32-bit single-precision floating-point
      aByteArray = readFloats(inputPath, inputWidth, inputHeight);
    } else if (dataType == 5) { // 5 = 64-bit double-precision floating-point
      aByteArray = readDoubles(inputPath, inputWidth, inputHeight);
    } else {
      throw new RuntimeException("Don't know how to read data type = " + dataType);
    }

    DataBuffer buffer = new DataBufferByte(aByteArray, aByteArray.length);
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
    ImageIO.write(out, "png", outputPath);
    System.err.println(outputPath);
  }

  static byte[] readFloats(File inputPath, int inputWidth, int inputHeight)
      throws java.io.IOException {
    FileInputStream is = new FileInputStream(inputPath);
    int numFloatsReadTotal = 0;
    byte[] aByteArray = new byte[inputWidth * inputHeight * 4];
    for (int layer = 0; layer < LAYER_TO_COLOR.length; layer++) {
      float[] input = new float[inputWidth * inputHeight];
      byte bytes[] = new byte[1024];
      int read;
      while((read = is.read(bytes)) != -1) {
        FloatBuffer floats =
          ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        if (numFloatsReadTotal + read/4 >=
            inputWidth * inputHeight * (layer + 1)) {
          numFloatsReadTotal += read / 4;
          break;
        }
        floats.get(input, numFloatsReadTotal - (inputWidth * inputHeight * layer),
          read / 4);
        numFloatsReadTotal += read / 4;
      }

      float minLevel = Float.POSITIVE_INFINITY;
      float maxLevel = Float.NEGATIVE_INFINITY;
      if (LAYER_TO_COLOR[layer] != -1) {
        for (int i = 0; i < inputWidth * inputHeight; i++) {
          float level = input[i];
          if (level < minLevel && level != Float.NEGATIVE_INFINITY) {
            minLevel = level;
          }
          if (level > maxLevel && level != Float.POSITIVE_INFINITY) {
            maxLevel = level;
          }

          if (layer == 3) {
            level = level / -0.1f;
          }

          byte out;
          if (level <= 0.0) {
            out = (byte)0xff;
          } else if (level >= 1.0) {
            out = 0x00;
          } else {
            out = (byte)((1.0 - level) * 255);
          }

          aByteArray[i * 4 + LAYER_TO_COLOR[layer]] = out;
          if (layer == 0) {
            aByteArray[i * 4 + 3] = (level > 0.0) ? (byte)255 : 0; // alpha
          }
        }
      }
      System.out.println(
        "  layer " + layer + ", min=" + minLevel + ", max=" + maxLevel);
    }
    return aByteArray;
  }

  static byte[] readDoubles(File inputPath, int inputWidth, int inputHeight)
      throws java.io.IOException {
    FileInputStream is = new FileInputStream(inputPath);
    int numDoublesReadTotal = 0;
    byte[] aByteArray = new byte[inputWidth * inputHeight * 4];
    for (int layer = 0; layer < LAYER_TO_COLOR.length; layer++) {
      System.out.println("layer:" + layer);
      double[] input = new double[inputWidth * inputHeight];
      byte bytes[] = new byte[1024];
      int read;
      while((read = is.read(bytes)) != -1) {
        DoubleBuffer doubles =
          ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer();
        if (numDoublesReadTotal + read/8 >=
            inputWidth * inputHeight * (layer + 1)) {
          numDoublesReadTotal += read / 8;
          break;
        }
        doubles.get(input, numDoublesReadTotal - (inputWidth * inputHeight * layer),
          read / 8);
        numDoublesReadTotal += read / 8;
      }

      if (LAYER_TO_COLOR[layer] != -1) {
        for (int i = 0; i < inputWidth * inputHeight; i++) {
          double level = input[i];
          if (layer == 3) {
            level = level / -0.1;
          }

          byte out;
          if (level <= 0.0) {
            out = (byte)0xff;
          } else if (level >= 1.0) {
            out = 0x00;
          } else {
            out = (byte)((1.0 - level) * 255);
          }

          aByteArray[i * 4 + LAYER_TO_COLOR[layer]] = out;
          if (layer == 0) {
            aByteArray[i * 4 + 3] = (level > 0.0) ? (byte)255 : 0; // alpha
          }
        }
      }
    }
    return aByteArray;
  }
}
