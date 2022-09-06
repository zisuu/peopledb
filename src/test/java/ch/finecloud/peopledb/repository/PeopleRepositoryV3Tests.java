package ch.finecloud.peopledb.repository;

import ch.finecloud.peopledb.model.Person;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class PeopleRepositoryV3Tests {

    private Connection connection;
    private PeopleRepositoryV3 repo;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:~/peopletest".replace("~", System.getProperty("user.home")));
        connection.setAutoCommit(false);
        repo = new PeopleRepositoryV3(connection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void canSaveOnePerson() {
        Person john = new Person("John", "Smith", ZonedDateTime.of(1980,11,15,15,15,0,0, ZoneId.of("-6")));
        Person savedPerson = repo.save(john);
        assertThat(savedPerson.getId()).isGreaterThan(0);
    }

    @Test
    public void canSaveTwoPeople() {
        Person john = new Person("John", "Smith", ZonedDateTime.of(1980,11,15,15,15,0,0, ZoneId.of("-6")));
        Person bobby = new Person("Bobby", "Smith", ZonedDateTime.of(1982,9,13,1,51,54,0, ZoneId.of("+1")));
        Person savedPerson1 = repo.save(john);
        Person savedPerson2 = repo.save(bobby);
        assertThat(savedPerson1.getId()).isNotEqualTo(savedPerson2.getId());
    }

    @Test
    public void canFindPersonById() {
        Person savedPerson = repo.save(new Person("Test", "Jackson", ZonedDateTime.now()));
        Person foundPerson = repo.findById(savedPerson.getId()).get();
        assertThat(foundPerson).isEqualTo(savedPerson);
    }

    @Test
    public void testPersonIdNotFound() {
        Optional<Person> foundPerson = repo.findById(-1L);
        assertThat(foundPerson).isEmpty();
    }

    @Test
    public void canDeleteOnePerson() {
        Person savedPerson = repo.save(new Person("Test", "Jackson", ZonedDateTime.now()));
        long startCount = repo.count();
        repo.delete(savedPerson);
        long endCount = repo.count();
        assertThat(endCount).isEqualTo(startCount-1);
    }

    @Test
    public void canDeleteMultiplePerson() {
        Person p1 = repo.save(new Person("Test1", "Jackson", ZonedDateTime.now()));
        Person p2 = repo.save(new Person("Test2", "Jackson", ZonedDateTime.now()));
        long startCount = repo.count();
        repo.delete(p1, p2);
        long endCount = repo.count();
        assertThat(endCount).isEqualTo(startCount -2);
    }

    @Test
    public void canGetCount() {
        long startCount = repo.count();
        repo.save(new Person("John1", "Smoth", ZonedDateTime.of(1880,11,15,4,44,0,0,ZoneId.of("+6"))));
        repo.save(new Person("John2", "Smoth", ZonedDateTime.of(1880,11,15,4,44,0,0,ZoneId.of("+6"))));
        long endCount = repo.count();
        assertThat(endCount).isEqualTo(startCount +2);
    }

    @Test
    public void canUpdate() {
        Person savedPerson = repo.save(new Person("Test2", "Jackson", ZonedDateTime.now()));
        Person p1 = repo.findById(savedPerson.getId()).get();

        savedPerson.setSalary(new BigDecimal("7300.28"));
        repo.update(savedPerson);

        Person P2 = repo.findById(savedPerson.getId()).get();
        assertThat(P2.getSalary()).isNotEqualTo(p1.getSalary());
    }

    @Test
    public void canFindAll() {
        repo.save(new Person("John", "Smith", ZonedDateTime.of(1980,11,14,16,22,11,0,ZoneId.of("+1"))));
        repo.save(new Person("John1", "Smith", ZonedDateTime.of(1980,11,14,16,22,11,0,ZoneId.of("+1"))));
        repo.save(new Person("John2", "Smith", ZonedDateTime.of(1980,11,14,16,22,11,0,ZoneId.of("+1"))));
        repo.save(new Person("John3", "Smith", ZonedDateTime.of(1980,11,14,16,22,11,0,ZoneId.of("+1"))));
        repo.save(new Person("John4", "Smith", ZonedDateTime.of(1980,11,14,16,22,11,0,ZoneId.of("+1"))));
        repo.save(new Person("John5", "Smith", ZonedDateTime.of(1980,11,14,16,22,11,0,ZoneId.of("+1"))));
        repo.save(new Person("John6", "Smith", ZonedDateTime.of(1980,11,14,16,22,11,0,ZoneId.of("+1"))));
        repo.save(new Person("John7", "Smith", ZonedDateTime.of(1980,11,14,16,22,11,0,ZoneId.of("+1"))));
        repo.save(new Person("John8", "Smith", ZonedDateTime.of(1980,11,14,16,22,11,0,ZoneId.of("+1"))));
        repo.save(new Person("John9", "Smith", ZonedDateTime.of(1980,11,14,16,22,11,0,ZoneId.of("+1"))));

        List<Person> people = repo.findAll();
        assertThat(people.size()).isGreaterThanOrEqualTo(10);
    }

    @Test
    public void loadData() throws IOException, SQLException {
            Files.lines(Path.of("/Users/Dave/Downloads/Hr5m.csv"))
                    .skip(1)
//                    .limit(100)
                    .map(l -> l.split(","))
                    .map(a -> {
                            LocalDate dob = LocalDate.parse(a[10], DateTimeFormatter.ofPattern("M/d/yyyy"));
                            LocalTime tob = LocalTime.parse(a[11], DateTimeFormatter.ofPattern("hh:mm:ss a").localizedBy(Locale.ENGLISH));
                            LocalDateTime dtob = LocalDateTime.of(dob, tob);
                            ZonedDateTime zdtob = ZonedDateTime.of(dtob, ZoneId.of("+0"));
                            Person person = new Person(a[2], a[4], zdtob);
                            person.setSalary(new BigDecimal(a[25]));
                            person.setEmail(a[6]);
                            return person;
                        })
                    .forEach(repo::save);
            connection.commit();
    }
}
