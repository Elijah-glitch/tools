package ch.hollenstein.gallerybuilder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Rotation;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

public class GalleryBuilderApp {

	public static void main(String[] args) {

		Properties props = new Properties();

		try (InputStream in = new FileInputStream("src/main/resources/galleryBuilder.properties")) {
			props.load(in);
			new GalleryBuilderApp(props);

		} catch (IOException | ImageProcessingException e) {
			e.printStackTrace();
		}
	}

	public GalleryBuilderApp(Properties props) throws IOException, ImageProcessingException {

		Path pathRoot = Paths.get(props.getProperty("path.root"));
		Path pathImagesRelative = Paths.get(props.getProperty("path.images.relative"));
		Path pathImages = pathRoot.resolve(pathImagesRelative);

		Path pathImagesOriginal = pathImages.resolve("original");
		Path pathImagesLarge = pathImages.resolve("large");
		Path pathImagesSmall = pathImages.resolve("small");
		Path pathImagesThumbs = pathImages.resolve("thumbs");

		int sizeLarge = Integer.parseInt(props.getProperty("image.size.large"));
		int sizeSmall = Integer.parseInt(props.getProperty("image.size.small"));
		int sizeThumb = Integer.parseInt(props.getProperty("image.size.thumb"));

		String filename = props.getProperty("filename.html");
		String title = props.getProperty("title");
		String author = props.getProperty("author");

		String galleryTemplate = readTemplate("galleryTemplate.html");
		String galleryEntryTemplateLarge = readTemplate("galleryEntryTemplateLarge.html");
		String galleryEntryTemplateSmall = readTemplate("galleryEntryTemplateSmall.html");
		String linkEntryTemplate = readTemplate("linkEntryTemplate.html");

		long t0 = System.currentTimeMillis();

		StringBuilder sbEntries = new StringBuilder();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(pathImagesOriginal)) {

			for (Path image : ds) {
				String fileName = image.getFileName().toString();

				BufferedImage img = ImageIO.read(image.toFile());
				int orientation = getOrientation(image.toFile());
				
				String galleryEntry;
				boolean createLargeTemplate = false;
				if (img.getHeight() > (sizeSmall * 1.2) || img.getWidth() > (sizeSmall * 1.2)) {
					galleryEntry = galleryEntryTemplateLarge;
					createLargeTemplate = true;
				} else {
					galleryEntry = galleryEntryTemplateSmall;
				}

				// -------- large --------
				BufferedImage scaledImage = convertImage(img, pathImagesLarge, fileName, sizeLarge, orientation);
				galleryEntry = galleryEntry.replace("{{image_large_path}}",
						createImagePath(pathRoot, pathImagesLarge, fileName));
				galleryEntry = galleryEntry.replace("{{image_actual_width}}", String.valueOf(scaledImage.getWidth()));
				galleryEntry = galleryEntry.replace("{{image_actual_height}}", String.valueOf(scaledImage.getHeight()));
				

				// -------- small --------
				if (createLargeTemplate) {
					scaledImage = convertImage(img, pathImagesSmall, fileName, sizeSmall, orientation);
					galleryEntry = galleryEntry.replace("{{image_small_path}}",
							createImagePath(pathRoot, pathImagesSmall, fileName));
					galleryEntry = galleryEntry.replace("{{image_large_size}}", String.valueOf(sizeLarge));
					galleryEntry = galleryEntry.replace("{{image_small_size}}", String.valueOf(sizeSmall));
				}
				
				// -------- thumbs --------
				convertImage(img, pathImagesThumbs, fileName, sizeThumb, orientation);
				galleryEntry = galleryEntry.replace("{{image_thumb_path}}",
						createImagePath(pathRoot, pathImagesThumbs, fileName));
				
				sbEntries.append(galleryEntry);
				sbEntries.append("\n");
			}

		}
		galleryTemplate = galleryTemplate.replace("{{title}}", title);
		galleryTemplate = galleryTemplate.replace("{{gallery_entries}}", sbEntries.toString());
		galleryTemplate = galleryTemplate.replace("{{author}}", author);

		System.out.println("Creating images took " + (System.currentTimeMillis() - t0) + "ms");

		try (PrintWriter out = new PrintWriter(pathImages.resolve(filename).toString())) {
			out.println(galleryTemplate);
		}
		linkEntryTemplate = linkEntryTemplate.replace("{{title}}", title);
		linkEntryTemplate = linkEntryTemplate.replace("{{link}}",
				pathImagesRelative.resolve(filename).toString().replace("\\", "/"));
		System.out.println(linkEntryTemplate);
	}

	private String createImagePath(Path pathRoot, Path pathImagesLarge, String fileName) {
		Path fullPath = pathImagesLarge.resolve(fileName);
		Path relPath = pathRoot.relativize(fullPath);
		return relPath.toString().replace("\\", "/");
	}

	private String readTemplate(String filename) throws IOException {
		return new String(Files.readAllBytes(Paths.get("src/main/resources/templates").resolve(filename)));
	}

	/**
	 * @see https://www.htmlgoodies.com/beyond/java/create-high-quality-thumbnails-using-the-imgscalr-library.html
	 * 
	 * @param imageOriginal
	 * @param subDir
	 * @param fileName
	 * @param targetSize
	 * @throws IOException
	 * @throws ImageProcessingException
	 */
	private BufferedImage convertImage(BufferedImage imgOrigin, Path subDir, String fileName, int targetSize, int orientation)
			throws IOException, ImageProcessingException {
		System.out.println("Converting image " + fileName + ", size=" + imgOrigin.getWidth() + "x" + imgOrigin.getHeight() + ", targetSize=" + targetSize);
		BufferedImage image = imgOrigin;
		
		if (targetSize < image.getHeight() || targetSize < image.getWidth()) {
			image = Scalr.resize(image, Method.ULTRA_QUALITY, targetSize);
		}
		
		if (orientation == 8) {
			image = Scalr.rotate(image, Rotation.CW_270);
		} else if (orientation == 3) {
			image = Scalr.rotate(image, Rotation.CW_180);
		} else if (orientation == 6) {
			image = Scalr.rotate(image, Rotation.CW_90);
		}
		File dir = subDir.toFile();
		if (!dir.exists()) {
			dir.mkdir();
		}
		ImageIO.write(image, "jpg", new File(dir, fileName));
		return image;
	}

	/**
	 * @see https://www.impulseadventure.com/photo/exif-orientation.html
	 * 
	 * @param image
	 * @return
	 * @throws ImageProcessingException
	 * @throws IOException
	 */
	private int getOrientation(File image) throws ImageProcessingException, IOException {
		Metadata metadata = ImageMetadataReader.readMetadata(image);
		ExifIFD0Directory exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
		if (exifDirectory != null) {
			String orientation = exifDirectory.getString(ExifIFD0Directory.TAG_ORIENTATION);
			if (orientation != null) {
				try {
					return Integer.parseInt(orientation);
				} catch (NumberFormatException e) {
					System.out.println("orientation is not a number: " + orientation);
				}
			}
		}
		return 1;
	}

}
