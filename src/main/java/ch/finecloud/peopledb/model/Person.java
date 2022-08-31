package ch.finecloud.peopledb.model;

import java.time.ZonedDateTime;

public class Person {
    private Long id;
    private String firstName;
    private String lastName;
    private ZonedDateTime dob;

    public Person(String firstName, String lastName, ZonedDateTime odb) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dob = odb;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public ZonedDateTime getDob() {
        return dob;
    }

    public void setDob(ZonedDateTime dob) {
        this.dob = dob;
    }
}
