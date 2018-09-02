package idbsp.types;

public class line_t {
	public NXPoint		p1, p2;
	public worldline_t  linedef;
	public int			side, offset;
	public boolean		grouped;				// internal error check
}
