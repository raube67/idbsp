package idbsp.types;

public class worldside_t {
	public int			firstrow;	
	public int			firstcollumn;
	public String		toptexture;
	public String		bottomtexture;
	public String		midtexture;
	public sectordef_t	sectordef;			// on the viewer's side
	public int			sector;				// only used when saving doom map
}
