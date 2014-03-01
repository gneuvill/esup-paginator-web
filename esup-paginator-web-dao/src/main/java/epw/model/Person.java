/**
 * ESUP-Portail Blank Application - Copyright (c) 2010 ESUP-Portail consortium.
 */
package epw.model;

import fj.F2;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "PERSON")
public class Person implements Serializable {
    @Id
    @GeneratedValue
    private Long id;

    /**
     * see <a href="https://hibernate.onjira.com/browse/HHH-6215">here</a>
     */
    @Column(nullable = false)
    private final String firstName;

    /**
     * see <a href="https://hibernate.onjira.com/browse/HHH-6215">here</a>
     */
    @Column(nullable = false)
    private final String lastName;

    @OneToOne @Type(type = "epw.model.AddressEntity")
    private final Address address;

    private Person() {
        firstName = "";
        lastName = "";
        address = Address.address("");
    }

    private Person(String firstName, String lastName, Address address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
    }

    public static Person person(String firstname, String lastname, Address address) {
        return new Person(firstname, lastname, address);
    }

    public static F2<String, String, Person> person = new F2<String, String, Person>() {
        public Person f(String fn, String ln) {
            return person(fn, ln, Address.address(""));
        }
    };

    public Long getId() { return id; }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}