/**
 * ESUP-Portail Blank Application - Copyright (c) 2010 ESUP-Portail consortium.
 */
package epw.domain.beans;

import fj.F2;

import javax.persistence.*;

@Entity
@Table(name = "PERSON")
public class Person {

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

    public Person() {
        firstName = "";
        lastName = "";
    }

    private Person(final String firstName, final String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public static Person person(final String firstname, final String lastname) {
        return new Person(firstname, lastname);
    }

    public static F2<String, String, Person> person = new F2<String, String, Person>() {
        public Person f(String fn, String ln) {
            return person(fn, ln);
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