import java.awt.Point;
import java.awt.Transparency;
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
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class ReadBSQ {
  public static void main(String[] argv) throws java.io.IOException {
    int width = 908;
    int height = 11504;

    String path = "NIS1_20140603_160543_atmcor_engL2VI.bsq";
    double[] input = new double[width * height];
    FileInputStream is = new FileInputStream(path);
    byte bytes[] = new byte[1024];
    int read;
    int i = 0;
    while((i < width * height - bytes.length) && (read = is.read(bytes)) != -1){
      DoubleBuffer doubles =
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer();
      doubles.get(input, i, read / 8);
      i += read / 8;
    }

    byte[] aByteArray = new byte[width * height * 3];
    DataBuffer buffer = new DataBufferByte(aByteArray, aByteArray.length);
    for (i = 0; i < width * height; i++) {
      double level = input[i];
      byte out;
      if (level <= 0.0) {
        out = 0x00;
      } else if (level >= 1.0) {
        out = (byte)0xff;
      } else {
        out = (byte)(level * 255);
      }

      aByteArray[i * 3] = out;
    }

    //3 bytes per pixel: red, green, blue
    WritableRaster raster = Raster.createInterleavedRaster(
      buffer, width, height, 3 * width, 3, new int[] {0, 1, 2}, (Point)null);
    ColorModel cm = new ComponentColorModel(
      ColorModel.getRGBdefault().getColorSpace(),
      false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE); 
    BufferedImage image = new BufferedImage(cm, raster, true, null);
    
    ImageIO.write(image, "png", new File("image.png"));
  }
}
