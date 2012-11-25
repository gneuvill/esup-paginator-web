/**
 * ESUP-Portail Blank Application - Copyright (c) 2010 ESUP-Portail consortium.
 */
package epw.domain.beans;

import fj.F2;

import javax.persistence.*;

@Entity
@Table(name = "\"USER\"")
public class User {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private final String firstName;

    @Column(nullable = false)
    private final String lastName;

    private User() {
        firstName = null;
        lastName = null;
    }

    private User(final String firstName, final String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public static User user(final String firstname, final String lastname) {
        return new User(firstname, lastname);
    }

    public static F2<String, String, User> user = new F2<String, String, User>() {
        public User f(String fn, String ln) {
            return user(fn, ln);
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