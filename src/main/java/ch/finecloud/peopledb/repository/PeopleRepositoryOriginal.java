package ch.finecloud.peopledb.repository;

import ch.finecloud.peopledb.model.Person;

import java.math.BigDecimal;
import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class PeopleRepositoryOriginal {
    public static final String SAVE_PERSON_SQL = String.format("INSERT INTO PEOPLE (FIRST_NAME, LAST_NAME, DOB) VALUES(?,?,?)");
    public static final String GET_PERSON_BY_ID = String.format("SELECT ID, FIRST_NAME, LAST_NAME, DOB, SALARY FROM PEOPLE WHERE ID=?");
    private Connection connection;
    public PeopleRepositoryOriginal(Connection connection) {
        this.connection = connection;
    }

    public Person save(Person person) {
        try {
            PreparedStatement ps = connection.prepareStatement(SAVE_PERSON_SQL, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, person.getFirstName());
            ps.setString(2, person.getLastName());
            ps.setTimestamp(3, convertODBtoTimeStamp(person.getDob()));
            int recordsAffected = ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            while (rs.next()) {
                long id = rs.getLong(1);
                person.setId(id);
                System.out.println(person);
            }
            System.out.printf("Records affected: %d%n", recordsAffected);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return person;
    }

    public Optional<Person> findById(Long id) {
        Person person = null;

        try {
            PreparedStatement ps = connection.prepareStatement(GET_PERSON_BY_ID);
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                person = extractPersonFromResultSet(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.ofNullable(person);
    }

    private Person extractPersonFromResultSet(ResultSet rs) throws SQLException {
        long personID = rs.getLong("ID");
        String firstName = rs.getString("FIRST_NAME");
        String lastName = rs.getString("LAST_NAME");
        ZonedDateTime dob = ZonedDateTime.of(rs.getTimestamp("DOB").toLocalDateTime(), ZoneId.of("+0"));
        BigDecimal salary = rs.getBigDecimal("SALARY");
        return new Person(personID, firstName, lastName, dob, salary);
    }

    public long count() {
        long count = 0;
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM PEOPLE");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                count = rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }
    public void delete(Person person) {
        try {
            PreparedStatement ps = connection.prepareStatement("DELETE FROM PEOPLE WHERE ID=?");
            ps.setLong(1, person.getId());
            int recordsAffected = ps.executeUpdate();
            System.out.println(recordsAffected);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(Person...people) {
//        This would be the easiest Option to delete people,
//        however from a DB view it's not the most efficient:
//        for (Person person : people) {
//            delete(person);
//        }
        try {
//            - Unfortunately the current version of H2 DB does not support a prepared SQL statement for this.
//            - This will make us vulnerable to SQL injection, but for demo purposes we do it anyway
//            - This should never be used in production
            Statement stmt = connection.createStatement();
            String ids = Arrays.stream(people).toList().stream()
                    .map(person -> person.getId().toString())
                    .collect(Collectors.joining(","));
            int affectedRecordCount = stmt.executeUpdate("DELETE FROM PEOPLE WHERE ID IN (:ids)".replace(":ids", ids));
            System.out.println(affectedRecordCount);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(Person person) {
        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE PEOPLE SET FIRST_NAME=?, LAST_NAME=?, DOB=?, SALARY=? WHERE ID=?");
            ps.setString(1, person.getFirstName());
            ps.setString(2, person.getLastName());
            ps.setTimestamp(3, convertODBtoTimeStamp(person.getDob()));
            ps.setBigDecimal(4, person.getSalary());
            ps.setLong(5, person.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Timestamp convertODBtoTimeStamp(ZonedDateTime dob) {
        return Timestamp.valueOf(dob.withZoneSameInstant(ZoneId.of("+0")).toLocalDateTime());
    }
}
