package idbsp.logic;

import static idbsp.logic.Constants.ML_TWOSIDED;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import idbsp.types.NXPoint;
import idbsp.types.sectordef_t;
import idbsp.types.worldline_t;
import idbsp.types.worldside_t;
import idbsp.types.worldthing_t;

/**
 * DoomMapLoader
 * 
 * @author Agnes
 *
 */
public class DoomMapLoader {

	private static Charset CHARSET_ISO_8559_1 = Charset.forName("iso-8859-1");

	private static final int WORLD_SERVER_VERSION = 4;

	private static final Pattern PATTERN_VERSION = Pattern.compile("^WorldServer version (?<version>\\d+)$");
	private static final Pattern PATTERN_LINECOUNT = Pattern.compile("^lines:(?<lines>\\d+)$");
	private static final Pattern PATTERN_THINGCOUNT = Pattern.compile("^things:(?<things>\\d+)$");
	private static final Pattern PATTERN_WORLDLINE_1 = Pattern.compile("^\\((.+),(.+)\\) to \\((.+),(.+)\\) : (.+) : (.+) : (.+)$");
	private static final Pattern PATTERN_WORLDLINE_2 = Pattern.compile("^(.+) \\((.+) : (.+) / (.+) / (.+) \\)$");
	private static final Pattern PATTERN_WORLDLINE_3 = Pattern.compile("^(.+) : (.+) (.+) : (.+) (.+) (.+) (.+)$");
	private static final Pattern PATTERN_WORLDTHING = Pattern.compile("^\\((.+),(.+), (.+)\\) :(.+), (.+)$");
	
	public DoomMap load(Path path) throws Exception {
		DoomMap map = new DoomMap();
		try (Stream<String> stream = Files.lines(path, CHARSET_ISO_8559_1)) {
			Iterator<String> iterator = stream.iterator();
			readWorldServerVersion(iterator, map);
			readLines(iterator, map);
			readThings(iterator, map);
		};
		return map;
	}
	
	private String nextLine(Iterator<String> iterator) {
		while (iterator.hasNext()) {
			String line = iterator.next().trim();
			if (!line.isEmpty()) {
				return line;
			}
		}
		return null;
	}
	
	private void readWorldServerVersion(Iterator<String> iterator, DoomMap map) {
		Matcher m = PATTERN_VERSION.matcher(nextLine(iterator));
		int version = (m.matches() ? Integer.parseInt(m.group("version")) : -1);
		if (WORLD_SERVER_VERSION != version) {
			throw new IllegalArgumentException(String.format("LoadDoomMap: not a version %d doom map", WORLD_SERVER_VERSION));
		}
		System.out.println("doom map version: " + version);
		map.setVersion(version);
	}

	private int readLineCount(Iterator<String> iterator) {
		Matcher m = PATTERN_LINECOUNT.matcher(nextLine(iterator));
		int count = (m.matches() ? Integer.parseInt(m.group("lines")) : -1);
		if (count < 0) {
			throw new IllegalArgumentException(String.format("LoadDoomMap: can't read linecount"));
		}
		System.out.println(String.format("%d lines", count));
		return count;
	}

	private void readLines(Iterator<String> iterator, DoomMap map) {
		int count = readLineCount(iterator);
		for (int i = 0; i < count; i++) {
			worldline_t line = readLine(iterator, i);
			map.getLineStore().add(line);
		}
	}
	
	private worldline_t readLine(Iterator<String> iterator, int linenum) {
		worldline_t line = new worldline_t();
		line.p1 = new NXPoint();
		line.p2 = new NXPoint();
		
		Matcher m1 = PATTERN_WORLDLINE_1.matcher(nextLine(iterator));
		if (!m1.matches()) {
			throw new IllegalArgumentException(String.format("Failed ReadLine"));
		}
		line.p1.x = Double.parseDouble(m1.group(1));
		line.p1.y = Double.parseDouble(m1.group(2));
		line.p2.x = Double.parseDouble(m1.group(3));
		line.p2.y = Double.parseDouble(m1.group(4));
		line.flags = Integer.parseInt(m1.group(5));
		line.special = Integer.parseInt(m1.group(6));
		line.tag = Integer.parseInt(m1.group(7));
		
		int sides = ((line.flags & ML_TWOSIDED) == 0 ? 1 : 2);
		line.side = new worldside_t[sides];

		for (int i = 0; i < sides; i++) {
			worldside_t s = line.side[i] = new worldside_t();
			sectordef_t e = line.side[i].sectordef = new sectordef_t();

			Matcher m2 = PATTERN_WORLDLINE_2.matcher(nextLine(iterator));
			if (!m2.matches()) {
				throw new IllegalArgumentException("Failed ReadLine (side)");
			}
			s.firstrow = Integer.parseInt(m2.group(1));
			s.firstcollumn = Integer.parseInt(m2.group(2));
			s.toptexture = m2.group(3);
			s.bottomtexture = m2.group(4);
			s.midtexture = m2.group(5);
			
			Matcher m3 = PATTERN_WORLDLINE_3.matcher(nextLine(iterator));
			if (!m3.matches()) {
				throw new IllegalArgumentException("Failed ReadLine (sector)");
			}
			e.floorheight = Integer.parseInt(m3.group(1));
			e.floorflat = m3.group(2);
			e.ceilingheight = Integer.parseInt(m3.group(3));
			e.ceilingflat = m3.group(4);
			e.lightlevel = Integer.parseInt(m3.group(5));
			e.special = Integer.parseInt(m3.group(6));
			e.tag = Integer.parseInt(m3.group(7));
			
			if ("-".equals(e.floorflat)) {
				System.err.println(String.format("WARNING: line %d has no sectordef", linenum));
			}
		}
		
		return line;
	}
	
	private int readThingCount(Iterator<String> iterator) {
		Matcher m = PATTERN_THINGCOUNT.matcher(nextLine(iterator));
		int count = (m.matches() ? Integer.parseInt(m.group("things")) : -1);
		if (count < 0) {
			throw new IllegalArgumentException(String.format("LoadDoomMap: can't read thingcount"));
		}
		System.out.println(String.format("%d things", count));
		return count;
	}

	private void readThings(Iterator<String> iterator, DoomMap map) {
		int count = readThingCount(iterator);
		for (int i = 0; i < count; i++) {
			worldthing_t thing = readThing(iterator, i);
			map.getThingStore().add(thing);
		}
	}
	
	private worldthing_t readThing(Iterator<String> iterator, int linenum) {
		worldthing_t thing = new worldthing_t();
		thing.origin = new NXPoint();

		Matcher m1 = PATTERN_WORLDTHING.matcher(iterator.next());
		if (!m1.matches()) {
			throw new IllegalArgumentException(String.format("Failed ReadThing"));
		}
		int x = Integer.parseInt(m1.group(1));
		int y = Integer.parseInt(m1.group(2));
		thing.angle = Integer.parseInt(m1.group(3));
		thing.type = Integer.parseInt(m1.group(4));
		thing.options = Integer.parseInt(m1.group(5));

		thing.origin.x = x & -16;
		thing.origin.y = y & -16;
		
		return thing;
	}
	
}
