package idbsp.logic;

import static idbsp.logic.Constants.ML_TWOSIDED;

import java.util.ArrayList;
import java.util.List;

import idbsp.types.NXPoint;
import idbsp.types.bbox_t;
import idbsp.types.bspnode_t;
import idbsp.types.divline_t;
import idbsp.types.line_t;
import idbsp.types.worldline_t;

/**
 * BinarySpacePartitioner
 * 
 * @author Agnes
 *
 */
public class BinarySpacePartitioner {

	private List<line_t> segmentsStore = new ArrayList<>();
	private int cuts = 0;
	private bspnode_t node;

	
	public List<line_t> getSegmentsStore() {
		return segmentsStore;
	}

	public int getCuts() {
		return cuts;
	}

	public bspnode_t getNode() {
		return node;
	}

	public void process(DoomMap doomMap) {
		makeSegments(doomMap);
		node = processList(segmentsStore);
		System.out.println(cuts + " cuts");
	}
	
	private void makeSegments(DoomMap doomMap) {
		for (int i = 0; i < doomMap.getLineStore().size(); i++) {
			worldline_t wl = doomMap.getLineStore().get(i);
			
			line_t li = new line_t();
			li.p1 = wl.p1;
			li.p2 = wl.p2;
			li.linedef = wl;
			li.side = 0;
			li.offset = 0;
			li.grouped = false;
			
			segmentsStore.add(li);
			
			if ((wl.flags & ML_TWOSIDED) == 0) {
				continue;
			}
			
			li = new line_t();
			li.p1 = wl.p2;
			li.p2 = wl.p1;
			li.linedef = wl;
			li.side = 1;
			li.offset = 0;
			li.grouped = false;
			
			segmentsStore.add(li);
		}
		
		System.out.println(segmentsStore.size() + " segments");

	}
	
	private bspnode_t processList(List<line_t> lines_i) {
		
		bspnode_t node_p = new bspnode_t();
		node_p.divline = new divline_t();
		node_p.bbox = computeBBox(lines_i);
		
		//
		// find the best line to partition on 
		//
		int c = lines_i.size();
		int grade = Integer.MAX_VALUE;	
		line_t bestline_p = null;
		int step = 1; // (c / 40) + 1;		// set this to 1 for an exhaustive search
		while (grade == Integer.MAX_VALUE) {
			for (int i = 0 ; i < c ; i += step) {
				line_t line_p = lines_i.get(i);
				int v = EvaluateSplit (lines_i, line_p, grade);
				if (v < grade) {
					grade = v;
					bestline_p = line_p;
				}
			}
			
			if (grade == Integer.MAX_VALUE) {
				if (step > 1) {	// possible to get here with non convex area if BSPSLIDE specials caused rejections
					step = 1;
					continue;
				}
				node_p.lines_i = lines_i;
				return node_p;
			}
		}		
		
		//
		// if none of the lines should be split, the remaining lines
		// are convex, and form a terminal node
		//

		//
		// divide the line list into two nodes along the best split line
		//
		node_p.divline = DivlineFromWorldline(bestline_p);

		List<line_t> frontlist_i = new ArrayList<>();
		List<line_t> backlist_i = new ArrayList<>();
		
		ExecuteSplit (lines_i, bestline_p, frontlist_i, backlist_i);

		//
		// recursively divide the lists
		//
		node_p.side = new bspnode_t[2];
		node_p.side[0] = processList(frontlist_i);
		node_p.side[1] = processList(backlist_i);
			
		return node_p;
		
	}
	
	
	private bbox_t computeBBox(List<line_t> lines_i) {
		bbox_t bbox = new bbox_t();
		
		bbox.x1 = Double.MAX_VALUE;
		bbox.x2 = -Double.MAX_VALUE;
		bbox.y1 = Double.MAX_VALUE;
		bbox.y2 = -Double.MAX_VALUE;
		
		for (line_t line : lines_i) {
			bbox.x1 = Math.min(bbox.x1, line.p1.x);
			bbox.x2 = Math.max(bbox.x2, line.p1.x);
			bbox.x1 = Math.min(bbox.x1, line.p2.x);
			bbox.x2 = Math.max(bbox.x2, line.p2.x);
			bbox.y1 = Math.min(bbox.y1, line.p1.y);
			bbox.y2 = Math.max(bbox.y2, line.p1.y);
			bbox.y1 = Math.min(bbox.y1, line.p2.y);
			bbox.y2 = Math.max(bbox.y2, line.p2.y);
		}
		
		return bbox;
	}
	
	/*
	================
	=
	= EvaluateSplit
	=
	= Returns a number grading the quality of a split along the given line
	= for the current list of lines.  Evaluation is halted as soon as it is
	= determined that a better split already exists
	= 
	= A split is good if it divides the lines evenly without cutting many lines
	= A horizontal or vertical split is better than a sloping split
	=
	= The LOWER the returned value, the better.  If the split line does not divide
	= any of the lines at all, MAXINT will be returned
	================
	*/

	private int EvaluateSplit(List<line_t> lines_i, line_t spliton, int worstgrade) {
		int frontcount = 0, backcount = 0, grade = 0;
		divline_t divline = DivlineFromWorldline(spliton);
		
		for (int i = 0 ; i < lines_i.size(); i++) {
			line_t line_p = lines_i.get(i);
			int side = (line_p == spliton ? 0 : Utils.LineOnSide(line_p, divline));
			switch (side) {
			case 0:
				frontcount++;
				break;
			case 1:
				backcount++;
				break;
			case -2:
				frontcount++;
				backcount++;
				break;
			}
			
			int maxl = Math.max(frontcount, backcount);
			int newl = (frontcount + backcount) - lines_i.size();  // measure for cuts (bad)
			grade = maxl + newl * 8;
			if (grade > worstgrade)
				return grade;		// might as well stop now
		}

		if (frontcount == 0 || backcount == 0) {
			return Integer.MAX_VALUE;			// line does not partition at all
		}

		return grade;
	}

	private divline_t DivlineFromWorldline(line_t w) {
		divline_t d = new divline_t();
		d.pt = w.p1;
		d.dx = w.p2.x - w.p1.x;
		d.dy = w.p2.y - w.p1.y;
		return d;
	}
	
	
	
	/*
	================
	=
	= ExecuteSplit
	=
	= Actually splits the line list as EvaluateLines predicted
	================
	*/

	private void ExecuteSplit(List<line_t> lines_i, line_t spliton, List<line_t> frontlist_i, List<line_t> backlist_i) {
		divline_t divline = DivlineFromWorldline(spliton);

		for (int i = 0 ; i < lines_i.size() ; i++) {
			line_t line_p = lines_i.get(i);
			int side = (line_p == spliton ? 0 : Utils.LineOnSide(line_p, divline));
			switch (side) {
			case 0:
				frontlist_i.add(line_p);
				break;
			case 1:
				backlist_i.add(line_p);
				break;
			case -2:
				line_t newline_p = CutLine(line_p, divline);
				frontlist_i.add(line_p);
				backlist_i.add(newline_p);
				break;
			default:
				throw new IllegalArgumentException("ExecuteSplit: bad side");
			}
		}
	}

	private static double round (double x) {
		if (x > 0) {
			if (x - (int) x < 0.1) {
				return (int) x;
			} else if (x - (int) x > 0.9) {
				return (int) x + 1;
			} else {
				return x;
			}
		}
		
		if ((int) x - x < 0.1) {
			return (int) x;
		} else if ((int) x - x > 0.9) {
			return  (int) x - 1;
		}
		return x;
	}

	/*
	==================
	=
	= CutLine
	=
	= Truncates the given worldline to the front side of the divline
	= and returns the cut off back side in a newly allocated worldline
	==================
	*/

	private line_t CutLine (line_t wl, divline_t bl) {
		int			side;
		line_t		new_p = new line_t();
		double		frac;
		NXPoint		intr = new NXPoint();
		int			offset;
		
		cuts++;
		divline_t wld = DivlineFromWorldline(wl);
		
		new_p.p1 = wl.p1; 
		new_p.p2 = wl.p2; 
		new_p.linedef = wl.linedef; 
		new_p.side = wl.side; 
		new_p.offset = wl.offset; 
		new_p.grouped = wl.grouped; 
		
		frac = InterceptVector (wld, bl);
		intr.x = wld.pt.x + round(wld.dx * frac);
		intr.y = wld.pt.y + round(wld.dy * frac);
		
		offset = (int) (wl.offset + round(frac * Math.sqrt(wld.dx * wld.dx + wld.dy * wld.dy)));
		side = Utils.PointOnSide (wl.p1, bl);
		if (side == 0) {
			// line starts on front side
			wl.p2 = intr;
			new_p.p1 = intr;
			new_p.offset = offset;
		} else {	
			// line starts on back side
			wl.p1 = intr;
			wl.offset = offset;
			new_p.p2 = intr;
		}
		
		return new_p;
	}

	/*
	===============
	=
	= InterceptVector
	=
	= Returns the fractional intercept point along first vector
	===============
	*/

	private static double InterceptVector (divline_t v2, divline_t v1) {
		double den = v1.dy * v2.dx - v1.dx * v2.dy;
		if (den == 0) {
			throw new IllegalArgumentException("InterceptVector: parallel");
		}
		double num = (v1.pt.x - v2.pt.x) * v1.dy + (v2.pt.y - v1.pt.y) * v1.dx;
		double frac = num / den;
		if (frac <= 0.0 || frac >= 1.0) {
			throw new IllegalArgumentException("InterceptVector: intersection outside line");
		}
		return frac;
	}

} 
