package idbsp.logic;

import java.util.ArrayList;
import java.util.List;

import idbsp.types.worldline_t;
import idbsp.types.worldthing_t;

/**
 * Data
 * 
 * @author Agnes
 *
 */
public class DoomMap {

	private int version;
	private List<worldline_t> lineStore = new ArrayList<>();
	private List<worldthing_t> thingStore = new ArrayList<>();

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public List<worldline_t> getLineStore() {
		return lineStore;
	}

	public List<worldthing_t> getThingStore() {
		return thingStore;
	}
	
	
}
