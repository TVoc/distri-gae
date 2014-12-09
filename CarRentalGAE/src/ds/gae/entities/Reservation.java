package ds.gae.entities;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.datanucleus.annotations.Unowned;

@Entity
public class Reservation {

    private int carId;
    
    /***************
	 * CONSTRUCTOR *
	 ***************/

    public Reservation() {
    	
    }
    
    public Reservation(Quote quote, int carId) {
    	this.quote = quote;
        this.carId = carId;
    }
    
    /******
     * ID *
     ******/
    
    public int getCarId() {
    	return carId;
    }
    
    public void setCarId(int carId) {
    	this.carId = carId;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;
    
    public Key getKey() {
    	return this.key;
    }
    
    public void setKey(Key key) {
    	this.key = key;
    }
    
    @OneToOne
    @Unowned
    private Quote quote;
    
    public Quote getQuote() {
    	return this.quote;
    }
    
    public void setQuote(Quote quote) {
    	this.quote = quote;
    }
    
    /*************
     * TO STRING *
     *************/
    
    public String getCarRenter() {
    	return this.getQuote().getCarRenter();
    }
    
    public Date getStartDate() {
    	return this.getQuote().getStartDate();
    }
    
    public Date getEndDate() {
    	return this.getQuote().getEndDate();
    }
    
    public String getRentalCompany() {
    	return this.getQuote().getRentalCompany();
    }
    
    public String getCarType() {
    	return this.getQuote().getCarType();
    }
    
    public double getRentalPrice() {
    	return this.getQuote().getRentalPrice();
    }
    
    @Override
    public String toString() {
        return String.format("Reservation for %s from %s to %s at %s\nCar type: %s\tCar: %s\nTotal price: %.2f", 
                getCarRenter(), getStartDate(), getEndDate(), getRentalCompany(), getCarType(), getCarId(), getRentalPrice());
    }
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + carId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj))
			return false;
		Reservation other = (Reservation) obj;
		if (carId != other.carId)
			return false;
		return true;
	}
}