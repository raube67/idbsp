package idbsp.drawing;

import java.awt.image.IndexColorModel;

public class ColorTable {

	private static final int[][] table = {
			{ 0, 0, 0},
			{ 0, 0, 0},
			{ 0, 0, 0},
			{ 0, 0, 0},
			{ 0, 0, 0},
			{ 127, 96, 64},
			{ 255, 255, 255 },
			{ 0, 255, 0 }
	};
	
	public static IndexColorModel createIndexColorModel() {
		final byte[] red = new byte[256];
		final byte[] green = new byte[256];
		final byte[] blue = new byte[256];
		
		for (int i = 0; i < table.length; i++) {
			red[i]   = (byte) table[i][0];
			green[i] = (byte) table[i][1];
			blue[i]  = (byte) table[i][2];
		}
		
		return new IndexColorModel(8, 256, red, green, blue);

	}
}
