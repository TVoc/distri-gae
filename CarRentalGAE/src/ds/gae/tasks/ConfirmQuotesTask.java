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
		EntityManager em = null;
		Quote persQuote = null;
		
		try {
        	for (Quote quote : this.getQuotes()) {
        		em = EMF.get().createEntityManager();
        		persQuote =  em.find(Quote.class, quote.getKey());
        		
        		em.detach(persQuote);
        		CarRentalCompany c = em.find(CarRentalCompany.class, quote.getRentalCompany());
        		Reservation res = c.confirmQuote(persQuote);        		
        		em.persist(res);
        		em.merge(persQuote);
        		
        		reservations.add(res);
        		em.close();        		
        	}
    	} catch (ReservationException e) {
    		em.close();
    		
    		for (Reservation res : reservations) {
    			EntityManager resEm = EMF.get().createEntityManager();
    			CarRentalCompany c = resEm.find(CarRentalCompany.class, res.getRentalCompany());
    			c.cancelReservation(res);
    			resEm.close();
    		}
    	} finally {
    		for (Quote quote : this.getQuotes()) {
    			em = EMF.get().createEntityManager();
    			persQuote = em.find(Quote.class, quote.getKey());
    			persQuote.setCompleted(true);
    			em.close();
    		}
    	}
	}

}
