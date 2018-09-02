package idbsp;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import idbsp.drawing.Drawing;
import idbsp.logic.BinarySpacePartitioner;
import idbsp.logic.CommandLineArguments;
import idbsp.logic.DoomMap;
import idbsp.logic.DoomMapLoader;
import idbsp.logic.Utils;
import idbsp.types.NXPoint;
import idbsp.types.bbox_t;
import idbsp.types.bspnode_t;
import idbsp.types.line_t;
import idbsp.types.view_t;
import idbsp.types.worldline_t;

/**
 * Main
 * 
 * @author Agnes
 *
 */
public class Main {

	private CommandLineArguments arguments = new CommandLineArguments();
	private DoomMapLoader doomMapLoader = new DoomMapLoader();
	private BinarySpacePartitioner partitioner = new BinarySpacePartitioner();
	private Drawing drawing = new Drawing();
	
	private DoomMap doomMap;
	
	
	public static void main(String[] args) {
		try {
			Main main = new Main();
			
			main.init(args);
			main.start();
			
			System.err.println("exit");
			
		} catch (Exception _ex) {
			_ex.printStackTrace();
		}
	
	}
	
	private void init(String[] args) throws Exception {
		arguments.parse(args);
		Path path = Paths.get(arguments.getInmapname());
		doomMap = doomMapLoader.load(path);
		
		partitioner.process(doomMap);		
		bbox_t bbox = partitioner.getNode().bbox;
		
		drawing.init(bbox);
		
	}
	
	private void drawBBox(bbox_t bbox, byte c) {
		drawing.drawSegment(bbox.x1, bbox.y1, bbox.x1, bbox.y2, c);
		drawing.drawSegment(bbox.x1, bbox.y2, bbox.x2, bbox.y2, c);
		drawing.drawSegment(bbox.x2, bbox.y2, bbox.x2, bbox.y1, c);
		drawing.drawSegment(bbox.x2, bbox.y1, bbox.x1, bbox.y1, c);
	}

	private void drawLines(List<line_t> lines, byte c) {
		for (line_t line : lines) {
			drawing.drawSegment(line.p1.x, line.p1.y, line.p2.x, line.p2.y, c);
		}
	}
	
	private void drawNode(bspnode_t node, byte c) {
		drawing.restore(0);
		drawBBox(node.bbox, (byte) 7);
		
		if (node.lines_i != null) {
			drawLines(node.lines_i, (byte) 6);
			drawing.update(null);
			Utils.sleep(50);
			
			return;
		}

		drawing.update(null);
		Utils.sleep(50);

		drawNode(node.side[0], (byte) 6);
		drawNode(node.side[1], (byte) 7);
		
	}

	private void start() {
		for (worldline_t line : doomMap.getLineStore()) {
			drawing.drawSegment(line.p1.x, line.p1.y, line.p2.x, line.p2.y, (byte) 5);
		}
		
		view_t view = new view_t();
		view.pt = new NXPoint();
		
		// -2496,-960
//		view.pt.x = -2176;
//		view.pt.y = -2096;
		view.pt.x = -2496;
		view.pt.y = -960;
		
		for (int i = -4; i < 4; i++)
			for (int j = -4; j < 4; j++) 
		drawing.drawSegment(view.pt.x + j, view.pt.y + i, view.pt.x + j, view.pt.y + i, (byte) 6);
		
		drawing.update(null);
		drawing.save();
		
//		drawNode(partitioner.getNode(), (byte) 6);
		
		RenderBSPNode(partitioner.getNode(), view);
		drawing.update(null);
		
	}
	
	private void RenderBSPNode(bspnode_t node, view_t view) {
		if (node.lines_i != null) {
			drawLines(node.lines_i, (byte) 6);
			drawing.update(null);	
			Utils.sleep(1000);
			return;
		}
		
		int side = Utils.PointOnSide(view.pt, node.divline);
		if (side == -1) side = 0;
		System.out.println("\tView side: " + side);
		
		RenderBSPNode(node.side[side], view);
		RenderBSPNode(node.side[side ^ 1], view);
	}
}
