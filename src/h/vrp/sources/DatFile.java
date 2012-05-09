package h.vrp.sources;

import h.vrp.model.Instance;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatFile implements IInstanceFactory {
	private List<Point2D.Float> points = new ArrayList<Point2D.Float>();
	private List<Integer> demands = new ArrayList<Integer>();
	private List<Integer> depots = new ArrayList<Integer>();
	private Map<String, String> attrs = new TreeMap<String, String>();
	private int maxLength;
	private int capacity;
	private int vehicles;
	
	private boolean hasCapCon = false;
	private boolean hasLenthCon = false;
	
	public DatFile(String filename) throws FileNotFoundException {
		Scanner s = new Scanner(new BufferedReader(new FileReader(filename)));
		s.useLocale(Locale.US);
		
		boolean shift = false;
		
		String lastToken = null;
		
		while (s.hasNext()) {
			String token = s.next();
			if (token.equalsIgnoreCase("NODE_COORD_SECTION")) {
				readCoordinates(s);
			} else if (token.equalsIgnoreCase("DEMAND_SECTION")) {
				readDemands(s);
			} else if (token.equalsIgnoreCase("DEPOT_SECTION")) {
				readDepots(s);
			} else if (token.equalsIgnoreCase("EOF")) {
				break;
			} else if (token.equals(":")) {
//				shift = true;
				String rol = s.nextLine().trim();
				handleKeyValue(lastToken, rol);
			} else {
//				if (shift) {
//					handleKeyValue(lastToken, token);
//				}
//				shift = false;
				lastToken = token;
			}
		}
		
		//reshuffle depots because of the stupid
		if (depots.size() > 1) {
			System.err.println("Warning : Dat file loader doesn't support multiple depots correctly");
		} else {
			int depot = depots.get(0) - 1;
			if (depot != 0) {
				System.err.println("Warning: Dat file loader swapping " + 0 + " and " + depot);
				Point2D.Float depotp = points.get(depot);
				int depotDemand = demands.get(depot);
				
				points.set(depot, points.get(0));
				demands.set(depot, demands.get(0));
				points.set(0, depotp);
				demands.set(0, depotDemand);
			}
		}
	}

	private void readCoordinates(Scanner s) {
		List<Float> row = new ArrayList<Float>(3);
		while (s.hasNextFloat()) {
			row.add(s.nextFloat());
			if (row.size() == 3) {
				points.add(new Point2D.Float(row.get(1), row.get(2)));
				row.clear();
			}
		}
	}

	private void readDemands(Scanner s) {
		boolean b = false;
		while (s.hasNextInt()) {
			if (b) {
				demands.add(s.nextInt());
				b = false;
			} else {
				s.nextInt();
				b = true;
			}
		}
	}

	private void readDepots(Scanner s) {
		while (s.hasNextInt()) {
			int i = s.nextInt();
			if (i >= 0)
				depots.add(i);
		}
	}

	private void handleKeyValue(String key, String value) {
		if (key.equals("VEHICLES")) {
			vehicles = Integer.parseInt(value);
		} else if (key.equals("CAPACITY")) {
			capacity = Integer.parseInt(value);
			hasCapCon = true;
		} else if (key.equals("MAXLENGTH")) {
			maxLength = Integer.parseInt(value);
			hasLenthCon = true;
		} else {
			attrs.put(key, value);
		}
	}
	
	

	@Override
	public String toString() {
		return "DatFile [attrs=" + attrs + ", capacity=" + capacity
				+ ", demands=" + demands + ", depots=" + depots
				+ ", maxLength=" + maxLength + ", points=" + points
				+ ", vehicles=" + vehicles + "]";
	}

	public List<Point2D.Float> getPoints() {
		return points;
	}

	public List<Integer> getDemands() {
		return demands;
	}

	public int getCapacity() {
		return capacity;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public int getVehicles() {
		return vehicles;
	}

	public Instance createInstance() {
		Instance i = new Instance(attrs.get("NAME"), points, demands, vehicles, capacity, maxLength);
		i.setCapacityConstraintEnabled(hasCapCon);
		i.setLengthConstraintEnabled(hasLenthCon);
		return i;
	}
	
	public String toPython() {
		StringBuilder sb = new StringBuilder();
		String tabs = "    ";
		String cn=",\n";
		sb.append("Instance(\n");
		sb.append(tabs + "name='" + attrs.get("NAME") + "'"  + cn);
		StringBuilder pointStr = new StringBuilder();
		
		for (Point2D.Float pt : points) {
			if (pointStr.length() > 0) pointStr.append(", ");
			pointStr.append("[" + pt.x + ", " + pt.y  + "]");
		}
		
		sb.append(tabs + "points=[" + pointStr + "]" + cn);
		sb.append(tabs + "demands=" + demands.toString() + cn);
		sb.append(tabs + "vehicles=" + vehicles);
		if (hasCapCon) sb.append(cn + tabs + "capacity=" + capacity);
		if (hasLenthCon) sb.append(cn + tabs + "maxlength=" + maxLength);
		sb.append(")\n");
		return sb.toString();
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		if (args[0].equals("csv")) {
		System.out.println("Name, Comment, Size, Kind, Capacity, Mean Demand, Max. Length, Mean Edge, Optimum");
		
			for (int ix = 1; ix<args.length ;ix++) {
				String s = args[ix];
				DatFile df = new DatFile(s);
				Instance i = df.createInstance();
				System.out.print(i.toString() + ", ");
				System.out.print(df.getComment().replace(",", " --") +", ");
				System.out.print(i.getPoints().size() + ", ");
				if (i.isCapacityConstraintEnabled() && i.isLengthConstraintEnabled()) {
					System.out.print("DCVRP, ");
				} else if (i.isCapacityConstraintEnabled()) {
					System.out.print("CVRP, ");
				} else if (i.isLengthConstraintEnabled()) {
					System.out.print("DVRP, "); 
				} else {
					System.out.print("TSP, ");
				}
				if (i.isCapacityConstraintEnabled()) {
					System.out.print(i.getCapacity() +", " +  i.getMeanVertexDemand() + ", ");
				} else {
					System.out.print("n/a, n/a, ");
				}
				if (i.isLengthConstraintEnabled()) {
					System.out.print(i.getMaxLength() +", " +  i.getMeanEdgeLength() + ", ");
				} else {
					System.out.print("n/a, n/a, ");
				}
				String comment = df.getComment();
				Pattern p = Pattern.compile("[Oo]ptimal [Vv]alue:\\s*(\\d+(\\.\\d+)?)");
				Matcher m = p.matcher(comment);
				if (m.find()) {
					System.out.println(m.group(1));
				} else {
					System.out.println("unknown");
				}
			}
		} else if (args[0].equals("py")) {
			for (int ix = 1; ix<args.length; ix++) {
				String s = args[ix];
				DatFile df = new DatFile(s);
				System.out.println(df.toPython());
			}
		}
	}

	private String getComment() {
		if (attrs.containsKey("COMMENT")) {
			return attrs.get("COMMENT");
		} else {
			return "none";
		}
	}
}
