package ds.gae.entities;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import ds.gae.ReservationException;

@Entity
@NamedQueries({
	@NamedQuery(name = "CarRentalCompany.names", query = "SELECT c.name FROM CarRentalCompany c"),
	@NamedQuery(name = "CarRentalCompany.types", query = "SELECT c.types FROM CarRentalCompany c WHERE c.name = :company")
})
public class CarRentalCompany {

	private static Logger logger = Logger.getLogger(CarRentalCompany.class.getName());
	
	@Id
	private String name;
	
	@OneToMany(cascade = CascadeType.ALL)
	private Set<CarType> types;

	/***************
	 * CONSTRUCTOR *
	 ***************/

	public CarRentalCompany() {
		
	}
	
	public CarRentalCompany(String name, Set<CarType> types) {
		logger.log(Level.INFO, "<{0}> Car Rental Company {0} starting up...", name);
		setName(name);
		this.types = types;
		for (CarType type : types) {
			type.setCompany(this.getName());
		}
	}

	/********
	 * NAME *
	 ********/

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	/*************
	 * CAR TYPES *
	 *************/

	public Collection<CarType> getAllCarTypes() {
		return this.types;
	}
	
	public Set<CarType> getTypes() {
		return this.types;
	}
	
	public void setTypes(Set<CarType> types) {
		this.types = types;
	}
	
	private Map<String, CarType> getCarTypeMap() {
		Map<String, CarType> toReturn = new HashMap<String, CarType>();
		for (CarType type : types) {
			toReturn.put(type.getName(), type);
		}
		return toReturn;
	}
	public CarType getCarType(String carTypeName) {
		Map<String, CarType> types = this.getCarTypeMap();
		
		if(types.containsKey(carTypeName))
			return types.get(carTypeName);
		throw new IllegalArgumentException("<" + carTypeName + "> No car type of name " + carTypeName);
	}
	
	public boolean isAvailable(String carTypeName, Date start, Date end) {
		logger.log(Level.INFO, "<{0}> Checking availability for car type {1}", new Object[]{name, carTypeName});
		if(this.getCarTypeMap().containsKey(carTypeName))
			return getAvailableCarTypes(start, end).contains(this.getCarTypeMap().get(carTypeName));
		throw new IllegalArgumentException("<" + carTypeName + "> No car type of name " + carTypeName);
	}
	
	public Set<CarType> getAvailableCarTypes(Date start, Date end) {
		Set<CarType> availableCarTypes = new HashSet<CarType>();
		for (CarType type : this.getAllCarTypes()) {
			for (Car car : type.getCars()) {
				if (car.isAvailable(start, end)) {
					availableCarTypes.add(type);
				}
			}
		}
		return availableCarTypes;
	}
	
	/*********
	 * CARS *
	 *********/
	
	private Car getCar(int uid) {
		for (Car car : this.getCars()) {
			if (car.getId() == uid)
				return car;
		}
		throw new IllegalArgumentException("<" + name + "> No car with uid " + uid);
	}
	
	public Set<Car> getCars() {
    	Set<Car> toReturn = new HashSet<Car>();
    	for (CarType type : this.getAllCarTypes()) {
    		toReturn.addAll(type.getCars());
    	}
    	return toReturn;
    }
	
	private List<Car> getAvailableCars(String carType, Date start, Date end) {
		List<Car> availableCars = new LinkedList<Car>();
		CarType type = this.getCarTypeMap().get(carType);
		for (Car car : type.getCars()) {
			if (car.isAvailable(start, end)) {
				availableCars.add(car);
			}
		}
		return availableCars;
	}

	/****************
	 * RESERVATIONS *
	 ****************/

	public Quote createQuote(ReservationConstraints constraints, String client)
			throws ReservationException {
		logger.log(Level.INFO, "<{0}> Creating tentative reservation for {1} with constraints {2}", 
                        new Object[]{name, client, constraints.toString()});
		
		CarType type = getCarType(constraints.getCarType());
		
		if(!isAvailable(constraints.getCarType(), constraints.getStartDate(), constraints.getEndDate()))
			throw new ReservationException("<" + name
				+ "> No cars available to satisfy the given constraints.");
		
		double price = calculateRentalPrice(type.getRentalPricePerDay(),constraints.getStartDate(), constraints.getEndDate());
		
		return new Quote(client, constraints.getStartDate(), constraints.getEndDate(), getName(), constraints.getCarType(), price);
	}

	// Implementation can be subject to different pricing strategies
	private double calculateRentalPrice(double rentalPricePerDay, Date start, Date end) {
		return rentalPricePerDay * Math.ceil((end.getTime() - start.getTime())
						/ (1000 * 60 * 60 * 24D));
	}

	public Reservation confirmQuote(Quote quote) throws ReservationException {
		logger.log(Level.INFO, "<{0}> Reservation of {1}", new Object[]{name, quote.toString()});
		List<Car> availableCars = getAvailableCars(quote.getCarType(), quote.getStartDate(), quote.getEndDate());
		if(availableCars.isEmpty())
			throw new ReservationException("Reservation failed, all cars of type " + quote.getCarType()
	                + " are unavailable from " + quote.getStartDate() + " to " + quote.getEndDate());
		Car car = availableCars.get((int)(Math.random()*availableCars.size()));
		
		Reservation res = new Reservation(quote, car.getId());
		car.addReservation(res);
		return res;
	}

	public void cancelReservation(Reservation res) {
		logger.log(Level.INFO, "<{0}> Cancelling reservation {1}", new Object[]{name, res.toString()});
		getCar(res.getCarId()).removeReservation(res);
	}
}