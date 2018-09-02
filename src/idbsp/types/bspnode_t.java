package idbsp.types;

import java.util.List;

public class bspnode_t {
	public List<line_t>				lines_i;		// if non NULL, the node is
	public divline_t				divline;		// terminal and has no children
	public bbox_t					bbox;
	public bspnode_t[]				side;
}
