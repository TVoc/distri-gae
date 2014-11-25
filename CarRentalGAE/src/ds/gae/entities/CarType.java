package ds.gae.entities;

import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import com.google.appengine.api.datastore.Key;

@Entity
public class CarType {
    
	@Basic
    private String name;
	
	@Basic
    private int nbOfSeats;
	
	@Basic
    private boolean smokingAllowed;
	
	@Basic
    private double rentalPricePerDay;
	
	@Basic
    //trunk space in liters
    private float trunkSpace;
	
	private String company;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Key key;
	
	@OneToMany(cascade = CascadeType.PERSIST)
	private List<Car> cars;
    
    /***************
	 * CONSTRUCTOR *
	 ***************/
    
	public CarType() {
		
	}
	
    public CarType(String name, int nbOfSeats, float trunkSpace, double rentalPricePerDay, boolean smokingAllowed, List<Car> cars) {
        this.name = name;
        this.nbOfSeats = nbOfSeats;
        this.trunkSpace = trunkSpace;
        this.rentalPricePerDay = rentalPricePerDay;
        this.smokingAllowed = smokingAllowed;
        this.cars = cars;
    }

    public String getName() {
    	return name;
    }
    
    public int getNbOfSeats() {
        return nbOfSeats;
    }
    
    public boolean isSmokingAllowed() {
        return smokingAllowed;
    }

    public double getRentalPricePerDay() {
        return rentalPricePerDay;
    }
    
    public float getTrunkSpace() {
    	return trunkSpace;
    }
    
    public List<Car> getCars() {
    	return this.cars;
    }
    
    public void setCars(List<Car> cars) {
    	this.cars = cars;
    }
    
    public String getCompany() {
    	return this.company;
    }
    
    public void setCompany(String company) {
    	this.company = company;
    }
    
    public Key getKey() {
    	return this.key;
    }
    
    public void setKey(Key key) {
    	this.key = key;
    }
    
    /*************
     * TO STRING *
     *************/
    
    @Override
    public String toString() {
    	return String.format("Car type: %s \t[seats: %d, price: %.2f, smoking: %b, trunk: %.0fl]" , 
                getName(), getNbOfSeats(), getRentalPricePerDay(), isSmokingAllowed(), getTrunkSpace());
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CarType other = (CarType) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}