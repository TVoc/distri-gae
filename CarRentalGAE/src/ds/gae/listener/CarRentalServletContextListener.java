package ds.gae.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import ds.gae.CarRentalModel;
import ds.gae.entities.Car;
import ds.gae.entities.CarRentalCompany;
import ds.gae.entities.CarType;

public class CarRentalServletContextListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		// This will be invoked as part of a warming request, 
		// or the first user request if no warming request was invoked.
						
		// check if dummy data is available, and add if necessary
		if(!isDummyDataAvailable()) {
			addDummyData();
		}
	}
	
	private boolean isDummyDataAvailable() {
		// If the Hertz car rental company is in the datastore, we assume the dummy data is available

		// FIXME: use persistence instead
		EntityManager em = null;
		try {
			em = ds.gae.EMF.get().createEntityManager();
			CarRentalCompany hertz = em.find(CarRentalCompany.class, "Hertz");
			em.close();
			return hertz != null;
		} finally {
		}
		
		

	}
	
	private void addDummyData() {
		loadRental("Hertz","hertz.csv");
        loadRental("Dockx","dockx.csv");
	}
	
	private void loadRental(String name, String datafile) {
		Logger.getLogger(CarRentalServletContextListener.class.getName()).log(Level.INFO, "loading {0} from file {1}", new Object[]{name, datafile});
        try {
        	
            Set<CarType> types = loadData(name, datafile);
            
            
    		EntityManager em = ds.gae.EMF.get().createEntityManager();
    		
    		CarRentalCompany company = new CarRentalCompany(name, types);
    		em.persist(company);
    		
    		/*
            for (Car car : cars) {
            	em.persist(car);
            	em.merge(car.getType());
            }
            */
    		
            em.close();

        } catch (NumberFormatException ex) {
            Logger.getLogger(CarRentalServletContextListener.class.getName()).log(Level.SEVERE, "bad file", ex);
        } catch (IOException ex) {
            Logger.getLogger(CarRentalServletContextListener.class.getName()).log(Level.SEVERE, null, ex);
        }
	}
	
	public static Set<CarType> loadData(String name, String datafile) throws NumberFormatException, IOException {
		// FIXME: adapt the implementation of this method to your entity structure
		
		Set<CarType> types = new HashSet<CarType>();
		int carId = 1;

		//open file from jar
		BufferedReader in = new BufferedReader(new InputStreamReader(CarRentalServletContextListener.class.getClassLoader().getResourceAsStream(datafile)));
		//while next line exists
		while (in.ready()) {
			//read line
			String line = in.readLine();
			//if comment: skip
			if (line.startsWith("#")) {
				continue;
			}
			List<Car> carList = new ArrayList<Car>();
			
			//tokenize on ,
			StringTokenizer csvReader = new StringTokenizer(line, ",");
			String typeName = csvReader.nextToken();
			int nbOfSeats = Integer.parseInt(csvReader.nextToken());
			float trunkSpace = Float.parseFloat(csvReader.nextToken());
			double rentalPricePerDay = Double.parseDouble(csvReader.nextToken());
			boolean smokingAllowed = Boolean.parseBoolean(csvReader.nextToken());
			//create new car type from first 5 fields
			//create N new cars with given type, where N is the 5th field
			for (int i = Integer.parseInt(csvReader.nextToken()); i > 0; i--) {
				carList.add(new Car(carId++));
			}
			types.add(new CarType(typeName, nbOfSeats, trunkSpace, rentalPricePerDay, smokingAllowed, carList));
			
		}

		return types;
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// App Engine does not currently invoke this method.
	}
}