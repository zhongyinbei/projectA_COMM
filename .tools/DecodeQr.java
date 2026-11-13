import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.io.File;

public class DecodeQr {
    public static void main(String[] args) throws Exception {
        var image = ImageIO.read(new File(args[0]));
        var source = new BufferedImageLuminanceSource(image);
        var bitmap = new HybridBinarizer(source);
        var result = new MultiFormatReader().decode(new BinaryBitmap(bitmap));
        System.out.println("QR=" + result.getText() + " WIDTH=" + image.getWidth() + " HEIGHT=" + image.getHeight());
    }
}
