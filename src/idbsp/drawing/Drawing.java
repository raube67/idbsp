package idbsp.drawing;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import idbsp.logic.Utils;
import idbsp.types.bbox_t;

public class Drawing extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String MOCHA_DOOM_TITLE = "Mocha Doom Alpha 1.6";
	public static final int BORDER = 32;

    protected GraphicsDevice device;
    
    protected Dimension size;
    protected Image screen;
    protected byte[] data;
	protected Canvas drawhere;
    protected Graphics2D g2d;

	private double fac;
	private double off_x, off_y;
	
	private List<byte[]> saved = new ArrayList<>();

	
	public void init(bbox_t bbox) throws Exception {
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = env.getScreenDevices();
        
        // Get device 0, because we're lazy.
		if (devices == null || devices.length == 0) {
			throw new IllegalStateException("No graphics device available");
		}
		
		device = devices[0];
		
		DisplayModePicker dmp = new DisplayModePicker(device);
		DisplayMode dm = dmp.pickLargest();
		
		size = new Dimension(dm.getWidth(), dm.getHeight());
		System.out.println(String.format("%d %d display mode", size.width, size.height));
		
		double fx = (size.width - 2 * BORDER) / (bbox.x2 - bbox.x1);
		double fy = (size.height - 2 * BORDER) / (bbox.y2 - bbox.y1);
		
		fac = Math.min(fx, fy);
		off_x = (bbox.x2 + bbox.x1) / 2;
		off_y = (bbox.y2 + bbox.y1) / 2;
		
		IndexColorModel icm = ColorTable.createIndexColorModel();
		WritableRaster wr = icm.createCompatibleWritableRaster(size.width, size.height);
		data = ((DataBufferByte) wr.getDataBuffer()).getData();
		screen = new BufferedImage(icm, wr, false, null);

		boolean isFullScreen = device.isFullScreenSupported();
		setUndecorated(isFullScreen);
		setResizable(!isFullScreen);
		
		drawhere = new Canvas();	
//		drawhere.setCursor(createInvisibleCursor());

		if (isFullScreen) {
			// Full-screen mode
			device.setFullScreenWindow(this);
			if (device.isDisplayChangeSupported()) {
				device.setDisplayMode(dm);
			}
			validate();
			setCanvasSize(size);

		} else {
			// Windowed mode
			pack();
			setVisible(true);
		}

		this.add(drawhere);
		this.getContentPane().setPreferredSize(drawhere.getPreferredSize());

		this.pack();
		this.setVisible(true);
		this.setResizable(false);
		this.setTitle(MOCHA_DOOM_TITLE);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		this.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				// 
			}
			@Override
			public void keyReleased(KeyEvent e) {
				// 
			}
			@Override
			public void keyPressed(KeyEvent e) {
				dispose();
				System.exit(1);
			}
		});

		// Gently tell the eventhandler to wake up and set itself.
		this.requestFocus();
		Utils.sleep(100); 
		
//		this.update(null);

	}
	

	   /** Modified update method: no context needs to passed.
     *  Will render only internal screens. Common between AWT 
     *  and Swing
     * 
     */
    public void paint(Graphics g) {
		if (g2d == null) {
			g2d = (Graphics2D) drawhere.getGraphics();
		}
		if (g2d != null) {
			g2d.drawImage(screen, 0, 0, this);
		}
    }
    
    private void setCanvasSize(Dimension size) {
        drawhere.setPreferredSize(size);
        drawhere.setBounds(0, 0, drawhere.getWidth()-1,drawhere.getHeight()-1);
        drawhere.setBackground(Color.black);
    }
	
    public void plot(int x, int y, byte c) {
    	if (x < 0 || x >= size.width || y < 0 || y >= size.height) return;
    	data[y * size.width + x] = c;
    }
    
    public void drawLine(int x1, int y1, int x2, int y2, byte c) {
        // delta of exact value and rounded value of the dependent variable
        int d = 0;
 
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
 
        int dx2 = 2 * dx; // slope scaling factors to
        int dy2 = 2 * dy; // avoid floating point
 
        int ix = x1 < x2 ? 1 : -1; // increment direction
        int iy = y1 < y2 ? 1 : -1;
 
        int x = x1;
        int y = y1;
 
        if (dx >= dy) {
            while (true) {
                plot(x, y, c);
                if (x == x2)
                    break;
                x += ix;
                d += dy2;
                if (d > dx) {
                    y += iy;
                    d -= dx2;
                }
            }
        } else {
            while (true) {
                plot(x, y, c);
                if (y == y2)
                    break;
                y += iy;
                d += dx2;
                if (d > dy) {
                    x += ix;
                    d -= dy2;
                }
            }
        }
    }
    
    public void drawSegment(double x1, double y1, double x2, double y2, byte c) {
    	drawLine(
    			(int) ((size.width / 2) + fac * (x1 - off_x)),
    			(int) ((size.height / 2) - fac * (y1 - off_y)),
    			(int) ((size.width / 2) + fac * (x2 - off_x)),
    			(int) ((size.height / 2) - fac * (y2 - off_y)),
    			c
    			);
    }
    
    public int save() {
    	byte[] temp = new byte[data.length];
    	System.arraycopy(data, 0, temp, 0, data.length);
    	saved.add(temp);
    	return saved.size() - 1;
    }
    
    public void restore(int index) {
    	byte[] temp = saved.get(index);
    	System.arraycopy(temp, 0, data, 0, data.length);
    }
} 
