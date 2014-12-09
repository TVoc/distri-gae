package ds.gae.tasks;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;

import com.google.appengine.api.taskqueue.DeferredTask;

import ds.gae.EMF;
import ds.gae.ReservationException;
import ds.gae.entities.CarRentalCompany;
import ds.gae.entities.Quote;
import ds.gae.entities.Reservation;

public class ConfirmQuotesTask implements DeferredTask {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4160055124556837025L;

	public ConfirmQuotesTask(List<Quote> quotes) throws IllegalArgumentException {
		if (quotes == null) {
			throw new IllegalArgumentException("quotes cannot be null");
		}
		if (quotes.isEmpty()) {
			throw new IllegalArgumentException("quotes cannot be empty");
		}
		this.quotes = quotes;
	}
	
	private List<Quote> quotes;
	
	private List<Quote> getQuotes() {
		return this.quotes;
	}
	
	@Override
	public void run() {
		
		List<Reservation> reservations = new LinkedList<Reservation>();
		
		try {
        	for (Quote quote : this.getQuotes()) {
        		EntityManager em = EMF.get().createEntityManager();
        		Quote persQuote = em.find(Quote.class, quote.getKey());
        		em.remove(persQuote);
        		em.close();
        		System.out.println("Removed key: " + persQuote.getKey());
        		persQuote.setKey(null);
        		System.out.println("After null: " + persQuote.getKey());
        		EntityManager newEm = EMF.get().createEntityManager();
        		System.out.println("Reached");
        		CarRentalCompany c = newEm.find(CarRentalCompany.class, quote.getRentalCompany());
        		Reservation res = c.confirmQuote(persQuote);
        		System.out.println("Reached 2");
        		newEm.persist(persQuote);
        		System.out.println("Reached 3");
        		newEm.persist(res);
        		System.out.println("Reached 4");
        		reservations.add(res);
        		System.out.println("Reached 5");
        		newEm.close();
        		System.out.println("Re-added key: " + persQuote.getKey());
        		quote.setKey(persQuote.getKey());
        	}
    	} catch (ReservationException e) {
    		for (Reservation res : reservations) {
    			EntityManager resEm = EMF.get().createEntityManager();
    			//Quote quote = resEm.find(Quote.class, res.getQuote().getKey()); /* quote must become attached, else deletion will
    			//be delayed until next time Reservation table is queried, producing an error */
    			CarRentalCompany c = resEm.find(CarRentalCompany.class, res.getRentalCompany());
    			//resEm.remove(quote);
    			c.cancelReservation(res);
    			resEm.close();
    		}
    	} finally {
    		for (Quote quote : this.getQuotes()) {
    			System.out.println("Key: " + quote.getKey());
    			EntityManager em = EMF.get().createEntityManager();
    			Quote persQuote = em.find(Quote.class, quote.getKey());
    			persQuote.setCompleted(true);
    			em.close();
    		}
    	}
	}

}
