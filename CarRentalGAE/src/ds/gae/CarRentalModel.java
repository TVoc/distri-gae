package ds.gae;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

import ds.gae.entities.Car;
import ds.gae.entities.CarRentalCompany;
import ds.gae.entities.CarType;
import ds.gae.entities.Quote;
import ds.gae.entities.Reservation;
import ds.gae.entities.ReservationConstraints;
import ds.gae.tasks.ConfirmQuotesTask;
 
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
		Query query = em.createNamedQuery("CarRentalCompany.types");
		query.setParameter("company", crcName);
		
		org.datanucleus.store.types.sco.backed.HashSet queryResult = (org.datanucleus.store.types.sco.backed.HashSet) query.getResultList().get(0);

    	Set<String> toReturn = new HashSet<String>();
    	
    	for (Object type : queryResult) {
    		toReturn.add(((CarType) type).getName());
    	}
    	
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
    	
		TypedQuery<String> query = em.createNamedQuery("CarRentalCompany.names", String.class);
		
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
		
		try {
			Reservation res = crc.confirmQuote(q);
			em.persist(q);
			em.persist(res);
		} catch (ReservationException e) {
			throw e;
		} finally {
			em.close();
		}
        
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
    public void confirmQuotes(List<Quote> quotes) throws ReservationException {    	
		// TODO add implementation
    	
    	for (Quote quote : quotes) {
    		EntityManager em = EMF.get().createEntityManager();
    		em.persist(quote);
    		em.close();
    	}
    	
    	
    	
    	ConfirmQuotesTask task = new ConfirmQuotesTask(quotes);
    	Queue queue = QueueFactory.getDefaultQueue();
    	queue.add(TaskOptions.Builder.withPayload(task));
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
		Query query = em.createNamedQuery("Reservation.all");
		query.setParameter("renter", renter);
    	
		List<Reservation> toReturn = new ArrayList<Reservation>();
		for (Object obj : query.getResultList()) {
			Reservation res = (Reservation) obj;
			if (res.getCarRenter().equals(renter)) {
				toReturn.add((Reservation) res);
			}
		}
		em.close();
    	return toReturn;
    }
	
	public List<Quote> getQuotesInProgress(String renter) {
		EntityManager em = EMF.get().createEntityManager();
		
		
		TypedQuery<Quote> query = em.createNamedQuery("Quote.inProgress", Quote.class);
		query.setParameter("renter", renter);
		
		List<Quote> toReturn = query.getResultList();
		em.close();
		return toReturn;
	}
	
	public List<Quote> getFailedQuotes(String renter) {
		EntityManager em = EMF.get().createEntityManager();
		
		
		TypedQuery<Quote> query = em.createNamedQuery("Quote.completed", Quote.class);
		query.setParameter("renter", renter);
		
		List<Quote> queryResult = query.getResultList();
		List<Quote> quotes = new ArrayList<Quote>();
		quotes.addAll(queryResult);
		
		
		query = em.createNamedQuery("Quote.fromReservations", Quote.class);
		
		List<Quote> resQuotes = query.getResultList();
		
		/*
		org.datanucleus.store.types.sco.backed.HashSet queryResult = (org.datanucleus.store.types.sco.backed.HashSet) nQuery.getResultList().get(0);
    	List<Quote> resQuotes = new ArrayList<Quote>();
    	
    	for (Object obj : queryResult) {
    		Quote toAdd = (Quote) obj;
    		if (toAdd.getCarRenter().equals(renter)) {
    			resQuotes.add(toAdd);
    		}
    		
    	}
    	*/
		
		em.close();
		
		quotes.removeAll(resQuotes);
		
		return quotes;
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
    	
    	Query query = em.createNamedQuery("CarRentalCompany.types");
    	query.setParameter("company", crc.getName());
    	
    	org.datanucleus.store.types.sco.backed.HashSet queryResult = (org.datanucleus.store.types.sco.backed.HashSet) query.getResultList().get(0);
    	List<CarType> toReturn = new ArrayList<CarType>();
    	
    	for (Object type : queryResult) {
    		toReturn.add((CarType) type);
    	}
    	
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
		
		
		TypedQuery<CarType> query = em.createNamedQuery("CarType.fromCompanyAndName", CarType.class);
		query.setParameter("company", company.getName());
		query.setParameter("name", carType.getName());
		
		CarType trueType = query.getResultList().get(0);
		
		Query newQuery = em.createNamedQuery("Car.fromType");
		newQuery.setParameter("key", trueType.getKey());
		
		org.datanucleus.store.types.sco.backed.ArrayList queryResult = (org.datanucleus.store.types.sco.backed.ArrayList) newQuery.getResultList().get(0);
    	List<Car> toReturn = new ArrayList<Car>();
    	
    	for (Object car : queryResult) {
    		toReturn.add((Car) car);
    	}
    	
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