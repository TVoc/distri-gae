package ds.gae;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Transaction;

import ds.gae.entities.Car;
import ds.gae.entities.CarRentalCompany;
import ds.gae.entities.CarType;
import ds.gae.entities.Quote;
import ds.gae.entities.Reservation;
import ds.gae.entities.ReservationConstraints;
 
public class CarRentalModel {
	
	public Map<String,CarRentalCompany> CRCS = new HashMap<String, CarRentalCompany>();	
	
	private static CarRentalModel instance;
	
	public static CarRentalModel get() {
		if (instance == null)
			instance = new CarRentalModel();
		return instance;
	}
		
	/**
	 * Get the car types available in the given car rental company.
	 *
	 * @param 	crcName
	 * 			the car rental company
	 * @return	The list of car types (i.e. name of car type), available
	 * 			in the given car rental company.
	 */
	public Set<String> getCarTypesNames(String crcName) {
		EntityManager em = EMF.get().createEntityManager();
		TypedQuery<String> query = em.createQuery("SELECT t.name FROM CarType t, CarRentalCompany c WHERE t MEMBER OF types", String.class);
		
		Set<String> toReturn = new HashSet<String>(query.getResultList());
		em.close();
		return toReturn;
	}

    /**
     * Get all registered car rental companies
     *
     * @return	the list of car rental companies
     */
    public Collection<String> getAllRentalCompanyNames() {
    	EntityManager em = EMF.get().createEntityManager();
    	
		String queryString = "SELECT c.name FROM CarRentalCompany c";
		TypedQuery<String> query = em.createQuery(queryString, String.class);
		
		List<String> toReturn = new ArrayList<String>(query.getResultList());
		em.close();
		
		return toReturn;
    }
	
	/**
	 * Create a quote according to the given reservation constraints (tentative reservation).
	 * 
	 * @param	company
	 * 			name of the car renter company
	 * @param	renterName 
	 * 			name of the car renter 
	 * @param 	constraints
	 * 			reservation constraints for the quote
	 * @return	The newly created quote.
	 *  
	 * @throws ReservationException
	 * 			No car available that fits the given constraints.
	 */
    public Quote createQuote(String company, String renterName, ReservationConstraints constraints) throws ReservationException {
		// FIXME: use persistence instead
    	
    	EntityManager em = EMF.get().createEntityManager();
    	
    	CarRentalCompany crc = em.find(CarRentalCompany.class, company);
    	Quote out = null;

        if (crc != null) {
            out = crc.createQuote(constraints, renterName);
        } else {
        	em.close();
        	throw new ReservationException("CarRentalCompany not found.");    	
        }
        
        em.close();
        return out;
    }
    
	/**
	 * Confirm the given quote.
	 *
	 * @param 	q
	 * 			Quote to confirm
	 * 
	 * @throws ReservationException
	 * 			Confirmation of given quote failed.	
	 */
	public void confirmQuote(Quote q) throws ReservationException {
		// FIXME: use persistence instead

		EntityManager em = EMF.get().createEntityManager();
		
		CarRentalCompany crc = em.find(CarRentalCompany.class, q.getRentalCompany());
        crc.confirmQuote(q);
	}
	
    /**
	 * Confirm the given list of quotes
	 * 
	 * @param 	quotes 
	 * 			the quotes to confirm
	 * @return	The list of reservations, resulting from confirming all given quotes.
	 * 
	 * @throws 	ReservationException
	 * 			One of the quotes cannot be confirmed. 
	 * 			Therefore none of the given quotes is confirmed.
	 */
    public List<Reservation> confirmQuotes(List<Quote> quotes) throws ReservationException {    	
		// TODO add implementation
    	
    	List<Reservation> toReturn = new ArrayList<Reservation>();
    	
    	DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    	EntityManager em = EMF.get().createEntityManager();
    	
    	Transaction txn = ds.beginTransaction();
    	
    	try {
        	for (Quote quote : quotes) {
        		CarRentalCompany c = em.find(CarRentalCompany.class, quote.getRentalCompany());
        		toReturn.add(c.confirmQuote(quote));
        	}
        	txn.commit();
    	} catch (ReservationException e) {
    		throw e;
    	} finally {
    		if (txn.isActive()) {
    			txn.rollback();
    		}
    	}
    
    	return toReturn;
    }
	
	/**
	 * Get all reservations made by the given car renter.
	 *
	 * @param 	renter
	 * 			name of the car renter
	 * @return	the list of reservations of the given car renter
	 */
	public List<Reservation> getReservations(String renter) {
		// FIXME: use persistence instead
		
		EntityManager em = EMF.get().createEntityManager();
		String queryString = "SELECT r FROM Reservation r WHERE carRenter = :renter";
		TypedQuery<Reservation> query = em.createQuery(queryString, Reservation.class);
    	
		List<Reservation> toReturn = new ArrayList<Reservation>(query.getResultList());
		em.close();
    	return toReturn;
    }

    /**
     * Get the car types available in the given car rental company.
     *
     * @param 	crcName
     * 			the given car rental company
     * @return	The list of car types in the given car rental company.
     */
    public Collection<CarType> getCarTypesOfCarRentalCompany(String crcName) {
		// FIXME: use persistence instead

    	EntityManager em = EMF.get().createEntityManager();
    	
    	CarRentalCompany crc = em.find(CarRentalCompany.class, crcName);
    	String queryString = "SELECT t FROM CarType t, CarRentalCompany c WHERE c.name = :company AND t MEMBER OF c.types";
    	
    	TypedQuery<CarType> query = em.createQuery(queryString, CarType.class);
    	query.setParameter("company", crc.getName());
    	
    	List<CarType> toReturn = new ArrayList<CarType>(query.getResultList());
    	em.close();
    	return toReturn;
    }
	
    /**
     * Get the list of cars of the given car type in the given car rental company.
     *
     * @param	crcName
	 * 			name of the car rental company
     * @param 	carType
     * 			the given car type
     * @return	A list of car IDs of cars with the given car type.
     */
    public Collection<Integer> getCarIdsByCarType(String crcName, CarType carType) {
    	Collection<Integer> out = new ArrayList<Integer>();
    	for (Car c : getCarsByCarType(crcName, carType)) {
    		out.add(c.getId());
    	}
    	return out;
    }
    
    /**
     * Get the amount of cars of the given car type in the given car rental company.
     *
     * @param	crcName
	 * 			name of the car rental company
     * @param 	carType
     * 			the given car type
     * @return	A number, representing the amount of cars of the given car type.
     */
    public int getAmountOfCarsByCarType(String crcName, CarType carType) {
    	return this.getCarsByCarType(crcName, carType).size();
    }

	/**
	 * Get the list of cars of the given car type in the given car rental company.
	 *
	 * @param	crcName
	 * 			name of the car rental company
	 * @param 	carType
	 * 			the given car type
	 * @return	List of cars of the given car type
	 */
	private List<Car> getCarsByCarType(String crcName, CarType carType) {
		EntityManager em = EMF.get().createEntityManager();
		
		CarRentalCompany company = em.find(CarRentalCompany.class, crcName);
		
		String queryString = "SELECT c FROM Car c, CarType t, CarRentalCompany comp WHERE comp = :company AND t IN types AND c IN cars";
		TypedQuery<Car> query = em.createQuery(queryString, Car.class);
		query.setParameter("company", company);
		
		List<Car> toReturn = new ArrayList<Car>(query.getResultList());
		em.close();
		return toReturn;
	}

	/**
	 * Check whether the given car renter has reservations.
	 *
	 * @param 	renter
	 * 			the car renter
	 * @return	True if the number of reservations of the given car renter is higher than 0.
	 * 			False otherwise.
	 */
	public boolean hasReservations(String renter) {
		return this.getReservations(renter).size() > 0;		
	}	
}