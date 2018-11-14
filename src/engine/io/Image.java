package engine.io;

import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_info_from_memory;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;

import engine.util.IOUtil;
import lwjgui.Color;

public class Image {
	private ByteBuffer image;
	private int[] imageArray;
	private int w = -1;
	private int h = -1;
	private int comp;
	private boolean resized;
	
	public Image( String imagePath ) {
		this( imagePath, false );
	}

	public Image(String imagePath,  boolean flipY) {
		//STBImage.nstbi_set_flip_vertically_on_load(1);
		ByteBuffer imageBuffer;
		try {
			imageBuffer = IOUtil.ioResourceToByteBuffer(imagePath, 4 * 1024);
		} catch (IOException e) {
			System.err.println("Image " + imagePath + " not found!");
			image = null;
			imageBuffer = null;
		}

		if ( imageBuffer == null )
			return;

		IntBuffer w = BufferUtils.createIntBuffer(1);
		IntBuffer h = BufferUtils.createIntBuffer(1);
		IntBuffer comp = BufferUtils.createIntBuffer(1);
		
		// FLIP
		STBImage.stbi_set_flip_vertically_on_load(flipY);

		// Use info to read image metadata without decoding the entire image.
		// We don't need this for this demo, just testing the API.
		if ( !stbi_info_from_memory(imageBuffer, w, h, comp) )
			throw new RuntimeException("Image " + imagePath + " Failed to read image information: " + stbi_failure_reason());

		/*System.out.println("Image width: " + w.get(0));
		System.out.println("Image height: " + h.get(0));
		System.out.println("Image components: " + comp.get(0));
		System.out.println("Image HDR: " + stbi_is_hdr_from_memory(imageBuffer));*/

		// Decode the image
		ByteBuffer data = stbi_load_from_memory(imageBuffer, w, h, comp, 0);
		setData( data );

		if ( image == null ) {
			System.err.println("Failed to load image: " + stbi_failure_reason());
			image = null;
			return;
		}

		this.w = w.get(0);
		this.h = h.get(0);
		this.comp = comp.get(0);
	}

	public Image(ByteBuffer imageData, int width, int height) {
		this.setData( imageData );
		this.w = width;
		this.h = height;

		this.comp = imageData.capacity() / ( width * height );

	}

	public Image(Color color, int width, int height) {
		this.comp = 4;
		ByteBuffer data = BufferUtils.createByteBuffer( width*height*comp );
		this.w = width;
		this.h = height;

		byte rr = (byte) (color.getRed() & 0xff);
		byte gg = (byte) (color.getGreen() & 0xff);
		byte bb = (byte) (color.getBlue() & 0xff);
		byte aa = (byte) (255 & 0xff);
		for (int i = 0; i < width*height; i++) {
			data.put(rr);
			data.put(gg);
			data.put(bb);
			data.put(aa);
		}
		data.flip();

		this.setData( data );
	}

	public void resize(int width, int height) {
		if ( getData() == null )
			return;

		long start = System.currentTimeMillis();
		ByteBuffer newImage = BufferUtils.createByteBuffer( width * height * this.comp );
		int alpha = (comp == 4)?3:STBImageResize.STBIR_ALPHA_CHANNEL_NONE;

		STBImageResize.stbir_resize(
		        image, //BUFFER LOADED FROM stbi_load_from_memory
		        this.w,
		        this.h,
		        this.w*comp,
		        newImage,
		        width,
		        height,
		        width*comp,
		        STBImageResize.STBIR_TYPE_UINT8,
		        comp,
		        alpha,
		        0,
		        STBImageResize.STBIR_EDGE_WRAP,
		        STBImageResize.STBIR_EDGE_WRAP,
		        STBImageResize.STBIR_FILTER_CUBICBSPLINE,
		        STBImageResize.STBIR_FILTER_CUBICBSPLINE,
		        STBImageResize.STBIR_COLORSPACE_LINEAR);

		if ( !resized ) {
			resized = true;
			STBImage.stbi_image_free(image);
		}

		this.setData( newImage );

		this.w = width;
		this.h = height;
		System.out.println("Image resized in " + (System.currentTimeMillis() - start) + " ms");
	}

	public void free() {
		imageArray = null;
		w = -1;
		h = -1;
		comp = -1;
		
		if ( image == null )
			return;
		
		IOUtil.freeBuffer(image);
		image = null;
	}

	public int getWidth() {
		return this.w;
	}

	public int getHeight() {
		return this.h;
	}

	public ByteBuffer getData() {
		return this.image;
	}

	public int getComponents() {
		return this.comp;
	}

	public int[] getDataArray() {
		if ( image == null )
			return null;

		if ( imageArray == null ) {
			image.rewind();

			imageArray = new int[image.capacity() / comp];
			for (int i = 0; i < imageArray.length; i++) {
				int[] color_a = new int[4];

				// Get RGBA components
				color_a[3] = 255 & 0xFF;
				for (int j = 0; j < comp; j++)
					color_a[j] = image.get() & 0xFF;

				// Store to array
				imageArray[i] = (color_a[3] << 24) | (color_a[0] << 16) | (color_a[1] << 8) | (color_a[2] << 0);
			}
			image.rewind();
		}

		return imageArray;
	}

	private void setData(ByteBuffer data) {
		this.image = data;
		this.imageArray = null;
	}

	public boolean loaded() {
		return w > -1 && h > -1;
	}
}