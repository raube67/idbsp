package idbsp.logic;

import idbsp.types.NXPoint;
import idbsp.types.divline_t;
import idbsp.types.line_t;

public class Utils {

	/*
	==================
	=
	= LineOnSide
	=
	= Returns side 0 (front), 1 (back) or -2 if line must be split
	= If the line is colinear, it will be placed on the front side if
	= it is going the same direction as the dividing line
	==================
	*/

	public static int LineOnSide (line_t wl, divline_t bl) {
		int s1 = PointOnSide (wl.p1, bl);
		int s2 = PointOnSide (wl.p2, bl);

		if (s1 == s2) {
			if (s1 == -1) {
				// colinear, so see if the directions are the same
				double dx = wl.p2.x - wl.p1.x;
				double dy = wl.p2.y - wl.p1.y;
				if (Math.signum(dx) == Math.signum(bl.dx) && Math.signum(dy) == Math.signum(bl.dy)) {
					return 0;
				}
				return 1;
			}
			return s1;
		}
		if (s1 == -1) {
			return s2;
		}
		if (s2 == -1) {
			return s1;
		}
			
		return -2;
	}
	

	/*
	==================
	=
	= PointOnSide
	=
	= Returns side 0 (front), 1 (back), or -1 (colinear)
	==================
	*/

	public static int PointOnSide(NXPoint p, divline_t l) {
		
		// Schnellprüfung, wenn divline parallel zu den Achsen
		
		if (l.dx == 0) {
			if (p.x > l.pt.x - 2 && p.x < l.pt.x + 2) {
				return -1;
			}
			if (p.x < l.pt.x) {
				return (l.dy > 0 ? 1 : 0);
			}
			return (l.dy < 0 ? 1 : 0);
		}
		
		if (l.dy == 0) {
			if (p.y > l.pt.y - 2 && p.y < l.pt.y + 2) {
				return -1;
			}
			if (p.y < l.pt.y) {
				return (l.dx < 0 ? 1 : 0);
			}
			return (l.dx > 0 ? 1 : 0);
		}

		double dx = p.x - l.pt.x;
		double dy = p.y - l.pt.y;

		// Kollinear? Innerhalb eines 2-Pixel-Streifens auf der divline?
		// Ansatz quadratische Gleichung:
		// Sei r = (l.dx, l.dy) der Richtungsvektor der divline
		// Frage: Ist dann dist( p,  (l.pt + t*r) ) < 2 für ein t?
		// (p.x - l.pt.x - t*l.dx) ^ 2 + (p.y - l.pt.y - t*l.dy) ^ 2 < 4 ?
		// (dx - t*l.dx)^2 + (dy + t*l.dy)^2 - 4 < 0 ?
		// dx^2 - t*2*dx*l.dx + t^2*l.dx^2 + dy^2 - t*2*dy*l.dy + t^2*l.dy^2 - 4 < 0?
		// a = l.dx * l.dx + l.dy * l.dy
		// b = - 2 * (l.dx * dx + l.dy * dy)
		// c = dx * dx + dy * dy - 4
		// a * t^2 + b * t + c < 0 ?
		// Frage: Ist b * b - 4 * a * c > 0 ? Ja -> kollinear
		
		double a = l.dx * l.dx + l.dy * l.dy;
		double b = -2.0 * (l.dx * dx + l.dy * dy);
		double c = dx * dx + dy * dy - 4.0;		// 2 unit radius
		double d = b * b - 4 * a * c;
		
		if (d > 0) {
			return -1;						// within four pixels of line
		}
		
		// Kreuzprodukt
		//  dx     l.dx        0
		//  dy  x  l.dy   =    0
		//  0      0           dx * l.dy - dy * l.dx
		// Frage: z-Achse des Kreuzprodukts positiv? Ja -> front
		
		return (dx * l.dy - dy * l.dx > 0 ? 0 : 1);			
	}
	
    public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			//
		}

    }
    


}
